package com.github.yuonsa.commitgenius.core.notification

import com.github.yuonsa.commitgenius.NotificationBundle
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.PropertyKey

/**
 * {@link Notifier}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
object Notifier {

    private val GROUP = NotificationGroupManager
        .getInstance()
        .getNotificationGroup("com.github.yuonsa.commitgenius.notificationGroup")

    fun info(
        @PropertyKey(resourceBundle = NotificationBundle.BUNDLE) contentKey: String,
        project: Project? = null
    ) {
        notify(contentKey, null, NotificationType.INFORMATION, project)
    }

    fun warn(
        @PropertyKey(resourceBundle = NotificationBundle.BUNDLE) contentKey: String,
        project: Project? = null
    ) {
        notify(contentKey, null, NotificationType.WARNING, project)
    }

    fun error(
        @PropertyKey(resourceBundle = NotificationBundle.BUNDLE) contentKey: String,
        project: Project? = null
    ) {
        notify(contentKey, null, NotificationType.ERROR, project)
    }

    fun notify(
        @PropertyKey(resourceBundle = NotificationBundle.BUNDLE) contentKey: String,
        @PropertyKey(resourceBundle = NotificationBundle.BUNDLE) titleKey: String? = null,
        type: NotificationType = NotificationType.INFORMATION,
        project: Project? = null,
    ) {
        val content = NotificationBundle[contentKey]
        val title = titleKey?.let { NotificationBundle[it] }
        notifyText(content, title, type, project)
    }

    fun notifyText(
        content: String,
        title: String? = null,
        type: NotificationType = NotificationType.INFORMATION,
        project: Project? = null,
        block: (Notification.() -> Unit)? = null
    ) {
        val notification = GROUP.createNotification(title ?: "", content, type)
        block?.invoke(notification)
        notification.notify(project)
    }
}
