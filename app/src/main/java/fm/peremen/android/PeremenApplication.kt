package fm.peremen.android

import android.app.Application
import timber.log.Timber

class PeremenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
