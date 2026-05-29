/**
 * 数概念
 * Number Concepts
 *
 * 定义数值类型的核心接口，包括数环、数域、标量、实数、整数、有理数、浮点数等数值类型层次结构。
 * Defines core interfaces for numeric types, including number ring, number field, scalar, real number, integer, rational number, floating number and other numeric type hierarchies.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.*

/**
 * 数环接口
 * Number Ring Interface
 *
 * 数环是具有加法群结构和乘法半群结构的代数结构。
 * A number ring is an algebraic structure with additive group structure and multiplicative semigroup structure.
 */
interface NumberRing<Self> : Ring<Self>, PlusGroup<Self>, TimesSemiGroup<Self>

/**
 * 数域接口
 * Number Field Interface
 *
 * 数域是具有加法群结构和乘法群结构的代数结构。
 * A number field is an algebraic structure with additive group structure and multiplicative group structure.
 */
interface NumberField<Self> : Field<Self>, NumberRing<Self>, TimesGroup<Self>

/**
 * 标量接口
 * Scalar Interface
 *
 * 标量是支持加法、乘法、叉积和绝对值运算的算术类型。
 * A scalar is an arithmetic type that supports addition, multiplication, cross product, and absolute value operations.
 *
 * @param Self 标量类型，必须继承自 Scalar
 * @param Self The scalar type, must extend Scalar
 */
interface Scalar<Self : Scalar<Self>> : Arithmetic<Self>,
    PlusSemiGroup<Self>, TimesSemiGroup<Self>,
    Cross<Self, Self>, Abs<Self> {
    /**
     * 叉积运算，默认实现为乘法
     * Cross product operation, default implementation is multiplication
     *
     * @param rhs 另一个标量
     * @param rhs The other scalar
     * @return 叉积结果
     * @return The cross product result
     */
    override infix fun x(rhs: Self) = this * rhs
}

/**
 * 实数接口
 * Real Number Interface
 *
 * 实数是具有序关系、有界性、无穷性、定点性和精度特性的标量类型，
 * 支持对数、幂、指数和三角函数运算。
 * A real number is a scalar type with ordering, boundedness, infinity, fixed-point, and precision characteristics,
 * supporting logarithm, power, exponential, and trigonometric operations.
 *
 * @param Self 实数类型，必须继承自 RealNumber
 * @param Self The real number type, must extend RealNumber
 */
interface RealNumber<Self : RealNumber<Self>> : Scalar<Self>, Invariant<Self>, Ord<Self>, Eq<Self>,
    Bounded<Self>, Infinite<Self>, Fixed<Self>, Epsilon<Self>,
    Log<FloatingNumber<*>, FloatingNumber<*>>,
    PowF<FloatingNumber<*>, FloatingNumber<*>>,
    Exp<FloatingNumber<*>>, Trigonometry<FloatingNumber<*>> {
    /**
     * 实数常量
     * Real number constants
     */
    override val constants: RealNumberConstants<Self>

    /**
     * 是否有界
     * Whether the number is bounded
     */
    override val isBounded: Boolean get() = true

    /**
     * 最小边界值
     * Minimum bound value
     */
    override val minBound: Self? get() = constants.minimum

    /**
     * 最大边界值
     * Maximum bound value
     */
    override val maxBound: Self? get() = constants.maximum

    /**
     * 是否支持无穷
     * Whether the number supports infinity
     */
    override val supportsInfinity: Boolean get() = constants.infinity != null || constants.negativeInfinity != null

    /**
     * 正无穷值
     * Positive infinity value
     */
    override val positiveInfinity: Self? get() = constants.infinity

    /**
     * 负无穷值
     * Negative infinity value
     */
    override val negativeInfinityValue: Self? get() = constants.negativeInfinity

    /**
     * 是否定点
     * Whether the number is fixed-point
     */
    override val isFixed: Boolean get() = constants.decimalDigits != null

    /**
     * 定点小数位数
     * Fixed-point decimal digits
     */
    override val fixedDigits: Int? get() = constants.decimalDigits

    /**
     * 定点精度值
     * Fixed-point precision value
     */
    override val fixedPrecision: Self? get() = constants.decimalPrecision

    /**
     * 精度误差值
     * Precision epsilon value
     */
    override val precisionEpsilon: Self? get() = constants.epsilon

    /**
     * 判断是否为正无穷
     * Check if the number is positive infinity
     *
     * @return 是否为正无穷
     * @return Whether the number is positive infinity
     */
    fun isInfinity(): Boolean = isPositiveInfinity()

    /**
     * 判断当前值是否为正无穷
     * Check whether the current value is positive infinity
     *
     * @return 是否为正无穷
     * @return Whether the value is positive infinity
     */
    fun isPositiveInfinity(): Boolean = positiveInfinity?.let { this == it } ?: false

    /**
     * 判断是否为负无穷
     * Check if the number is negative infinity
     *
     * @return 是否为负无穷
     * @return Whether the number is negative infinity
     */
    fun isNegativeInfinity(): Boolean = negativeInfinityValue?.let { this == it } ?: false

    /**
     * 判断当前值是否为无穷（正无穷或负无穷）
     * Check whether the current value is infinite (positive or negative infinity)
     *
     * @return 是否为无穷
     * @return Whether the value is infinite
     */
    fun isInfinite(): Boolean = isPositiveInfinity() || isNegativeInfinity()

    /**
     * 判断当前值是否为有限值
     * Check whether the current value is finite
     *
     * @return 是否为有限值
     * @return Whether the value is finite
     */
    fun isFinite(): Boolean = !isInfinite()

    override fun isWithinBounds(value: Self): Boolean {
        val lowerOkay = minBound?.let { value >= it } ?: true
        val upperOkay = maxBound?.let { value <= it } ?: true
        return lowerOkay && upperOkay
    }

    override fun clampToBounds(value: Self): Self {
        return when {
            minBound != null && value < minBound!! -> minBound!!
            maxBound != null && value > maxBound!! -> maxBound!!
            else -> value
        }
    }

    /**
     * 获取自身（CRTP 模式的类型安全转换）
     * Get self (type-safe cast for CRTP pattern)
     *
     * @return 当前实例的 Self 类型引用
     * @return The Self-typed reference to this instance
     */
    @Suppress("UNCHECKED_CAST")
    private fun self(): Self {
        // 安全不变量：RealNumber<Self> 使用自类型约束，运行时 this 与 Self 一致。
        // Safety invariant: RealNumber<Self> uses self-typed constraint, so runtime this matches Self.
        return this as Self
    }

    /**
     * 判断当前值是否在边界范围内
     * Check whether the current value is within bounds
     *
     * @return 是否在边界内
     * @return Whether the value is within bounds
     */
    fun isSelfWithinBounds(): Boolean = isWithinBounds(self())
    /**
     * 将当前值限制在边界范围内
     * Clamp the current value to within bounds
     *
     * @return 限制后的值
     * @return The clamped value
     */
    fun clampSelfToBounds(): Self = clampToBounds(self())

    /**
     * 等价判断
     * Equivalence checking
     *
     * @param rhs 另一个实数
     * @param rhs The other real number
     * @return 是否等价
     * @return Whether the numbers are equivalent
     */
    override infix fun equiv(rhs: Self) = this == rhs

    /**
     * 转换为 8 位有符号整数
     * Convert to 8-bit signed integer
     *
     * @return Int8 值
     * @return Int8 value
     */
    fun toInt8(): Int8

    /**
     * 转换为 16 位有符号整数
     * Convert to 16-bit signed integer
     *
     * @return Int16 值
     * @return Int16 value
     */
    fun toInt16(): Int16

    /**
     * 转换为 32 位有符号整数
     * Convert to 32-bit signed integer
     *
     * @return Int32 值
     * @return Int32 value
     */
    fun toInt32(): Int32

    /**
     * 转换为 64 位有符号整数
     * Convert to 64-bit signed integer
     *
     * @return Int64 值
     * @return Int64 value
     */
    fun toInt64(): Int64

    /**
     * 转换为任意精度有符号整数
     * Convert to arbitrary precision signed integer
     *
     * @return IntX 值
     * @return IntX value
     */
    fun toIntX(): IntX

    /**
     * 转换为 8 位无符号整数
     * Convert to 8-bit unsigned integer
     *
     * @return UInt8 值
     * @return UInt8 value
     */
    fun toUInt8(): UInt8

    /**
     * 转换为 16 位无符号整数
     * Convert to 16-bit unsigned integer
     *
     * @return UInt16 值
     * @return UInt16 value
     */
    fun toUInt16(): UInt16

    /**
     * 转换为 32 位无符号整数
     * Convert to 32-bit unsigned integer
     *
     * @return UInt32 值
     * @return UInt32 value
     */
    fun toUInt32(): UInt32

    /**
     * 转换为 64 位无符号整数
     * Convert to 64-bit unsigned integer
     *
     * @return UInt64 值
     * @return UInt64 value
     */
    fun toUInt64(): UInt64

    /**
     * 转换为任意精度无符号整数
     * Convert to arbitrary precision unsigned integer
     *
     * @return UIntX 值
     * @return UIntX value
     */
    fun toUIntX(): UIntX

    /**
     * 转换为可空 8 位有符号整数
     * Convert to nullable 8-bit signed integer
     *
     * @return NInt8 值
     * @return NInt8 value
     */
    fun toNInt8(): NInt8 = NInt8(toInt8())

    /**
     * 转换为可空 16 位有符号整数
     * Convert to nullable 16-bit signed integer
     *
     * @return NInt16 值
     * @return NInt16 value
     */
    fun toNInt16(): NInt16 = NInt16(toInt16())

    /**
     * 转换为可空 32 位有符号整数
     * Convert to nullable 32-bit signed integer
     *
     * @return NInt32 值
     * @return NInt32 value
     */
    fun toNInt32(): NInt32 = NInt32(toInt32())

    /**
     * 转换为可空 64 位有符号整数
     * Convert to nullable 64-bit signed integer
     *
     * @return NInt64 值
     * @return NInt64 value
     */
    fun toNInt64(): NInt64 = NInt64(toInt64())

    /**
     * 转换为可空任意精度有符号整数
     * Convert to nullable arbitrary precision signed integer
     *
     * @return NIntX 值
     * @return NIntX value
     */
    fun toNIntX(): NIntX = NIntX(toIntX())

    /**
     * 转换为可空 8 位无符号整数
     * Convert to nullable 8-bit unsigned integer
     *
     * @return NUInt8 值
     * @return NUInt8 value
     */
    fun toNUInt8(): NUInt8 = NUInt8(toUInt8())

    /**
     * 转换为可空 16 位无符号整数
     * Convert to nullable 16-bit unsigned integer
     *
     * @return NUInt16 值
     * @return NUInt16 value
     */
    fun toNUInt16(): NUInt16 = NUInt16(toUInt16())

    /**
     * 转换为可空 32 位无符号整数
     * Convert to nullable 32-bit unsigned integer
     *
     * @return NUInt32 值
     * @return NUInt32 value
     */
    fun toNUInt32(): NUInt32 = NUInt32(toUInt32())

    /**
     * 转换为可空 64 位无符号整数
     * Convert to nullable 64-bit unsigned integer
     *
     * @return NUInt64 值
     * @return NUInt64 value
     */
    fun toNUInt64(): NUInt64 = NUInt64(toUInt64())

    /**
     * 转换为可空任意精度无符号整数
     * Convert to nullable arbitrary precision unsigned integer
     *
     * @return NUIntX 值
     * @return NUIntX value
     */
    fun toNUIntX(): NUIntX = NUIntX(toUIntX())

    /**
     * 转换为 8 位有符号整数比率
     * Convert to 8-bit signed integer ratio
     *
     * @return Rtn8 值
     * @return Rtn8 value
     */
    fun toRtn8(): Rtn8 = Rtn8(toInt8(), Int8.one)

    /**
     * 转换为 16 位有符号整数比率
     * Convert to 16-bit signed integer ratio
     *
     * @return Rtn16 值
     * @return Rtn16 value
     */
    fun toRtn16(): Rtn16 = Rtn16(toInt16(), Int16.one)

    /**
     * 转换为 32 位有符号整数比率
     * Convert to 32-bit signed integer ratio
     *
     * @return Rtn32 值
     * @return Rtn32 value
     */
    fun toRtn32(): Rtn32 = Rtn32(toInt32(), Int32.one)

    /**
     * 转换为 64 位有符号整数比率
     * Convert to 64-bit signed integer ratio
     *
     * @return Rtn64 值
     * @return Rtn64 value
     */
    fun toRtn64(): Rtn64 = Rtn64(toInt64(), Int64.one)

    /**
     * 转换为任意精度有符号整数比率
     * Convert to arbitrary precision signed integer ratio
     *
     * @return RtnX 值
     * @return RtnX value
     */
    fun toRtnX(): RtnX = RtnX(toIntX(), IntX.one)

    /**
     * 转换为 8 位无符号整数比率
     * Convert to 8-bit unsigned integer ratio
     *
     * @return URtn8 值
     * @return URtn8 value
     */
    fun toURtn8(): URtn8 = URtn8(toUInt8(), UInt8.one)

    /**
     * 转换为 16 位无符号整数比率
     * Convert to 16-bit unsigned integer ratio
     *
     * @return URtn16 值
     * @return URtn16 value
     */
    fun toURtn16(): URtn16 = URtn16(toUInt16(), UInt16.one)

    /**
     * 转换为 32 位无符号整数比率
     * Convert to 32-bit unsigned integer ratio
     *
     * @return URtn32 值
     * @return URtn32 value
     */
    fun toURtn32(): URtn32 = URtn32(toUInt32(), UInt32.one)

    /**
     * 转换为 64 位无符号整数比率
     * Convert to 64-bit unsigned integer ratio
     *
     * @return URtn64 值
     * @return URtn64 value
     */
    fun toURtn64(): URtn64 = URtn64(toUInt64(), UInt64.one)

    /**
     * 转换为任意精度无符号整数比率
     * Convert to arbitrary precision unsigned integer ratio
     *
     * @return URtnX 值
     * @return URtnX value
     */
    fun toURtnX(): URtnX = URtnX(toUIntX(), UIntX.one)

    /**
     * 转换为 32 位浮点数
     * Convert to 32-bit floating point number
     *
     * @return Flt32 值
     * @return Flt32 value
     */
    fun toFlt32(): Flt32

    /**
     * 转换为 64 位浮点数
     * Convert to 64-bit floating point number
     *
     * @return Flt64 值
     * @return Flt64 value
     */
    fun toFlt64(): Flt64

    /**
     * 转换为任意精度浮点数
     * Convert to arbitrary precision floating point number
     *
     * @return FltX 值
     * @return FltX value
     */
    fun toFltX(): FltX
}

/**
 * 实数常量接口
 * Real Number Constants Interface
 *
 * 提供实数类型的基本常量。
 * Provides basic constants for real number types.
 *
 * @param Self 实数类型，必须继承自 RealNumber
 * @param Self The real number type, must extend RealNumber
 */
interface RealNumberConstants<Self : RealNumber<Self>> : ArithmeticConstants<Self>, RealConst<Self> {
    /**
     * 常量二
     * Constant two
     */
    override val two: Self

    /**
     * 常量三
     * Constant three
     */
    override val three: Self

    /**
     * 常量五
     * Constant five
     */
    override val five: Self

    /**
     * 常量十
     * Constant ten
     */
    override val ten: Self

    /**
     * 最小值
     * Minimum value
     */
    override val minimum: Self

    /**
     * 最大值
     * Maximum value
     */
    override val maximum: Self

    /**
     * 正最小值，默认为一
     * Positive minimum value, defaults to one
     */
    val positiveMinimum get() = one

    /**
     * 小数位数，默认为空
     * Decimal digits, defaults to null
     */
    override val decimalDigits: Int? get() = null

    /**
     * 小数精度值，默认为零
     * Decimal precision value, defaults to zero
     */
    override val decimalPrecision: Self get() = zero

    /**
     * 精度误差值，默认为零
     * Precision epsilon value, defaults to zero
     */
    override val epsilon: Self get() = zero

    /**
     * NaN 值，默认为空
     * NaN value, defaults to null
     */
    override val nan: Self? get() = null

    /**
     * 正无穷值，默认为空
     * Positive infinity value, defaults to null
     */
    override val infinity: Self? get() = null

    /**
     * 负无穷值，默认为空
     * Negative infinity value, defaults to null
     */
    override val negativeInfinity: Self? get() = null
}

/**
 * 整数接口
 * Integer Interface
 *
 * 整数是支持范围运算的实数类型。
 * An integer is a real number type that supports range operations.
 *
 * @param Self 整数类型，必须继承自 RealNumber
 * @param Self The integer type, must extend RealNumber
 */
interface Integer<Self : RealNumber<Self>> : RealNumber<Self>, RangeTo<Self, Self>

/**
 * 有符号整数接口
 * Integer Number Interface
 *
 * 有符号整数是支持幂运算的数域整数类型。
 * A signed integer is an integer number field type that supports power operations.
 *
 * @param Self 有符号整数类型，必须继承自 IntegerNumber
 * @param Self The integer number type, must extend IntegerNumber
 */
interface IntegerNumber<Self : IntegerNumber<Self>> : Integer<Self>, NumberField<Self>, Pow<Self>

/**
 * 无符号整数接口
 * Unsigned Integer Number Interface
 *
 * 无符号整数是支持幂运算的数域整数类型。
 * An unsigned integer is an integer number field type that supports power operations.
 *
 * @param Self 无符号整数类型，必须继承自 UIntegerNumber
 * @param Self The unsigned integer number type, must extend UIntegerNumber
 */
interface UIntegerNumber<Self : UIntegerNumber<Self>> : Integer<Self>, NumberField<Self>, Pow<Self>

/**
 * 有理数接口
 * Rational Number Interface
 *
 * 有理数是由两个整数构成的比例表示的数域实数类型。
 * A rational number is a real number field type represented by a ratio of two integers.
 *
 * @param Self 有理数类型，必须继承自 RationalNumber
 * @param Self The rational number type, must extend RationalNumber
 * @param I 整数类型
 * @param I The integer type
 */
interface RationalNumber<Self : RationalNumber<Self, I>, I> : RealNumber<Self>, NumberField<Self>, Pow<Self>
        where I : Integer<I>, I : NumberField<I>

/**
 * 有理数常量接口
 * Rational Number Constants Interface
 *
 * 提供有理数类型的常量，包括半值常量。
 * Provides constants for rational number types, including half value constant.
 *
 * @param Self 有理数类型，必须继承自 RationalNumber
 * @param Self The rational number type, must extend RationalNumber
 * @param I 整数类型
 * @param I The integer type
 */
interface RationalNumberConstants<Self : RationalNumber<Self, I>, I> : RealNumberConstants<Self>
        where I : Integer<I>, I : NumberField<I> {
    /**
     * 常量 1/2
     * Constant 1/2
     */
    val half: Self
}

/**
 * 浮点数接口
 * Floating Number Interface
 *
 * 浮点数是支持幂运算和倒数的数域实数类型。
 * A floating number is a real number field type that supports power operations and reciprocal.
 *
 * @param Self 浮点数类型，必须继承自 FloatingNumber
 * @param Self The floating number type, must extend FloatingNumber
 */
interface FloatingNumber<Self : FloatingNumber<Self>> : RealNumber<Self>, NumberField<Self>, Pow<Self>, Reciprocal<Self> {
    /**
     * 浮点数常量
     * Floating number constants
     */
    override val constants: FloatingNumberConstants<Self>
}

/**
 * 浮点数常量接口
 * Floating Number Constants Interface
 *
 * 提供浮点数类型的常量，包括半值、圆周率、自然常数和常用对数常量。
 * Provides constants for floating number types, including half, pi, e, and lg2 constants.
 *
 * @param Self 浮点数类型，必须继承自 FloatingNumber
 * @param Self The floating number type, must extend FloatingNumber
 */
interface FloatingNumberConstants<Self : FloatingNumber<Self>> : RealNumberConstants<Self>, FloatingConst<Self> {
    /**
     * 正最小值，默认为精度误差值
     * Positive minimum value, defaults to epsilon
     */
    override val positiveMinimum: Self get() = epsilon

    /**
     * 常量 1/2
     * Constant 1/2
     */
    override val half: Self

    /**
     * 圆周率常量
     * Pi constant
     */
    override val pi: Self

    /**
     * 自然常数
     * E constant
     */
    override val e: Self

    /**
     * 以 2 为底的常用对数常量
     * Log base 2 constant
     */
    override val lg2: Self
}

/**
 * 数值有符号整数接口
 * Numeric Integer Number Interface
 *
 * 数值有符号整数是支持加法群、乘法半群、倒数、除法、整除、取余和幂运算的整数类型。
 * A numeric integer number is an integer type that supports additive group, multiplicative semigroup, reciprocal, division, integer division, remainder, and power operations.
 *
 * @param Self 数值有符号整数类型
 * @param Self The numeric integer number type
 * @param I 有符号整数类型
 * @param I The integer number type
 */
interface NumericIntegerNumber<Self : NumericIntegerNumber<Self, I>, I : IntegerNumber<I>> : Integer<Self>,
    PlusGroup<Self>, TimesSemiGroup<Self>,
    Reciprocal<RationalNumber<*, I>>,
    Div<Self, RationalNumber<*, I>>,
    IntDiv<Self, Self>,
    Rem<Self, Self>,
    Pow<RationalNumber<*, I>>

/**
 * 数值无符号整数接口
 * Numeric Unsigned Integer Number Interface
 *
 * 数值无符号整数是支持加法半群、乘法半群、自减、取负、减法、倒数、除法、整除、取余和幂运算的整数类型。
 * A numeric unsigned integer number is an integer type that supports additive semigroup, multiplicative semigroup, decrement, negation, subtraction, reciprocal, division, integer division, remainder, and power operations.
 *
 * @param Self 数值无符号整数类型
 * @param Self The numeric unsigned integer number type
 * @param I 无符号整数类型
 * @param I The unsigned integer number type
 */
interface NumericUIntegerNumber<Self : NumericUIntegerNumber<Self, I>, I : UIntegerNumber<I>> : Integer<Self>,
    PlusSemiGroup<Self>, TimesSemiGroup<Self>,
    Dec<Self>,
    Neg<NumericIntegerNumber<*, *>>,
    Minus<Self, NumericIntegerNumber<*, *>>,
    Reciprocal<RationalNumber<*, I>>,
    Div<Self, RationalNumber<*, I>>,
    IntDiv<Self, Self>,
    Rem<Self, Self>,
    Pow<RationalNumber<*, I>>
