package fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** Solver 边界的数值可行性容差，不属于领域物理量。 / Solver-boundary feasibility tolerances; not domain quantities.
 *
 * @property absoluteTolerance 归一化 solver 值的绝对容差 / Absolute tolerance in normalized solver values
 * @property relativeTolerance 按比较值规模放大的相对容差 / Relative tolerance scaled by the compared values
 */
data class GeneralizedFlowSolverTolerance(
    val absoluteTolerance: Flt64 = Flt64(1e-7),
    val relativeTolerance: Flt64 = Flt64(1e-7)
) {
    internal fun isValid(): Boolean {
        return absoluteTolerance.isFinite() && relativeTolerance.isFinite() &&
            absoluteTolerance >= Flt64.zero && relativeTolerance >= Flt64.zero
    }

    internal fun forValues(values: List<Flt64>): Flt64? {
        if (values.any { !it.isFinite() }) {
            return null
        }

        var scale = Flt64.zero
        for (value in values) {
            val magnitude = value.abs()
            if (magnitude > scale) {
                scale = magnitude
            }
        }
        val tolerance = absoluteTolerance + relativeTolerance * scale
        return tolerance.takeIf { it.isFinite() }
    }
}
