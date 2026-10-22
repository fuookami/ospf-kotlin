package fuookami.ospf.kotlin.framework.bpp.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.*

/** 求解器数值边界；所有有量纲值先归一到显式单位。 / Solver boundary with explicit unit normalization.
 * @param V 领域数值 / Domain number
 */
interface PackingValueAdapter<V : FloatingNumber<V>> {
    /** 转为求解器数值。 / Convert to solver value.
     * @param value 领域值 / Domain value
     * @return 求解器值 / Solver value
     */
    fun toSolver(value: V): Flt64

    /** 转回领域数值。 / Convert to domain value.
     * @param value 求解器值 / Solver value
     * @return 领域值 / Domain value
     */
    fun fromSolver(value: Flt64): V

    /** 归一量值。 / Normalize a quantity.
     * @param value 量值 / Quantity
     * @param unit 目标单位 / Target unit
     * @return 有限数值或失败 / Finite value or failure
     */
    fun quantity(value: Quantity<V>, unit: PhysicalUnit): Ret<Flt64> {
        val converted = Quantity(toSolver(value.value), value.unit).to(unit)
            ?: return Failed(ErrorCode.IllegalArgument, "量纲不兼容 / Incompatible quantity dimensions")
        return finite(converted.value)
    }
}

/** Flt64 适配器。 / Flt64 adapter. */
data object Flt64PackingValueAdapter : PackingValueAdapter<Flt64> {
    override fun toSolver(value: Flt64) = value
    override fun fromSolver(value: Flt64) = value
}

/** FltX 适配器。 / FltX adapter. */
data object FltXPackingValueAdapter : PackingValueAdapter<FltX> {
    override fun toSolver(value: FltX) = value.toFlt64()
    override fun fromSolver(value: Flt64) = FltX(value.toDouble())
}

/** 验证有限值。 / Validate a finite value.
 * @param value 待验证值 / Value to validate
 * @return 有限值或失败 / Finite value or failure
 */
fun finite(value: Flt64): Ret<Flt64> {
    return if (value.toDouble().isFinite()) Ok(value)
    else Failed(ErrorCode.IllegalArgument, "数值必须有限 / Value must be finite")
}
