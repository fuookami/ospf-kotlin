package fuookami.ospf.kotlin.utils.parallel

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfParallelly(UInt64.ten, constants, extractor)
}

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    segment: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        val iterator = this@sumOfParallelly.iterator()
        while (iterator.hasNext()) {
            val thisSegment = ArrayList<T>()
            var i = UInt64.zero
            while (iterator.hasNext() && i != segment) {
                thisSegment.add(iterator.next())
                ++i
            }
            promises.add(async(Dispatchers.Default) {
                thisSegment.sumOf(constants, extractor)
            })
        }

        promises.forEach {
            sum += it.await()
        }
    }
    return sum
}

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: TryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfParallelly(UInt64.ten, constants, extractor)
}

suspend inline fun <T, U> Iterable<T>.sumOfParallelly(
    segment: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: TryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    var error: Error? = null
    var sum = constants.zero

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<U>>()
            val iterator = this@sumOfParallelly.iterator()
            while (iterator.hasNext()) {
                val thisSegment = ArrayList<T>()
                var i = UInt64.zero
                while (iterator.hasNext() && i != segment) {
                    thisSegment.add(iterator.next())
                    ++i
                }
                promises.add(async(Dispatchers.Default) {
                    thisSegment.sumOf(constants) {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                constants.zero
                            }
                        }
                    }
                })
            }

            promises.forEach {
                sum += it.await()
            }
        }

        Ok(sum)
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(constants.zero)
    }
}

suspend inline fun <T, U> Collection<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).sumOfParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        constants,
        extractor
    )
}

suspend inline fun <T, U> Collection<T>.sumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).sumOfParallelly(UInt64(this.size) / concurrentAmount, constants, extractor)
}

suspend inline fun <T, U> Collection<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: TryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).sumOfParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        constants,
        extractor
    )
}

suspend inline fun <T, U> Collection<T>.sumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: TryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return (this as Iterable<T>).sumOfParallelly(UInt64(this.size) / concurrentAmount, constants, extractor)
}

suspend inline fun <T, U> List<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        constants,
        extractor
    )
}

suspend inline fun <T, U> List<T>.sumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: Extractor<U, T>
): U
        where U : Arithmetic<U>, U : Plus<U, U> {
    var sum = constants.zero
    coroutineScope {
        val promises = ArrayList<Deferred<U>>()
        val segmentAmount = this@sumOfParallelly.size / concurrentAmount.toInt()
        var i = 0
        while (i != this@sumOfParallelly.size) {
            val j = i
            val k = i + minOf(
                segmentAmount,
                this@sumOfParallelly.size - i
            )
            promises.add(async(Dispatchers.Default) {
                this@sumOfParallelly.subList(j, k).sumOf(constants, extractor)
            })
            i = k
        }

        promises.forEach {
            sum += it.await()
        }
    }
    return sum
}

suspend inline fun <T, U> List<T>.sumOfParallelly(
    constants: ArithmeticConstants<U>,
    crossinline extractor: TryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    return this.sumOfParallelly(
        UInt64(
            maxOf(
                minOf(
                    Flt64(this.size).log(Flt64.two)!!.toFlt64().floor().toUInt64().toInt(),
                    Runtime.getRuntime().availableProcessors()
                ),
                1
            )
        ),
        constants,
        extractor
    )
}

suspend inline fun <T, U> List<T>.sumOfParallelly(
    concurrentAmount: UInt64,
    constants: ArithmeticConstants<U>,
    crossinline extractor: TryExtractor<U, T>
): Ret<U>
        where U : Arithmetic<U>, U : Plus<U, U> {
    var error: Error? = null
    var sum = constants.zero

    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<U>>()
            val segmentAmount = this@sumOfParallelly.size / concurrentAmount.toInt()
            var i = 0
            while (i != this@sumOfParallelly.size) {
                val j = i
                val k = i + minOf(
                    segmentAmount,
                    this@sumOfParallelly.size - i
                )
                promises.add(async(Dispatchers.Default) {
                    this@sumOfParallelly.subList(j, k).sumOf(constants) {
                        when (val result = extractor(it)) {
                            is Ok -> {
                                result.value
                            }

                            is Failed -> {
                                error = result.error
                                cancel()
                                constants.zero
                            }
                        }
                    }
                })
                i = k
            }

            promises.forEach {
                sum += it.await()
            }
        }

        Ok(sum)
    } catch (e: CancellationException) {
        error?.let { Failed(it) } ?: Ok(constants.zero)
    }
}
