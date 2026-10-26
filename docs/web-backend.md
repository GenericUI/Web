# `:web` Backend Architecture (`jsMain` & `wasmJsMain`)

## Overview
`:web` implements the complete GenericUI widget, container, dialog, window, navigation history, and system service contract for modern web browsers using Kotlin/JS (`jsMain`) and Kotlin/Wasm (`wasmJsMain`). It depends on `:common` and `:systemCommon`.

---

## Source Set Layout

```text
web/
├── src/commonMain/kotlin/com/nexustools/genericui/web/
│   ├── WebLauncher.kt                  # Entry point invoked by generated jsMain/wasmJsMain launchers
│   ├── WebNativeBootstrap.kt           # Provider registration + WebFragmentHistoryBridge setup
│   ├── WebFragmentHistoryBridge.kt     # #hash + window.history.state integration for FragmentHostFragment
│   ├── WebBrowserDomBridge.kt          # Expect/actual bridge to browser DOM, History, and Storage APIs
│   ├── WebDomMountEngine.kt            # Reconciles virtual DOM nodes and mounts into #genericui-root
│   ├── WebBaseNativeImpl.kt            # Base web widget (DOM tag, classes, inline styles, mountedInTitleBar)
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

---

## System Services (`com.nexustools.genericui.web.system`)

- **`WebResourceResolverImpl`**: Maps `ResourceId` entries to `"assets/${resId.type.directoryName}/$fileName"` URLs and resolves localized strings, colors, dimensions, and raw bytes.
- **`WebPreferenceStoreImpl`**: Persists key-value and typed `@PreferenceObject` preferences in `window.localStorage` using `SystemStorageKeyHelper` (`<appId>:<key>`).
- **`WebKeystoreImpl`**: Web Crypto API (`SubtleCrypto` AES-GCM) encrypted storage backed by `IndexedDB` / `localStorage`.
- **`WebClipboardImpl`**: Browser `navigator.clipboard.readText()` / `writeText()`.
- **`WebNetworkMonitorImpl`**: Browser `navigator.onLine` and `online` / `offline` window events.
- **`WebNotificationManagerImpl`**: HTML5 `Notification` API (`Notification.requestPermission()`).
