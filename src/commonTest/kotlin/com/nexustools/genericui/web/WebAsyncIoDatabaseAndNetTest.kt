package com.nexustools.genericui.web

import com.nexustools.genericui.component.system.PreferenceStore
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.image.ImageBuffer
import com.nexustools.genericui.image.ImageLoader
import com.nexustools.genericui.image.PixelFormat
import com.nexustools.genericui.image.cache.ImageCacheManager
import com.nexustools.genericui.image.codec.LibPngCodec
import com.nexustools.genericui.io.ByteBuffer
import com.nexustools.genericui.io.FileSystem
import com.nexustools.genericui.io.FileSystemAvailability
import com.nexustools.genericui.io.ReadableByteBuffer
import com.nexustools.genericui.nativeimpl.io.DatabaseReadableNativeImpl
import com.nexustools.genericui.nativeimpl.io.DatabaseWritableNativeImpl
import com.nexustools.genericui.net.Network
import com.nexustools.genericui.net.http.HttpClient
import com.nexustools.genericui.net.http.HttpMethod
import com.nexustools.genericui.net.http.HttpResponse
import com.nexustools.genericui.net.socketio.SocketIoClient
import com.nexustools.genericui.resources.ResourceType
import com.nexustools.genericui.resources.ResourceID
import com.nexustools.genericui.style.color.RgbaColor
import com.nexustools.genericui.util.CacheDirectory
import com.nexustools.genericui.util.ConfigDirectory
import com.nexustools.genericui.util.Database
import com.nexustools.genericui.util.db.DatabaseColumnSpec
import com.nexustools.genericui.util.db.DatabaseColumnType
import com.nexustools.genericui.util.db.DatabaseFilter
import com.nexustools.genericui.util.db.DatabaseQuery
import com.nexustools.genericui.util.db.DatabaseRecord
import com.nexustools.genericui.util.db.DatabaseSortDirection
import com.nexustools.genericui.util.db.DatabaseTableSchema
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebAsyncIoDatabaseAndNetTest {

    @BeforeTest
    fun setUp() {
        WebBrowserDomBridge.localStorageClear()
        WebBrowserDomBridge.idbUserDirResetAll()
        WebBrowserDomBridge.idbDatabaseResetAll()
        WebBrowserDomBridge.isFileSystemAccessSupported = true
        WebBrowserDomBridge.browserFetchHook = null
        WebBrowserDomBridge.browserWebSocketFactoryHook = null
        GenericUI.applicationName = "WebAsyncStudioApp"
        GenericUI.activeFactory = WebNativeFactory()
    }

    @AfterTest
    fun tearDown() {
        WebBrowserDomBridge.localStorageClear()
        WebBrowserDomBridge.idbUserDirResetAll()
        WebBrowserDomBridge.idbDatabaseResetAll()
        WebBrowserDomBridge.isFileSystemAccessSupported = true
        WebBrowserDomBridge.browserFetchHook = null
        WebBrowserDomBridge.browserWebSocketFactoryHook = null
    }

    private fun runSuspend(block: suspend () -> Unit) {
        var failure: Throwable? = null
        var completed = false
        block.startCoroutine(
            object : Continuation<Unit> {
                override val context: CoroutineContext = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) {
                    completed = true
                    failure = result.exceptionOrNull()
                }
            }
        )
        failure?.let { throw it }
        assertTrue(completed, "Expected suspend test block to complete without hanging")
    }

    @Test
    fun testConfigAndCacheDirectoriesUseDirectIndexedDbWithoutDatabaseOrConflict() = runSuspend {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory

        val configImpl = factory.createConfigDirectory("WebAsyncStudioApp", "") as WebUserDirectoryNativeImpl
        val cacheImpl = factory.createCacheDirectory("WebAsyncStudioApp", "") as WebUserDirectoryNativeImpl

        assertFalse(configImpl.usesFileSystemBasePath)
        assertFalse(cacheImpl.usesFileSystemBasePath)
        assertEquals("genericui_user_dirs:WebAsyncStudioApp", configImpl.indexedDbDatabaseName)
        assertEquals("genericui_user_dirs:WebAsyncStudioApp", cacheImpl.indexedDbDatabaseName)
        assertEquals("config", configImpl.storageTableName)
        assertEquals("cache", cacheImpl.storageTableName)

        val configDir = ConfigDirectory(appName = "WebAsyncStudioApp", factory = factory)
        val cacheDir = CacheDirectory(appName = "WebAsyncStudioApp", factory = factory)

        // Write to ConfigDirectory & CacheDirectory via async and sync APIs
        assertTrue(configDir.write("workspace.json", ByteBuffer.fromString("{\"layout\":\"docked\"}")))
        assertTrue(cacheDir.write("preview.bin", ByteBuffer.wrap(byteArrayOf(10, 20, 30, 40))))

        val cfgWriter = configDir.openWritable("recent_files.txt", append = false)
        assertTrue(cfgWriter is WebFileWritableNativeImpl)
        cfgWriter.writeText("scene1.gui\n")
        cfgWriter.endText("scene2.gui")
        val cfgReader = configDir.openReadable("recent_files.txt")
        assertTrue(cfgReader is WebFileReadableNativeImpl)
        assertEquals("scene1.gui\nscene2.gui", cfgReader.readAllText())

        // Verify ConfigDirectory & CacheDirectory wrote to UserDirectory IndexedDB and did NOT touch Database IndexedDB!
        assertTrue(WebBrowserDomBridge.idbUserDirDatabaseNames().contains("genericui_user_dirs:WebAsyncStudioApp"))
        assertTrue(WebBrowserDomBridge.idbDatabaseNames().isEmpty(), "UserDirectory must NEVER instantiate or write to Database IndexedDB")

        // Now create a Database instance with tables explicitly named "config" and "cache"
        val db = Database(databaseName = "WebAsyncStudioApp", factory = factory)
        val dbImpl = db.nativeImpl as WebDatabaseNativeImpl
        assertTrue(dbImpl.indexedDbName.startsWith("genericui_db:"))
        assertFalse(dbImpl.indexedDbName.startsWith("genericui_user_dirs:"))
        assertNotEquals(configImpl.indexedDbDatabaseName, dbImpl.indexedDbName)
        assertEquals("tbl:config", dbImpl.resolveObjectStoreName("config"))
        assertEquals("tbl:cache", dbImpl.resolveObjectStoreName("cache"))

        assertTrue(db.writeText("config", "workspace.json", "DATABASE_CONFIG_VALUE"))
        assertTrue(db.writeBytes("cache", "preview.bin", byteArrayOf(99, 98, 97)))

        // Verify raw object store names in Database IndexedDB are prefixed with "tbl:"
        val rawDbStores = WebBrowserDomBridge.idbDatabaseObjectStoreNames(dbImpl.indexedDbName)
        assertTrue(rawDbStores.contains("tbl:config"))
        assertTrue(rawDbStores.contains("tbl:cache"))
        assertFalse(rawDbStores.contains("config"))
        assertFalse(rawDbStores.contains("cache"))

        // Verify ConfigDirectory and CacheDirectory values are 100% untouched by Database's "config" and "cache" tables!
        assertEquals("{\"layout\":\"docked\"}", configDir.read("workspace.json")?.toString())
        assertContentEquals(byteArrayOf(10, 20, 30, 40), cacheDir.read("preview.bin")?.toByteArray())
        assertEquals("DATABASE_CONFIG_VALUE", db.readText("config", "workspace.json"))
        assertContentEquals(byteArrayOf(99, 98, 97), db.readBytes("cache", "preview.bin"))

        // Clearing Database "config" and "cache" tables does NOT affect ConfigDirectory or CacheDirectory
        db.clearTable("config")
        db.clearTable("cache")
        assertEquals("{\"layout\":\"docked\"}", configDir.read("workspace.json")?.toString())
        assertContentEquals(byteArrayOf(10, 20, 30, 40), cacheDir.read("preview.bin")?.toByteArray())

        db.dispose()
        dbImpl.dispose()
        configDir.dispose()
        cacheDir.dispose()
        configImpl.dispose()
        cacheImpl.dispose()
    }

    @Test
    fun testWebDatabaseStructuredQueriesAndStreams() = runSuspend {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory

        val db = Database(databaseName = "StudioAssetsDb", version = 2, factory = factory)
        val schema = DatabaseTableSchema(
            tableName = "assets",
            primaryKeyColumn = "key",
            columns = listOf(
                DatabaseColumnSpec("key", DatabaseColumnType.STRING, primaryKey = true),
                DatabaseColumnSpec("name", DatabaseColumnType.STRING, indexed = true),
                DatabaseColumnSpec("sizeBytes", DatabaseColumnType.INT64, indexed = true),
                DatabaseColumnSpec("favorite", DatabaseColumnType.BOOLEAN)
            )
        )
        assertTrue(db.defineTable(schema))
        assertTrue(
            db.putRecord(
                "assets",
                DatabaseRecord("a1")
                    .withString("name", "Brush Soft")
                    .withLong("sizeBytes", 4096L)
                    .withBoolean("favorite", true)
            )
        )
        assertTrue(
            db.putRecord(
                "assets",
                DatabaseRecord("a2")
                    .withString("name", "Brush Ink")
                    .withLong("sizeBytes", 8192L)
                    .withBoolean("favorite", true)
            )
        )
        assertTrue(
            db.putRecord(
                "assets",
                DatabaseRecord("a3")
                    .withString("name", "Eraser Hard")
                    .withLong("sizeBytes", 1024L)
                    .withBoolean("favorite", false)
            )
        )

        val query = DatabaseQuery.from("assets")
            .where(
                DatabaseFilter.and(
                    DatabaseFilter.startsWith("name", "Brush "),
                    DatabaseFilter.eq("favorite", true)
                )
            )
            .orderBy("sizeBytes", DatabaseSortDirection.DESCENDING)

        val results = db.query(query)
        assertEquals(2, results.size)
        assertEquals("a2", results[0].key)
        assertEquals("a1", results[1].key)
        assertEquals(3L, db.count("assets"))

        val writable = db.openWritable("blobs", "thumb_a1")
        assertTrue(writable is DatabaseWritableNativeImpl)
        writable.writeText("THUMBNAIL_PAYLOAD")
        writable.end()

        val readable = db.openReadable("blobs", "thumb_a1")
        assertNotNull(readable)
        assertTrue(readable is DatabaseReadableNativeImpl)
        assertEquals("THUMBNAIL_PAYLOAD", readable.readAllText())

        db.dispose()
    }

    @Test
    fun testPreferenceStoreRemainsOnWindowLocalStorageWithGenericUiPrefixAndImageLoaderIsBrowserManaged() = runSuspend {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory

        // Simulate a third-party JS library / browser addon entry in window.localStorage
        WebBrowserDomBridge.localStorageSetItem("browser_addon:session_id", "ext-12345")
        WebBrowserDomBridge.localStorageSetItem("analytics_sdk_flag", "enabled")

        val prefs = PreferenceStore(appName = "WebAsyncStudioApp", factory = factory)
        assertEquals("window.localStorage", prefs.storageLocation)
        assertEquals("genericui:", WebPreferenceStoreImpl.KEY_PREFIX)

        val changedEvents = ArrayList<Pair<String?, Any?>>()
        prefs.addChangeListener { _, key, newValue ->
            changedEvents.add(key to newValue)
        }

        prefs.putString("ui.theme", "dark")
        prefs.putInt("ui.zoom", 125)
        assertEquals("dark", prefs.getString("ui.theme"))
        assertEquals(125, prefs.getInt("ui.zoom"))
        assertTrue(prefs.contains("ui.theme"))
        assertFalse(prefs.contains("browser_addon:session_id"))

        // Verify localStorage stores keys with the "genericui:" prefix and never bare keys
        assertNotNull(WebBrowserDomBridge.localStorageGetItem("genericui:ui.theme"))
        assertNotNull(WebBrowserDomBridge.localStorageGetItem("genericui:ui.zoom"))
        assertNull(WebBrowserDomBridge.localStorageGetItem("ui.theme"))
        assertNull(WebBrowserDomBridge.localStorageGetItem("ui.zoom"))

        // Verify keys() returns only stripped "genericui:" logical keys and ignores third-party keys
        assertEquals(setOf("ui.theme", "ui.zoom"), prefs.keys())
        assertTrue(WebBrowserDomBridge.idbUserDirDatabaseNames().isEmpty(), "PreferenceStore on :web must use window.localStorage, not IndexedDB")

        // Verify storage events from third-party keys are ignored while "genericui:" keys notify with stripped key
        val eventCountBeforeExternal = changedEvents.size
        WebBrowserDomBridge.dispatchStorageEvent("browser_addon:session_id", "ext-99999")
        assertEquals(eventCountBeforeExternal, changedEvents.size)

        WebBrowserDomBridge.dispatchStorageEvent("genericui:ui.theme", "\"light\"")
        assertEquals(eventCountBeforeExternal + 1, changedEvents.size)
        assertEquals("ui.theme", changedEvents.last().first)
        assertEquals("light", prefs.getString("ui.theme"))

        // Verify clear() removes ONLY "genericui:" keys and preserves third-party JS library / addon keys
        prefs.clear()
        assertTrue(prefs.keys().isEmpty())
        assertNull(WebBrowserDomBridge.localStorageGetItem("genericui:ui.theme"))
        assertNull(WebBrowserDomBridge.localStorageGetItem("genericui:ui.zoom"))
        assertEquals("ext-99999", WebBrowserDomBridge.localStorageGetItem("browser_addon:session_id"))
        assertEquals("enabled", WebBrowserDomBridge.localStorageGetItem("analytics_sdk_flag"))

        // Verify ImageLoader & ImageCacheManager on :web have isWebBrowserManaged == true and never write to CacheDirectory
        assertTrue(ImageLoader.isWebBrowserManaged)
        assertTrue(ImageCacheManager.isWebBrowserManaged)
        assertEquals(true, ImageLoader.activeNativeCodec?.usesBrowserManagedImageLoading)

        val sampleImg = ImageBuffer(4, 4, PixelFormat.RGBA_8888).fill(RgbaColor(10, 120, 240, 1f))
        val encodedPng = LibPngCodec.encode(sampleImg)

        ImageCacheManager.putInCache("https://cdn.example.com/sprite.png", encodedPng, sampleImg)
        assertFalse(ImageLoader.isCached("https://cdn.example.com/sprite.png"))
        assertEquals(0, ImageCacheManager.cachedFileCount)
        assertEquals(0L, ImageLoader.cachedDiskByteSize)
        assertTrue(ImageLoader.cacheDirectory.listFiles().isEmpty())

        // Verify WebFileSystemNativeImpl availability, path resolution, and WebFileReadableNativeImpl / WebFileWritableNativeImpl
        assertEquals(FileSystemAvailability.WithPermission, FileSystem.isAvailable())
        val resolvedFsPath = FileSystem.resolve("indexdb://workspace/project.json")
        assertEquals("/workspace/project.json", resolvedFsPath)
        assertFalse(resolvedFsPath.contains("indexdb://"))

        val fsWrite = FileSystem.openWritable("/workspace/project.json")
        assertTrue(fsWrite is WebFileWritableNativeImpl)
        fsWrite.writeText("{\"version\":1}")
        fsWrite.end()

        val fsRead = FileSystem.openReadable("/workspace/project.json")
        assertTrue(fsRead is WebFileReadableNativeImpl)
        assertEquals("{\"version\":1}", fsRead.read()?.toString())
        fsRead.close()

        WebBrowserDomBridge.isFileSystemAccessSupported = false
        assertEquals(FileSystemAvailability.Unavailable, FileSystem.isAvailable())
        WebBrowserDomBridge.isFileSystemAccessSupported = true

        // Verify WebResourceResolverImpl.openResourceStream(resId: Int) and (resId: ResourceID)
        val rawRes = ResourceID(
            id = ResourceType.RAW.buildNumericId(42),
            type = ResourceType.RAW,
            name = "preload_config",
            relativePath = "raw/preload_config.json"
        )
        WebResourceResolverImpl.registerPreloadedResource(rawRes, "{\"preloaded\":true}", "application/json")
        val resolver = factory.createResourceResolver() as WebResourceResolverImpl
        val streamById = resolver.openResourceStream(rawRes)
        assertNotNull(streamById)
        assertTrue(streamById is ReadableByteBuffer)
        val bytes = streamById.read()
        assertNotNull(bytes)
        assertEquals("{\"preloaded\":true}", bytes.toString())
        streamById.close()

        prefs.dispose()
    }

    @Test
    fun testWebNetworkingDisallowsTcpAndUdpWhileSupportingAsyncHttpAndSocketIo() = runSuspend {
        val factory = WebNativeFactory()
        GenericUI.activeFactory = factory

        assertFalse(factory.supportsTcp())
        assertFalse(factory.supportsUdp())
        assertFalse(factory.supportsMulticastUdp())

        assertFalse(Network.supportsTcp())
        assertFalse(Network.supportsUdp())
        assertFalse(Network.supportsMulticastUdp())

        assertFailsWith<UnsupportedOperationException> { factory.createTcpSocket() }
        assertFailsWith<UnsupportedOperationException> { factory.createUdpSocket() }
        assertFailsWith<UnsupportedOperationException> { Network.openTcpSocket() }
        assertFailsWith<UnsupportedOperationException> { Network.openTcpSocket("127.0.0.1", 8080) }
        assertFailsWith<UnsupportedOperationException> { Network.openUdpSocket() }

        // Verify WebHttpClientNativeImpl, WebHttpConnectionNativeImpl, WebHttpRequestNativeImpl, WebHttpResponseNativeImpl via HttpClient
        val httpClient = HttpClient()
        httpClient.registerRoute("https://api.studio.web/status") { req ->
            HttpResponse(
                statusCode = 200,
                statusText = "OK",
                headers = mapOf("Content-Type" to "application/json"),
                bodyBytes = """{"status":"online","method":"${req.method.name}"}""".encodeToByteArray(),
                url = req.url
            )
        }
        val httpConn = httpClient.get("https://api.studio.web/status")
        assertTrue(httpConn is WebHttpConnectionNativeImpl)
        assertTrue(httpConn.request is WebHttpRequestNativeImpl)
        assertTrue(httpConn.response is WebHttpResponseNativeImpl)
        assertTrue(httpConn.isSuccessful)
        assertEquals("""{"status":"online","method":"${HttpMethod.GET.name}"}""", httpConn.bodyText)
        assertEquals("""{"status":"online","method":"${HttpMethod.GET.name}"}""", httpConn.read()?.toString())
        httpConn.close()

        // Verify WebBrowserDomBridge.browserFetchHook interception
        WebBrowserDomBridge.browserFetchHook = { req ->
            if (req.url == "https://api.studio.web/fetch-hook") {
                HttpResponse(
                    statusCode = 200,
                    statusText = "OK",
                    bodyBytes = "from-browser-fetch".encodeToByteArray(),
                    url = req.url
                )
            } else {
                null
            }
        }
        assertEquals("from-browser-fetch", httpClient.getText("https://api.studio.web/fetch-hook"))
        httpClient.dispose()

        // Verify WebWebSocketNativeImpl via factory & SocketIoClient
        val rawWs = factory.createWebSocket()
        assertTrue(rawWs is WebWebSocketNativeImpl)
        rawWs.dispose()

        val socketIo = SocketIoClient("https://realtime.studio.web")
        var connected = false
        socketIo.onConnect { connected = true }

        assertTrue(socketIo.connect())
        assertTrue(connected)
        assertTrue(socketIo.isConnected)
        assertTrue(socketIo.emit("workspace:hello", "web-client"))

        val ack = socketIo.emitWithAck("workspace:ping", "payload")
        assertNotNull(ack)
        assertEquals(listOf("ok"), ack)

        socketIo.disconnect()
        assertFalse(socketIo.isConnected)
        socketIo.dispose()
    }
}
