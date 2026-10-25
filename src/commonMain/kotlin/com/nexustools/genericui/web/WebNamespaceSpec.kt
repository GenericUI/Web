package com.nexustools.genericui.web

/**
 * XML Namespace Specification (`xmlns:genericui="https://nexustools.com/ns/genericui"`) and
 * custom `<genericui:<componentname>>` DOM tag registry for the `:web` backend.
 *
 * Defines:
 * - [NAMESPACE_PREFIX] (`"genericui"`) and [NAMESPACE_URI] (`"https://nexustools.com/ns/genericui"`)
 * - [DEFAULT_NAMESPACE_XML_FILENAME] (`"genericui-namespace.xml"`) and [DEFAULT_NAMESPACE_XML]
 * - [DEFAULT_NAMESPACE_CSS] ensuring browsers lay out `genericui\:*` custom elements with their proper
 *   `display: block`, `display: flex`, `display: inline-flex`, `display: inline-block`, or `display: grid` defaults
 * - [CUSTOM_COMPONENT_TAGS] containing all supported `genericui:<componentname>` tags
 * - [inferTagForClassPrefix] mapping component class prefixes to `genericui:<componentname>` tags while
 *   preserving standard HTML tags (`div`, `header`, `footer`, `main`, `section`, `article`, `aside`, `form`, etc.)
 */
object WebNamespaceSpec {

    const val NAMESPACE_PREFIX: String = "genericui"
    const val NAMESPACE_URI: String = "https://nexustools.com/ns/genericui"
    const val NAMESPACE_SPEC_URI: String = "https://nexustools.com/ns/genericui/spec"
    const val DEFAULT_NAMESPACE_XML_FILENAME: String = "genericui-namespace.xml"

    /**
     * Complete set of custom `genericui:<componentname>` element tag names generated on `:web`
     * and emulated by `CssStyleSheet` on non-Web backends.
     */
    val CUSTOM_COMPONENT_TAGS: Set<String> = linkedSetOf(
        "genericui:component",
        // Shell & Window Chrome
        "genericui:menubar",
        "genericui:menu",
        "genericui:menuitem",
        "genericui:checkmenuitem",
        "genericui:radiomenuitem",
        "genericui:menuseparator",
        "genericui:contextmenu",
        "genericui:splitpane",
        "genericui:titlebar",
        "genericui:statusbar",
        "genericui:mdicontainer",
        "genericui:mdidesktop",
        "genericui:mdiwindow",
        // Dockable Studio Workspace
        "genericui:dockworkspace",
        "genericui:dockarea",
        "genericui:dockgroup",
        "genericui:dockpanel",
        "genericui:panelwindow",
        "genericui:toolpalette",
        "genericui:toolpaletteitem",
        "genericui:categorizedtreeview",
        "genericui:colorswatchbox",
        // Layout Containers
        "genericui:container",
        "genericui:box",
        "genericui:row",
        "genericui:col",
        "genericui:flexbox",
        "genericui:hstack",
        "genericui:vstack",
        "genericui:scrollview",
        "genericui:stepscrollview",
        // Lazy Virtualized Collections
        "genericui:lazylist",
        "genericui:lazycolumn",
        "genericui:lazyrow",
        "genericui:lazygrid",
        "genericui:lazygridtree",
        "genericui:treeview",
        // Surface & Grouping Containers
        "genericui:card",
        "genericui:collapsiblepanel",
        "genericui:accordion",
        "genericui:accordionitem",
        "genericui:jumbotron",
        "genericui:well",
        "genericui:listgroup",
        "genericui:listgroupitem",
        // Tabs
        "genericui:tabcontainer",
        "genericui:tab",
        "genericui:closeabletab",
        "genericui:pinnedtab",
        "genericui:badgetab",
        "genericui:editabletab",
        // Navigation
        "genericui:navbar",
        "genericui:nav",
        "genericui:sidenav",
        "genericui:bottomnavigationbar",
        "genericui:breadcrumb",
        "genericui:breadcrumbitem",
        "genericui:pagination",
        "genericui:stepper",
        // Action Controls
        "genericui:dropdown",
        "genericui:buttongroup",
        "genericui:toolbar",
        // Form Groups & Composite Inputs
        "genericui:formgroup",
        "genericui:inputgroup",
        "genericui:radiogroup",
        "genericui:compoundunitinput",
        "genericui:filedropzone",
        // Rich Typography & Content
        "genericui:markdownview",
        "genericui:avatar",
        "genericui:masterdetailstepselect",
        "genericui:horizontalcarouselselector",
        "genericui:currencypursebar",
        // Feedback & Progress Indicators
        "genericui:alert",
        "genericui:banner",
        "genericui:toast",
        "genericui:toastcontainer",
        "genericui:progressbar",
        "genericui:spinner",
        "genericui:skeletonplaceholder",
        // Overlays & Modals
        "genericui:modal",
        "genericui:drawer",
        "genericui:bottomsheet",
        "genericui:popover",
        "genericui:tooltip",
        "genericui:backdrop",
        // Dialog Windows
        "genericui:dialogwindow"
    )

    val ALL_CUSTOM_TAGS: Set<String>
        get() = CUSTOM_COMPONENT_TAGS

    /**
     * Standard built-in HTML container and semantic element tags preserved when passed explicitly.
     */
    val STANDARD_HTML_CONTAINER_TAGS: Set<String> = linkedSetOf(
        "div",
        "span",
        "header",
        "footer",
        "main",
        "section",
        "article",
        "aside",
        "nav",
        "form",
        "fieldset",
        "figure",
        "figcaption",
        "details",
        "summary",
        "ul",
        "ol",
        "li",
        "table",
        "thead",
        "tbody",
        "tfoot",
        "tr",
        "th",
        "td",
        "p",
        "a",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "pre",
        "code",
        "blockquote",
        "hr",
        "br"
    )

    /**
     * Default CSS rules injected into `<style id="genericui-namespace-styles">` so browsers
     * apply the proper block, flex, inline-flex, inline-block, or grid display behavior to
     * custom `genericui:<componentname>` elements before component classes/styles override them.
     */
    const val DEFAULT_NAMESPACE_CSS: String = """genericui\:component, genericui\:container, genericui\:box, genericui\:col, genericui\:scrollview, genericui\:stepscrollview, genericui\:lazylist, genericui\:lazycolumn, genericui\:lazygrid, genericui\:lazygridtree, genericui\:treeview, genericui\:collapsiblepanel, genericui\:accordion, genericui\:accordionitem, genericui\:jumbotron, genericui\:well, genericui\:listgroupitem, genericui\:tabcontainer, genericui\:formgroup, genericui\:radiogroup, genericui\:markdownview, genericui\:alert, genericui\:toast, genericui\:skeletonplaceholder, genericui\:modal, genericui\:popover, genericui\:backdrop, genericui\:dialogwindow, genericui\:dockpanel, genericui\:panelwindow, genericui\:categorizedtreeview, genericui\:menuitem, genericui\:checkmenuitem, genericui\:radiomenuitem, genericui\:menuseparator, genericui\:contextmenu, genericui\:mdidesktop { display: block; box-sizing: border-box; }
genericui\:menubar, genericui\:splitpane, genericui\:titlebar, genericui\:statusbar, genericui\:mdicontainer, genericui\:mdiwindow, genericui\:dockworkspace, genericui\:dockarea, genericui\:dockgroup, genericui\:row, genericui\:flexbox, genericui\:hstack, genericui\:vstack, genericui\:lazyrow, genericui\:card, genericui\:listgroup, genericui\:navbar, genericui\:nav, genericui\:sidenav, genericui\:bottomnavigationbar, genericui\:breadcrumb, genericui\:pagination, genericui\:stepper, genericui\:toolbar, genericui\:inputgroup, genericui\:compoundunitinput, genericui\:filedropzone, genericui\:masterdetailstepselect, genericui\:horizontalcarouselselector, genericui\:banner, genericui\:toastcontainer, genericui\:progressbar, genericui\:drawer, genericui\:bottomsheet { display: flex; box-sizing: border-box; }
genericui\:toolpaletteitem, genericui\:colorswatchbox, genericui\:breadcrumbitem, genericui\:buttongroup, genericui\:avatar, genericui\:currencypursebar { display: inline-flex; box-sizing: border-box; }
genericui\:menu, genericui\:tab, genericui\:closeabletab, genericui\:pinnedtab, genericui\:badgetab, genericui\:editabletab, genericui\:dropdown, genericui\:spinner, genericui\:tooltip { display: inline-block; box-sizing: border-box; }
genericui\:toolpalette { display: grid; box-sizing: border-box; }"""

    /**
     * Full XML specification matching `src/commonMain/resources/templates/genericui-namespace.xml`.
     */
    const val DEFAULT_NAMESPACE_XML: String = """<?xml version="1.0" encoding="UTF-8"?>
<namespaceSpec xmlns="https://nexustools.com/ns/genericui/spec" prefix="genericui" uri="https://nexustools.com/ns/genericui" version="1.0">
    <commonAttributes>
        <attribute name="id" type="string" description="Unique DOM element identifier."/>
        <attribute name="class" type="string" description="Space-separated Bootstrap 5 and custom CSS class list."/>
        <attribute name="style" type="string" description="Inline CSS property declarations."/>
        <attribute name="title" type="string" description="Tooltip text displayed on hover."/>
        <attribute name="role" type="string" description="WAI-ARIA semantic role."/>
        <attribute name="data-bs-theme" type="string" description="Bootstrap color mode (dark or light)."/>
    </commonAttributes>
    <elements>
        <!-- Core Base Component -->
        <element name="component" tag="genericui:component" componentClass="AbstractComponent" category="core" display="block" role="generic" description="Base GenericUI component element."/>

        <!-- Shell & Window Chrome -->
        <element name="menubar" tag="genericui:menubar" componentClass="MenuBar" category="shell" display="flex" role="menubar" description="Top-level horizontal application menu bar."/>
        <element name="menu" tag="genericui:menu" componentClass="Menu" category="shell" display="inline-block" role="menu" description="Pull-down or cascading submenu container.">
            <attribute name="data-menu-label" type="string" description="Menu header label."/>
        </element>
        <element name="menuitem" tag="genericui:menuitem" componentClass="MenuItem" category="shell" display="block" role="menuitem" description="Executable action item inside a menu or context menu.">
            <attribute name="data-icon" type="string" description="Optional leading icon identifier."/>
            <attribute name="data-shortcut" type="string" description="Keyboard shortcut accelerator label."/>
        </element>
        <element name="checkmenuitem" tag="genericui:checkmenuitem" componentClass="CheckMenuItem" category="shell" display="block" role="menuitemcheckbox" description="Checkable toggle menu item.">
            <attribute name="aria-checked" type="boolean" description="Checked state of the menu item."/>
        </element>
        <element name="radiomenuitem" tag="genericui:radiomenuitem" componentClass="RadioMenuItem" category="shell" display="block" role="menuitemradio" description="Mutually exclusive radio menu item.">
            <attribute name="aria-checked" type="boolean" description="Selected state of the radio menu item."/>
        </element>
        <element name="menuseparator" tag="genericui:menuseparator" componentClass="MenuSeparator" category="shell" display="block" role="separator" description="Visual horizontal divider between menu items."/>
        <element name="contextmenu" tag="genericui:contextmenu" componentClass="ContextMenu" category="shell" display="block" role="menu" description="Floating popup context menu anchored at pointer coordinates."/>
        <element name="splitpane" tag="genericui:splitpane" componentClass="SplitPane" category="shell" display="flex" role="group" description="Resizable two-panel horizontal or vertical split container.">
            <attribute name="data-divider-ratio" type="number" description="Normalized split divider ratio between 0.05 and 0.95."/>
        </element>
        <element name="titlebar" tag="genericui:titlebar" componentClass="TitleBar" category="shell" display="flex" role="banner" description="Custom window title bar with window controls and title."/>
        <element name="statusbar" tag="genericui:statusbar" componentClass="StatusBar" category="shell" display="flex" role="status" description="Bottom application status bar displaying status segments and indicators."/>
        <element name="mdicontainer" tag="genericui:mdicontainer" componentClass="MdiContainer" category="shell" display="flex" role="region" description="Multi-Document Interface (MDI) workspace container."/>
        <element name="mdidesktop" tag="genericui:mdidesktop" componentClass="MdiContainer.DesktopSurface" category="shell" display="block" role="group" description="MDI desktop surface hosting internal MDI child windows."/>
        <element name="mdiwindow" tag="genericui:mdiwindow" componentClass="MdiWindow" category="shell" display="flex" role="dialog" description="Movable, resizable, minimizable, and maximizable internal MDI child window."/>

        <!-- Dockable Studio Workspace (GIMP / Photoshop Style) -->
        <element name="dockworkspace" tag="genericui:dockworkspace" componentClass="DockWorkspace" category="dock" display="flex" role="main" description="Root multi-area dockable studio workspace."/>
        <element name="dockarea" tag="genericui:dockarea" componentClass="DockArea" category="dock" display="flex" role="region" description="Left, right, top, or bottom dockable panel area.">
            <attribute name="data-dock-position" type="string" description="Dock position (left, right, top, bottom, center)."/>
        </element>
        <element name="dockgroup" tag="genericui:dockgroup" componentClass="DockGroup" category="dock" display="flex" role="tablist" description="Tabbed or split stack of dockable panels within a dock area.">
            <attribute name="data-group-mode" type="string" description="Dock group mode (tabbed, vertical_stack, horizontal_split)."/>
        </element>
        <element name="dockpanel" tag="genericui:dockpanel" componentClass="DockPanel" category="dock" display="block" role="tabpanel" description="Individual dockable studio tool or inspector panel.">
            <attribute name="data-panel-title" type="string" description="Display title of the dock panel."/>
        </element>
        <element name="panelwindow" tag="genericui:panelwindow" componentClass="PanelWindow" category="dock" display="block" role="dialog" description="Floating utility panel window detached from a dock area.">
            <attribute name="data-dock-window-title" type="string" description="Title of the floating panel window."/>
        </element>
        <element name="toolpalette" tag="genericui:toolpalette" componentClass="ToolPalette" category="dock" display="grid" role="toolbar" description="Multi-column icon tool palette with foreground/background color swatches."/>
        <element name="toolpaletteitem" tag="genericui:toolpaletteitem" componentClass="ToolPaletteItem" category="dock" display="inline-flex" role="button" description="Selectable tool button inside a tool palette."/>
        <element name="categorizedtreeview" tag="genericui:categorizedtreeview" componentClass="CategorizedTreeView" category="dock" display="block" role="tree" description="Categorizedsearchable hierarchy tree view for studio presets and assets."/>
        <element name="colorswatchbox" tag="genericui:colorswatchbox" componentClass="ColorSwatchBox" category="dock" display="inline-flex" role="group" description="Foreground and background color swatch selector box."/>

        <!-- Layout Containers -->
        <element name="container" tag="genericui:container" componentClass="Container" category="layout" display="block" role="region" description="Responsive layout container."/>
        <element name="box" tag="genericui:box" componentClass="Box" category="layout" display="block" role="group" description="Single-purpose positioning and alignment box container."/>
        <element name="row" tag="genericui:row" componentClass="Row" category="layout" display="flex" role="group" description="12-column responsive Bootstrap grid row."/>
        <element name="col" tag="genericui:col" componentClass="Col" category="layout" display="block" role="group" description="Responsive grid column within a genericui:row."/>
        <element name="flexbox" tag="genericui:flexbox" componentClass="FlexBox" category="layout" display="flex" role="group" description="Configurable CSS Flexbox layout container."/>
        <element name="hstack" tag="genericui:hstack" componentClass="HStack" category="layout" display="flex" role="group" description="Horizontal flex stack layout container."/>
        <element name="vstack" tag="genericui:vstack" componentClass="VStack" category="layout" display="flex" role="group" description="Vertical flex stack layout container."/>
        <element name="scrollview" tag="genericui:scrollview" componentClass="ScrollView" category="layout" display="block" role="region" description="Scrollable viewport container."/>
        <element name="stepscrollview" tag="genericui:stepscrollview" componentClass="StepScrollView" category="layout" display="block" role="region" description="Discrete step-by-step scrollable container."/>

        <!-- Lazy Virtualized Collections -->
        <element name="lazylist" tag="genericui:lazylist" componentClass="LazyList" category="lazy" display="block" role="list" description="Virtualized scrolling list mounting only visible item DOM nodes."/>
        <element name="lazycolumn" tag="genericui:lazycolumn" componentClass="LazyColumn" category="lazy" display="block" role="list" description="Vertical virtualized scrolling list."/>
        <element name="lazyrow" tag="genericui:lazyrow" componentClass="LazyRow" category="lazy" display="flex" role="list" description="Horizontal virtualized scrolling list."/>
        <element name="lazygrid" tag="genericui:lazygrid" componentClass="LazyGrid" category="lazy" display="block" role="grid" description="Multi-column virtualized scrolling grid."/>
        <element name="lazygridtree" tag="genericui:lazygridtree" componentClass="LazyGridTree" category="lazy" display="block" role="treegrid" description="Virtualized multi-column collapsible tree-grid supporting text and component cells."/>
        <element name="treeview" tag="genericui:treeview" componentClass="TreeView" category="lazy" display="block" role="tree" description="Virtualized hierarchical tree view with expandable nodes."/>

        <!-- Surface & Grouping Containers -->
        <element name="card" tag="genericui:card" componentClass="Card" category="surface" display="flex" role="region" description="Elevated surface card with optional header, body, and footer."/>
        <element name="collapsiblepanel" tag="genericui:collapsiblepanel" componentClass="CollapsiblePanel" category="surface" display="block" role="region" description="Expandable and collapsible content panel.">
            <attribute name="data-collapse-title" type="string" description="Header title of the collapsible panel."/>
        </element>
        <element name="accordion" tag="genericui:accordion" componentClass="Accordion" category="surface" display="block" role="region" description="Vertically stacked group of collapsible accordion items."/>
        <element name="accordionitem" tag="genericui:accordionitem" componentClass="AccordionItem" category="surface" display="block" role="region" description="Individual collapsible section within an accordion."/>
        <element name="jumbotron" tag="genericui:jumbotron" componentClass="Jumbotron" category="surface" display="block" role="region" description="Hero showcase surface container."/>
        <element name="well" tag="genericui:well" componentClass="Well" category="surface" display="block" role="region" description="Inset bordered surface container."/>
        <element name="listgroup" tag="genericui:listgroup" componentClass="ListGroup" category="surface" display="flex" role="list" description="Bordered vertical list group of interactive or static items."/>
        <element name="listgroupitem" tag="genericui:listgroupitem" componentClass="ListGroupItem" category="surface" display="block" role="listitem" description="Individual item within a genericui:listgroup."/>

        <!-- Tabs -->
        <element name="tabcontainer" tag="genericui:tabcontainer" componentClass="TabContainer" category="tabs" display="block" role="region" description="Multi-tab container with a navigation tab strip and tab content panes."/>
        <element name="tab" tag="genericui:tab" componentClass="Tab" category="tabs" display="inline-block" role="presentation" description="Standard tab header item within a genericui:tabcontainer.">
            <attribute name="data-tab-id" type="string" description="Unique identifier of the tab."/>
        </element>
        <element name="closeabletab" tag="genericui:closeabletab" componentClass="CloseableTab" category="tabs" display="inline-block" role="presentation" description="Tab header item with an interactive close button.">
            <attribute name="data-tab-id" type="string" description="Unique identifier of the closeable tab."/>
        </element>
        <element name="pinnedtab" tag="genericui:pinnedtab" componentClass="PinnedTab" category="tabs" display="inline-block" role="presentation" description="Non-closeable pinned tab header item."/>
        <element name="badgetab" tag="genericui:badgetab" componentClass="BadgeTab" category="tabs" display="inline-block" role="presentation" description="Tab header item with an attached status/counter badge."/>
        <element name="editabletab" tag="genericui:editabletab" componentClass="EditableTab" category="tabs" display="inline-block" role="presentation" description="Tab header item supporting inline title renaming."/>

        <!-- Navigation -->
        <element name="navbar" tag="genericui:navbar" componentClass="Navbar" category="navigation" display="flex" role="navigation" description="Responsive top navigation bar with brand title and navigation links.">
            <attribute name="data-brand-title" type="string" description="Application or page brand title."/>
        </element>
        <element name="nav" tag="genericui:nav" componentClass="Nav" category="navigation" display="flex" role="navigation" description="Horizontal or vertical navigation link group."/>
        <element name="sidenav" tag="genericui:sidenav" componentClass="SideNav" category="navigation" display="flex" role="navigation" description="Vertical sidebar navigation panel."/>
        <element name="bottomnavigationbar" tag="genericui:bottomnavigationbar" componentClass="BottomNavigationBar" category="navigation" display="flex" role="navigation" description="Mobile/compact bottom navigation bar with icon/label destinations."/>
        <element name="breadcrumb" tag="genericui:breadcrumb" componentClass="Breadcrumb" category="navigation" display="flex" role="navigation" description="Hierarchical page location breadcrumb trail."/>
        <element name="breadcrumbitem" tag="genericui:breadcrumbitem" componentClass="BreadcrumbItem" category="navigation" display="inline-flex" role="listitem" description="Individual step link inside a genericui:breadcrumb."/>
        <element name="pagination" tag="genericui:pagination" componentClass="Pagination" category="navigation" display="flex" role="navigation" description="Page number navigation bar."/>
        <element name="stepper" tag="genericui:stepper" componentClass="Stepper" category="navigation" display="flex" role="group" description="Multi-step wizard progress and navigation indicator."/>

        <!-- Action Controls -->
        <element name="dropdown" tag="genericui:dropdown" componentClass="DropdownButton" category="action" display="inline-block" role="group" description="Button that toggles a floating action selection menu.">
            <attribute name="data-dropdown-label" type="string" description="Button label of the dropdown."/>
            <attribute name="data-item-count" type="number" description="Number of selectable dropdown items."/>
        </element>
        <element name="buttongroup" tag="genericui:buttongroup" componentClass="ButtonGroup" category="action" display="inline-flex" role="group" description="Segmented horizontal or vertical group of buttons."/>
        <element name="toolbar" tag="genericui:toolbar" componentClass="Toolbar" category="action" display="flex" role="toolbar" description="Action toolbar grouping multiple button groups and controls."/>

        <!-- Form Groups & Composite Inputs -->
        <element name="formgroup" tag="genericui:formgroup" componentClass="FormGroup" category="form" display="block" role="group" description="Labeled form field group with validation feedback and help text."/>
        <element name="inputgroup" tag="genericui:inputgroup" componentClass="InputGroup" category="form" display="flex" role="group" description="Composite input control with attached prefix/suffix addons or buttons."/>
        <element name="radiogroup" tag="genericui:radiogroup" componentClass="RadioGroup" category="form" display="block" role="radiogroup" description="Mutually exclusive group of radio button options."/>
        <element name="compoundunitinput" tag="genericui:compoundunitinput" componentClass="CompoundUnitInput" category="form" display="flex" role="group" description="Numeric input paired with a unit selector (e.g., px, %, em, deg)."/>
        <element name="filedropzone" tag="genericui:filedropzone" componentClass="FileDropZone" category="form" display="flex" role="region" description="Drag-and-drop file upload target zone."/>

        <!-- Rich Typography & Content -->
        <element name="markdownview" tag="genericui:markdownview" componentClass="MarkdownView" category="typography" display="block" role="document" description="Formatted Markdown document viewer."/>
        <element name="avatar" tag="genericui:avatar" componentClass="Avatar" category="content" display="inline-flex" role="img" description="Circular or rounded user/entity avatar with image or initials fallback."/>
        <element name="masterdetailstepselect" tag="genericui:masterdetailstepselect" componentClass="MasterDetailStepSelect" category="content" display="flex" role="region" description="Two-pane master category and detail item selector."/>
        <element name="horizontalcarouselselector" tag="genericui:horizontalcarouselselector" componentClass="HorizontalCarouselSelector" category="content" display="flex" role="region" description="Horizontal card carousel with previous/next selection controls."/>
        <element name="currencypursebar" tag="genericui:currencypursebar" componentClass="CurrencyPurseBar" category="content" display="inline-flex" role="status" description="Multi-denomination currency or resource balance bar."/>

        <!-- Feedback & Progress Indicators -->
        <element name="alert" tag="genericui:alert" componentClass="Alert" category="feedback" display="block" role="alert" description="Contextual feedback alert banner."/>
        <element name="banner" tag="genericui:banner" componentClass="Banner" category="feedback" display="flex" role="banner" description="Full-width top announcement or warning banner."/>
        <element name="toast" tag="genericui:toast" componentClass="Toast" category="feedback" display="block" role="status" description="Transient non-modal notification toast."/>
        <element name="toastcontainer" tag="genericui:toastcontainer" componentClass="ToastContainer" category="feedback" display="flex" role="region" description="Viewport-anchored stack container for transient toasts."/>
        <element name="progressbar" tag="genericui:progressbar" componentClass="ProgressBar" category="feedback" display="flex" role="progressbar" description="Determinate or indeterminate horizontal progress bar.">
            <attribute name="aria-valuenow" type="number" description="Current progress percentage (0 to 100)."/>
        </element>
        <element name="spinner" tag="genericui:spinner" componentClass="Spinner" category="feedback" display="inline-block" role="status" description="Animated border or grow loading spinner.">
            <attribute name="aria-label" type="string" description="Accessible loading label."/>
        </element>
        <element name="skeletonplaceholder" tag="genericui:skeletonplaceholder" componentClass="SkeletonPlaceholder" category="feedback" display="block" role="status" description="Shimmering content placeholder shown while data is loading."/>

        <!-- Overlays & Modals -->
        <element name="modal" tag="genericui:modal" componentClass="Modal" category="overlay" display="block" role="dialog" description="Modal overlay container with header, body, and action footer."/>
        <element name="drawer" tag="genericui:drawer" componentClass="Drawer" category="overlay" display="flex" role="dialog" description="Slide-out offcanvas edge drawer."/>
        <element name="bottomsheet" tag="genericui:bottomsheet" componentClass="BottomSheet" category="overlay" display="flex" role="dialog" description="Bottom-anchored slide-up sheet overlay."/>
        <element name="popover" tag="genericui:popover" componentClass="Popover" category="overlay" display="block" role="tooltip" description="Anchored floating popover card.">
            <attribute name="data-popover-title" type="string" description="Optional popover header title."/>
            <attribute name="data-bs-placement" type="string" description="Anchor placement (top, bottom, left, right)."/>
        </element>
        <element name="tooltip" tag="genericui:tooltip" componentClass="Tooltip" category="overlay" display="inline-block" role="tooltip" description="Floating text tooltip anchored to a target element."/>
        <element name="backdrop" tag="genericui:backdrop" componentClass="Backdrop" category="overlay" display="block" role="presentation" description="Dimmed modal or drawer background scrim."/>

        <!-- Dialog Windows -->
        <element name="dialogwindow" tag="genericui:dialogwindow" componentClass="DialogWindow" category="dialog" display="block" role="dialog" description="Modal or non-modal dialog window.">
            <attribute name="data-dialog-title" type="string" description="Dialog window title."/>
            <attribute name="data-presentation-style" type="string" description="Dialog presentation style (window_modal, pull_down_sheet, fullscreen)."/>
            <attribute name="aria-modal" type="boolean" description="Whether the dialog blocks interaction with background content."/>
        </element>
    </elements>
</namespaceSpec>"""

    /**
     * SHA-1 hash of [DEFAULT_NAMESPACE_XML] used for `{{NAMESPACE_XML_URL}}?{{NAMESPACE_XML_HASH}}`.
     */
    val defaultNamespaceXmlSha1Hash: String by lazy {
        WebSha1Hasher.sha1Hex(DEFAULT_NAMESPACE_XML)
    }

    /**
     * Infers the `:web` DOM element tag name for a container given its [classPrefix] and
     * optional [explicitTagName].
     *
     * - If [explicitTagName] is non-blank and is not the generic `"genericui:container"` placeholder,
     *   returns [explicitTagName] directly (preserving standard HTML tags like `"div"`, `"header"`,
     *   `"footer"`, `"main"`, `"section"`, `"article"`, `"aside"`, `"form"`, `"fieldset"`, `"figure"`,
     *   `"details"`, `"ul"`, `"ol"`, `"li"`, `"thead"`, `"tbody"`, `"tfoot"`, `"tr"`, `"th"`, `"td"`,
     *   as well as explicit custom tags like `"genericui:box"`, `"genericui:hstack"`, `"genericui:vstack"`).
     * - Otherwise maps [classPrefix] to its corresponding `"genericui:<componentname>"` tag, defaulting
     *   to `"genericui:container"`.
     */
    fun inferTagForClassPrefix(classPrefix: String, explicitTagName: String? = null): String {
        val cleanExplicit = explicitTagName?.trim()?.lowercase().orEmpty()
        if (cleanExplicit.isNotEmpty() && cleanExplicit != "genericui:container") {
            return cleanExplicit
        }

        val cleanPrefix = classPrefix.trim().lowercase()
        if (cleanPrefix.isEmpty()) {
            return if (cleanExplicit.isNotEmpty()) cleanExplicit else "genericui:container"
        }

        if (cleanPrefix.startsWith("genericui:")) {
            return cleanPrefix
        }

        mapSingleClassToken(cleanPrefix)?.let { return it }

        val tokens = cleanPrefix.split(Regex("\\s+")).filter { it.isNotEmpty() }
        // Check multi-token specificity first (e.g., "offcanvas offcanvas-bottom", "d-flex flex-column")
        if ("mdi-desktop-surface" in tokens) return "genericui:mdidesktop"
        if ("mdi-container" in tokens) return "genericui:mdicontainer"
        if ("mdi-window" in tokens) return "genericui:mdiwindow"
        if ("offcanvas-bottom" in tokens) return "genericui:bottomsheet"
        if ("offcanvas" in tokens) return "genericui:drawer"
        if ("hstack" in tokens) return "genericui:hstack"
        if ("vstack" in tokens) return "genericui:vstack"
        if ("list-group-item" in tokens) return "genericui:listgroupitem"
        if ("list-group" in tokens) return "genericui:listgroup"
        if ("breadcrumb-item" in tokens) return "genericui:breadcrumbitem"
        if ("breadcrumb" in tokens) return "genericui:breadcrumb"
        if ("toast-container" in tokens) return "genericui:toastcontainer"
        if ("toast" in tokens) return "genericui:toast"
        if ("accordion-item" in tokens) return "genericui:accordionitem"
        if ("accordion" in tokens) return "genericui:accordion"
        if ("modal-backdrop" in tokens) return "genericui:backdrop"
        if ("modal" in tokens) return "genericui:modal"

        for (token in tokens) {
            mapSingleClassToken(token)?.let { return it }
        }

        return "genericui:container"
    }

    private fun mapSingleClassToken(token: String): String? {
        if (token == "col" || token.startsWith("col-")) {
            return "genericui:col"
        }
        return when (token) {
            "container", "container-fluid", "container-sm", "container-md", "container-lg", "container-xl", "container-xxl" -> "genericui:container"
            "box" -> "genericui:box"
            "row" -> "genericui:row"
            "d-flex", "d-inline-flex", "flexbox" -> "genericui:flexbox"
            "hstack" -> "genericui:hstack"
            "vstack" -> "genericui:vstack"
            "scrollview", "scroll-view", "overflow-auto", "overflow-scroll" -> "genericui:scrollview"
            "stepscrollview", "step-scroll-view" -> "genericui:stepscrollview"
            "lazylist", "lazy-list" -> "genericui:lazylist"
            "lazycolumn", "lazy-column" -> "genericui:lazycolumn"
            "lazyrow", "lazy-row" -> "genericui:lazyrow"
            "lazygrid", "lazy-grid" -> "genericui:lazygrid"
            "lazygridtree", "lazy-grid-tree" -> "genericui:lazygridtree"
            "treeview", "tree-view" -> "genericui:treeview"
            "card" -> "genericui:card"
            "collapsiblepanel", "collapsible-panel", "collapse" -> "genericui:collapsiblepanel"
            "accordion" -> "genericui:accordion"
            "accordionitem", "accordion-item" -> "genericui:accordionitem"
            "jumbotron" -> "genericui:jumbotron"
            "well" -> "genericui:well"
            "listgroup", "list-group" -> "genericui:listgroup"
            "listgroupitem", "list-group-item" -> "genericui:listgroupitem"
            "tabcontainer", "tab-container" -> "genericui:tabcontainer"
            "tab", "nav-item" -> "genericui:tab"
            "closeabletab", "closeable-tab" -> "genericui:closeabletab"
            "pinnedtab", "pinned-tab" -> "genericui:pinnedtab"
            "badgetab", "badge-tab" -> "genericui:badgetab"
            "editabletab", "editable-tab" -> "genericui:editabletab"
            "navbar" -> "genericui:navbar"
            "nav" -> "genericui:nav"
            "sidenav", "side-nav" -> "genericui:sidenav"
            "bottomnavigationbar", "bottom-navigation-bar", "navbar-bottom" -> "genericui:bottomnavigationbar"
            "breadcrumb" -> "genericui:breadcrumb"
            "breadcrumbitem", "breadcrumb-item" -> "genericui:breadcrumbitem"
            "pagination" -> "genericui:pagination"
            "stepper" -> "genericui:stepper"
            "dropdown" -> "genericui:dropdown"
            "buttongroup", "btn-group", "btn-group-vertical" -> "genericui:buttongroup"
            "toolbar", "btn-toolbar" -> "genericui:toolbar"
            "formgroup", "form-group", "mb-3" -> "genericui:formgroup"
            "inputgroup", "input-group" -> "genericui:inputgroup"
            "radiogroup", "radio-group" -> "genericui:radiogroup"
            "compoundunitinput", "compound-unit-input" -> "genericui:compoundunitinput"
            "filedropzone", "file-drop-zone" -> "genericui:filedropzone"
            "markdownview", "markdown-view", "markdown-body" -> "genericui:markdownview"
            "avatar" -> "genericui:avatar"
            "masterdetailstepselect", "master-detail-step-select" -> "genericui:masterdetailstepselect"
            "horizontalcarouselselector", "horizontal-carousel-selector" -> "genericui:horizontalcarouselselector"
            "currencypursebar", "currency-purse-bar" -> "genericui:currencypursebar"
            "alert" -> "genericui:alert"
            "banner" -> "genericui:banner"
            "toast" -> "genericui:toast"
            "toastcontainer", "toast-container" -> "genericui:toastcontainer"
            "progressbar", "progress" -> "genericui:progressbar"
            "spinner", "spinner-border", "spinner-grow" -> "genericui:spinner"
            "skeletonplaceholder", "skeleton-placeholder", "placeholder-glow", "placeholder-wave" -> "genericui:skeletonplaceholder"
            "modal" -> "genericui:modal"
            "drawer", "offcanvas" -> "genericui:drawer"
            "bottomsheet", "bottom-sheet", "offcanvas-bottom" -> "genericui:bottomsheet"
            "popover" -> "genericui:popover"
            "tooltip" -> "genericui:tooltip"
            "backdrop", "modal-backdrop" -> "genericui:backdrop"
            "dialogwindow", "dialog-window" -> "genericui:dialogwindow"
            "menubar", "menu-bar" -> "genericui:menubar"
            "menu" -> "genericui:menu"
            "menuitem", "menu-item", "dropdown-item" -> "genericui:menuitem"
            "checkmenuitem", "check-menu-item" -> "genericui:checkmenuitem"
            "radiomenuitem", "radio-menu-item" -> "genericui:radiomenuitem"
            "menuseparator", "menu-separator" -> "genericui:menuseparator"
            "contextmenu", "context-menu", "dropdown-menu" -> "genericui:contextmenu"
            "splitpane", "split-pane" -> "genericui:splitpane"
            "titlebar", "title-bar" -> "genericui:titlebar"
            "statusbar", "status-bar" -> "genericui:statusbar"
            "mdicontainer", "mdi-container" -> "genericui:mdicontainer"
            "mdidesktop", "mdi-desktop", "mdi-desktop-surface" -> "genericui:mdidesktop"
            "mdiwindow", "mdi-window" -> "genericui:mdiwindow"
            "dockworkspace", "dock-workspace" -> "genericui:dockworkspace"
            "dockarea", "dock-area" -> "genericui:dockarea"
            "dockgroup", "dock-group" -> "genericui:dockgroup"
            "dockpanel", "dock-panel" -> "genericui:dockpanel"
            "panelwindow", "panel-window", "dock-window" -> "genericui:panelwindow"
            "toolpalette", "tool-palette" -> "genericui:toolpalette"
            "toolpaletteitem", "tool-palette-item" -> "genericui:toolpaletteitem"
            "categorizedtreeview", "categorized-tree-view" -> "genericui:categorizedtreeview"
            "colorswatchbox", "color-swatch-box" -> "genericui:colorswatchbox"
            else -> null
        }
    }
}
