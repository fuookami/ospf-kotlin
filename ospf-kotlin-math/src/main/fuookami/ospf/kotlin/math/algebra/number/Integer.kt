/**
 * 有符号整数模块
 * Signed Integer Module
 *
 * 本模块定义了有符号整数的类型系统，包括 Int8、Int16、Int32、Int64 和 IntX（任意精度整数）。
 * 这些类型提供了完整的算术运算、比较操作、类型转换以及数学函数支持。
 *
 * This module defines the signed integer type system, including Int8, Int16, Int32, Int64, and IntX (arbitrary precision integer).
 * These types provide full support for arithmetic operations, comparison operations, type conversions, and mathematical functions.
 */
package fuookami.ospf.kotlin.math.algebra.number

import java.math.BigInteger
import kotlin.math.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.ordinary.pow

/**
 * 使用浮点基数计算整数的对数
 * Calculate the logarithm of an integer using a floating-point base
 *
 * @param value 整数值
 *              The integer value
 * @param base 浮点数基数
 *              The floating-point base
 * @param toFltX 转换到 FltX 的函数
 *              The function to convert to FltX
 * @param source 源类型名称，用于错误信息
 *               The source type name for error messages
 * @return 对数结果，以浮点数表示；如果无法计算则返回 null
 *         The logarithm result as a floating-point number; null if cannot be calculated
 */
private fun integerLogByFloatingBase(
    value: Number,
    base: FloatingNumber<*>,
    toFltX: () -> FltX,
    source: String
): FloatingNumber<*>? = when (base) {
    is Flt32 -> Flt32(log(value.toFloat(), base.value))
    is Flt64 -> Flt64(log(value.toDouble(), base.value))
    is FltX -> toFltX().log(base)
    else -> toFltX().log(base.toFltX())
}

/**
 * 使用浮点指数计算整数的幂
 * Calculate the power of an integer using a floating-point index
 *
 * @param value 整数值
 *              The integer value
 * @param index 浮点数指数
 *              The floating-point index
 * @param toFltX 转换到 FltX 的函数
 *              The function to convert to FltX
 * @param source 源类型名称，用于错误信息
 *               The source type name for error messages
 * @return 幂运算结果，以浮点数表示
 *         The power operation result as a floating-point number
 */
private fun integerPowByFloatingIndex(
    value: Number,
    index: FloatingNumber<*>,
    toFltX: () -> FltX,
    source: String
): FloatingNumber<*>? = when (index) {
    is Flt32 -> Flt32(value.toFloat().pow(index.value))
    is Flt64 -> Flt64(value.toDouble().pow(index.value))
    is FltX -> toFltX().pow(index)
    else -> toFltX().pow(index.toFltX())
}

/**
 * 有符号整数实现接口
 * Signed Integer Implementation Interface
 *
 * 提供有符号整数类型的通用实现，包括倒数、自增自减、整数除法、
 * 对数、幂运算、平方、立方、三角函数等数学运算的默认实现。
 *
 * Provides common implementation for signed integer types, including default implementations
 * for reciprocal, increment/decrement, integer division, logarithm, power operations,
 * square, cube, trigonometric functions and other mathematical operations.
 *
 * @param Self 实现此接口的具体类型
 *             The concrete type implementing this interface
 */
interface IntegerNumberImpl<Self : IntegerNumberImpl<Self>> : IntegerNumber<Self> {
    /** 可空倒数；仅对 +/-1 有效 / Nullable reciprocal; only valid for +/-1 */
    fun reciprocalOrNull(): Self? = when (this) {
        constants.one -> constants.one.copy()
        -constants.one -> -constants.one
        else -> null
    }

    /** 安全倒数；仅对 +/-1 有效 / Safe reciprocal; only valid for +/-1 */
    fun reciprocalSafe(): Ret<Self> {
        return reciprocalOrNull()?.let { ok(it) }
            ?: Failed(
                ErrorCode.IllegalArgument,
                "整数倒数未定义：非单位值没有整数域倒数，value=$this。 / Integer reciprocal is undefined for non-unit value: $this."
            )
    }

    /** 倒数；仅对 +/-1 有效 / Reciprocal; only valid for +/-1 */
    override fun reciprocal(): Self = reciprocalOrNull()
        ?: throw ArithmeticException("Reciprocal is undefined in Integer domain for non-unit value: $this")

    /** 自增 / Increment */
    override operator fun inc() = this + constants.one
    /** 自减 / Decrement */
    override operator fun dec() = this - constants.one

    /** 整数除法 / Integer division */
    override fun intDiv(rhs: Self) = this / rhs

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
    override infix fun until(rhs: Self) = this.rangeTo(rhs - constants.one)
}

/**
 * Int8 序列化器
 * Int8 Serializer
 *
 * 用于 Int8 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Int8 type in the Kotlin serialization framework.
 */
data object Int8Serializer : KSerializer<Int8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int8) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): Int8 {
        return Int8(decoder.decodeInt().toByte())
    }
}

/**
 * 8位有符号整数
 * 8-bit Signed Integer
 *
 * 基于 Kotlin Byte 类型封装的 8 位有符号整数，值范围为 -128 到 127。
 * 支持完整的算术运算、比较操作和类型转换。
 *
 * An 8-bit signed integer encapsulated based on Kotlin Byte type, with value range from -128 to 127.
 * Supports full arithmetic operations, comparison operations, and type conversions.
 *
 * @property value 内部的 Byte 值
 *                 The internal Byte value
 */
@JvmInline
@Serializable(with = Int8Serializer::class)
value class Int8(internal val value: Byte) : IntegerNumberImpl<Int8>, Copyable<Int8> {
    /**
     * Int8 常量对象
     * Int8 Constants Object
     *
     * 提供常用的数值常量，如 zero、one、two、three、five、ten、minimum、maximum 等。
     * Provides common numeric constants such as zero, one, two, three, five, ten, minimum, maximum, etc.
     */
    companion object : RealNumberConstants<Int8> {
        override val zero: Int8 get() = Int8(0)
        override val one: Int8 get() = Int8(1)
        override val two: Int8 get() = Int8(2)
        override val three: Int8 get() = Int8(3)
        override val five: Int8 get() = Int8(5)
        override val ten: Int8 get() = Int8(10)
        override val minimum: Int8 get() = Int8(Byte.MIN_VALUE)
        override val maximum: Int8 get() = Int8(Byte.MAX_VALUE)
    }

    override val constants: RealNumberConstants<Int8> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = Int8(value)

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
    override fun partialOrd(rhs: Int8) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: Int8) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = Int8((-value).toByte())
    /** 绝对值 / Absolute value */
    override fun abs() = Int8(abs(value.toInt()).toByte())

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Int8) = Int8((value + rhs.value).toByte())
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Int8) = Int8((value - rhs.value).toByte())
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Int8) = Int8((value * rhs.value).toByte())
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Int8) = Int8((value / rhs.value).toByte())
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: Int8) = Int8((value % rhs.value).toByte())

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
        integerLogByFloatingBase(value, base, ::toFltX, "Int8")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? =
        integerPowByFloatingIndex(value, index, ::toFltX, "Int8")

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = copy()
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
 * Int16 序列化器
 * Int16 Serializer
 *
 * 用于 Int16 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Int16 type in the Kotlin serialization framework.
 */
data object Int16Serializer : KSerializer<Int16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int16) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): Int16 {
        return Int16(decoder.decodeInt().toShort())
    }
}

/**
 * 16位有符号整数
 * 16-bit Signed Integer
 *
 * 基于 Kotlin Short 类型封装的 16 位有符号整数，值范围为 -32768 到 32767。
 * 支持完整的算术运算、比较操作和类型转换。
 *
 * A 16-bit signed integer encapsulated based on Kotlin Short type, with value range from -32768 to 32767.
 * Supports full arithmetic operations, comparison operations, and type conversions.
 *
 * @property value 内部的 Short 值
 *                 The internal Short value
 */
@JvmInline
@Serializable(with = Int16Serializer::class)
value class Int16(internal val value: Short) : IntegerNumberImpl<Int16>, Copyable<Int16> {
    /**
     * Int16 常量对象
     * Int16 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<Int16> {
        override val zero: Int16 get() = Int16(0)
        override val one: Int16 get() = Int16(1)
        override val two: Int16 get() = Int16(2)
        override val three: Int16 get() = Int16(3)
        override val five: Int16 get() = Int16(5)
        override val ten: Int16 get() = Int16(10)
        override val minimum: Int16 get() = Int16(Short.MIN_VALUE)
        override val maximum: Int16 get() = Int16(Short.MAX_VALUE)
    }

    override val constants: RealNumberConstants<Int16> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = Int16(value)

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
    override fun partialOrd(rhs: Int16) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: Int16) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = Int16((-value).toShort())
    /** 绝对值 / Absolute value */
    override fun abs() = Int16((abs(value.toInt())).toShort())

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Int16) = Int16((value + rhs.value).toShort())
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Int16) = Int16((value - rhs.value).toShort())
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Int16) = Int16((value * rhs.value).toShort())
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Int16) = Int16((value / rhs.value).toShort())
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: Int16) = Int16((value % rhs.value).toShort())

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
        integerLogByFloatingBase(value, base, ::toFltX, "Int16")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? =
        integerPowByFloatingIndex(value, index, ::toFltX, "Int16")

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = copy()
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
 * Int32 序列化器
 * Int32 Serializer
 *
 * 用于 Int32 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Int32 type in the Kotlin serialization framework.
 */
data object Int32Serializer : KSerializer<Int32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int32) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): Int32 {
        return Int32(decoder.decodeInt())
    }
}

/**
 * 32位有符号整数
 * 32-bit Signed Integer
 *
 * 基于 Kotlin Int 类型封装的 32 位有符号整数，值范围为 -2147483648 到 2147483647。
 * 支持完整的算术运算、比较操作和类型转换。这是最常用的整数类型。
 *
 * A 32-bit signed integer encapsulated based on Kotlin Int type, with value range from -2147483648 to 2147483647.
 * Supports full arithmetic operations, comparison operations, and type conversions. This is the most commonly used integer type.
 *
 * @property value 内部的 Int 值
 *                 The internal Int value
 */
@JvmInline
@Serializable(with = Int32Serializer::class)
value class Int32(val value: Int) : IntegerNumberImpl<Int32>, Copyable<Int32> {
    /**
     * Int32 常量对象
     * Int32 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<Int32> {
        override val zero: Int32 get() = Int32(0)
        override val one: Int32 get() = Int32(1)
        override val two: Int32 get() = Int32(2)
        override val three: Int32 get() = Int32(3)
        override val five: Int32 get() = Int32(5)
        override val ten: Int32 get() = Int32(10)
        override val minimum: Int32 get() = Int32(Int.MIN_VALUE)
        override val maximum: Int32 get() = Int32(Int.MAX_VALUE)
    }

    override val constants: RealNumberConstants<Int32> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = Int32(value)

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
    override fun partialOrd(rhs: Int32) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: Int32) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = Int32(-value)
    /** 绝对值 / Absolute value */
    override fun abs() = Int32(abs(value))

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Int32) = Int32(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Int32) = Int32(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Int32) = Int32(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Int32) = Int32(value / rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: Int32) = Int32(value % rhs.value)

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
        integerLogByFloatingBase(value, base, ::toFltX, "Int32")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? =
        integerPowByFloatingIndex(value, index, ::toFltX, "Int32")

    /** 转换为原始 Int 值 / Convert to raw Int value */
    fun toInt() = value
    /** 转换为 Long / Convert to Long */
    fun toLong() = value.toLong()

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = copy()
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value.toLong())
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value.toLong())

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUByte())
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
 * Int64 序列化器
 * Int64 Serializer
 *
 * 用于 Int64 类型的 Kotlin 序列化框架序列化器。
 * Serializer for the Int64 type in the Kotlin serialization framework.
 */
data object Int64Serializer : KSerializer<Int64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Int64) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: Decoder): Int64 {
        return Int64(decoder.decodeLong())
    }
}

/**
 * 64位有符号整数
 * 64-bit Signed Integer
 *
 * 基于 Kotlin Long 类型封装的 64 位有符号整数，值范围为 -9223372036854775808 到 9223372036854775807。
 * 支持完整的算术运算、比较操作和类型转换。适用于需要更大数值范围的情况。
 *
 * A 64-bit signed integer encapsulated based on Kotlin Long type, with value range from -9223372036854775808 to 9223372036854775807.
 * Supports full arithmetic operations, comparison operations, and type conversions. Suitable for cases requiring larger numerical range.
 *
 * @property value 内部的 Long 值
 *                 The internal Long value
 */
@JvmInline
@Serializable(with = Int64Serializer::class)
value class Int64(internal val value: Long) : IntegerNumberImpl<Int64>, Copyable<Int64> {
    /**
     * Int64 常量对象
     * Int64 Constants Object
     *
     * 提供常用的数值常量。
     * Provides common numeric constants.
     */
    companion object : RealNumberConstants<Int64> {
        override val zero: Int64 get() = Int64(0L)
        override val one: Int64 get() = Int64(1L)
        override val two: Int64 get() = Int64(2L)
        override val three: Int64 get() = Int64(3L)
        override val five: Int64 get() = Int64(5L)
        override val ten: Int64 get() = Int64(10L)
        override val minimum: Int64 get() = Int64(Long.MIN_VALUE)
        override val maximum: Int64 get() = Int64(Long.MAX_VALUE)
    }

    override val constants: RealNumberConstants<Int64> get() = Companion

    /** 创建副本 / Create a copy */
    override fun copy() = Int64(value)

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
    override fun partialOrd(rhs: Int64) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: Int64) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = Int64(-value)
    /** 绝对值 / Absolute value */
    override fun abs() = Int64(abs(value))

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: Int64) = Int64(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: Int64) = Int64(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: Int64) = Int64(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: Int64) = Int64(value / rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: Int64) = Int64(value % rhs.value)

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
        integerLogByFloatingBase(value, base, ::toFltX, "Int64")

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
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? =
        integerPowByFloatingIndex(value, index, ::toFltX, "Int64")

    /** 转换为 Int / Convert to Int */
    fun toInt() = value.toInt()
    /** 转换为原始 Long 值 / Convert to raw Long value */
    fun toLong() = value

    /** 转换为 Int8 / Convert to Int8 */
    override fun toInt8() = Int8(value.toByte())
    /** 转换为 Int16 / Convert to Int16 */
    override fun toInt16() = Int16(value.toShort())
    /** 转换为 Int32 / Convert to Int32 */
    override fun toInt32() = Int32(value.toInt())
    /** 转换为 Int64 / Convert to Int64 */
    override fun toInt64() = Int64(value)
    /** 转换为 IntX / Convert to IntX */
    override fun toIntX() = IntX(value)

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value)

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toString())
}

/**
 * IntX 序列化器
 * IntX Serializer
 *
 * 用于 IntX（任意精度整数）类型的 Kotlin 序列化框架序列化器。
 * 使用字符串格式进行序列化和反序列化，以支持任意大小的整数。
 *
 * Serializer for the IntX (arbitrary precision integer) type in the Kotlin serialization framework.
 * Uses string format for serialization and deserialization to support integers of arbitrary size.
 */
data object IntXSerializer : KSerializer<IntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IntX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): IntX {
        return IntX(BigInteger(decoder.decodeString()))
    }
}

/**
 * 任意精度有符号整数
 * Arbitrary Precision Signed Integer
 *
 * 基于 Java BigInteger 类型封装的任意精度有符号整数，没有固定的数值范围限制。
 * 支持完整的算术运算、比较操作和类型转换。适用于需要极大数值或精确计算的场景。
 *
 * An arbitrary precision signed integer encapsulated based on Java BigInteger type, with no fixed numerical range limit.
 * Supports full arithmetic operations, comparison operations, and type conversions. Suitable for scenarios requiring extremely large numbers or precise calculations.
 *
 * @property value 内部的 BigInteger 值
 *                 The internal BigInteger value
 */
@JvmInline
@Serializable(with = IntXSerializer::class)
value class IntX(internal val value: BigInteger) : IntegerNumberImpl<IntX>, Copyable<IntX> {
    /**
     * IntX 常量对象
     * IntX Constants Object
     *
     * 提供常用的数值常量。由于 IntX 是任意精度类型，minimum 和 maximum 常量
     * 使用 Double 类型的最小/最大值作为参考界限。
     *
     * Provides common numeric constants. Since IntX is an arbitrary precision type,
     * minimum and maximum constants use Double type min/max values as reference bounds.
     */
    companion object : RealNumberConstants<IntX> {
        override val zero: IntX get() = IntX(0L)
        override val one: IntX get() = IntX(1L)
        override val two: IntX get() = IntX(2L)
        override val three: IntX get() = IntX(3L)
        override val five: IntX get() = IntX(5L)
        override val ten: IntX get() = IntX(10L)
        override val minimum: IntX get() = IntX(Double.MIN_VALUE.toString())
        override val maximum: IntX get() = IntX(Double.MAX_VALUE.toString())
    }

    /**
     * 从 Long 构造 IntX 的构造函数
     * Constructor for IntX from Long
     *
     * @param value Long 值
     *              The Long value
     */
    constructor(value: Long) : this(BigInteger.valueOf(value))

    /**
     * 从字符串构造 IntX 的构造函数
     * Constructor for IntX from String
     *
     * @param value 字符串表示的数值
     *              The string representation of the value
     * @param radix 进制基数，默认为 10
     *              The radix base, defaults to 10
     */
    constructor(value: String, radix: Int = 10) : this(BigInteger(value, radix))

    override val constants: RealNumberConstants<IntX> get() = Companion
    override val isBounded: Boolean get() = false
    override val minBound: IntX? get() = null
    override val maxBound: IntX? get() = null

    /** 创建副本 / Create a copy */
    override fun copy() = IntX(value)

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
    override fun partialOrd(rhs: IntX) = orderOf(value.compareTo(rhs.value))
    /**
     * 相等性比较
     * Equality comparison
     *
     * @param rhs 要比较的右侧值
     *            The right-hand side value to compare
     * @return 是否相等
     *         Whether they are equal
     */
    override fun partialEq(rhs: IntX) = (value.compareTo(rhs.value) == 0)

    /** 取负 / Negation */
    override operator fun unaryMinus() = IntX(-value)
    /** 绝对值 / Absolute value */
    override fun abs() = IntX(value.abs())

    /**
     * 加法
     * Addition
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相加结果
     *         The sum result
     */
    override operator fun plus(rhs: IntX) = IntX(value + rhs.value)
    /**
     * 减法
     * Subtraction
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相减结果
     *         The subtraction result
     */
    override operator fun minus(rhs: IntX) = IntX(value - rhs.value)
    /**
     * 乘法
     * Multiplication
     *
     * @param rhs 右侧操作数
     *            The right-hand side operand
     * @return 相乘结果
     *         The multiplication result
     */
    override operator fun times(rhs: IntX) = IntX(value * rhs.value)
    /**
     * 除法
     * Division
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 相除结果
     *         The division result
     */
    override operator fun div(rhs: IntX) = IntX(value / rhs.value)
    /**
     * 取余
     * Remainder
     *
     * @param rhs 右侧操作数（除数）
     *            The right-hand side operand (divisor)
     * @return 余数
     *         The remainder
     */
    override operator fun rem(rhs: IntX) = IntX(value % rhs.value)

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
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFltX().log(base)
        is Flt64 -> toFltX().log(base)
        is FltX -> toFltX().log(base)
        else -> toFltX().log(base.toFltX())
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
    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*>? = when (index) {
        is Flt32 -> toFltX().pow(index)
        is Flt64 -> toFltX().pow(index)
        is FltX -> toFltX().pow(index)
        else -> toFltX().pow(index.toFltX())
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
    override fun toIntX() = copy()

    /** 转换为 UInt8 / Convert to UInt8 */
    override fun toUInt8() = UInt8(value.toLong().toUByte())
    /** 转换为 UInt16 / Convert to UInt16 */
    override fun toUInt16() = UInt16(value.toLong().toUShort())
    /** 转换为 UInt32 / Convert to UInt32 */
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    /** 转换为 UInt64 / Convert to UInt64 */
    override fun toUInt64() = UInt64(value.toLong().toULong())
    /** 转换为 UIntX / Convert to UIntX */
    override fun toUIntX() = UIntX(value)

    /** 转换为 Flt32 / Convert to Flt32 */
    override fun toFlt32() = Flt32(value.toFloat())
    /** 转换为 Flt64 / Convert to Flt64 */
    override fun toFlt64() = Flt64(value.toDouble())
    /** 转换为 FltX / Convert to FltX */
    override fun toFltX() = FltX(value.toBigDecimal())
}

/**
 * 将 Boolean 值转换为 Int8
 * Convert Boolean value to Int8
 *
 * @return 如果为 true 则返回 Int8.one，否则返回 Int8.zero
 *         Returns Int8.one if true, otherwise Int8.zero
 */
fun Boolean.toInt8() = if (this) {
    Int8.one
} else {
    Int8.zero
}

/**
 * 将字符串转换为 Int8
 * Convert string to Int8
 *
 * @return Int8 值
 *         The Int8 value
 */
fun String.toInt8() = Int8(toByte())

/**
 * 将字符串转换为 Int8，如果转换失败则返回 null
 * Convert string to Int8, returns null if conversion fails
 *
 * @return Int8 值或 null
 *         The Int8 value or null
 */
fun String.toInt8OrNull() = toByteOrNull()?.let { Int8(it) }

/**
 * 将字符串转换为 Int16
 * Convert string to Int16
 *
 * @return Int16 值
 *         The Int16 value
 */
fun String.toInt16() = Int16(toShort())

/**
 * 将字符串转换为 Int16，如果转换失败则返回 null
 * Convert string to Int16, returns null if conversion fails
 *
 * @return Int16 值或 null
 *         The Int16 value or null
 */
fun String.toInt16OrNull() = toShortOrNull()?.let { Int16(it) }

/**
 * 将字符串转换为 Int32
 * Convert string to Int32
 *
 * @return Int32 值
 *         The Int32 value
 */
fun String.toInt32() = Int32(toInt())

/**
 * 将字符串转换为 Int32，如果转换失败则返回 null
 * Convert string to Int32, returns null if conversion fails
 *
 * @return Int32 值或 null
 *         The Int32 value or null
 */
fun String.toInt32OrNull() = toIntOrNull()?.let { Int32(it) }

/**
 * 将字符串转换为 Int64
 * Convert string to Int64
 *
 * @return Int64 值
 *         The Int64 value
 */
fun String.toInt64() = Int64(toLong())

/**
 * 将字符串转换为 Int64，如果转换失败则返回 null
 * Convert string to Int64, returns null if conversion fails
 *
 * @return Int64 值或 null
 *         The Int64 value or null
 */
fun String.toInt64OrNull() = toLongOrNull()?.let { Int64(it) }

/**
 * 将字符串转换为 IntX
 * Convert string to IntX
 *
 * @param radix 进制基数，默认为 10
 *              The radix base, defaults to 10
 * @return IntX 值
 *         The IntX value
 */
fun String.toIntX(radix: Int = 10) = IntX(this, radix)

/**
 * 将字符串转换为 IntX，如果转换失败则返回 null
 * Convert string to IntX, returns null if conversion fails
 *
 * @param radix 进制基数，默认为 10
 *              The radix base, defaults to 10
 * @return IntX 值或 null
 *         The IntX value or null
 */
fun String.toIntXOrNull(radix: Int = 10) = runCatching { IntX(this, radix) }.getOrNull()
