/**
 * 值特性
 * Value Traits
 *
 * 定义数值类型的特性接口，包括不变性 (Invariant)、可变性 (Variant)、有界性 (Bounded)、无穷性 (Infinite)、定点性 (Fixed) 和精度特性 (Epsilon)。
 * Defines trait interfaces for numeric types, including invariance (Invariant), variance (Variant), boundedness (Bounded), infinity support (Infinite), fixed-point property (Fixed), and precision characteristics (Epsilon).
 */
package fuookami.ospf.kotlin.math.algebra.concept

interface Invariant<T> {
    @Suppress("UNCHECKED_CAST")
    fun value(): T = this as T
}

interface Variant<T> {
    fun value(): T? = null
}

interface Bounded<Self> {
    val isBounded: Boolean
    val minBound: Self?
    val maxBound: Self?
}

interface Infinite<Self> {
    val supportsInfinity: Boolean
    val positiveInfinity: Self?
    val negativeInfinityValue: Self?
}

interface Fixed<Self> {
    val isFixed: Boolean
    val fixedDigits: Int?
    val fixedPrecision: Self?
}

interface Epsilon<Self> {
    val precisionEpsilon: Self?
}

