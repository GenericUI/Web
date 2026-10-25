# `:web` — HTML5 DOM Widgets, Lazy Windowing, File Picker, & Web Canvases

> **Parent Submodule**: [`:web` (`../README.md`)](../README.md) | **Root Repository**: [**GenericUI (`../../README.md`)**](../../README.md)  
> **Root Docs**: [SourceSet Hierarchy](../../docs/sourcesets.md) · [Backends Guide](../../docs/backends.md) · [Extensions Guide](../../docs/extensions.md) · [Gradle Plugin](../../docs/gradle-plugin.md) · [UI Testing](../../docs/testing.md)  
> **Companion Web Guide**: [**Dual Wasm/JS Loader & Bootstrap Splash Screen (`./wasm-js-loader-and-splash.md`)**](./wasm-js-loader-and-splash.md)

The **`:web`** module (`com.nexustools.genericui.web.*`) implements GenericUI's `NativeBackendFactory` (`WebNativeFactory`) directly atop semantic **HTML5 DOM elements**, **Bootstrap 5 CSS/JS**, **HTML5 2D Canvas / WebGL2 / WebGPU**, and a zero-overhead **`WebBrowserDomBridge`** compiled for both **Kotlin/Wasm (`wasmJsMain`)** and **Kotlin/JS IR (`jsMain`)**.

---

## Core Architecture & Native Web Capabilities

### 1. Semantic HTML5 DOM Widgets & Dual-Mode Bootstrap 5 Styling (`WebNativeFactory`, `WebBaseNativeImpl`, `WebDomElement`)
Unlike canvas-emulated web UI toolkits that render text and buttons inside a single opaque `<canvas>` (breaking browser accessibility, text selection, password managers, and DevTools inspection), `:web` maps every GenericUI component to a true, semantic HTML5 DOM element:

| GenericUI Component | `:web` Peer Class | HTML5 DOM Element & Bootstrap 5 Classes |
| :--- | :--- | :--- |
| `Button` | `WebButtonImpl` | `<button type="button" class="btn btn-primary ...">` |
| `TextInput` | `WebTextInputImpl` | `<input class="form-control ...">` / `<textarea class="form-control ...">` |
| `Select` | `WebSelectImpl` | `<select class="form-select ..."><option value="...">...</option></select>` |
| `CheckBox` / Switch | `WebCheckboxImpl` | `<div class="form-check"><input type="checkbox" class="form-check-input">...</div>` |
| `Slider` / `RangeSlider` | `WebRangeSliderImpl` | `<input type="range" class="form-range">` |
| `ProgressBar` | `WebProgressBarImpl` | `<div class="progress"><div class="progress-bar ..."></div></div>` |
| `Container` | `WebContainerImpl` | `<div class="d-flex ...">` |
| `SplitPane` | `WebSplitPaneImpl` | `<div class="genericui-split-pane ...">` with interactive splitter handle |
| `TabContainer` | `WebTabContainerImpl` | `<ul class="nav nav-tabs">...</ul>` + `<div class="tab-content">...</div>` |
| `DataTable` / `Table` | `WebTableImpl` | `<table class="table table-hover ..."><thead>...</thead><tbody>...</tbody></table>` |
| `Dialog` | `WebDialogWindowImpl` | `<div class="modal fade show" role="dialog">...</div>` |
| `TitleBar` (Instruction) | `WebWindowImpl` / `WebNavbarImpl` | Updates `document.title` and mounts `<nav class="navbar navbar-expand ...">` |

- **Dual-Mode Styling**:
  1. **Bootstrap 5 Enums**: Calling `button.setType(BootstrapType.PRIMARY)`, `button.setSize(BootstrapSize.LG)`, or `container.setPaddingAll(SpacingScale.P_3)` synchronizes directly with `WebDomElement.classList` (`btn-primary`, `btn-lg`, `p-3`) via `BootstrapClassMapper`.
  2. **Compiled SASS/CSS Cascade**: `WebBaseNativeImpl.applyComputedStyle(style)` applies resolved CSS properties, `LinearGradient` / `RadialGradient` (`linear-gradient(...)` / `radial-gradient(...)`), box shadows, transforms, and native CSS transitions (`style["transition"]`).

---

### 2. Invisible `<input type="file">` File & Folder Browser (`WebFileBrowserDialogImpl`)

Browsers restrict programmatic filesystem dialogs to `<input type="file">` elements triggered from user interaction. `WebFileBrowserDialogImpl` provides a seamless, zero-visual-artifact implementation of `FileBrowserDialogNativeImpl`:

1. **Completely Invisible & Un-Interactable DOM Element**:
   Creates a temporary `<input type="file">` element with:
   - `tabindex="-1"` and `aria-hidden="true"` (cannot be tabbed to or read by screen readers)
   - `display: none; position: fixed; left: -9999px; top: -9999px; width: 0px; height: 0px; opacity: 0; pointer-events: none; visibility: hidden`
2. **All `FileBrowserMode` Modes Supported**:
   - **`FileBrowserMode.SELECT_FOLDER`**: Sets `webkitdirectory = true` and `directory = true` (`<input type="file" webkitdirectory="true" directory="true">`) so the browser opens its native directory picker.
   - **`FileBrowserMode.OPEN_MULTIPLE_FILES`**: Sets `multiple = true` (`<input type="file" multiple="true">`).
   - **`FileBrowserMode.OPEN_SINGLE_FILE` / `SAVE_FILE`**: Sets `multiple = false`.
3. **`accept` Attribute & Post-Selection Validation**:
   - Translates `request.filters` (`FileTypeFilter`) into a standard HTML `accept` string via `FileTypeFilter.toWebAcceptAttribute(request.filters)` (e.g. `".png,.jpg,.webp,image/png,image/jpeg"`).
   - Performs post-selection validation (`matchesFilters(file, request.filters)`) when the `change` event fires, handles `cancel` and `window-focus-fallback` events when the user dismisses the dialog, and immediately removes the temporary `<input>` from the DOM (`cleanupActiveInput()`).

---

### 3. True DOM-Windowed Lazy Views (`WebLazyListImpl`, `WebLazyGridImpl`, `WebTreeViewImpl`)

Mounting thousands of DOM nodes freezes browser layout engines. GenericUI's `:web` lazy views implement true DOM windowing:

- **`WebLazyListImpl`**:
  - Maintains three child containers inside the scrollable `<div class="lazy-list">`:
    1. `topSpacer` (`<div class="lazy-list-spacer-start">`) sized to `firstVisibleIndex * estimatedItemExtentPx`
    2. `viewportHost` (`<div class="lazy-list-viewport">`) containing **only** the DOM elements for `firstVisibleIndex .. lastVisibleIndex`
    3. `bottomSpacer` (`<div class="lazy-list-spacer-end">`) sized to `(totalItemCount - 1 - lastVisibleIndex) * estimatedItemExtentPx`
  - On every `scroll` event, `recomputeVisibleWindow()` unmounts and disposes (`viewportHost.removeChild` + `impl.dispose()`) any item that scrolled out of `[firstVisibleIndex .. lastVisibleIndex]` and mounts newly visible items with `data-lazy-index="$idx"`.
- **`WebLazyGridImpl`** & **`WebTreeViewImpl`**:
  - Apply the same viewport-windowing algorithm to multi-column CSS Grid layouts and flattened hierarchical tree nodes.

---

### 4. HTML5 `Canvas2D`, `WebGL2`, & `WebGPU` with `requestAnimationFrame` (`deltaMs`)

- **VBlank Synchronization (`WebBrowserDomBridge`)**:
  - `AbstractCanvas.registerVBlankRender { deltaMs -> ... }` hooks directly into the browser's `window.requestAnimationFrame(timestamp)` loop, passing the exact high-resolution `DOMHighResTimeStamp` delta (`deltaMs: Double`) between display frames.
- **`Canvas2D` (`:extensions:canvas2d:web`)**:
  - Uses `<canvas>` `CanvasRenderingContext2D` with automatic `window.devicePixelRatio` HiDPI backing store scaling.
- **`GLCanvas` (`:extensions:canvas3d:web` + `:extensions:gpu:web`)**:
  - Uses **WebGL 2.0** (`WebGL2RenderingContext`), which maps 1:1 to GenericUI's OpenGL ES 3.0 (`#version 300 es`) shader pipeline.
- **`VKCanvas` (`:extensions:canvas3d:web` + `:extensions:gpu:web`)**:
  - Uses **WebGPU** (`navigator.gpu` / `GPUCanvasContext`) with WebGL 2.0 fallback.
- **Native Browser Image Codec (`WebBrowserImageCodecImpl`)**:
  - Decodes and encodes `ImageBuffer` instances using browser-native `createImageBitmap`, `OffscreenCanvas` / `<canvas>`, and `ImageData`.

---

## SourceSet Hierarchy (`common` $\rightarrow$ `web` / `webMain` / `webTest`)

When `web()` is enabled in `genericui { backends { ... } }`, the Gradle plugin configures both `wasmJs` and `js(IR)` targets and creates the unified **`web`** sourceSet tier:

```
commonMain / commonTest
 └── webMain / webTest (:web)
      ├── wasmJsMain / wasmJsTest (Kotlin/Wasm — Default Primary Bundle)
      └── jsMain / jsTest (Kotlin/JS IR — Automatic Fallback Bundle)
```

- Place all shared web UI, DOM, and browser logic in **`src/webMain/kotlin`** and **`src/webTest/kotlin`**.
- Only drop down to `src/wasmJsMain/kotlin` or `src/jsMain/kotlin` if you are writing low-level `external` JS interop declarations that differ between Kotlin/Wasm and Kotlin/JS.

---

## Code Examples

### 1. Initializing & Running a Web Application (`WebNativeFactory`)

```kotlin
package com.example.webapp

import com.nexustools.genericui.component.action.Button
import com.nexustools.genericui.component.action.TitleBar
import com.nexustools.genericui.component.container.Container
import com.nexustools.genericui.component.data.LazyList
import com.nexustools.genericui.component.text.Text
import com.nexustools.genericui.component.shell.Application
import com.nexustools.genericui.core.GenericUI
import com.nexustools.genericui.enums.bootstrap.BootstrapType
import com.nexustools.genericui.enums.bootstrap.FlexDirection
import com.nexustools.genericui.enums.bootstrap.SpacingScale
import com.nexustools.genericui.web.WebNativeFactory

class StudioWebApp : Application(title = "Nexus Web Studio", width = 1280, height = 800) {
    override fun onCreate() {
        // Updates document.title and renders a responsive Bootstrap 5 <nav class="navbar"> header:
        mainWindow.setTitleBar(
            TitleBar(title = "Nexus Web Studio", subtitle = "Kotlin/Wasm + JS IR Fallback")
        )

        val root = Container().apply {
            setFlexDirection(FlexDirection.COLUMN)
            setPaddingAll(SpacingScale.P_3)

            addChild(
                Button("Import Folder from Browser").apply {
                    id = "import-folder-btn"
                    setType(BootstrapType.PRIMARY)
                }
            )

            // True DOM-windowed LazyList: only visible rows exist in the DOM!
            addChild(
                LazyList().apply {
                    id = "asset-lazy-list"
                    setItems(count = 25_000) { index ->
                        Text("Vector & Raster Asset #$index")
                    }
                }
            )
        }
        mainWindow.setContent(root)
    }
}

fun main() {
    GenericUI.initialize(WebNativeFactory())
    StudioWebApp().run(GenericUI.requireFactory())
}
```

### 2. Using `FileBrowserDialog` on `:web` (`WebFileBrowserDialogImpl`)

The exact same `FileBrowserDialog` common code works on `:web` using `WebFileBrowserDialogImpl`'s invisible `<input type="file">`:

```kotlin
import com.nexustools.genericui.component.dialog.FileBrowserCallback
import com.nexustools.genericui.component.dialog.FileBrowserDialog
import com.nexustools.genericui.component.dialog.FileBrowserRequest
import com.nexustools.genericui.component.dialog.FileTypeFilter
import com.nexustools.genericui.component.dialog.SelectedFile
import com.nexustools.genericui.enums.dialog.FileBrowserMode

// 1. Select a folder in the browser (creates invisible <input type="file" webkitdirectory="true">):
fun pickWebFolder(dialog: FileBrowserDialog) {
    dialog.show(
        request = FileBrowserRequest(
            title = "Select Project Folder",
            mode = FileBrowserMode.SELECT_FOLDER
        ),
        callback = object : FileBrowserCallback {
            override fun onFilesSelected(files: List<SelectedFile>, activeFilter: FileTypeFilter?) {
                println("Browser folder selected (${files.size} entries): ${files.first().pathOrUri}")
            }

            override fun onCancelled() {
                println("Browser folder selection cancelled.")
            }
        }
    )
}

// 2. Select multiple SVG/PNG files (creates invisible <input type="file" multiple="true" accept=".svg,.png,image/svg+xml,image/png">):
fun pickWebArtworkFiles(dialog: FileBrowserDialog) {
    dialog.show(
        request = FileBrowserRequest(
            title = "Upload Artwork",
            mode = FileBrowserMode.OPEN_MULTIPLE_FILES,
            filters = listOf(
                FileTypeFilter(
                    description = "Vector & Raster Images",
                    extensions = listOf("svg", "png", "webp"),
                    mimeTypes = listOf("image/svg+xml", "image/png", "image/webp")
                )
            )
        ),
        callback = object : FileBrowserCallback {
            override fun onFilesSelected(files: List<SelectedFile>, activeFilter: FileTypeFilter?) {
                files.forEach { file ->
                    println("Selected web file: ${file.name} (${file.mimeType}, ${file.sizeBytes} bytes)")
                }
            }

            override fun onCancelled() {}
        }
    )
}
```

---

## Related Documentation

- **Parent Web Overview**: [`../README.md`](../README.md)
- **Companion Web Guide**: [**Dual Wasm/JS Loader, SHA-1 Hashing, & Bootstrap Splash Screen (`./wasm-js-loader-and-splash.md`)**](./wasm-js-loader-and-splash.md)
- **Web UI Testing**: [Platform Test Runners (`WebJestElectricRunnerNativeImpl`)](../../tests/docs/platform-test-runners.md)
- **Root GenericUI Documentation**: [Root README](../../README.md) · [SourceSet Hierarchy](../../docs/sourcesets.md) · [Backends Guide](../../docs/backends.md) · [Extensions Guide](../../docs/extensions.md) · [Gradle Plugin](../../docs/gradle-plugin.md) · [UI Testing](../../docs/testing.md)
- **Sibling Submodules**: [`:desktop`](../../desktop/README.md) · [`:mobile`](../../mobile/README.md) · [`:tests`](../../tests/README.md) · [`:NJS`](../../lib/NJS/README.md)
