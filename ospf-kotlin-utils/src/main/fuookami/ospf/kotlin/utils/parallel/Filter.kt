package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.SuspendIndexedPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryIndexedPredicate
import fuookami.ospf.kotlin.utils.functional.SuspendTryPredicate

suspend inline fun <T : Any> Iterable<T>.filterParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for (element in this@filterToParallelly) {
            promises.add(async(Dispatchers.Default) { element to predicate(element) })
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@tryFilterToParallelly) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = predicate(element)) {
                    is Ok -> Ok(element to ret.value)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any> Iterable<T?>.filterNotNullParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterNotNullToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T?>.tryFilterNotNullParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterNotNullToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.filterNotNullToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for (element in this@filterNotNullToParallelly) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) { element to predicate(element) })
            }
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T?>.tryFilterNotNullToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@tryFilterNotNullToParallelly) {
            if (element != null) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> Ok(element to ret.value)
                        is Failed -> Failed(ret.error)
                        is Fatal -> Fatal(ret.errors)
                    }
                })
            }
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any> Iterable<T>.filterNotParallelly(
    crossinline predicate: SuspendPredicate<T>
): List<T> {
    return filterNotToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterNotParallelly(
    crossinline predicate: SuspendTryPredicate<T>
): Ret<List<T>> {
    return tryFilterNotToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterNotToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for (element in this@filterNotToParallelly) {
            promises.add(async(Dispatchers.Default) { element to !predicate(element) })
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterNotToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for (element in this@tryFilterNotToParallelly) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = predicate(element)) {
                    is Ok -> Ok(element to !ret.value)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <T : Any> Iterable<T>.filterIndexedParallelly(
    crossinline predicate: SuspendIndexedPredicate<T>
): List<T> {
    return filterIndexedToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any> Iterable<T>.tryFilterIndexedParallelly(
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<List<T>> {
    return tryFilterIndexedToParallelly(ArrayList(), predicate)
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.filterIndexedToParallelly(
    destination: C,
    crossinline predicate: SuspendIndexedPredicate<T>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<T, Boolean>>>()
        for ((index, element) in this@filterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) { element to predicate(index, element) })
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <T : Any, C : MutableCollection<in T>> Iterable<T>.tryFilterIndexedToParallelly(
    destination: C,
    crossinline predicate: SuspendTryIndexedPredicate<T>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<T, Boolean>>>>()
        for ((index, element) in this@tryFilterIndexedToParallelly.withIndex()) {
            promises.add(async(Dispatchers.Default) {
                when (val ret = predicate(index, element)) {
                    is Ok -> Ok(element to ret.value)
                    is Failed -> Failed(ret.error)
                    is Fatal -> Fatal(ret.errors)
                }
            })
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}

suspend inline fun <reified U : Any, T> Iterable<T>.filterIsInstanceParallelly(
    crossinline predicate: SuspendPredicate<U>
): List<U> {
    return filterIsInstanceToParallelly(ArrayList(), predicate)
}

suspend inline fun <reified U : Any, T> Iterable<T>.tryFilterIsInstanceParallelly(
    crossinline predicate: SuspendTryPredicate<U>
): Ret<List<U>> {
    return tryFilterIsInstanceToParallelly(ArrayList(), predicate)
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.filterIsInstanceToParallelly(
    destination: C,
    crossinline predicate: SuspendPredicate<U>
): C {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<U, Boolean>>>()
        for (element in this@filterIsInstanceToParallelly) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) { element to predicate(element) })
            }
        }
        for (promise in promises) {
            val (element, keep) = promise.await()
            if (keep) {
                destination.add(element)
            }
        }
        destination
    }
}

suspend inline fun <reified U : Any, T, C : MutableCollection<U>> Iterable<T>.tryFilterIsInstanceToParallelly(
    destination: C,
    crossinline predicate: SuspendTryPredicate<U>
): Ret<C> {
    return coroutineScope {
        val promises = ArrayList<Deferred<Ret<Pair<U, Boolean>>>>()
        for (element in this@tryFilterIsInstanceToParallelly) {
            if (element is U) {
                promises.add(async(Dispatchers.Default) {
                    when (val ret = predicate(element)) {
                        is Ok -> Ok(element to ret.value)
                        is Failed -> Failed(ret.error)
                        is Fatal -> Fatal(ret.errors)
                    }
                })
            }
        }
        for (promise in promises) {
            when (val ret = promise.await()) {
                is Ok -> {
                    val (element, keep) = ret.value
                    if (keep) {
                        destination.add(element)
                    }
                }

                is Failed -> return@coroutineScope Failed(ret.error)
                is Fatal -> return@coroutineScope Fatal(ret.errors)
            }
        }
        Ok(destination)
    }
}
