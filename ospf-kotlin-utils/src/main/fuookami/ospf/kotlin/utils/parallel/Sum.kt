package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*

suspend inline fun <T> Iterable<T>.sumParallelly(constants: ArithmeticConstants<T>): T
        where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumParallelly(UInt64.ten, constants)
}

suspend inline fun <T> Iterable<T>.sumParallelly(segment: UInt64, constants: ArithmeticConstants<T>): T
        where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val iterator = this@sumParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.sum(constants)
            })
        }

        promises.forEach {
            sum += it.await()
        }
    }
    return sum
}

suspend inline fun <T> Collection<T>.sumParallelly(constants: ArithmeticConstants<T>): T
        where T : Arithmetic<T>, T : Plus<T, T> {
    return (this as Iterable<T>).sumParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        constants
    )
}

suspend inline fun <T> Collection<T>.sumParallelly(concurrentAmount: UInt64, constants: ArithmeticConstants<T>): T
        where T : Arithmetic<T>, T : Plus<T, T> {
    return (this as Iterable<T>).sumParallelly(UInt64(this.size) / concurrentAmount, constants)
}

suspend inline fun <T> List<T>.sumParallelly(constants: ArithmeticConstants<T>): T
        where T : Arithmetic<T>, T : Plus<T, T> {
    return this.sumParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        constants
    )
}

suspend inline fun <T> List<T>.sumParallelly(concurrentAmount: UInt64, constants: ArithmeticConstants<T>): T
        where T : Arithmetic<T>, T : Plus<T, T> {
    var sum = constants.zero
    coroutineScope {
        val promises = ArrayList<Deferred<T>>()
        val segmentAmount = this@sumParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@sumParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@sumParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@sumParallelly.subList(j, k).sum(constants)
            })
            i = k
        }

        promises.forEach {
            sum += it.await()
        }
    }
    return sum
}
