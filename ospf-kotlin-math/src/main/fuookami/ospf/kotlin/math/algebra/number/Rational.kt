/**
 * 有理数模块
 * Rational Number Module
 *
 * 本模块定义了有理数的类型系统，包括有符号有理数（Rtn8、Rtn16、Rtn32、Rtn64、RtnX）
 * 和无符号有理数（URtn8、URtn16、URtn32、URtn64、URtnX）。
 * 有理数以分子和分母的形式表示，在构造时自动进行约分化简。
 * 支持完整的算术运算、比较操作、类型转换以及各种数学函数。
 *
 * This module defines the rational number type system, including signed rational numbers (Rtn8, Rtn16, Rtn32, Rtn64, RtnX)
 * and unsigned rational numbers (URtn8, URtn16, URtn32, URtn64, URtnX).
 * Rational numbers are represented in numerator and denominator form, with automatic simplification during construction.
 * Supports full arithmetic operations, comparison operations, type conversions, and various mathematical functions.
 */
package fuookami.ospf.kotlin.math.algebra.number

import kotlin.ConsistentCopyVisibility
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 确保分母不为零
 * Ensure denominator is not zero
 *
 * 检查分母是否为零，如果为零则抛出算术异常。
 * Checks if the denominator is zero, throws an arithmetic exception if it is.
 *
 * @param den 分母值
 *            The denominator value
 * @throws ArithmeticException 如果分母为零
 *                            If the denominator is zero
 */
private fun <I> ensureNonZeroDenominator(den: I)
        where I : Integer<I>, I : NumberField<I> {
    if (den eq den.constants.zero) {
        throw ArithmeticException("Rational denominator cannot be zero.")
    }
}

/**
 * Checks if the denominator is zero.
 * 中文判断分母是否为零。
 *
 * @param den the denominator value to check / 待检查的分母值
 * @return true if the denominator is zero, false otherwise / 分母为零时返回 true，否则返回 false
 */
private fun <I> isZeroDenominator(den: I): Boolean
        where I : Integer<I>, I : NumberField<I> {
    return den eq den.constants.zero
}

/**
 * Builds a failure result for a zero denominator.
 * 中文构建分母为零的失败结果。
 *
 * @param den the zero denominator value / 为零的分母值
 * @return a failed result with an error message about zero denominator / 包含零分母错误信息的失败结果
 */
private fun <Self> zeroDenominatorFailure(den: Any?): Ret<Self> {
    return Failed(
        ErrorCode.IllegalArgument,
        "有理数分母不能为零：den=$den。 / Rational denominator cannot be zero: den=$den."
    )
}

/**
 * Constructs a rational number with zero-denominator validation.
 * 构造有理数，并进行分母为零的验证。
 *
 * @param den the denominator value to validate / 待验证的分母值
 * @param build the constructor lambda invoked when denominator is non-zero / 分母非零时调用的构造 lambda
 * @return the constructed rational number, or failure if denominator is zero / 构造的有理数，分母为零时返回失败
 */
private inline fun <Self, I> rationalOf(
    den: I,
    build: () -> Self
): Ret<Self> where I : Integer<I>, I : NumberField<I> {
    return if (isZeroDenominator(den)) {
        zeroDenominatorFailure(den)
    } else {
        ok(build())
    }
}

/**
 * Converts a Ret result to a nullable value.
 * 中文将 Ret 结果转换为可空值。
 *
 * @return the value if Ok, or null if Failed or Fatal / Ok 时返回值，Failed 或 Fatal 时返回 null
 */
private fun <Self> Ret<Self>.orNull(): Self? {
    return when (this) {
        is Ok -> value
        is Failed -> null
        is Fatal -> null
    }
}

/**
 * 有理数序列化器抽象类
 * Abstract Rational Serializer
 *
 * 用于有理数类型的 Kotlin 序列化框架序列化器基类。
 * 使用 JSON 对象格式，包含分子（num）和分母（den）两个字段。
 *
 * Base class for rational number type serializers in the Kotlin serialization framework.
 * Uses JSON object format with numerator (num) and denominator (den) fields.
 *
 * @param Self 有理数类型
 *             The rational number type
 * @param I 整数类型
 *          The integer type
 * @param name 序列化器名称
 *             The serializer name
 * @param ctor 有理数构造函数
 *             The rational number constructor
 */
abstract class RationalSerializer<Self, I>(
    name: String,
    val ctor: (I, I) -> Ret<Self>,
) : KSerializer<Self> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
        element<JsonElement>("num")
        element<JsonElement>("den")
    }

    abstract val valueSerializer: KSerializer<I>

    override fun serialize(encoder: Encoder, value: Self) {
        val jsonEncoder = requireJsonEncoder(encoder, "RationalSerializer")
        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                put("num", jsonEncoder.json.encodeToJsonElement(valueSerializer, value.num))
                put("den", jsonEncoder.json.encodeToJsonElement(valueSerializer, value.den))
            }
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Self {
        val jsonDecoder = requireJsonDecoder(decoder, "RationalSerializer")
        val element = requireJsonObject(
            element = jsonDecoder.decodeJsonElement(),
            serializerName = "RationalSerializer"
        )
        requireJsonFields(
            element = element,
            fields = descriptor.elementNames,
            serializerName = "RationalSerializer"
        )
        return when (val result = ctor(
            jsonDecoder.json.decodeFromJsonElement(valueSerializer, element["num"]!!),
            jsonDecoder.json.decodeFromJsonElement(valueSerializer, element["den"]!!)
        )) {
            is Ok -> result.value
            is Failed -> serializationFailure(result.error.message)
            is Fatal -> serializationFailure(result.errors.joinToString { it.message })
        }
    }
}

/**
 * 有理数抽象类
 * Abstract Rational Number
 *
 * 有理数类型的抽象基类，以分子和分母的形式表示有理数。
 * 提供了有理数的通用操作，包括算术运算、比较操作、类型转换、
 * 对数、幂运算、三角函数等数学运算的默认实现。
 *
 * Abstract base class for rational number types, representing rational numbers in numerator and denominator form.
 * Provides common operations for rational numbers, including default implementations for arithmetic operations,
 * comparison operations, type conversions, logarithm, power operations, trigonometric functions, and other mathematical operations.
 *
 * @param Self 有理数类型
 *             The rational number type
 * @param I 整数类型
 *          The integer type
 * @param ctor 有理数构造函数
 *             The rational number constructor
 * @param integerConstants 整数常量对象
 *                          The integer constants object
 */
abstract class Rational<Self, I> protected constructor(
    private val ctor: (I, I) -> Self,
    private val integerConstants: RealNumberConstants<I>
) : RationalNumber<Self, I> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    abstract val num: I
    abstract val den: I

    /** 创建副本 / Create a copy */
    override fun copy() = ctor(num, den)

    /** 转换为字符串 / Convert to string */
    override fun toString() = "($num / $den)"
    /** 以指定进制转换为字符串 / Convert to string with specified radix */
    abstract fun toString(radix: Int): String;

    /** 偏序比较 / Partial order comparison */
    override fun partialOrd(rhs: Self) = orderOf((num * rhs.den).compareTo(den * rhs.num))
    /** 相等性比较 / Equality comparison */
    override fun partialEq(rhs: Self) = num.eq(rhs.num) && den.eq(rhs.den)

    /** 取负 / Negation */
    override operator fun unaryMinus() = ctor(-num, den)
    /**
     * Returns the nullable reciprocal of this rational number.
     * 返回此有理数的可空倒数。
     *
     * If the numerator is zero, returns null since zero has no reciprocal.
     * 如果分子为零，返回 null，因为零没有倒数。
     *
     * @return the reciprocal, or null if this rational is zero / 倒数，若此有理数为零则返回 null
     */
    fun reciprocalOrNull(): Self? = if (num eq integerConstants.zero) {
        null
    } else {
        ctor(den, num)
    }
    /**
     * Returns the safe reciprocal of this rational number as a result type.
     * 以结果类型返回此有理数的安全倒数。
     *
     * If the numerator is zero, returns a failure with an error message.
     * 如果分子为零，返回包含错误信息的失败结果。
     *
     * @return the reciprocal as a successful result, or failure if this rational is zero / 成功结果形式的倒数，若此有理数为零则返回失败
     */
    fun reciprocalSafe(): Ret<Self> {
        return reciprocalOrNull()?.let { ok(it) }
            ?: Failed(
                ErrorCode.IllegalArgument,
                "有理数倒数未定义：零没有倒数，value=$this。 / Rational reciprocal is undefined for zero: $this."
            )
    }
    /** 倒数 / Reciprocal */
    override fun reciprocal(): Self = reciprocalOrNull()
        ?: throw ArithmeticException("Reciprocal is undefined for zero rational value: $this")
    /** 绝对值 / Absolute value */
    override fun abs() = ctor(num.abs(), den)

    /** 自增 / Increment */
    override operator fun inc() = ctor(num + den, den)
    /** 自减 / Decrement */
    override operator fun dec() = ctor(num - den, den)

    /** 取余 / Remainder */
    override operator fun rem(rhs: Self): Self {
        val k = this intDiv rhs;
        return this - k * rhs;
    }

    /** 整数除法 / Integer division */
    override fun intDiv(rhs: Self): Self {
        val divisor = this / rhs;
        return ctor(divisor.num / divisor.den, integerConstants.one);
    }

    /** 以指定基数计算对数 / Calculate logarithm with specified base */
    override fun log(base: FloatingNumber<*>) = toFltX().log(base)
    /** 常用对数（以 10 为底）/ Common logarithm (base 10) */
    override fun lg() = log(FltX.ten)
    /** 二进制对数（以 2 为底）/ Binary logarithm (base 2) */
    override fun lg2() = log(FltX.two)
    /** 自然对数（以 e 为底）/ Natural logarithm (base e) */
    override fun ln() = log(FltX.e)

    /** 计算浮点数次幂 / Calculate floating-point power */
    override fun pow(index: FloatingNumber<*>) = toFltX().pow(index)
    /** 计算整数次幂 / Calculate integer power */
    override fun pow(index: Int) = pow(copy(), index, constants)
    /** 平方 / Square */
    override fun sqr() = pow(2)
    /** 立方 / Cube */
    override fun cub() = pow(3)

    /** 平方根 / Square root */
    override fun sqrt() = pow(FltX.two.reciprocal())
    /** 立方根 / Cube root */
    override fun cbrt() = pow(FltX.three.reciprocal())

    /** 指数函数 e^x / Exponential function e^x */
    override fun exp() = toFltX().exp()

    /** 正弦 / Sine */
    override fun sin() = toFltX().sin()
    /** 余弦 / Cosine */
    override fun cos() = toFltX().cos()
    /** 正割 / Secant */
    override fun sec() = toFltX().sec()
    /** 余割 / Cosecant */
    override fun csc() = toFltX().csc()
    /** 正切 / Tangent */
    override fun tan() = toFltX().tan()
    /** 余切 / Cotangent */
    override fun cot() = toFltX().cot()

    /** 反正弦 / Arcsine */
    override fun asin() = toFltX().asin()
    /** 反余弦 / Arccosine */
    override fun acos() = toFltX().acos()
    /** 反正割 / Arcsecant */
    override fun asec() = toFltX().asec()
    /** 反余割 / Arccosecant */
    override fun acsc() = toFltX().acsc()
    /** 反正切 / Arctangent */
    override fun atan() = toFltX().atan()
    /** 反余切 / Arccotangent */
    override fun acot() = toFltX().acot()

    /** 双曲正弦 / Hyperbolic sine */
    override fun sinh() = toFltX().sinh()
    /** 双曲余弦 / Hyperbolic cosine */
    override fun cosh() = toFltX().cosh()
    /** 双曲正割 / Hyperbolic secant */
    override fun sech() = toFltX().sech()
    /** 双曲余割 / Hyperbolic cosecant */
    override fun csch() = toFltX().csch()
    /** 双曲正切 / Hyperbolic tangent */
    override fun tanh() = toFltX().tanh()
    /** 双曲余切 / Hyperbolic cotangent */
    override fun coth() = toFltX().coth()

    /** 反双曲正弦 / Inverse hyperbolic sine */
    override fun asinh() = toFltX().asinh()
    /** 反双曲余弦 / Inverse hyperbolic cosine */
    override fun acosh() = toFltX().acosh()
    /** 反双曲正割 / Inverse hyperbolic secant */
    override fun asech() = toFltX().asech()
    /** 反双曲余割 / Inverse hyperbolic cosecant */
    override fun acsch() = toFltX().acsch()
    /** 反双曲正切 / Inverse hyperbolic tangent */
    override fun atanh() = toFltX().atanh()
    /** 反双曲余切 / Inverse hyperbolic cotangent */
    override fun acoth() = toFltX().acoth()

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = (num / den).toInt8()
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = (num / den).toInt16()
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = (num / den).toInt32()
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = (num / den).toInt64()
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = (num / den).toIntX()

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = (num / den).toUInt8()
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = (num / den).toUInt16()
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = (num / den).toUInt32()
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = (num / den).toUInt64()
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = (num / den).toUIntX()

    /** 转换为 Rtn8 / Convert to Rtn8 */
    override fun toRtn8() = Rtn8(num.toInt8(), den.toInt8())
    /** 转换为 Rtn16 / Convert to Rtn16 */
    override fun toRtn16() = Rtn16(num.toInt16(), den.toInt16())
    /** 转换为 Rtn32 / Convert to Rtn32 */
    override fun toRtn32() = Rtn32(num.toInt32(), den.toInt32())
    /** 转换为 Rtn64 / Convert to Rtn64 */
    override fun toRtn64() = Rtn64(num.toInt64(), den.toInt64())
    /** 转换为 RtnX / Convert to RtnX */
    override fun toRtnX() = RtnX(num.toIntX(), den.toIntX())

    /** 转换为 URtn8 / Convert to URtn8 */
    override fun toURtn8() = URtn8(num.toUInt8(), den.toUInt8())
    /** 转换为 URtn16 / Convert to URtn16 */
    override fun toURtn16() = URtn16(num.toUInt16(), den.toUInt16())
    /** 转换为 URtn32 / Convert to URtn32 */
    override fun toURtn32() = URtn32(num.toUInt32(), den.toUInt32())
    /** 转换为 URtn64 / Convert to URtn64 */
    override fun toURtn64() = URtn64(num.toUInt64(), den.toUInt64())
    /** 转换为 URtnX / Convert to URtnX */
    override fun toURtnX() = URtnX(num.toUIntX(), den.toUIntX())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = num.toFlt32() / den.toFlt32()
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = num.toFlt64() / den.toFlt64()
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = num.toFltX() / den.toFltX()
}

/**
 * 有理数常量抽象类
 * Abstract Rational Number Constants
 *
 * 有理数常量对象的抽象基类，提供常用的数值常量。
 * Abstract base class for rational number constants objects, providing common numeric constants.
 *
 * @param Self 有理数类型
 *             The rational number type
 * @param I 整数类型
 *          The integer type
 * @param ctor 有理数构造函数
 *             The rational number constructor
 * @param constants 整数常量对象
 *                  The integer constants object
 */
abstract class RationalConstants<Self, I> protected constructor(
    private val ctor: (I, I) -> Self,
    private val constants: RealNumberConstants<I>
) : RationalNumberConstants<Self, I> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    override val zero: Self get() = ctor(constants.zero, constants.one)
    override val one: Self get() = ctor(constants.one, constants.one)
    override val two: Self get() = ctor(constants.two, constants.one)
    override val three: Self get() = ctor(constants.three, constants.one)
    override val five: Self get() = ctor(constants.five, constants.one)
    override val ten: Self get() = ctor(constants.ten, constants.one)
    override val minimum: Self get() = ctor(constants.minimum, constants.one)
    override val maximum: Self get() = ctor(constants.maximum, constants.one)

    override val half: Self get() = ctor(constants.one, constants.two)

}

/**
 * Rtn8 序列化器
 * Rtn8 Serializer
 *
 * 用于 Rtn8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn8 type in the Kotlin serialization framework.
 */
data object Rtn8Serializer : RationalSerializer<Rtn8, Int8>("Rtn8", Rtn8::of) {
    override val valueSerializer = Int8Serializer
}

/**
 * 基于 Int8 的有理数
 * Rational Number based on Int8
 *
 * 使用 8 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简，并确保符号规范化。
 *
 * A rational number type using 8-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction and ensures sign normalization.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn8Serializer::class)
@ConsistentCopyVisibility
data class Rtn8 internal constructor(
    override val num: Int8,
    override val den: Int8
) : Rational<Rtn8, Int8>(Rtn8::invoke, Int8), Copyable<Rtn8> {
    /**
     * Companion object providing constants and factory methods for the Rtn8 rational number type.
     * 中文Rtn8 有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<Rtn8, Int8>(Rtn8::invoke, Int8) {
        /**
         * Safely constructs a rational number from the given numerator and denominator.
         * 安全地从给定的分子和分母构造有理数。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the construction result, returns failure if denominator is zero / 构造结果，分母为零时返回失败
         */
        fun of(num: Int8, den: Int8): Ret<Rtn8> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                val negative = (num < Int8.zero) xor (den < Int8.zero)
                if (negative) {
                    Rtn8(-num.abs() / divisor, den.abs() / divisor)
                } else {
                    Rtn8(num.abs() / divisor, den.abs() / divisor)
                }
            }
        }

        /**
         * Safely constructs a rational number, returns null if denominator is zero.
         * 安全构造有理数，分母为零时返回 null。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed rational number, or null if denominator is zero / 构造的有理数，分母为零时返回 null
         */
        fun ofOrNull(num: Int8, den: Int8): Rtn8? {
            return of(num, den).orNull()
        }

        /**
         * Constructs an Rtn8 rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 Rtn8 有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed Rtn8 rational number / 构造的 Rtn8 有理数
         */
        operator fun invoke(num: Int8, den: Int8): Rtn8 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            val negative = (num < Int8.zero) xor (den < Int8.zero);
            return if (negative) {
                Rtn8(-num.abs() / divisor, den.abs() / divisor)
            } else {
                Rtn8(num.abs() / divisor, den.abs() / divisor)
            }
        }
    }

    override val constants: RealNumberConstants<Rtn8> get() = Companion

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Rtn8) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Rtn8) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Rtn8) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Rtn8) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * Rtn16 序列化器
 * Rtn16 Serializer
 *
 * 用于 Rtn16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn16 type in the Kotlin serialization framework.
 */
data object Rtn16Serializer : RationalSerializer<Rtn16, Int16>("Rtn16", Rtn16::of) {
    override val valueSerializer = Int16Serializer
}

/**
 * 基于 Int16 的有理数
 * Rational Number based on Int16
 *
 * 使用 16 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。
 *
 * A rational number type using 16-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn16Serializer::class)
@ConsistentCopyVisibility
data class Rtn16 internal constructor(
    override val num: Int16,
    override val den: Int16
) : Rational<Rtn16, Int16>(Rtn16::invoke, Int16), Copyable<Rtn16> {
    /**
     * Companion object providing constants and factory methods for the Rtn16 rational number type.
     * 中文Rtn16 有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<Rtn16, Int16>(Rtn16::invoke, Int16) {
        /**
         * Safely constructs a rational number from the given numerator and denominator.
         * 安全地从给定的分子和分母构造有理数。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the construction result, returns failure if denominator is zero / 构造结果，分母为零时返回失败
         */
        fun of(num: Int16, den: Int16): Ret<Rtn16> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                Rtn16(num / divisor, den / divisor)
            }
        }

        /**
         * Safely constructs a rational number, returns null if denominator is zero.
         * 安全构造有理数，分母为零时返回 null。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed rational number, or null if denominator is zero / 构造的有理数，分母为零时返回 null
         */
        fun ofOrNull(num: Int16, den: Int16): Rtn16? {
            return of(num, den).orNull()
        }

        /**
         * Constructs an Rtn16 rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 Rtn16 有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed Rtn16 rational number / 构造的 Rtn16 有理数
         */
        operator fun invoke(num: Int16, den: Int16): Rtn16 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return Rtn16(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn16> get() = Rtn16

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Rtn16) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Rtn16) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Rtn16) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Rtn16) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * Rtn32 序列化器
 * Rtn32 Serializer
 *
 * 用于 Rtn32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn32 type in the Kotlin serialization framework.
 */
data object Rtn32Serializer : RationalSerializer<Rtn32, Int32>("Rtn32", Rtn32::of) {
    override val valueSerializer = Int32Serializer
}

/**
 * 基于 Int32 的有理数
 * Rational Number based on Int32
 *
 * 使用 32 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。这是常用的有理数类型。
 *
 * A rational number type using 32-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction. This is a commonly used rational number type.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn32Serializer::class)
@ConsistentCopyVisibility
data class Rtn32 internal constructor(
    override val num: Int32,
    override val den: Int32
) : Rational<Rtn32, Int32>(Rtn32::invoke, Int32), Copyable<Rtn32> {
    /**
     * Companion object providing constants and factory methods for the Rtn32 rational number type.
     * 中文Rtn32 有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<Rtn32, Int32>(Rtn32::invoke, Int32) {
        /**
         * Safely constructs a rational number from the given numerator and denominator.
         * 安全地从给定的分子和分母构造有理数。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the construction result, returns failure if denominator is zero / 构造结果，分母为零时返回失败
         */
        fun of(num: Int32, den: Int32): Ret<Rtn32> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                Rtn32(num / divisor, den / divisor)
            }
        }

        /**
         * Safely constructs a rational number, returns null if denominator is zero.
         * 安全构造有理数，分母为零时返回 null。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed rational number, or null if denominator is zero / 构造的有理数，分母为零时返回 null
         */
        fun ofOrNull(num: Int32, den: Int32): Rtn32? {
            return of(num, den).orNull()
        }

        /**
         * Constructs an Rtn32 rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 Rtn32 有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed Rtn32 rational number / 构造的 Rtn32 有理数
         */
        operator fun invoke(num: Int32, den: Int32): Rtn32 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return Rtn32(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn32> get() = Rtn32

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Rtn32) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Rtn32) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Rtn32) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Rtn32) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * Rtn64 序列化器
 * Rtn64 Serializer
 *
 * 用于 Rtn64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Rtn64 type in the Kotlin serialization framework.
 */
data object Rtn64Serializer : RationalSerializer<Rtn64, Int64>("Rtn64", Rtn64::of) {
    override val valueSerializer = Int64Serializer
}

/**
 * 基于 Int64 的有理数
 * Rational Number based on Int64
 *
 * 使用 64 位有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。适用于需要更大数值范围的情况。
 *
 * A rational number type using 64-bit signed integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for cases requiring larger numerical range.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = Rtn64Serializer::class)
@ConsistentCopyVisibility
data class Rtn64 internal constructor(
    override val num: Int64,
    override val den: Int64
) : Rational<Rtn64, Int64>(Rtn64::invoke, Int64), Copyable<Rtn64> {
    /**
     * Companion object providing constants, factory methods, and Flt64 value conversion for the Rtn64 rational number type.
     * 中文Rtn64 有理数类型的伴生对象，提供常量、工厂方法和 Flt64 值转换。
     */
    companion object : RationalConstants<Rtn64, Int64>(Rtn64::invoke, Int64), Flt64ValueConverter<Rtn64> {
        /**
         * Safely constructs a rational number from the given numerator and denominator.
         * 安全地从给定的分子和分母构造有理数。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the construction result, returns failure if denominator is zero / 构造结果，分母为零时返回失败
         */
        fun of(num: Int64, den: Int64): Ret<Rtn64> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                Rtn64(num / divisor, den / divisor)
            }
        }

        /**
         * Safely constructs a rational number, returns null if denominator is zero.
         * 安全构造有理数，分母为零时返回 null。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed rational number, or null if denominator is zero / 构造的有理数，分母为零时返回 null
         */
        fun ofOrNull(num: Int64, den: Int64): Rtn64? {
            return of(num, den).orNull()
        }

        /**
         * Constructs an Rtn64 rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 Rtn64 有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed Rtn64 rational number / 构造的 Rtn64 有理数
         */
        operator fun invoke(num: Int64, den: Int64): Rtn64 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return Rtn64(num / divisor, den / divisor)
        }

        override val zero: Rtn64 get() = Rtn64(Int64.zero, Int64.one)
        override val one: Rtn64 get() = Rtn64(Int64.one, Int64.one)

        override fun intoValue(value: Flt64): Rtn64 = value.toRtn64()
    }

    override val constants: RealNumberConstants<Rtn64> get() = Rtn64

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Rtn64) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Rtn64) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Rtn64) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Rtn64) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * RtnX 序列化器
 * RtnX Serializer
 *
 * 用于 RtnX（任意精度有理数）类型的 Kotlin 序列化框架序列化器。
 * Serializer for the RtnX (arbitrary precision rational number) type in the Kotlin serialization framework.
 */
data object RtnXSerializer : RationalSerializer<RtnX, IntX>("RtnX", RtnX::of) {
    override val valueSerializer = IntXSerializer
}

/**
 * 任意精度有理数
 * Arbitrary Precision Rational Number
 *
 * 使用任意精度有符号整数作为分子和分母的有理数类型。
 * 在构造时自动进行约分化简。适用于需要精确计算的场景。
 *
 * A rational number type using arbitrary precision signed integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for scenarios requiring precise calculations.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = RtnXSerializer::class)
@ConsistentCopyVisibility
data class RtnX internal constructor(
    override val num: IntX,
    override val den: IntX
) : Rational<RtnX, IntX>(RtnX::invoke, IntX), Copyable<RtnX> {
    /**
     * Companion object providing constants, factory methods, and Flt64 value conversion for the RtnX arbitrary precision rational number type.
     * 中文RtnX 任意精度有理数类型的伴生对象，提供常量、工厂方法和 Flt64 值转换。
     */
    companion object : RationalConstants<RtnX, IntX>(RtnX::invoke, IntX), Flt64ValueConverter<RtnX> {
        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子（Kotlin Int）
         *            The numerator (Kotlin Int)
         * @param den 分母（Kotlin Int）
         *            The denominator (Kotlin Int)
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: Int, den: Int): Ret<RtnX> {
            return of(IntX(num.toLong()), IntX(den.toLong()))
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子（Kotlin Int）
         *            The numerator (Kotlin Int)
         * @param den 分母（Kotlin Int）
         *            The denominator (Kotlin Int)
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: Int, den: Int): RtnX? {
            return of(num, den).orNull()
        }

        /**
         * Constructs an RtnX rational number from Kotlin Int numerator and denominator.
         * 中文从 Kotlin Int 分子和分母构造 RtnX 有理数。
         *
         * @param num the numerator as a Kotlin Int / Kotlin Int 类型的分子
         * @param den the denominator as a Kotlin Int / Kotlin Int 类型的分母
         * @return the constructed RtnX rational number / 构造的 RtnX 有理数
         */
        operator fun invoke(num: Int, den: Int): RtnX {
            return RtnX(IntX(num.toLong()), IntX(den.toLong()))
        }

        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: IntX, den: IntX): Ret<RtnX> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                RtnX(num / divisor, den / divisor)
            }
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: IntX, den: IntX): RtnX? {
            return of(num, den).orNull()
        }

        /**
         * Constructs an RtnX rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 RtnX 有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed RtnX rational number / 构造的 RtnX 有理数
         */
        operator fun invoke(num: IntX, den: IntX): RtnX {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return RtnX(num / divisor, den / divisor)
        }

        override val zero: RtnX get() = RtnX(IntX.zero, IntX.one)
        override val one: RtnX get() = RtnX(IntX.one, IntX.one)

        override fun intoValue(value: Flt64): RtnX = value.toRtnX()
    }

    override val constants: RealNumberConstants<RtnX> get() = RtnX
    override val isBounded: Boolean get() = false
    override val minBound: RtnX? get() = null
    override val maxBound: RtnX? get() = null

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: RtnX) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: RtnX) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: RtnX) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: RtnX) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn8 序列化器
 * URtn8 Serializer
 *
 * 用于 URtn8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn8 type in the Kotlin serialization framework.
 */
object URtn8Serializer : RationalSerializer<URtn8, UInt8>("URtn8", URtn8::of) {
    override val valueSerializer = UInt8Serializer
}

/**
 * 基于 UInt8 的无符号有理数
 * Unsigned Rational Number based on UInt8
 *
 * 使用 8 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。
 *
 * An unsigned rational number type using 8-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn8Serializer::class)
@ConsistentCopyVisibility
data class URtn8 internal constructor(
    override val num: UInt8,
    override val den: UInt8
) : Rational<URtn8, UInt8>(URtn8::invoke, UInt8), Copyable<URtn8> {
    /**
     * Companion object providing constants and factory methods for the URtn8 unsigned rational number type.
     * 中文URtn8 无符号有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<URtn8, UInt8>(URtn8::invoke, UInt8) {
        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: UInt8, den: UInt8): Ret<URtn8> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                URtn8(num / divisor, den / divisor)
            }
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: UInt8, den: UInt8): URtn8? {
            return of(num, den).orNull()
        }

        /**
         * Constructs a URtn8 unsigned rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 URtn8 无符号有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed URtn8 rational number / 构造的 URtn8 有理数
         */
        operator fun invoke(num: UInt8, den: UInt8): URtn8 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return URtn8(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn8> get() = URtn8

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: URtn8) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: URtn8) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: URtn8) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: URtn8) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn16 序列化器
 * URtn16 Serializer
 *
 * 用于 URtn16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn16 type in the Kotlin serialization framework.
 */
object URtn16Serializer : RationalSerializer<URtn16, UInt16>("URtn16", URtn16::of) {
    override val valueSerializer = UInt16Serializer
}

/**
 * 基于 UInt16 的无符号有理数
 * Unsigned Rational Number based on UInt16
 *
 * 使用 16 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。
 *
 * An unsigned rational number type using 16-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn16Serializer::class)
@ConsistentCopyVisibility
data class URtn16 internal constructor(
    override val num: UInt16,
    override val den: UInt16
) : Rational<URtn16, UInt16>(URtn16::invoke, UInt16), Copyable<URtn16> {
    /**
     * Companion object providing constants and factory methods for the URtn16 unsigned rational number type.
     * 中文URtn16 无符号有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<URtn16, UInt16>(URtn16::invoke, UInt16) {
        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: UInt16, den: UInt16): Ret<URtn16> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                URtn16(num / divisor, den / divisor)
            }
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: UInt16, den: UInt16): URtn16? {
            return of(num, den).orNull()
        }

        /**
         * Constructs a URtn16 unsigned rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 URtn16 无符号有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed URtn16 rational number / 构造的 URtn16 有理数
         */
        operator fun invoke(num: UInt16, den: UInt16): URtn16 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return URtn16(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn16> get() = URtn16

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: URtn16) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: URtn16) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: URtn16) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: URtn16) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn32 序列化器
 * URtn32 Serializer
 *
 * 用于 URtn32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn32 type in the Kotlin serialization framework.
 */
object URtn32Serializer : RationalSerializer<URtn32, UInt32>("URtn32", URtn32::of) {
    override val valueSerializer = UInt32Serializer
}

/**
 * 基于 UInt32 的无符号有理数
 * Unsigned Rational Number based on UInt32
 *
 * 使用 32 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。这是常用的无符号有理数类型。
 *
 * An unsigned rational number type using 32-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction. This is a commonly used unsigned rational number type.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn32Serializer::class)
@ConsistentCopyVisibility
data class URtn32 internal constructor(
    override val num: UInt32,
    override val den: UInt32
) : Rational<URtn32, UInt32>(URtn32::invoke, UInt32), Copyable<URtn32> {
    /**
     * Companion object providing constants and factory methods for the URtn32 unsigned rational number type.
     * 中文URtn32 无符号有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<URtn32, UInt32>(URtn32::invoke, UInt32) {
        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: UInt32, den: UInt32): Ret<URtn32> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                URtn32(num / divisor, den / divisor)
            }
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: UInt32, den: UInt32): URtn32? {
            return of(num, den).orNull()
        }

        /**
         * Constructs a URtn32 unsigned rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 URtn32 无符号有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed URtn32 rational number / 构造的 URtn32 有理数
         */
        operator fun invoke(num: UInt32, den: UInt32): URtn32 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return URtn32(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn32> get() = URtn32

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: URtn32) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: URtn32) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: URtn32) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: URtn32) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtn64 序列化器
 * URtn64 Serializer
 *
 * 用于 URtn64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtn64 type in the Kotlin serialization framework.
 */
object URtn64Serializer : RationalSerializer<URtn64, UInt64>("URtn64", URtn64::of) {
    override val valueSerializer = UInt64Serializer
}

/**
 * 基于 UInt64 的无符号有理数
 * Unsigned Rational Number based on UInt64
 *
 * 使用 64 位无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。适用于需要更大数值范围的情况。
 *
 * An unsigned rational number type using 64-bit unsigned integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for cases requiring larger numerical range.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtn64Serializer::class)
@ConsistentCopyVisibility
data class URtn64 internal constructor(
    override val num: UInt64,
    override val den: UInt64
) : Rational<URtn64, UInt64>(URtn64::invoke, UInt64), Copyable<URtn64> {
    /**
     * Companion object providing constants and factory methods for the URtn64 unsigned rational number type.
     * 中文URtn64 无符号有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<URtn64, UInt64>(URtn64::invoke, UInt64) {
        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: UInt64, den: UInt64): Ret<URtn64> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                URtn64(num / divisor, den / divisor)
            }
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: UInt64, den: UInt64): URtn64? {
            return of(num, den).orNull()
        }

        /**
         * Constructs a URtn64 unsigned rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 URtn64 无符号有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed URtn64 rational number / 构造的 URtn64 有理数
         */
        operator fun invoke(num: UInt64, den: UInt64): URtn64 {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return URtn64(num / divisor, den / divisor)
        }

        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子（Kotlin Int）
         *            The numerator (Kotlin Int)
         * @param den 分母（Kotlin Int）
         *            The denominator (Kotlin Int)
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: Int, den: Int): Ret<URtn64> {
            return of(UInt64(num), UInt64(den))
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子（Kotlin Int）
         *            The numerator (Kotlin Int)
         * @param den 分母（Kotlin Int）
         *            The denominator (Kotlin Int)
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: Int, den: Int): URtn64? {
            return of(num, den).orNull()
        }

        /**
         * Constructs a URtn64 unsigned rational number from Kotlin Int numerator and denominator.
         * 中文从 Kotlin Int 分子和分母构造 URtn64 无符号有理数。
         *
         * @param num the numerator as a Kotlin Int / Kotlin Int 类型的分子
         * @param den the denominator as a Kotlin Int / Kotlin Int 类型的分母
         * @return the constructed URtn64 rational number / 构造的 URtn64 有理数
         */
        operator fun invoke(num: Int, den: Int): URtn64 {
            return this(UInt64(num), UInt64(den))
        }
    }

    override val constants: RealNumberConstants<URtn64> get() = URtn64

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: URtn64) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: URtn64) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: URtn64) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: URtn64) = invoke(num * rhs.den, rhs.num * den)
}

/**
 * URtnX 序列化器
 * URtnX Serializer
 *
 * 用于 URtnX（任意精度无符号有理数）类型的 Kotlin 序列化框架序列化器。
 * Serializer for the URtnX (arbitrary precision unsigned rational number) type in the Kotlin serialization framework.
 */
object URtnXSerializer : RationalSerializer<URtnX, UIntX>("URtnX", URtnX::of) {
    override val valueSerializer = UIntXSerializer
}

/**
 * 任意精度无符号有理数
 * Arbitrary Precision Unsigned Rational Number
 *
 * 使用任意精度无符号整数作为分子和分母的无符号有理数类型。
 * 在构造时自动进行约分化简。适用于需要精确计算的场景。
 *
 * An unsigned rational number type using arbitrary precision unsigned integers as numerator and denominator.
 * Automatically simplifies during construction. Suitable for scenarios requiring precise calculations.
 *
 * @property num 分子
 *               The numerator
 * @property den 分母
 *               The denominator
 */
@Serializable(with = URtnXSerializer::class)
@ConsistentCopyVisibility
data class URtnX internal constructor(
    override val num: UIntX,
    override val den: UIntX
) : Rational<URtnX, UIntX>(URtnX::invoke, UIntX), Copyable<URtnX> {
    /**
     * Companion object providing constants and factory methods for the URtnX arbitrary precision unsigned rational number type.
     * 中文URtnX 任意精度无符号有理数类型的伴生对象，提供常量和工厂方法。
     */
    companion object : RationalConstants<URtnX, UIntX>(URtnX::invoke, UIntX) {
        /**
         * 安全构造有理数
         * Safely construct a rational number
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造结果，分母为零时返回失败
         *         The construction result, returns failure if denominator is zero
         */
        fun of(num: UIntX, den: UIntX): Ret<URtnX> {
            return rationalOf(den) {
                val divisor = gcdMod(num.abs(), den.abs())
                URtnX(num / divisor, den / divisor)
            }
        }

        /**
         * 安全构造有理数，分母为零时返回 null
         * Safely construct a rational number, returns null if denominator is zero
         *
         * @param num 分子
         *            The numerator
         * @param den 分母
         *            The denominator
         * @return 构造的有理数，分母为零时返回 null
         *         The constructed rational number, or null if denominator is zero
         */
        fun ofOrNull(num: UIntX, den: UIntX): URtnX? {
            return of(num, den).orNull()
        }

        /**
         * Constructs a URtnX unsigned rational number from the given numerator and denominator, throwing on zero denominator.
         * 中文从给定的分子和分母构造 URtnX 无符号有理数，分母为零时抛出异常。
         *
         * @param num the numerator / 分子
         * @param den the denominator / 分母
         * @return the constructed URtnX rational number / 构造的 URtnX 有理数
         */
        operator fun invoke(num: UIntX, den: UIntX): URtnX {
            ensureNonZeroDenominator(den)
            val divisor = gcdMod(num.abs(), den.abs())
            return URtnX(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtnX> get() = URtnX
    override val isBounded: Boolean get() = false
    override val minBound: URtnX? get() = null
    override val maxBound: URtnX? get() = null

    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: URtnX) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: URtnX) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: URtnX) = invoke(num * rhs.num, den * rhs.den)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: URtnX) = invoke(num * rhs.den, rhs.num * den)
}
