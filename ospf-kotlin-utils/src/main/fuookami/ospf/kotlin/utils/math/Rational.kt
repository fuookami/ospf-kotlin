package fuookami.ospf.kotlin.utils.math

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*

abstract class RationalSerializer<Self, I>(
    name: String,
    val ctor: (I, I) -> Self,
) : KSerializer<Self> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name) {
        element<JsonElement>("num")
        element<JsonElement>("den")
    }

    abstract val valueSerializer: KSerializer<I>

    override fun serialize(encoder: Encoder, value: Self) {
        require(encoder is JsonEncoder)
        encoder.encodeJsonElement(
            buildJsonObject {
                put("num", encoder.json.encodeToJsonElement(valueSerializer, value.num))
                put("den", encoder.json.encodeToJsonElement(valueSerializer, value.den))
            }
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Self {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        require(element is JsonObject)
        require(descriptor.elementNames.all { it in element })
        return ctor(
            decoder.json.decodeFromJsonElement(valueSerializer, element["num"]!!),
            decoder.json.decodeFromJsonElement(valueSerializer, element["den"]!!),
        )
    }
}

abstract class Rational<Self, I> protected constructor(
    private val ctor: (I, I) -> Self,
    private val integerConstants: RealNumberConstants<I>
) : RationalNumber<Self, I> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    abstract val num: I
    abstract val den: I

    override fun copy() = ctor(num, den)

    override fun toString() = "($num / $den)"
    abstract fun toString(radix: Int): String;

    override fun partialOrd(rhs: Self) = orderOf((num * rhs.den).compareTo(den * rhs.num))
    override fun partialEq(rhs: Self) = num.eq(rhs.num) && den.eq(rhs.den)

    override fun unaryMinus() = ctor(-num, den)
    override fun reciprocal() = ctor(den, num)
    override fun abs() = ctor(num.abs(), den)

    override fun inc() = ctor(num + den, den)
    override fun dec() = ctor(num - den, den)

    override fun rem(rhs: Self): Self {
        val k = this intDiv rhs;
        return this - k * rhs;
    }

    override fun intDiv(rhs: Self): Self {
        val divisor = this / rhs;
        return ctor(divisor.num / divisor.den, integerConstants.one);
    }

    override fun log(base: FloatingNumber<*>) = toFltX().log(base)
    override fun lg() = log(FltX.ten)
    override fun lg2() = log(FltX.two)
    override fun ln() = log(FltX.e)

    override fun pow(index: FloatingNumber<*>) = toFltX().pow(index)
    override fun pow(index: Int) = pow(copy(), index, constants)
    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(FltX.two.reciprocal())
    override fun cbrt() = pow(FltX.three.reciprocal())

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

    override fun toInt8() = (num / den).toInt8()
    override fun toInt16() = (num / den).toInt16()
    override fun toInt32() = (num / den).toInt32()
    override fun toInt64() = (num / den).toInt64()
    override fun toIntX() = (num / den).toIntX()

    override fun toUInt8() = (num / den).toUInt8()
    override fun toUInt16() = (num / den).toUInt16()
    override fun toUInt32() = (num / den).toUInt32()
    override fun toUInt64() = (num / den).toUInt64()
    override fun toUIntX() = (num / den).toUIntX()

    override fun toRtn8() = Rtn8(num.toInt8(), den.toInt8())
    override fun toRtn16() = Rtn16(num.toInt16(), den.toInt16())
    override fun toRtn32() = Rtn32(num.toInt32(), den.toInt32())
    override fun toRtn64() = Rtn64(num.toInt64(), den.toInt64())
    override fun toRtnX() = RtnX(num.toIntX(), den.toIntX())

    override fun toURtn8() = URtn8(num.toUInt8(), den.toUInt8())
    override fun toURtn16() = URtn16(num.toUInt16(), den.toUInt16())
    override fun toURtn32() = URtn32(num.toUInt32(), den.toUInt32())
    override fun toURtn64() = URtn64(num.toUInt64(), den.toUInt64())
    override fun toURtnX() = URtnX(num.toUIntX(), den.toUIntX())

    override fun toFlt32() = num.toFlt32() / den.toFlt32()
    override fun toFlt64() = num.toFlt64() / den.toFlt64()
    override fun toFltX() = num.toFltX() / den.toFltX()
}

abstract class RationalConstants<Self, I> protected constructor(
    private val ctor: (I, I) -> Self,
    private val constants: RealNumberConstants<I>
) : RealNumberConstants<Self> where Self : Rational<Self, I>, I : Integer<I>, I : NumberField<I> {
    override val zero: Self get() = ctor(constants.zero, constants.one)
    override val one: Self get() = ctor(constants.one, constants.one)
    override val two: Self get() = ctor(constants.two, constants.one)
    override val three: Self get() = ctor(constants.three, constants.one)
    override val five: Self get() = ctor(constants.five, constants.one)
    override val ten: Self get() = ctor(constants.ten, constants.one)
    override val minimum: Self get() = ctor(constants.minimum, constants.one)
    override val maximum: Self get() = ctor(constants.maximum, constants.one)

    override val nan: Self get() = ctor(constants.zero, constants.zero)
    override val infinity: Self get() = ctor(constants.one, constants.zero)
    override val negativeInfinity: Self get() = ctor(-constants.one, constants.zero)
}

data object Rtn8Serializer : RationalSerializer<Rtn8, Int8>("Rtn8", Rtn8::invoke) {
    override val valueSerializer = Int8Serializer
}

@Serializable(with = Rtn8Serializer::class)
data class Rtn8 internal constructor(
    override val num: Int8,
    override val den: Int8
) : Rational<Rtn8, Int8>(Rtn8::invoke, Int8), Copyable<Rtn8> {
    companion object : RationalConstants<Rtn8, Int8>(Rtn8::invoke, Int8) {
        operator fun invoke(num: Int8, den: Int8): Rtn8 {
            val divisor = gcd(num, den)
            val negative = (num < Int8.zero) xor (den < Int8.zero);
            return if (negative) {
                Rtn8(-num.abs() / divisor, den.abs() / divisor)
            } else {
                Rtn8(num.abs() / divisor, den.abs() / divisor)
            }
        }
    }

    override val constants: RealNumberConstants<Rtn8> get() = Companion

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: Rtn8) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: Rtn8) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: Rtn8) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: Rtn8) = invoke(num * rhs.den, rhs.num * den)
}

data object Rtn16Serializer : RationalSerializer<Rtn16, Int16>("Rtn16", Rtn16::invoke) {
    override val valueSerializer = Int16Serializer
}

@Serializable(with = Rtn16Serializer::class)
data class Rtn16 internal constructor(
    override val num: Int16,
    override val den: Int16
) : Rational<Rtn16, Int16>(Rtn16::invoke, Int16), Copyable<Rtn16> {
    companion object : RationalConstants<Rtn16, Int16>(Rtn16::invoke, Int16) {
        operator fun invoke(num: Int16, den: Int16): Rtn16 {
            val divisor = gcd(num, den)
            return Rtn16(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn16> get() = Rtn16

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: Rtn16) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: Rtn16) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: Rtn16) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: Rtn16) = invoke(num * rhs.den, rhs.num * den)
}

data object Rtn32Serializer : RationalSerializer<Rtn32, Int32>("Rtn32", Rtn32::invoke) {
    override val valueSerializer = Int32Serializer
}

@Serializable(with = Rtn32Serializer::class)
data class Rtn32 internal constructor(
    override val num: Int32,
    override val den: Int32
) : Rational<Rtn32, Int32>(Rtn32::invoke, Int32), Copyable<Rtn32> {
    companion object : RationalConstants<Rtn32, Int32>(Rtn32::invoke, Int32) {
        operator fun invoke(num: Int32, den: Int32): Rtn32 {
            val divisor = gcd(num, den)
            return Rtn32(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn32> get() = Rtn32

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: Rtn32) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: Rtn32) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: Rtn32) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: Rtn32) = invoke(num * rhs.den, rhs.num * den)
}

data object Rtn64Serializer : RationalSerializer<Rtn64, Int64>("Rtn64", Rtn64::invoke) {
    override val valueSerializer = Int64Serializer
}

@Serializable(with = Rtn64Serializer::class)
data class Rtn64 internal constructor(
    override val num: Int64,
    override val den: Int64
) : Rational<Rtn64, Int64>(Rtn64::invoke, Int64), Copyable<Rtn64> {
    companion object : RationalConstants<Rtn64, Int64>(Rtn64::invoke, Int64) {
        operator fun invoke(num: Int64, den: Int64): Rtn64 {
            val divisor = gcd(num, den)
            return Rtn64(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<Rtn64> get() = Rtn64

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: Rtn64) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: Rtn64) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: Rtn64) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: Rtn64) = invoke(num * rhs.den, rhs.num * den)
}

data object RtnXSerializer : RationalSerializer<RtnX, IntX>("RtnX", RtnX::invoke) {
    override val valueSerializer = IntXSerializer
}

@Serializable(with = RtnXSerializer::class)
data class RtnX internal constructor(
    override val num: IntX,
    override val den: IntX
) : Rational<RtnX, IntX>(RtnX::invoke, IntX), Copyable<RtnX> {
    companion object : RationalConstants<RtnX, IntX>(RtnX::invoke, IntX) {
        operator fun invoke(num: IntX, den: IntX): RtnX {
            val divisor = gcd(num, den)
            return RtnX(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<RtnX> get() = RtnX

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: RtnX) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: RtnX) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: RtnX) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: RtnX) = invoke(num * rhs.den, rhs.num * den)
}

object URtn8Serializer : RationalSerializer<URtn8, UInt8>("URtn8", URtn8::invoke) {
    override val valueSerializer = UInt8Serializer
}

@Serializable(with = URtn8Serializer::class)
data class URtn8 internal constructor(
    override val num: UInt8,
    override val den: UInt8
) : Rational<URtn8, UInt8>(URtn8::invoke, UInt8), Copyable<URtn8> {
    companion object : RationalConstants<URtn8, UInt8>(URtn8::invoke, UInt8) {
        operator fun invoke(num: UInt8, den: UInt8): URtn8 {
            val divisor = gcd(num, den)
            return URtn8(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn8> get() = URtn8

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: URtn8) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: URtn8) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: URtn8) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: URtn8) = invoke(num * rhs.den, rhs.num * den)
}

object URtn16Serializer : RationalSerializer<URtn16, UInt16>("URtn16", URtn16::invoke) {
    override val valueSerializer = UInt16Serializer
}

@Serializable(with = URtn16Serializer::class)
data class URtn16 internal constructor(
    override val num: UInt16,
    override val den: UInt16
) : Rational<URtn16, UInt16>(URtn16::invoke, UInt16), Copyable<URtn16> {
    companion object : RationalConstants<URtn16, UInt16>(URtn16::invoke, UInt16) {
        operator fun invoke(num: UInt16, den: UInt16): URtn16 {
            val divisor = gcd(num, den)
            return URtn16(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn16> get() = URtn16

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: URtn16) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: URtn16) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: URtn16) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: URtn16) = invoke(num * rhs.den, rhs.num * den)
}

object URtn32Serializer : RationalSerializer<URtn32, UInt32>("URtn32", URtn32::invoke) {
    override val valueSerializer = UInt32Serializer
}

@Serializable(with = URtn32Serializer::class)
data class URtn32 internal constructor(
    override val num: UInt32,
    override val den: UInt32
) : Rational<URtn32, UInt32>(URtn32::invoke, UInt32), Copyable<URtn32> {
    companion object : RationalConstants<URtn32, UInt32>(URtn32::invoke, UInt32) {
        operator fun invoke(num: UInt32, den: UInt32): URtn32 {
            val divisor = gcd(num, den)
            return URtn32(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtn32> get() = URtn32

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: URtn32) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: URtn32) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: URtn32) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: URtn32) = invoke(num * rhs.den, rhs.num * den)
}

object URtn64Serializer : RationalSerializer<URtn64, UInt64>("URtn64", URtn64::invoke) {
    override val valueSerializer = UInt64Serializer
}

@Serializable(with = URtn64Serializer::class)
data class URtn64 internal constructor(
    override val num: UInt64,
    override val den: UInt64
) : Rational<URtn64, UInt64>(URtn64::invoke, UInt64), Copyable<URtn64> {
    companion object : RationalConstants<URtn64, UInt64>(URtn64::invoke, UInt64) {
        operator fun invoke(num: UInt64, den: UInt64): URtn64 {
            val divisor = gcd(num, den)
            return URtn64(num / divisor, den / divisor)
        }

        operator fun invoke(num: Int, den: Int): URtn64 {
            return this(UInt64(num), UInt64(den))
        }
    }

    override val constants: RealNumberConstants<URtn64> get() = URtn64

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: URtn64) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: URtn64) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: URtn64) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: URtn64) = invoke(num * rhs.den, rhs.num * den)
}

object URtnXSerializer : RationalSerializer<URtnX, UIntX>("URtnX", URtnX::invoke) {
    override val valueSerializer = UIntXSerializer
}

@Serializable(with = URtnXSerializer::class)
data class URtnX internal constructor(
    override val num: UIntX,
    override val den: UIntX
) : Rational<URtnX, UIntX>(URtnX::invoke, UIntX), Copyable<URtnX> {
    companion object : RationalConstants<URtnX, UIntX>(URtnX::invoke, UIntX) {
        operator fun invoke(num: UIntX, den: UIntX): URtnX {
            val divisor = gcd(num, den)
            return URtnX(num / divisor, den / divisor)
        }
    }

    override val constants: RealNumberConstants<URtnX> get() = URtnX

    override fun toString(radix: Int) = "(${num.toString(radix)} / ${den.toString(radix)})"

    override fun plus(rhs: URtnX) = invoke(num * rhs.den + rhs.num * den, den * rhs.den)
    override fun minus(rhs: URtnX) = invoke(num * rhs.den - rhs.num * den, den * rhs.den)
    override fun times(rhs: URtnX) = invoke(num * rhs.num, den * rhs.den)
    override fun div(rhs: URtnX) = invoke(num * rhs.den, rhs.num * den)
}
