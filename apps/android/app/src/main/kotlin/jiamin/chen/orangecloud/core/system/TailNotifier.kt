package jiamin.chen.orangecloud.core.system

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jiamin.chen.orangecloud.MainActivity
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Workers tail 实时日志通知（对应 iOS tail Live Activity）。
 * 连接期间常驻通知显示事件数 + 最新行；Android 16（API 36）请求促升为实况通知，低版本常规常驻。
 *
 * 受设置页通知偏好约束：仅当「通知」总开关与「Worker 错误」均开启、且系统已授予
 * POST_NOTIFICATIONS 时才推送（偏好从 DataStore 流式缓存，供非挂起的 [update] 同步读取）。
 */
@Singleton
class TailNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    appPrefs: AppPrefs,
    @ApplicationScope externalScope: CoroutineScope,
) {
    private val manager = NotificationManagerCompat.from(context)

    @Volatile private var notificationsEnabled = false
    @Volatile private var workerErrorsEnabled = true

    init {
        externalScope.launch { appPrefs.notificationsEnabled.collect { notificationsEnabled = it } }
        externalScope.launch { appPrefs.notifyWorkerErrors.collect { workerErrorsEnabled = it } }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.tail_title), NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    // hasPermission() 已确认 POST_NOTIFICATIONS；notify() 另有 runCatching 兜底 SecurityException。
    @SuppressLint("MissingPermission")
    fun update(scriptName: String, eventCount: Int, lastLine: String, connected: Boolean) {
        if (!notificationsEnabled || !workerErrorsEnabled) return
        if (!hasPermission()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("orangecloud://open/workers")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val statusRes = if (connected) R.string.tail_connected else R.string.tail_disconnected
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(scriptName)
            .setContentText(lastLine.ifBlank { context.getString(R.string.tail_events, eventCount) })
            .setSubText(context.getString(statusRes))
            .setOngoing(connected)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pending)
        // TODO(API36): Notification.Builder.requestPromotedOngoing()/setShortCriticalText 促升为实况通知（NotificationCompat 暂未透出）。

        runCatching { manager.notify(NOTIFICATION_ID, builder.build()) }
    }

    fun cancel() {
        manager.cancel(NOTIFICATION_ID)
    }

    private companion object {
        const val CHANNEL_ID = "tail"
        const val NOTIFICATION_ID = 4201
    }
}
