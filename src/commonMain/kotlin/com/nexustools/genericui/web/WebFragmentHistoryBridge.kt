package com.nexustools.genericui.web

import com.nexustools.genericui.component.fragment.FragmentBundle
import com.nexustools.genericui.component.fragment.helper.FragmentHistoryBridge
import com.nexustools.genericui.component.system.PreferenceObjectCodec

/**
 * Web (`:web`) implementation of [FragmentHistoryBridge] that synchronizes [Fragment] navigation
 * with the browser URL `#hash` (`window.location.hash`) and HTML5 History API
 * (`window.history.pushState` / `replaceState` / `popstate` / `hashchange`).
 */
class WebFragmentHistoryBridge : FragmentHistoryBridge {

    private var popStateListener: ((route: String, historyIndex: Int?, restoredState: FragmentBundle?) -> Unit)? = null
    private var disposed: Boolean = false

    init {
        WebBrowserDomBridge.registerPopStateListener { routeHash, historyIndex, stateJson ->
            if (!disposed) {
                val restoredBundle = deserializeBundle(stateJson)
                popStateListener?.invoke(routeHash.removePrefix("#"), historyIndex, restoredBundle)
            }
        }
    }

    override val isSupported: Boolean
        get() = true

    override fun currentRouteHash(): String? {
        val raw = WebBrowserDomBridge.getLocationHash().removePrefix("#").trim()
        return raw.ifEmpty { null }
    }

    override fun currentHistoryIndex(): Int? =
        WebBrowserDomBridge.getCurrentHistoryState().first

    override fun currentSavedState(): FragmentBundle? =
        deserializeBundle(WebBrowserDomBridge.getCurrentHistoryState().second)

    override fun pushState(route: String, historyIndex: Int, title: String, state: FragmentBundle?) {
        if (disposed) return
        val cleanRoute = route.removePrefix("#")
        val stateJson = serializeBundle(state)
        WebBrowserDomBridge.historyPushState(cleanRoute, historyIndex, title, stateJson)
    }

    override fun replaceState(route: String, historyIndex: Int, title: String, state: FragmentBundle?) {
        if (disposed) return
        val cleanRoute = route.removePrefix("#")
        val stateJson = serializeBundle(state)
        WebBrowserDomBridge.historyReplaceState(cleanRoute, historyIndex, title, stateJson)
    }

    override fun goBack(): Boolean {
        if (disposed) return false
        return WebBrowserDomBridge.historyBack()
    }

    override fun goForward(): Boolean {
        if (disposed) return false
        return WebBrowserDomBridge.historyForward()
    }

    override fun setOnPopStateListener(
        listener: ((route: String, historyIndex: Int?, restoredState: FragmentBundle?) -> Unit)?
    ) {
        popStateListener = listener
    }

    override fun dispose() {
        disposed = true
        popStateListener = null
    }

    companion object {
        private const val LONG_KEYS_META = "__genericui_long_keys__"

        fun serializeBundle(bundle: FragmentBundle?): String? {
            if (bundle == null) return null
            val rawMap = bundle.toMap()
            if (rawMap.isEmpty()) return "{}"
            val encodedMap = LinkedHashMap<String, Any?>(rawMap.size + 1)
            val longKeys = mutableListOf<String>()
            for ((k, v) in rawMap) {
                if (v is Long) {
                    longKeys.add(k)
                }
                encodedMap[k] = v
            }
            if (longKeys.isNotEmpty()) {
                encodedMap[LONG_KEYS_META] = longKeys
            }
            return PreferenceObjectCodec.toJson(encodedMap)
        }

        @Suppress("UNCHECKED_CAST")
        fun deserializeBundle(json: String?): FragmentBundle? {
            if (json.isNullOrBlank()) return null
            val decoded = runCatching { PreferenceObjectCodec.fromJson(json) }.getOrNull() as? Map<String, Any?>
                ?: return null
            val longKeys = (decoded[LONG_KEYS_META] as? List<*>)?.filterIsInstance<String>()?.toSet().orEmpty()
            val bundle = FragmentBundle()
            for ((k, v) in decoded) {
                if (k == LONG_KEYS_META) continue
                when (v) {
                    is Long -> {
                        if (k in longKeys || v < Int.MIN_VALUE.toLong() || v > Int.MAX_VALUE.toLong()) {
                            bundle.putLong(k, v)
                        } else {
                            bundle.putInt(k, v.toInt())
                        }
                    }
                    is Int -> bundle.putInt(k, v)
                    is Double -> bundle.putDouble(k, v)
                    is Boolean -> bundle.putBoolean(k, v)
                    is String -> bundle.putString(k, v)
                    else -> bundle.put(k, v)
                }
            }
            return bundle
        }
    }
}
