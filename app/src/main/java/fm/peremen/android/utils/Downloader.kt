package fm.peremen.android.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun downloadFileToCache(context: Context, url: String, name: String) = withContext(Dispatchers.IO) {
    URL(url).openStream().use { inputStream ->
        context.openFileOutput(name, MODE_PRIVATE).use { fileOutputStream ->
            inputStream.copyTo(fileOutputStream)
        }
    }
}
