package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.error.*

// allParallelly: Check if all elements match the predicate in parallel / 并行检查所有元素是否匹配谓词
suspend inline fun <T> Iterable<T>.allParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return try {
        coroutineScope {
            val channel = Channel<Boolean>()
            for (element in this@allParallelly.iterator()) {
                launch(Dispatchers.Default) {
                    channel.send(predicate(element))
                }
            }
            for (value in channel) {
                if (!value) {
                    cancel()
                }
            }
            true
        }
    } catch (e: CancellationException) {
        false
    }
}

// tryAllParallelly: Try version of allParallelly / allParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryAllParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val channel = Channel<Ret<Boolean>>()
            for (element in this@tryAllParallelly.iterator()) {
                launch(Dispatchers.Default) {
                    channel.send(predicate(element))
                }
            }
            for (result in channel) {
                when (result) {
                    is Ok -> {
                        if (!result.value) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                    }
                }
            }

            Ok(true)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(false)
    }
}

// anyParallelly: Check if any element matches the predicate in parallel / 并行检查是否有元素匹配谓词
suspend inline fun <T> Iterable<T>.anyParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return try {
        coroutineScope {
            val channel = Channel<Boolean>()
            for (element in this@anyParallelly.iterator()) {
                launch(Dispatchers.Default) {
                    channel.send(predicate(element))
                }
            }
            for (value in channel) {
                if (value) {
                    cancel()
                }
            }
            false
        }
    } catch (e: CancellationException) {
        true
    }
}

// tryAnyParallelly: Try version of anyParallelly / anyParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryAnyParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val channel = Channel<Ret<Boolean>>()
            for (element in this@tryAnyParallelly.iterator()) {
                launch(Dispatchers.Default) {
                    channel.send(predicate(element))
                }
            }
            for (result in channel) {
                when (result) {
                    is Ok -> {
                        if (result.value) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                    }
                }
            }

            Ok(false)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(true)
    }
}

// noneParallelly: Check if no element matches the predicate in parallel / 并行检查是否没有元素匹配谓词
suspend inline fun <T> Iterable<T>.noneParallelly(
    crossinline predicate: SuspendPredicate<T>
): Boolean {
    return try {
        coroutineScope {
            val channel = Channel<Boolean>()
            for (element in this@noneParallelly.iterator()) {
                launch(Dispatchers.Default) {
                    channel.send(predicate(element))
                }
            }
            for (value in channel) {
                if (value) {
                    cancel()
                }
            }
            true
        }
    } catch (e: CancellationException) {
        false
    }
}

// tryNoneParallelly: Try version of noneParallelly / noneParallelly 的 try 版本
suspend inline fun <T> Iterable<T>.tryNoneParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Boolean> {
    var error: Error? = null

    return try {
        coroutineScope {
            val channel = Channel<Ret<Boolean>>()
            for (element in this@tryNoneParallelly.iterator()) {
                launch(Dispatchers.Default) {
                    channel.send(predicate(element))
                }
            }
            for (result in channel) {
                when (result) {
                    is Ok -> {
                        if (result.value) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                    }
                }
            }

            Ok(true)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(false)
    }
}