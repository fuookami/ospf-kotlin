package fuookami.ospf.kotlin.utils.math

import java.math.*
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*

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
    for (n in 1 until ds.length - index) {
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

@Suppress("UNCHECKED_CAST")
interface FloatingImpl<Self : FloatingNumber<Self>> : FloatingNumber<Self> {
    override infix fun eq(rhs: Self) = (this - rhs).abs() <= this.constants.decimalPrecision
    override infix fun neq(rhs: Self) = !this.eq(rhs)

    override infix fun ls(rhs: Self) = (this - rhs) < -this.constants.decimalPrecision
    override infix fun leq(rhs: Self) = (this - rhs) <= this.constants.decimalPrecision
    override infix fun gr(rhs: Self) = (this - rhs) > this.constants.decimalPrecision
    override infix fun geq(rhs: Self) = (this - rhs) >= -this.constants.decimalPrecision

    override fun inc(): Self = this + constants.one
    override fun dec(): Self = this - constants.one

    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt(): Self = pow(constants.one / constants.two) as Self
    override fun cbrt(): Self = pow(constants.one / constants.three) as Self

    override fun lg(): Self? = log(constants.ten) as Self?
    override fun ln(): Self? = log(constants.e) as Self?

    override fun toRtn8() = floatingToRational(value(), { Int8(it.toByte()) }, { Int8(it.toByte()) }, Rtn8::invoke)
    override fun toRtn16() =
        floatingToRational(value(), { Int16(it.toShort()) }, { Int16(it.toShort()) }, Rtn16::invoke)

    override fun toRtn32() = floatingToRational(value(), { Int32(it.toInt()) }, { Int32(it.toInt()) }, Rtn32::invoke)
    override fun toRtn64() = floatingToRational(value(), { Int64(it.toLong()) }, { Int64(it) }, Rtn64::invoke)
    override fun toRtnX() = floatingToRational(value(), { IntX(it) }, { IntX(it) }, RtnX::invoke)

    override fun toURtn8() =
        floatingToRational(value(), { UInt8(it.toUByte()) }, { UInt8(it.toUByte()) }, URtn8::invoke)

    override fun toURtn16() =
        floatingToRational(value(), { UInt16(it.toUShort()) }, { UInt16(it.toUShort()) }, URtn16::invoke)

    override fun toURtn32() =
        floatingToRational(value(), { UInt32(it.toUInt()) }, { UInt32(it.toUInt()) }, URtn32::invoke)

    override fun toURtn64() =
        floatingToRational(value(), { UInt64(it.toULong()) }, { UInt64(it.toULong()) }, URtn64::invoke)

    override fun toURtnX() = floatingToRational(value(), { UIntX(it) }, { UIntX(it) }, URtnX::invoke)
}

object Flt32Serializer : RealNumberKSerializer<Flt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flt32", PrimitiveKind.DOUBLE)
    override val constants = Flt32

    override fun serialize(encoder: Encoder, value: Flt32) {
        encoder.encodeDouble(value.value.toDouble())
    }

    override fun deserialize(decoder: Decoder): Flt32 {
        return Flt32(decoder.decodeDouble().toFloat())
    }
}

@JvmInline
@Serializable(with = Flt32Serializer::class)
value class Flt32(internal val value: Float) : FloatingImpl<Flt32> {
    companion object : FloatingNumberConstants<Flt32> {
        override val zero: Flt32 get() = Flt32(0.0F)
        override val one: Flt32 get() = Flt32(1.0F)
        override val two: Flt32 get() = Flt32(2.0F)
        override val three: Flt32 get() = Flt32(3.0F)
        override val ten: Flt32 get() = Flt32(10.0F)
        override val minimum: Flt32 get() = Flt32(-Float.MAX_VALUE)
        override val maximum: Flt32 get() = Flt32(Float.MAX_VALUE)
        override val decimalDigits: Int get() = 6
        override val decimalPrecision: Flt32 get() = Flt32(1.19209e-07F)
        override val epsilon: Flt32 get() = Flt32(Float.MIN_VALUE)
        override val nan: Flt32 get() = Flt32(Float.NaN)
        override val infinity: Flt32 get() = Flt32(Float.POSITIVE_INFINITY)
        override val negativeInfinity: Flt32 get() = Flt32(Float.NEGATIVE_INFINITY)

        override val pi: Flt32 get() = Flt32(PI.toFloat())
        override val e: Flt32 get() = Flt32(E.toFloat())
    }

    override val constants: FloatingNumberConstants<Flt32> get() = Companion

    override fun copy() = Flt32(value)

    override fun toString() = value.toString()

    override fun partialOrd(rhs: Flt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Flt32) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = Flt32(-value)
    override fun abs() = Flt32(abs(value))
    override fun reciprocal() = Flt32(1.0F / value)

    override fun plus(rhs: Flt32) = Flt32(value + rhs.value)
    override fun minus(rhs: Flt32) = Flt32(value - rhs.value)
    override fun times(rhs: Flt32) = Flt32(value * rhs.value)
    override fun div(rhs: Flt32) = Flt32(value / rhs.value)
    override fun intDiv(rhs: Flt32) = Flt32(value - value % rhs.value)
    override fun rem(rhs: Flt32) = Flt32(value % rhs.value)

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
}

object Flt64Serializer : RealNumberKSerializer<Flt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flt64", PrimitiveKind.DOUBLE)
    override val constants = Flt64

    override fun serialize(encoder: Encoder, value: Flt64) {
        encoder.encodeDouble(value.value)
    }

    override fun deserialize(decoder: Decoder): Flt64 {
        return Flt64(decoder.decodeDouble())
    }
}

@JvmInline
@Serializable(with = Flt64Serializer::class)
value class Flt64(internal val value: Double) : FloatingImpl<Flt64> {
    companion object : FloatingNumberConstants<Flt64> {
        override val zero: Flt64 get() = Flt64(0.0)
        override val one: Flt64 get() = Flt64(1.0)
        override val two: Flt64 get() = Flt64(2.0)
        override val three: Flt64 get() = Flt64(3.0)
        override val ten: Flt64 get() = Flt64(10.0)
        override val minimum: Flt64 get() = Flt64(-Double.MAX_VALUE)
        override val maximum: Flt64 get() = Flt64(Double.MAX_VALUE)
        override val decimalDigits: Int get() = 15
        override val decimalPrecision: Flt64 get() = Flt64(2.22045e-16)
        override val epsilon: Flt64 get() = Flt64(Double.MIN_VALUE)
        override val nan: Flt64 get() = Flt64(Double.NaN)
        override val infinity: Flt64 get() = Flt64(Double.POSITIVE_INFINITY)
        override val negativeInfinity: Flt64 get() = Flt64(Double.NEGATIVE_INFINITY)

        override val pi: Flt64 get() = Flt64(PI)
        override val e: Flt64 get() = Flt64(E)
    }

    override val constants: FloatingNumberConstants<Flt64> get() = Flt64

    override fun copy() = Flt64(value)

    override fun toString() = value.toString()

    override fun partialOrd(rhs: Flt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: Flt64) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = Flt64(-value)
    override fun abs() = Flt64(abs(value))
    override fun reciprocal() = Flt64(1.0 / value)

    override fun plus(rhs: Flt64) = Flt64(value + rhs.value)
    override fun minus(rhs: Flt64) = Flt64(value - rhs.value)
    override fun times(rhs: Flt64) = Flt64(value * rhs.value)
    override fun div(rhs: Flt64) = Flt64(value / rhs.value)
    override fun intDiv(rhs: Flt64) = Flt64(value - value % rhs.value)
    override fun rem(rhs: Flt64) = Flt64(value % rhs.value)

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

    fun floor() = Flt64(floor(value))
    fun ceil() = Flt64(ceil(value))
    fun round() = Flt64(round(value))

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

    fun toDouble() = value
}

object FltXSerializer : RealNumberKSerializer<FltX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FltX", PrimitiveKind.STRING)
    override val constants = FltX

    override fun serialize(encoder: Encoder, value: FltX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): FltX {
        return FltX(decoder.decodeString())
    }
}

@JvmInline
@Serializable(with = FltXSerializer::class)
value class FltX(internal val value: BigDecimal) : FloatingImpl<FltX> {
    companion object : FloatingNumberConstants<FltX> {
        override val zero: FltX get() = FltX(BigDecimal.ZERO)
        override val one: FltX get() = FltX(BigDecimal.ONE)
        override val two: FltX get() = FltX(2L)
        override val three: FltX get() = FltX(3L)
        override val ten: FltX get() = FltX(10L)
        override val minimum: FltX get() = FltX(-Double.MAX_VALUE)
        override val maximum: FltX get() = FltX(Double.MAX_VALUE)
        override val decimalDigits: Int get() = 18
        override val decimalPrecision: FltX get() = FltX(1e-18)
        override val epsilon: FltX get() = decimalPrecision

        override val pi: FltX get() = FltX(PI.toBigDecimal())
        override val e: FltX get() = FltX(E.toBigDecimal())
    }

    constructor(value: Double) : this(BigDecimal.valueOf(value))
    constructor(value: Long) : this(BigDecimal.valueOf(value))
    constructor(value: String) : this(BigDecimal(value))

    override val constants: FloatingNumberConstants<FltX> get() = Companion

    override fun copy() = FltX(value)

    override fun toString() = value.toString()

    override fun partialOrd(rhs: FltX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: FltX) = (value.compareTo(rhs.value) == 0)

    override fun unaryMinus() = FltX(-value)
    override fun abs() = FltX(value.abs())
    override fun reciprocal() = FltX(one.value / value)

    override fun plus(rhs: FltX) = FltX(value + rhs.value)
    override fun minus(rhs: FltX) = FltX(value - rhs.value)
    override fun times(rhs: FltX) = FltX(value * rhs.value)
    override fun div(rhs: FltX) = FltX(value / rhs.value)
    override fun intDiv(rhs: FltX) = FltX(value - value % rhs.value)
    override fun rem(rhs: FltX) = FltX(value % rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FltX? = when (base) {
        is Flt32 -> log(this, base.toFltX(), FltX)
        is Flt64 -> log(this, base.toFltX(), FltX)
        is FltX -> log(this, base, FltX)
        else -> throw IllegalArgumentException("Unknown argument type to FltX.log: ${base.javaClass}")
    }

    override fun pow(index: Int): FltX = FltX(value.pow(index))

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FltX = when (index) {
        is Flt32 -> pow(this, index.toFltX(), FltX)
        is Flt64 -> pow(this, index.toFltX(), FltX)
        is FltX -> pow(this, index, FltX)
        else -> throw IllegalArgumentException("Unknown argument type to FltX.pow: ${index.javaClass}")
    }

    override fun exp(): FltX = FltX(exp(value.toDouble()))

    override fun toInt8() = Int8(value.toInt().toByte())
    override fun toInt16() = Int16(value.toInt().toShort())
    override fun toInt32() = Int32(value.toInt())
    override fun toInt64() = Int64(value.toLong())
    override fun toIntX() = IntX(value.toString())

    override fun toUInt8() = UInt8(value.toInt().toUByte())
    override fun toUInt16() = UInt16(value.toInt().toUShort())
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    override fun toUInt64() = UInt64(value.toLong().toULong())
    override fun toUIntX() = UIntX(value.toString())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = copy()
}
