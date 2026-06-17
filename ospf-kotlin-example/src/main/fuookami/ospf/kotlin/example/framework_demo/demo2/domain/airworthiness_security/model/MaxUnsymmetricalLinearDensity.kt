package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * 飞机左右两侧不对称线性密度的限制。Limits on unsymmetrical linear density between left and right sides of the aircraft.
 *
 * @property limitZones 参数。
 */
class MaxUnsymmetricalLinearDensity(
    val limitZones: List<LimitZone>
) {
    data class Limit(
        val leftCoefficient: Flt64?,
        val rightCoefficient: Flt64?,
        val maxSum: Quantity<Flt64>
    )

    data class LimitPoint(
        val lhs: Quantity<Flt64>,
        val rhs: Quantity<Flt64>
    )

    data class LimitLine(
        val zone: LimitZone,
        val arm: Quantity<Flt64>,
        val positions: List<Position>
    )

    data class LimitZone(
        val name: String,
        val frontArm: Quantity<Flt64>,
        val backArm: Quantity<Flt64>,
        val lines: List<LimitLine>,
        val limits: List<Limit>
    ) {
        companion object {
            operator fun invoke(
                name: String,
                frontArm: Quantity<Flt64>,
                backArm: Quantity<Flt64>,
                points: List<LimitPoint>,
                positions: List<Position>
            ): LimitZone {
                TODO("not implemented yet")
            }
        }
    }
}
