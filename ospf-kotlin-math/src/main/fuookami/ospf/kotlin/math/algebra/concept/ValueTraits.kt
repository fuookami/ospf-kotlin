/**
 * 值特性
 * Value Traits
 *
 * 定义数值类型的特性接口，包括不变性 (Invariant)、可变性 (Variant)、有界性 (Bounded)、无穷性 (Infinite)、定点性 (Fixed) 和精度特性 (Epsilon)。
 * Defines trait interfaces for numeric types, including invariance (Invariant), variance (Variant), boundedness (Bounded), infinity support (Infinite), fixed-point property (Fixed), and precision characteristics (Epsilon).
 */
package fuookami.ospf.kotlin.math.algebra.concept

/**
 * 不变性接口
 * Invariance interface
 *
 * 表示值类型与内部值类型相同，可直接获取值。
 * Indicates the value type is identical to the internal value type, allowing direct value retrieval.
 *
 * @param T 内部值类型
 * @param T The internal value type
 */
interface Invariant<T> {
    /**
     * 获取内部值
     * Get the internal value
     *
     * @return 内部值
     * @return The internal value
     */
    fun value(): T = this as T
}

/**
 * 可变性接口
 * Variance interface
 *
 * 表示值类型可能与内部值类型不同，内部值可能为空。
 * Indicates the value type may differ from the internal value type, and the internal value may be null.
 *
 * @param T 内部值类型
 * @param T The internal value type
 */
interface Variant<T> {
    /**
     * 获取内部值
     * Get the internal value
     *
     * @return 内部值，可能为空
     * @return The internal value, may be null
     */
    fun value(): T? = null
}

/**
 * 有界性接口
 * Boundedness interface
 *
 * 表示数值类型具有上下界。
 * Indicates the numeric type has lower and upper bounds.
 *
 * @param Self 有界类型
 * @param Self The bounded type
 */
interface Bounded<Self : Comparable<Self>> {
    /** 是否有界 / Whether the type is bounded */
    val isBounded: Boolean
    /** 下界 / Lower bound */
    val minBound: Self?
    /** 上界 / Upper bound */
    val maxBound: Self?

    /** 是否有下界 / Whether a lower bound exists */
    val hasLowerBound: Boolean
        get() = minBound != null

    /** 是否有上界 / Whether an upper bound exists */
    val hasUpperBound: Boolean
        get() = maxBound != null

    /**
     * 判断值是否在边界内
     * Check if a value is within bounds
     *
     * @param value 待检查的值
     * @param value The value to check
     * @return 是否在边界内
     * @return Whether the value is within bounds
     */
    fun isWithinBounds(value: Self): Boolean {
        val lowerOkay = minBound?.let { value >= it } ?: true
        val upperOkay = maxBound?.let { value <= it } ?: true
        return lowerOkay && upperOkay
    }

    /**
     * 将值限制在边界内
     * Clamp a value to within bounds
     *
     * @param value 待限制的值
     * @param value The value to clamp
     * @return 限制后的值
     * @return The clamped value
     */
    fun clampToBounds(value: Self): Self {
        return when {
            minBound != null && value < minBound!! -> minBound!!
            maxBound != null && value > maxBound!! -> maxBound!!
            else -> value
        }
    }
}

/**
 * 无穷性接口
 * Infinity interface
 *
 * 表示数值类型支持正无穷和负无穷。
 * Indicates the numeric type supports positive and negative infinity.
 *
 * @param Self 无穷类型
 * @param Self The infinity type
 */
interface Infinite<Self> {
    /** 是否支持无穷 / Whether infinity is supported */
    val supportsInfinity: Boolean
    /** 正无穷值 / Positive infinity value */
    val positiveInfinity: Self?
    /** 负无穷值 / Negative infinity value */
    val negativeInfinityValue: Self?

    /** 是否有正无穷 / Whether positive infinity exists */
    val hasPositiveInfinity: Boolean
        get() = positiveInfinity != null

    /** 是否有负无穷 / Whether negative infinity exists */
    val hasNegativeInfinity: Boolean
        get() = negativeInfinityValue != null

    /**
     * 判断是否为正无穷
     * Check if the value is positive infinity
     */
    fun isPositiveInfinity(value: Self): Boolean {
        return positiveInfinity?.let { value == it } ?: false
    }

    /**
     * 判断是否为负无穷
     * Check if the value is negative infinity
     */
    fun isNegativeInfinity(value: Self): Boolean {
        return negativeInfinityValue?.let { value == it } ?: false
    }

    /**
     * 判断是否为无穷
     * Check if the value is infinite
     */
    fun isInfinite(value: Self): Boolean {
        return isPositiveInfinity(value) || isNegativeInfinity(value)
    }

    /**
     * 判断是否为有限值
     * Check if the value is finite
     */
    fun isFinite(value: Self): Boolean {
        return !isInfinite(value)
    }
}

/**
 * 定点性接口
 * Fixed-point interface
 *
 * 表示数值类型具有定点精度特性。
 * Indicates the numeric type has fixed-point precision characteristics.
 *
 * @param Self 定点类型
 * @param Self The fixed-point type
 */
interface Fixed<Self> {
    /** 是否定点 / Whether the type is fixed-point */
    val isFixed: Boolean
    /** 定点小数位数 / Fixed-point decimal digits */
    val fixedDigits: Int?
    /** 定点精度值 / Fixed-point precision value */
    val fixedPrecision: Self?

    /** 是否有定点精度 / Whether fixed-point precision exists */
    val hasFixedPrecision: Boolean
        get() = fixedDigits != null || fixedPrecision != null

    /** 定点值 / Fixed value */
    val fixedValue: Self?
        get() = null

    /** 是否退化 / Whether the value is degenerate */
    val isDegenerate: Boolean
        get() = fixedValue != null
}

/**
 * 精度误差接口
 * Epsilon interface
 *
 * 表示数值类型具有精度误差。
 * Indicates the numeric type has a precision epsilon.
 *
 * @param Self 精度误差类型
 * @param Self The epsilon type
 */
interface Epsilon<Self> {
    /** 精度误差值 / Precision epsilon value */
    val precisionEpsilon: Self?

    /** 是否有精度误差 / Whether epsilon exists */
    val hasEpsilon: Boolean
        get() = precisionEpsilon != null
}

