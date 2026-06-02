package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

/**
 * Quantity<V> 算术策略 / Arithmetic strategy for Quantity<V>
 *
 * 提供泛型数值下的 Quantity 加减运算，避免在领域模型中直接依赖 Flt64/FltX / Provides Quantity addition/subtraction under generic numerics, avoiding direct Flt64/FltX dependency in domain models
 *
 * @param V 数值类型 / Numeric value type
 */
interface QuantityArithmetic<V : RealNumber<V>> {
    fun add(a: Quantity<V>, b: Quantity<V>): Quantity<V>
    fun subtract(a: Quantity<V>, b: Quantity<V>): Quantity<V>
    fun zero(unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit): Quantity<V>

    fun isPositive(q: Quantity<V>): Boolean {
        return when (q.value partialOrd q.value.constants.zero) {
            is Order.Greater -> true
            else -> false
        }
    }

    fun isNonNegative(q: Quantity<V>): Boolean {
        return when (q.value partialOrd q.value.constants.zero) {
            is Order.Less -> false
            else -> true
        }
    }

    fun isZero(q: Quantity<V>): Boolean {
        return q.value partialOrd q.value.constants.zero == Order.Equal
    }
}

object Flt64QuantityArithmetic : QuantityArithmetic<Flt64> {
    override fun add(a: Quantity<Flt64>, b: Quantity<Flt64>): Quantity<Flt64> = a + b
    override fun subtract(a: Quantity<Flt64>, b: Quantity<Flt64>): Quantity<Flt64> = a - b
    override fun zero(unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit): Quantity<Flt64> =
        Quantity(Flt64.zero, unit)
}

object FltXQuantityArithmetic : QuantityArithmetic<FltX> {
    override fun add(a: Quantity<FltX>, b: Quantity<FltX>): Quantity<FltX> = a + b
    override fun subtract(a: Quantity<FltX>, b: Quantity<FltX>): Quantity<FltX> = a - b
    override fun zero(unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit): Quantity<FltX> =
        Quantity(FltX.zero, unit)
}

object DefaultQuantityArithmetic {
    @Deprecated(
        message = "Cannot reliably determine V at runtime; use resolveFor(sample) or inject explicitly",
        level = DeprecationLevel.ERROR
    )
    fun <V : RealNumber<V>> resolve(): QuantityArithmetic<V> {
        @Suppress("UNCHECKED_CAST")
        return Flt64QuantityArithmetic as QuantityArithmetic<V>
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : RealNumber<V>> resolveFor(sample: V): QuantityArithmetic<V> {
        return when (sample) {
            is Flt64 -> Flt64QuantityArithmetic as QuantityArithmetic<V>
            is FltX -> FltXQuantityArithmetic as QuantityArithmetic<V>
            else -> throw IllegalArgumentException("Unsupported RealNumber type: ${sample::class}")
        }
    }
}
