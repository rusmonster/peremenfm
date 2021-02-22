package fm.peremen.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

private const val NOTIFICATION_CHANNEL_ID = "peremen_notification_id"
private const val NOTIFICATION_NAME = "PeremenFM"
private const val NOTIFICATION_ID = 12345

class PeremenService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onCreate() {
        super.onCreate()
        Timber.d("PeremenService onCreate")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notificationBuilder)
    }

    companion object {

        fun start(context: Context) = ContextCompat.startForegroundService(context, createIntent(context))

        fun stop(context: Context) = context.stopService(createIntent(context))

        private fun createIntent(context: Context) = Intent(context, PeremenService::class.java)
    }
}
