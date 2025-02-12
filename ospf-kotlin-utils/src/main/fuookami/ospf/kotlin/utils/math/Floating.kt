package fuookami.ospf.kotlin.utils.math

import java.math.*
import kotlin.math.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.Flt32.Companion
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

    fun floorTo(precision: Int = this.constants.decimalDigits!!): Self
    fun ceilTo(precision: Int = this.constants.decimalDigits!!): Self
    fun roundTo(precision: Int = this.constants.decimalDigits!!): Self
}

data object Flt32Serializer : KSerializer<Flt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flt32", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Flt32) {
        encoder.encodeDouble(value.value.toDouble())
    }

    override fun deserialize(decoder: Decoder): Flt32 {
        return Flt32(decoder.decodeDouble().toFloat())
    }
}

interface Flt32Interface {
    fun toFloat(): Float
}

@JvmInline
@Serializable(with = Flt32Serializer::class)
value class Flt32(internal val value: Float) : Flt32Interface, FloatingImpl<Flt32>, Copyable<Flt32> {
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
        override val pi: Flt32 get() = Flt32(PI.toFloat())
        @JvmStatic
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

    override fun floorTo(precision: Int) = Flt32(floor(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    override fun ceilTo(precision: Int) = Flt32(ceil(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
    override fun roundTo(precision: Int) = Flt32(round(value * 10.0F.pow(precision)) / 10.0F.pow(precision))
}

data object Flt64Serializer : KSerializer<Flt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Flt64", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Flt64) {
        encoder.encodeDouble(value.value)
    }

    override fun deserialize(decoder: Decoder): Flt64 {
        return Flt64(decoder.decodeDouble())
    }
}

interface Flt64Interface {
    fun toDouble(): Double
}

@JvmInline
@Serializable(with = Flt64Serializer::class)
value class Flt64(internal val value: Double) : Flt64Interface, FloatingImpl<Flt64>, Copyable<Flt64> {
    constructor(value: Int) : this(value.toDouble())

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
        override val pi: Flt64 get() = Flt64(PI)
        @JvmStatic
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

    override fun floorTo(precision: Int) = Flt64(floor(value * 10.0.pow(precision)) / 10.0.pow(precision))
    override fun ceilTo(precision: Int) = Flt64(ceil(value * 10.0.pow(precision)) / 10.0.pow(precision))
    override fun roundTo(precision: Int) = Flt64(round(value * 10.0.pow(precision)) / 10.0.pow(precision))
}

data object FltXSerializer : KSerializer<FltX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FltX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: FltX) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): FltX {
        return FltX(decoder.decodeString())
    }
}

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

interface FltXInterface {
    fun toDecimal(): BigDecimal
}

@JvmInline
@Serializable(with = FltXSerializer::class)
value class FltX(internal val value: BigDecimal) : FltXInterface, FloatingImpl<FltX>, Copyable<FltX> {
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
        override val pi: FltX get() = FltX(PI.toBigDecimal())
        @JvmStatic
        override val e: FltX get() = FltX(E.toBigDecimal())
    }

    constructor(value: Double, scale: Int = decimalDigits, roundingMode: RoundingMode = RoundingMode.HALF_UP) : this(BigDecimal.valueOf(value).setScale(scale, roundingMode))
    constructor(value: Long, scale: Int = 2, roundingMode: RoundingMode = RoundingMode.HALF_UP) : this(BigDecimal.valueOf(value).setScale(scale, roundingMode))
    constructor(value: String, scale: Int = decimalDigits, roundingMode: RoundingMode = RoundingMode.HALF_UP) : this(BigDecimal(value).setScale(scale, roundingMode))

    fun withScale(scale: Int) = FltX(value.setScale(scale))
    fun withScale(scale: Int, roundingMode: RoundingMode) = FltX(value.setScale(scale, roundingMode))

    override val constants: FloatingNumberConstants<FltX> get() = Companion

    override fun copy() = FltX(value)

    override fun toString() = value.toString()
    fun toEngineeringString(): String = value.stripTrailingZeros().toEngineeringString()
    fun toPlainString(): String = value.stripTrailingZeros().toPlainString()

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

    override fun pow(index: Int) = FltX(value.pow(index))

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FltX = when (index) {
        is Flt32 -> pow(this, index.toFltX(), FltX)
        is Flt64 -> pow(this, index.toFltX(), FltX)
        is FltX -> pow(this, index, FltX)
        else -> throw IllegalArgumentException("Unknown argument type to FltX.pow: ${index.javaClass}")
    }

    override fun exp() = FltX(exp(value.toDouble()))

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
    override fun toIntX() = IntX(value.toString())

    override fun toUInt8() = UInt8(value.toInt().toUByte())
    override fun toUInt16() = UInt16(value.toInt().toUShort())
    override fun toUInt32() = UInt32(value.toLong().toUInt())
    override fun toUInt64() = UInt64(value.toLong().toULong())
    override fun toUIntX() = UIntX(value.toString())

    override fun toFlt32() = Flt32(value.toFloat())
    override fun toFlt64() = Flt64(value.toDouble())
    override fun toFltX() = copy()

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
}

fun String.toFlt32() = Flt32(toFloat())
fun String.toFlt32OrNull() = toFloatOrNull()?.let { Flt32(it) }
fun String.toFlt64() = Flt64(toDouble())
fun String.toFlt64OrNull() = toDoubleOrNull()?.let { Flt64(it) }
fun String.toFltX() = FltX(toBigDecimal())
fun String.toFltXOrNull() = toBigDecimalOrNull()?.let { FltX(it) }
