/**
 * 集合类型别名
 * Collection Type Aliases
 *
 * 为 Kotlin 集合类型提供扩展属性，包括无符号大小索引 (usize) 和无符号索引范围 (uIndices)。
 * Provides extension properties for Kotlin collection types, including unsigned size index (usize) and unsigned index range (uIndices).
 */
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

val Collection<*>.usize: UInt64
    get() = UInt64(size)

val Collection<*>.uIndices: IntegerRange<UInt64>
    get() = UInt64.zero until usize




