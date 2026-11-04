package com.nexustools.genericui.web

/**
 * Provides the Wasm-first / JS-IR-fallback loader JavaScript (`genericui-loader.js`) for the `:web` backend.
 *
 * The loader script:
 * 1. Attaches to `#genericui-splash` and `#genericui-splash-progress-bar` if present in the DOM.
 * 2. Strictly enforces the short 1-2 word loader status messages:
 *    - `"Initializing"`
 *    - `"Loading Resources"`
 *    - `"Loading Application"`
 *    - `"Loading Fallback"`
 *    - `"Error"`
 *    - `"Ready"`
 * 3. Preloads all resources listed in `window.__GENERICUI_RESOURCE_MANIFEST__` (or `./resources-manifest.json`)
 *    into `window.__GENERICUI_PRELOADED_RESOURCES__` (`0%` -> `50%` progress) before loading Wasm or JS,
 *    whether `manifest.webmanifest` is enabled or not.
 * 4. Detects browser WebAssembly + WasmGC support and loads the Wasm bundle (`50%` -> `100%` progress).
 *    If WebAssembly fails to load/compile/instantiate, dumps the error to `console.error` silently,
 *    displays `"Loading Fallback"`, and loads the compiled Kotlin/JS IR bundle instead.
 * 5. Sets `"Ready"` and fades out the splash screen once loaded, or sets `"Error"` on fatal failure.
 */
object WebWasmJsLoader {

    const val DEFAULT_LOADER_FILENAME: String = "loader.js"

    const val STATUS_INITIALIZING: String = WebSplashConfig.STATUS_INITIALIZING
    const val STATUS_LOADING_RESOURCES: String = WebSplashConfig.STATUS_LOADING_RESOURCES
    const val STATUS_LOADING_APPLICATION: String = WebSplashConfig.STATUS_LOADING_APPLICATION
    const val STATUS_LOADING_FALLBACK: String = WebSplashConfig.STATUS_LOADING_FALLBACK
    const val STATUS_ERROR: String = WebSplashConfig.STATUS_ERROR
    const val STATUS_READY: String = WebSplashConfig.STATUS_READY

    val ALLOWED_STATUS_MESSAGES: List<String> = listOf(
        STATUS_INITIALIZING,
        STATUS_LOADING_RESOURCES,
        STATUS_LOADING_APPLICATION,
        STATUS_LOADING_FALLBACK,
        STATUS_ERROR,
        STATUS_READY
    )

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

    const setTheme = (theme) => {
        document.documentElement.setAttribute('data-bs-theme', theme);
    };
    global.setTheme = setTheme;

    function applyInitialBrowserTheme() {
        var doc = global.document;
        if (!doc || !doc.documentElement) {
            return;
        }
        var currentTheme = (doc.documentElement.getAttribute("data-bs-theme") || "auto").toLowerCase();
        if (currentTheme === "auto") {
            const isDarkMode = typeof global.matchMedia === "function"
                ? global.matchMedia('(prefers-color-scheme: dark)').matches
                : true;
            if (global.console && typeof global.console.log === "function") {
                global.console.log(isDarkMode ? 'Dark mode' : 'Light mode');
            }
            setTheme(isDarkMode ? 'dark' : 'light');
        }
    }
    applyInitialBrowserTheme();

    var STATUS_INITIALIZING = "Initializing";
    var STATUS_LOADING_RESOURCES = "Loading Resources";
    var STATUS_LOADING_APPLICATION = "Loading Application";
    var STATUS_LOADING_FALLBACK = "Loading Fallback";
    var STATUS_ERROR = "Error";
    var STATUS_READY = "Ready";

    var ALLOWED_STATUS_MAP = {
        "Initializing": true,
        "Loading Resources": true,
        "Loading Application": true,
        "Loading Fallback": true,
        "Error": true,
        "Ready": true
    };

    if (!global.__GENERICUI_PRELOADED_RESOURCES__) {
        global.__GENERICUI_PRELOADED_RESOURCES__ = {};
    }

    function normalizeStatusMessage(statusText) {
        if (!statusText || typeof statusText !== "string") {
            return STATUS_INITIALIZING;
        }
        var trimmed = statusText.replace(/^\s+|\s+$/g, "");
        if (ALLOWED_STATUS_MAP[trimmed]) {
            return trimmed;
        }
        var lower = trimmed.toLowerCase();
        if (lower.indexOf("fallback") !== -1) {
            return STATUS_LOADING_FALLBACK;
        }
        if (lower.indexOf("error") !== -1 || lower.indexOf("fail") !== -1 || lower.indexOf("exception") !== -1) {
            return STATUS_ERROR;
        }
        if (lower.indexOf("ready") !== -1 || lower.indexOf("complete") !== -1 || lower.indexOf("finish") !== -1) {
            return STATUS_READY;
        }
        if (lower.indexOf("resource") !== -1 || lower.indexOf("preload") !== -1 || lower.indexOf("asset") !== -1) {
            return STATUS_LOADING_RESOURCES;
        }
        if (lower.indexOf("application") !== -1 || lower.indexOf("wasm") !== -1 || lower.indexOf("bundle") !== -1 || lower.indexOf("download") !== -1) {
            return STATUS_LOADING_APPLICATION;
        }
        return STATUS_INITIALIZING;
    }

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
        var resourcesLink = doc.querySelector('link[rel="resources"]') ||
            doc.getElementById("genericui-resources-manifest");
        var manifestScript = doc.getElementById("genericui-resource-manifest") ||
            doc.querySelector('script[type="genericui-resource-manifest"]');

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
            jsDescriptor: jsDescriptor,
            resourcesLink: resourcesLink,
            manifestScript: manifestScript
        };
    }

    var state = {
        hasError: false,
        fadedOut: false,
        currentProgress: 0,
        currentStatus: STATUS_INITIALIZING,
        wasmSupported: null,
        totalBytes: 0,
        loadedBytes: 0,
        resourceBytes: 0,
        wasmBinaryBytes: 0,
        wasmJsBytes: 0,
        jsIrBytes: 0,
        preloadedWasmBytes: {},
        lastPreloadedWasmBytes: null,
        loadingWasm: false,
        canFallbackToJs: false,
        activeWasmPromises: [],
        onWasmAsyncFailure: null,
        runtimeTarget: null,
        diagnostics: [],
        startTime: Date.now()
    };

    function resolveAbsoluteUrl(url) {
        try {
            var base = (global.document && global.document.baseURI) || (global.location && global.location.href) || undefined;
            return new global.URL(String(url), base).href;
        } catch (e) {
            return String(url);
        }
    }

    function logDiagnostic(message) {
        var line = "[+" + (Date.now() - state.startTime) + "ms] " + message;
        state.diagnostics.push(line);
        if (state.diagnostics.length > 300) {
            state.diagnostics.shift();
        }
        if (global.console && typeof global.console.debug === "function") {
            global.console.debug("[GenericUI Loader] " + line);
        }
    }

    function createLoaderError(message, cause, context) {
        var err = new Error(message);
        if (cause !== undefined && cause !== null) {
            try {
                err.cause = cause;
            } catch (e) {
            }
        }
        if (context) {
            err.genericUiContext = context;
        }
        return err;
    }

    function describeContext(ctx) {
        if (!ctx || typeof ctx !== "object") {
            return "";
        }
        var parts = [];
        var keys = Object.keys(ctx);
        for (var i = 0; i < keys.length; i++) {
            var v = ctx[keys[i]];
            if (v !== undefined && v !== null && v !== "") {
                parts.push(keys[i] + "=" + String(v));
            }
        }
        return parts.length > 0 ? " {" + parts.join(", ") + "}" : "";
    }

    var BOOTSTRAP_BG_CLASSES = [
        "bg-primary",
        "bg-secondary",
        "bg-success",
        "bg-danger",
        "bg-warning",
        "bg-info",
        "bg-light",
        "bg-dark"
    ];

    var BOOTSTRAP_TEXT_CLASSES = [
        "text-primary",
        "text-secondary",
        "text-success",
        "text-danger",
        "text-warning",
        "text-info",
        "text-light",
        "text-dark",
        "text-body-secondary",
        "fw-semibold"
    ];

    var STATUS_ICON_CLASSES = {
        "Ready": "genericui-splash-status-icon bi bi-check-circle-fill text-success",
        "Error": "genericui-splash-status-icon bi bi-dash-circle-fill text-danger"
    };
    var STATUS_SPINNER_CLASS = "genericui-splash-status-icon spinner-border spinner-border-sm";

    function ensureStatusParts(els) {
        var label = els.statusLabel;
        if (!label || !els.doc) {
            return null;
        }
        var icon = els.doc.getElementById("genericui-splash-status-icon");
        var text = els.doc.getElementById("genericui-splash-status-text");
        if (!icon || !text) {
            var existingText = label.textContent || "";
            while (label.firstChild) {
                label.removeChild(label.firstChild);
            }
            icon = els.doc.createElement("span");
            icon.id = "genericui-splash-status-icon";
            icon.setAttribute("aria-hidden", "true");
            text = els.doc.createElement("span");
            text.id = "genericui-splash-status-text";
            text.className = "genericui-splash-status-text";
            text.textContent = existingText.replace(/^\s+|\s+$/g, "");
            label.appendChild(icon);
            label.appendChild(text);
        }
        return { icon: icon, text: text };
    }

    function setStatusDisplay(els, status, bootstrapType) {
        var parts = ensureStatusParts(els);
        if (!parts) {
            return;
        }
        parts.text.textContent = status;
        var normType = (bootstrapType && typeof bootstrapType === "string")
            ? bootstrapType.toLowerCase().replace(/^\s+|\s+$/g, "")
            : "";
        var iconClass;
        if (normType === "danger" || status === STATUS_ERROR) {
            iconClass = STATUS_ICON_CLASSES["Error"];
        } else if (status === STATUS_READY || normType === "success") {
            iconClass = STATUS_ICON_CLASSES["Ready"];
        } else {
            iconClass = STATUS_SPINNER_CLASS + (normType && normType !== "none" ? " text-" + normType : "");
        }
        if (parts.icon.className !== iconClass) {
            parts.icon.className = iconClass;
        }
        if (els.statusLabel.classList) {
            for (var i = 0; i < BOOTSTRAP_TEXT_CLASSES.length; i++) {
                els.statusLabel.classList.remove(BOOTSTRAP_TEXT_CLASSES[i]);
            }
            if (normType && normType !== "none") {
                els.statusLabel.classList.add("text-" + normType);
                if (normType === "danger") {
                    els.statusLabel.classList.add("fw-semibold");
                }
            } else if (status === STATUS_ERROR) {
                els.statusLabel.classList.add("text-danger", "fw-semibold");
            } else {
                els.statusLabel.classList.add("text-body-secondary");
            }
        }
        els.statusLabel.setAttribute("data-status", status.toLowerCase().replace(/\s+/g, "-"));
    }

    function updateProgress(percent, statusText, allowCustomStatus, bootstrapType) {
        if (state.hasError) {
            return;
        }
        var clamped = Math.max(0, Math.min(100, Math.round(percent)));
        if (clamped < state.currentProgress && clamped !== 0) {
            clamped = state.currentProgress;
        }
        state.currentProgress = clamped;

        var normalizedStatus;
        if (allowCustomStatus === true && typeof statusText === "string" && statusText.replace(/^\s+|\s+$/g, "").length > 0) {
            normalizedStatus = statusText.replace(/^\s+|\s+$/g, "");
        } else {
            normalizedStatus = statusText ? normalizeStatusMessage(statusText) : state.currentStatus;
        }
        state.currentStatus = normalizedStatus;

        var els = getDomElements();
        if (els.progressBar) {
            els.progressBar.style.width = clamped + "%";
            els.progressBar.setAttribute("aria-valuenow", String(clamped));
        }
        if (els.progressContainer) {
            els.progressContainer.setAttribute("aria-valuenow", String(clamped));
        }
        if (els.statusLabel) {
            setStatusDisplay(els, normalizedStatus, bootstrapType);
        }
    }

    function setStatus(statusText, bootstrapType) {
        if (state.hasError && (!bootstrapType || String(bootstrapType).toLowerCase() !== "danger")) {
            return;
        }
        if (typeof statusText === "string" && statusText.replace(/^\s+|\s+$/g, "").length > 0) {
            state.currentStatus = statusText.replace(/^\s+|\s+$/g, "");
        }
        var els = getDomElements();
        if (els.statusLabel) {
            setStatusDisplay(els, state.currentStatus, bootstrapType);
        }
    }

    function setProgress(percent, bootstrapType) {
        if (state.hasError) {
            return;
        }
        var clamped = Math.max(0, Math.min(100, Math.round(typeof percent === "number" ? percent : state.currentProgress)));
        state.currentProgress = clamped;
        var normType = (bootstrapType && typeof bootstrapType === "string")
            ? bootstrapType.toLowerCase().replace(/^\s+|\s+$/g, "")
            : "primary";
        var els = getDomElements();
        if (els.progressBar) {
            els.progressBar.style.width = clamped + "%";
            els.progressBar.setAttribute("aria-valuenow", String(clamped));
            if (els.progressBar.classList) {
                for (var i = 0; i < BOOTSTRAP_BG_CLASSES.length; i++) {
                    els.progressBar.classList.remove(BOOTSTRAP_BG_CLASSES[i]);
                }
                var bgClass = (normType && normType !== "none") ? ("bg-" + normType) : "bg-primary";
                els.progressBar.classList.add(bgClass);
            }
        }
        if (els.progressContainer) {
            els.progressContainer.setAttribute("aria-valuenow", String(clamped));
        }
    }

    function setError(message, stackTrace, title) {
        var err = new Error(message || "Application Error");
        err.stack = (stackTrace !== undefined && stackTrace !== null) ? String(stackTrace) : "";
        showError(err, title || STATUS_ERROR);
    }

    function setApplicationStatus(statusText, percent) {
        var pct = typeof percent === "number" ? percent : Math.max(state.currentProgress, 99);
        updateProgress(pct, statusText, true);
    }

    function formatErrorDetails(err) {
        var lines = [];
        var headline = "An unknown application error occurred.";
        var queue = [{ error: err, prefix: "" }];
        var seen = [];
        var count = 0;

        while (queue.length > 0 && count < 12) {
            var item = queue.shift();
            var current = item.error;
            count++;
            if (current === undefined || current === null) {
                if (count === 1) {
                    lines.push("Error: (no error object was provided; the failure was reported as " + String(current) + ")");
                }
                continue;
            }
            if (seen.indexOf(current) !== -1) {
                continue;
            }
            seen.push(current);

            if (typeof current !== "object" && typeof current !== "function") {
                var primitiveText = item.prefix + "Thrown value (" + typeof current + "): " + String(current);
                lines.push(primitiveText);
                if (count === 1) {
                    headline = String(current);
                }
                continue;
            }

            var name = current.name || (current.constructor && current.constructor.name) || "Error";
            var message = current.message !== undefined ? String(current.message) : String(current);
            if (count === 1) {
                headline = name + ": " + message;
            }
            if (lines.length > 0) {
                lines.push("");
            }
            lines.push(item.prefix + name + ": " + message);
            if (current.genericUiContext) {
                var ctxKeys = Object.keys(current.genericUiContext);
                for (var c = 0; c < ctxKeys.length; c++) {
                    lines.push("    [context] " + ctxKeys[c] + ": " + String(current.genericUiContext[ctxKeys[c]]));
                }
            }
            if (current.genericUiWasmContext) {
                var wKeys = Object.keys(current.genericUiWasmContext);
                for (var w = 0; w < wKeys.length; w++) {
                    lines.push("    [wasm] " + wKeys[w] + ": " + String(current.genericUiWasmContext[wKeys[w]]));
                }
            }
            if (current.filename || current.lineno) {
                lines.push("    [source] " + (current.filename || "?") + ":" + (current.lineno || 0) + ":" + (current.colno || 0));
            }
            var stack = current.stack ? String(current.stack) : "";
            if (stack) {
                var stackLines = stack.split("\n");
                for (var s = 0; s < stackLines.length; s++) {
                    var sl = stackLines[s];
                    if (s === 0 && (sl.indexOf(message) !== -1 || sl === name)) {
                        continue;
                    }
                    lines.push("    " + sl.replace(/^\s+/, ""));
                }
            } else {
                lines.push("    (no stacktrace available)");
            }

            if (current.cause !== undefined && current.cause !== null) {
                queue.push({ error: current.cause, prefix: "Caused by: " });
            }
            if (current.error && current.error !== current && typeof current.error === "object") {
                queue.push({ error: current.error, prefix: "Wrapped error: " });
            }
            if (current.reason && current.reason !== current) {
                queue.push({ error: current.reason, prefix: "Rejection reason: " });
            }
            if (Array.isArray(current.suppressed)) {
                for (var p = 0; p < current.suppressed.length; p++) {
                    queue.push({ error: current.suppressed[p], prefix: "Earlier failure: " });
                }
            }
            if (Array.isArray(current.errors)) {
                for (var a = 0; a < current.errors.length; a++) {
                    queue.push({ error: current.errors[a], prefix: "Aggregated error: " });
                }
            }
        }

        lines.push("");
        lines.push("Environment:");
        lines.push("    runtimeTarget: " + (state.runtimeTarget || "(not selected yet)"));
        lines.push("    loaderStatus: " + state.currentStatus + " (" + state.currentProgress + "%)");
        lines.push("    page: " + (global.location ? global.location.href : "(unknown)"));
        lines.push("    baseURI: " + (global.document && global.document.baseURI ? global.document.baseURI : "(unknown)"));
        lines.push("    userAgent: " + (global.navigator && global.navigator.userAgent ? global.navigator.userAgent : "(unknown)"));
        lines.push("    elapsed: " + (Date.now() - state.startTime) + "ms");

        if (state.diagnostics.length > 0) {
            lines.push("");
            lines.push("Loader log (most recent last):");
            var startIdx = Math.max(0, state.diagnostics.length - 60);
            for (var d = startIdx; d < state.diagnostics.length; d++) {
                lines.push("    " + state.diagnostics[d]);
            }
        }

        return {
            message: headline,
            stack: lines.join("\n")
        };
    }

    function showError(err, phaseTitle) {
        state.hasError = true;
        state.loadingWasm = false;
        state.currentStatus = STATUS_ERROR;
        var details = formatErrorDetails(err);
        var els = getDomElements();

        if (global.console && typeof global.console.error === "function") {
            global.console.error("[GenericUI Loader] " + (phaseTitle || STATUS_ERROR) + ": " + details.message + "\n" + details.stack, err);
        }

        if (!els.doc) {
            return;
        }

        if (els.splash) {
            els.splash.classList.remove("d-none", "opacity-0", "genericui-splash-hidden");
        }

        if (els.progressBar) {
            els.progressBar.classList.remove("progress-bar-animated", "progress-bar-striped");
            els.progressBar.classList.add("bg-danger");
            els.progressBar.style.width = "100%";
        }

        if (els.statusLabel) {
            setStatusDisplay(els, STATUS_ERROR);
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
            headerDiv.textContent = phaseTitle || STATUS_ERROR;
            errorBox.appendChild(headerDiv);

            errorMessage = els.doc.createElement("div");
            errorMessage.id = "genericui-splash-error-message";
            errorMessage.className = "genericui-splash-error-message small fw-semibold mb-2";
            errorBox.appendChild(errorMessage);

            errorStack = els.doc.createElement("pre");
            errorStack.id = "genericui-splash-error-stack";
            errorStack.className = "genericui-splash-error-stack small mb-0 p-2 bg-dark text-light rounded overflow-auto";
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
        state.loadingWasm = false;
        updateProgress(100, STATUS_READY);

        var els = getDomElements();
        var splash = els.splash;
        if (!splash) {
            return;
        }
        var duration = typeof fadeDurationMs === "number" && fadeDurationMs >= 0 ? fadeDurationMs : 350;
        if (duration !== 350) {
            splash.style.transitionDuration = duration + "ms";
        }
        splash.classList.add("genericui-splash-hidden");

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

    function warnWasmUnsupported(reason, error) {
        if (global.console && typeof global.console.warn === "function") {
            var detail = "";
            if (error) {
                detail = " Details: " + (error.stack || error.message || String(error));
            }
            global.console.warn(
                "[GenericUI Loader] WebAssembly detected as not supported: " + reason + detail
            );
        }
    }

    function logWasmFallbackErrorSilently(error) {
        if (global.console && typeof global.console.error === "function") {
            var details = formatErrorDetails(error);
            global.console.error(
                "[GenericUI Loader] WebAssembly failed to load; falling back to the JS bundle.\n" + details.stack,
                error
            );
        }
    }

    function probeWasmCompileError(bytes) {
        if (!global.WebAssembly || typeof global.WebAssembly.Module !== "function") {
            return "";
        }
        try {
            new global.WebAssembly.Module(bytes);
            return "";
        } catch (compileErr) {
            return compileErr && compileErr.message ? compileErr.message : String(compileErr);
        }
    }

    function detectWasmSupport() {
        try {
            if (typeof global.WebAssembly !== "object" || global.WebAssembly === null) {
                warnWasmUnsupported(
                    "global WebAssembly object is missing (typeof WebAssembly === \"" + (typeof global.WebAssembly) + "\")."
                );
                return false;
            }
            if (typeof global.WebAssembly.instantiate !== "function") {
                warnWasmUnsupported(
                    "WebAssembly.instantiate is not a function (typeof WebAssembly.instantiate === \"" + (typeof global.WebAssembly.instantiate) + "\")."
                );
                return false;
            }
            if (typeof global.WebAssembly.validate !== "function") {
                warnWasmUnsupported(
                    "WebAssembly.validate is not a function (typeof WebAssembly.validate === \"" + (typeof global.WebAssembly.validate) + "\")."
                );
                return false;
            }
            var basicWasmProbe = new Uint8Array([0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00]);
            if (!global.WebAssembly.validate(basicWasmProbe)) {
                var basicError = probeWasmCompileError(basicWasmProbe);
                warnWasmUnsupported(
                    "baseline WebAssembly module header validation (WebAssembly.validate) returned false" +
                    (basicError ? " (" + basicError + ")." : ".")
                );
                return false;
            }
            var wasmGcProbe = new Uint8Array([
                0x00, 0x61, 0x73, 0x6d,
                0x01, 0x00, 0x00, 0x00,
                0x01, 0x03, 0x01, 0x5f, 0x00
            ]);
            if (global.WebAssembly.validate(wasmGcProbe)) {
                return true;
            }
            if (global.GenericUiLoaderConfig && global.GenericUiLoaderConfig.allowStandardWasm === true) {
                return true;
            }
            var gcError = probeWasmCompileError(wasmGcProbe);
            warnWasmUnsupported(
                "browser supports baseline WebAssembly, but WasmGC struct type validation (0x5f required by Kotlin/Wasm) returned false" +
                (gcError ? " (" + gcError + ")." : ".")
            );
            return false;
        } catch (err) {
            warnWasmUnsupported("an exception was thrown while probing WebAssembly support.", err);
            return false;
        }
    }

    function downloadWithProgress(url, minPct, maxPct, statusMessage, expectedBytes) {
        var resolvedStatus = normalizeStatusMessage(statusMessage);
        if (state.totalBytes <= 0) {
            updateProgress(minPct, resolvedStatus);
        } else {
            var startPct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
            updateProgress(startPct, resolvedStatus);
        }
        if (typeof global.fetch !== "function") {
            return new Promise(function (resolve, reject) {
                var xhr = new global.XMLHttpRequest();
                xhr.open("GET", url, true);
                xhr.responseType = "arraybuffer";
                xhr.onprogress = function (ev) {
                    if (state.totalBytes > 0 && ev.loaded > 0) {
                        var inFlightBytes = (typeof expectedBytes === "number" && expectedBytes > 0)
                            ? Math.min(expectedBytes, ev.loaded)
                            : ev.loaded;
                        var bytePct = Math.min(99, Math.round(((state.loadedBytes + inFlightBytes) / state.totalBytes) * 100));
                        updateProgress(bytePct, resolvedStatus);
                    } else if (ev.lengthComputable && ev.total > 0) {
                        var ratio = ev.loaded / ev.total;
                        var pct = minPct + ratio * (maxPct - minPct);
                        updateProgress(pct, resolvedStatus);
                    }
                };
                xhr.onload = function () {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        var respLen = xhr.response ? xhr.response.byteLength : 0;
                        if (state.totalBytes > 0) {
                            var doneBytes = (typeof expectedBytes === "number" && expectedBytes > 0) ? expectedBytes : respLen;
                            state.loadedBytes += doneBytes;
                            var donePct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                            updateProgress(donePct, resolvedStatus);
                        } else {
                            updateProgress(maxPct, resolvedStatus);
                        }
                        logDiagnostic("XHR GET " + resolveAbsoluteUrl(url) + " -> " + xhr.status + " (" + respLen + " bytes)");
                        resolve(new Uint8Array(xhr.response));
                    } else {
                        logDiagnostic("XHR GET " + resolveAbsoluteUrl(url) + " -> " + xhr.status + " " + xhr.statusText);
                        reject(createLoaderError(
                            "HTTP " + xhr.status + " " + (xhr.statusText || "") + " while downloading " + resolveAbsoluteUrl(url),
                            null,
                            {
                                phase: resolvedStatus,
                                requestedUrl: url,
                                resolvedUrl: resolveAbsoluteUrl(url),
                                httpStatus: xhr.status,
                                httpStatusText: xhr.statusText,
                                contentType: xhr.getResponseHeader ? xhr.getResponseHeader("Content-Type") : ""
                            }
                        ));
                    }
                };
                xhr.onerror = function () {
                    logDiagnostic("XHR GET " + resolveAbsoluteUrl(url) + " -> network error");
                    reject(createLoaderError(
                        "Network error while downloading " + resolveAbsoluteUrl(url) + " (offline, CORS, DNS, or connection reset)",
                        null,
                        { phase: resolvedStatus, requestedUrl: url, resolvedUrl: resolveAbsoluteUrl(url) }
                    ));
                };
                xhr.send();
            });
        }

        var absoluteUrl = resolveAbsoluteUrl(url);
        return global.fetch(url, { credentials: "same-origin" }).then(function (response) {
            if (!response.ok) {
                var errContentType = response.headers ? response.headers.get("Content-Type") : "";
                logDiagnostic("GET " + absoluteUrl + " -> " + response.status + " " + response.statusText + " (" + (errContentType || "no content-type") + ")");
                throw createLoaderError(
                    "HTTP " + response.status + " " + (response.statusText || "") + " while downloading " + absoluteUrl,
                    null,
                    {
                        phase: resolvedStatus,
                        requestedUrl: url,
                        resolvedUrl: absoluteUrl,
                        responseUrl: response.url,
                        redirected: response.redirected,
                        httpStatus: response.status,
                        httpStatusText: response.statusText,
                        contentType: errContentType
                    }
                );
            }
            var contentLengthHeader = response.headers ? response.headers.get("Content-Length") : null;
            var totalBytes = contentLengthHeader ? parseInt(contentLengthHeader, 10) : 0;
            logDiagnostic("GET " + absoluteUrl + " -> " + response.status + " (" + (response.headers ? response.headers.get("Content-Type") : "?") + ", " + (totalBytes || "unknown") + " bytes)");

            if (!response.body || typeof response.body.getReader !== "function") {
                return response.arrayBuffer().then(function (buf) {
                    var out = new Uint8Array(buf);
                    if (state.totalBytes > 0) {
                        var doneBytes = (typeof expectedBytes === "number" && expectedBytes > 0) ? expectedBytes : out.byteLength;
                        state.loadedBytes += doneBytes;
                        var donePct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                        updateProgress(donePct, resolvedStatus);
                    } else {
                        updateProgress(maxPct, resolvedStatus);
                    }
                    return out;
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
                        if (state.totalBytes > 0) {
                            var doneBytes = (typeof expectedBytes === "number" && expectedBytes > 0) ? expectedBytes : receivedBytes;
                            state.loadedBytes += doneBytes;
                            var donePct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                            updateProgress(donePct, resolvedStatus);
                        } else {
                            updateProgress(maxPct, resolvedStatus);
                        }
                        return combined;
                    }
                    var chunk = result.value;
                    chunks.push(chunk);
                    receivedBytes += chunk.byteLength;

                    if (state.totalBytes > 0) {
                        var inFlightBytes = (typeof expectedBytes === "number" && expectedBytes > 0)
                            ? Math.min(expectedBytes, receivedBytes)
                            : receivedBytes;
                        var bytePct = Math.min(99, Math.round(((state.loadedBytes + inFlightBytes) / state.totalBytes) * 100));
                        updateProgress(bytePct, resolvedStatus);
                    } else {
                        var fraction = totalBytes > 0
                            ? Math.min(1, receivedBytes / totalBytes)
                            : Math.min(0.95, 1 - Math.exp(-receivedBytes / (512 * 1024)));
                        var pct = minPct + fraction * (maxPct - minPct);
                        updateProgress(pct, resolvedStatus);
                    }
                    return pump();
                });
            }

            return pump();
        }).catch(function (fetchErr) {
            if (fetchErr && fetchErr.genericUiContext) {
                throw fetchErr;
            }
            logDiagnostic("GET " + absoluteUrl + " -> failed: " + (fetchErr && fetchErr.message ? fetchErr.message : String(fetchErr)));
            throw createLoaderError(
                "Request failed while downloading " + absoluteUrl + " (network error, CORS, offline, or the response stream was interrupted)",
                fetchErr,
                { phase: resolvedStatus, requestedUrl: url, resolvedUrl: absoluteUrl }
            );
        });
    }

    var BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    function bytesToBase64(bytes) {
        if (!bytes || bytes.length === 0) {
            return "";
        }
        if (typeof global.btoa === "function") {
            var binary = "";
            var chunkSize = 0x8000;
            for (var c = 0; c < bytes.length; c += chunkSize) {
                binary += String.fromCharCode.apply(null, bytes.subarray(c, Math.min(c + chunkSize, bytes.length)));
            }
            return global.btoa(binary);
        }
        var result = "";
        var len = bytes.length;
        var i = 0;
        while (i < len) {
            var b0 = bytes[i++] & 0xff;
            if (i === len) {
                result += BASE64_CHARS.charAt(b0 >> 2);
                result += BASE64_CHARS.charAt((b0 & 0x03) << 4);
                result += "==";
                break;
            }
            var b1 = bytes[i++] & 0xff;
            if (i === len) {
                result += BASE64_CHARS.charAt(b0 >> 2);
                result += BASE64_CHARS.charAt(((b0 & 0x03) << 4) | ((b1 & 0xf0) >> 4));
                result += BASE64_CHARS.charAt((b1 & 0x0f) << 2);
                result += "=";
                break;
            }
            var b2 = bytes[i++] & 0xff;
            result += BASE64_CHARS.charAt(b0 >> 2);
            result += BASE64_CHARS.charAt(((b0 & 0x03) << 4) | ((b1 & 0xf0) >> 4));
            result += BASE64_CHARS.charAt(((b1 & 0x0f) << 2) | ((b2 & 0xc0) >> 6));
            result += BASE64_CHARS.charAt(b2 & 0x3f);
        }
        return result;
    }

    function base64ToBytes(base64) {
        if (!base64) {
            return new Uint8Array(0);
        }
        var clean = String(base64).replace(/[^A-Za-z0-9+/=]/g, "");
        if (typeof global.atob === "function") {
            var bin = global.atob(clean);
            var out = new Uint8Array(bin.length);
            for (var i = 0; i < bin.length; i++) {
                out[i] = bin.charCodeAt(i) & 0xff;
            }
            return out;
        }
        var bytes = [];
        for (var j = 0; j < clean.length; j += 4) {
            var c0 = BASE64_CHARS.indexOf(clean.charAt(j));
            var c1 = BASE64_CHARS.indexOf(clean.charAt(j + 1));
            var c2 = clean.charAt(j + 2) === "=" ? -1 : BASE64_CHARS.indexOf(clean.charAt(j + 2));
            var c3 = clean.charAt(j + 3) === "=" ? -1 : BASE64_CHARS.indexOf(clean.charAt(j + 3));
            if (c0 < 0 || c1 < 0) {
                break;
            }
            bytes.push((c0 << 2) | (c1 >> 4));
            if (c2 >= 0) {
                bytes.push(((c1 & 15) << 4) | (c2 >> 2));
                if (c3 >= 0) {
                    bytes.push(((c2 & 3) << 6) | c3);
                }
            }
        }
        return new Uint8Array(bytes);
    }

    function utf8ToBytes(text) {
        if (typeof global.TextEncoder === "function") {
            return new global.TextEncoder().encode(text || "");
        }
        var str = String(text || "");
        var utf8 = unescape(encodeURIComponent(str));
        var arr = new Uint8Array(utf8.length);
        for (var i = 0; i < utf8.length; i++) {
            arr[i] = utf8.charCodeAt(i) & 0xff;
        }
        return arr;
    }

    function bytesToUtf8(bytes) {
        if (!bytes) {
            return "";
        }
        if (typeof global.TextDecoder === "function") {
            return new global.TextDecoder("utf-8").decode(bytes);
        }
        var s = "";
        for (var i = 0; i < bytes.length; i++) {
            s += String.fromCharCode(bytes[i]);
        }
        try {
            return decodeURIComponent(escape(s));
        } catch (e) {
            return s;
        }
    }

    function guessMimeType(pathOrName, resourceType) {
        var clean = String(pathOrName || "").split("?")[0].split("#")[0].toLowerCase();
        var dotIdx = clean.lastIndexOf(".");
        var ext = dotIdx >= 0 ? clean.substring(dotIdx + 1) : clean;
        switch (ext) {
            case "css":
            case "scss":
            case "sass":
                return "text/css";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "ico":
                return "image/x-icon";
            case "woff2":
                return "font/woff2";
            case "woff":
                return "font/woff";
            case "ttf":
                return "font/ttf";
            case "otf":
                return "font/otf";
            case "json":
            case "webmanifest":
                return "application/json";
            case "xml":
                return "application/xml";
            case "txt":
                return "text/plain";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "ogg":
                return "audio/ogg";
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
        }
        var t = String(resourceType || "").toLowerCase();
        if (t === "style" || t === "styles" || t === "stylesheet" || t === "css") {
            return "text/css";
        }
        if (t === "drawable" || t === "image" || t === "icon") {
            return "image/png";
        }
        if (t === "font") {
            return "font/ttf";
        }
        if (t === "xml") {
            return "application/xml";
        }
        if (t === "string") {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    function isTextMimeType(mimeType, resourceType) {
        var m = String(mimeType || "").toLowerCase();
        var t = String(resourceType || "").toLowerCase();
        return m.indexOf("text/") === 0 ||
            m.indexOf("xml") !== -1 ||
            m.indexOf("json") !== -1 ||
            m.indexOf("svg") !== -1 ||
            m.indexOf("javascript") !== -1 ||
            t === "style" ||
            t === "styles" ||
            t === "xml" ||
            t === "string";
    }

    function inferResourceType(path) {
        var clean = String(path || "").replace(/^\.?\//, "").toLowerCase();
        if (clean.indexOf("styles/") === 0 || clean.indexOf("css/") === 0 || /\.s?css$/i.test(clean)) {
            return "style";
        }
        if (clean.indexOf("assets/drawable/") === 0 || clean.indexOf("drawable/") === 0 || /\.(png|jpe?g|gif|webp|svg|ico)$/i.test(clean)) {
            return "drawable";
        }
        if (clean.indexOf("assets/font/") === 0 || clean.indexOf("font/") === 0 || /\.(woff2?|ttf|otf)$/i.test(clean)) {
            return "font";
        }
        if (clean.indexOf("assets/xml/") === 0 || clean.indexOf("xml/") === 0 || /\.xml$/i.test(clean)) {
            return "xml";
        }
        return "raw";
    }

    function inferResourceName(path, id) {
        var clean = String(path || "").split("?")[0].split("#")[0];
        var lastSlash = clean.lastIndexOf("/");
        var file = lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
        var dot = file.lastIndexOf(".");
        var stem = dot > 0 ? file.substring(0, dot) : file;
        return stem || String(id || "");
    }

    function registerNameKeysInStore(store, type, name, record) {
        if (!name) {
            return;
        }
        var variants = [name];
        var underscored = name.replace(/-/g, "_");
        if (underscored !== name) {
            variants.push(underscored);
        }
        for (var i = 0; i < variants.length; i++) {
            var n = variants[i];
            store[n] = record;
            if (type) {
                store[type + "/" + n] = record;
                store["res://" + type + "/" + n] = record;
                store["@" + type + "/" + n] = record;
                if (type === "style" || type === "styles") {
                    store["style/" + n] = record;
                    store["res://style/" + n] = record;
                    store["@style/" + n] = record;
                    store["styles/" + n] = record;
                    store["res://styles/" + n] = record;
                    store["@styles/" + n] = record;
                    store["styles/" + n + ".css"] = record;
                }
            }
        }
    }

    function applyGenericUiResourcesCatalog(jsonText) {
        if (!jsonText) {
            return;
        }
        try {
            var catalog = typeof jsonText === "string" ? JSON.parse(jsonText) : jsonText;
            if (!catalog || typeof catalog !== "object") {
                return;
            }
            var store = global.__GENERICUI_PRELOADED_RESOURCES__;
            if (!store) {
                return;
            }
            var drawables = catalog.drawables;
            if (drawables && typeof drawables === "object") {
                var keys = Object.keys(drawables);
                for (var i = 0; i < keys.length; i++) {
                    var resName = keys[i];
                    var item = drawables[resName];
                    if (item && typeof item === "object") {
                        var rec = (item.path && store[item.path]) ||
                            store["drawable/" + resName] ||
                            store[resName];
                        if (rec) {
                            if (item.id != null) {
                                store[String(item.id)] = rec;
                                store["res://" + String(item.id)] = rec;
                            }
                            registerNameKeysInStore(store, "drawable", resName, rec);
                        }
                    }
                }
            }
        } catch (e) {
        }
    }

    function buildHashedResourceUrl(rawUrl, hash) {
        if (!rawUrl) {
            return "";
        }
        var urlStr = String(rawUrl);
        var h = hash != null ? String(hash).replace(/^\s+|\s+$/g, "").replace(/^\?+/, "") : "";
        if (!h) {
            return urlStr;
        }
        var base = urlStr.split("?")[0].split("#")[0];
        return base + "?" + h;
    }

    function registerPreloadedResourceEntry(entry, bytes) {
        if (!global.__GENERICUI_PRELOADED_RESOURCES__) {
            global.__GENERICUI_PRELOADED_RESOURCES__ = {};
        }
        var store = global.__GENERICUI_PRELOADED_RESOURCES__;
        var id = entry && entry.id != null ? String(entry.id) : (entry && entry.resourceId != null ? String(entry.resourceId) : "");
        var path = (entry && (entry.path || entry.url || entry.src || entry.href)) ? String(entry.path || entry.url || entry.src || entry.href) : "";
        var entryHash = (entry && (entry.hash || entry.sha1 || entry.version)) ? String(entry.hash || entry.sha1 || entry.version) : "";
        var hashedUrl = buildHashedResourceUrl(path, entryHash);
        var type = (entry && (entry.type || entry.resourceType)) ? String(entry.type || entry.resourceType) : inferResourceType(path);
        var name = (entry && entry.name) ? String(entry.name) : inferResourceName(path, id);
        var mimeType = (entry && (entry.mimeType || entry.contentType)) ? String(entry.mimeType || entry.contentType) : guessMimeType(path || name, type);

        var resolvedBytes = bytes instanceof Uint8Array
            ? bytes
            : (bytes ? new Uint8Array(bytes) : new Uint8Array(0));
        var base64 = bytesToBase64(resolvedBytes);
        var dataUrl = (entry && typeof entry.dataUrl === "string" && entry.dataUrl.indexOf("data:") === 0)
            ? entry.dataUrl
            : ("data:" + mimeType + ";base64," + base64);
        var blob = typeof global.Blob === "function"
            ? new global.Blob([resolvedBytes], { type: mimeType })
            : null;
        var text = (entry && typeof entry.text === "string")
            ? entry.text
            : (isTextMimeType(mimeType, type) ? bytesToUtf8(resolvedBytes) : null);

        var record = {
            id: id,
            type: type,
            name: name,
            path: path,
            hash: entryHash,
            url: hashedUrl,
            mimeType: mimeType,
            bytes: resolvedBytes,
            blob: blob,
            dataUrl: dataUrl,
            text: text
        };

        if (id) {
            store[id] = record;
            store["res://" + id] = record;
        }
        if (hashedUrl) {
            store[hashedUrl] = record;
        }
        if (path) {
            store[path] = record;
            var cleanPath = path.replace(/^\.?\//, "").split("?")[0];
            store[cleanPath] = record;
            if (cleanPath.indexOf("res/") === 0 || cleanPath.indexOf("gui/") === 0) {
                var strippedPath = cleanPath.substring(4);
                store[strippedPath] = record;
                if (/\.scss$/i.test(strippedPath)) {
                    store[strippedPath.replace(/\.scss$/i, ".css")] = record;
                }
            }
            if (/\.scss$/i.test(cleanPath)) {
                store[cleanPath.replace(/\.scss$/i, ".css")] = record;
            }
        }
        registerNameKeysInStore(store, type, name, record);

        if (text && (name === "genericui-resources" || /genericui-resources\.json$/i.test(path))) {
            applyGenericUiResourcesCatalog(text);
        }

        return record;
    }

    function getPreloadedResource(key) {
        var store = global.__GENERICUI_PRELOADED_RESOURCES__;
        if (!store || !key) {
            return null;
        }
        var k = String(key);
        if (store[k]) {
            return store[k];
        }
        var clean = k.replace(/^\.?\//, "").split("?")[0];
        if (store[clean]) {
            return store[clean];
        }
        if (store["res://" + clean]) {
            return store["res://" + clean];
        }
        return null;
    }

    function extractManifestEntries(manifestData) {
        if (!manifestData) {
            return [];
        }
        if (Array.isArray(manifestData)) {
            return manifestData;
        }
        if (Array.isArray(manifestData.resources)) {
            return manifestData.resources;
        }
        if (Array.isArray(manifestData.entries)) {
            return manifestData.entries;
        }
        if (Array.isArray(manifestData.assets)) {
            return manifestData.assets;
        }
        if (manifestData.drawables && typeof manifestData.drawables === "object") {
            var drawableList = [];
            var dKeys = Object.keys(manifestData.drawables);
            for (var d = 0; d < dKeys.length; d++) {
                var dName = dKeys[d];
                var dItem = manifestData.drawables[dName];
                if (dItem && dItem.path) {
                    drawableList.push({
                        id: dItem.id != null ? String(dItem.id) : ("drawable/" + dName),
                        type: "drawable",
                        name: dName,
                        path: dItem.path
                    });
                }
            }
            return drawableList;
        }
        if (typeof manifestData === "object") {
            var list = [];
            var keys = Object.keys(manifestData);
            for (var i = 0; i < keys.length; i++) {
                var k = keys[i];
                var item = manifestData[k];
                if (item && typeof item === "object") {
                    if (!item.id && !item.path) {
                        item.id = k;
                    }
                    list.push(item);
                }
            }
            return list;
        }
        return [];
    }

    function shouldSkipManifestResourcePath(path) {
        if (!path || typeof path !== "string") {
            return false;
        }
        var clean = path.split("?")[0].split("#")[0].toLowerCase();
        return /\.php$/i.test(clean) ||
            /\.map$/i.test(clean) ||
            /\.s[ac]ss$/i.test(clean);
    }

    function filterManifestEntries(entries, config) {
        if (!Array.isArray(entries)) {
            return [];
        }
        state.resourceBytes = 0;
        state.wasmBinaryBytes = 0;
        state.wasmJsBytes = 0;
        state.jsIrBytes = 0;
        state.loadedBytes = 0;
        var filtered = [];
        for (var i = 0; i < entries.length; i++) {
            var item = entries[i];
            if (!item) {
                continue;
            }
            var itemType = String(item.type || item.resourceType || "").toLowerCase();
            var targetPath = item.path || item.url || item.src || item.href || "";
            var itemSize = (typeof item.size === "number" && item.size > 0) ? item.size : 0;
            if (itemType === "wasm" || /\.wasm(\?.*)?$/i.test(targetPath)) {
                if (itemSize > 0) {
                    state.wasmBinaryBytes = itemSize;
                }
                continue;
            }
            if (itemType === "js") {
                if (itemSize > 0) {
                    state.wasmJsBytes = itemSize;
                    state.jsIrBytes = itemSize;
                }
                continue;
            }
            if (targetPath && shouldSkipManifestResourcePath(targetPath) && !item.bytes && !item.dataUrl && !item.base64 && typeof item.text !== "string") {
                continue;
            }
            state.resourceBytes += itemSize;
            filtered.push(item);
        }
        var wasmSupported = state.wasmSupported !== null ? state.wasmSupported : detectWasmSupport();
        var useWasm = Boolean(wasmSupported && config && config.wasmJsUrl);
        var appBundleBytes = useWasm ? (state.wasmBinaryBytes + state.wasmJsBytes) : state.jsIrBytes;
        state.totalBytes = state.resourceBytes + appBundleBytes;
        return filtered;
    }

    function resolveManifestEntries(config) {
        if (global.__GENERICUI_RESOURCE_MANIFEST__) {
            var inlineEntries = filterManifestEntries(extractManifestEntries(global.__GENERICUI_RESOURCE_MANIFEST__), config);
            if (inlineEntries.length > 0) {
                return Promise.resolve(inlineEntries);
            }
        }
        var els = getDomElements();
        if (els.manifestScript && els.manifestScript.textContent) {
            var scriptText = els.manifestScript.textContent.replace(/^\s+|\s+$/g, "");
            if (scriptText.length > 0 && scriptText.indexOf("window.__GENERICUI_RESOURCE_MANIFEST__") === -1) {
                try {
                    var parsedInline = JSON.parse(scriptText);
                    global.__GENERICUI_RESOURCE_MANIFEST__ = parsedInline;
                    return Promise.resolve(filterManifestEntries(extractManifestEntries(parsedInline), config));
                } catch (parseErr) {
                    return Promise.reject(parseErr);
                }
            }
        }
        var linkHref = (els.resourcesLink && typeof els.resourcesLink.getAttribute === "function")
            ? cleanTemplateAttr(els.resourcesLink.getAttribute("href"))
            : "";
        var manifestUrl = (config && config.resourceManifestUrl)
            ? config.resourceManifestUrl
            : (linkHref || "./resources.json");
        if (typeof global.fetch !== "function" && typeof global.XMLHttpRequest !== "function") {
            return Promise.resolve([]);
        }
        return downloadWithProgress(manifestUrl, 0, 5, STATUS_LOADING_RESOURCES, 0)
            .then(function (bytes) {
                var jsonText = bytesToUtf8(bytes);
                if (!jsonText || !jsonText.replace(/^\s+|\s+$/g, "")) {
                    return [];
                }
                var parsed = JSON.parse(jsonText);
                global.__GENERICUI_RESOURCE_MANIFEST__ = parsed;
                return filterManifestEntries(extractManifestEntries(parsed), config);
            })
            .catch(function (err) {
                if (config && config.requireResourceManifest === true) {
                    throw err;
                }
                return downloadWithProgress("./gui/genericui-resources.json", 0, 5, STATUS_LOADING_RESOURCES, 0)
                    .then(function (bytes) {
                        var jsonText = bytesToUtf8(bytes);
                        if (!jsonText || !jsonText.replace(/^\s+|\s+$/g, "")) {
                            return [];
                        }
                        var parsed = JSON.parse(jsonText);
                        return filterManifestEntries(extractManifestEntries(parsed), config);
                    })
                    .catch(function () {
                        return [];
                    });
            });
    }

    function preloadSingleResource(entry, minPct, maxPct, config) {
        if (!entry) {
            updateProgress(maxPct, STATUS_LOADING_RESOURCES);
            return Promise.resolve(null);
        }
        var entrySize = (typeof entry.size === "number" && entry.size > 0) ? entry.size : 0;
        function advanceInlineBytes(bytesLen) {
            if (state.totalBytes > 0) {
                state.loadedBytes += entrySize > 0 ? entrySize : bytesLen;
                var pct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                updateProgress(pct, STATUS_LOADING_RESOURCES);
            } else {
                updateProgress(maxPct, STATUS_LOADING_RESOURCES);
            }
        }
        if (entry.bytes instanceof Uint8Array) {
            var recBytes = registerPreloadedResourceEntry(entry, entry.bytes);
            advanceInlineBytes(entry.bytes.byteLength);
            return Promise.resolve(recBytes);
        }
        if (Array.isArray(entry.bytes)) {
            var arrBytes = new Uint8Array(entry.bytes);
            var recArr = registerPreloadedResourceEntry(entry, arrBytes);
            advanceInlineBytes(arrBytes.byteLength);
            return Promise.resolve(recArr);
        }
        if (typeof entry.dataUrl === "string" && entry.dataUrl.indexOf("data:") === 0) {
            var commaIdx = entry.dataUrl.indexOf(",");
            var payload = commaIdx >= 0 ? entry.dataUrl.substring(commaIdx + 1) : "";
            var decoded = entry.dataUrl.indexOf(";base64,") !== -1
                ? base64ToBytes(payload)
                : utf8ToBytes(decodeURIComponent(payload));
            var recDataUrl = registerPreloadedResourceEntry(entry, decoded);
            advanceInlineBytes(decoded.byteLength);
            return Promise.resolve(recDataUrl);
        }
        if (typeof entry.base64 === "string" && entry.base64.length > 0) {
            var b64Bytes = base64ToBytes(entry.base64);
            var recB64 = registerPreloadedResourceEntry(entry, b64Bytes);
            advanceInlineBytes(b64Bytes.byteLength);
            return Promise.resolve(recB64);
        }
        if (typeof entry.text === "string") {
            var txtBytes = utf8ToBytes(entry.text);
            var recText = registerPreloadedResourceEntry(entry, txtBytes);
            advanceInlineBytes(txtBytes.byteLength);
            return Promise.resolve(recText);
        }
        var rawTargetUrl = entry.path || entry.url || entry.src || entry.href;
        if (!rawTargetUrl) {
            var recEmpty = registerPreloadedResourceEntry(entry, new Uint8Array(0));
            advanceInlineBytes(0);
            return Promise.resolve(recEmpty);
        }
        var entryHash = entry.hash || entry.sha1 || entry.version || "";
        var targetUrl = buildHashedResourceUrl(rawTargetUrl, entryHash);
        return downloadWithProgress(targetUrl, minPct, maxPct, STATUS_LOADING_RESOURCES, entrySize)
            .then(function (downloadedBytes) {
                return registerPreloadedResourceEntry(entry, downloadedBytes);
            })
            .catch(function (err) {
                if (config && config.requireResourceManifest === true) {
                    throw err;
                }
                logDiagnostic("resource preload skipped for " + resolveAbsoluteUrl(targetUrl) + ": " + (err && err.message ? err.message : String(err)));
                if (global.console && typeof global.console.warn === "function") {
                    global.console.warn("[GenericUI Loader] Failed to preload resource " + resolveAbsoluteUrl(targetUrl) + "\n" + formatErrorDetails(err).stack, err);
                }
                advanceInlineBytes(0);
                return null;
            });
    }

    function preloadAllResources(config) {
        updateProgress(0, STATUS_LOADING_RESOURCES);
        return resolveManifestEntries(config).then(function (entries) {
            if (!entries || entries.length === 0) {
                if (state.totalBytes <= 0) {
                    updateProgress(50, STATUS_LOADING_RESOURCES);
                }
                return global.__GENERICUI_PRELOADED_RESOURCES__;
            }
            var count = entries.length;
            var nextIndex = 0;
            var completed = 0;
            var concurrency = Math.max(1, Math.min(6, count));

            function runWorker() {
                if (nextIndex >= count) {
                    return Promise.resolve();
                }
                var item = entries[nextIndex++];
                return preloadSingleResource(item, 5, 5, config).then(function () {
                    completed++;
                    if (state.totalBytes <= 0) {
                        updateProgress(5 + Math.floor((completed / count) * 45), STATUS_LOADING_RESOURCES);
                    }
                    return runWorker();
                });
            }

            var workers = [];
            for (var w = 0; w < concurrency; w++) {
                workers.push(runWorker());
            }
            return Promise.all(workers).then(function () {
                if (state.totalBytes > 0) {
                    var resDonePct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                    updateProgress(resDonePct, STATUS_LOADING_RESOURCES);
                } else {
                    updateProgress(50, STATUS_LOADING_RESOURCES);
                }
                return global.__GENERICUI_PRELOADED_RESOURCES__;
            });
        });
    }

    function installWasmHooks() {
        if (!global.__GENERICUI_WASM_FETCH_HOOKED__ && typeof global.fetch === "function") {
            global.__GENERICUI_WASM_FETCH_HOOKED__ = true;
            var originalFetch = global.fetch.bind(global);
            global.fetch = function (input, init) {
                var reqUrl = typeof input === "string"
                    ? input
                    : (input && typeof input.url === "string"
                        ? input.url
                        : (input && typeof input.href === "string" ? input.href : String(input || "")));
                var reqClean = reqUrl.split("?")[0];
                var reqFile = reqClean.split("/").pop();
                var isWasmRequest = /\.wasm$/i.test(reqClean);
                var cached = state.preloadedWasmBytes[reqUrl] ||
                    state.preloadedWasmBytes[reqClean] ||
                    (reqFile ? state.preloadedWasmBytes[reqFile] : null) ||
                    (isWasmRequest ? state.preloadedWasmBytes["*"] : null);
                if (cached && typeof global.Response === "function") {
                    logDiagnostic("fetch " + resolveAbsoluteUrl(reqUrl) + " -> served from preloaded Wasm bytes (" + cached.byteLength + " bytes)");
                    var slice = cached.buffer.slice(cached.byteOffset, cached.byteOffset + cached.byteLength);
                    return Promise.resolve(new global.Response(slice, {
                        status: 200,
                        headers: { "Content-Type": "application/wasm" }
                    }));
                }
                if (isWasmRequest) {
                    var preloadedNames = Object.keys(state.preloadedWasmBytes).join(", ") || "(none)";
                    logDiagnostic("fetch " + resolveAbsoluteUrl(reqUrl) + " -> not in preloaded Wasm cache (preloaded: " + preloadedNames + "); requesting from network");
                }
                function logWasmResponse(label, res) {
                    if (isWasmRequest && res) {
                        logDiagnostic(label + " " + (res.url || resolveAbsoluteUrl(reqUrl)) + " -> " + res.status + " " + (res.statusText || "") + " (" + (res.headers ? res.headers.get("Content-Type") : "?") + ")");
                    }
                    return res;
                }
                if (isWasmRequest && reqFile && reqClean.indexOf("/wasm/") === -1 && reqClean.indexOf("wasm/") !== 0) {
                    return originalFetch(input, init).then(function (res) {
                        logWasmResponse("fetch", res);
                        if (res && !res.ok) {
                            logDiagnostic("retrying Wasm binary from wasm/" + reqFile);
                            return originalFetch("wasm/" + reqFile, init).then(function (retryRes) {
                                return logWasmResponse("fetch (retry)", retryRes);
                            });
                        }
                        return res;
                    });
                }
                if (isWasmRequest) {
                    return originalFetch(input, init).then(function (res) {
                        return logWasmResponse("fetch", res);
                    });
                }
                return originalFetch(input, init);
            };
        }

        if (!global.__GENERICUI_WASM_COMPILE_HOOKED__ && typeof global.WebAssembly === "object" && global.WebAssembly !== null) {
            global.__GENERICUI_WASM_COMPILE_HOOKED__ = true;
            var wasmObj = global.WebAssembly;
            var methods = ["instantiateStreaming", "compileStreaming", "instantiate", "compile"];
            for (var i = 0; i < methods.length; i++) {
                (function (methodName) {
                    var orig = wasmObj[methodName];
                    if (typeof orig === "function") {
                        var boundOrig = orig.bind(wasmObj);
                        try {
                            wasmObj[methodName] = function () {
                                var args = arguments;
                                var source = args.length > 0 ? args[0] : undefined;
                                var sourceInfo = { method: "WebAssembly." + methodName };

                                function describeResponse(res) {
                                    if (res && typeof res === "object" && typeof res.status === "number") {
                                        sourceInfo.responseUrl = res.url || "(constructed Response, no URL)";
                                        sourceInfo.httpStatus = res.status + " " + (res.statusText || "");
                                        sourceInfo.contentType = res.headers ? res.headers.get("Content-Type") : "";
                                    }
                                }

                                if (source && typeof source.then === "function") {
                                    source.then(describeResponse, function (srcErr) {
                                        sourceInfo.sourcePromiseRejected = srcErr && srcErr.message ? srcErr.message : String(srcErr);
                                    });
                                } else if (source && typeof source.byteLength === "number") {
                                    sourceInfo.inputBytes = source.byteLength;
                                } else if (source && typeof source === "object" && typeof source.status === "number") {
                                    describeResponse(source);
                                } else if (source && typeof global.WebAssembly.Module === "function" && source instanceof global.WebAssembly.Module) {
                                    sourceInfo.input = "WebAssembly.Module";
                                }

                                logDiagnostic(sourceInfo.method + " called" + describeContext({ inputBytes: sourceInfo.inputBytes, input: sourceInfo.input }));
                                var p = boundOrig.apply(wasmObj, args);
                                if (p && typeof p.then === "function") {
                                    p.then(function () {
                                        logDiagnostic(sourceInfo.method + " succeeded" + describeContext({ responseUrl: sourceInfo.responseUrl, httpStatus: sourceInfo.httpStatus }));
                                    }, function (wasmErr) {
                                        logDiagnostic(sourceInfo.method + " failed: " + (wasmErr && wasmErr.name ? wasmErr.name + ": " : "") + (wasmErr && wasmErr.message ? wasmErr.message : String(wasmErr)) + describeContext(sourceInfo));
                                        if (wasmErr && typeof wasmErr === "object") {
                                            try {
                                                wasmErr.genericUiWasmContext = sourceInfo;
                                            } catch (annotateErr) {
                                            }
                                        }
                                    });
                                    if (state.loadingWasm) {
                                        p.catch(function () {});
                                        state.activeWasmPromises.push(p);
                                    }
                                }
                                return p;
                            };
                        } catch (e) {
                        }
                    }
                })(methods[i]);
            }
        }
    }

    function registerPreloadedWasmBinary(wasmUrl, wasmBytes) {
        if (!wasmUrl || !wasmBytes) {
            return;
        }
        installWasmHooks();
        var cleanUrl = wasmUrl.split("?")[0];
        var cleanName = cleanUrl.split("/").pop();
        state.preloadedWasmBytes[wasmUrl] = wasmBytes;
        state.preloadedWasmBytes[cleanUrl] = wasmBytes;
        state.preloadedWasmBytes["*"] = wasmBytes;
        if (cleanName) {
            state.preloadedWasmBytes[cleanName] = wasmBytes;
        }
        logDiagnostic("registered preloaded Wasm binary " + cleanName + " (" + wasmBytes.byteLength + " bytes) from " + resolveAbsoluteUrl(wasmUrl));
    }

    function snapshotGlobalPromiseExports(beforeKeys) {
        var found = [];
        try {
            var currentKeys = Object.keys(global);
            for (var i = 0; i < currentKeys.length; i++) {
                var k = currentKeys[i];
                if (!beforeKeys[k]) {
                    var val = global[k];
                    if (val && (typeof val === "object" || typeof val === "function") && typeof val.then === "function") {
                        val.catch(function () {});
                        found.push(val);
                    }
                }
            }
        } catch (e) {
        }
        return found;
    }

    function executeDownloadedScript(jsBytes, sourceUrl, isWasm) {
        return new Promise(function (resolve, reject) {
            var settled = false;
            function settleResolve() {
                if (!settled) {
                    settled = true;
                    state.onWasmAsyncFailure = null;
                    resolve();
                }
            }
            function settleReject(err) {
                if (!settled) {
                    settled = true;
                    state.onWasmAsyncFailure = null;
                    reject(err);
                }
            }

            if (isWasm) {
                state.activeWasmPromises = [];
                state.onWasmAsyncFailure = settleReject;
            }

            function finalizeExecution(beforeKeys) {
                if (!isWasm) {
                    settleResolve();
                    return;
                }
                var exportedPromises = snapshotGlobalPromiseExports(beforeKeys);
                var allPromises = state.activeWasmPromises.concat(exportedPromises);
                logDiagnostic("executed " + resolveAbsoluteUrl(sourceUrl) + "; awaiting " + allPromises.length + " Wasm initialization promise(s)");
                if (allPromises.length > 0) {
                    Promise.all(allPromises).then(function () {
                        logDiagnostic("Kotlin/Wasm module initialized successfully");
                        settleResolve();
                    }).catch(function (wasmErr) {
                        settleReject(createLoaderError(
                            "Kotlin/Wasm module failed to initialize after executing " + resolveAbsoluteUrl(sourceUrl),
                            wasmErr,
                            { phase: "Wasm initialization", glueScript: resolveAbsoluteUrl(sourceUrl) }
                        ));
                    });
                } else {
                    settleResolve();
                }
            }

            try {
                var code = bytesToUtf8(jsBytes);
                var els = getDomElements();
                if (!els.doc) {
                    settleResolve();
                    return;
                }

                var beforeKeys = {};
                if (isWasm) {
                    try {
                        var keys = Object.keys(global);
                        for (var i = 0; i < keys.length; i++) {
                            beforeKeys[keys[i]] = true;
                        }
                    } catch (e) {
                    }
                }

                var cleanUrl = sourceUrl.split("?")[0];
                var isEsModule = /\.mjs$/i.test(cleanUrl) ||
                    /(?:^|\n)\s*(?:import|export)\s+/m.test(code);

                if (isWasm && !isEsModule && sourceUrl) {
                    var wasmGlueScript = els.doc.createElement("script");
                    wasmGlueScript.src = sourceUrl;
                    wasmGlueScript.onload = function () {
                        finalizeExecution(beforeKeys);
                    };
                    wasmGlueScript.onerror = function () {
                        settleReject(createLoaderError(
                            "Failed to load Wasm glue script " + resolveAbsoluteUrl(sourceUrl) + " via <script src> (HTTP error, blocked by CSP, or network failure)",
                            null,
                            { phase: "Wasm glue script", scriptUrl: resolveAbsoluteUrl(sourceUrl) }
                        ));
                    };
                    logDiagnostic("executing Wasm glue script " + resolveAbsoluteUrl(sourceUrl) + " via <script src> (keeps webpack publicPath at the script's directory)");
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(wasmGlueScript);
                    return;
                }

                if (isEsModule && typeof global.Blob === "function" && global.URL && typeof global.URL.createObjectURL === "function") {
                    var blob = new global.Blob([code], { type: "text/javascript" });
                    var blobUrl = global.URL.createObjectURL(blob);
                    var modScript = els.doc.createElement("script");
                    modScript.type = "module";
                    modScript.src = blobUrl;
                    modScript.onload = function () {
                        global.URL.revokeObjectURL(blobUrl);
                        finalizeExecution(beforeKeys);
                    };
                    modScript.onerror = function () {
                        global.URL.revokeObjectURL(blobUrl);
                        settleReject(createLoaderError(
                            "Failed to execute ES module script " + resolveAbsoluteUrl(sourceUrl),
                            null,
                            { phase: isWasm ? "Wasm glue script" : "JS bundle", scriptUrl: resolveAbsoluteUrl(sourceUrl), bytes: jsBytes ? jsBytes.byteLength : 0 }
                        ));
                    };
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(modScript);
                    return;
                }

                if (code.length > 0) {
                    var inlineScript = els.doc.createElement("script");
                    inlineScript.type = "text/javascript";
                    inlineScript.text = code + "\n//# sourceURL=" + sourceUrl;
                    logDiagnostic("executing " + (isWasm ? "Wasm glue" : "JS bundle") + " " + resolveAbsoluteUrl(sourceUrl) + " inline (" + jsBytes.byteLength + " bytes)");
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(inlineScript);
                    finalizeExecution(beforeKeys);
                } else {
                    var extScript = els.doc.createElement("script");
                    extScript.src = sourceUrl;
                    extScript.onload = function () {
                        finalizeExecution(beforeKeys);
                    };
                    extScript.onerror = function () {
                        settleReject(createLoaderError(
                            "Failed to load script " + resolveAbsoluteUrl(sourceUrl) + " (downloaded body was empty and <script src> also failed)",
                            null,
                            { phase: isWasm ? "Wasm glue script" : "JS bundle", scriptUrl: resolveAbsoluteUrl(sourceUrl) }
                        ));
                    };
                    (els.doc.body || els.doc.head || els.doc.documentElement).appendChild(extScript);
                }
            } catch (err) {
                settleReject(createLoaderError(
                    "Exception while executing " + resolveAbsoluteUrl(sourceUrl),
                    err,
                    { phase: isWasm ? "Wasm glue script" : "JS bundle", scriptUrl: resolveAbsoluteUrl(sourceUrl) }
                ));
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

        var rawWasmSrc = readTagAttr(wasmTag, "src");

        var wasmJsUrl = cfg.wasmJsUrl ||
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

        if (rawWasmSrc) {
            var cleanWasmSrc = rawWasmSrc.split("?")[0].split("#")[0];
            if (/\.wasm$/i.test(cleanWasmSrc)) {
                if (!wasmBinaryUrl) {
                    wasmBinaryUrl = rawWasmSrc;
                }
                if (!wasmJsUrl && jsIrUrl) {
                    wasmJsUrl = jsIrUrl;
                }
            } else if (!wasmJsUrl) {
                wasmJsUrl = rawWasmSrc;
            }
        }

        var resourcesLink = els.resourcesLink;
        var resourceManifestUrl = cfg.resourceManifestUrl ||
            readTagAttr(resourcesLink, "href") ||
            readTagAttr(scriptEl, "data-resource-manifest-url") ||
            readTagAttr(splashEl, "data-resource-manifest-url") ||
            "./resources.json";

        var fadeDurationMs = typeof cfg.fadeDurationMs === "number"
            ? cfg.fadeDurationMs
            : (parseInt(readTagAttr(scriptEl, "data-fade-duration-ms") || readTagAttr(splashEl, "data-fade-duration-ms"), 10) || 350);

        return {
            wasmJsUrl: wasmJsUrl,
            wasmBinaryUrl: wasmBinaryUrl,
            jsIrUrl: jsIrUrl,
            resourceManifestUrl: resourceManifestUrl,
            requireResourceManifest: cfg.requireResourceManifest === true,
            fadeDurationMs: fadeDurationMs
        };
    }

    function loadJsIrFallback(config, statusOverride, priorWasmError) {
        if (!config.jsIrUrl) {
            throw createLoaderError(
                "Neither a supported WebAssembly bundle nor a fallback JS IR bundle URL was configured.",
                priorWasmError || null,
                { wasmJsUrl: config.wasmJsUrl, wasmBinaryUrl: config.wasmBinaryUrl, jsIrUrl: config.jsIrUrl }
            );
        }
        var statusMsg = statusOverride || STATUS_LOADING_APPLICATION;
        state.loadingWasm = false;
        state.runtimeTarget = "js-ir";
        global.__GENERICUI_WEB_RUNTIME__ = "js-ir";
        logDiagnostic("loading JS IR bundle " + resolveAbsoluteUrl(config.jsIrUrl) + (priorWasmError ? " (fallback after WebAssembly failure)" : ""));
        if (state.resourceBytes + state.jsIrBytes > 0) {
            state.totalBytes = state.resourceBytes + state.jsIrBytes;
            state.loadedBytes = state.resourceBytes;
            var jsStartPct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
            updateProgress(jsStartPct, statusMsg);
        } else {
            updateProgress(50, statusMsg);
        }
        return downloadWithProgress(config.jsIrUrl, 55, 95, statusMsg, state.jsIrBytes)
            .then(function (jsBytes) {
                updateProgress(99, statusMsg);
                return executeDownloadedScript(jsBytes, config.jsIrUrl, false);
            })
            .catch(function (jsErr) {
                if (priorWasmError) {
                    var combined = createLoaderError(
                        "Both the WebAssembly bundle and the JS fallback bundle failed to load",
                        jsErr,
                        { jsIrUrl: resolveAbsoluteUrl(config.jsIrUrl), wasmJsUrl: resolveAbsoluteUrl(config.wasmJsUrl || "") }
                    );
                    combined.suppressed = [priorWasmError];
                    throw combined;
                }
                throw jsErr;
            });
    }

    function loadWasmPrimaryWithFallback(config) {
        var canFallback = Boolean(config.jsIrUrl && config.jsIrUrl !== config.wasmJsUrl);
        state.canFallbackToJs = canFallback;
        logDiagnostic("config" + describeContext({
            wasmJsUrl: config.wasmJsUrl ? resolveAbsoluteUrl(config.wasmJsUrl) : "",
            wasmBinaryUrl: config.wasmBinaryUrl ? resolveAbsoluteUrl(config.wasmBinaryUrl) : "",
            jsIrUrl: config.jsIrUrl ? resolveAbsoluteUrl(config.jsIrUrl) : "",
            canFallbackToJs: canFallback
        }));
        var wasmSupported = state.wasmSupported !== null ? state.wasmSupported : detectWasmSupport();
        logDiagnostic("WebAssembly + WasmGC support detected: " + wasmSupported);
        if (wasmSupported && config.wasmJsUrl) {
            installWasmHooks();
            state.loadingWasm = true;
            state.runtimeTarget = "wasm";
            global.__GENERICUI_WEB_RUNTIME__ = "wasm";
            if (state.totalBytes > 0) {
                var wasmStartPct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                updateProgress(wasmStartPct, STATUS_LOADING_APPLICATION);
            } else {
                updateProgress(50, STATUS_LOADING_APPLICATION);
            }

            var wasmChain = Promise.resolve();
            if (config.wasmBinaryUrl) {
                wasmChain = downloadWithProgress(config.wasmBinaryUrl, 50, 80, STATUS_LOADING_APPLICATION, state.wasmBinaryBytes)
                    .then(function (wasmBytes) {
                        registerPreloadedWasmBinary(config.wasmBinaryUrl, wasmBytes);
                    });
            }

            return wasmChain
                .then(function () {
                    var startPct = config.wasmBinaryUrl ? 80 : 55;
                    return downloadWithProgress(config.wasmJsUrl, startPct, 95, STATUS_LOADING_APPLICATION, state.wasmJsBytes);
                })
                .then(function (wasmJsBytes) {
                    updateProgress(99, STATUS_LOADING_APPLICATION);
                    return executeDownloadedScript(wasmJsBytes, config.wasmJsUrl, true);
                })
                .then(function () {
                    state.loadingWasm = false;
                })
                .catch(function (wasmErr) {
                    state.loadingWasm = false;
                    logWasmFallbackErrorSilently(wasmErr);
                    if (canFallback && !state.hasError) {
                        state.currentProgress = state.totalBytes > 0
                            ? Math.min(99, Math.round((state.resourceBytes / Math.max(1, state.resourceBytes + state.jsIrBytes)) * 100))
                            : 50;
                        updateProgress(state.currentProgress, STATUS_LOADING_FALLBACK);
                        return loadJsIrFallback(config, STATUS_LOADING_FALLBACK, wasmErr);
                    }
                    throw wasmErr;
                });
        }

        if (config.wasmJsUrl && canFallback) {
            var unsupportedErr = createLoaderError(
                "WebAssembly (with WasmGC) is not supported in this browser; loading JS fallback.",
                null,
                { userAgent: global.navigator ? global.navigator.userAgent : "" }
            );
            logWasmFallbackErrorSilently(unsupportedErr);
            state.currentProgress = state.totalBytes > 0
                ? Math.min(99, Math.round((state.resourceBytes / Math.max(1, state.resourceBytes + state.jsIrBytes)) * 100))
                : 50;
            updateProgress(state.currentProgress, STATUS_LOADING_FALLBACK);
            return loadJsIrFallback(config, STATUS_LOADING_FALLBACK, unsupportedErr);
        }

        return loadJsIrFallback(config, STATUS_LOADING_APPLICATION, null);
    }

    function errorFromEvent(event) {
        if (event && event.error) {
            var e = event.error;
            if (e && typeof e === "object" && !e.genericUiContext && (event.filename || event.lineno)) {
                try {
                    e.genericUiContext = {
                        source: (event.filename || "?") + ":" + (event.lineno || 0) + ":" + (event.colno || 0)
                    };
                } catch (annotateErr) {
                }
            }
            return e;
        }
        return createLoaderError(
            (event && event.message ? event.message : "Unhandled runtime error (no error object; possibly a cross-origin script error)"),
            null,
            {
                source: event && event.filename ? event.filename + ":" + (event.lineno || 0) + ":" + (event.colno || 0) : "",
                target: event && event.target && event.target.tagName ? event.target.tagName + " " + (event.target.src || event.target.href || "") : ""
            }
        );
    }

    function installGlobalErrorListeners() {
        if (typeof global.addEventListener !== "function") {
            return;
        }
        global.addEventListener("error", function (event) {
            var errObj = errorFromEvent(event);
            logDiagnostic("window error event: " + (errObj && errObj.message ? errObj.message : String(errObj)));
            if (state.loadingWasm && state.canFallbackToJs && typeof state.onWasmAsyncFailure === "function") {
                if (event && typeof event.preventDefault === "function") {
                    event.preventDefault();
                }
                state.onWasmAsyncFailure(errObj);
                return;
            }
            showError(errObj, STATUS_ERROR);
        });
        global.addEventListener("unhandledrejection", function (event) {
            var reason = event && event.reason !== undefined && event.reason !== null
                ? event.reason
                : createLoaderError("Unhandled Promise rejection with no reason value", null, null);
            logDiagnostic("unhandled rejection: " + (reason && reason.message ? reason.message : String(reason)));
            if (state.loadingWasm && state.canFallbackToJs && typeof state.onWasmAsyncFailure === "function") {
                if (event && typeof event.preventDefault === "function") {
                    event.preventDefault();
                }
                state.onWasmAsyncFailure(reason);
                return;
            }
            showError(reason, STATUS_ERROR);
        });
    }

    function ensureRootContainer() {
        var doc = global.document;
        if (!doc) {
            return null;
        }
        var root = (typeof doc.getElementsByTagName === "function" && doc.getElementsByTagName("genericui:app")[0]) ||
            (typeof doc.querySelector === "function" ? doc.querySelector("genericui\\:app") : null);
        if (!root && doc.body && typeof doc.createElement === "function") {
            root = doc.createElement("genericui:app");
            doc.body.appendChild(root);
        }
        if (root && typeof root.removeAttribute === "function") {
            root.removeAttribute("id");
            root.removeAttribute("data-bs-theme");
        }
        return root;
    }

    function registerServiceWorkerIfEnabled() {
        var doc = global.document;
        if (!doc || !global.navigator || !("serviceWorker" in global.navigator)) {
            return;
        }
        var manifestLink = typeof doc.getElementById === "function" ? doc.getElementById("genericui-web-manifest") : null;
        if (manifestLink && global.navigator.serviceWorker && typeof global.navigator.serviceWorker.register === "function") {
            try {
                var p = global.navigator.serviceWorker.register('./gui/genericui-sw.js');
                if (p && typeof p.catch === "function") {
                    p.catch(function () {});
                }
            } catch (e) {
            }
        }
    }

    function startLoader() {
        applyInitialBrowserTheme();
        installGlobalErrorListeners();
        ensureRootContainer();
        registerServiceWorkerIfEnabled();
        var config = resolveLoaderConfig();

        updateProgress(0, STATUS_INITIALIZING);
        state.wasmSupported = detectWasmSupport();
        return preloadAllResources(config)
            .then(function () {
                if (!config.wasmJsUrl && !config.jsIrUrl) {
                    return Promise.resolve();
                }
                if (state.totalBytes <= 0) {
                    updateProgress(50, STATUS_LOADING_APPLICATION);
                } else {
                    var afterResPct = Math.min(99, Math.round((state.loadedBytes / state.totalBytes) * 100));
                    updateProgress(afterResPct, STATUS_LOADING_APPLICATION);
                }
                return loadWasmPrimaryWithFallback(config);
            })
            .catch(function (err) {
                showError(err, STATUS_ERROR);
            });
    }

    global.GenericUiSplash = {
        STATUS_INITIALIZING: STATUS_INITIALIZING,
        STATUS_LOADING_RESOURCES: STATUS_LOADING_RESOURCES,
        STATUS_LOADING_APPLICATION: STATUS_LOADING_APPLICATION,
        STATUS_LOADING_FALLBACK: STATUS_LOADING_FALLBACK,
        STATUS_ERROR: STATUS_ERROR,
        STATUS_READY: STATUS_READY,
        ALLOWED_STATUS_MESSAGES: [
            STATUS_INITIALIZING,
            STATUS_LOADING_RESOURCES,
            STATUS_LOADING_APPLICATION,
            STATUS_LOADING_FALLBACK,
            STATUS_ERROR,
            STATUS_READY
        ],
        normalizeStatusMessage: normalizeStatusMessage,
        detectWasmSupport: detectWasmSupport,
        ensureRootContainer: ensureRootContainer,
        preloadAllResources: preloadAllResources,
        buildHashedResourceUrl: buildHashedResourceUrl,
        registerPreloadedResource: registerPreloadedResourceEntry,
        getPreloadedResource: getPreloadedResource,
        updateProgress: updateProgress,
        setStatus: setStatus,
        setProgress: setProgress,
        setError: setError,
        setApplicationStatus: setApplicationStatus,
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
