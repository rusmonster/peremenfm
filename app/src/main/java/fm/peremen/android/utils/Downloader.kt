@file:Suppress("BlockingMethodInNonBlockingContext")

package fm.peremen.android.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.yield
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

suspend fun downloadFileToCache(context: Context, url: String, name: String) = withCancellableContext(Dispatchers.IO) {
    URL(url).openStream().use { inputStream ->
        yield()
        context.openFileOutput(name, MODE_PRIVATE).use { fileOutputStream ->
            Timber.d("Copy streams...")
            inputStream.copyTo(fileOutputStream)
        }
    }
}

/**
 * Copy-pasted from InputStream.copyTo() from standard library with adding yield() to make it cancellable
 */
private suspend fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long {
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    var bytes = read(buffer)
    while (bytes >= 0) {
        yield()
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = read(buffer)
    }
    return bytesCopied
}
