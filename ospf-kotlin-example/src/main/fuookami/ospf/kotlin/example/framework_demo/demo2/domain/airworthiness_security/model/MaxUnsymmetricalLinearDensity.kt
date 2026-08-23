package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Limits on unsymmetrical linear density between left and right sides of the aircraft.
 * 飞机左右两侧不对称线性密度的限制。
 *
 * @property limitZones 不对称线性密度限制区域列表 / The list of unsymmetrical linear density limit zones.
*/
class MaxUnsymmetricalLinearDensity(
    val limitZones: List<LimitZone>
) {

    /**
     * A limit on unsymmetrical linear density.
     * 不对称线性密度限制。
     *
     * @property leftCoefficient 左侧系数，可为空 / Coefficient for the left side, nullable.
     * @property rightCoefficient 右侧系数，可为空 / Coefficient for the right side, nullable.
     * @property maxSum 最大允许和 / The maximum allowed sum.
    */
    data class Limit(
        val leftCoefficient: Flt64?,
        val rightCoefficient: Flt64?,
        val maxSum: Quantity<Flt64>
    )

    /**
     * A point defining the unsymmetrical linear density limit.
     * 定义不对称线性密度限制的点。
     *
     * @property lhs 左侧值 / The left-hand side value.
     * @property rhs 右侧值 / The right-hand side value.
    */
    data class LimitPoint(
        val lhs: Quantity<Flt64>,
        val rhs: Quantity<Flt64>
    )

    /**
     * A limit line within an unsymmetrical linear density zone.
     * 不对称线性密度区域内的限制线。
     *
     * @property zone 父限制区域 / The parent limit zone.
     * @property arm 此线的力臂位置 / The arm position of this line.
     * @property positions 此线覆盖的位置 / The positions covered by this line.
    */
    data class LimitLine(
        val zone: LimitZone,
        val arm: Quantity<Flt64>,
        val positions: List<Position>
    )

    /**
     * A zone defining unsymmetrical linear density limits.
     * 定义不对称线性密度限制的区域。
     *
     * @property name 限制区域名称 / The name of the limit zone.
     * @property frontArm 区域的前力臂 / The front arm of the zone.
     * @property backArm 区域的后力臂 / The back arm of the zone.
     * @property lines 此区域内的限制线 / The limit lines within this zone.
     * @property limits 此区域内的限制 / The limits within this zone.
    */
    data class LimitZone(
        val name: String,
        val frontArm: Quantity<Flt64>,
        val backArm: Quantity<Flt64>,
        var lines: List<LimitLine>,
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
                val zone = LimitZone(
                    name = name,
                    frontArm = frontArm,
                    backArm = backArm,
                    lines = emptyList(),
                    limits = points.flatMap { point ->
                        listOf(
                            Limit(
                                leftCoefficient = Flt64.one,
                                rightCoefficient = -Flt64.one,
                                maxSum = point.lhs
                            ),
                            Limit(
                                leftCoefficient = -Flt64.one,
                                rightCoefficient = Flt64.one,
                                maxSum = point.rhs
                            )
                        )
                    }
                )
                val zonePositions = positions.filter { position ->
                    position.coordinate.withIntersectionWith(frontArm, backArm)
                }
                val arms = buildList {
                    add(frontArm)
                    add(backArm)
                    zonePositions.forEach { position ->
                        add(position.coordinate.frontArm)
                        add(position.coordinate.backArm)
                    }
                }
                    .distinctBy { it.to(frontArm.unit)!!.value.toDouble() }
                    .sortedBy { it.to(frontArm.unit)!!.value.toDouble() }
                    .filter { arm ->
                        if ((backArm ls frontArm)!!) {
                            (arm geq backArm)!! && (arm leq frontArm)!!
                        } else {
                            (arm geq frontArm)!! && (arm leq backArm)!!
                        }
                    }

                zone.lines = arms.mapNotNull { arm ->
                    val leftPositions = zonePositions.filter { position ->
                        position.coordinate.on(arm) && position.coordinate.onLeft
                    }
                    val rightPositions = zonePositions.filter { position ->
                        position.coordinate.on(arm) && position.coordinate.onRight
                    }
                    if (leftPositions.size == 1 && rightPositions.size == 1) {
                        LimitLine(
                            zone = zone,
                            arm = arm,
                            positions = listOf(leftPositions.single(), rightPositions.single())
                        )
                    } else {
                        null
                    }
                }
                return zone
            }
        }
    }
}
