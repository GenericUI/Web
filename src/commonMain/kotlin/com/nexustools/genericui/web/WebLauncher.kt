package com.nexustools.genericui.web

import com.nexustools.genericui.component.shell.IApplication
import com.nexustools.genericui.core.GenericUiAppDescriptor
import com.nexustools.genericui.core.GenericUiLaunchSequence

/**
 * Platform launcher for the Web (`js` + `wasmJs`) (`:web`) backend target.
 */
object WebLauncher {

    /**
     * Prepares the [IApplication] for the Web backend without starting the event loop.
     * Command-line arguments are not forwarded on Web (`isSystemTarget = false`).
     */
    fun prepare(descriptor: GenericUiAppDescriptor): IApplication =
        GenericUiLaunchSequence.prepareApplication(
            factory = WebNativeFactory(),
            args = emptyArray(),
            descriptor = descriptor,
            isSystemTarget = false
        )

    /**
     * Overload accepting [args] for uniform generated entrypoint invocation while ignoring
     * command-line arguments on Web (`isSystemTarget = false`).
     */
    fun prepare(
        args: Array<String>,
        descriptor: GenericUiAppDescriptor
    ): IApplication = prepare(descriptor)

    /**
     * Prepares and launches the [IApplication] on the Web backend (`isSystemTarget = false`).
     */
    fun launch(descriptor: GenericUiAppDescriptor): IApplication =
        GenericUiLaunchSequence.launch(
            factory = WebNativeFactory(),
            args = emptyArray(),
            descriptor = descriptor,
            isSystemTarget = false
        )

    /**
     * Overload accepting [args] for uniform generated entrypoint invocation while ignoring
     * command-line arguments on Web (`isSystemTarget = false`).
     */
    fun launch(
        args: Array<String>,
        descriptor: GenericUiAppDescriptor
    ): IApplication = launch(descriptor)
}
