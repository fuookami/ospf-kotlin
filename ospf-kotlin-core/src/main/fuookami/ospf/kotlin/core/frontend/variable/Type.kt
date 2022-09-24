package  fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

sealed interface VariableTypeInterface<T> where T : RealNumber<T>, T : NumberField<T> {
    val constants: RealNumberConstants<T>
    val minimum: T
    val maximum: T

    fun isBinaryType() = false
    fun isIntegerType() = false
    fun isNotBinaryIntegerType() = isBinaryType() && !isIntegerType()
}

sealed interface IntegerVariableType<T : IntegerNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.minimum
    override val maximum get() = constants.maximum

    override fun isIntegerType() = true
}

sealed interface UIntegerVariableType<T : UIntegerNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.zero
    override val maximum get() = constants.maximum

    override fun isIntegerType() = true
}

sealed interface ContinuesVariableType<T : FloatingNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.minimum
    override val maximum get() = constants.maximum
}

sealed interface UContinuesVariableType<T : FloatingNumber<T>> : VariableTypeInterface<T> {
    override val minimum get() = constants.zero
    override val maximum get() = constants.maximum
}

sealed class VariableType<T>(
    override val constants: RealNumberConstants<T>
) : VariableTypeInterface<T> where T : RealNumber<T>, T : NumberField<T>

object Binary : VariableType<UInt8>(UInt8), UIntegerVariableType<UInt8> {
    override val maximum get() = constants.one

    override fun isBinaryType() = true

    override fun toString(): String = "Binary"
}

object Ternary : VariableType<UInt8>(UInt8), UIntegerVariableType<UInt8> {
    override val maximum get() = constants.two

    override fun toString(): String = "Ternary"
}

object BalancedTernary : VariableType<Int8>(Int8), IntegerVariableType<Int8> {
    override val minimum get() = -constants.one
    override val maximum get() = constants.one

    override fun toString(): String = "BalancedTernary"
}

object Percentage : VariableType<Flt64>(Flt64), UContinuesVariableType<Flt64> {
    override val maximum get() = constants.one

    override fun toString(): String = "Percentage"
}

object Integer : VariableType<Int64>(Int64), IntegerVariableType<Int64> {
    override fun toString(): String = "Integer"
}

object UInteger : VariableType<UInt64>(UInt64), UIntegerVariableType<UInt64> {
    override fun toString(): String = "UInteger"
}

object Continues : VariableType<Flt64>(Flt64), ContinuesVariableType<Flt64> {
    override fun toString(): String = "Continues"
}

object UContinues : VariableType<Flt64>(Flt64), UContinuesVariableType<Flt64> {
    override fun toString(): String = "UContinues"
}
