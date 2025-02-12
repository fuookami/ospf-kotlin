package fuookami.ospf.kotlin.utils.math

import java.math.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*

interface NumericInteger<Self, I>
    : NumericIntegerNumber<Self, I> where Self : NumericInteger<Self, I>, I : IntegerNumber<I>, I : NumberField<I> {
    override fun inc() = this + constants.one
    override fun dec() = this - constants.one

    override fun lg() = log(Flt64.ten)
    override fun lg2() = log(Flt64.two)
    override fun ln() = log(Flt64.e)

    override fun sqr() = pow(2)
    override fun cub() = pow(3)

    override fun sqrt() = pow(Flt64.two.reciprocal())
    override fun cbrt() = pow(Flt64.three.reciprocal())

    override fun rangeTo(rhs: Self) = IntegerRange(copy(), rhs, constants.one, constants)
    override infix fun until(rhs: Self) = this.rangeTo(rhs - constants.one)
}

abstract class NumericIntegerConstants<Self, I>(
    private val ctor: (I) -> Self,
    private val constants: RealNumberConstants<I>
) : RealNumberConstants<Self> where Self : NumericInteger<Self, I>, I : IntegerNumber<I>, I : NumberField<I> {
    override val zero: Self get() = ctor(constants.zero)
    override val one: Self get() = ctor(constants.one)
    override val two: Self get() = ctor(constants.two)
    override val three: Self get() = ctor(constants.three)
    override val five: Self get() = ctor(constants.five)
    override val ten: Self get() = ctor(constants.ten)
    override val minimum: Self get() = ctor(constants.minimum)
    override val maximum: Self get() = ctor(constants.maximum)
}

data object NInt8Serializer : KSerializer<NInt8> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt8", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NInt8) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NInt8 {
        return NInt8(Int8(decoder.decodeInt().toByte()))
    }
}

@JvmInline
@Serializable(with = NInt8Serializer::class)
value class NInt8(val value: Int8) : NumericInteger<NInt8, Int8>, Copyable<NInt8> {
    companion object : NumericIntegerConstants<NInt8, Int8>(NInt8::invoke, Int8) {
        operator fun invoke(value: Int8) = NInt8(value)
    }

    override val constants: RealNumberConstants<NInt8> get() = NInt8

    override fun copy() = NInt8(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt8) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt8) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn8(Int8.one, value)
    override fun unaryMinus() = NInt8(-value)
    override fun abs() = NInt8(value.abs())

    override fun plus(rhs: NInt8) = NInt8(value + rhs.value)
    override fun minus(rhs: NInt8) = NInt8(value - rhs.value)
    override fun times(rhs: NInt8) = NInt8(value * rhs.value)
    override fun div(rhs: NInt8) = Rtn8(value, rhs.value)
    override fun rem(rhs: NInt8) = NInt8(value % rhs.value)
    override fun intDiv(rhs: NInt8) = NInt8(value / rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NInt8.log: ${base.javaClass}")
    }

    override fun pow(index: Int): Rtn8 {
        return if (index >= 1) {
            Rtn8(value.pow(index), Int8.one)
        } else if (index <= -1) {
            Rtn8(Int8.one, value.pow(index))
        } else {
            Rtn8.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NInt8.log: ${index.javaClass}")
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

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

data object NInt16Serializer : KSerializer<NInt16> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt16", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NInt16) {
        encoder.encodeInt(value.value.value.toInt())
    }

    override fun deserialize(decoder: Decoder): NInt16 {
        return NInt16(Int16(decoder.decodeInt().toShort()))
    }
}

@JvmInline
@Serializable(with = NInt16Serializer::class)
value class NInt16(val value: Int16) : NumericInteger<NInt16, Int16>, Copyable<NInt16> {
    companion object : NumericIntegerConstants<NInt16, Int16>(NInt16::invoke, Int16) {
        operator fun invoke(value: Int16) = NInt16(value)
    }

    override val constants: RealNumberConstants<NInt16> get() = NInt16

    override fun copy() = NInt16(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt16) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt16) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn16(Int16.one, value)
    override fun unaryMinus() = NInt16(-value)
    override fun abs() = NInt16(value.abs())

    override fun plus(rhs: NInt16) = NInt16(value + rhs.value)
    override fun minus(rhs: NInt16) = NInt16(value - rhs.value)
    override fun times(rhs: NInt16) = NInt16(value * rhs.value)
    override fun div(rhs: NInt16) = Rtn16(value, rhs.value)
    override fun rem(rhs: NInt16) = NInt16(value % rhs.value)
    override fun intDiv(rhs: NInt16) = NInt16(value / rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NInt16.log: ${base.javaClass}")
    }

    override fun pow(index: Int): Rtn16 {
        return if (index >= 1) {
            Rtn16(value.pow(index), Int16.one)
        } else if (index <= -1) {
            Rtn16(Int16.one, value.pow(index))
        } else {
            Rtn16.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NInt16.log: ${index.javaClass}")
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

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

data object NInt32Serializer : KSerializer<NInt32> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt32", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: NInt32) {
        encoder.encodeInt(value.value.value)
    }

    override fun deserialize(decoder: Decoder): NInt32 {
        return NInt32(Int32(decoder.decodeInt()))
    }
}

@JvmInline
@Serializable(with = NInt32Serializer::class)
value class NInt32(val value: Int32) : NumericInteger<NInt32, Int32>, Copyable<NInt32> {
    companion object : NumericIntegerConstants<NInt32, Int32>(NInt32::invoke, Int32) {
        operator fun invoke(value: Int32) = NInt32(value)
    }

    override val constants: RealNumberConstants<NInt32> get() = NInt32

    override fun copy(): NInt32 = NInt32(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt32) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt32) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn32(Int32.one, value)
    override fun unaryMinus() = NInt32(-value)
    override fun abs() = NInt32(value.abs())

    override fun plus(rhs: NInt32) = NInt32(value + rhs.value)
    override fun minus(rhs: NInt32) = NInt32(value - rhs.value)
    override fun times(rhs: NInt32) = NInt32(value * rhs.value)
    override fun div(rhs: NInt32) = Rtn32(value, rhs.value)
    override fun rem(rhs: NInt32) = NInt32(value % rhs.value)
    override fun intDiv(rhs: NInt32) = NInt32(value / rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NInt32.log: ${base.javaClass}")
    }

    override fun pow(index: Int): Rtn32 {
        return if (index >= 1) {
            Rtn32(value.pow(index), Int32.one)
        } else if (index <= -1) {
            Rtn32(Int32.one, value.pow(index))
        } else {
            Rtn32.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NInt32.log: ${index.javaClass}")
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

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

data object NInt64Serializer : KSerializer<NInt64> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NInt64", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: NInt64) {
        encoder.encodeLong(value.value.value)
    }

    override fun deserialize(decoder: Decoder): NInt64 {
        return NInt64(Int64(decoder.decodeLong()))
    }
}

@JvmInline
@Serializable(with = NInt64Serializer::class)
value class NInt64(val value: Int64) : NumericInteger<NInt64, Int64>, Copyable<NInt64> {
    companion object : NumericIntegerConstants<NInt64, Int64>(NInt64::invoke, Int64) {
        operator fun invoke(value: Int64) = NInt64(value)
    }

    override val constants: RealNumberConstants<NInt64> get() = NInt64

    override fun copy(): NInt64 = NInt64(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NInt64) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NInt64) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = Rtn64(Int64.one, value)
    override fun unaryMinus() = NInt64(-value)
    override fun abs() = NInt64(value.abs())

    override fun plus(rhs: NInt64) = NInt64(value + rhs.value)
    override fun minus(rhs: NInt64) = NInt64(value - rhs.value)
    override fun times(rhs: NInt64) = NInt64(value * rhs.value)
    override fun div(rhs: NInt64) = Rtn64(value, rhs.value)
    override fun rem(rhs: NInt64) = NInt64(value % rhs.value)
    override fun intDiv(rhs: NInt64) = NInt64(value / rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NInt64.log: ${base.javaClass}")
    }

    override fun pow(index: Int): Rtn64 {
        return if (index >= 1) {
            Rtn64(value.pow(index), Int64.one)
        } else if (index <= -1) {
            Rtn64(Int64.one, value.pow(index))
        } else {
            Rtn64.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NInt64.log: ${index.javaClass}")
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

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}

data object NIntXSerializer : KSerializer<NIntX> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NIntX", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NIntX) {
        encoder.encodeString(value.value.toString(10))
    }

    override fun deserialize(decoder: Decoder): NIntX {
        return NIntX(IntX(BigInteger(decoder.decodeString())))
    }
}

@JvmInline
@Serializable(NIntXSerializer::class)
value class NIntX(val value: IntX) : NumericInteger<NIntX, IntX>, Copyable<NIntX> {
    companion object : NumericIntegerConstants<NIntX, IntX>(NIntX::invoke, IntX) {
        operator fun invoke(value: IntX) = NIntX(value)
    }

    override val constants: RealNumberConstants<NIntX> get() = NIntX

    override fun copy(): NIntX = NIntX(value)

    override fun toString() = value.toString()
    fun toString(radix: Int) = value.toString(radix)

    override fun partialOrd(rhs: NIntX) = orderOf(value.compareTo(rhs.value))
    override fun partialEq(rhs: NIntX) = (value.compareTo(rhs.value) == 0)

    override fun reciprocal() = RtnX(IntX.one, value)
    override fun unaryMinus() = NIntX(-value)
    override fun abs() = NIntX(value.abs())

    override fun plus(rhs: NIntX) = NIntX(value + rhs.value)
    override fun minus(rhs: NIntX) = NIntX(value - rhs.value)
    override fun times(rhs: NIntX) = NIntX(value * rhs.value)
    override fun div(rhs: NIntX) = RtnX(value, rhs.value)
    override fun rem(rhs: NIntX) = NIntX(value % rhs.value)
    override fun intDiv(rhs: NIntX) = NIntX(value / rhs.value)

    @Throws(IllegalArgumentException::class)
    override fun log(base: FloatingNumber<*>): FloatingNumber<*>? = when (base) {
        is Flt32 -> toFlt32().log(base)
        is Flt64 -> toFlt64().log(base)
        is FltX -> toFltX().log(base)
        else -> throw IllegalArgumentException("Unknown argument type to NIntX.log: ${base.javaClass}")
    }

    override fun lg() = log(FltX(10.0))
    override fun ln() = log(FltX.e)

    override fun pow(index: Int): RtnX {
        return if (index >= 1) {
            RtnX(value.pow(index), IntX.one)
        } else if (index <= -1) {
            RtnX(IntX.one, value.pow(index))
        } else {
            RtnX.one
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun pow(index: FloatingNumber<*>): FloatingNumber<*> = when (index) {
        is Flt32 -> toFlt32().pow(index)
        is Flt64 -> toFlt64().pow(index)
        is FltX -> toFltX().pow(index)
        else -> throw IllegalArgumentException("Unknown argument type to NIntX.log: ${index.javaClass}")
    }

    override fun sqrt() = pow(FltX(1.0 / 2.0))
    override fun cbrt() = pow(FltX(1.0 / 3.0))

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

    override fun toInt8() = value.toInt8()
    override fun toInt16() = value.toInt16()
    override fun toInt32() = value.toInt32()
    override fun toInt64() = value.toInt64()
    override fun toIntX() = value.toIntX()

    override fun toUInt8() = value.toUInt8()
    override fun toUInt16() = value.toUInt16()
    override fun toUInt32() = value.toUInt32()
    override fun toUInt64() = value.toUInt64()
    override fun toUIntX() = value.toUIntX()

    override fun toFlt32() = value.toFlt32()
    override fun toFlt64() = value.toFlt64()
    override fun toFltX() = value.toFltX()
}
