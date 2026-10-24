package com.nexustools.genericui.web

import com.nexustools.genericui.enums.system.PermissionStatus
import com.nexustools.genericui.enums.system.PermissionType
import com.nexustools.genericui.nativeimpl.system.PermissionManagerNativeImpl

/**
 * Web `navigator.permissions` implementation of [PermissionManagerNativeImpl].
 */
class WebPermissionManagerImpl : PermissionManagerNativeImpl {
    private val statuses = mutableMapOf<PermissionType, PermissionStatus>()

    override fun checkPermission(permission: PermissionType): PermissionStatus =
        statuses[permission] ?: PermissionStatus.GRANTED

    override fun requestPermission(permission: PermissionType, onResult: (PermissionStatus) -> Unit) {
        val status = statuses.getOrPut(permission) { PermissionStatus.GRANTED }
        onResult(status)
    }

    override fun dispose() {}
}
