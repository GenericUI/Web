package com.nexustools.genericui.web

/**
 * Provides the Wasm-first / JS-IR-fallback loader JavaScript (`genericui-loader.js`) for the `:web` backend.
 *
 * The loader script:
 * 1. Attaches to `#genericui-splash` and `#genericui-splash-progress-bar` if present in the DOM.
 * 2. Reads `<script type="loader-wasm" src="{{WASM_URL}}?{{WASM_HASH}}">` and
 *    `<script type="loader-js" src="{{JS_URL}}?{{JS_HASH}}">`.
 * 3. Detects browser WebAssembly + WasmGC support.
 * 4. Attempts to download and run the compiled Kotlin/Wasm (`loader-wasm`) bundle first as the default,
 *    updating the Bootstrap progress bar while downloading.
 * 5. Automatically falls back to downloading and running the compiled Kotlin/JS IR (`loader-js`) bundle
 *    if the browser does not support Wasm or if loading the Wasm bundle fails.
 * 6. Fades out the splash screen once the Wasm/JS bundle has loaded and started.
 * 7. Displays an error alert with a formatted stacktrace inside `#genericui-splash-error`
 *    if the Wasm/JS bundle throws an error during download or execution.
 */
object WebWasmJsLoader {

    const val DEFAULT_LOADER_FILENAME: String = "genericui-loader.js"

    /**
     * SHA-1 hash of [DEFAULT_LOADER_SCRIPT] used for `{{LOADER_JS_URL}}?{{LOADER_JS_HASH}}`.
     */
    val defaultLoaderSha1Hash: String by lazy {
        WebSha1Hasher.sha1Hex(DEFAULT_LOADER_SCRIPT)
    }

    /**
     * Full source of `src/commonMain/resources/templates/genericui-loader.js`.
     */
    const val DEFAULT_LOADER_SCRIPT: String = """(function (global) {
    "use strict";

    if (global.__GENERICUI_LOADER_ACTIVE__) {
        return;
    }
    global.__GENERICUI_LOADER_ACTIVE__ = true;

    function getDomElements() {
        var doc = global.document;
        if (!doc) {
            return {};
        }
        var splash = doc.getElementById("genericui-splash") || doc.querySelector(".genericui-splash");
        var progressContainer = doc.getElementById("genericui-splash-progress") ||
            (splash ? splash.querySelector(".progress") : null);
        var progressBar = doc.getElementById("genericui-splash-progress-bar") ||
            (progressContainer ? progressContainer.querySelector(".progress-bar") : null);
        var statusLabel = doc.getElementById("genericui-splash-status") ||
            (splash ? splash.querySelector(".genericui-splash-status") : null);
        var errorBox = doc.getElementById("genericui-splash-error") ||
            (splash ? splash.querySelector(".genericui-splash-error") : null);
        var errorTitle = doc.getElementById("genericui-splash-error-title");
        var errorMessage = doc.getElementById("genericui-splash-error-message") ||
            (errorBox ? errorBox.querySelector(".genericui-splash-error-message") : null);
        var errorStack = doc.getElementById("genericui-splash-error-stack") ||
            (errorBox ? errorBox.querySelector("pre") : null);
        var loaderScript = doc.getElementById("genericui-loader-script") || doc.currentScript;
        var wasmDescriptor = doc.querySelector('script[type="loader-wasm"]');
        var jsDescriptor = doc.querySelector('script[type="loader-js"]');

        return {
            doc: doc,
            splash: splash,
            progressContainer: progressContainer,
            progressBar: progressBar,
            statusLabel: statusLabel,
            errorBox: errorBox,
            errorTitle: errorTitle,
            errorMessage: errorMessage,
            errorStack: errorStack,
            loaderScript: loaderScript,
            wasmDescriptor: wasmDescriptor,
            jsDescriptor: jsDescriptor
        };
    }

    var state = {
        hasError: false,
        fadedOut: false,
        currentProgress: 0,
        preloadedWasmBytes: {},
        runtimeTarget: null
    };

    function updateProgress(percent, statusText) {
        if (state.hasError) {
            return;
        }
        var clamped = Math.max(0, Math.min(100, Math.round(percent)));
        if (clamped < state.currentProgress && clamped !== 0) {
            clamped = state.currentProgress;
        }
        state.currentProgress = clamped;

        var els = getDomElements();
        if (els.progressBar) {
            els.progressBar.style.width = clamped + "%";
            els.progressBar.setAttribute("aria-valuenow", String(clamped));
        }
        if (els.progressContainer) {
            els.progressContainer.setAttribute("aria-valuenow", String(clamped));
        }
        if (statusText && els.statusLabel) {
            els.statusLabel.textContent = statusText;
        }
    }

    function formatErrorDetails(err) {
        if (!err) {
            return {
                message: "An unknown application error occurred.",
                stack: "No stacktrace available."
            };
        }
        if (typeof err === "string") {
            return {
                message: err,
                stack: err
            };
        }
        var message = err.message || (err.reason && err.reason.message) || String(err);
        var stack = err.stack ||
            (err.error && err.error.stack) ||
            (err.reason && err.reason.stack) ||
            (err.cause && err.cause.stack) ||
            String(err);
        if (stack && stack.indexOf(message) === -1) {
            stack = message + "\n" + stack;
        }
        return {
            message: message,
            stack: stack
        };
    }

    function showError(err, phaseTitle) {
        state.hasError = true;
        var details = formatErrorDetails(err);
        var els = getDomElements();

        if (global.console && typeof global.console.error === "function") {
            global.console.error("[GenericUI Loader] " + (phaseTitle || "Application Error") + ":", err);
        }

        if (!els.doc) {
            return;
        }

        if (els.splash) {
            els.splash.classList.remove("d-none", "opacity-0");
            els.splash.style.opacity = "1";
            els.splash.style.visibility = "visible";
            els.splash.style.pointerEvents = "auto";
        }

        if (els.progressBar) {
            els.progressBar.classList.remove("progress-bar-animated", "progress-bar-striped");
            els.progressBar.classList.add("bg-danger");
            els.progressBar.style.width = "100%";
        }

        if (els.statusLabel) {
            els.statusLabel.textContent = phaseTitle || "Failed to start application";
            els.statusLabel.classList.remove("text-body-secondary");
            els.statusLabel.classList.add("text-danger", "fw-semibold");
        }

        var errorBox = els.errorBox;
        var errorMessage = els.errorMessage;
        var errorStack = els.errorStack;

        if (!errorBox && els.splash) {
            errorBox = els.doc.createElement("div");
            errorBox.id = "genericui-splash-error";
            errorBox.className = "genericui-splash-error alert alert-danger text-start w-100 mt-3 mb-0 shadow-sm";
            errorBox.setAttribute("role", "alert");

            var headerDiv = els.doc.createElement("div");
            headerDiv.className = "fw-bold mb-1";
            headerDiv.id = "genericui-splash-error-title";
            headerDiv.textContent = phaseTitle || "Application Error";
            errorBox.appendChild(headerDiv);

            errorMessage = els.doc.createElement("div");
            errorMessage.id = "genericui-splash-error-message";
            errorMessage.className = "genericui-splash-error-message small fw-semibold mb-2";
            errorBox.appendChild(errorMessage);

            errorStack = els.doc.createElement("pre");
            errorStack.id = "genericui-splash-error-stack";
            errorStack.className = "genericui-splash-error-stack small mb-0 p-2 bg-dark text-light rounded overflow-auto";
            errorStack.style.maxHeight = "240px";
            errorStack.style.whiteSpace = "pre-wrap";
            errorStack.style.wordBreak = "break-word";
            errorBox.appendChild(errorStack);

            var innerWrapper = els.splash.firstElementChild || els.splash;
            innerWrapper.appendChild(errorBox);
        }

        if (els.errorTitle && phaseTitle) {
            els.errorTitle.textContent = phaseTitle;
        }
        if (errorMessage) {
            errorMessage.textContent = details.message;
        }
        if (errorStack) {
            errorStack.textContent = details.stack;
        }
        if (errorBox) {
            errorBox.classList.remove("d-none");
        }
    }

    function fadeOutSplash(fadeDurationMs) {
        if (state.hasError || state.fadedOut) {
            return;
        }
        state.fadedOut = true;
        updateProgress(100, "Ready");

        var els = getDomElements();
        var splash = els.splash;
        if (!splash) {
            return;
        }
        var duration = typeof fadeDurationMs === "number" && fadeDurationMs >= 0 ? fadeDurationMs : 350;
        splash.style.transition = "opacity " + duration + "ms ease, visibility " + duration + "ms ease";
        splash.classList.add("opacity-0");
        splash.style.opacity = "0";
        splash.style.pointerEvents = "none";

        function removeSplashNode() {
            if (!state.hasError && splash) {
                if (typeof splash.remove === "function") {
                    splash.remove();
                } else if (splash.parentNode) {
                    splash.parentNode.removeChild(splash);
                }
            }
        }

        if (typeof global.setTimeout === "function") {
            global.setTimeout(removeSplashNode, duration + 20);
        } else {
            removeSplashNode();
        }
    }

    function detectWasmSupport() {
        try {
            if (typeof global.WebAssembly !== "object" ||
                typeof global.WebAssembly.instantiate !== "function" ||
                typeof global.WebAssembly.validate !== "function") {
                return false;
            }
            var wasmGcProbe = new Uint8Array([
                0x00, 0x61, 0x73, 0x6d,
                0x01, 0x00, 0x00, 0x00,
                0x01, 0x04, 0x01, 0x5f, 0x00, 0x0b
            ]);
            if (global.WebAssembly.validate(wasmGcProbe)) {
                return true;
            }
            if (global.GenericUiLoaderConfig && global.GenericUiLoaderConfig.allowStandardWasm === true) {
                var basicWasmProbe = new Uint8Array([0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00]);
                return global.WebAssembly.validate(basicWasmProbe);
            }
            return false;
        } catch (_e) {
            return false;
        }
    }

    function downloadWithProgress(url, minPct, maxPct, statusPrefix) {
        updateProgress(minPct, statusPrefix);
        if (typeof global.fetch !== "function") {
            return new Promise(function (resolve, reject) {
                var xhr = new global.XMLHttpRequest();
                xhr.open("GET", url, true);
                xhr.responseType = "arraybuffer";
                xhr.onprogress = function (ev) {
                    if (ev.lengthComputable && ev.total > 0) {
                        var ratio = ev.loaded / ev.total;
                        var pct = minPct + ratio * (maxPct - minPct);
                        updateProgress(pct, statusPrefix + " (" + Math.round(ratio * 100) + "%)");
                    }
                };
                xhr.onload = function () {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        updateProgress(maxPct, statusPrefix);
                        resolve(new Uint8Array(xhr.response));
                    } else {
                        reject(new Error("HTTP " + xhr.status + " while downloading " + url));
                    }
                };
                xhr.onerror = function () {
                    reject(new Error("Network error while downloading " + url));
                };
                xhr.send();
            });
        }

        return global.fetch(url, { credentials: "same-origin" }).then(function (response) {
            if (!response.ok) {
                throw new Error("HTTP " + response.status + " (" + response.statusText + ") while downloading " + url);
            }
            var contentLengthHeader = response.headers ? response.headers.get("Content-Length") : null;
            var totalBytes = contentLengthHeader ? parseInt(contentLengthHeader, 10) : 0;

            if (!response.body || typeof response.body.getReader !== "function") {
                return response.arrayBuffer().then(function (buf) {
                    updateProgress(maxPct, statusPrefix);
                    return new Uint8Array(buf);
                });
            }

            var reader = response.body.getReader();
            var chunks = [];
            var receivedBytes = 0;

            function pump() {
                return reader.read().then(function (result) {
                    if (result.done) {
                        var combined = new Uint8Array(receivedBytes);
                        var offset = 0;
                        for (var i = 0; i < chunks.length; i++) {
                            combined.set(chunks[i], offset);
                            offset += chunks[i].byteLength;
                        }
                        updateProgress(maxPct, statusPrefix);
                        return combined;
                    }
                    var chunk = result.value;
                    chunks.push(chunk);
                    receivedBytes += chunk.byteLength;

                    var fraction = totalBytes > 0
                        ? Math.min(1, receivedBytes / totalBytes)
                        : Math.min(0.95, 1 - Math.exp(-receivedBytes / (512 * 1024)));
                    var pct = minPct + fraction * (maxPct - minPct);
                    var suffix = totalBytes > 0
                        ? " (" + Math.round(fraction * 100) + "%)"
                        : "";
                    updateProgress(pct, statusPrefix + suffix);
                    return pump();
                });
            }

            return pump();
        });
    }

    function registerPreloadedWasmBinary(wasmUrl, wasmBytes) {
        if (!wasmUrl || !wasmBytes) {
            return;
        }
        var cleanUrl = wasmUrl.split("?")[0];
        var cleanName = cleanUrl.split("/").pop();
        state.preloadedWasmBytes[wasmUrl] = wasmBytes;
        state.preloadedWasmBytes[cleanUrl] = wasmBytes;
        if (cleanName) {
            state.preloadedWasmBytes[cleanName] = wasmBytes;
        }

        if (!global.__GENERICUI_WASM_FETCH_HOOKED__ && typeof global.fetch === "function") {
            global.__GENERICUI_WASM_FETCH_HOOKED__ = true;
            var originalFetch = global.fetch.bind(global);
            global.fetch = function (input, init) {
                var reqUrl = typeof input === "string" ? input : (input && input.url ? input.url : "");
                var reqClean = reqUrl.split("?")[0];
                var reqFile = reqClean.split("/").pop();
                var cached = state.preloadedWasmBytes[reqUrl] ||
                    state.preloadedWasmBytes[reqClean] ||
                    (reqFile ? state.preloadedWasmBytes[reqFile] : null);
                if (cached && typeof global.Response === "function") {
                    return Promise.resolve(new global.Response(cached.buffer, {
                        status: 200,
                        headers: { "Content-Type": "application/wasm" }
                    }));
                }
                return originalFetch(input, init);
            };
        }
    }

    function executeDownloadedScript(jsBytes, sourceUrl) {
        return new Promise(function (resolve, reject) {
            try {
                var decoder = typeof global.TextDecoder === "function"
                    ? new global.TextDecoder("utf-8")
                    : null;
                var code = decoder ? decoder.decode(jsBytes) : "";
                var els = getDomElements();
                if (!els.doc) {
                    resolve();
                    return;
                }

                var cleanUrl = sourceUrl.split("?")[0];
                var isEsModule = /\.mjs$/i.test(cleanUrl) ||
                    /(?:^|\n)\s*(?:import|export)\s+/m.test(code);

                if (isEsModule && typeof global.Blob === "function" && global.URL && typeof global.URL.createObjectURL === "function") {
                    var blob = new global.Blob([code], { type: "text/javascript" });
                    var blobUrl = global.URL.createObjectURL(blob);
                    var modScript = els.doc.createElement("script");
                    modScript.type = "module";
                    modScript.src = blobUrl;
                    modScript.onload = function () {
                        global.URL.revokeObjectURL(blobUrl);
                        resolve();
                    };
                    modScript.onerror = function () {
                        global.URL.revokeObjectURL(blobUrl);
                        reject(new Error("Failed to execute ES module script: " + sourceUrl));
                    };
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(modScript);
                    return;
                }

                if (code.length > 0) {
                    var inlineScript = els.doc.createElement("script");
                    inlineScript.type = "text/javascript";
                    inlineScript.text = code + "\n//# sourceURL=" + sourceUrl;
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(inlineScript);
                    resolve();
                } else {
                    var extScript = els.doc.createElement("script");
                    extScript.src = sourceUrl;
                    extScript.onload = function () {
                        resolve();
                    };
                    extScript.onerror = function () {
                        reject(new Error("Failed to load script: " + sourceUrl));
                    };
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(extScript);
                }
            } catch (err) {
                reject(err);
            }
        });
    }

    function cleanTemplateAttr(val) {
        if (!val || val.indexOf("{{") !== -1) {
            return "";
        }
        if (val.charAt(0) === "?") {
            return "";
        }
        return val;
    }

    function resolveLoaderConfig() {
        var cfg = global.GenericUiLoaderConfig || {};
        var els = getDomElements();
        var wasmTag = els.wasmDescriptor;
        var jsTag = els.jsDescriptor;
        var scriptEl = els.loaderScript;
        var splashEl = els.splash;

        function readTagAttr(el, attrName) {
            if (el && typeof el.getAttribute === "function") {
                return cleanTemplateAttr(el.getAttribute(attrName));
            }
            return "";
        }

        var wasmJsUrl = cfg.wasmJsUrl ||
            readTagAttr(wasmTag, "src") ||
            readTagAttr(scriptEl, "data-wasm-js-url") ||
            readTagAttr(splashEl, "data-wasm-js-url");

        var wasmBinaryUrl = cfg.wasmBinaryUrl ||
            readTagAttr(wasmTag, "data-wasm-binary") ||
            readTagAttr(wasmTag, "data-wasm-binary-src") ||
            readTagAttr(scriptEl, "data-wasm-binary-url") ||
            readTagAttr(splashEl, "data-wasm-binary-url");

        var jsIrUrl = cfg.jsIrUrl ||
            readTagAttr(jsTag, "src") ||
            readTagAttr(scriptEl, "data-js-ir-url") ||
            readTagAttr(splashEl, "data-js-ir-url") ||
            cfg.appBundleUrl ||
            readTagAttr(scriptEl, "data-app-bundle-url");

        var fadeDurationMs = typeof cfg.fadeDurationMs === "number"
            ? cfg.fadeDurationMs
            : (parseInt(readTagAttr(scriptEl, "data-fade-duration-ms") || readTagAttr(splashEl, "data-fade-duration-ms"), 10) || 350);

        return {
            wasmJsUrl: wasmJsUrl,
            wasmBinaryUrl: wasmBinaryUrl,
            jsIrUrl: jsIrUrl,
            fadeDurationMs: fadeDurationMs
        };
    }

    function loadJsIrFallback(config) {
        if (!config.jsIrUrl) {
            throw new Error("Neither a supported WebAssembly bundle nor a fallback JS IR bundle URL was configured.");
        }
        state.runtimeTarget = "js-ir";
        global.__GENERICUI_WEB_RUNTIME__ = "js-ir";
        return downloadWithProgress(config.jsIrUrl, 10, 92, "Downloading JS application bundle")
            .then(function (jsBytes) {
                updateProgress(95, "Starting application (JS IR)...");
                return executeDownloadedScript(jsBytes, config.jsIrUrl);
            });
    }

    function loadWasmPrimaryWithFallback(config) {
        var wasmSupported = detectWasmSupport();
        if (wasmSupported && config.wasmJsUrl) {
            state.runtimeTarget = "wasm";
            global.__GENERICUI_WEB_RUNTIME__ = "wasm";
            updateProgress(5, "WebAssembly supported — downloading Wasm bundle");

            var wasmChain = Promise.resolve();
            if (config.wasmBinaryUrl) {
                wasmChain = downloadWithProgress(config.wasmBinaryUrl, 5, 72, "Downloading WebAssembly binary")
                    .then(function (wasmBytes) {
                        registerPreloadedWasmBinary(config.wasmBinaryUrl, wasmBytes);
                    });
            }

            return wasmChain
                .then(function () {
                    var startPct = config.wasmBinaryUrl ? 72 : 10;
                    return downloadWithProgress(config.wasmJsUrl, startPct, 92, "Downloading WebAssembly loader");
                })
                .then(function (wasmJsBytes) {
                    updateProgress(95, "Starting application (WebAssembly)...");
                    return executeDownloadedScript(wasmJsBytes, config.wasmJsUrl);
                })
                .catch(function (wasmErr) {
                    if (config.jsIrUrl && config.jsIrUrl !== config.wasmJsUrl && !state.hasError) {
                        if (global.console && typeof global.console.warn === "function") {
                            global.console.warn(
                                "[GenericUI Loader] WebAssembly bundle failed to load (" +
                                (wasmErr && wasmErr.message ? wasmErr.message : wasmErr) +
                                "); falling back to JS IR bundle: " + config.jsIrUrl
                            );
                        }
                        state.currentProgress = 10;
                        return loadJsIrFallback(config);
                    }
                    throw wasmErr;
                });
        }

        return loadJsIrFallback(config);
    }

    function installGlobalErrorListeners() {
        if (typeof global.addEventListener !== "function") {
            return;
        }
        global.addEventListener("error", function (event) {
            var errObj = (event && event.error) ? event.error : new Error(
                (event && event.message ? event.message : "Unhandled runtime error") +
                (event && event.filename ? " at " + event.filename + ":" + (event.lineno || 0) : "")
            );
            showError(errObj, "Runtime Exception");
        });
        global.addEventListener("unhandledrejection", function (event) {
            var reason = event && event.reason ? event.reason : new Error("Unhandled Promise rejection");
            showError(reason, "Unhandled Promise Rejection");
        });
    }

    function ensureRootContainer() {
        var doc = global.document;
        if (!doc) {
            return null;
        }
        var root = (typeof doc.getElementsByTagName === "function" && doc.getElementsByTagName("genericui:root")[0]) ||
            (typeof doc.querySelector === "function" ? doc.querySelector("genericui\\:root") : null);
        if (!root && doc.body && typeof doc.createElement === "function") {
            root = doc.createElement("genericui:root");
            doc.body.appendChild(root);
        }
        if (root && typeof root.removeAttribute === "function") {
            root.removeAttribute("id");
            root.removeAttribute("data-bs-theme");
        }
        return root;
    }

    function startLoader() {
        installGlobalErrorListeners();
        ensureRootContainer();
        var config = resolveLoaderConfig();

        if (!config.wasmJsUrl && !config.jsIrUrl) {
            updateProgress(100, "Ready");
            fadeOutSplash(config.fadeDurationMs);
            return Promise.resolve();
        }

        updateProgress(2, "Detecting WebAssembly support...");
        return loadWasmPrimaryWithFallback(config)
            .then(function () {
                if (!state.hasError) {
                    fadeOutSplash(config.fadeDurationMs);
                }
            })
            .catch(function (err) {
                showError(err, "Failed to Load Application");
            });
    }

    global.GenericUiSplash = {
        detectWasmSupport: detectWasmSupport,
        ensureRootContainer: ensureRootContainer,
        updateProgress: updateProgress,
        finish: fadeOutSplash,
        showError: showError,
        start: startLoader,
        getRuntimeTarget: function () {
            return state.runtimeTarget;
        }
    };

    if (global.document) {
        if (global.document.readyState === "loading") {
            global.document.addEventListener("DOMContentLoaded", function () {
                startLoader();
            });
        } else {
            startLoader();
        }
    }
})(typeof window !== "undefined" ? window : globalThis);
"""

    const val DEFAULT_LOADER_JS: String = DEFAULT_LOADER_SCRIPT
}
