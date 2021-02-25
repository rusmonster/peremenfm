package fm.peremen.android

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import timber.log.Timber

class PeremenApplication : Application(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        PeremenManager.setup(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onBackground() {
        if (PeremenManager.isStarted) {
            PeremenService.start(this)
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onForeground() {
        PeremenService.stop(this)
    }
}
