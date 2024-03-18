package fuookami.ospf.kotlin.utils.parallel

import kotlin.reflect.full.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

val Collection<*>.defaultConcurrentAmount: UInt64
    get() = UInt64(
        maxOf(
            minOf(
                Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                Runtime.getRuntime().availableProcessors()
            ),
            1
        )
    )

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

suspend inline fun <T> Iterable<T>.countParallelly(
    crossinline predicate: SuspendPredicate<T>
): Int {
    return coroutineScope {
        val promises = ArrayList<Deferred<Boolean>>()
        for (element in this@countParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) { predicate(element) })
        }
        promises.count { it.await() }
    }
}

suspend inline fun <T> Iterable<T>.tryCountParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<Int> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Boolean>>>()
            for (element in this@tryCountParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) { predicate(element) })
            }
            Ok(promises.count {
                when (val result = it.await()) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        false
                    }
                }
            })
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(0)
    }
}

suspend inline fun <T> Iterable<T>.firstParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return this.firstOrNullParallelly(predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend inline fun <T> Iterable<T>.tryFirstParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = this.tryFirstOrNullParallelly(predicate)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

suspend inline fun <T> Iterable<T>.firstOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            for (element in this@firstOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

suspend inline fun <T> Iterable<T>.tryFirstOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFirstOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
                for (promise in promises) {
                    when (val ret = promise.await()) {
                        is Ok -> {
                            result = ret.value
                            if (result != null) {
                                cancel()
                            }
                        }

                        is Failed -> {
                            error = ret.error
                            cancel()
                        }
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <R, T> Iterable<T>.firstNotNullOfParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return this.firstNotNullOfOrNullParallelly(extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = this.tryFirstNotNullOfOrNullParallelly(extractor)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(
                    Err(
                        ErrorCode.ApplicationException,
                        "No element of the collection was transformed to a non-null value."
                    )
                )
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

suspend inline fun <R, T> Iterable<T>.firstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            for (element in this@firstNotNullOfOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

suspend inline fun <R, T> Iterable<T>.tryFirstNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for (element in this@tryFirstNotNullOfOrNullParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        result = ret.value
                        if (result != null) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )
    }
}

suspend inline fun <T> Iterable<T>.lastParallelly(
    crossinline predicate: SuspendPredicate<T>
): T {
    return this.lastOrNullParallelly(predicate)
        ?: throw NoSuchElementException("Collection contains no element matching the predicate.")
}

suspend inline fun <T> Iterable<T>.tryLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T> {
    return when (val result = this.tryLastOrNullParallelly(predicate)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

suspend inline fun <T> Iterable<T>.lastOrNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    var result: T? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            for (element in this@lastOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

suspend inline fun <T> Iterable<T>.tryLastOrNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    var result: T? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryLastOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        result = ret.value
                        if (result != null) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the predicate."))
    }
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R {
    return this.lastNotNullOfOrNullParallelly(extractor)
        ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R> {
    return when (val result = this.tryLastNotNullOfOrNullParallelly(extractor)) {
        is Ok -> {
            result.value
                ?.let { Ok(it) }
                ?: Failed(
                    Err(
                        ErrorCode.ApplicationException,
                        "No element of the collection was transformed to a non-null value."
                    )
                )
        }

        is Failed -> {
            Failed(result.error)
        }
    }
}

suspend inline fun <R, T> Iterable<T>.lastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): R? {
    var result: R? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            for (element in this@lastNotNullOfOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                result = promise.await()
                if (result != null) {
                    cancel()
                }
            }
            null
        }
    } catch (e: CancellationException) {
        result
    }
}

suspend inline fun <R, T> Iterable<T>.tryLastNotNullOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<R?> {
    var result: R? = null
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for (element in this@tryLastNotNullOfOrNullParallelly.reversed().iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            for (promise in promises) {
                when (val ret = promise.await()) {
                    is Ok -> {
                        result = ret.value
                        if (result != null) {
                            cancel()
                        }
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                    }
                }
            }
            Ok(null)
        }
    } catch (e: CancellationException) {
        result?.let { Ok(it) }
            ?: error?.let { Failed(it) }
            ?: Failed(
                Err(
                    ErrorCode.ApplicationException,
                    "No element of the collection was transformed to a non-null value."
                )
            )
    }
}

suspend inline fun <T> Iterable<T>.findParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return this.firstOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryFindParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return this.tryFirstOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.findLastParallelly(
    crossinline predicate: SuspendPredicate<T>
): T? {
    return this.lastOrNullParallelly(predicate)
}

suspend inline fun <T> Iterable<T>.tryFindLastParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<T?> {
    return this.tryLastOrNullParallelly(predicate)
}

suspend inline fun <T : Any> Iterable<T>.filterParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for (element in this@filterToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                if (predicate(element)) {
                    element
                } else {
                    null
                }
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFilterToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <T : Any> Iterable<T?>.filterNotNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterNotNullToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T?>.tryFilterNotNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotNullToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.filterNotNullToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for (element in this@filterNotNullToParallelly.iterator()) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.tryFilterNotNullToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFilterNotNullToParallelly.iterator()) {
                if (element != null) {
                    promises.add(async(Dispatchers.Default) {
                        when (val ret = predicate(element)) {
                            is Ok -> {
                                if (ret.value) {
                                    Ok(element)
                                } else {
                                    Ok(null)
                                }
                            }

                            is Failed -> {
                                Failed(ret.error)
                            }
                        }
                    })
                }
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <T : Any> Iterable<T>.filterNotParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return this.filterNotToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterNotParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return this.tryFilterNotToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for (element in this@filterNotToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                if (predicate(element)) {
                    null
                } else {
                    element
                }
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterNotToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for (element in this@tryFilterNotToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(null)
                            } else {
                                Ok(element)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }


                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <T : Any> Iterable<T>.filterIndexedParallelly(
    crossinline predicate: SuspendIndexedPredicate<T>
): List<T> {
    return this.filterIndexedToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterIndexedParallelly(
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<List<T>> {
    return this.tryFilterIndexedToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterIndexedToParallelly(
    destination: C,
    crossinline predicate: SuspendIndexedPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        for ((index, element) in this@filterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                if (predicate(index, element)) {
                    element
                } else {
                    null
                }
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    destination: C,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<T?>>>()
            for ((index, element) in this@tryFilterIndexedToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(index, element)) {
                        is Ok -> {
                            if (ret.value) {
                                Ok(element)
                            } else {
                                Ok(null)
                            }
                        }

                        is Failed -> {
                            Failed(ret.error)
                        }
                    }
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <reified U : Any, T> Iterable<T>.filterIsInstanceParallelly(
    crossinline predicate: SuspendPredicate<U>
): List<U> {
    return this.filterIsInstanceToParallelly(ArrayList(), predicate)
}

suspend inline fun <reified U : Any, T> Iterable<T>.tryFilterIsInstanceParallelly(
    crossinline predicate: SuspendTryPredicate<U>
): Ret<List<U>> {
    return this.tryFilterIsInstanceToParallelly(ArrayList(), predicate)
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.filterIsInstanceToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<U>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<U?>>()
        for (element in this@filterIsInstanceToParallelly.iterator()) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) {
                    if (predicate(element)) {
                        element
                    } else {
                        null
                    }
                })
            }
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.tryFilterIsInstanceToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<U?>>>()
            for (element in this@tryFilterIsInstanceToParallelly.iterator()) {
                if (element is U) {
                    promises.add(async(Dispatchers.Default) {
                        when (val ret = predicate(element)) {
                            is Ok -> {
                                if (ret.value) {
                                    Ok(element)
                                } else {
                                    Ok(null)
                                }
                            }

                            is Failed -> {
                                Failed(ret.error)
                            }
                        }
                    })
                }
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R : Any, T> Iterable<T>.mapParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): List<R> {
    return this.mapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for (element in this@mapToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.mapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R>>>()
            for (element in this@tryMapToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R : Any, T> Iterable<T>.mapNotNullParallelly(
    crossinline extractor: SuspendExtractor<R?, T>
): List<R> {
    return this.mapNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapNotNullParallelly(
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<List<R>> {
    return this.tryMapNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for (element in this@mapNotNullToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<R?, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for (element in this@tryMapNotNullToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R, T> Iterable<T>.mapIndexedParallelly(
    crossinline extractor: SuspendIndexedExtractor<R, T>
): List<R> {
    return this.mapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapIndexedParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<List<R>> {
    return this.tryMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        for ((index, element) in this@mapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        promises.mapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R>>>()
            for ((index, element) in this@tryMapIndexedToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(index, element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R : Any, T> Iterable<T>.mapIndexedNotNullParallelly(
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): List<R> {
    return this.mapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryMapIndexedNotNullParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<List<R>> {
    return this.tryMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R : Any, T, C : MutableCollection<in R>> Iterable<T>.mapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<R?, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        for ((index, element) in this@mapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        promises.mapNotNullTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<R?, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<R?>>>()
            for ((index, element) in this@tryMapIndexedNotNullToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(index, element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <K, V, T> Iterable<T>.associateParallelly(
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): Map<K, V> {
    return this.associateToParallelly(LinkedHashMap(), extractor)
}

suspend inline fun <K, V, T> Iterable<T>.tryAssociateToParallelly(
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<Map<K, V>> {
    return this.tryAssociateToParallelly(LinkedHashMap(), extractor)
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.associateToParallelly(
    destination: M,
    crossinline extractor: SuspendExtractor<Pair<K, V>, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, V>>>()
        for (element in this@associateToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.associateTo(destination) { it.await() }
    }
}

suspend inline fun <K, V, T, M : MutableMap<in K, in V>> Iterable<T>.tryAssociateToParallelly(
    destination: M,
    crossinline extractor: SuspendTryExtractor<Pair<K, V>, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<K, V>>>>()
            for (element in this@tryAssociateToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.putAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <K, T> Iterable<T>.associateByParallelly(
    crossinline keyExtractor: SuspendExtractor<K, T>
): Map<K, T> {
    return this.associateByToParallelly(LinkedHashMap(), keyExtractor)
}

suspend inline fun <K, T> Iterable<T>.tryAssociateByToParallelly(
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<Map<K, T>> {
    return this.tryAssociateByToParallelly(LinkedHashMap(), keyExtractor)
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.associateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendExtractor<K, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<K, T>>>()
        for (element in this@associateByToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                keyExtractor(element) to element
            })
        }
        promises.associateTo(destination) { it.await() }
    }
}

suspend inline fun <K, T, M : MutableMap<in K, in T>> Iterable<T>.tryAssociateByToParallelly(
    destination: M,
    crossinline keyExtractor: SuspendTryExtractor<K, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<K, T>>>>()
            for (element in this@tryAssociateByToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    keyExtractor(element).map {
                        it to element
                    }
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.putAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <V, T> Iterable<T>.associateWithParallelly(
    crossinline valueExtractor: SuspendExtractor<V, T>
): Map<T, V> {
    return this.associateWithToParallelly(LinkedHashMap(), valueExtractor)
}

suspend inline fun <V, T> Iterable<T>.tryAssociateWithToParallelly(
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<Map<T, V>> {
    return this.tryAssociateWithToParallelly(LinkedHashMap(), valueExtractor)
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.associateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendExtractor<V, T>
): M {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, V>>>()
        for (element in this@associateWithToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                element to valueExtractor(element)
            })
        }
        promises.associateTo(destination) { it.await() }
    }
}

suspend inline fun <V, T, M : MutableMap<in T, in V>> Iterable<T>.tryAssociateWithToParallelly(
    destination: M,
    crossinline valueExtractor: SuspendTryExtractor<V, T>
): Ret<M> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Pair<T, V>>>>()
            for (element in this@tryAssociateWithToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    valueExtractor(element).map {
                        element to it
                    }
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.putAll(result)
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): List<R> {
    return this.flatMapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryFlatMapToParallelly(
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.tryFlatMapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for (element in this@flatMapToParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
            for (element in this@tryFlatMapToParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result.flatten())
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedParallelly(
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): List<R> {
    return this.flatMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return this.tryFlatMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for ((index, element) in this@flatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        promises.flatMapTo(destination) { it.await() }
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
            for ((index, element) in this@tryFlatMapIndexedToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(index, element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            destination.addAll(result.flatten())
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullParallelly(
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): List<R> {
    return this.flatMapIndexedNotNullToParallelly(ArrayList(), extractor)
}


suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<List<R>> {
    return this.tryFlatMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R?>>>()
        for ((index, element) in this@flatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                extractor(index, element)
            })
        }
        for (promise in promises) {
            destination.addAll(promise.await().filterNotNull())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<C> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<Iterable<R?>>>>()
            for ((index, element) in this@tryFlatMapIndexedNotNullToParallelly.withIndex()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(index, element)
                })
            }
            val result = promises.mapNotNull {
                when (val ret = it.await()) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        null
                    }
                }
            }
            for (iterable in result) {
                destination.addAll(iterable.filterNotNull())
            }
            Ok(destination)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(destination)
    }
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <T, reified U> Iterable<T>.sumOfParallelly(
    crossinline extractor: SuspendExtractor<U, T>
): U where U: Arithmetic<U>, U: Plus<U, U> {
    return coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        for (element in this@sumOfParallelly.iterator()) {
            promises.add(async(Dispatchers.Default) {
                extractor(element)
            })
        }
        var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
        for (promise in promises) {
            sum += promise.await()
        }
        sum
    }
}

@Suppress("UNCHECKED_CAST")
suspend inline fun <T, reified U> Iterable<T>.trySumOfParallelly(
    crossinline extractor: SuspendTryExtractor<U, T>
): Ret<U> where U: Arithmetic<U>, U: Plus<U, U> {
    var error: Error? = null

    return try{
        coroutineScope {
            val promises = ArrayList<Deferred<Ret<U>>>()
            for (element in this@trySumOfParallelly.iterator()) {
                promises.add(async(Dispatchers.Default) {
                    extractor(element)
                })
            }
            var sum = (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
            for (promise in promises) {
                sum += when (val result = promise.await()) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        (U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero
                    }
                }
            }
            Ok(sum)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok((U::class.companionObjectInstance!! as ArithmeticConstants<U>).zero)
    }
}

suspend inline fun <T> Iterable<T>.foldParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return this.foldParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.foldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@foldParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { acc, value -> operation(acc, value) }
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = operation(accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.tryFoldParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@tryFoldParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { lhs, rhs ->
                        if (error != null) {
                            lhs
                        } else {
                            when (val ret = operation(lhs, rhs)) {
                                is Ok -> {
                                    ret.value
                                }

                                is Failed -> {
                                    error = ret.error
                                    lhs
                                }
                            }
                        }
                    }
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = when (val ret = operation(accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return this.foldIndexedParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.foldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@foldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                operation(rhs.first, lhs, rhs.second)
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = operation(promise.first, accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldIndexedParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.tryFoldIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@tryFoldIndexedParallelly.iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(rhs.first, lhs, rhs.second)) {
                                        is Ok -> {
                                            ret.value
                                        }

                                        is Failed -> {
                                            error = ret.error
                                            lhs
                                        }
                                    }
                                }
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = when (val ret = operation(promise.first, accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}

suspend inline fun <T> Iterable<T>.foldRightParallelly(
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    return this.foldRightParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.foldRightParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: suspend (acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@foldRightParallelly.reversed().iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { acc, value -> operation(acc, value) }
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = operation(accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldRightParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.tryFoldRightParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T>>()
            val iterator = this@tryFoldRightParallelly.reversed().iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.fold(initial) { lhs, rhs ->
                        if (error != null) {
                            lhs
                        } else {
                            when (val ret = operation(lhs, rhs)) {
                                is Ok -> {
                                    ret.value
                                }

                                is Failed -> {
                                    error = ret.error
                                    lhs
                                }
                            }
                        }
                    }
                })
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.await()
                accumulator = when (val ret = operation(accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}

suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    return this.foldRightIndexedParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.foldRightIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> T
): T {
    var exception: Exception? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@foldRightIndexedParallelly.reversed().iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<Pair<Int, T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(Pair(i, iterator.next()))
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().first,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                operation(rhs.first, lhs, rhs.second)
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = operation(promise.first, accumulator, result)
                if (index == 0 && accumulator != result) {
                    exception = IllegalArgumentException("operation is order dependent")
                    cancel()
                    return@coroutineScope accumulator
                }
            }

            accumulator
        }
    } catch (e: CancellationException) {
        exception?.let { throw it }
        accumulator
    }
}

suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    return this.tryFoldRightIndexedParallelly(UInt64.ten, initial, operation)
}

suspend inline fun <T> Iterable<T>.tryFoldRightIndexedParallelly(
    segment: UInt64,
    initial: T,
    crossinline operation: (index: Int, acc: T, T) -> Ret<T>
): Ret<T> {
    var error: Error? = null
    var accumulator = initial

    return try {
        coroutineScope {
            val promises = ArrayList<Pair<Int, Deferred<T>>>()
            val iterator = this@tryFoldRightIndexedParallelly.withIndex().reversed().iterator()
            var i = 0
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<IndexedValue<T>>()
                var j = UInt64.zero
                while (iterator.hasNext() && j != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                    ++j
                }
                if (thisSegment.isNotEmpty()) {
                    promises.add(Pair(
                        thisSegment.first().index,
                        async(Dispatchers.Default) {
                            thisSegment.fold(initial) { lhs, rhs ->
                                if (error != null) {
                                    lhs
                                } else {
                                    when (val ret = operation(rhs.index, lhs, rhs.value)) {
                                        is Ok -> {
                                            ret.value
                                        }

                                        is Failed -> {
                                            error = ret.error
                                            lhs
                                        }
                                    }
                                }
                            }
                        }
                    ))
                }
            }

            for ((index, promise) in promises.withIndex()) {
                val result = promise.second.await()
                accumulator = when (val ret = operation(promise.first, accumulator, result)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        error = ret.error
                        cancel()
                        return@coroutineScope Failed(error as Error)
                    }
                }
                if (index == 0 && accumulator != result) {
                    error = Err(ErrorCode.ApplicationException, "operation is order dependent")
                    cancel()
                    return@coroutineScope Failed(error as Error)
                }
            }

            Ok(accumulator)
        }
    } catch (e: CancellationException) {
        error?.let { Failed(it) }
            ?: Ok(accumulator)
    }
}
