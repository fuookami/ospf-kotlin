/**
 * 集合类型别名
 * Collection Type Aliases
 *
 * 为 Kotlin 集合类型提供扩展属性，包括无符号大小索引 (usize) 和无符号索引范围 (uIndices)。
 * Provides extension properties for Kotlin collection types, including unsigned size index (usize) and unsigned index range (uIndices).
 */
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.number.*

/** 无符号大小 / Unsigned size */
val Collection<*>.usize: UInt64
    get() = UInt64(size)

/** 无符号索引范围 / Unsigned index range */
val Collection<*>.uIndices: IntegerRange<UInt64>
    get() = UInt64.zero until usize
