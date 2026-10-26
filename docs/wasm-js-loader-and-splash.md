# Wasm-First / JS Fallback Loader & Generated Splash Screen

## Overview
The GenericUI Gradle plugin (`GenericUiGenerateSplashTask`) automatically generates a zero-configuration `index.html`, `genericui-loader.js`, and CSS splash screen for every `:web` consumer application.

---

## How `GenericUiGenerateSplashTask` Works

During the Gradle build, `GenericUiGenerateSplashTask` reads the `genericui` DSL block from `build.gradle.kts`:

```kotlin
genericui {
    applicationId = "com.example.myapp"
    appName = "My App"
    web {
        splashTitle = "My App"
        splashSubtitle = "Loading WebAssembly UI..."
        accentColor = "#3B82F6"
        backgroundColor = "#0F172A"
    }
}
```

It emits into `build/generated/genericui/webSplash/`:
1. **`index.html`**:
   - Includes Bootstrap 5 CSS (`bootstrap.min.css`, `bootstrap-icons.css`).
   - Renders the `#genericui-splash` overlay with application icon/logo, title, subtitle, and animated progress bar.
   - Provides the `#genericui-root` mount container (`width: 100vw; height: 100vh`).
   - Loads `genericui-loader.js`.
2. **`genericui-loader.js` (Wasm-First with Automatic JS Fallback)**:
   - Detects WebAssembly Garbage Collection (WasmGC) support in the active browser.
   - If WasmGC is supported, streams and instantiates the `wasmJs` bundle (`<module>-wasm-js.js` + `.wasm`) with live download progress reported to `#genericui-splash`.
   - If WasmGC is unavailable, transparently falls back to loading the Kotlin/JS IR bundle (`<module>-js.js`).
   - Once `WebLauncher.launch(...)` mounts the initial `@MainAppFragment` into `#genericui-root`, `WebDomMountEngine` invokes `window.__genericuiHideSplash()` to smoothly fade out and remove the splash overlay.
