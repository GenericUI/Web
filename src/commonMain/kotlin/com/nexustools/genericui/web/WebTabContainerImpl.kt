package com.nexustools.genericui.web

import com.nexustools.genericui.component.tabs.TabBarStyle
import com.nexustools.genericui.component.tabs.TabPosition
import com.nexustools.genericui.core.NativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabContainerNativeImpl
import com.nexustools.genericui.nativeimpl.tabs.TabNativeImpl

/**
 * Web implementation of [TabContainerNativeImpl] rendered as `<genericui:tabcontainer class="tab-container">`
 * using Bootstrap `.nav-tabs` + `.tab-pane.fade`, mounting each [WebTabImpl] into the `<ul class="nav nav-tabs">`
 * header strip and each content pane into `<div class="tab-content">`.
 */
class WebTabContainerImpl : WebBaseContainerNativeImpl("genericui:tabcontainer", "tab-container d-flex flex-column"), TabContainerNativeImpl<WebDomElement> {
    override val supportsNativeFadeTransition: Boolean = true

    val navListElement: WebDomElement = WebDomElement("ul").apply {
        classList.add("nav")
        classList.add("nav-tabs")
        setAttribute("role", "tablist")
    }
    val tabContentElement: WebDomElement = WebDomElement("div").apply {
        classList.add("tab-content")
        classList.add("flex-grow-1")
    }

    var currentTabPosition: TabPosition = TabPosition.TOP
        private set

    private val tabImpls = mutableListOf<TabNativeImpl<*>?>()
    private val tabPanes = mutableListOf<Pair<String, NativeImpl<*>?>>()
    private val buttonTabFlags = mutableListOf<Boolean>()
    private var selectedIndex: Int = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var onTabCloseRequestedListener: ((Int) -> Boolean)? = null

    init {
        nativeWidget.classList.add(TabPosition.TOP.cssClass)
        nativeWidget.appendChild(navListElement)
        nativeWidget.appendChild(tabContentElement)
    }

    override fun addChildNative(childImpl: NativeImpl<*>, index: Int) {
        // TabContainer manages child mounting into navListElement and tabContentElement via addTabNative.
    }

    override fun removeChildNative(childImpl: NativeImpl<*>) {
        // Managed by removeTabNative.
    }

    override fun clearChildrenNative() {
        navListElement.clearChildren()
        tabContentElement.clearChildren()
        tabImpls.clear()
        tabPanes.clear()
        buttonTabFlags.clear()
    }

    private fun syncPaneDomVisibility() {
        tabImpls.forEachIndexed { i, tabImpl ->
            val isBtn = buttonTabFlags.getOrElse(i) { false }
            tabImpl?.setSelectedNative(i == selectedIndex && !isBtn)
        }
        tabPanes.forEachIndexed { i, (_, impl) ->
            val dom = impl?.nativeWidget as? WebDomElement ?: return@forEachIndexed
            val isBtn = buttonTabFlags.getOrElse(i) { false }
            val isActive = (i == selectedIndex) && !isBtn
            if (isActive) {
                dom.classList.add("active")
                dom.classList.add("show")
                dom.classList.remove("d-none")
                if (dom.style["display"] == "none") {
                    dom.style.remove("display")
                }
            } else {
                dom.classList.remove("active")
                dom.classList.remove("show")
                dom.classList.add("d-none")
                dom.style["display"] = "none"
            }
        }
    }

    override fun addTabNative(tabImpl: TabNativeImpl<*>, contentImpl: NativeImpl<*>, index: Int) {
        val webTab = tabImpl as? WebTabImpl
        val title = webTab?.titleVal ?: ""
        val tabDom = tabImpl.nativeWidget as? WebDomElement
        val contentDom = contentImpl.nativeWidget as? WebDomElement
        contentDom?.classList?.add("tab-pane")
        contentDom?.classList?.add("fade")

        val existingIdx = tabImpls.indexOfFirst { it === tabImpl }
        if (existingIdx >= 0) {
            tabPanes[existingIdx] = title to contentImpl
            buttonTabFlags[existingIdx] = false
            if (tabDom != null && tabDom.parent !== navListElement) {
                navListElement.appendChild(tabDom)
            }
            if (contentDom != null && contentDom.parent !== tabContentElement) {
                tabContentElement.appendChild(contentDom)
            }
            syncPaneDomVisibility()
            return
        }

        if (index < 0 || index >= tabPanes.size) {
            tabImpls.add(tabImpl)
            tabPanes.add(title to contentImpl)
            buttonTabFlags.add(false)
            if (tabDom != null) navListElement.appendChild(tabDom)
            if (contentDom != null) tabContentElement.appendChild(contentDom)
            webTab?.buttonElement?.addEventListener("click") {
                val currentIdx = tabImpls.indexOf(tabImpl)
                if (currentIdx >= 0) setSelectedTabIndex(currentIdx, true)
            }
        } else {
            tabImpls.add(index, tabImpl)
            tabPanes.add(index, title to contentImpl)
            buttonTabFlags.add(index, false)
            if (tabDom != null) navListElement.insertChildAt(index, tabDom)
            if (contentDom != null) {
                val contentInsertIdx = tabPanes.take(index).count { it.second != null }
                tabContentElement.insertChildAt(contentInsertIdx, contentDom)
            }
            webTab?.buttonElement?.addEventListener("click") {
                val currentIdx = tabImpls.indexOf(tabImpl)
                if (currentIdx >= 0) setSelectedTabIndex(currentIdx, true)
            }
        }
        if (selectedIndex !in tabPanes.indices || buttonTabFlags.getOrElse(selectedIndex) { false }) {
            val firstSelectable = buttonTabFlags.indexOfFirst { !it }
            if (firstSelectable >= 0) {
                selectedIndex = firstSelectable
            }
        }
        syncPaneDomVisibility()
    }

    override fun addButtonTabNative(tabImpl: TabNativeImpl<*>, index: Int) {
        val webTab = tabImpl as? WebTabImpl
        val title = webTab?.titleVal ?: ""
        val tabDom = tabImpl.nativeWidget as? WebDomElement
        webTab?.buttonElement?.apply {
            setAttribute("role", "button")
            classList.remove("active")
            setAttribute("aria-selected", "false")
        }

        val existingIdx = tabImpls.indexOfFirst { it === tabImpl }
        if (existingIdx >= 0) {
            tabPanes[existingIdx] = title to null
            buttonTabFlags[existingIdx] = true
            if (tabDom != null && tabDom.parent !== navListElement) {
                navListElement.appendChild(tabDom)
            }
            syncPaneDomVisibility()
            return
        }

        if (index < 0 || index >= tabPanes.size) {
            tabImpls.add(tabImpl)
            tabPanes.add(title to null)
            buttonTabFlags.add(true)
            if (tabDom != null) navListElement.appendChild(tabDom)
        } else {
            tabImpls.add(index, tabImpl)
            tabPanes.add(index, title to null)
            buttonTabFlags.add(index, true)
            if (tabDom != null) navListElement.insertChildAt(index, tabDom)
        }
        webTab?.buttonElement?.addEventListener("click") {
            webTab.setSelectedNative(false)
            if (webTab.hasClickListener()) {
                webTab.dispatchClick()
            } else {
                val currentIdx = tabImpls.indexOf(tabImpl)
                if (currentIdx >= 0) {
                    onTabSelectedListener?.invoke(currentIdx)
                }
            }
        }
        syncPaneDomVisibility()
    }

    override fun addTabNative(title: String, contentImpl: NativeImpl<*>, index: Int) {
        val syntheticTab = WebTabImpl(false).apply { setTitleNative(title) }
        addTabNative(syntheticTab, contentImpl, index)
    }

    override fun updateTabNative(index: Int, tabImpl: TabNativeImpl<*>) {
        if (index in tabImpls.indices) {
            tabImpls[index] = tabImpl
            val title = (tabImpl as? WebTabImpl)?.titleVal ?: tabPanes[index].first
            tabPanes[index] = title to tabPanes[index].second
        }
    }

    override fun removeTabNative(index: Int) {
        if (index !in tabPanes.indices) return
        val removedTab = tabImpls.removeAt(index)
        (removedTab?.nativeWidget as? WebDomElement)?.let { navListElement.removeChild(it) }
        val (_, impl) = tabPanes.removeAt(index)
        buttonTabFlags.removeAt(index)
        (impl?.nativeWidget as? WebDomElement)?.let { tabContentElement.removeChild(it) }
        if (tabPanes.isNotEmpty()) {
            if (selectedIndex >= tabPanes.size) {
                selectedIndex = (tabPanes.size - 1).coerceAtLeast(0)
            }
            if (buttonTabFlags.getOrElse(selectedIndex) { false }) {
                val firstSelectable = buttonTabFlags.indexOfFirst { !it }
                if (firstSelectable >= 0) {
                    selectedIndex = firstSelectable
                }
            }
        } else {
            selectedIndex = 0
        }
        syncPaneDomVisibility()
    }

    override fun setTabTitleNative(index: Int, title: String) {
        if (index !in tabPanes.indices) return
        tabPanes[index] = title to tabPanes[index].second
        tabImpls[index]?.setTitleNative(title)
    }

    override fun setTabPositionNative(position: TabPosition) {
        currentTabPosition = position
        TabPosition.entries.forEach { nativeWidget.classList.remove(it.cssClass) }
        nativeWidget.classList.add(position.cssClass)
        if (position == TabPosition.BOTTOM) {
            navListElement.classList.add("mt-auto")
            navListElement.classList.add("border-top")
            navListElement.classList.add("border-bottom-0")
            if (nativeWidget.children.firstOrNull() !== tabContentElement) {
                nativeWidget.insertChildAt(0, tabContentElement)
            }
            if (nativeWidget.children.lastOrNull() !== navListElement) {
                nativeWidget.appendChild(navListElement)
            }
        } else {
            navListElement.classList.remove("mt-auto")
            navListElement.classList.remove("border-top")
            navListElement.classList.remove("border-bottom-0")
            if (nativeWidget.children.firstOrNull() !== navListElement) {
                nativeWidget.insertChildAt(0, navListElement)
            }
            if (nativeWidget.children.lastOrNull() !== tabContentElement) {
                nativeWidget.appendChild(tabContentElement)
            }
        }
    }

    override fun setTabBarStyleNative(style: TabBarStyle) {
        TabBarStyle.entries.forEach { navListElement.classList.remove(it.cssClass) }
        navListElement.classList.add(style.cssClass)
    }

    override fun setSelectedTabIndex(index: Int, fadeTransition: Boolean) {
        if (index !in tabPanes.indices) return
        if (buttonTabFlags.getOrElse(index) { false }) {
            val webTab = tabImpls.getOrNull(index) as? WebTabImpl
            webTab?.setSelectedNative(false)
            if (webTab != null && webTab.hasClickListener()) {
                webTab.dispatchClick()
            } else {
                onTabSelectedListener?.invoke(index)
            }
            syncPaneDomVisibility()
            return
        }
        val changed = (selectedIndex != index)
        selectedIndex = index
        syncPaneDomVisibility()
        if (changed) {
            onTabSelectedListener?.invoke(index)
        }
    }

    override fun getSelectedTabIndex(): Int = selectedIndex

    override fun setOnTabSelected(listener: ((Int) -> Unit)?) {
        onTabSelectedListener = listener
    }

    override fun setOnTabCloseRequested(listener: ((Int) -> Boolean)?) {
        onTabCloseRequestedListener = listener
    }
}
