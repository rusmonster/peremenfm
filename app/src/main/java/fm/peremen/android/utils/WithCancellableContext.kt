package fm.peremen.android.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.coroutines.CoroutineContext

// Makes it possible to keep coroutine immediately cancellable while running blocking code
suspend inline fun <R> withCancellableContext(
        context: CoroutineContext,
        crossinline block: suspend CoroutineScope.() -> R
): R {
    val job = GlobalScope.async(context) { runCatching { block() } }
    val result = try {
        job.await()
    } catch (e: CancellationException) {
        job.cancel()
        throw e
    }
    return result.getOrThrow()
}
