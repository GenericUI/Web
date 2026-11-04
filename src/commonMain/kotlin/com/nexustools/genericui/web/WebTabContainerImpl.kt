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
    private val tabPanes = mutableListOf<Pair<String, NativeImpl<*>>>()
    private var selectedIndex: Int = 0
    private var onTabSelectedListener: ((Int) -> Unit)? = null
    private var onTabCloseRequestedListener: ((Int) -> Boolean)? = null

    init {
        nativeWidget.classList.add(TabPosition.TOP.cssClass)
        nativeWidget.appendChild(navListElement)
        nativeWidget.appendChild(tabContentElement)
    }

    override fun addChildNative(child: NativeImpl<*>, index: Int) {
        // TabContainer manages child mounting into navListElement and tabContentElement via addTabNative.
    }

    override fun removeChildNative(child: NativeImpl<*>) {
        // Managed by removeTabNative.
    }

    override fun clearChildrenNative() {
        navListElement.clearChildren()
        tabContentElement.clearChildren()
        tabImpls.clear()
        tabPanes.clear()
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
            if (tabDom != null && tabDom.parent !== navListElement) {
                navListElement.appendChild(tabDom)
            }
            if (contentDom != null && contentDom.parent !== tabContentElement) {
                tabContentElement.appendChild(contentDom)
            }
            return
        }

        if (index < 0 || index >= tabPanes.size) {
            val targetIdx = tabPanes.size
            tabImpls.add(tabImpl)
            tabPanes.add(title to contentImpl)
            if (tabDom != null) navListElement.appendChild(tabDom)
            if (contentDom != null) tabContentElement.appendChild(contentDom)
            webTab?.buttonElement?.addEventListener("click") {
                val currentIdx = tabImpls.indexOf(tabImpl)
                if (currentIdx >= 0) setSelectedTabIndex(currentIdx, true)
            }
            if (targetIdx == 0) {
                setSelectedTabIndex(0, false)
            }
        } else {
            tabImpls.add(index, tabImpl)
            tabPanes.add(index, title to contentImpl)
            if (tabDom != null) navListElement.insertChildAt(index, tabDom)
            if (contentDom != null) tabContentElement.insertChildAt(index, contentDom)
            webTab?.buttonElement?.addEventListener("click") {
                val currentIdx = tabImpls.indexOf(tabImpl)
                if (currentIdx >= 0) setSelectedTabIndex(currentIdx, true)
            }
        }
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
        (impl.nativeWidget as? WebDomElement)?.let { tabContentElement.removeChild(it) }
        if (selectedIndex >= tabPanes.size) {
            selectedIndex = (tabPanes.size - 1).coerceAtLeast(0)
        }
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
        val changed = (selectedIndex != index)
        selectedIndex = index
        tabImpls.forEachIndexed { i, tabImpl ->
            tabImpl?.setSelectedNative(i == index)
        }
        tabPanes.forEachIndexed { i, (_, impl) ->
            val dom = impl.nativeWidget as? WebDomElement ?: return@forEachIndexed
            if (i == index) {
                dom.classList.add("active")
                dom.classList.add("show")
            } else {
                dom.classList.remove("active")
                dom.classList.remove("show")
            }
        }
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
