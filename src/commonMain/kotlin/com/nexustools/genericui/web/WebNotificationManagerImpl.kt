package com.nexustools.genericui.web

import com.nexustools.genericui.component.system.Notification
import com.nexustools.genericui.component.system.NotificationChannel
import com.nexustools.genericui.nativeimpl.system.NotificationManagerNativeImpl

/**
 * Web `window.Notification` / ServiceWorker notification implementation of [NotificationManagerNativeImpl].
 */
class WebNotificationManagerImpl : NotificationManagerNativeImpl {
    private val channels = mutableListOf<NotificationChannel>()
    private val activeNotifications = mutableListOf<Notification>()

    override fun registerChannel(channel: NotificationChannel) {
        channels.add(channel)
    }

    override fun showNotification(notification: Notification, onActionClicked: ((String) -> Unit)?) {
        activeNotifications.add(notification)
    }

    override fun cancelNotification(notificationId: String) {}

    override fun dispose() {
        activeNotifications.clear()
    }
}
