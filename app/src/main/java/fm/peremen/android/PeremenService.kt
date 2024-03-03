package fm.peremen.android

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import fm.peremen.android.utils.getNotificationReadableStatus
import fm.peremen.android.utils.isPlaybackStarted
import timber.log.Timber

private const val NOTIFICATION_CHANNEL_ID = "fm.peremen.notification_channel"
private const val NOTIFICATION_NAME = "PeremenFM"
private const val NOTIFICATION_ID = 12345

private const val ACTION_PLAY = "fm.peremen.action.play"
private const val ACTION_PAUSE = "fm.peremen.action.pause"
private const val ACTION_STOP = "fm.peremen.action.stop"

class PeremenService : Service() {

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = createNotification();

        if (Build.VERSION.SDK_INT >= 29)
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
        else
            startForeground(NOTIFICATION_ID, notification)

        PeremenManager.onChanged += this::onManagerChanged
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        PeremenManager.onChanged -= this::onManagerChanged
    }

    private fun onManagerChanged() {
        updateNotification()
        sharedPreferences.isPlaybackStarted = PeremenManager.isStarted
    }

    private fun updateNotification() {
        Timber.d("updateNotification")
        val notificationManager = NotificationManagerCompat.from(this)
        val notification = createNotification()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(PeremenManager.getNotificationReadableStatus(this))
                .setSmallIcon(R.drawable.ic_notification)
                .setNotificationSilent()
                .setContentIntent(pendingIntent)

        if (PeremenManager.isStarted) {
            val intent = Intent(ACTION_PAUSE).apply { setClass(this@PeremenService, PeremenService::class.java) }
            val pauseIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val pauseTitle = getString(R.string.notification_action_pause)
            notificationBuilder.addAction(R.drawable.ic_baseline_pause_circle_outline_24, pauseTitle, pauseIntent)
        } else {
            val intent = Intent(ACTION_PLAY).apply { setClass(this@PeremenService, PeremenService::class.java) }
            val playIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val playTitle = getString(R.string.notification_action_play)
            notificationBuilder.addAction(R.drawable.ic_baseline_play_circle_outline_24, playTitle, playIntent)
        }

        val intent = Intent(ACTION_STOP).apply { setClass(this@PeremenService, PeremenService::class.java) }
        val stopIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val stopTitle = getString(R.string.notification_action_stop)
        notificationBuilder.addAction(R.drawable.ic_baseline_stop_circle_24, stopTitle, stopIntent)

        return notificationBuilder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand: ${intent?.action}")

        if (intent == null && sharedPreferences.isPlaybackStarted) { // service has been restarted after process is killed.
            Timber.d("onStartCommand: process restarted, restarting playback...")
            PeremenManager.start()
        }

        when (intent?.action) {

            ACTION_PLAY -> PeremenManager.start()

            ACTION_PAUSE -> PeremenManager.stop()

            ACTION_STOP -> {
                PeremenManager.stop()
                stopSelf()
            }
        }

        return START_STICKY
    }

    companion object {

        fun start(context: Context) = ContextCompat.startForegroundService(context, createIntent(context))

        fun stop(context: Context) = context.stopService(createIntent(context))

        private fun createIntent(context: Context) = Intent(context, PeremenService::class.java)
    }
}
