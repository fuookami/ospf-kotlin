/**
 * 随机数生成
 * Random Number Generation
 *
 * 为 Kotlin Random 类提供扩展函数，支持生成 Int64、UInt64 和 Flt64 等类型的随机数，可指定范围或边界。
 * Provides extension functions for Kotlin Random class, supporting generation of random numbers in Int64, UInt64, and Flt64 types, with optional range or bound specifications.
 */
package fuookami.ospf.kotlin.math

import kotlin.random.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** 生成随机 Int64 / Generate random Int64 */
fun Random.nextInt64() = Int64(nextLong())
/** 生成 [0, until) 范围的随机 Int64 / Generate random Int64 in [0, until) */
fun Random.nextInt64(until: Int64) = Int64(nextLong(until.value))
/** 生成指定区间内的随机 Int64 / Generate random Int64 in specified range */
fun Random.nextInt64(range: IntegerRange<Int64>) = Int64(nextLong(LongRange(range.first.value, range.last.value)))
/** 生成 [lowerBound, upperBound) 范围的随机 Int64 / Generate random Int64 in [lowerBound, upperBound) */
fun Random.nextInt64(lowerBound: Int64, upperBound: Int64) = Int64(nextLong(lowerBound.value, upperBound.value))

/** 生成随机 UInt64 / Generate random UInt64 */
fun Random.nextUInt64() = UInt64(nextULong())
/** 生成 [0, until) 范围的随机 UInt64 / Generate random UInt64 in [0, until) */
fun Random.nextUInt64(until: UInt64) = UInt64(nextULong(until.value))
/** 生成指定区间内的随机 UInt64 / Generate random UInt64 in specified range */
fun Random.nextUInt64(range: IntegerRange<UInt64>) = UInt64(nextULong(ULongRange(range.first.value, range.last.value)))
/** 生成 [lowerBound, upperBound) 范围的随机 UInt64 / Generate random UInt64 in [lowerBound, upperBound) */
fun Random.nextUInt64(lowerBound: UInt64, upperBound: UInt64) = UInt64(nextULong(lowerBound.value, upperBound.value))

/** 生成随机 Flt64 / Generate random Flt64 */
fun Random.nextFlt64() = Flt64(nextDouble())
/** 生成 [0, until) 范围的随机 Flt64 / Generate random Flt64 in [0, until) */
fun Random.nextFlt64(until: Flt64) = Flt64(nextDouble(until.value))
/** 生成 [lowerBound, upperBound) 范围的随机 Flt64 / Generate random Flt64 in [lowerBound, upperBound) */
fun Random.nextFlt64(lowerBound: Flt64, upperBound: Flt64) = Flt64(nextDouble(lowerBound.value, upperBound.value))
