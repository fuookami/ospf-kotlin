package fuookami.ospf.kotlin.utils.parallel

import kotlin.reflect.full.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.minMaxWith
import fuookami.ospf.kotlin.utils.math.ordinary.minMaxWithOrNull
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
): U where U : Arithmetic<U>, U : Plus<U, U> {
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
): Ret<U> where U : Arithmetic<U>, U : Plus<U, U> {
    var error: Error? = null

    return try {
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
                    thisSegment.add(i to iterator.next())
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
                    thisSegment.add(i to iterator.next())
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
                    thisSegment.add(i to iterator.next())
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

suspend inline fun <T : Comparable<T>> Iterable<T>.maxParallelly(): T {
    return this.maxParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.maxParallelly(
    segment: UInt64
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@maxParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.max()
            })
        }

        promises.maxOf { it.await() }
    }
}

suspend inline fun <T : Comparable<T>> Iterable<T>.maxOrNullParallelly(): T? {
    return this.maxOrNullParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.maxOrNullParallelly(
    segment: UInt64
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxOrNull()
            })
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): T {
    return this.maxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>>>()
        val iterator = this@maxByParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { it to extractor(it) }.maxBy { it.second }
            })
        }

        promises.map { it.await() }.maxBy { it.second }.first
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T> {
    return this.tryMaxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMaxByParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                it to result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.maxByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.let { Ok(it.first) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return this.maxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@maxByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { it to extractor(it) }.maxByOrNull { it.second }
            })
        }

        promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.first
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    return this.tryMaxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMaxByOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                it to result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.maxByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.maxByOrNull { it.second }?.let { Ok(it.first) }
                ?: Ok(null)
        }
    } catch (e: CancellationException) {
        Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxOfOrNull { extractor(it) }
            })
        }

        promises.mapNotNull { it.await() }.max()
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMaxOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.maxOrNull()
                })
            }

            promises.mapNotNull { it.await() }.maxOrNull()?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.maxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxOfOrNull { extractor(it) }
            })
        }

        promises.mapNotNull { it.await() }.maxOrNull()
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return this.tryMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMaxOfOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.maxOrNull()
                })
            }

            promises.mapNotNull { it.await() }.maxOrNull()?.let { Ok(it) }
                ?: Ok(null)
        }
    } catch (e: CancellationException) {
        Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.maxWithParallelly(
    comparator: KComparator<T>
): T {
    return this.maxWithParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithParallelly(
    segment: UInt64,
    comparator: KComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@maxWithParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWith(comparator)
            })
        }

        promises.map { it.await() }.maxWith(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithComparatorParallelly(
    crossinline comparator: Comparator<T>
): T {
    return this.maxWithComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@maxWithComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithComparator(comparator)
            })
        }

        promises.map { it.await() }.maxWithComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithPartialComparatorParallelly(
    crossinline comparator: PartialComparator<T>
): T {
    return this.maxWithPartialComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithPartialComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@maxWithPartialComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithPartialComparator(comparator)
            })
        }

        promises.map { it.await() }.maxWithPartialComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithThreeWayComparatorParallelly(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.maxWithThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@maxWithThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithThreeWayComparator(comparator)
            })
        }

        promises.map { it.await() }.maxWithThreeWayComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorParallelly(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.maxWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@maxWithPartialThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithPartialThreeWayComparator(comparator)
            })
        }

        promises.map { it.await() }.maxWithPartialThreeWayComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.tryMaxWithComparatorParallelly(
    crossinline comparator: TryComparator<T>
): Ret<T> {
    return this.tryMaxWithComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMaxWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMaxWithComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.maxWithComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            Ok(promises.mapNotNull { it.await() }.maxWithComparator { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
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
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.tryMaxWithThreeWayComparatorParallelly(
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T> {
    return this.tryMaxWithThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMaxWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMaxWithThreeWayComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.maxWithThreeWayComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            Ok(promises.mapNotNull { it.await() }.maxWithThreeWayComparator { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Order.Equal
                    }
                }
            })
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.maxWithOrNullParallelly(
    comparator: KComparator<T>
): T? {
    return this.maxWithOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithOrNullParallelly(
    segment: UInt64,
    comparator: KComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxWithOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithComparatorOrNullParallelly(
    crossinline comparator: Comparator<T>
): T? {
    return this.maxWithComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithPartialComparatorOrNullParallelly(
    crossinline comparator: PartialComparator<T>
): T? {
    return this.maxWithPartialComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithPartialComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxWithPartialComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithPartialComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithPartialComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.maxWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxWithThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNullParallelly(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.maxWithPartialThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.maxWithPartialThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@maxWithPartialThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.maxWithPartialThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithPartialThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.tryMaxWithComparatorOrNullParallelly(
    crossinline comparator: TryComparator<T>
): Ret<T?> {
    return this.tryMaxWithComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMaxWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMaxWithComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.maxWithComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            Ok(promises.mapNotNull { it.await() }.maxWithComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
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
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.tryMaxWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T?> {
    return this.tryMaxWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMaxWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMaxWithThreeWayComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.maxWithThreeWayComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            Ok(promises.mapNotNull { it.await() }.maxWithThreeWayComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Order.Equal
                    }
                }
            })
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithParallelly(
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfWithParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithParallelly(
    segment: UInt64,
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@maxOfWithParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWith(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWith(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithComparatorParallelly(
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@maxOfWithComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialComparatorParallelly(
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfWithPartialComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@maxOfWithPartialComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithPartialComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithPartialComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithThreeWayComparatorParallelly(
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@maxOfWithThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithThreeWayComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithThreeWayComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialThreeWayComparatorParallelly(
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.maxOfWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@maxOfWithPartialThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithPartialThreeWayComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithPartialThreeWayComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithComparatorParallelly(
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@tryMaxOfWithComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                null
                            }
                        }
                    }.maxWithComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                false
                            }
                        }
                    }
                })
            }
            promises.mapNotNull { it.await() }.maxWithComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        false
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithThreeWayComparatorParallelly(
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@tryMaxOfWithThreeWayComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                null
                            }
                        }
                    }.maxWithThreeWayComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                Order.Equal
                            }
                        }
                    }
                })
            }
            promises.mapNotNull { it.await() }.maxWithThreeWayComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        Order.Equal
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithOrNullParallelly(
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfWithParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithOrNullParallelly(
    segment: UInt64,
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfWithOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithComparatorOrNullParallelly(
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialComparatorOrNullParallelly(
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfWithPartialComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfWithPartialComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithPartialComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithPartialComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfWithThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialThreeWayComparatorOrNullParallelly(
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.maxOfWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.maxOfWithPartialThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@maxOfWithPartialThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.maxWithPartialThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.maxWithPartialThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithComparatorOrNullParallelly(
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMaxOfWithComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                null
                            }
                        }
                    }.maxWithComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                false
                            }
                        }
                    }
                })
            }
            promises.mapNotNull { it.await() }.maxWithComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        false
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMaxOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMaxOfWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMaxOfWithThreeWayComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                null
                            }
                        }
                    }.maxWithThreeWayComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                Order.Equal
                            }
                        }
                    }
                })
            }
            promises.mapNotNull { it.await() }.maxWithThreeWayComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        Order.Equal
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minParallelly(): T {
    return this.minParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minParallelly(
    segment: UInt64
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@minParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.min()
            })
        }

        promises.minOfOrNull { it.await() }!!
    }
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minOrNullParallelly(): T? {
    return minOrNullParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minOrNullParallelly(
    segment: UInt64
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minOrNull()
            })
        }

        promises.mapNotNull { it.await() }.minOrNull()
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    crossinline extractor: Extractor<R, T>
): T {
    return this.minByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>>>()
        val iterator = this@minByParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { it to extractor(it) }.minBy { it.second }
            })
        }

        promises.map { it.await() }.minBy { it.second }.first
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T> {
    return this.tryMinByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMinByParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                it to result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.minByOrNull { it.second }?.let { Ok(it.first) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): T? {
    return this.minByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, R>?>>()
        val iterator = this@minByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { it to extractor(it) }.minByOrNull { it.second }
            })
        }

        promises.mapNotNull { it.await() }.minByOrNull { it.second }?.first
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    return this.tryMinByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, R>?>>()
            val iterator = this@tryMinByOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                it to result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minByOrNull { it.second }
                })
            }

            promises.mapNotNull { it.await() }.minByOrNull { it.second }?.let { Ok(it.first) }
                ?: Ok(null)
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfParallelly(
    crossinline extractor: Extractor<R, T>
): R {
    return this.minOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@minOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minOf { extractor(it) }
            })
        }

        promises.minOf { it.await() }
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return tryMinOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMinOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minOrNull()
                })
            }

            promises.mapNotNull { it.await() }.minOrNull()?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extracting."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfOrNullParallelly(
    crossinline extractor: Extractor<R, T>
): R? {
    return this.minOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minOfOrNull { extractor(it) }
            })
        }

        promises.mapNotNull { it.await() }.minOfOrNull { it }
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return tryMinOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMinOfOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                null
                            }
                        }
                    }.minOrNull()
                })
            }

            promises.mapNotNull { it.await() }.minOrNull()?.let { Ok(it) }
                ?: Ok(null)
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.minWithParallelly(
    comparator: KComparator<T>
): T {
    return this.minWithParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithParallelly(
    segment: UInt64,
    comparator: KComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@minWithParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWith(comparator)
            })
        }

        promises.map { it.await() }.minWith(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithComparatorParallelly(
    crossinline comparator: Comparator<T>
): T {
    return this.minWithComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@minWithComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithComparator(comparator)
            })
        }

        promises.map { it.await() }.minWithComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithPartialComparatorParallelly(
    crossinline comparator: PartialComparator<T>
): T {
    return this.minWithPartialComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithPartialComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@minWithPartialComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithPartialComparator(comparator)
            })
        }

        promises.map { it.await() }.minWithPartialComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithThreeWayComparatorParallelly(
    crossinline comparator: ThreeWayComparator<T>
): T {
    return this.minWithThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@minWithThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithThreeWayComparator(comparator)
            })
        }

        promises.map { it.await() }.minWithThreeWayComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithPartialThreeWayComparatorParallelly(
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return this.minWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithPartialThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<T>
): T {
    return coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@minWithPartialThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithPartialThreeWayComparator(comparator)
            })
        }

        promises.map { it.await() }.minWithPartialThreeWayComparator(comparator)
    }
}

suspend inline fun <T> Iterable<T>.tryMinWithComparatorParallelly(
    crossinline comparator: TryComparator<T>
): Ret<T> {
    return this.tryMinWithComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMinWithComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minWithComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        false
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.tryMinWithThreeWayComparatorParallelly(
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T> {
    return this.tryMinWithThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMinWithThreeWayComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minWithThreeWayComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithThreeWayComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Order.Equal
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.minWithOrNullParallelly(
    comparator: KComparator<T>
): T? {
    return this.minWithOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithOrNullParallelly(
    segment: UInt64,
    comparator: KComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minWithOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithComparatorOrNullParallelly(
    crossinline comparator: Comparator<T>
): T? {
    return this.minWithComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithPartialComparatorOrNullParallelly(
    crossinline comparator: PartialComparator<T>
): T? {
    return this.minWithPartialComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithPartialComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minWithPartialComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithPartialComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithPartialComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return this.minWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minWithThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNullParallelly(
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return this.minWithPartialThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minWithPartialThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<T>
): T? {
    return coroutineScope {
        val promises = ArrayList<Deferred<T?>>()
        val iterator = this@minWithPartialThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minWithPartialThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithPartialThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T> Iterable<T>.tryMinWithComparatorOrNullParallelly(
    crossinline comparator: TryComparator<T>
): Ret<T?> {
    return this.tryMinWithComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMinWithComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minWithComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        false
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.tryMinWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T?> {
    return this.tryMinWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<T>
): Ret<T?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<T?>>()
            val iterator = this@tryMinWithThreeWayComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minWithThreeWayComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithThreeWayComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Order.Equal
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithParallelly(
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.minOfWithParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithParallelly(
    segment: UInt64,
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@minOfWithParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWith(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWith(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithComparatorParallelly(
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.minOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@minOfWithComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialComparatorParallelly(
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.minOfWithPartialComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@minOfWithPartialComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithPartialComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithPartialComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithThreeWayComparatorParallelly(
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.minOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@minOfWithThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithThreeWayComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithThreeWayComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialThreeWayComparatorParallelly(
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return this.minOfWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}


suspend inline fun <T, R> Iterable<T>.minOfWithPartialThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R {
    return coroutineScope {
        val promises = ArrayList<Deferred<R>>()
        val iterator = this@minOfWithPartialThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithPartialThreeWayComparator(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithPartialThreeWayComparator(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithComparatorParallelly(
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMinOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@tryMinOfWithComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minWithComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithComparatorOrNull() { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        false
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithThreeWayComparatorParallelly(
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    return this.tryMinOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R>>()
            val iterator = this@tryMinOfWithThreeWayComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minWithThreeWayComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithThreeWayComparatorOrNull() { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Order.Equal
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithComparatorOrNullParallelly(
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.minOfWithComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithComparatorOrNullParallelly(
    segment: UInt64,
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithComparatorOrNullParallelly(
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.minOfWithComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialComparatorOrNullParallelly(
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.minOfWithPartialComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfWithPartialComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithPartialComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithPartialComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.minOfWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfWithThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialThreeWayComparatorOrNullParallelly(
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return this.minOfWithPartialThreeWayComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minOfWithPartialThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): R? {
    return coroutineScope {
        val promises = ArrayList<Deferred<R?>>()
        val iterator = this@minOfWithPartialThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minWithPartialThreeWayComparatorOrNull(comparator)
            })
        }

        promises.mapNotNull { it.await() }.minWithPartialThreeWayComparatorOrNull(comparator)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithComparatorOrNullParallelly(
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return this.tryMinOfWithComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMinOfWithComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minWithComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        false
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    return this.tryMinOfWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinOfWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<R?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<R?>>()
            val iterator = this@tryMinOfWithThreeWayComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minWithThreeWayComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            promises.mapNotNull { it.await() }.minWithThreeWayComparatorOrNull { lhs, rhs ->
                when (val result = comparator(lhs, rhs)) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        error = result.error
                        cancel()
                        Order.Equal
                    }
                }
            }?.let { Ok(it) }
                ?: Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minMaxParallelly(): Pair<T, T> {
    return minMaxParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minMaxParallelly(
    segment: UInt64
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>>>()
        val iterator = this@minMaxParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMax()
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minBy { it.first } }
        val maxPromise = async { segmentResults.maxBy { it.second } }
        Pair(minPromise.await().first, maxPromise.await().second)
    }
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minMaxOrNullParallelly(): Pair<T, T>? {
    return minMaxOrNullParallelly(UInt64.ten)
}

suspend inline fun <T : Comparable<T>> Iterable<T>.minMaxOrNullParallelly(
    segment: UInt64
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxOrNull()
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minBy { it.first } }
        val maxPromise = async { segmentResults.maxBy { it.second } }
        Pair(minPromise.await().first, maxPromise.await().second)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T> {
    return minMaxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>>>()
        val iterator = this@minMaxByParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { it to extractor(it) }.minMaxBy { it.second }
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minBy { it.first.second } }
        val maxPromise = async { segmentResults.maxBy { it.second.second } }
        Pair(minPromise.await().first.first, maxPromise.await().second.first)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>> {
    return tryMinMaxByParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>>>()
            val iterator = this@tryMinMaxByParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                it to result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxBy { it.second }
                })
            }

            val segmentResults = promises.map { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extractor."))
            }

            val minPromise = async { segmentResults.minBy { it.first.second } }
            val maxPromise = async { segmentResults.maxBy { it.second.second } }
            Ok(Pair(minPromise.await().first.first, maxPromise.await().second.first))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return minMaxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val iterator = this@minMaxByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { it to extractor(it) }.minMaxByOrNull { it.second }
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minBy { it.first.second } }
        val maxPromise = async { segmentResults.maxBy { it.second.second } }
        Pair(minPromise.await().first.first, maxPromise.await().second.first)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    return tryMinMaxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxByOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<T, T>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
            val iterator = this@tryMinMaxByOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                it to result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxByOrNull { it.second }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Ok(null)
            }

            val minPromise = async { segmentResults.minBy { it.first.second } }
            val maxPromise = async { segmentResults.maxBy { it.second.second } }
            Ok(Pair(minPromise.await().first.first, maxPromise.await().second.first))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return minMaxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>>>()
        val iterator = this@minMaxOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMax()
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minBy { it.first } }
        val maxPromise = async { segmentResults.maxBy { it.second } }
        Pair(minPromise.await().first, maxPromise.await().second)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return tryMinMaxOfParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>>>()
            val iterator = this@tryMinMaxOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMax()
                })
            }

            val segmentResults = promises.map { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the extractor."))
            }

            val minPromise = async { segmentResults.minBy { it.first } }
            val maxPromise = async { segmentResults.maxBy { it.second } }
            Ok(Pair(minPromise.await().first, maxPromise.await().second))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNullParallelly(
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return minMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxOrNull()
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minBy { it.first } }
        val maxPromise = async { segmentResults.maxBy { it.second } }
        Pair(minPromise.await().first, maxPromise.await().second)
    }
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfOrNullParallelly(
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return tryMinMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.tryMinMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxOrNull()
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Ok(null)
            }

            val minPromise = async { segmentResults.minBy { it.first } }
            val maxPromise = async { segmentResults.maxBy { it.second } }
            Ok(Pair(minPromise.await().first, maxPromise.await().second))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithParallelly(
    comparator: KComparator<T>
): Pair<T, T> {
    return minMaxWithParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithParallelly(
    segment: UInt64,
    comparator: KComparator<T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>>>()
        val iterator = this@minMaxWithParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWith(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWith(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWith(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithComparatorParallelly(
    crossinline comparator: Comparator<T>
): Pair<T, T> {
    return minMaxWithComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>>>()
        val iterator = this@minMaxWithComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialComparatorParallelly(
    crossinline comparator: PartialComparator<T>
): Pair<T, T> {
    return minMaxWithPartialComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>>>()
        val iterator = this@minMaxWithPartialComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithPartialComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithPartialComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithThreeWayComparatorParallelly(
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T> {
    return minMaxWithThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>>>()
        val iterator = this@minMaxWithThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithThreeWayComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorParallelly(
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T> {
    return minMaxWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>>>()
        val iterator = this@minMaxWithPartialThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithPartialThreeWayComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithPartialThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithComparatorParallelly(
    crossinline comparator: TryComparator<T>
): Ret<Pair<T, T>> {
    return tryMinMaxWithComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<T>
): Ret<Pair<T, T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, T>>>()
            val iterator = this@tryMinMaxWithComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minMaxWithComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.map { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
            }

            val minPromise = async {
                segmentResults.minOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.first }
            }
            val maxPromise = async {
                segmentResults.maxOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithThreeWayComparatorParallelly(
    crossinline comparator: TryThreeWayComparator<T>
): Ret<Pair<T, T>> {
    return tryMinMaxWithThreeWayComparatorParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<T>
): Ret<Pair<T, T>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, T>>>()
            val iterator = this@tryMinMaxWithThreeWayComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minMaxWithThreeWayComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.map { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
            }

            val minPromise = async {
                segmentResults.minOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.first }
            }
            val maxPromise = async {
                segmentResults.maxOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithOrNullParallelly(
    comparator: KComparator<T>
): Pair<T, T>? {
    return minMaxWithOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithOrNullParallelly(
    segment: UInt64,
    comparator: KComparator<T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxWithOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWith(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWith(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithComparatorOrNullParallelly(
    crossinline comparator: Comparator<T>
): Pair<T, T>? {
    return minMaxWithComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialComparatorOrNullParallelly(
    crossinline comparator: PartialComparator<T>
): Pair<T, T>? {
    return minMaxWithPartialComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxWithPartialComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithPartialComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithPartialComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T>? {
    return minMaxWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxWithThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithThreeWayComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorOrNullParallelly(
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T>? {
    return minMaxWithPartialThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.minMaxWithPartialThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<T>
): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, T>?>>()
        val iterator = this@minMaxWithPartialThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxWithPartialThreeWayComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithPartialThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithComparatorOrNullParallelly(
    crossinline comparator: TryComparator<T>
): Ret<Pair<T, T>?> {
    return tryMinMaxWithComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<T>
): Ret<Pair<T, T>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, T>?>>()
            val iterator = this@tryMinMaxWithComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minMaxWithComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
            }

            val minPromise = async {
                segmentResults.minOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.first }
            }
            val maxPromise = async {
                segmentResults.maxOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: TryThreeWayComparator<T>
): Ret<Pair<T, T>?> {
    return tryMinMaxWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator)
}

suspend inline fun <T> Iterable<T>.tryMinMaxWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<T>
): Ret<Pair<T, T>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<T, T>?>>()
            val iterator = this@tryMinMaxWithThreeWayComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.minMaxWithThreeWayComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
            }

            val minPromise = async {
                segmentResults.minOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.first }
            }
            val maxPromise = async {
                segmentResults.maxOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithParallelly(
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return minMaxOfWithParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithParallelly(
    segment: UInt64,
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>>>()
        val iterator = this@minMaxOfWithParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWith(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWith(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWith(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithComparatorParallelly(
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return minMaxOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>>>()
        val iterator = this@minMaxOfWithComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialComparatorParallelly(
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return minMaxOfWithPartialComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>>>()
        val iterator = this@minMaxOfWithPartialComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithPartialComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithPartialComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithThreeWayComparatorParallelly(
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return minMaxOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>>>()
        val iterator = this@minMaxOfWithThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithThreeWayComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialThreeWayComparatorParallelly(
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return minMaxOfWithPartialThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>>>()
        val iterator = this@minMaxOfWithPartialThreeWayComparatorParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithPartialThreeWayComparator(comparator)
            })
        }

        val segmentResults = promises.map { it.await() }
        val minPromise = async { segmentResults.minOfWithPartialThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithComparatorParallelly(
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return tryMinMaxOfWithComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfWithComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxWithComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
            }

            val minPromise = async {
                segmentResults.minOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.first }
            }
            val maxPromise = async {
                segmentResults.maxOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithThreeWayComparatorParallelly(
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    return tryMinMaxOfWithThreeWayComparatorParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithThreeWayComparatorParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfWithThreeWayComparatorParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxWithThreeWayComparator { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            if (segmentResults.isEmpty()) {
                return@coroutineScope Failed(Err(ErrorCode.ApplicationException, "Collection contains no element matching the comparator."))
            }

            val minPromise = async {
                segmentResults.minOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.first }
            }
            val maxPromise = async {
                segmentResults.maxOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        return Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithOrNullParallelly(
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return minMaxOfWithOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithOrNullParallelly(
    segment: UInt64,
    comparator: KComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfWithOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWith(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWith(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithComparatorOrNullParallelly(
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return minMaxOfWithComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: Comparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfWithComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialComparatorOrNullParallelly(
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return minMaxOfWithPartialComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfWithPartialComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithPartialComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithPartialComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return minMaxOfWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: ThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfWithThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithThreeWayComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialThreeWayComparatorOrNullParallelly(
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return minMaxOfWithPartialThreeWayComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.minMaxOfWithPartialThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: PartialThreeWayComparator<R>,
    crossinline extractor: SuspendExtractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfWithPartialThreeWayComparatorOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.map { extractor(it) }.minMaxWithPartialThreeWayComparatorOrNull(comparator)
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        if (segmentResults.isEmpty()) {
            return@coroutineScope null
        }

        val minPromise = async { segmentResults.minOfWithPartialThreeWayComparator(comparator) { it.first } }
        val maxPromise = async { segmentResults.maxOfWithPartialThreeWayComparator(comparator) { it.second } }
        Pair(minPromise.await(), maxPromise.await())
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithComparatorOrNullParallelly(
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return tryMinMaxOfWithComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfWithComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxWithComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                false
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            val maxPromise = async {
                segmentResults.minOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.first }
            }
            val minPromise = async {
                segmentResults.maxOfWithComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            false
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        Failed(error!!)
    }
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithThreeWayComparatorOrNullParallelly(
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    return tryMinMaxOfWithThreeWayComparatorOrNullParallelly(UInt64.ten, comparator, extractor)
}

suspend inline fun <T, R> Iterable<T>.tryMinMaxOfWithThreeWayComparatorOrNullParallelly(
    segment: UInt64,
    crossinline comparator: TryThreeWayComparator<R>,
    crossinline extractor: SuspendTryExtractor<R, T>
): Ret<Pair<R, R>?> {
    var error: Error? = null

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Pair<R, R>?>>()
            val iterator = this@tryMinMaxOfWithThreeWayComparatorOrNullParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.mapNotNull {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                null
                            }
                        }
                    }.minMaxWithThreeWayComparatorOrNull { lhs, rhs ->
                        when (val result = comparator(lhs, rhs)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                Order.Equal
                            }
                        }
                    }
                })
            }

            val segmentResults = promises.mapNotNull { it.await() }
            val maxPromise = async {
                segmentResults.minOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.first }
            }
            val minPromise = async {
                segmentResults.maxOfWithThreeWayComparator({ lhs, rhs ->
                    when (val result = comparator(lhs, rhs)) {
                        is Ok -> {
                            result.value
                        }

                        is Failed -> {
                            error = result.error
                            cancel()
                            Order.Equal
                        }
                    }
                }) { it.second }
            }
            Ok(Pair(minPromise.await(), maxPromise.await()))
        }
    } catch (e: CancellationException) {
        Failed(error!!)
    }
}
