package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, R: Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    return this.minMaxByOrNullParallelly(UInt64.ten, extractor)
}

suspend inline fun <T, R: Comparable<R>> Iterable<T>.minMaxByOrNullParallelly(segment: UInt64, crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val iterator = this@minMaxByOrNullParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<Pair<T, R>>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                val v = iterator.next()
                thisSegment.add(Pair(v, extractor(v)))
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.minMaxByOrNull { it.second }
            })
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.map { it.first }.minByOrNull { it.second }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.map { it.second }.maxByOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min.first
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max.first
            }
        }

        Pair(min, max)
    }
}

suspend inline fun <T, R: Comparable<R>> Collection<T>.minMaxByOrNullParallelly(crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    return (this as Iterable<T>).minMaxByOrNullParallelly(
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

suspend inline fun <T, R: Comparable<R>> Collection<T>.minMaxByOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    return (this as Iterable<T>).minMaxByOrNullParallelly(UInt64(this.size) / concurrentAmount, extractor)
}

suspend inline fun <T, R: Comparable<R>> List<T>.minMaxByOrNullParallelly(crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    return this.minMaxByOrNullParallelly(
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

suspend inline fun <T, R: Comparable<R>> List<T>.minMaxByOrNullParallelly(concurrentAmount: UInt64, crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    return coroutineScope {
        val promises = ArrayList<Deferred<Pair<Pair<T, R>, Pair<T, R>>?>>()
        val segmentAmount = this@minMaxByOrNullParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@minMaxByOrNullParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@minMaxByOrNullParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@minMaxByOrNullParallelly.subList(j, k).map { Pair(it, extractor(it)) }.minMaxByOrNull { it.second }
            })
            i = k
        }

        val segmentResults = promises.mapNotNull { it.await() }
        val minPromise = async(Dispatchers.Default) {
            segmentResults.map { it.first }.minByOrNull { it.second }
        }
        val maxPromise = async(Dispatchers.Default) {
            segmentResults.map { it.second }.maxByOrNull { it.second }
        }

        val min = when (val min = minPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                min.first
            }
        }

        val max = when (val max = maxPromise.await()) {
            null -> {
                return@coroutineScope null
            }

            else -> {
                max.first
            }
        }

        Pair(min, max)
    }
}
