package com.nexustools.genericui.web

import com.nexustools.genericui.component.system.PreferenceChangeListener
import com.nexustools.genericui.component.system.PreferenceElement
import com.nexustools.genericui.component.system.PreferenceObjectCodec
import com.nexustools.genericui.nativeimpl.system.PreferenceStoreNativeImpl

/**
 * Web (`js` + `wasmJs`) implementation of [PreferenceStoreNativeImpl] backed by `window.localStorage`
 * and the browser's native `window.addEventListener("storage", ...)` event.
 *
 * Every preference `key` is stored in `window.localStorage` with the [KEY_PREFIX] (`"genericui:"`)
 * prefix (`localStorage.getItem("genericui:<key>")` / `localStorage.setItem("genericui:<key>", json)`)
 * to prevent collisions with third-party JavaScript libraries, plugins, and browser extensions, while
 * exposing unprefixed logical keys through [keys] and [PreferenceChangeListener].
 */
class WebPreferenceStoreImpl(
    override val appName: String = "GenericUI"
) : PreferenceStoreNativeImpl {

    override val storageLocationDescription: String = "window.localStorage"
    override val usesNativeChangeListener: Boolean = true

    override var isDisposed: Boolean = false
        private set

    private val fallbackStorage = LinkedHashMap<String, String>()
    private val listeners = mutableListOf<PreferenceChangeListener>()

    init {
        WebBrowserDomBridge.registerStorageListener { rawKey, newValue ->
            if (isDisposed) return@registerStorageListener
            if (rawKey == null) {
                fallbackStorage.clear()
                notifyListeners(null, null)
                return@registerStorageListener
            }
            if (!rawKey.startsWith(KEY_PREFIX)) {
                return@registerStorageListener
            }
            val logicalKey = fromStorageKey(rawKey)
            if (newValue == null) {
                fallbackStorage.remove(logicalKey)
                notifyListeners(logicalKey, null)
            } else {
                fallbackStorage[logicalKey] = newValue
                val parsed = runCatching { PreferenceObjectCodec.fromJson(newValue) }
                    .getOrDefault(newValue)
                notifyListeners(logicalKey, parsed)
            }
        }
    }

    fun toStorageKey(key: String): String =
        if (key.startsWith(KEY_PREFIX)) key else "$KEY_PREFIX$key"

    fun fromStorageKey(rawKey: String): String =
        rawKey.removePrefix(KEY_PREFIX)

    override fun writeElement(key: String, element: PreferenceElement) {
        val logicalKey = fromStorageKey(key)
        if (element.isNull) {
            remove(logicalKey)
            return
        }
        val json = PreferenceObjectCodec.elementToJson(element)
        fallbackStorage[logicalKey] = json
        WebBrowserDomBridge.localStorageSetItem(toStorageKey(logicalKey), json)
        notifyListeners(logicalKey, element.toPlainValue())
    }

    override fun readElement(key: String): PreferenceElement? {
        val logicalKey = fromStorageKey(key)
        val raw = WebBrowserDomBridge.localStorageGetItem(toStorageKey(logicalKey))
            ?: fallbackStorage[logicalKey]
            ?: return null
        return runCatching { PreferenceObjectCodec.fromJsonElement(raw) }
            .getOrElse { PreferenceElement.ofString(raw) }
    }

    override fun contains(key: String): Boolean {
        val logicalKey = fromStorageKey(key)
        return WebBrowserDomBridge.localStorageGetItem(toStorageKey(logicalKey)) != null ||
            fallbackStorage.containsKey(logicalKey)
    }

    override fun keys(): Set<String> {
        val result = LinkedHashSet<String>()
        for (rawKey in WebBrowserDomBridge.localStorageKeys()) {
            if (rawKey.startsWith(KEY_PREFIX)) {
                val logicalKey = fromStorageKey(rawKey)
                if (logicalKey.isNotEmpty()) {
                    result.add(logicalKey)
                }
            }
        }
        result.addAll(fallbackStorage.keys)
        return result
    }

    override fun remove(key: String) {
        val logicalKey = fromStorageKey(key)
        val existed = contains(logicalKey)
        fallbackStorage.remove(logicalKey)
        WebBrowserDomBridge.localStorageRemoveItem(toStorageKey(logicalKey))
        if (existed) {
            notifyListeners(logicalKey, null)
        }
    }

    override fun clear() {
        val hadAny = keys().isNotEmpty()
        fallbackStorage.clear()
        val prefixedKeys = WebBrowserDomBridge.localStorageKeys().filter { it.startsWith(KEY_PREFIX) }
        for (rawKey in prefixedKeys) {
            WebBrowserDomBridge.localStorageRemoveItem(rawKey)
        }
        if (hadAny) {
            notifyListeners(null, null)
        }
    }

    override fun flush(): Boolean = !isDisposed

    override fun addChangeListener(listener: PreferenceChangeListener) {
        if (listener !in listeners) {
            listeners.add(listener)
        }
    }

    override fun removeChangeListener(listener: PreferenceChangeListener) {
        listeners.remove(listener)
    }

    override fun dispose() {
        isDisposed = true
        listeners.clear()
    }

    private fun notifyListeners(key: String?, newValue: Any?) {
        val snapshot = listeners.toList()
        for (listener in snapshot) {
            listener.onPreferenceChanged(appName, key, newValue)
        }
    }

    companion object {
        const val KEY_PREFIX: String = "genericui:"

        fun toStorageKey(key: String): String =
            if (key.startsWith(KEY_PREFIX)) key else "$KEY_PREFIX$key"

        fun fromStorageKey(rawKey: String): String =
            rawKey.removePrefix(KEY_PREFIX)
    }
}
