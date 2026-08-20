package app.footyos.reminders

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val id = intent.getIntExtra(extraNotificationId, 100)
        val title = intent.getStringExtra(extraTitle).orEmpty()
        val body = intent.getStringExtra(extraBody).orEmpty()
        val deepLink = intent.getStringExtra(extraDeepLink) ?: "footyos://open/today"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(ReminderScheduler.contentIntent(context, id, deepLink))
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    companion object {
        const val channelId = "footyos_reminders"
        const val extraTitle = "title"
        const val extraBody = "body"
        const val extraDeepLink = "deep_link"
        const val extraNotificationId = "notification_id"
    }
}
