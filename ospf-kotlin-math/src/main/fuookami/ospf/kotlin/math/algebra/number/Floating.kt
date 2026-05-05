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

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.operator.ExpP
import fuookami.ospf.kotlin.math.operator.LogP
import fuookami.ospf.kotlin.math.operator.PowFP
import fuookami.ospf.kotlin.utils.functional.orderOf
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.*

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
    val ds = f.toString().trimEnd('0').trimEnd('.')
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
    while ((num % 5L == 2L) && (den % 5L == 0L)) {
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

    override fun sqrt(): Self = pow(constants.one / constants.two) as Self
    override fun cbrt(): Self = pow(constants.one / constants.three) as Self

    override fun lg(): Self? = log(constants.ten) as Self?
    override fun lg2(): Self? = log(constants.two) as Self?
    override fun ln(): Self? = log(constants.e) as Self?

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

    fun floor(): Self
    fun ceil(): Self
    fun round(): Self
    fun trunc(): Self
    fun bankerRound(): Self

    fun floorTo(precision: Int = this.constants.decimalDigits!!): Self
    fun ceilTo(precision: Int = this.constants.decimalDigits!!): Self
    fun roundTo(precision: Int = this.constants.decimalDigits!!): Self
    fun truncTo(precision: Int = this.constants.decimalDigits!!): Self
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

    override fun copy() = Flt32(value)

    override fun toString() = value.toString()

    override fun partialOrd(rhs: Flt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Flt32) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = Flt32(-value)
    override fun abs() = Flt32(abs(value))
    override fun reciprocal() = Flt32(1.0F / value)

    override operator fun plus(rhs: Flt32) = Flt32(value + rhs.value)
    override operator fun minus(rhs: Flt32) = Flt32(value - rhs.value)
    override operator fun times(rhs: Flt32) = Flt32(value * rhs.value)
    override operator fun div(rhs: Flt32) = Flt32(value / rhs.value)
    override fun intDiv(rhs: Flt32) = Flt32(value - value % rhs.value)
    override operator fun rem(rhs: Flt32) = Flt32(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value, base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to Flt32.log: ${base.javaClass}")
    }

    override fun pow(index: Int) = pow(copy(), index, Flt32)

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to Flt32.pow: ${index.javaClass}")
    }

    override fun exp() = Flt32(exp(value))

    override fun sin() = Flt32(sin(value))
    override fun cos() = Flt32(cos(value))
    override fun sec(): Flt32? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    override fun csc(): Flt32? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    override fun tan(): Flt32? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            this.sin() / temp
        }
    }

    override fun cot(): Flt32? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            this.cos() / temp
        }
    }

    override fun asin(): Flt32? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt32(asin(value))
        }
    }

    override fun acos(): Flt32? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt32(acos(value))
        }
    }

    override fun asec(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acos()
        }
    }

    override fun acsc(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asin()
        }
    }

    override fun atan() = Flt32(atan(value))
    override fun acot(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atan()
        }
    }

    override fun sinh() = Flt32(sinh(value))
    override fun cosh() = Flt32(cosh(value))
    override fun sech() = this.cosh().reciprocal()
    override fun csch(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.sinh().reciprocal()
        }
    }

    override fun tanh() = Flt32(tanh(value))
    override fun coth(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.tanh().reciprocal()
        }
    }

    override fun asinh() = Flt32(asinh(value))
    override fun acosh(): Flt32? {
        return if (this ls one) {
            null
        } else {
            Flt32(acosh(value))
        }
    }

    override fun asech(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acosh()
        }
    }

    override fun acsch(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asinh()
        }
    }

    override fun atanh(): Flt32? {
        return if (this leq -one || this geq one) {
            null
        } else {
            Flt32(atanh(value))
        }
    }

    override fun acoth(): Flt32? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atanh()
        }
    }

    override fun toInt8() = Int8(value.toInt().toByte())
    override fun toInt16() = Int16(value.toInt().toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toString())

    override fun toUInt8() = UInt8(value.toUInt().toUByte())
    override fun toUInt16() = UInt16(value.toUInt().toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toString())

    override fun toFlt32() = copy()
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())

    override fun toFloat() = value
    override fun floor() = Flt32(floor(value))
    override fun ceil() = Flt32(ceil(value))
    override fun round() = Flt32(round(value))
    override fun trunc() = Flt32(truncate(value))
    override fun bankerRound() = bankerRound(this)

    override fun floorTo(precision: Int) = Flt32(floor(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    override fun ceilTo(precision: Int) = Flt32(ceil(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    override fun roundTo(precision: Int) = Flt32(round(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    override fun truncTo(precision: Int) = Flt32(truncate(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
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
    companion object : FloatingNumberConstants<Flt64> {
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
    }

    override val constants: FloatingNumberConstants<Flt64> get() = Flt64

    override fun copy() = Flt64(value)

    override fun toString() = value.toString()

    override fun partialOrd(rhs: Flt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Flt64) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = Flt64(-value)
    override fun abs() = Flt64(abs(value))
    override fun reciprocal() = Flt64(1.0 / value)

    override operator fun plus(rhs: Flt64) = Flt64(value + rhs.value)
    override operator fun minus(rhs: Flt64) = Flt64(value - rhs.value)
    override operator fun times(rhs: Flt64) = Flt64(value * rhs.value)
    override operator fun div(rhs: Flt64) = Flt64(value / rhs.value)
    override fun intDiv(rhs: Flt64) = Flt64(value - value % rhs.value)
    override operator fun rem(rhs: Flt64) = Flt64(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt64(log(value, base.value.toDouble()))
        is Flt64 -> Flt64(log(value, base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to Flt64.log: ${base.javaClass}")
    }

    override fun pow(index: Int) = pow(copy(), index, Flt64)

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt64(value.pow(index.value.toDouble()))
        is Flt64 -> Flt64(value.pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to Flt64.pow: ${index.javaClass}")
    }

    override fun exp() = Flt64(exp(value))

    override fun sin() = Flt64(sin(value))
    override fun cos() = Flt64(cos(value))
    override fun sec(): Flt64? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    override fun csc(): Flt64? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            temp.reciprocal()
        }
    }

    override fun tan(): Flt64? {
        val temp = this.cos()
        return if (temp eq zero) {
            null
        } else {
            this.sin() / temp
        }
    }

    override fun cot(): Flt64? {
        val temp = this.sin()
        return if (temp eq zero) {
            null
        } else {
            this.cos() / temp
        }
    }

    override fun asin(): Flt64? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt64(asin(value))
        }
    }

    override fun acos(): Flt64? {
        return if (this ls -one || this gr one) {
            null
        } else {
            Flt64(acos(value))
        }
    }

    override fun asec(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acos()
        }
    }

    override fun acsc(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asin()
        }
    }

    override fun atan() = Flt64(atan(value))
    override fun acot(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atan()
        }
    }

    override fun sinh() = Flt64(sinh(value))
    override fun cosh() = Flt64(cosh(value))
    override fun sech() = this.cosh().reciprocal()
    override fun csch(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.sinh().reciprocal()
        }
    }

    override fun tanh() = Flt64(tanh(value))
    override fun coth(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.tanh().reciprocal()
        }
    }

    override fun asinh() = Flt64(asinh(value))
    override fun acosh(): Flt64? {
        return if (this ls one) {
            null
        } else {
            Flt64(acosh(value))
        }
    }

    override fun asech(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().acosh()
        }
    }

    override fun acsch(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().asinh()
        }
    }

    override fun atanh(): Flt64? {
        return if (this leq -one || this geq one) {
            null
        } else {
            Flt64(atanh(value))
        }
    }

    override fun acoth(): Flt64? {
        return if (this eq zero) {
            null
        } else {
            this.reciprocal().atanh()
        }
    }

    override fun toInt8() = Int8(value.toInt().toByte())
    override fun toInt16() = Int16(value.toInt().toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toString())

    override fun toUInt8() = UInt8(value.toUInt().toUByte())
    override fun toUInt16() = UInt16(value.toUInt().toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toString())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = copy()
    override fun toFltX() = FltX(value)

    override fun toDouble() = value
    override fun floor() = Flt64(floor(value))
    override fun ceil() = Flt64(ceil(value))
    override fun round() = Flt64(round(value))
    override fun trunc() = Flt64(truncate(value))
    override fun bankerRound() = bankerRound(this)

    override fun floorTo(precision: Int) = Flt64(floor(value * 10.0.pow(precision)) / 10.0.pow(precision))
    override fun ceilTo(precision: Int) = Flt64(ceil(value * 10.0.pow(precision)) / 10.0.pow(precision))
    override fun roundTo(precision: Int) = Flt64(round(value * 10.0.pow(precision)) / 10.0.pow(precision))
    override fun truncTo(precision: Int) = Flt64(truncate(value * 10.0.pow(precision)) / 10.0.pow(precision))
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
        decoder as? JsonDecoder ?: throw IllegalStateException(
            "This serializer can be used only with Json format." + "Expected Decoder to be JsonDecoder, got ${this::class}"
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
    LogP<FloatingNumber<*>, FloatingNumber<*>>, PowFP<FloatingNumber<*>, FloatingNumber<*>>, ExpP<FloatingNumber<*>> {
    /**
     * FltX 常量对象
     * FltX Constants Object
     *
     * 提供常用的数值常量，包括数学常数（pi、e）等。
     * Provides common numeric constants, including mathematical constants (pi, e), etc.
     */
    companion object : FloatingNumberConstants<FltX> {
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

    override fun copy() = FltX(value)

    override fun toString() = value.toString()
    fun toEngineeringString(): String = value.stripTrailingZeros().toEngineeringString()
    fun toPlainString(): String = value.stripTrailingZeros().toPlainString()

    override fun partialOrd(rhs: FltX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: FltX) = (value.compareTo(rhs.value) == 0)

    override operator fun unaryMinus() = FltX(-value)
    override fun abs() = FltX(value.abs())
    override fun reciprocal() = FltX(one.value / value)

    override operator fun plus(rhs: FltX) = FltX(value + rhs.value)
    override operator fun minus(rhs: FltX) = FltX(value - rhs.value)
    override operator fun times(rhs: FltX) = FltX(value * rhs.value)
    override operator fun div(rhs: FltX) = FltX(
        value.setScale(max(value.scale(), decimalDigits), RoundingMode.HALF_UP)
                / rhs.value.setScale(max(value.scale(), decimalDigits), RoundingMode.HALF_UP)
    )

    override fun intDiv(rhs: FltX) = FltX(value - value % rhs.value)
    override operator fun rem(rhs: FltX) = FltX(value % rhs.value)

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

        else -> throw IllegalArgumentException("Unknown argument type to FltX.log: ${base.javaClass}")
    }

    fun log(
        base: FloatingNumber<*>,
        digits: Int
    ): FltX? = log(
        base = base,
        digits = digits + 1,
        precision = FltXPowerStrategy.defaultPrecision(digits + 1)
    )

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
            else -> throw IllegalArgumentException("Unknown argument type to FltX.log: ${base.javaClass}")
        }.withScale(digits)
        val precisionFltX = precision.toFltX()
        return FltXPowerStrategy.ln(scaled, digits, precisionFltX)?.let { numerator ->
            FltXPowerStrategy.ln(baseFltX, digits, precisionFltX)?.let { denominator ->
                numerator / denominator
            }
        }
    }

    override fun pow(index: Int) = pow(copy(), index, FltX)

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FltX = when (index) {
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

        else -> throw IllegalArgumentException("Unknown argument type to FltX.pow: ${index.javaClass}")
    }

    fun pow(
        index: FloatingNumber<*>,
        digits: Int
    ): FltX = pow(
        index = index,
        digits = digits + 1,
        precision = FltXPowerStrategy.defaultPrecision(digits + 1)
    )

    @Throws(IllegalArgumentException::class)
    override fun pow(
        index: FloatingNumber<*>,
        digits: Int,
        precision: FloatingNumber<*>
    ): FltX {
        val indexFltX = when (index) {
            is Flt32 -> index.toFltX()
            is Flt64 -> index.toFltX()
            is FltX -> index
            else -> throw IllegalArgumentException("Unknown argument type to FltX.log: ${index.javaClass}")
        }
        return FltXPowerStrategy.pow(
            base = this.withScale(digits),
            index = indexFltX.withScale(digits),
            digits = digits,
            precision = precision.toFltX()
        )
    }

    override fun exp() = exp(decimalDigits)

    fun exp(digits: Int) = exp(
        index = this,
        digits = digits + 1,
        precision = FltXPowerStrategy.defaultPrecision(digits + 1)
    )

    override fun exp(
        digits: Int,
        precision: FloatingNumber<*>
    ): FloatingNumber<*> = FltXPowerStrategy.exp(
        index = this.withScale(digits),
        digits = digits,
        precision = precision.toFltX()
    )

    override fun sin() = toFlt64().sin().toFltX()
    override fun cos() = toFlt64().cos().toFltX()
    override fun sec() = toFlt64().sec()?.toFltX()
    override fun csc() = toFlt64().csc()?.toFltX()
    override fun tan() = toFlt64().tan()?.toFltX()
    override fun cot() = toFlt64().cot()?.toFltX()

    override fun asin() = toFlt64().asin()?.toFltX()
    override fun acos() = toFlt64().acos()?.toFltX()
    override fun asec() = toFlt64().asec()?.toFltX()
    override fun acsc() = toFlt64().acsc()?.toFltX()
    override fun atan() = toFlt64().atan().toFltX()
    override fun acot() = toFlt64().acot()?.toFltX()

    override fun sinh() = toFlt64().sinh().toFltX()
    override fun cosh() = toFlt64().cosh().toFltX()
    override fun sech() = toFlt64().sech().toFltX()
    override fun csch() = toFlt64().csch()?.toFltX()
    override fun tanh() = toFlt64().tanh().toFltX()
    override fun coth() = toFlt64().coth()?.toFltX()

    override fun asinh() = toFlt64().asinh().toFltX()
    override fun acosh() = toFlt64().acosh()?.toFltX()
    override fun asech() = toFlt64().asech()?.toFltX()
    override fun acsch() = toFlt64().acsch()?.toFltX()
    override fun atanh() = toFlt64().atanh()?.toFltX()
    override fun acoth() = toFlt64().acoth()?.toFltX()

    override fun toInt8() = Int8(value.toInt().toByte())
    override fun toInt16() = Int16(value.toInt().toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(toPlainString())

    override fun toUInt8() = UInt8(value.toInt().toUByte())
    override fun toUInt16() = UInt16(value.toInt().toUShort())
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    override fun toUInt64() = UInt64(value.toLong().toULong())
    override fun toUIntX() = UIntX(toPlainString())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = copy()

    override fun toDouble() = value.toDouble()
    override fun toDecimal() = value

    override fun floor(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.FLOOR).setScale(scale))
    }

    override fun ceil(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.CEILING).setScale(scale))
    }

    override fun round(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.HALF_UP).setScale(scale))
    }

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

    override fun bankerRound(): FltX {
        val scale = value.scale()
        return FltX(value.setScale(0, RoundingMode.HALF_EVEN).setScale(scale))
    }

    override fun floorTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.FLOOR).setScale(scale))
    }

    override fun ceilTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.CEILING).setScale(scale))
    }

    override fun roundTo(precision: Int): FltX {
        val scale = value.scale()
        return FltX(value.setScale(precision, RoundingMode.HALF_UP).setScale(scale))
    }

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







