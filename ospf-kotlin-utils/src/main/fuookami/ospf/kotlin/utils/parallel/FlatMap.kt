package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryExtractor
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedExtractor

suspend inline fun <R, T> Iterable<T>.flatMapParallelly(
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): List<R> {
    return flatMapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T> Iterable<T>.tryFlatMapToParallelly(
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return tryFlatMapToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapToParallelly(
    destination: C,
    crossinline extractor: SuspendExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for (element in this@flatMapToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            destination.addAll(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapToParallelly(
    destination: C,
    crossinline extractor: SuspendTryExtractor<Iterable<R>, T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
        for (element in this@tryFlatMapToParallelly) {
            promises.add(async(Dispatchers.Default) { extractor(element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value)
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedParallelly(
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): List<R> {
    return flatMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<List<R>> {
    return tryFlatMapIndexedToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R>>>()
        for ((index, element) in this@flatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
        }
        for (promise in promises) {
            destination.addAll(promise.await())
        }
        destination
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedToParallelly(
    destination: C,
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R>, T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R>>>>()
        for ((index, element) in this@tryFlatMapIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value)
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullParallelly(
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): List<R> {
    return flatMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.tryFlatMapIndexedNotNullToParallelly(
    crossinline extractor: SuspendTryIndexedExtractor<Iterable<R?>, T>
): Ret<List<R>> {
    return tryFlatMapIndexedNotNullToParallelly(ArrayList(), extractor)
}

suspend inline fun <R, T, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedNotNullToParallelly(
    destination: C,
    crossinline extractor: SuspendIndexedExtractor<Iterable<R?>, T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Iterable<R?>>>()
        for ((index, element) in this@flatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
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
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Iterable<R?>>>>()
        for ((index, element) in this@tryFlatMapIndexedNotNullToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { extractor(index, element) })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> destination.addAll(ret.value.filterNotNull())
                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}
