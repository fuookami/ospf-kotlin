package fuookami.ospf.kotlin.utils.math

import kotlin.random.*

fun Random.nextInt64() = Int64(nextLong())
fun Random.nextInt64(until: Int64) = Int64(nextLong(until.value))
fun Random.nextInt64(range: IntegerRange<Int64>) = Int64(nextLong(LongRange(range.first.value, range.last.value)))
fun Random.nextInt64(lowerBound: Int64, upperBound: Int64) = Int64(nextLong(lowerBound.value, upperBound.value))

fun Random.nextUInt64() = UInt64(nextULong())
fun Random.nextUInt64(until: UInt64) = UInt64(nextULong(until.value))
fun Random.nextUInt64(range: IntegerRange<UInt64>) = UInt64(nextULong(ULongRange(range.first.value, range.last.value)))
fun Random.nextUInt64(lowerBound: UInt64, upperBound: UInt64) = UInt64(nextULong(lowerBound.value, upperBound.value))

fun Random.nextFlt64() = Flt64(nextDouble())
fun Random.nextFlt64(until: Flt64) = Flt64(nextDouble(until.value))
fun Random.nextFlt64(lowerBound: Flt64, upperBound: Flt64) = Flt64(nextDouble(lowerBound.value, upperBound.value))
