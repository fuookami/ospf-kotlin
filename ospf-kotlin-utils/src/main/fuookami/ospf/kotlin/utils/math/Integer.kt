package fuookami.ospf.kotlin.utils.math

import java.math.*
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*

interface IntegerNumberImpl<Self : IntegerNumber<Self>> : IntegerNumber<Self> {
    override fun reciprocal() = constants.zero.copy()

    override fun inc() = this + constants.one
    override fun dec() = this - constants.one

    override fun intDiv(rhs: Self) = this / rhs

    override fun lg() = log(Flt64.ten)
    override fun lg2() = log(Flt64.two)
    override fun ln() = log(Flt64.e)

    override fun pow(index: Int) = pow(copy(), index, constants)
    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(Flt64.two.reciprocal())
    override fun cbrt() = pow(Flt64.three.reciprocal())

    override fun rangeTo(rhs: Self) = IntegerRange(copy(), rhs, constants.one, constants)
    override infix fun until(rhs: Self) = this.rangeTo(rhs - constants.one)
}

data object Int8Serializer : KSerializer<Int8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int8) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): Int8 {
        return Int8(decoder.decodeInt().toByte())
    }
}

@JvmInline
@Serializable(with = Int8Serializer::class)
value class Int8(internal val value: Byte) : IntegerNumberImpl<Int8>, Copyable<Int8> {
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

    override fun copy() = Int8(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: Int8) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Int8) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = Int8((-value).toByte())
    override fun abs() = Int8(abs(value.toInt()).toByte())

    override fun plus(rhs: Int8) = Int8((value + rhs.value).toByte())
    override fun minus(rhs: Int8) = Int8((value - rhs.value).toByte())
    override fun times(rhs: Int8) = Int8((value * rhs.value).toByte())
    override fun div(rhs: Int8) = Int8((value / rhs.value).toByte())
    override fun rem(rhs: Int8) = Int8((value % rhs.value).toByte())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to Int8.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to Int8.log: ${index.javaClass}")
    }

    override fun exp() = toFlt64().exp()

    override fun sin() = toFlt64().sin()
    override fun cos() = toFlt64().cos()
    override fun sec() = toFlt64().sec()
    override fun csc() = toFlt64().csc()
    override fun tan() = toFlt64().tan()
    override fun cot() = toFlt64().cot()

    override fun asin() = toFlt64().asin()
    override fun acos() = toFlt64().acos()
    override fun asec() = toFlt64().asec()
    override fun acsc() = toFlt64().acsc()
    override fun atan() = toFlt64().atan()
    override fun acot() = toFlt64().acot()

    override fun sinh() = toFlt64().sinh()
    override fun cosh() = toFlt64().cosh()
    override fun sech() = toFlt64().sech()
    override fun csch() = toFlt64().csch()
    override fun tanh() = toFlt64().tanh()
    override fun coth() = toFlt64().coth()

    override fun asinh() = toFlt64().asinh()
    override fun acosh() = toFlt64().acosh()
    override fun asech() = toFlt64().asech()
    override fun acsch() = toFlt64().acsch()
    override fun atanh() = toFlt64().atanh()
    override fun acoth() = toFlt64().acoth()

    override fun toInt8() = copy()
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

data object Int16Serializer : KSerializer<Int16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int16) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): Int16 {
        return Int16(decoder.decodeInt().toShort())
    }
}

@JvmInline
@Serializable(with = Int16Serializer::class)
value class Int16(internal val value: Short) : IntegerNumberImpl<Int16>, Copyable<Int16> {
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

    override fun copy() = Int16(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: Int16) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Int16) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = Int16((-value).toShort())
    override fun abs() = Int16((abs(value.toInt())).toShort())

    override fun plus(rhs: Int16) = Int16((value + rhs.value).toShort())
    override fun minus(rhs: Int16) = Int16((value - rhs.value).toShort())
    override fun times(rhs: Int16) = Int16((value * rhs.value).toShort())
    override fun div(rhs: Int16) = Int16((value / rhs.value).toShort())
    override fun rem(rhs: Int16) = Int16((value % rhs.value).toShort())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to Int16.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to Int16.pow: ${index.javaClass}")
    }

    override fun exp() = toFlt64().exp()

    override fun sin() = toFlt64().sin()
    override fun cos() = toFlt64().cos()
    override fun sec() = toFlt64().sec()
    override fun csc() = toFlt64().csc()
    override fun tan() = toFlt64().tan()
    override fun cot() = toFlt64().cot()

    override fun asin() = toFlt64().asin()
    override fun acos() = toFlt64().acos()
    override fun asec() = toFlt64().asec()
    override fun acsc() = toFlt64().acsc()
    override fun atan() = toFlt64().atan()
    override fun acot() = toFlt64().acot()

    override fun sinh() = toFlt64().sinh()
    override fun cosh() = toFlt64().cosh()
    override fun sech() = toFlt64().sech()
    override fun csch() = toFlt64().csch()
    override fun tanh() = toFlt64().tanh()
    override fun coth() = toFlt64().coth()

    override fun asinh() = toFlt64().asinh()
    override fun acosh() = toFlt64().acosh()
    override fun asech() = toFlt64().asech()
    override fun acsch() = toFlt64().acsch()
    override fun atanh() = toFlt64().atanh()
    override fun acoth() = toFlt64().acoth()

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = copy()
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

data object Int32Serializer : KSerializer<Int32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int32) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): Int32 {
        return Int32(decoder.decodeInt())
    }
}

@JvmInline
@Serializable(with = Int32Serializer::class)
value class Int32(val value: Int) : IntegerNumberImpl<Int32>, Copyable<Int32> {
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

    override fun copy() = Int32(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: Int32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Int32) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = Int32(-value)
    override fun abs() = Int32(abs(value))

    override fun plus(rhs: Int32) = Int32(value + rhs.value)
    override fun minus(rhs: Int32) = Int32(value - rhs.value)
    override fun times(rhs: Int32) = Int32(value * rhs.value)
    override fun div(rhs: Int32) = Int32(value / rhs.value)
    override fun rem(rhs: Int32) = Int32(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to Int32.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to Int32.pow: ${index.javaClass}")
    }

    override fun exp() = toFlt64().exp()

    override fun sin() = toFlt64().sin()
    override fun cos() = toFlt64().cos()
    override fun sec() = toFlt64().sec()
    override fun csc() = toFlt64().csc()
    override fun tan() = toFlt64().tan()
    override fun cot() = toFlt64().cot()

    override fun asin() = toFlt64().asin()
    override fun acos() = toFlt64().acos()
    override fun asec() = toFlt64().asec()
    override fun acsc() = toFlt64().acsc()
    override fun atan() = toFlt64().atan()
    override fun acot() = toFlt64().acot()

    override fun sinh() = toFlt64().sinh()
    override fun cosh() = toFlt64().cosh()
    override fun sech() = toFlt64().sech()
    override fun csch() = toFlt64().csch()
    override fun tanh() = toFlt64().tanh()
    override fun coth() = toFlt64().coth()

    override fun asinh() = toFlt64().asinh()
    override fun acosh() = toFlt64().acosh()
    override fun asech() = toFlt64().asech()
    override fun acsch() = toFlt64().acsch()
    override fun atanh() = toFlt64().atanh()
    override fun acoth() = toFlt64().acoth()

    fun toInt() = value
    fun toLong() = value.toLong()

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = copy()
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

data object Int64Serializer : KSerializer<Int64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Int64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Int64) {
        encoder.encodeLong(value.value)
    }

    override fun deserialize(decoder: Decoder): Int64 {
        return Int64(decoder.decodeLong())
    }
}

@JvmInline
@Serializable(with = Int64Serializer::class)
value class Int64(internal val value: Long) : IntegerNumberImpl<Int64>, Copyable<Int64> {
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

    override fun copy() = Int64(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: Int64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Int64) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = Int64(-value)
    override fun abs() = Int64(abs(value))

    override fun plus(rhs: Int64) = Int64(value + rhs.value)
    override fun minus(rhs: Int64) = Int64(value - rhs.value)
    override fun times(rhs: Int64) = Int64(value * rhs.value)
    override fun div(rhs: Int64) = Int64(value / rhs.value)
    override fun rem(rhs: Int64) = Int64(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to Int64.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to Int64.pow: ${index.javaClass}")
    }

    override fun exp() = toFlt64().exp()

    override fun sin() = toFlt64().sin()
    override fun cos() = toFlt64().cos()
    override fun sec() = toFlt64().sec()
    override fun csc() = toFlt64().csc()
    override fun tan() = toFlt64().tan()
    override fun cot() = toFlt64().cot()

    override fun asin() = toFlt64().asin()
    override fun acos() = toFlt64().acos()
    override fun asec() = toFlt64().asec()
    override fun acsc() = toFlt64().acsc()
    override fun atan() = toFlt64().atan()
    override fun acot() = toFlt64().acot()

    override fun sinh() = toFlt64().sinh()
    override fun cosh() = toFlt64().cosh()
    override fun sech() = toFlt64().sech()
    override fun csch() = toFlt64().csch()
    override fun tanh() = toFlt64().tanh()
    override fun coth() = toFlt64().coth()

    override fun asinh() = toFlt64().asinh()
    override fun acosh() = toFlt64().acosh()
    override fun asech() = toFlt64().asech()
    override fun acsch() = toFlt64().acsch()
    override fun atanh() = toFlt64().atanh()
    override fun acoth() = toFlt64().acoth()

    fun toInt() = value.toInt()
    fun toLong() = value

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value)
    override fun toIntX() = IntX(value)

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value)

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toString())
}

data object IntXSerializer : KSerializer<IntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IntX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): IntX {
        return IntX(BigInteger(decoder.decodeString()))
    }
}

@JvmInline
@Serializable(with = IntXSerializer::class)
value class IntX(internal val value: BigInteger) : IntegerNumberImpl<IntX>, Copyable<IntX> {
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

    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: String, radix: Int = 10) : this(BigInteger(value, radix))

    override val constants: RealNumberConstants<IntX> get() = Companion

    override fun copy() = IntX(value)

    override fun toString() = value.toString()
    fun toString(radix: Int): String = value.toString(radix)

    override fun partialOrd(rhs: IntX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: IntX) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = IntX(-value)
    override fun abs() = IntX(value.abs())

    override fun plus(rhs: IntX) = IntX(value + rhs.value)
    override fun minus(rhs: IntX) = IntX(value - rhs.value)
    override fun times(rhs: IntX) = IntX(value * rhs.value)
    override fun div(rhs: IntX) = IntX(value / rhs.value)
    override fun rem(rhs: IntX) = IntX(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFltX().log(base)
        is Flt64 -> toFltX().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to IntX.log: ${base.javaClass}")
    }

    override fun lg() = log(FltX(10.0)) as FltX
    override fun ln() = log(FltX.e) as FltX

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFltX().pow(index)
        is Flt64 -> toFltX().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to IntX.pow: ${index.javaClass}")
    }

    override fun sqrt() = pow(FltX(1.0 / 2.0)) as FltX
    override fun cbrt() = pow(FltX(1.0 / 3.0)) as FltX

    override fun exp() = toFltX().exp()

    override fun sin() = toFltX().sin()
    override fun cos() = toFltX().cos()
    override fun sec() = toFltX().sec()
    override fun csc() = toFltX().csc()
    override fun tan() = toFltX().tan()
    override fun cot() = toFltX().cot()

    override fun asin() = toFltX().asin()
    override fun acos() = toFltX().acos()
    override fun asec() = toFltX().asec()
    override fun acsc() = toFltX().acsc()
    override fun atan() = toFltX().atan()
    override fun acot() = toFltX().acot()

    override fun sinh() = toFltX().sinh()
    override fun cosh() = toFltX().cosh()
    override fun sech() = toFltX().sech()
    override fun csch() = toFltX().csch()
    override fun tanh() = toFltX().tanh()
    override fun coth() = toFltX().coth()

    override fun asinh() = toFltX().asinh()
    override fun acosh() = toFltX().acosh()
    override fun asech() = toFltX().asech()
    override fun acsch() = toFltX().acsch()
    override fun atanh() = toFltX().atanh()
    override fun acoth() = toFltX().acoth()

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = copy()

    override fun toUInt8() = UInt8(value.toLong().toUByte())
    override fun toUInt16() = UInt16(value.toLong().toUShort())
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    override fun toUInt64() = UInt64(value.toLong().toULong())
    override fun toUIntX() = UIntX(value)

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toBigDecimal())
}

fun String.toInt8() = Int8(toByte())
fun String.toInt8OrNull() = toByteOrNull()?.let { Int8(it) }
fun String.toInt16() = Int16(toShort())
fun String.toInt16OrNull() = toShortOrNull()?.let { Int16(it) }
fun String.toInt32() = Int32(toInt())
fun String.toInt32OrNull() = toIntOrNull()?.let { Int32(it) }
fun String.toInt64() = Int64(toLong())
fun String.toInt64OrNull() = toLongOrNull()?.let { Int64(it) }
fun String.toIntX(radix: Int = 10) = IntX(this, radix)
fun String.toIntXOrNull(radix: Int = 10) = try {
    IntX(this, radix)
} catch (e: Exception) {
    e.printStackTrace()
    null
}
