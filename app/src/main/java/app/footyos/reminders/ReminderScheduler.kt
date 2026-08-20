package app.footyos.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.footyos.MainActivity

data class ReminderRequest(
    val id: Int,
    val title: String,
    val body: String,
    val triggerAtMillis: Long,
    val deepLink: String,
)

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                ReminderReceiver.channelId,
                "Training reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun schedule(request: ReminderRequest) {
        val receiverIntent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.extraTitle, request.title)
            putExtra(ReminderReceiver.extraBody, request.body)
            putExtra(ReminderReceiver.extraDeepLink, request.deepLink)
            putExtra(ReminderReceiver.extraNotificationId, request.id)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            request.id,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            request.triggerAtMillis,
            pending,
        )
    }

    companion object {
        fun contentIntent(context: Context, id: Int, deepLink: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(deepLink)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            return PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
