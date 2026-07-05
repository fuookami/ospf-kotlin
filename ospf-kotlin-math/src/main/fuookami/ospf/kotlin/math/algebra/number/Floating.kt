/**
 * 浮点数模块
 * Floating-Point Number Module
 *
 * 本模块定义了浮点数的类型系统，包括 Flt32、Flt64 和 FltX（任意精度浮点数）。
 * 这些类型提供了完整的算术运算、比较操作、类型转换以及各种数学函数支持，
 * 包括三角函数、双曲函数、指数函数、对数函数等。
 *
 * This module defines the floating-point number type system, including Flt32, Flt64, and FltX (arbitrary precision floating-point number).
 * These types provide full support for arithmetic operations, comparison operations, type conversions, and various mathematical functions,
 * including trigonometric functions, hyperbolic functions, exponential functions, logarithmic functions, etc.
 */
package fuookami.ospf.kotlin.math.algebra.number

import java.math.*
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.operator.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.orderOf

/**
 * 将浮点数转换为有理数
 * Convert floating-point number to rational number
 *
 * 将浮点数的字符串表示转换为有理数形式（分子/分母），并进行约分化简。
 * Converts the string representation of a floating-point number to rational form (numerator/denominator) and simplifies.
 *
 * @param f 浮点数值
 *          The floating-point number value
 * @param converter1 字符串到整数类型的转换函数
 *                   The conversion function from string to integer type
 * @param converter2 Long 到整数类型的转换函数
 *                   The conversion function from Long to integer type
 * @param ctor 有理数构造函数
 *             The rational number constructor
 * @return 有理数值
 *         The rational number value
 */
private fun <F : FloatingNumber<F>, I : Integer<I>, R : Rational<R, I>> floatingToRational(
    f: F,
    converter1: (String) -> I,
    converter2: (Long) -> I,
    ctor: (I, I) -> R
): R {
    val ds = BigDecimal(f.toString()).stripTrailingZeros().toPlainString()
    val index = ds.indexOf('.')
    if (index == -1) {
        val num = converter1(ds)
        return ctor(num, num.constants.one)
    }
    var num = ds.replace(".", "").toLong()
    var den = 1L
    for (n in 1 until (ds.length - index)) {
        den *= 10L
    }
    while ((num % 2L == 0L) && (den % 2L == 0L)) {
        num /= 2L
        den /= 2L
    }
    while ((num % 5L == 0L) && (den % 5L == 0L)) {
        num /= 5L
        den /= 5L
    }
    return ctor(converter2(num), converter2(den))
}

/**
 * 银行家舍入函数
 * Banker's rounding function
 *
 * 使用银行家舍入法（也称为四舍六入五成双）对浮点数进行舍入。
 * 当小数部分正好为 0.5 时，舍入到最近的偶数。
 *
 * Rounds a floating-point number using banker's rounding (also known as round half to even).
 * When the fractional part is exactly 0.5, rounds to the nearest even number.
 *
 * @param value 要舍入的浮点数值
 *              The floating-point number value to round
 * @return 舍入后的浮点数值
 *         The rounded floating-point number value
 */
private fun <F : FloatingImpl<F>> bankerRound(value: F): F {
    val fractional = value - value.floor()

    return if (fractional gr value.constants.half) {
        value.ceil()
    } else if (fractional ls value.constants.half) {
        value.floor()
    } else {
        val integer = value.floor()
        if (integer % value.constants.two eq value.constants.zero) {
            integer
        } else {
            integer + value.constants.one
        }
    }
}

/**
 * 浮点数实现接口
 * Floating-Point Number Implementation Interface
 *
 * 提供浮点数类型的通用实现，包括相等比较、大小比较、自增自减、
 * 幂运算、开方、对数、有理数转换等操作的默认实现。
 *
 * Provides common implementation for floating-point types, including default implementations
 * for equality comparison, magnitude comparison, increment/decrement, power operations,
 * root extraction, logarithm, rational number conversion, and other operations.
 *
 * @param Self 实现此接口的具体类型
 *             The concrete type implementing this interface
 */
interface FloatingImpl<Self : FloatingImpl<Self>> : FloatingNumber<Self> {
    override infix fun eq(rhs: Self) = (this - rhs).abs() <= this.constants.decimalPrecision
    override infix fun neq(rhs: Self) = !this.eq(rhs)

    override infix fun ls(rhs: Self) = (this - rhs) < -this.constants.decimalPrecision
    override infix fun leq(rhs: Self) = (this - rhs) <= this.constants.decimalPrecision
    override infix fun gr(rhs: Self) = (this - rhs) > this.constants.decimalPrecision
    override infix fun geq(rhs: Self) = (this - rhs) >= -this.constants.decimalPrecision

    override operator fun inc(): Self = this + constants.one
    override operator fun dec(): Self = this - constants.one

    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    /**
     * 将浮点数值强制转换为 Self 类型
     * Cast a floating-point value to Self type
     *
     * 安全不变量：Self 实现 FloatingImpl<Self>，pow/log 返回值运行时与 Self 一致。
     * Safety invariant: Self implements FloatingImpl<Self>, and pow/log runtime results are Self-compatible.
     *
     * @param value 要转换的值
     *              The value to cast
     * @return 转换后的 Self 值
     *         The cast Self value
     */
    @Suppress("UNCHECKED_CAST")
    private fun castFloatingToSelf(value: Any?): Self {
        return value as Self
    }

    /**
     * 将可空浮点数值强制转换为 Self 类型
     * Cast a nullable floating-point value to Self type
     *
     * 安全不变量：同上；null 分支保留，非 null 分支运行时为 Self。
     * Safety invariant: same as above; null stays null, non-null runtime value is Self-compatible.
     *
     * @param value 要转换的可空值
     *              The nullable value to cast
     * @return 转换后的 Self 值或 null
     *         The cast Self value or null
     */
    @Suppress("UNCHECKED_CAST")
    private fun castNullableFloatingToSelf(value: Any?): Self? {
        return value as Self?
    }

    /** 计算平方根 / Compute square root */
    override fun sqrt(): Self? = castNullableFloatingToSelf(pow(constants.one / constants.two))
    /** 计算立方根 / Compute cube root */
    override fun cbrt(): Self? = castNullableFloatingToSelf(pow(constants.one / constants.three))

    override fun lg(): Self? = castNullableFloatingToSelf(log(constants.ten))
    override fun lg2(): Self? = castNullableFloatingToSelf(log(constants.two))
    override fun ln(): Self? = castNullableFloatingToSelf(log(constants.e))

    override fun toRtn8() = floatingToRational(value(), { Int8(it.toByte()) }, { Int8(it.toByte()) }, Rtn8::invoke)
    override fun toRtn16() = floatingToRational(value(), { Int16(it.toShort()) }, { Int16(it.toShort()) }, Rtn16::invoke)
    override fun toRtn32() = floatingToRational(value(), { Int32(it.toInt()) }, { Int32(it.toInt()) }, Rtn32::invoke)
    override fun toRtn64() = floatingToRational(value(), { Int64(it.toLong()) }, { Int64(it) }, Rtn64::invoke)
    override fun toRtnX() = floatingToRational(value(), { IntX(it) }, { IntX(it) }, RtnX::invoke)

    override fun toURtn8() = floatingToRational(value(), { UInt8(it.toUByte()) }, { UInt8(it.toUByte()) }, URtn8::invoke)
    override fun toURtn16() = floatingToRational(value(), { UInt16(it.toUShort()) }, { UInt16(it.toUShort()) }, URtn16::invoke)
    override fun toURtn32() = floatingToRational(value(), { UInt32(it.toUInt()) }, { UInt32(it.toUInt()) }, URtn32::invoke)
    override fun toURtn64() = floatingToRational(value(), { UInt64(it.toULong()) }, { UInt64(it.toULong()) }, URtn64::invoke)

    override fun toURtnX() = floatingToRational(value(), { UIntX(it) }, { UIntX(it) }, URtnX::invoke)

    /**
     * 向下取整
     * Floor
     *
     * @return 向下取整后的值
     *         The floored value
     */
    fun floor(): Self
    /**
     * 向上取整
     * Ceiling
     *
     * @return 向上取整后的值
     *         The ceiled value
     */
    fun ceil(): Self
    /**
     * 四舍五入
     * Round half up
     *
     * @return 四舍五入后的值
     *         The rounded value
     */
    fun round(): Self
    /**
     * 截断小数部分
     * Truncate fractional part
     *
     * @return 截断后的值
     *         The truncated value
     */
    fun trunc(): Self
    /**
     * 银行家舍入（四舍六入五成双）
     * Banker's rounding (round half to even)
     *
     * @return 银行家舍入后的值
     *         The banker-rounded value
     */
    fun bankerRound(): Self

    /**
     * 向下取整到指定精度
     * Floor to specified precision
     *
     * @param precision 小数精度位数
     *                  The number of decimal places
     * @return 舍入后的值
     *         The rounded value
     */
    fun floorTo(precision: Int = this.constants.decimalDigits!!): Self
    /**
     * 向上取整到指定精度
     * Ceil to specified precision
     *
     * @param precision 小数精度位数
     *                  The number of decimal places
     * @return 舍入后的值
     *         The rounded value
     */
    fun ceilTo(precision: Int = this.constants.decimalDigits!!): Self
    /**
     * 四舍五入到指定精度
     * Round to specified precision
     *
     * @param precision 小数精度位数
     *                  The number of decimal places
     * @return 舍入后的值
     *         The rounded value
     */
    fun roundTo(precision: Int = this.constants.decimalDigits!!): Self
    /**
     * 截断到指定精度
     * Truncate to specified precision
     *
     * @param precision 小数精度位数
     *                  The number of decimal places
     * @return 舍入后的值
     *         The rounded value
     */
    fun truncTo(precision: Int = this.constants.decimalDigits!!): Self
    /**
     * 银行家舍入到指定精度
     * Banker's round to specified precision
     *
     * @param precision 小数精度位数
     *                  The number of decimal places
     * @return 舍入后的值
     *         The rounded value
     */
    fun bankerRoundTo(precision: Int = this.constants.decimalDigits!!): Self
}

/**
 * Flt32 序列化器
 * Flt32 Serializer
 *
 * 用于 Flt32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Flt32 type in the Kotlin serialization framework.
 */
data object Flt32Serializer : KSerializer<Flt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flt32", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Flt32) {
        encoder.encodeDouble(value.value.toDouble())
    }

    override fun deserialize(decoder: Decoder): Flt32 {
        return Flt32(decoder.decodeDouble().toFloat())
    }
}

/**
 * Flt32 接口
 * Flt32 Interface
 *
 * 定义了转换为 Float 的能力。
 * Defines the ability to convert to Float.
 */
interface Flt32Interface {
    /**
     * 转换为 Float 值
     * Convert to Float value
     *
     * @return Float 值
     *         The Float value
     */
    fun toFloat(): Float
}

/**
 * 32位浮点数
 * 32-bit Floating-Point Number
 *
 * 基于 Kotlin Float 类型封装的 32 位单精度浮点数，符合 IEEE 754 标准。
 * 支持完整的算术运算、比较操作、类型转换以及各种数学函数。
 * 精度约为 6-7 位有效数字。
 *
 * A 32-bit single-precision floating-point number encapsulated based on Kotlin Float type, conforming to IEEE 754 standard.
 * Supports full arithmetic operations, comparison operations, type conversions, and various mathematical functions.
 * Precision is approximately 6-7 significant digits.
 *
 * @property value 内部的 Float 值
 *                 The internal Float value
 */
@JvmInline
@Serializable(with = Flt32Serializer::class)
value class Flt32(internal val value: Float) : Flt32Interface, FloatingImpl<Flt32>, Copyable<Flt32> {
    /**
     * Flt32 常量对象
     * Flt32 Constants Object
     *
     * 提供常用的数值常量，包括数学常数（pi、e）、特殊值（nan、infinity）等。
     * Provides common numeric constants, including mathematical constants (pi, e), special values (nan, infinity), etc.
     */
    companion object : FloatingNumberConstants<Flt32> {
        @JvmStatic
        override val zero: Flt32 get() = Flt32(0.0F)

        @JvmStatic
        override val one: Flt32 get() = Flt32(1.0F)

        @JvmStatic
        override val two: Flt32 get() = Flt32(2.0F)

        @JvmStatic
        override val three: Flt32 get() = Flt32(3.0F)

        @JvmStatic
        override val five: Flt32 get() = Flt32(5.0F)

        @JvmStatic
        override val ten: Flt32 get() = Flt32(10.0F)

        @JvmStatic
        override val minimum: Flt32 get() = Flt32(-Float.MAX_VALUE)

        @JvmStatic
        override val maximum: Flt32 get() = Flt32(Float.MAX_VALUE)

        @JvmStatic
        override val decimalDigits: Int get() = 6

        @JvmStatic
        override val decimalPrecision: Flt32 get() = Flt32(1.19209e-07F)

        @JvmStatic
        override val epsilon: Flt32 get() = Flt32(Float.MIN_VALUE)

        @JvmStatic
        override val nan: Flt32 get() = Flt32(Float.NaN)

        @JvmStatic
        override val infinity: Flt32 get() = Flt32(Float.POSITIVE_INFINITY)

        @JvmStatic
        override val negativeInfinity: Flt32 get() = Flt32(Float.NEGATIVE_INFINITY)

        @JvmStatic
        override val half: Flt32 get() = Flt32(0.5f)

        @JvmStatic
        override val pi: Flt32 get() = Flt32(PI.toFloat())

        @JvmStatic
        override val e: Flt32 get() = Flt32(E.toFloat())

        @JvmStatic
        override val lg2: Flt32 by lazy {
            ln(two, this)!!
        }
    }

    override val constants: FloatingNumberConstants<Flt32> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = Flt32(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: Flt32) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: Flt32) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = Flt32(-value)
    /** 绝对值 / Absolute value */
    override fun abs() = Flt32(abs(value))
    /** 倒数 / Reciprocal */
    override fun reciprocal() = Flt32(1.0F / value)

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Flt32) = Flt32(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Flt32) = Flt32(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Flt32) = Flt32(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Flt32) = Flt32(value / rhs.value)
    /**
     * 整数除法
     * Integer division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 整数除法结果
     *         The integer division result
     */
    override fun intDiv(rhs: Flt32) = Flt32(value - value % rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: Flt32) = Flt32(value % rhs.value)

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果；当基数不支持时返回 null
     *         The logarithm result; null if the base is unsupported
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value, base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> toFltX().log(base.toFltX())
    }

    /**
     * 计算整数次幂
     * Calculate integer power
     *
     * @param index 整数指数
     *              The integer exponent
     * @return 幂运算结果
     *         The power result
     */
    override fun pow(index: Int) = pow(copy(), index, Flt32)

    /**
     * 计算浮点数次幂
     * Calculate floating-point power
     *
     * @param index 指数
     *              The exponent
     * @return 幂运算结果
     *         The power result
     */
    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? = when (index) {
        is Flt32 -> Flt32(value.pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> toFltX().pow(index.toFltX())
    }

    /** 平方根 / Square root */
    override fun sqrt() = Flt32(sqrt(value))
    /** 立方根 / Cube root */
    override fun cbrt() = Flt32(value.toDouble().pow(1.0 / 3.0).toFloat())

    /** 指数函数 e^x / Exponential function e^x */
    override fun exp() = Flt32(exp(value))

    /** 正弦 / Sine */
    override fun sin() = Flt32(sin(value))
    /** 余弦 / Cosine */
    override fun cos() = Flt32(cos(value))
    /** 正割；当余弦为零时返回 null / Secant; null when cosine is zero */
    override fun sec(): Flt32? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    /** 余割；当正弦为零时返回 null / Cosecant; null when sine is zero */
    override fun csc(): Flt32? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    /** 正切；当余弦为零时返回 null / Tangent; null when cosine is zero */
    override fun tan(): Flt32? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            this.sin() / temp
        }
    }

    /** 余切；当正弦为零时返回 null / Cotangent; null when sine is zero */
    override fun cot(): Flt32? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            this.cos() / temp
        }
    }

    /** 反正弦；当值超出 [-1, 1] 时返回 null / Arcsine; null when value is outside [-1, 1] */
    override fun asin(): Flt32? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt32(asin(value))
        }
    }

    /** 反余弦；当值超出 [-1, 1] 时返回 null / Arccosine; null when value is outside [-1, 1] */
    override fun acos(): Flt32? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt32(acos(value))
        }
    }

    /** 反正割；当值为零时返回 null / Arcsecant; null when value is zero */
    override fun asec(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acos()
        }
    }

    /** 反余割；当值为零时返回 null / Arccosecant; null when value is zero */
    override fun acsc(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asin()
        }
    }

    /** 反正切 / Arctangent */
    override fun atan() = Flt32(atan(value))
    /** 反余切；当值为零时返回 null / Arccotangent; null when value is zero */
    override fun acot(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atan()
        }
    }

    /** 双曲正弦 / Hyperbolic sine */
    override fun sinh() = Flt32(sinh(value))
    /** 双曲余弦 / Hyperbolic cosine */
    override fun cosh() = Flt32(cosh(value))
    /** 双曲正割 / Hyperbolic secant */
    override fun sech() = this.cosh().reciprocal()
    /** 双曲余割；当值为零时返回 null / Hyperbolic cosecant; null when value is zero */
    override fun csch(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.sinh().reciprocal()
        }
    }

    /** 双曲正切 / Hyperbolic tangent */
    override fun tanh() = Flt32(tanh(value))
    /** 双曲余切；当值为零时返回 null / Hyperbolic cotangent; null when value is zero */
    override fun coth(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.tanh().reciprocal()
        }
    }

    /** 反双曲正弦 / Inverse hyperbolic sine */
    override fun asinh() = Flt32(asinh(value))
    /** 反双曲余弦；当值小于 1 时返回 null / Inverse hyperbolic cosine; null when value is less than 1 */
    override fun acosh(): Flt32? {
        return if (this ls one) {
            null
        } else {
            Flt32(acosh(value))
        }
    }

    /** 反双曲正割；当值为零时返回 null / Inverse hyperbolic secant; null when value is zero */
    override fun asech(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acosh()
        }
    }

    /** 反双曲余割；当值为零时返回 null / Inverse hyperbolic cosecant; null when value is zero */
    override fun acsch(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asinh()
        }
    }

    /** 反双曲正切；当值的绝对值 >= 1 时返回 null / Inverse hyperbolic tangent; null when |value| >= 1 */
    override fun atanh(): Flt32? {
        return if (this leq -one || this geq one) {
            null
        } else {
            Flt32(atanh(value))
        }
    }

    /** 反双曲余切；当值为零时返回 null / Inverse hyperbolic cotangent; null when value is zero */
    override fun acoth(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atanh()
        }
    }

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toInt().toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toInt().toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toString())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUInt().toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toUInt().toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value.toString())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = copy()
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toDouble())

    /** 转换为原始 Float 值 / Convert to raw Float value */
    override fun toFloat() = value
    /** 向下取整 / Floor */
    override fun floor() = Flt32(floor(value))
    /** 向上取整 / Ceiling */
    override fun ceil() = Flt32(ceil(value))
    /** 四舍五入 / Round half up */
    override fun round() = Flt32(round(value))
    /** 截断小数部分 / Truncate fractional part */
    override fun trunc() = Flt32(truncate(value))
    /** 银行家舍入 / Banker's rounding */
    override fun bankerRound() = bankerRound(this)

    /** 向下取整到指定精度 / Floor to specified precision */
    override fun floorTo(precision: Int) = Flt32(floor(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    /** 向上取整到指定精度 / Ceil to specified precision */
    override fun ceilTo(precision: Int) = Flt32(ceil(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    /** 四舍五入到指定精度 / Round to specified precision */
    override fun roundTo(precision: Int) = Flt32(round(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    /** 截断到指定精度 / Truncate to specified precision */
    override fun truncTo(precision: Int) = Flt32(truncate(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    /** 银行家舍入到指定精度 / Banker's round to specified precision */
    override fun bankerRoundTo(precision: Int) = bankerRound(Flt32(value * 10.0F.pow(precision))) / Flt32(10.0F.pow(precision))
}

/**
 * Flt64 序列化器
 * Flt64 Serializer
 *
 * 用于 Flt64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Flt64 type in the Kotlin serialization framework.
 */
data object Flt64Serializer : KSerializer<Flt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flt64", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Flt64) {
        encoder.encodeDouble(value.value)
    }

    override fun deserialize(decoder: Decoder): Flt64 {
        return Flt64(decoder.decodeDouble())
    }
}

/**
 * Flt64 接口
 * Flt64 Interface
 *
 * 定义了转换为 Double 的能力。
 * Defines the ability to convert to Double.
 */
interface Flt64Interface {
    /**
     * 转换为 Double 值
     * Convert to Double value
     *
     * @return Double 值
     *         The Double value
     */
    fun toDouble(): Double
}

/**
 * 64位浮点数
 * 64-bit Floating-Point Number
 *
 * 基于 Kotlin Double 类型封装的 64 位双精度浮点数，符合 IEEE 754 标准。
 * 支持完整的算术运算、比较操作、类型转换以及各种数学函数。
 * 精度约为 15-16 位有效数字。这是最常用的浮点数类型。
 *
 * A 64-bit double-precision floating-point number encapsulated based on Kotlin Double type, conforming to IEEE 754 standard.
 * Supports full arithmetic operations, comparison operations, type conversions, and various mathematical functions.
 * Precision is approximately 15-16 significant digits. This is the most commonly used floating-point type.
 *
 * @property value 内部的 Double 值
 *                 The internal Double value
 */
@JvmInline
@Serializable(with = Flt64Serializer::class)
value class Flt64(internal val value: Double) : Flt64Interface, FloatingImpl<Flt64>, Copyable<Flt64> {
    /**
     * 从 Int 构造 Flt64 的构造函数
     * Constructor for Flt64 from Int
     *
     * @param value Int 值
     *              The Int value
     */
    constructor(value: Int) : this(value.toDouble())

    /**
     * Flt64 常量对象
     * Flt64 Constants Object
     *
     * 提供常用的数值常量，包括数学常数（pi、e）、特殊值（nan、infinity）等。
     * Provides common numeric constants, including mathematical constants (pi, e), special values (nan, infinity), etc.
     */
    companion object : FloatingNumberConstants<Flt64>, Flt64ValueConverter<Flt64> {
        @JvmStatic
        override val zero: Flt64 get() = Flt64(0.0)

        @JvmStatic
        override val one: Flt64 get() = Flt64(1.0)

        @JvmStatic
        override val two: Flt64 get() = Flt64(2.0)

        @JvmStatic
        override val three: Flt64 get() = Flt64(3.0)

        @JvmStatic
        override val five: Flt64 get() = Flt64(5.0)

        @JvmStatic
        override val ten: Flt64 get() = Flt64(10.0)

        @JvmStatic
        override val minimum: Flt64 get() = Flt64(-Double.MAX_VALUE)

        @JvmStatic
        override val maximum: Flt64 get() = Flt64(Double.MAX_VALUE)

        @JvmStatic
        override val decimalDigits: Int get() = 15

        @JvmStatic
        override val decimalPrecision: Flt64 get() = Flt64(2.22045e-16)

        @JvmStatic
        override val nan: Flt64 get() = Flt64(Double.NaN)

        @JvmStatic
        override val epsilon: Flt64 get() = Flt64(Double.MIN_VALUE)

        @JvmStatic
        override val infinity: Flt64 get() = Flt64(Double.POSITIVE_INFINITY)

        @JvmStatic
        override val negativeInfinity: Flt64 get() = Flt64(Double.NEGATIVE_INFINITY)

        @JvmStatic
        override val half: Flt64 get() = Flt64(0.5)

        @JvmStatic
        override val pi: Flt64 get() = Flt64(PI)

        @JvmStatic
        override val e: Flt64 get() = Flt64(E)

        @JvmStatic
        override val lg2: Flt64 by lazy {
            ln(two, this)!!
        }

        override fun intoValue(value: Flt64): Flt64 = value
        override fun fromValue(value: Flt64): Flt64 = value
    }

    override val constants: FloatingNumberConstants<Flt64> get() = Flt64

    /** 创建副本 / Create a copy */
    override fun copy() = Flt64(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: Flt64) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: Flt64) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = Flt64(-value)
    /** 绝对值 / Absolute value */
    override fun abs() = Flt64(abs(value))
    /** 倒数 / Reciprocal */
    override fun reciprocal() = Flt64(1.0 / value)

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Flt64) = Flt64(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Flt64) = Flt64(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Flt64) = Flt64(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Flt64) = Flt64(value / rhs.value)
    /**
     * 整数除法
     * Integer division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 整数除法结果
     *         The integer division result
     */
    override fun intDiv(rhs: Flt64) = Flt64(value - value % rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: Flt64) = Flt64(value % rhs.value)

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果；当基数不支持时返回 null
     *         The logarithm result; null if the base is unsupported
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt64(log(value, base.value.toDouble()))
        is Flt64 -> Flt64(log(value, base.value))
        is FltX -> toFltX().log(base)
        else -> toFltX().log(base.toFltX())
    }

    /**
     * 计算整数次幂
     * Calculate integer power
     *
     * @param index 整数指数
     *              The integer exponent
     * @return 幂运算结果
     *         The power result
     */
    override fun pow(index: Int) = pow(copy(), index, Flt64)

    /**
     * 计算浮点数次幂
     * Calculate floating-point power
     *
     * @param index 指数
     *              The exponent
     * @return 幂运算结果
     *         The power result
     */
    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? = when (index) {
        is Flt32 -> Flt64(value.pow(index.value.toDouble()))
        is Flt64 -> Flt64(value.pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> toFltX().pow(index.toFltX())
    }

    /** 平方根 / Square root */
    override fun sqrt() = Flt64(sqrt(value))
    /** 立方根 / Cube root */
    override fun cbrt() = Flt64(value.pow(1.0 / 3.0))

    /** 指数函数 e^x / Exponential function e^x */
    override fun exp() = Flt64(exp(value))

    /** 正弦 / Sine */
    override fun sin() = Flt64(sin(value))
    /** 余弦 / Cosine */
    override fun cos() = Flt64(cos(value))
    /** 正割；当余弦为零时返回 null / Secant; null when cosine is zero */
    override fun sec(): Flt64? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    /** 余割；当正弦为零时返回 null / Cosecant; null when sine is zero */
    override fun csc(): Flt64? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    /** 正切；当余弦为零时返回 null / Tangent; null when cosine is zero */
    override fun tan(): Flt64? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            this.sin() / temp
        }
    }

    /** 余切；当正弦为零时返回 null / Cotangent; null when sine is zero */
    override fun cot(): Flt64? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            this.cos() / temp
        }
    }

    /** 反正弦；当值超出 [-1, 1] 时返回 null / Arcsine; null when value is outside [-1, 1] */
    override fun asin(): Flt64? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt64(asin(value))
        }
    }

    /** 反余弦；当值超出 [-1, 1] 时返回 null / Arccosine; null when value is outside [-1, 1] */
    override fun acos(): Flt64? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt64(acos(value))
        }
    }

    /** 反正割；当值为零时返回 null / Arcsecant; null when value is zero */
    override fun asec(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acos()
        }
    }

    /** 反余割；当值为零时返回 null / Arccosecant; null when value is zero */
    override fun acsc(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asin()
        }
    }

    /** 反正切 / Arctangent */
    override fun atan() = Flt64(atan(value))
    /** 反余切；当值为零时返回 null / Arccotangent; null when value is zero */
    override fun acot(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atan()
        }
    }

    /** 双曲正弦 / Hyperbolic sine */
    override fun sinh() = Flt64(sinh(value))
    /** 双曲余弦 / Hyperbolic cosine */
    override fun cosh() = Flt64(cosh(value))
    /** 双曲正割 / Hyperbolic secant */
    override fun sech() = this.cosh().reciprocal()
    /** 双曲余割；当值为零时返回 null / Hyperbolic cosecant; null when value is zero */
    override fun csch(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.sinh().reciprocal()
        }
    }

    /** 双曲正切 / Hyperbolic tangent */
    override fun tanh() = Flt64(tanh(value))
    /** 双曲余切；当值为零时返回 null / Hyperbolic cotangent; null when value is zero */
    override fun coth(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.tanh().reciprocal()
        }
    }

    /** 反双曲正弦 / Inverse hyperbolic sine */
    override fun asinh() = Flt64(asinh(value))
    /** 反双曲余弦；当值小于 1 时返回 null / Inverse hyperbolic cosine; null when value is less than 1 */
    override fun acosh(): Flt64? {
        return if (this ls one) {
            null
        } else {
            Flt64(acosh(value))
        }
    }

    /** 反双曲正割；当值为零时返回 null / Inverse hyperbolic secant; null when value is zero */
    override fun asech(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acosh()
        }
    }

    /** 反双曲余割；当值为零时返回 null / Inverse hyperbolic cosecant; null when value is zero */
    override fun acsch(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asinh()
        }
    }

    /** 反双曲正切；当值的绝对值 >= 1 时返回 null / Inverse hyperbolic tangent; null when |value| >= 1 */
    override fun atanh(): Flt64? {
        return if (this leq -one || this geq one) {
            null
        } else {
            Flt64(atanh(value))
        }
    }

    /** 反双曲余切；当值为零时返回 null / Inverse hyperbolic cotangent; null when value is zero */
    override fun acoth(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atanh()
        }
    }

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toInt().toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toInt().toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toString())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUInt().toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toUInt().toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value.toString())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = copy()
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value)

    /** 转换为原始 Double 值 / Convert to raw Double value */
    override fun toDouble() = value
    /** 向下取整 / Floor */
    override fun floor() = Flt64(floor(value))
    /** 向上取整 / Ceiling */
    override fun ceil() = Flt64(ceil(value))
    /** 四舍五入 / Round half up */
    override fun round() = Flt64(round(value))
    /** 截断小数部分 / Truncate fractional part */
    override fun trunc() = Flt64(truncate(value))
    /** 银行家舍入 / Banker's rounding */
    override fun bankerRound() = bankerRound(this)

    /** 向下取整到指定精度 / Floor to specified precision */
    override fun floorTo(precision: Int) = Flt64(floor(value * 10.0.pow(precision)) / 10.0.pow(precision))
    /** 向上取整到指定精度 / Ceil to specified precision */
    override fun ceilTo(precision: Int) = Flt64(ceil(value * 10.0.pow(precision)) / 10.0.pow(precision))
    /** 四舍五入到指定精度 / Round to specified precision */
    override fun roundTo(precision: Int) = Flt64(round(value * 10.0.pow(precision)) / 10.0.pow(precision))
    /** 截断到指定精度 / Truncate to specified precision */
    override fun truncTo(precision: Int) = Flt64(truncate(value * 10.0.pow(precision)) / 10.0.pow(precision))
    /** 银行家舍入到指定精度 / Banker's round to specified precision */
    override fun bankerRoundTo(precision: Int) = bankerRound(Flt64(value * 10.0.pow(precision))) / Flt64(10.0.pow(precision))
}

/**
 * FltX 序列化器
 * FltX Serializer
 *
 * 用于 FltX（任意精度浮点数）类型的 Kotlin 序列化框架序列化器。
 * 使用字符串格式进行序列化和反序列化，以支持任意精度的浮点数。
 *
 * Serializer for the FltX (arbitrary precision floating-point number) type in the Kotlin serialization framework.
 * Uses string format for serialization and deserialization to support floating-point numbers of arbitrary precision.
 */
data object FltXSerializer : KSerializer<FltX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FltX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FltX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): FltX {
        return FltX(decoder.decodeString())
    }
}

/**
 * FltX JSON 序列化器
 * FltX JSON Serializer
 *
 * 用于 FltX 类型的 JSON 格式序列化器，专门用于 JSON 序列化场景。
 * Serializer for the FltX type in JSON format, specifically designed for JSON serialization scenarios.
 */
data object FltXJsonSerializer : KSerializer<FltX> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = SerialDescriptor("FltX", JsonElement::class.serializer().descriptor)

    @OptIn(InternalSerializationApi::class)
    override fun deserialize(decoder: Decoder): FltX {
        decoder as? JsonDecoder ?: throw SerializationException(
            "This serializer can be used only with Json format. Expected Decoder to be JsonDecoder, got ${decoder::class}"
        )

        val element = decoder.decodeSerializableValue(JsonPrimitive::class.serializer())
        return FltX(element.content, FltX.decimalDigits)
    }

    override fun serialize(encoder: Encoder, value: FltX) {
        encoder.encodeString(value.toPlainString())
    }
}

/**
 * FltX 接口
 * FltX Interface
 *
 * 定义了转换为 BigDecimal 的能力。
 * Defines the ability to convert to BigDecimal.
 */
interface FltXInterface {

    /**
     * 转换为 BigDecimal 值
     * Convert to BigDecimal value
     *
     * @return BigDecimal 值
     *         The BigDecimal value
     */
    fun toDecimal(): BigDecimal

    /**
     * 转换为 Double 值
     * Convert to Double value
     *
     * @return Double 值
     *         The Double value
     */
    fun toDouble(): Double
}

/**
 * 任意精度浮点数
 * Arbitrary Precision Floating-Point Number
 *
 * 基于 Java BigDecimal 类型封装的任意精度浮点数，可以精确控制精度和舍入模式。
 * 支持完整的算术运算、比较操作、类型转换以及各种数学函数。
 * 默认精度为 18 位有效数字。适用于需要精确计算的场景，如金融计算。
 *
 * An arbitrary precision floating-point number encapsulated based on Java BigDecimal type, allowing precise control over precision and rounding mode.
 * Supports full arithmetic operations, comparison operations, type conversions, and various mathematical functions.
 * Default precision is 18 significant digits. Suitable for scenarios requiring precise calculations, such as financial computations.
 *
 * @property value 内部的 BigDecimal 值
 *                 The internal BigDecimal value
 */
@JvmInline
@Serializable(with = FltXSerializer::class)
value class FltX(internal val value: BigDecimal) :
    FltXInterface, FloatingImpl<FltX>, Copyable<FltX>,
    LogP<FloatingNumber<*>, FloatingNumber<*>>, ExpP<FloatingNumber<*>> {
    /**
     * FltX 常量对象
     * FltX Constants Object
     *
     * 提供常用的数值常量，包括数学常数（pi、e）等。
     * Provides common numeric constants, including mathematical constants (pi, e), etc.
     */
    companion object : FloatingNumberConstants<FltX>, Flt64ValueConverter<FltX> {
        @JvmStatic
        override val zero: FltX get() = FltX(BigDecimal.ZERO)

        @JvmStatic
        override val one: FltX get() = FltX(BigDecimal.ONE)

        @JvmStatic
        override val two: FltX get() = FltX(2L)

        @JvmStatic
        override val three: FltX get() = FltX(3L)

        @JvmStatic
        override val five: FltX get() = FltX(5L)

        @JvmStatic
        override val ten: FltX get() = FltX(10L)

        @JvmStatic
        override val minimum: FltX get() = FltX(-Double.MAX_VALUE)

        @JvmStatic
        override val maximum: FltX get() = FltX(Double.MAX_VALUE)

        @JvmStatic
        override val decimalDigits: Int get() = 18

        @JvmStatic
        override val decimalPrecision: FltX get() = FltX(1e-18)

        @JvmStatic
        override val epsilon: FltX get() = decimalPrecision

        @JvmStatic
        override val half: FltX get() = FltX("0.5", 1)

        @JvmStatic
        override val pi: FltX get() = FltX(PI.toBigDecimal())

        @JvmStatic
        override val e: FltX get() = FltX(E.toBigDecimal())

        @JvmStatic
        override val lg2: FltX by lazy {
            ln(two, this)!!
        }

        override fun intoValue(value: Flt64): FltX = FltX(value.toDouble())
    }

    /**
     * 从 Double 构造 FltX 的构造函数
     * Constructor for FltX from Double
     *
     * @param value Double 值
     *              The Double value
     * @param scale 小数位数，默认为 18
     *              The number of decimal places, defaults to 18
     * @param roundingMode 舍入模式，默认为 HALF_UP
     *                     The rounding mode, defaults to HALF_UP
     */
    constructor(value: Double, scale: Int = decimalDigits, roundingMode: RoundingMode = RoundingMode.HALF_UP) : this(BigDecimal.valueOf(value).setScale(scale, roundingMode))

    /**
     * 从 Long 构造 FltX 的构造函数
     * Constructor for FltX from Long
     *
     * @param value Long 值
     *              The Long value
     * @param scale 小数位数，默认为 0
     *              The number of decimal places, defaults to 0
     * @param roundingMode 舍入模式，默认为 HALF_UP
     *                     The rounding mode, defaults to HALF_UP
     */
    constructor(value: Long, scale: Int = 0, roundingMode: RoundingMode = RoundingMode.HALF_UP) : this(BigDecimal.valueOf(value).setScale(scale, roundingMode))

    /**
     * 从字符串构造 FltX 的构造函数
     * Constructor for FltX from String
     *
     * @param value 字符串表示的数值
     *              The string representation of the value
     * @param scale 小数位数，默认为 18
     *              The number of decimal places, defaults to 18
     * @param roundingMode 舍入模式，默认为 HALF_UP
     *                     The rounding mode, defaults to HALF_UP
     */
    constructor(value: String, scale: Int = decimalDigits, roundingMode: RoundingMode = RoundingMode.HALF_UP) : this(BigDecimal(value).setScale(scale, roundingMode))

    /**
     * 移除尾部零
     * Strip trailing zeros
     *
     * 返回一个移除了尾部零的新 FltX 实例。
     * Returns a new FltX instance with trailing zeros removed.
     *
     * @return 移除尾部零后的 FltX 值
     *         The FltX value with trailing zeros removed
     */
    fun stripTrailingZeros() = if (value.scale() < 0) {
        FltX(value.setScale(0, RoundingMode.HALF_UP))
    } else if (value.scale() > 0) {
        FltX(value.stripTrailingZeros())
    } else {
        this
    }

    /**
     * 设置小数位数
     * Set scale
     *
     * 返回一个设置了指定小数位数的新 FltX 实例。
     * Returns a new FltX instance with the specified number of decimal places.
     *
     * @param scale 小数位数
     *              The number of decimal places
     * @return 设置小数位数后的 FltX 值
     *         The FltX value with the specified scale
     */
    fun withScale(scale: Int) = FltX(value.setScale(scale))

    /**
     * 设置小数位数（指定舍入模式）
     * Set scale with rounding mode
     *
     * 返回一个设置了指定小数位数和舍入模式的新 FltX 实例。
     * Returns a new FltX instance with the specified number of decimal places and rounding mode.
     *
     * @param scale 小数位数
     *              The number of decimal places
     * @param roundingMode 舍入模式
     *                     The rounding mode
     * @return 设置小数位数后的 FltX 值
     *         The FltX value with the specified scale
     */
    fun withScale(scale: Int, roundingMode: RoundingMode) = FltX(value.setScale(scale, roundingMode))

    override val constants: FloatingNumberConstants<FltX> get() = Companion
    override val isBounded: Boolean get() = false
    override val minBound: FltX? get() = null
    override val maxBound: FltX? get() = null

    /** 创建副本 / Create a copy */
    override fun copy() = FltX(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()
    /**
     * 转换为工程计数法字符串
     * Convert to engineering notation string
     *
     * @return 工程计数法字符串表示
     *         The engineering notation string representation
     */
    fun toEngineeringString(): String = value.stripTrailingZeros().toEngineeringString()
    /**
     * 转换为普通字符串（无科学计数法）
     * Convert to plain string (no scientific notation)
     *
     * @return 普通字符串表示
     *         The plain string representation
     */
    fun toPlainString(): String = value.stripTrailingZeros().toPlainString()

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: FltX) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: FltX) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = FltX(-value)
    /** 绝对值 / Absolute value */
    override fun abs() = FltX(value.abs())
    /** 倒数 / Reciprocal */
    override fun reciprocal() = FltX(one.value / value)

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: FltX) = FltX(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: FltX) = FltX(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: FltX) = FltX(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: FltX) = FltX(
        value.setScale(max(value.scale(), decimalDigits), RoundingMode.HALF_UP)
                / rhs.value.setScale(max(value.scale(), decimalDigits), RoundingMode.HALF_UP)
    )

    /**
     * 整数除法
     * Integer division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 整数除法结果
     *         The integer division result
     */
    override fun intDiv(rhs: FltX) = FltX(value - value % rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: FltX) = FltX(value % rhs.value)

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果；当基数不支持时返回 null
     *         The logarithm result; null if the base is unsupported
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FltX? = when (base) {
        is Flt32 -> log(base, decimalDigits)
        is Flt64 -> log(base, decimalDigits)
        is FltX -> log(
            base = base,
            digits = maxOf(
                value.scale(),
                base.value.scale(),
                decimalDigits
            )
        )

        else -> log(
            base = base.toFltX(),
            digits = maxOf(
                value.scale(),
                base.toFltX().value.scale(),
                decimalDigits
            )
        )
    }

    /**
     * 以指定基数和精度计算对数
     * Calculate logarithm with specified base and precision
     *
     * @param base 对数基数
     *             The logarithm base
     * @param digits 小数精度位数
     *               The number of decimal places
     * @return 对数结果
     *         The logarithm result
     */
    fun log(
        base: FloatingNumber<*>,
        digits: Int
    ): FltX? = log(
        base = base,
        digits = digits + 1,
        precision = FltXPowerStrategy.defaultPrecision(digits + 1)
    )

    /**
     * 以指定基数、精度和计算精度计算对数
     * Calculate logarithm with specified base, digits, and calculation precision
     *
     * @param base 对数基数
     *             The logarithm base
     * @param digits 小数精度位数
     *               The number of decimal places
     * @param precision 计算精度
     *                  The calculation precision
     * @return 对数结果
     *         The logarithm result
     */
    @Throws(IllegalArgumentException::class)
    override fun log(
        base: FloatingNumber<*>,
        digits: Int,
        precision: FloatingNumber<*>
    ): FltX? {
        val scaled = this.withScale(digits)
        val baseFltX = when (base) {
            is Flt32 -> base.toFltX()
            is Flt64 -> base.toFltX()
            is FltX -> base
            else -> base.toFltX()
        }.withScale(digits)
        val precisionFltX = precision.toFltX()
        return FltXPowerStrategy.ln(scaled, digits, precisionFltX)?.let { numerator ->
            FltXPowerStrategy.ln(baseFltX, digits, precisionFltX)?.let { denominator ->
                numerator / denominator
            }
        }
    }

    /**
     * 计算整数次幂
     * Calculate integer power
     *
     * @param index 整数指数
     *              The integer exponent
     * @return 幂运算结果
     *         The power result
     */
    override fun pow(index: Int) = pow(copy(), index, FltX)

    /**
     * 计算浮点数次幂
     * Calculate floating-point power
     *
     * @param index 指数
     *              The exponent
     * @return 幂运算结果
     *         The power result
     */
    override fun pow(index: FloatingNumber<*>): FltX? = when (index) {
        is Flt32 -> pow(index, decimalDigits)
        is Flt64 -> pow(index, decimalDigits)
        is FltX -> pow(
            index = index,
            digits = maxOf(
                value.scale(),
                index.value.scale(),
                decimalDigits
            )
        )

        else -> pow(
            index = index.toFltX(),
            digits = maxOf(
                value.scale(),
                index.toFltX().value.scale(),
                decimalDigits
            )
        )
    }

    /**
     * 以指定精度计算浮点数次幂
     * Calculate floating-point power with specified precision
     *
     * @param index 指数
     *              The exponent
     * @param digits 小数精度位数
     *               The number of decimal places
     * @return 幂运算结果
     *         The power result
     */
    fun pow(
        index: FloatingNumber<*>,
        digits: Int
    ): FltX? = pow(
        index = index,
        digits = digits + 1,
        precision = FltXPowerStrategy.defaultPrecision(digits + 1)
    )

    /**
     * 以指定精度和计算精度计算浮点数次幂
     * Calculate floating-point power with specified digits and calculation precision
     *
     * @param index 指数
     *              The exponent
     * @param digits 小数精度位数
     *               The number of decimal places
     * @param precision 计算精度
     *                  The calculation precision
     * @return 幂运算结果
     *         The power result
     */
    fun pow(
        index: FloatingNumber<*>,
        digits: Int,
        precision: FloatingNumber<*>
    ): FltX? {
        val indexFltX = when (index) {
            is Flt32 -> index.toFltX()
            is Flt64 -> index.toFltX()
            is FltX -> index
            else -> index.toFltX()
        }
        return FltXPowerStrategy.powOrNull(
            base = this.withScale(digits),
            index = indexFltX.withScale(digits),
            digits = digits,
            precision = precision.toFltX()
        )
    }

    /** 指数函数 e^x / Exponential function e^x */
    override fun exp() = exp(decimalDigits)

    /**
     * 以指定精度计算指数函数
     * Calculate exponential function with specified precision
     *
     * @param digits 小数精度位数
     *               The number of decimal places
     * @return 指数运算结果
     *         The exponential result
     */
    fun exp(digits: Int) = exp(
        index = this,
        constants = FltX,
        digits = digits + 1,
        precision = FltXPowerStrategy.defaultPrecision(digits + 1)
    )

    /**
     * 以指定精度和计算精度计算指数函数
     * Calculate exponential function with specified digits and calculation precision
     *
     * @param digits 小数精度位数
     *               The number of decimal places
     * @param precision 计算精度
     *                  The calculation precision
     * @return 指数运算结果
     *         The exponential result
     */
    override fun exp(
        digits: Int,
        precision: FloatingNumber<*>
    ): FloatingNumber<*> = FltXPowerStrategy.exp(
        index = this.withScale(digits),
        digits = digits,
        precision = precision.toFltX()
    )

    /** 正弦 / Sine */
    override fun sin() = toFlt64().sin().toFltX()
    /** 余弦 / Cosine */
    override fun cos() = toFlt64().cos().toFltX()
    /** 正割 / Secant */
    override fun sec() = toFlt64().sec()?.toFltX()
    /** 余割 / Cosecant */
    override fun csc() = toFlt64().csc()?.toFltX()
    /** 正切 / Tangent */
    override fun tan() = toFlt64().tan()?.toFltX()
    /** 余切 / Cotangent */
    override fun cot() = toFlt64().cot()?.toFltX()

    /** 反正弦 / Arcsine */
    override fun asin() = toFlt64().asin()?.toFltX()
    /** 反余弦 / Arccosine */
    override fun acos() = toFlt64().acos()?.toFltX()
    /** 反正割 / Arcsecant */
    override fun asec() = toFlt64().asec()?.toFltX()
    /** 反余割 / Arccosecant */
    override fun acsc() = toFlt64().acsc()?.toFltX()
    /** 反正切 / Arctangent */
    override fun atan() = toFlt64().atan().toFltX()
    /** 反余切 / Arccotangent */
    override fun acot() = toFlt64().acot()?.toFltX()

    /** 双曲正弦 / Hyperbolic sine */
    override fun sinh() = toFlt64().sinh().toFltX()
    /** 双曲余弦 / Hyperbolic cosine */
    override fun cosh() = toFlt64().cosh().toFltX()
    /** 双曲正割 / Hyperbolic secant */
    override fun sech() = toFlt64().sech().toFltX()
    /** 双曲余割 / Hyperbolic cosecant */
    override fun csch() = toFlt64().csch()?.toFltX()
    /** 双曲正切 / Hyperbolic tangent */
    override fun tanh() = toFlt64().tanh().toFltX()
    /** 双曲余切 / Hyperbolic cotangent */
    override fun coth() = toFlt64().coth()?.toFltX()

    /** 反双曲正弦 / Inverse hyperbolic sine */
    override fun asinh() = toFlt64().asinh().toFltX()
    /** 反双曲余弦 / Inverse hyperbolic cosine */
    override fun acosh() = toFlt64().acosh()?.toFltX()
    /** 反双曲正割 / Inverse hyperbolic secant */
    override fun asech() = toFlt64().asech()?.toFltX()
    /** 反双曲余割 / Inverse hyperbolic cosecant */
    override fun acsch() = toFlt64().acsch()?.toFltX()
    /** 反双曲正切 / Inverse hyperbolic tangent */
    override fun atanh() = toFlt64().atanh()?.toFltX()
    /** 反双曲余切 / Inverse hyperbolic cotangent */
    override fun acoth() = toFlt64().acoth()?.toFltX()

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toInt().toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toInt().toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(toPlainString())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toInt().toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toInt().toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toLong().toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(toPlainString())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = copy()

    /** 转换为 Double 值 / Convert to Double value */
    override fun toDouble() = value.toDouble()
    /** 转换为 BigDecimal 值 / Convert to BigDecimal value */
    override fun toDecimal() = value

    /** 向下取整 / Floor */
    override fun floor(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.FLOOR).setScale(scale))
    }

    /** 向上取整 / Ceiling */
    override fun ceil(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.CEILING).setScale(scale))
    }

    /** 四舍五入 / Round half up */
    override fun round(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.HALF_UP).setScale(scale))
    }

    /** 截断小数部分 / Truncate fractional part */
    override fun trunc(): FltX {
        val scale = value.scale()
        return if (value > BigDecimal.ZERO) {
            FltX(value.setScale(0, RoundingMode.FLOOR).setScale(scale))
        } else if (value < BigDecimal.ZERO) {
            FltX(value.setScale(0, RoundingMode.CEILING).setScale(scale))
        } else {
            this
        }
    }

    /** 银行家舍入 / Banker's rounding */
    override fun bankerRound(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.HALF_EVEN).setScale(scale))
    }

    /** 向下取整到指定精度 / Floor to specified precision */
    override fun floorTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.FLOOR).setScale(scale))
    }

    /** 向上取整到指定精度 / Ceil to specified precision */
    override fun ceilTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.CEILING).setScale(scale))
    }

    /** 四舍五入到指定精度 / Round to specified precision */
    override fun roundTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.HALF_UP).setScale(scale))
    }

    /** 截断到指定精度 / Truncate to specified precision */
    override fun truncTo(precision: Int): FltX {
        val scale = value.scale()
        return if (value > BigDecimal.ZERO) {
            FltX(value.setScale(precision, RoundingMode.FLOOR).setScale(scale))
        } else if (value < BigDecimal.ZERO) {
            FltX(value.setScale(precision, RoundingMode.CEILING).setScale(scale))
        } else {
            this
        }
    }

    /** 银行家舍入到指定精度 / Banker's round to specified precision */
    override fun bankerRoundTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.HALF_EVEN).setScale(scale))
    }
}

/**
 * 将 Boolean 值转换为 Flt64
 * Convert Boolean value to Flt64
 *
 * @return 如果为 true 则返回 Flt64.one，否则返回 Flt64.zero
 *         Returns Flt64.one if true, otherwise Flt64.zero
 */
fun Boolean.toFlt64() = if (this) {
    Flt64.one
} else {
    Flt64.zero
}

/**
 * 将字符串转换为 Flt32
 * Convert string to Flt32
 *
 * @return Flt32 值
 *         The Flt32 value
 */
fun String.toFlt32() = Flt32(toFloat())

/**
 * 将字符串转换为 Flt32，如果转换失败则返回 null
 * Convert string to Flt32, returns null if conversion fails
 *
 * @return Flt32 值或 null
 *         The Flt32 value or null
 */
fun String.toFlt32OrNull() = toFloatOrNull()?.let { Flt32(it) }

/**
 * 将字符串转换为 Flt64
 * Convert string to Flt64
 *
 * @return Flt64 值
 *         The Flt64 value
 */
fun String.toFlt64() = Flt64(toDouble())

/**
 * 将字符串转换为 Flt64，如果转换失败则返回 null
 * Convert string to Flt64, returns null if conversion fails
 *
 * @return Flt64 值或 null
 *         The Flt64 value or null
 */
fun String.toFlt64OrNull() = toDoubleOrNull()?.let { Flt64(it) }

/**
 * 将字符串转换为 FltX
 * Convert string to FltX
 *
 * @return FltX 值
 *         The FltX value
 */
fun String.toFltX() = FltX(toBigDecimal())

/**
 * 将字符串转换为 FltX，如果转换失败则返回 null
 * Convert string to FltX, returns null if conversion fails
 *
 * @return FltX 值或 null
 *         The FltX value or null
 */
fun String.toFltXOrNull() = toBigDecimalOrNull()?.let { FltX(it) }
