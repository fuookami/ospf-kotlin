package fuookami.ospf.kotlin.utils.math.algebra.concept

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*
import fuookami.ospf.kotlin.utils.operator.Abs
import fuookami.ospf.kotlin.utils.operator.Cross
import fuookami.ospf.kotlin.utils.operator.Eq
import fuookami.ospf.kotlin.utils.operator.Exp
import fuookami.ospf.kotlin.utils.operator.Log
import fuookami.ospf.kotlin.utils.operator.Ord
import fuookami.ospf.kotlin.utils.operator.Pow
import fuookami.ospf.kotlin.utils.operator.PowF
import fuookami.ospf.kotlin.utils.operator.RangeTo
import fuookami.ospf.kotlin.utils.operator.Trigonometry

interface NumberRing<Self> : Ring<Self>, PlusGroup<Self>, TimesSemiGroup<Self>

interface NumberField<Self> : Field<Self>, NumberRing<Self>, TimesGroup<Self>

interface Scalar<Self : Scalar<Self>> : Arithmetic<Self>,
    PlusSemiGroup<Self>, TimesSemiGroup<Self>,
    Cross<Self, Self>, Abs<Self> {
    override infix fun x(rhs: Self) = this * rhs
}

interface RealNumber<Self : RealNumber<Self>> : Scalar<Self>, Invariant<Self>, Ord<Self>, Eq<Self>,
    Bounded<Self>, Infinite<Self>, Fixed<Self>, Epsilon<Self>,
    Log<FloatingNumber<*>, FloatingNumber<*>>,
    PowF<FloatingNumber<*>, FloatingNumber<*>>,
    Exp<FloatingNumber<*>>, Trigonometry<FloatingNumber<*>> {
    override val constants: RealNumberConstants<Self>
    override val isBounded: Boolean get() = true
    override val minBound: Self? get() = constants.minimum
    override val maxBound: Self? get() = constants.maximum
    override val supportsInfinity: Boolean get() = constants.infinity != null || constants.negativeInfinity != null
    override val positiveInfinity: Self? get() = constants.infinity
    override val negativeInfinityValue: Self? get() = constants.negativeInfinity
    override val isFixed: Boolean get() = constants.decimalDigits != null
    override val fixedDigits: Int? get() = constants.decimalDigits
    override val fixedPrecision: Self? get() = constants.decimalPrecision
    override val precisionEpsilon: Self? get() = constants.epsilon

    fun isInfinity(): Boolean = this == constants.infinity
    fun isNegativeInfinity(): Boolean = this == constants.negativeInfinity

    override infix fun equiv(rhs: Self) = this == rhs

    fun toInt8(): Int8
    fun toInt16(): Int16
    fun toInt32(): Int32
    fun toInt64(): Int64
    fun toIntX(): IntX

    fun toUInt8(): UInt8
    fun toUInt16(): UInt16
    fun toUInt32(): UInt32
    fun toUInt64(): UInt64
    fun toUIntX(): UIntX

    fun toNInt8(): NInt8 = NInt8(toInt8())
    fun toNInt16(): NInt16 = NInt16(toInt16())
    fun toNInt32(): NInt32 = NInt32(toInt32())
    fun toNInt64(): NInt64 = NInt64(toInt64())
    fun toNIntX(): NIntX = NIntX(toIntX())

    fun toNUInt8(): NUInt8 = NUInt8(toUInt8())
    fun toNUInt16(): NUInt16 = NUInt16(toUInt16())
    fun toNUInt32(): NUInt32 = NUInt32(toUInt32())
    fun toNUInt64(): NUInt64 = NUInt64(toUInt64())
    fun toNUIntX(): NUIntX = NUIntX(toUIntX())

    fun toRtn8(): Rtn8 = Rtn8(toInt8(), Int8.one)
    fun toRtn16(): Rtn16 = Rtn16(toInt16(), Int16.one)
    fun toRtn32(): Rtn32 = Rtn32(toInt32(), Int32.one)
    fun toRtn64(): Rtn64 = Rtn64(toInt64(), Int64.one)
    fun toRtnX(): RtnX = RtnX(toIntX(), IntX.one)

    fun toURtn8(): URtn8 = URtn8(toUInt8(), UInt8.one)
    fun toURtn16(): URtn16 = URtn16(toUInt16(), UInt16.one)
    fun toURtn32(): URtn32 = URtn32(toUInt32(), UInt32.one)
    fun toURtn64(): URtn64 = URtn64(toUInt64(), UInt64.one)
    fun toURtnX(): URtnX = URtnX(toUIntX(), UIntX.one)

    fun toFlt32(): Flt32
    fun toFlt64(): Flt64
    fun toFltX(): FltX
}

interface RealNumberConstants<Self : RealNumber<Self>> : ArithmeticConstants<Self> {
    val two: Self
    val three: Self
    val five: Self
    val ten: Self

    val minimum: Self
    val maximum: Self
    val positiveMinimum get() = one

    val decimalDigits: Int? get() = null
    val decimalPrecision: Self get() = zero
    val epsilon: Self get() = zero

    val nan: Self? get() = null
    val infinity: Self? get() = null
    val negativeInfinity: Self? get() = null
}

interface Integer<Self : RealNumber<Self>> : RealNumber<Self>, RangeTo<Self, Self>

interface IntegerNumber<Self : IntegerNumber<Self>> : Integer<Self>, NumberField<Self>, Pow<Self>

interface UIntegerNumber<Self : UIntegerNumber<Self>> : Integer<Self>, NumberField<Self>, Pow<Self>

interface RationalNumber<Self : RationalNumber<Self, I>, I> : RealNumber<Self>, NumberField<Self>, Pow<Self>
        where I : Integer<I>, I : NumberField<I>

interface RationalNumberConstants<Self : RationalNumber<Self, I>, I> : RealNumberConstants<Self>
        where I : Integer<I>, I : NumberField<I> {
    val half: Self
}

interface FloatingNumber<Self : FloatingNumber<Self>> : RealNumber<Self>, NumberField<Self>, Pow<Self> {
    override val constants: FloatingNumberConstants<Self>
}

interface FloatingNumberConstants<Self : FloatingNumber<Self>> : RealNumberConstants<Self> {
    override val positiveMinimum: Self get() = epsilon

    val half: Self
    val pi: Self
    val e: Self
    val lg2: Self
}

interface NumericIntegerNumber<Self : NumericIntegerNumber<Self, I>, I : IntegerNumber<I>> : Integer<Self>,
    PlusGroup<Self>, TimesSemiGroup<Self>,
    fuookami.ospf.kotlin.utils.operator.Reciprocal<RationalNumber<*, I>>,
    fuookami.ospf.kotlin.utils.operator.Div<Self, RationalNumber<*, I>>,
    fuookami.ospf.kotlin.utils.operator.IntDiv<Self, Self>,
    fuookami.ospf.kotlin.utils.operator.Rem<Self, Self>,
    Pow<RationalNumber<*, I>>

interface NumericUIntegerNumber<Self : NumericUIntegerNumber<Self, I>, I : UIntegerNumber<I>> : Integer<Self>,
    PlusSemiGroup<Self>, TimesSemiGroup<Self>,
    fuookami.ospf.kotlin.utils.operator.Dec<Self>,
    fuookami.ospf.kotlin.utils.operator.Neg<NumericIntegerNumber<*, *>>,
    fuookami.ospf.kotlin.utils.operator.Minus<Self, NumericIntegerNumber<*, *>>,
    fuookami.ospf.kotlin.utils.operator.Reciprocal<RationalNumber<*, I>>,
    fuookami.ospf.kotlin.utils.operator.Div<Self, RationalNumber<*, I>>,
    fuookami.ospf.kotlin.utils.operator.IntDiv<Self, Self>,
    fuookami.ospf.kotlin.utils.operator.Rem<Self, Self>,
    Pow<RationalNumber<*, I>>


