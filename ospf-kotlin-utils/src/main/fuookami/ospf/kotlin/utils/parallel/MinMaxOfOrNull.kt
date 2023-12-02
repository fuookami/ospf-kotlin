package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNullParallelly(crossinline extractor: Extractor<R, T>): Pair<R, R>? {
    return this.minMaxOfOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNullParallelly(
    segment: UInt64,
    crossinline extractor: Extractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val iterator = this@minMaxOfOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<R>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(extractor(iterator.next()))
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxOrNull()
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.minOfOrNull { it.first }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.maxOfOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max
            }
        }

        Pair(min, max)
    }
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxOfOrNullParallelly(crossinline extractor: Extractor<R, T>): Pair<R, R>? {
    return (this as Iterable<T>).minMaxOfOrNullParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> Collection<T>.minMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): Pair<R, R>? {
    return (this as Iterable<T>).minMaxOfOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxOfOrNullParallelly(crossinline extractor: Extractor<R, T>): Pair<R, R>? {
    return this.minMaxOfOrNullParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        extractor
    )
}

suspend inline fun <T, R : Comparable<R>> List<T>.minMaxOfOrNullParallelly(
    concurrentAmount: UInt64,
    crossinline extractor: Extractor<R, T>
): Pair<R, R>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<R, R>?>>()
        val segmentAmount = this@minMaxOfOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxOfOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxOfOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxOfOrNullParallelly.subList(j, k).map { extractor(it) }.minMaxOrNull()
            })
            i = k
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.minOfOrNull { it.first }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.maxOfOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max
            }
        }

        Pair(min, max)
    }
}
