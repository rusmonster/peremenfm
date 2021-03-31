package fm.peremen.android.timeengine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import fm.peremen.android.utils.hasGpsPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

class GpsTimeSource(id: Int, private val context: Context) : AccuracyTimeSource(id) {

    @SuppressLint("MissingPermission")
    override suspend fun timeRequestFlow(): Flow<TimeRequestResult> = callbackFlow {
        while (!context.hasGpsPermission()) delay(1000)

        Timber.d("Requesting GPS location...")

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Timber.d("onLocationChanged provider: ${location.provider}; acc: ${location.accuracy}; time: ${location.time}; systemTimDiff: ${System.currentTimeMillis() - location.time}")
                runCatching { offer(TimeRequestResult(location.time - SystemClock.elapsedRealtime(), location.accuracy.toDouble())) }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Timber.d("onStatusChanged: { provide: $provider; status: $status }")
            }

            override fun onProviderEnabled(provider: String) {
                Timber.d("onProviderEnabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Timber.d("onProviderDisabled: $provider")
            }
        }

        val mgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mgr.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L,
            0.0f,
            listener,
            null
        )
        awaitClose { mgr.removeUpdates(listener) }
    }
}
