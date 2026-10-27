# `:web` Backend Architecture (`jsMain` & `wasmJsMain`)

## Overview
`:web` implements the complete GenericUI widget, container, dialog, window, styling, navigation history, and system service contract for modern web browsers using Kotlin/Wasm (`wasmJsMain`) and Kotlin/JS (`jsMain`). It depends on `:common` and `:systemCommon`.

---

## Source Set Layout

```text
web/
├── src/commonMain/kotlin/com/nexustools/genericui/web/
│   ├── WebLauncher.kt                  # Launcher invoked by generated jsMain/wasmJsMain entry points
│   ├── WebNativeBootstrap.kt           # Provider registration + WebFragmentHistoryBridge setup
│   ├── WebDomElement.kt                # DOM abstraction separating userClassList and enumClassList + !important styles
│   ├── WebStyleSheetManager.kt         # Manages <link rel="stylesheet" href="..."> in document.head
│   ├── WebFragmentHistoryBridge.kt     # #hash + window.history.state integration for FragmentHostFragment
│   ├── WebBrowserDomBridge.kt          # Expect/actual bridge to browser DOM, History, matchMedia, and Storage APIs
│   ├── WebDomMountEngine.kt            # Reconciles virtual DOM nodes and mounts into #genericui-root
│   ├── WebBaseNativeImpl.kt            # Base web widget (ComponentStyle, ComponentBootstrap, CssCustomCursor)
│   ├── WebBaseContainerNativeImpl.kt   # Base web container
│   ├── WebWindowImpl.kt                # Browser viewport window + 4-state Navbar/MenuBar chrome
│   ├── WebNavbarImpl.kt                # Bootstrap 5 <nav class="navbar"> renderer
│   ├── widget/*                        # Bootstrap 5 widget implementations (Button, TextInput, Slider, etc.)
│   ├── container/*                     # Flexbox/Grid/Tab/Modal/Drawer/SplitPane/LazyList/LazyGrid containers
│   ├── dialog/*                        # Bootstrap Modal dialogs + <input type="file"> file browser
│   └── system/*                        # WebResourceResolverImpl, WebPreferenceStoreImpl, WebClipboardImpl, etc.
├── src/jsMain/kotlin/                  # Kotlin/JS actuals for WebBrowserDomBridge
└── src/wasmJsMain/kotlin/              # Kotlin/Wasm actuals for WebBrowserDomBridge
```

---

## Strict User CSS Class Isolation (`WebDomElement`) & Styling Architecture

| Feature | Implementation in `:web` |
|---------|--------------------------|
| **User CSS Class Isolation** | `WebDomElement` stores user classes in `userClassList` and `ComponentBootstrap` enum classes in `enumClassList`. `addClass`, `removeClass`, `hasClass`, `getClasses()`, `getClass()`, and `className` inspect/modify **only** `userClassList`. `getEffectiveDomClassName()` combines `enumClassList + userClassList` solely when writing the DOM `class` attribute. |
| **`ComponentBootstrap` (`getBootstrap()`)** | Maps `BootstrapType` (`BootstrapType.NONE` default), `BootstrapSize`, `BootstrapWidth`, `BootstrapHeight`, `SpacingScale`, `FlexDirection`, `JustifyContent`, `AlignItems`, `BorderRadiusStyle`, `ShadowElevation`, etc. into `WebDomElement.enumClassList`. |
| **`ComponentStyle` (`getStyle()`) & `!important`** | Maps all standard W3C CSS properties (`backgroundColor`, `color`, `accentColor` / `accent-color`, `border*`, `margin*`, `padding*`, `width`/`height`, `flex*`, `gap`, `transform`, `transition`, `filter`, `backdropFilter`, etc.) to `WebDomElement.setStyle(property, value, important)`. |
| **Custom Cursors (`CssCustomCursor`)** | Translates `CssCustomCursor` (`ResourceID`, SVG, or `ImageBuffer` + `hotspotX`, `hotspotY`, `fallback`) into `url("...") <hotspotX> <hotspotY>, <fallback>` on `WebDomElement.style["cursor"]`. |
| **Stylesheets (`WebStyleSheetManager`)** | `IApplication.addStyleSheet` / `removeStyleSheet` (accepting `String` path, `ResourceID` `R.style.<name>`, or `PrecompiledStyleSheet`) normalizes `.scss`/`.sass` paths to `.css` and adds/removes `<link rel="stylesheet" href="...">` in `document.head`. |
| **Theming (`ThemeMode.AUTO` Default)** | `IApplication.setTheme` / `getTheme` / `getResolvedTheme` defaults to `ThemeMode.AUTO`, listens to `window.matchMedia("(prefers-color-scheme: dark)")`, and sets `document.documentElement.setAttribute("data-bs-theme", "light" \| "dark")`. |

---

## `FragmentHostFragment` Browser History Integration (`WebFragmentHistoryBridge`)

`WebFragmentHistoryBridge` connects `FragmentHostFragment` directly to the browser's URL `#hash` and `window.history.state`:

| Operation | Browser API Invoked | Behavior |
|-----------|---------------------|----------|
| `FragmentHostFragment.navigate(fragment, args, tag)` | `window.history.pushState(json, "", "#$tag")` | Updates URL hash to `#<tag>` and saves `args` + `savedState` JSON in `window.history.state` |
| `FragmentHostFragment.replace(fragment, args, tag)` | `window.history.replaceState(json, "", "#$tag")` | Replaces current URL hash and history state entry without pushing a new back-stack entry |
| `FragmentHostFragment.saveCurrentState(fragmentId, args, savedState)` | `window.history.replaceState(json, "", "#$fragmentId")` | Persists updated `onSaveState(outState)` bundle into the active `window.history.state` entry |
| Browser Back / Forward (`popstate` event) | `window.addEventListener("popstate", ...)` | Deserializes `FragmentHistoryState` from `event.state` (or `window.location.hash`) and restores the fragment + `onRestoreState(savedState)` |

---

## 4-State Top Chrome Rendering (`WebWindowImpl` & `WebNavbarImpl`)

Because the browser itself owns the window titlebar and tab bar, `WebWindowImpl.titleCapabilities` reports `isTitleBarRequired = false`, `titleBarRendersAsNavbar = true`, `menuBarRendersAsNavbar = false`, and `menuBarMergesIntoTitleNavbar = true`:

| `window.titleBar` | `window.menuBar` | Rendered DOM Top Chrome |
|-------------------|------------------|-------------------------|
| `null` | `null` | None (zero top chrome; root container occupies 100% of viewport height) |
| `null` | `MenuBar` | `<nav class="genericui-menubar">` horizontal menu bar |
| `TitleBar` | `null` | `<nav class="navbar navbar-expand">` with icon, title, subtitle, leading, center, and trailing components |
| `TitleBar` | `MenuBar` | Single merged `<nav class="navbar navbar-expand">` with `TitleBar` (icon, title, leading, center) on the left and `MenuBar` dropdowns + trailing components on the right |

All custom components attached to `TitleBar` (`leadingComponents`, `centerComponents`, `trailingComponents`) have `mountedInTitleBar = true` set on their `WebBaseNativeImpl` so `WebDomMountEngine` renders them inside `<nav class="navbar">` and filters them out of the window body container.
