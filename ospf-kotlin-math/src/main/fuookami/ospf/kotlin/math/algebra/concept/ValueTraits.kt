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

interface Bounded<Self : Comparable<Self>> {
    val isBounded: Boolean
    val minBound: Self?
    val maxBound: Self?

    val hasLowerBound: Boolean
        get() = minBound != null

    val hasUpperBound: Boolean
        get() = maxBound != null

    fun isWithinBounds(value: Self): Boolean {
        val lowerOkay = minBound?.let { value >= it } ?: true
        val upperOkay = maxBound?.let { value <= it } ?: true
        return lowerOkay && upperOkay
    }

    fun clampToBounds(value: Self): Self {
        return when {
            minBound != null && value < minBound!! -> minBound!!
            maxBound != null && value > maxBound!! -> maxBound!!
            else -> value
        }
    }
}

interface Infinite<Self> {
    val supportsInfinity: Boolean
    val positiveInfinity: Self?
    val negativeInfinityValue: Self?

    val hasPositiveInfinity: Boolean
        get() = positiveInfinity != null

    val hasNegativeInfinity: Boolean
        get() = negativeInfinityValue != null

    fun isPositiveInfinity(value: Self): Boolean {
        return positiveInfinity?.let { value == it } ?: false
    }

    fun isNegativeInfinity(value: Self): Boolean {
        return negativeInfinityValue?.let { value == it } ?: false
    }

    fun isInfinite(value: Self): Boolean {
        return isPositiveInfinity(value) || isNegativeInfinity(value)
    }

    fun isFinite(value: Self): Boolean {
        return !isInfinite(value)
    }
}

interface Fixed<Self> {
    val isFixed: Boolean
    val fixedDigits: Int?
    val fixedPrecision: Self?

    val hasFixedPrecision: Boolean
        get() = fixedDigits != null || fixedPrecision != null

    val fixedValue: Self?
        get() = null

    val isDegenerate: Boolean
        get() = fixedValue != null
}

interface Epsilon<Self> {
    val precisionEpsilon: Self?

    val hasEpsilon: Boolean
        get() = precisionEpsilon != null
}

