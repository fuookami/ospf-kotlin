package fuookami.ospf.kotlin.utils.math

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*

interface Arithmetic<Self> : Copyable<Self>, PartialEq<Self> {
    val constants: ArithmeticConstants<Self>

    infix fun equiv(rhs: Self): Boolean
}

interface ArithmeticConstants<Self> {
    val zero: Self
    val one: Self
}

interface Invariant<T> {
    @Suppress("UNCHECKED_CAST")
    fun value(): T = this as T
}

interface Variant<T> {
    fun value(): T? = null
}

interface PlusSemiGroup<Self> : Plus<Self, Self>, Inc<Self>
interface PlusGroup<Self> : PlusSemiGroup<Self>,
    Neg<Self>, Minus<Self, Self>, Dec<Self>

interface TimesSemiGroup<Self> : Times<Self, Self>
interface TimesGroup<Self> : TimesSemiGroup<Self>,
    Reciprocal<Self>, Div<Self, Self>, IntDiv<Self, Self>, Rem<Self, Self>

interface NumberRing<Self> : PlusGroup<Self>, TimesSemiGroup<Self>
interface NumberField<Self> : NumberRing<Self>, TimesGroup<Self>

interface Scalar<Self : Scalar<Self>> : Arithmetic<Self>,
    PlusSemiGroup<Self>, TimesSemiGroup<Self>,
    Cross<Self, Self>, Abs<Self> {
    override infix fun x(rhs: Self) = this * rhs
}

interface RealNumber<Self : RealNumber<Self>> : Scalar<Self>, Invariant<Self>, Ord<Self>, Eq<Self>,
    Log<FloatingNumber<*>, FloatingNumber<*>>,
    PowF<FloatingNumber<*>, FloatingNumber<*>>,
    Exp<FloatingNumber<*>>, Trigonometry<FloatingNumber<*>> {
    override val constants: RealNumberConstants<Self>

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

interface FloatingNumber<Self : FloatingNumber<Self>> : RealNumber<Self>, NumberField<Self>, Pow<Self> {
    override val constants: FloatingNumberConstants<Self>
}

interface FloatingNumberConstants<Self : FloatingNumber<Self>> : RealNumberConstants<Self> {
    override val positiveMinimum: Self get() = epsilon

    val pi: Self
    val e: Self
}

interface NumericIntegerNumber<Self : NumericIntegerNumber<Self, I>, I : IntegerNumber<I>> : Integer<Self>,
    PlusGroup<Self>, TimesSemiGroup<Self>,
    Reciprocal<RationalNumber<*, I>>, Div<Self, RationalNumber<*, I>>, IntDiv<Self, Self>, Rem<Self, Self>,
    Pow<RationalNumber<*, I>>

interface NumericUIntegerNumber<Self : NumericUIntegerNumber<Self, I>, I : UIntegerNumber<I>> : Integer<Self>,
    PlusSemiGroup<Self>, TimesSemiGroup<Self>,
    Dec<Self>, Neg<NumericIntegerNumber<*, *>>, Minus<Self, NumericIntegerNumber<*, *>>,
    Reciprocal<RationalNumber<*, I>>, Div<Self, RationalNumber<*, I>>, IntDiv<Self, Self>, Rem<Self, Self>,
    Pow<RationalNumber<*, I>>
typealias NaturalNumber<Self, I> = NumericUIntegerNumber<Self, I>

data object Infinity {
    override fun toString() = "inf"
}

data object NegativeInfinity {
    override fun toString() = "-inf"
}

val <T> Collection<T>.usize get() = UInt64(size)
val <T> Collection<T>.uIndices get() = UInt64.zero until usize
val <T> List<T>.lastUIndex get() = UInt64(lastIndex)
val <K, V> Map<K, V>.usize get() = UInt64(size)
operator fun <T> List<T>.get(index: UInt32) = get(index.toInt())
operator fun <T> MutableList<T>.set(index: UInt32, element: T) = set(index.toInt(), element)
operator fun <T> List<T>.get(index: UInt64) = get(index.toInt())
operator fun <T> MutableList<T>.set(index: UInt64, element: T) = set(index.toInt(), element)
