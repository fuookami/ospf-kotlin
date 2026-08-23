package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Cumulative load weight limits along the fuselage in forward or aft direction.
 * 沿机身向前或向后的累积载荷重量限制。
 *
 * @property limitZones 累积载荷重量限制区域列表 / The list of cumulative load weight limit zones.
*/
class MaxCumulativeLoadWeight(
    val limitZones: List<LimitZone>,
) {

    /** The direction of cumulative load weight calculation. / 累积载荷重量计算的方向。 */
    enum class Direction {
        FWD,
        AFT
    }

    /**
     * A part of a checkpoint contributing to cumulative load weight.
     * 对累积载荷重量有贡献的检查点部分。
     *
     * @property position 货物位置 / The cargo position.
     * @property weight 此部分的重量系数 / The weight coefficient for this part.
    */
    data class Part(
        val position: Position,
        val weight: Flt64
    )

    /**
     * A checkpoint for cumulative load weight verification.
     * 累积载荷重量验证的检查点。
     *
     * @property zone 父限制区域 / The parent limit zone.
     * @property toArm 累积载荷的力臂上限 / The arm up to which load is accumulated.
     * @property maxSum 最大允许累积和 / The maximum allowed cumulative sum.
     * @property parts 贡献此检查点的部分 / The parts contributing to this checkpoint.
    */
    data class CheckPoint(
        val zone: LimitZone,
        val toArm: Quantity<Flt64>,
        val maxSum: Quantity<Flt64>,
        val parts: List<Part>
    )

    /**
     * A point defining the cumulative load weight limit curve.
     * 定义累积载荷重量限制曲线的点。
     *
     * @property toArm 力臂位置 / The arm position.
     * @property maxSum 此力臂处的最大累积和 / The maximum cumulative sum at this arm.
    */
    data class Point(
        val toArm: Quantity<Flt64>,
        val maxSum: Quantity<Flt64>,
    )

    /**
     * A zone defining cumulative load weight limits in a direction.
     * 定义某个方向上累积载荷重量限制的区域。
     *
     * @property direction 累积方向（前向或后向） / The direction of accumulation (FWD or AFT).
     * @property name 限制区域名称 / The name of the limit zone.
     * @property fromArm 累积的起始力臂 / The starting arm for accumulation.
     * @property points 限制曲线点 / The limit curve points.
     * @property checkpoints 此区域内的检查点 / The checkpoints within this zone.
    */
    data class LimitZone(
        val direction: Direction,
        val name: String,
        val fromArm: Quantity<Flt64>,
        val points: List<Point>,
        var checkpoints: List<CheckPoint>
    ) {
        companion object {
            operator fun invoke(
                direction: Direction,
                fromArm: Quantity<Flt64>,
                points: List<Point>,
                positions: List<Position>
            ): LimitZone {
                val unit = fromArm.unit
                val orderedPoints = points
                    .distinctBy { it.toArm.to(unit)!!.value.toDouble() }
                    .sortedBy { it.toArm.to(unit)!!.value.toDouble() }
                    .let { sorted ->
                        if (direction == Direction.AFT) {
                            sorted.reversed()
                        } else {
                            sorted
                        }
                    }
                val zone = LimitZone(
                    direction = direction,
                    name = "cumulative_${direction.name.lowercase()}_${fromArm.value}",
                    fromArm = fromArm,
                    points = orderedPoints,
                    checkpoints = emptyList()
                )

                zone.checkpoints = orderedPoints.map { point ->
                    CheckPoint(
                        zone = zone,
                        toArm = point.toArm,
                        maxSum = point.maxSum,
                        parts = contributingParts(
                            fromArm = fromArm,
                            toArm = point.toArm,
                            positions = positions
                        )
                    )
                }
                return zone
            }

            private fun contributingParts(
                fromArm: Quantity<Flt64>,
                toArm: Quantity<Flt64>,
                positions: List<Position>
            ): List<Part> {
                val unit = fromArm.unit
                val intervalStart = minOf(
                    fromArm.value.toDouble(),
                    toArm.to(unit)!!.value.toDouble()
                )
                val intervalEnd = maxOf(
                    fromArm.value.toDouble(),
                    toArm.to(unit)!!.value.toDouble()
                )
                if (intervalStart == intervalEnd) {
                    return emptyList()
                }

                return positions.mapNotNull { position ->
                    val frontArm = position.coordinate.frontArm.to(unit)!!.value.toDouble()
                    val backArm = position.coordinate.backArm.to(unit)!!.value.toDouble()
                    val length = backArm - frontArm
                    if (length <= 0.0) {
                        null
                    } else {
                        val overlap = minOf(backArm, intervalEnd) - maxOf(frontArm, intervalStart)
                        if (overlap > 0.0) {
                            Part(
                                position = position,
                                weight = Flt64(overlap / length)
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    val checkPoints by lazy {
        limitZones.flatMap { it.checkpoints }
    }
}
