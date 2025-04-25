package fuookami.ospf.kotlin.utils.math

import java.math.*
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*

interface UIntegerNumberImpl<Self : UIntegerNumber<Self>> : UIntegerNumber<Self> {
    override fun abs() = copy()
    override fun reciprocal() = constants.zero.copy()

    override fun intDiv(rhs: Self) = this / rhs

    override fun inc(): Self = this + constants.one
    override fun dec(): Self = this - constants.one

    override fun lg() = log(Flt64.ten)
    override fun lg2() = log(Flt64.two)
    override fun ln() = log(Flt64.e)

    override fun pow(index: Int) = pow(copy(), index, constants)
    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(Flt64.two.reciprocal())
    override fun cbrt() = pow(Flt64.three.reciprocal())

    override fun rangeTo(rhs: Self) = IntegerRange(copy(), rhs, constants.one, constants)
    override infix fun until(rhs: Self) = if (rhs == constants.zero) {
        this.rangeTo(rhs)
    } else {
        this.rangeTo(rhs - constants.one)
    }
}

data object UInt8Serializer : KSerializer<UInt8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt8) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt8 {
        return UInt8(decoder.decodeInt().toUByte())
    }
}

@JvmInline
@Serializable(with = UInt8Serializer::class)
value class UInt8(internal val value: UByte) : UIntegerNumberImpl<UInt8>, Copyable<UInt8> {
    constructor(value: Boolean) : this(
        if (value) {
            1U
        } else {
            0U
        }
    )

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

    override fun copy() = UInt8(value)

    override fun toString() = value.toString()
    fun toString(radix: Int): String = value.toString(radix)

    override fun partialOrd(rhs: UInt8) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt8) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = maximum - this

    override fun plus(rhs: UInt8) = UInt8((value + rhs.value).toUByte())
    override fun minus(rhs: UInt8) = UInt8((value - rhs.value).toUByte())
    override fun times(rhs: UInt8) = UInt8((value * rhs.value).toUByte())
    override fun div(rhs: UInt8) = UInt8((value / rhs.value).toUByte())
    override fun rem(rhs: UInt8) = UInt8((value % rhs.value).toUByte())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UInt8.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UInt8.pow: ${index.javaClass}")
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
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = copy()
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

data object UInt16Serializer : KSerializer<UInt16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt16) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt16 {
        return UInt16(decoder.decodeInt().toUShort())
    }
}

@JvmInline
@Serializable(with = UInt16Serializer::class)
value class UInt16(internal val value: UShort) : UIntegerNumberImpl<UInt16>, Copyable<UInt16> {
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

    override fun copy() = UInt16(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: UInt16) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt16) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = maximum - this

    override fun plus(rhs: UInt16) = UInt16((value + rhs.value).toUShort())
    override fun minus(rhs: UInt16) = UInt16((value - rhs.value).toUShort())
    override fun times(rhs: UInt16) = UInt16((value * rhs.value).toUShort())
    override fun div(rhs: UInt16) = UInt16((value / rhs.value).toUShort())
    override fun rem(rhs: UInt16) = UInt16((value % rhs.value).toUShort())

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UInt16.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UInt16.pow: ${index.javaClass}")
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
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = copy()
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

data object UInt32Serializer : KSerializer<UInt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: UInt32) {
        encoder.encodeInt(value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): UInt32 {
        return UInt32(decoder.decodeInt().toUInt())
    }
}

@JvmInline
@Serializable(with = UInt32Serializer::class)
value class UInt32(internal val value: UInt) : UIntegerNumberImpl<UInt32>, Copyable<UInt32> {
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

    override fun copy() = UInt32(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: UInt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt32) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = maximum - this

    override fun plus(rhs: UInt32) = UInt32(value + rhs.value)
    override fun minus(rhs: UInt32) = UInt32(value - rhs.value)
    override fun times(rhs: UInt32) = UInt32(value * rhs.value)
    override fun div(rhs: UInt32) = UInt32(value / rhs.value)
    override fun rem(rhs: UInt32) = UInt32(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UInt32.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UInt32.pow: ${index.javaClass}")
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
    fun toLong() = value.toLong()

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toLong())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = copy()
    override fun toUInt64() = UInt64(value.toULong())
    override fun toUIntX() = UIntX(value.toLong())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toDouble())
}

data object UInt64Serializer : KSerializer<UInt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UInt64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: UInt64) {
        encoder.encodeLong(value.value.toLong())
    }

    override fun deserialize(decoder: Decoder): UInt64 {
        return UInt64(decoder.decodeLong().toULong())
    }
}

@JvmInline
@Serializable(with = UInt64Serializer::class)
value class UInt64(internal val value: ULong) : UIntegerNumberImpl<UInt64>, Copyable<UInt64> {
    constructor(value: Int) : this(value.toULong())

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

    override fun copy() = UInt64(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: UInt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UInt64) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = maximum - this

    override fun plus(rhs: UInt64) = UInt64(value + rhs.value)
    override operator fun minus(rhs: UInt64) = UInt64(value - rhs.value)
    override fun times(rhs: UInt64) = UInt64(value * rhs.value)
    override fun div(rhs: UInt64) = UInt64(value / rhs.value)
    override fun rem(rhs: UInt64) = UInt64(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> Flt32(log(value.toFloat(), base.value))
        is Flt64 -> Flt64(log(value.toDouble(), base.value))
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UInt64.log: ${base.javaClass}")
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> Flt32(value.toFloat().pow(index.value))
        is Flt64 -> Flt64(value.toDouble().pow(index.value))
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UInt64.pow: ${index.javaClass}")
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
    fun toLong() = value.toLong()

    val indices get() = zero until this

    override fun toInt8() = Int8(value.toByte())
    override fun toInt16() = Int16(value.toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toString())

    override fun toUInt8() = UInt8(value.toUByte())
    override fun toUInt16() = UInt16(value.toUShort())
    override fun toUInt32() = UInt32(value.toUInt())
    override fun toUInt64() = copy()
    override fun toUIntX() = UIntX(value.toString())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toString())
}

data object UIntXSerializer : KSerializer<UIntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UIntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UIntX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): UIntX {
        return UIntX(decoder.decodeString())
    }
}

@JvmInline
@Serializable(with = UIntXSerializer::class)
value class UIntX(internal val value: BigInteger) : UIntegerNumberImpl<UIntX>, Copyable<UIntX> {
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

    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: String, radix: Int = 10) : this(BigInteger(value, radix))

    init {
        if (value < BigInteger.ZERO) {
            throw IllegalArgumentException("Illegal negative value to UIntX: $value")
        }
    }

    override val constants: RealNumberConstants<UIntX> get() = UIntX

    override fun copy() = UIntX(value)

    override fun toString() = value.toString()
    fun toString(radix: Int): String = value.toString(radix)

    override fun partialOrd(rhs: UIntX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: UIntX) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = maximum - this

    override fun plus(rhs: UIntX) = UIntX(value + rhs.value)
    override fun minus(rhs: UIntX) = UIntX(value - rhs.value)
    override fun times(rhs: UIntX) = UIntX(value * rhs.value)
    override fun div(rhs: UIntX) = UIntX(value / rhs.value)
    override fun rem(rhs: UIntX) = UIntX(value % rhs.value)

    @kotlin.jvm.Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFltX().log(base)
        is Flt64 -> toFltX().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to UIntX.log: ${base.javaClass}")
    }

    override fun lg() = log(FltX(10.0)) as FltX
    override fun ln() = log(FltX.e) as FltX

    @kotlin.jvm.Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFltX().pow(index)
        is Flt64 -> toFltX().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to UIntX.pow: ${index.javaClass}")
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
    override fun toIntX() = IntX(value)

    override fun toUInt8() = UInt8(value.toLong().toUByte())
    override fun toUInt16() = UInt16(value.toLong().toUShort())
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    override fun toUInt64() = UInt64(value.toLong().toULong())
    override fun toUIntX() = copy()

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = FltX(value.toBigDecimal())
}

fun String.toUInt8() = UInt8(toUByte())
fun String.toUInt8OrNull() = toUByteOrNull()?.let { UInt8(it) }
fun String.toUInt16() = UInt16(toUShort())
fun String.toUInt16OrNull() = toUShortOrNull()?.let { UInt16(it) }
fun String.toUInt32() = UInt32(toUInt())
fun String.toUInt32OrNull() = toUIntOrNull()?.let { UInt32(it) }
fun String.toUInt64() = UInt64(toULong())
fun String.toUInt64OrNull() = toULongOrNull()?.let { UInt64(it) }
fun String.toUIntX(radix: Int = 10) = UIntX(this, radix)
fun String.toUIntXOrNull(radix: Int = 10) = try {
    UIntX(this, radix)
} catch (e: Exception) {
    e.printStackTrace()
    null
}
