package com.nexustools.genericui.web

import com.nexustools.genericui.component.system.PreferenceChangeListener
import com.nexustools.genericui.component.system.PreferenceElement
import com.nexustools.genericui.component.system.PreferenceObjectCodec
import com.nexustools.genericui.nativeimpl.system.PreferenceStoreNativeImpl

/**
 * Web (`js` + `wasmJs`) implementation of [PreferenceStoreNativeImpl] backed by `window.localStorage`
 * and the browser's native `window.addEventListener("storage", ...)` event.
 *
 * Each preference `key` is stored **directly** as a `localStorage` key (`localStorage.getItem(key)` /
 * `localStorage.setItem(key, json)`) without any key prefixing, with values serialized as JSON via
 * [PreferenceObjectCodec].
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
        WebBrowserDomBridge.registerStorageListener { key, newValue ->
            if (isDisposed) return@registerStorageListener
            if (key == null) {
                notifyListeners(null, null)
            } else if (newValue == null) {
                notifyListeners(key, null)
            } else {
                val parsed = runCatching { PreferenceObjectCodec.fromJson(newValue) }
                    .getOrDefault(newValue)
                notifyListeners(key, parsed)
            }
        }
    }

    override fun writeElement(key: String, element: PreferenceElement) {
        if (element.isNull) {
            remove(key)
            return
        }
        val json = PreferenceObjectCodec.elementToJson(element)
        fallbackStorage[key] = json
        WebBrowserDomBridge.localStorageSetItem(key, json)
        notifyListeners(key, element.toPlainValue())
    }

    override fun readElement(key: String): PreferenceElement? {
        val raw = WebBrowserDomBridge.localStorageGetItem(key) ?: fallbackStorage[key] ?: return null
        return runCatching { PreferenceObjectCodec.fromJsonElement(raw) }
            .getOrElse { PreferenceElement.ofString(raw) }
    }

    override fun contains(key: String): Boolean =
        WebBrowserDomBridge.localStorageGetItem(key) != null || fallbackStorage.containsKey(key)

    override fun keys(): Set<String> {
        val result = LinkedHashSet<String>()
        result.addAll(WebBrowserDomBridge.localStorageKeys())
        result.addAll(fallbackStorage.keys)
        return result
    }

    override fun remove(key: String) {
        val existed = contains(key)
        fallbackStorage.remove(key)
        WebBrowserDomBridge.localStorageRemoveItem(key)
        if (existed) {
            notifyListeners(key, null)
        }
    }

    override fun clear() {
        val hadAny = keys().isNotEmpty()
        fallbackStorage.clear()
        WebBrowserDomBridge.localStorageClear()
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
}
