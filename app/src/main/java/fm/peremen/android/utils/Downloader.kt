@file:Suppress("BlockingMethodInNonBlockingContext")

package fm.peremen.android.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

suspend fun downloadFileToCache(context: Context, url: String, name: String) = withContext(Dispatchers.IO) {
    URL(url).openStream().use { inputStream ->
        context.openFileOutput(name, MODE_PRIVATE).use { fileOutputStream ->
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
