# GenericUI — Web Submodule (`:web`)

> **Main Repository**: [**GenericUI/GenericUI**](https://github.com/GenericUI/GenericUI)

This repository is the **Web Backend Submodule (`:web`)** of the [**GenericUI**](https://github.com/GenericUI/GenericUI) Kotlin Multiplatform Native-First UI framework, targeting **Kotlin/JS** (`js(IR)`) and **Kotlin/Wasm** (`wasmJs`).

---

## Key Features

- **Dual-Mode DOM & Bootstrap 5 Styling (`WebBaseNativeImpl`)**:
  - Applies CSS classes directly to HTML5 DOM elements (`classList`) while supporting the full `GenericUI` Bootstrap 5 Enum API (`BootstrapType`, `BootstrapSize`, `SpacingScale`), inline styles, `LinearGradient` / `RadialGradient` backgrounds, and native CSS transitions.
- **Zero-Overhead Browser DOM Bridge (`WebBrowserDomBridge`)**:
  - Dedicated `jsMain` and `wasmJsMain` DOM bridges that mount live HTML elements, synchronize events (`click`, `dblclick`, `pointerdown`, `pointermove`, `pointerup`, `wheel`, `keydown`, `keyup`), and drive `requestAnimationFrame` VBlank render loops with high-resolution `deltaMs` timing.
- **Native Browser File & Folder Picker (`WebFileBrowserDialogImpl`)**:
  - Simulates native OS file and directory pickers using hidden `<input type="file">` elements with `multiple`, `webkitdirectory` / `directory` (for `FileBrowserMode.SELECT_FOLDER`), and full `FileTypeFilter` MIME/extension `accept` attribute translation.
- **True Viewport-Windowed Lazy Views (`WebLazyListImpl`, `WebLazyGridImpl`, `WebTreeViewImpl`)**:
  - Dynamically mounts DOM nodes only for items inside the visible scroll window `[firstVisible .. lastVisible]` and unmounts offscreen items using top/bottom scroll spacers.

---

## Usage via the GenericUI Gradle Plugin

Enable the Web backend in your project's `build.gradle.kts` via the [GenericUI Gradle Plugin](https://github.com/GenericUI/GenericUI):

```kotlin
plugins {
    id("com.nexustools.genericui")
}

genericui {
    backends {
        web()
    }
}
```

---

## Related GenericUI Repositories

- **Main Repository**: [https://github.com/GenericUI/GenericUI](https://github.com/GenericUI/GenericUI)
- **Desktop Backends (`:desktop`)**: [https://github.com/GenericUI/Desktop](https://github.com/GenericUI/Desktop)
- **Mobile Backends (`:mobile`)**: [https://github.com/GenericUI/Mobile](https://github.com/GenericUI/Mobile)
- **Extensions (`:extensions`)**: [https://github.com/GenericUI/Extensions](https://github.com/GenericUI/Extensions)
- **UI Testing Suite (`:tests` / `JestElectricRunner`)**: [https://github.com/GenericUI/Tests](https://github.com/GenericUI/Tests)
