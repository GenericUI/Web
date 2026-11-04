package com.nexustools.genericui.web

import com.nexustools.genericui.io.ByteBuffer
import com.nexustools.genericui.nativeimpl.io.DatabaseReadableNativeImpl
import com.nexustools.genericui.nativeimpl.io.DatabaseWritableNativeImpl
import com.nexustools.genericui.nativeimpl.io.DefaultDatabaseReadableNativeImpl
import com.nexustools.genericui.nativeimpl.io.DefaultDatabaseWritableNativeImpl
import com.nexustools.genericui.nativeimpl.system.DefaultDatabaseNativeImpl
import com.nexustools.genericui.util.db.DatabaseQuery
import com.nexustools.genericui.util.db.DatabaseRecord
import com.nexustools.genericui.util.db.DatabaseTableSchema
import com.nexustools.genericui.util.db.DatabaseValue

/**
 * Web (`:web`, `js` + `wasmJs`) implementation of [com.nexustools.genericui.nativeimpl.system.DatabaseNativeImpl]
 * backed by browser `window.indexedDB` in a strictly isolated namespace:
 * - IndexedDB database name: `"genericui_db:<databaseName>:<sanitizedFilePath>"` (never `"genericui_user_dirs:..."`)
 * - Object store name: `"tbl:<tableName>"` (never bare `"config"` or `"cache"`)
 *
 * This dual-level isolation guarantees that [com.nexustools.genericui.util.Database] on `:web` can
 * **never** conflict with the `"config"` and `"cache"` stores used by `ConfigDirectory` and `CacheDirectory`,
 * even if an application creates a `Database` table literally named `"config"` or `"cache"`.
 */
class WebDatabaseNativeImpl(
    databaseName: String = "GenericUI",
    filePath: String = "$databaseName.db",
    version: Int = 1
) : DefaultDatabaseNativeImpl(databaseName, filePath, version) {

    /**
     * Strictly isolated browser IndexedDB database name (`"genericui_db:<databaseName>:<sanitizedFilePath>"`).
     */
    val indexedDbName: String = buildIsolatedIndexedDbName(databaseName, filePath)

    private var writeTickCounter: Long = 1700000000000L

    init {
        WebBrowserDomBridge.idbDatabaseOpen(indexedDbName, version)
    }

    /**
     * Resolves the prefixed IndexedDB object store name (`"tbl:<tableName>"`) for [table],
     * guaranteeing it never collides with bare `"config"` or `"cache"` stores.
     */
    fun resolveObjectStoreName(table: String): String =
        "tbl:${normalizeTableName(table)}"

    override suspend fun defineTable(schema: DatabaseTableSchema): Boolean {
        if (isDisposed) return false
        super.defineTable(schema)
        return WebBrowserDomBridge.idbDatabaseDefineTable(indexedDbName, schema)
    }

    override suspend fun putRecord(table: String, record: DatabaseRecord): Boolean {
        if (isDisposed) return false
        val norm = normalizeTableName(table)
        super.putRecord(norm, record)
        return WebBrowserDomBridge.idbDatabasePutRecord(indexedDbName, norm, record)
    }

    override suspend fun getRecord(table: String, key: String): DatabaseRecord? {
        if (isDisposed) return null
        val norm = normalizeTableName(table)
        return WebBrowserDomBridge.idbDatabaseGetRecord(indexedDbName, norm, key)
            ?: super.getRecord(norm, key)
    }

    override suspend fun query(query: DatabaseQuery): List<DatabaseRecord> {
        if (isDisposed) return emptyList()
        val norm = normalizeTableName(query.table)
        val records = WebBrowserDomBridge.idbDatabaseGetAllRecords(indexedDbName, norm)
        return query.applyToRecords(records)
    }

    override suspend fun count(query: DatabaseQuery): Long {
        if (isDisposed) return 0L
        val norm = normalizeTableName(query.table)
        val records = WebBrowserDomBridge.idbDatabaseGetAllRecords(indexedDbName, norm)
        val activeFilter = query.filter ?: return records.size.toLong()
        return records.count { activeFilter.matches(it) }.toLong()
    }

    override suspend fun deleteWhere(query: DatabaseQuery): Int {
        if (isDisposed) return 0
        val norm = normalizeTableName(query.table)
        val activeFilter = query.filter
        if (activeFilter == null) {
            super.clearTable(norm)
            return WebBrowserDomBridge.idbDatabaseClearTable(indexedDbName, norm)
        }
        val records = WebBrowserDomBridge.idbDatabaseGetAllRecords(indexedDbName, norm)
        val matchingKeys = records.filter { activeFilter.matches(it) }.map { it.key }
        var removed = 0
        for (k in matchingKeys) {
            super.delete(norm, k)
            if (WebBrowserDomBridge.idbDatabaseDeleteRecord(indexedDbName, norm, k)) {
                removed++
            }
        }
        return removed
    }

    override suspend fun read(table: String, key: String): ByteBuffer? =
        getRecord(table, key)?.getBuffer("value")

    override suspend fun write(
        table: String,
        key: String,
        buffer: ByteBuffer,
        append: Boolean
    ): Boolean {
        if (isDisposed) return false
        val payload = if (append) {
            val existing = read(table, key)?.toByteArray() ?: ByteArray(0)
            val incoming = buffer.toByteArray()
            ByteBuffer.wrap(if (existing.isEmpty()) incoming else existing + incoming)
        } else {
            buffer.copy()
        }
        val existingRecord = getRecord(table, key)
        val nextValues = LinkedHashMap<String, DatabaseValue>()
        if (existingRecord != null) {
            nextValues.putAll(existingRecord.values)
        }
        nextValues["key"] = DatabaseValue.of(key)
        nextValues["value"] = DatabaseValue.of(payload)
        nextValues["updatedAt"] = DatabaseValue.of(++writeTickCounter)
        return putRecord(table, DatabaseRecord(key = key, values = nextValues))
    }

    override suspend fun openReadable(table: String, key: String): DatabaseReadableNativeImpl? {
        val buf = read(table, key) ?: return null
        return DefaultDatabaseReadableNativeImpl(
            table = normalizeTableName(table),
            key = key,
            initialBytes = buf.toByteArray()
        )
    }

    override suspend fun openWritable(
        table: String,
        key: String,
        append: Boolean
    ): DatabaseWritableNativeImpl {
        val initial = if (append) read(table, key)?.toByteArray() ?: ByteArray(0) else ByteArray(0)
        return DefaultDatabaseWritableNativeImpl(
            table = normalizeTableName(table),
            key = key,
            appendMode = append,
            initialBytes = initial
        ) { committed ->
            write(table, key, ByteBuffer.wrap(committed), append = false)
        }
    }

    override suspend fun exists(table: String, key: String): Boolean {
        if (isDisposed) return false
        return getRecord(table, key) != null
    }

    override suspend fun byteSize(table: String, key: String): Long =
        read(table, key)?.size?.toLong() ?: 0L

    override suspend fun delete(table: String, key: String): Boolean {
        if (isDisposed) return false
        val norm = normalizeTableName(table)
        super.delete(norm, key)
        return WebBrowserDomBridge.idbDatabaseDeleteRecord(indexedDbName, norm, key)
    }

    override suspend fun keys(table: String, prefix: String): List<String> {
        if (isDisposed) return emptyList()
        val norm = normalizeTableName(table)
        return WebBrowserDomBridge.idbDatabaseKeys(indexedDbName, norm, prefix)
    }

    override suspend fun clearTable(table: String): Int {
        if (isDisposed) return 0
        val norm = normalizeTableName(table)
        super.clearTable(norm)
        return WebBrowserDomBridge.idbDatabaseClearTable(indexedDbName, norm)
    }

    override suspend fun dropTable(table: String): Boolean {
        if (isDisposed) return false
        val norm = normalizeTableName(table)
        super.dropTable(norm)
        return WebBrowserDomBridge.idbDatabaseDropTable(indexedDbName, norm)
    }

    override suspend fun tables(): List<String> {
        if (isDisposed) return emptyList()
        return WebBrowserDomBridge.idbDatabaseTables(indexedDbName)
    }

    override suspend fun totalByteSize(table: String): Long {
        if (isDisposed) return 0L
        val norm = normalizeTableName(table)
        val records = WebBrowserDomBridge.idbDatabaseGetAllRecords(indexedDbName, norm)
        var total = 0L
        for (rec in records) {
            total += rec.getBuffer("value")?.size?.toLong() ?: 0L
        }
        return total
    }

    companion object {
        fun buildIsolatedIndexedDbName(
            databaseName: String,
            filePath: String = "$databaseName.db"
        ): String = WebBrowserDomBridge.buildIsolatedIndexedDbName(databaseName, filePath)
    }
}
