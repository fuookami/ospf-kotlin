/**
 * 无符号整数模块
 * Unsigned Integer Module
 *
 * 本模块定义了无符号整数的类型系统，包括 UInt8、UInt16、UInt32、UInt64 和 UIntX（任意精度无符号整数）。
 * 这些类型提供了完整的算术运算、比较操作、类型转换以及数学函数支持。
 * 无符号整数只能表示非负值，适用于需要确保数值为正的场景。
 *
 * This module defines the unsigned integer type system, including UInt8, UInt16, UInt32, UInt64, and UIntX (arbitrary precision unsigned integer).
 * These types provide full support for arithmetic operations, comparison operations, type conversions, and mathematical functions.
 * Unsigned integers can only represent non-negative values, suitable for scenarios where positive values need to be ensured.
 */
package fuookami.ospf.kotlin.math.algebra.number

import java.math.BigInteger
import kotlin.math.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.orderOf
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.pow

/**
 * 使用浮点基数计算无符号整数的对数
 * Calculate the logarithm of an unsigned integer using a floating-point base
 *
 * @param floatValue Float 值表示
 *                   The Float value representation
 * @param doubleValue Double 值表示
 *                    The Double value representation
 * @param base 浮点数基数
 *              The floating-point base
 * @param toFltX 转换到 FltX 的函数
 *              The function to convert to FltX
 * @param source 源类型名称，用于错误信息
 *               The source type name for error messages
 * @return 对数结果，以浮点数表示；如果无法计算则返回 null
 *         The logarithm result as a floating-point number; null if cannot be calculated
 */
private fun uIntegerLogByFloatingBase(
    floatValue: Float,
    doubleValue: Double,
    base: FloatingNumber<*>,
    toFltX: () -> FltX,
    source: String
): FloatingNumber<*>? = when (base) {
    is Flt32 -> Flt32(log(floatValue, base.value))
    is Flt64 -> Flt64(log(doubleValue, base.value))
    is FltX -> toFltX().log(base)
    else -> throw IllegalArgumentException("Unknown argument type to $source.log: ${base.javaClass}")
}

/**
 * 使用浮点指数计算无符号整数的幂
 * Calculate the power of an unsigned integer using a floating-point index
 *
 * @param floatValue Float 值表示
 *                   The Float value representation
 * @param doubleValue Double 值表示
 *                    The Double value representation
 * @param index 浮点数指数
 *              The floating-point index
 * @param toFltX 转换到 FltX 的函数
 *              The function to convert to FltX
 * @param source 源类型名称，用于错误信息
 *               The source type name for error messages
 * @return 幂运算结果，以浮点数表示
 *         The power operation result as a floating-point number
 */
private fun uIntegerPowByFloatingIndex(
    floatValue: Float,
    doubleValue: Double,
    index: FloatingNumber<*>,
    toFltX: () -> FltX,
    source: String
): FloatingNumber<*> = when (index) {
    is Flt32 -> Flt32(floatValue.pow(index.value))
    is Flt64 -> Flt64(doubleValue.pow(index.value))
    is FltX -> toFltX().pow(index)
    else -> throw IllegalArgumentException("Unknown argument type to $source.pow: ${index.javaClass}")
}

/**
 * 无符号整数实现接口
 * Unsigned Integer Implementation Interface
 *
 * 提供无符号整数类型的通用实现，包括绝对值、倒数、自增自减、整数除法、
 * 对数、幂运算、平方、立方、三角函数等数学运算的默认实现。
 * 注意：无符号整数的倒数仅对单位值（1）有效。
 *
 * Provides common implementation for unsigned integer types, including default implementations
 * for absolute value, reciprocal, increment/decrement, integer division, logarithm, power operations,
 * square, cube, trigonometric functions and other mathematical operations.
 * Note: Reciprocal of unsigned integers is only valid for unit value (1).
 *
 * @param Self 实现此接口的具体类型
 *             The concrete type implementing this interface
 */
interface UIntegerNumberImpl<Self : UIntegerNumberImpl<Self>> : UIntegerNumber<Self> {
    /** 绝对值（返回自身）/ Absolute value (returns self) */
    override fun abs() = copy()
    /** 倒数；仅对 1 有效 / Reciprocal; only valid for 1 */
    override fun reciprocal() = when (this) {
        constants.one -> constants.one.copy()
        else -> throw ArithmeticException("Reciprocal is undefined in UInteger domain for non-unit value: $this")
    }

    /** 整数除法 / Integer division */
    override fun intDiv(rhs: Self) = this / rhs

    /** 自增 / Increment */
    override operator fun inc(): Self = this + constants.one
    /** 自减 / Decrement */
    override operator fun dec(): Self = this - constants.one

    /** 常用对数（以 10 为底）/ Common logarithm (base 10) */
    override fun lg() = log(Flt64.ten)
    /** 二进制对数（以 2 为底）/ Binary logarithm (base 2) */
    override fun lg2() = log(Flt64.two)
    /** 自然对数（以 e 为底）/ Natural logarithm (base e) */
    override fun ln() = log(Flt64.e)

    /** 计算整数次幂 / Calculate integer power */
    override fun pow(index: Int) = pow(copy(), index, constants)
    /** 平方 / Square */
    override fun sqr() = pow(2)
    /** 立方 / Cube */
    override fun cub() = pow(3)

    /** 平方根 / Square root */
    override fun sqrt() = pow(Flt64.two.reciprocal())
    /** 立方根 / Cube root */
    override fun cbrt() = pow(Flt64.three.reciprocal())

    /** 指数函数 e^x / Exponential function e^x */
    override fun exp(): FloatingNumber<*> = toFlt64().exp()

    /** 正弦 / Sine */
    override fun sin(): FloatingNumber<*> = toFlt64().sin()
    /** 余弦 / Cosine */
    override fun cos(): FloatingNumber<*> = toFlt64().cos()
    /** 正割 / Secant */
    override fun sec(): FloatingNumber<*>? = toFlt64().sec()
    /** 余割 / Cosecant */
    override fun csc(): FloatingNumber<*>? = toFlt64().csc()
    /** 正切 / Tangent */
    override fun tan(): FloatingNumber<*>? = toFlt64().tan()
    /** 余切 / Cotangent */
    override fun cot(): FloatingNumber<*>? = toFlt64().cot()

    /** 反正弦 / Arcsine */
    override fun asin(): FloatingNumber<*>? = toFlt64().asin()
    /** 反余弦 / Arccosine */
    override fun acos(): FloatingNumber<*>? = toFlt64().acos()
    /** 反正割 / Arcsecant */
    override fun asec(): FloatingNumber<*>? = toFlt64().asec()
    /** 反余割 / Arccosecant */
    override fun acsc(): FloatingNumber<*>? = toFlt64().acsc()
    /** 反正切 / Arctangent */
    override fun atan(): FloatingNumber<*> = toFlt64().atan()
    /** 反余切 / Arccotangent */
    override fun acot(): FloatingNumber<*>? = toFlt64().acot()

    /** 双曲正弦 / Hyperbolic sine */
    override fun sinh(): FloatingNumber<*> = toFlt64().sinh()
    /** 双曲余弦 / Hyperbolic cosine */
    override fun cosh(): FloatingNumber<*> = toFlt64().cosh()
    /** 双曲正割 / Hyperbolic secant */
    override fun sech(): FloatingNumber<*> = toFlt64().sech()
    /** 双曲余割 / Hyperbolic cosecant */
    override fun csch(): FloatingNumber<*>? = toFlt64().csch()
    /** 双曲正切 / Hyperbolic tangent */
    override fun tanh(): FloatingNumber<*> = toFlt64().tanh()
    /** 双曲余切 / Hyperbolic cotangent */
    override fun coth(): FloatingNumber<*>? = toFlt64().coth()

    /** 反双曲正弦 / Inverse hyperbolic sine */
    override fun asinh(): FloatingNumber<*> = toFlt64().asinh()
    /** 反双曲余弦 / Inverse hyperbolic cosine */
    override fun acosh(): FloatingNumber<*>? = toFlt64().acosh()
    /** 反双曲正割 / Inverse hyperbolic secant */
    override fun asech(): FloatingNumber<*>? = toFlt64().asech()
    /** 反双曲余割 / Inverse hyperbolic cosecant */
    override fun acsch(): FloatingNumber<*>? = toFlt64().acsch()
    /** 反双曲正切 / Inverse hyperbolic tangent */
    override fun atanh(): FloatingNumber<*>? = toFlt64().atanh()
    /** 反双曲余切 / Inverse hyperbolic cotangent */
    override fun acoth(): FloatingNumber<*>? = toFlt64().acoth()

    /** 创建整数范围 / Create integer range */
    override fun rangeTo(rhs: Self) = IntegerRange(copy(), rhs, constants.one, constants)
    /** 创建不包含终点的整数范围 / Create integer range excluding end */
    override infix fun until(rhs: Self) = if (rhs == constants.zero) {
        this.rangeTo(rhs)
    } else {
        this.rangeTo(rhs - constants.one)
    }
}

/**
 * UInt8 序列化器
 * UInt8 Serializer
 *
 * 用于 UInt8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt8 type in the Kotlin serialization framework.
 */
data object UInt8Serializer : KSerializer<UInt8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt8) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt8 {
        return UInt8(decoder.decodeInt().toUByte())
    }
}

/**
 * 8位无符号整数
 * 8-bit Unsigned Integer
 *
 * 基于 Kotlin UByte 类型封装的 8 位无符号整数，值范围为 0 到 255。
 * 支持完整的算术运算、比较操作和类型转换。
 *
 * An 8-bit unsigned integer encapsulated based on Kotlin UByte type, with value range from 0 to 255.
 * Supports full arithmetic operations, comparison operations, and type conversions.
 *
 * @property value 内部的 UByte 值
 *                 The internal UByte value
 */
@JvmInline
@Serializable(with = UInt8Serializer::class)
value class UInt8(internal val value: UByte) : UIntegerNumberImpl<UInt8>, Copyable<UInt8> {
    /**
     * 从 Boolean 构造 UInt8 的构造函数
     * Constructor for UInt8 from Boolean
     *
     * @param value Boolean 值，true 对应 1，false 对应 0
     *              Boolean value, true corresponds to 1, false corresponds to 0
     */
    constructor(value: Boolean) : this(
        if (value) {
            1U
        } else {
            0U
        }
    )

    /**
     * UInt8 常量对象
     * UInt8 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt8> {
        @JvmStatic
        override val zero: UInt8 get() = UInt8(0U)

        @JvmStatic
        override val one: UInt8 get() = UInt8(1U)

        @JvmStatic
        override val two: UInt8 get() = UInt8(2U)

        @JvmStatic
        override val three: UInt8 get() = UInt8(3U)

        @JvmStatic
        override val five: UInt8 get() = UInt8(5U)

        @JvmStatic
        override val ten: UInt8 get() = UInt8(10U)

        @JvmStatic
        override val minimum: UInt8 get() = UInt8(UByte.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt8 get() = UInt8(UByte.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt8> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = UInt8(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()
    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    fun toString(radix: Int): String = value.toString(radix)

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: UInt8) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: UInt8) = (value.compareTo(rhs.value) == 0)

    /** 取负（模运算）/ Negation (modular) */
    override operator fun unaryMinus() = maximum - this

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: UInt8) = UInt8((value + rhs.value).toUByte())
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: UInt8) = UInt8((value - rhs.value).toUByte())
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: UInt8) = UInt8((value * rhs.value).toUByte())
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: UInt8) = UInt8((value / rhs.value).toUByte())
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: UInt8) = UInt8((value % rhs.value).toUByte())

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果
     *         The logarithm result
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt8")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt8")

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toLong())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = copy()
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value.toLong())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toDouble())
}

/**
 * UInt16 序列化器
 * UInt16 Serializer
 *
 * 用于 UInt16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt16 type in the Kotlin serialization framework.
 */
data object UInt16Serializer : KSerializer<UInt16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt16) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt16 {
        return UInt16(decoder.decodeInt().toUShort())
    }
}

/**
 * 16位无符号整数
 * 16-bit Unsigned Integer
 *
 * 基于 Kotlin UShort 类型封装的 16 位无符号整数，值范围为 0 到 65535。
 * 支持完整的算术运算、比较操作和类型转换。
 *
 * A 16-bit unsigned integer encapsulated based on Kotlin UShort type, with value range from 0 to 65535.
 * Supports full arithmetic operations, comparison operations, and type conversions.
 *
 * @property value 内部的 UShort 值
 *                 The internal UShort value
 */
@JvmInline
@Serializable(with = UInt16Serializer::class)
value class UInt16(internal val value: UShort) : UIntegerNumberImpl<UInt16>, Copyable<UInt16> {
    /**
     * UInt16 常量对象
     * UInt16 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt16> {
        @JvmStatic
        override val zero: UInt16 get() = UInt16(0U)

        @JvmStatic
        override val one: UInt16 get() = UInt16(1U)

        @JvmStatic
        override val two: UInt16 get() = UInt16(2U)

        @JvmStatic
        override val three: UInt16 get() = UInt16(3U)

        @JvmStatic
        override val five: UInt16 get() = UInt16(5U)

        @JvmStatic
        override val ten: UInt16 get() = UInt16(10U)

        @JvmStatic
        override val minimum: UInt16 get() = UInt16(UShort.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt16 get() = UInt16(UShort.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt16> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = UInt16(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()
    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    fun toString(radix: Int) = value.toString(radix)

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: UInt16) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: UInt16) = (value.compareTo(rhs.value) == 0)

    /** 取负（模运算）/ Negation (modular) */
    override operator fun unaryMinus() = maximum - this

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: UInt16) = UInt16((value + rhs.value).toUShort())
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: UInt16) = UInt16((value - rhs.value).toUShort())
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: UInt16) = UInt16((value * rhs.value).toUShort())
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: UInt16) = UInt16((value / rhs.value).toUShort())
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: UInt16) = UInt16((value % rhs.value).toUShort())

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果
     *         The logarithm result
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt16")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt16")

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toLong())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = copy()
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value.toLong())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toDouble())
}

/**
 * UInt32 序列化器
 * UInt32 Serializer
 *
 * 用于 UInt32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt32 type in the Kotlin serialization framework.
 */
data object UInt32Serializer : KSerializer<UInt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt32) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt32 {
        return UInt32(decoder.decodeInt().toUInt())
    }
}

/**
 * 32位无符号整数
 * 32-bit Unsigned Integer
 *
 * 基于 Kotlin UInt 类型封装的 32 位无符号整数，值范围为 0 到 4294967295。
 * 支持完整的算术运算、比较操作和类型转换。这是常用的无符号整数类型。
 *
 * A 32-bit unsigned integer encapsulated based on Kotlin UInt type, with value range from 0 to 4294967295.
 * Supports full arithmetic operations, comparison operations, and type conversions. This is a commonly used unsigned integer type.
 *
 * @property value 内部的 UInt 值
 *                 The internal UInt value
 */
@JvmInline
@Serializable(with = UInt32Serializer::class)
value class UInt32(internal val value: UInt) : UIntegerNumberImpl<UInt32>, Copyable<UInt32> {
    /**
     * UInt32 常量对象
     * UInt32 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt32> {
        @JvmStatic
        override val zero: UInt32 get() = UInt32(0U)

        @JvmStatic
        override val one: UInt32 get() = UInt32(1U)

        @JvmStatic
        override val two: UInt32 get() = UInt32(2U)

        @JvmStatic
        override val three: UInt32 get() = UInt32(3U)

        @JvmStatic
        override val five: UInt32 get() = UInt32(5U)

        @JvmStatic
        override val ten: UInt32 get() = UInt32(10U)

        @JvmStatic
        override val minimum: UInt32 get() = UInt32(UInt.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt32 get() = UInt32(UInt.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt32> get() = UInt32

    /** 创建副本 / Create a copy */
    override fun copy() = UInt32(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()
    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    fun toString(radix: Int) = value.toString(radix)

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: UInt32) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: UInt32) = (value.compareTo(rhs.value) == 0)

    /** 取负（模运算）/ Negation (modular) */
    override operator fun unaryMinus() = maximum - this

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: UInt32) = UInt32(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: UInt32) = UInt32(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: UInt32) = UInt32(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: UInt32) = UInt32(value / rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: UInt32) = UInt32(value % rhs.value)

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果
     *         The logarithm result
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt32")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt32")

    /** 转换为原始 Int 值 / Convert to raw Int value */
    fun toInt() = value.toInt()
    /** 转换为 Long / Convert to Long */
    fun toLong() = value.toLong()

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toLong())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = copy()
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value.toLong())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toDouble())
}

/**
 * UInt64 序列化器
 * UInt64 Serializer
 *
 * 用于 UInt64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the UInt64 type in the Kotlin serialization framework.
 */
data object UInt64Serializer : KSerializer<UInt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: UInt64) {
        encoder.encodeLong(value.value.toLong())
    }

    override fun deserialize(decoder: Decoder): UInt64 {
        return UInt64(decoder.decodeLong().toULong())
    }
}

/**
 * 64位无符号整数
 * 64-bit Unsigned Integer
 *
 * 基于 Kotlin ULong 类型封装的 64 位无符号整数，值范围为 0 到 18446744073709551615。
 * 支持完整的算术运算、比较操作和类型转换。适用于需要更大数值范围的情况。
 *
 * A 64-bit unsigned integer encapsulated based on Kotlin ULong type, with value range from 0 to 18446744073709551615.
 * Supports full arithmetic operations, comparison operations, and type conversions. Suitable for cases requiring larger numerical range.
 *
 * @property value 内部的 ULong 值
 *                 The internal ULong value
 */
@JvmInline
@Serializable(with = UInt64Serializer::class)
value class UInt64(internal val value: ULong) : UIntegerNumberImpl<UInt64>, Copyable<UInt64> {
    /**
     * 从 Int 构造 UInt64 的构造函数
     * Constructor for UInt64 from Int
     *
     * @param value Int 值
     *              The Int value
     */
    constructor(value: Int) : this(value.toULong())

    /**
     * UInt64 常量对象
     * UInt64 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UInt64> {
        @JvmStatic
        override val zero: UInt64 get() = UInt64(0UL)

        @JvmStatic
        override val one: UInt64 get() = UInt64(1UL)

        @JvmStatic
        override val two: UInt64 get() = UInt64(2UL)

        @JvmStatic
        override val three: UInt64 get() = UInt64(3UL)

        @JvmStatic
        override val five: UInt64 get() = UInt64(5UL)

        @JvmStatic
        override val ten: UInt64 get() = UInt64(10UL)

        @JvmStatic
        override val minimum: UInt64 get() = UInt64(ULong.MIN_VALUE)

        @JvmStatic
        override val maximum: UInt64 get() = UInt64(ULong.MAX_VALUE)
    }

    override val constants: RealNumberConstants<UInt64> get() = UInt64

    /** 创建副本 / Create a copy */
    override fun copy() = UInt64(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()
    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    fun toString(radix: Int) = value.toString(radix)

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: UInt64) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: UInt64) = (value.compareTo(rhs.value) == 0)

    /** 取负（模运算）/ Negation (modular) */
    override operator fun unaryMinus() = maximum - this

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: UInt64) = UInt64(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: UInt64) = UInt64(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: UInt64) = UInt64(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: UInt64) = UInt64(value / rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: UInt64) = UInt64(value % rhs.value)

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果
     *         The logarithm result
     */
    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? =
        uIntegerLogByFloatingBase(value.toFloat(), value.toDouble(), base, ::toFltX, "UInt64")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> =
        uIntegerPowByFloatingIndex(value.toFloat(), value.toDouble(), index, ::toFltX, "UInt64")

    /** 转换为 Int / Convert to Int */
    fun toInt() = value.toInt()
    /** 转换为 Long / Convert to Long */
    fun toLong() = value.toLong()
    /** 转换为原始 ULong 值 / Convert to raw ULong value */
    fun toULong() = value

    /** 获取从零到自身的索引范围 / Get index range from zero to self */
    val indices get() = zero until this

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toString())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = copy()
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value.toString())

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toString())
}

/**
 * UIntX 序列化器
 * UIntX Serializer
 *
 * 用于 UIntX（任意精度无符号整数）类型的 Kotlin 序列化框架序列化器。
 * 使用字符串格式进行序列化和反序列化，以支持任意大小的无符号整数。
 *
 * Serializer for the UIntX (arbitrary precision unsigned integer) type in the Kotlin serialization framework.
 * Uses string format for serialization and deserialization to support unsigned integers of arbitrary size.
 */
data object UIntXSerializer : KSerializer<UIntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UIntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UIntX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): UIntX {
        return UIntX(decoder.decodeString())
    }
}

/**
 * 任意精度无符号整数
 * Arbitrary Precision Unsigned Integer
 *
 * 基于 Java BigInteger 类型封装的任意精度无符号整数，没有固定的数值范围限制（只能表示非负值）。
 * 支持完整的算术运算、比较操作和类型转换。适用于需要极大数值或精确计算的场景。
 *
 * An arbitrary precision unsigned integer encapsulated based on Java BigInteger type, with no fixed numerical range limit (can only represent non-negative values).
 * Supports full arithmetic operations, comparison operations, and type conversions. Suitable for scenarios requiring extremely large numbers or precise calculations.
 *
 * @property value 内部的 BigInteger 值，必须为非负数
 *                 The internal BigInteger value, must be non-negative
 */
@JvmInline
@Serializable(with = UIntXSerializer::class)
value class UIntX(internal val value: BigInteger) : UIntegerNumberImpl<UIntX>, Copyable<UIntX> {
    /**
     * UIntX 常量对象
     * UIntX Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<UIntX> {
        @JvmStatic
        override val zero: UIntX get() = UIntX(0L)

        @JvmStatic
        override val one: UIntX get() = UIntX(1L)

        @JvmStatic
        override val two: UIntX get() = UIntX(2L)

        @JvmStatic
        override val three: UIntX get() = UIntX(3L)

        @JvmStatic
        override val five: UIntX get() = UIntX(5L)

        @JvmStatic
        override val ten: UIntX get() = UIntX(10L)

        @JvmStatic
        override val minimum: UIntX get() = UIntX(0L)

        @JvmStatic
        override val maximum: UIntX get() = UIntX(Double.MAX_VALUE.toString())
    }

    /**
     * 从 Long 构造 UIntX 的构造函数
     * Constructor for UIntX from Long
     *
     * @param value Long 值，必须为非负数
     *              The Long value, must be non-negative
     */
    constructor(value: Long) : this(BigInteger.valueOf(value))

    /**
     * 从字符串构造 UIntX 的构造函数
     * Constructor for UIntX from String
     *
     * @param value 字符串表示的数值
     *              The string representation of the value
     * @param radix 进制基数，默认为 10
     *              The radix base, defaults to 10
     */
    constructor(value: String, radix: Int = 10) : this(BigInteger(value, radix))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Illegal negative value to UIntX: $value")
        }
    }

    override val constants: RealNumberConstants<UIntX> get() = UIntX
    override val isBounded: Boolean get() = false
    override val minBound: UIntX? get() = null
    override val maxBound: UIntX? get() = null

    /** 创建副本 / Create a copy */
    override fun copy() = UIntX(value)

    /** 转换为字符串 / Convert to string */
    override fun toString() = value.toString()
    /**
     * 以指定进制转换为字符串
     * Convert to string with specified radix
     *
     * @param radix 进制基数
     *              The radix base
     * @return 指定进制的字符串表示
     *         The string representation in the specified radix
     */
    fun toString(radix: Int): String = value.toString(radix)

    /**
     * 偏序比较
     * Partial order comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 比较结果
     *         The comparison result
     */
    override fun partialOrd(rhs: UIntX) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: UIntX) = (value.compareTo(rhs.value) == 0)

    /** 取负（模运算）/ Negation (modular) */
    override operator fun unaryMinus() = maximum - this

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: UIntX) = UIntX(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: UIntX) = UIntX(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: UIntX) = UIntX(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: UIntX) = UIntX(value / rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: UIntX) = UIntX(value % rhs.value)

    /**
     * 以指定基数计算对数
     * Calculate logarithm with specified base
     *
     * @param base 对数基数
     *             The logarithm base
     * @return 对数结果
     *         The logarithm result
     */
    @kotlin.jvm.Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFltX().log(base)
        is Flt64 -> toFltX().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UIntX.log: ${base.javaClass}")
    }

    /** 常用对数（以 10 为底）/ Common logarithm (base 10) */
    override fun lg() = log(FltX(10.0)) as FltX
    /** 自然对数（以 e 为底）/ Natural logarithm (base e) */
    override fun ln() = log(FltX.e) as FltX

    /**
     * 计算浮点数次幂
     * Calculate floating-point power
     *
     * @param index 指数
     *              The exponent
     * @return 幂运算结果
     *         The power result
     */
    @kotlin.jvm.Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFltX().pow(index)
        is Flt64 -> toFltX().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UIntX.pow: ${index.javaClass}")
    }

    /** 平方根 / Square root */
    override fun sqrt() = pow(FltX(1.0 / 2.0)) as FltX
    /** 立方根 / Cube root */
    override fun cbrt() = pow(FltX(1.0 / 3.0)) as FltX
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
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value)

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toLong().toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toLong().toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toLong().toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = copy()

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toBigDecimal())
}

/**
 * 将 Boolean 值转换为 UInt8
 * Convert Boolean value to UInt8
 *
 * @return 如果为 true 则返回 UInt8.one，否则返回 UInt8.zero
 *         Returns UInt8.one if true, otherwise UInt8.zero
 */
fun Boolean.toUInt8() = if (this) {
    UInt8.one
} else {
    UInt8.zero
}

/**
 * 将 Boolean 值转换为 UInt64
 * Convert Boolean value to UInt64
 *
 * @return 如果为 true 则返回 UInt64.one，否则返回 UInt64.zero
 *         Returns UInt64.one if true, otherwise UInt64.zero
 */
fun Boolean.toUInt64() = if (this) {
    UInt64.one
} else {
    UInt64.zero
}

/**
 * 将字符串转换为 UInt8
 * Convert string to UInt8
 *
 * @return UInt8 值
 *         The UInt8 value
 */
fun String.toUInt8() = UInt8(toUByte())

/**
 * 将字符串转换为 UInt8，如果转换失败则返回 null
 * Convert string to UInt8, returns null if conversion fails
 *
 * @return UInt8 值或 null
 *         The UInt8 value or null
 */
fun String.toUInt8OrNull() = toUByteOrNull()?.let { UInt8(it) }

/**
 * 将字符串转换为 UInt16
 * Convert string to UInt16
 *
 * @return UInt16 值
 *         The UInt16 value
 */
fun String.toUInt16() = UInt16(toUShort())

/**
 * 将字符串转换为 UInt16，如果转换失败则返回 null
 * Convert string to UInt16, returns null if conversion fails
 *
 * @return UInt16 值或 null
 *         The UInt16 value or null
 */
fun String.toUInt16OrNull() = toUShortOrNull()?.let { UInt16(it) }

/**
 * 将字符串转换为 UInt32
 * Convert string to UInt32
 *
 * @return UInt32 值
 *         The UInt32 value
 */
fun String.toUInt32() = UInt32(toUInt())

/**
 * 将字符串转换为 UInt32，如果转换失败则返回 null
 * Convert string to UInt32, returns null if conversion fails
 *
 * @return UInt32 值或 null
 *         The UInt32 value or null
 */
fun String.toUInt32OrNull() = toUIntOrNull()?.let { UInt32(it) }

/**
 * 将字符串转换为 UInt64
 * Convert string to UInt64
 *
 * @return UInt64 值
 *         The UInt64 value
 */
fun String.toUInt64() = UInt64(toULong())

/**
 * 将字符串转换为 UInt64，如果转换失败则返回 null
 * Convert string to UInt64, returns null if conversion fails
 *
 * @return UInt64 值或 null
 *         The UInt64 value or null
 */
fun String.toUInt64OrNull() = toULongOrNull()?.let { UInt64(it) }

/**
 * 将字符串转换为 UIntX
 * Convert string to UIntX
 *
 * @param radix 进制基数，默认为 10
 *              The radix base, defaults to 10
 * @return UIntX 值
 *         The UIntX value
 */
fun String.toUIntX(radix: Int = 10) = UIntX(this, radix)

/**
 * 将字符串转换为 UIntX，如果转换失败则返回 null
 * Convert string to UIntX, returns null if conversion fails
 *
 * @param radix 进制基数，默认为 10
 *              The radix base, defaults to 10
 * @return UIntX 值或 null
 *         The UIntX value or null
 */
fun String.toUIntXOrNull(radix: Int = 10) = runCatching { UIntX(this, radix) }.getOrNull()
