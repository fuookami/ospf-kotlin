package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Cumulative load weight limits along the fuselage in forward or aft direction.
 * 沿机身向前或向后的累积载荷重量限制。
 *
 * @property limitZones The list of cumulative load weight limit zones. / 累积载荷重量限制区域列表
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
     * @property position The cargo position. / 货物位置
     * @property weight The weight coefficient for this part. / 此部分的重量系数
    */
    data class Part(
        val position: Position,
        val weight: Flt64
    )

    /**
     * A checkpoint for cumulative load weight verification.
     * 累积载荷重量验证的检查点。
     *
     * @property zone The parent limit zone. / 父限制区域
     * @property toArm The arm up to which load is accumulated. / 累积载荷的力臂上限
     * @property maxSum The maximum allowed cumulative sum. / 最大允许累积和
     * @property parts The parts contributing to this checkpoint. / 贡献此检查点的部分
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
     * @property toArm The arm position. / 力臂位置
     * @property maxSum The maximum cumulative sum at this arm. / 此力臂处的最大累积和
    */
    data class Point(
        val toArm: Quantity<Flt64>,
        val maxSum: Quantity<Flt64>,
    )

    /**
     * A zone defining cumulative load weight limits in a direction.
     * 定义某个方向上累积载荷重量限制的区域。
     *
     * @property direction The direction of accumulation (FWD or AFT). / 累积方向（前向或后向）
     * @property name The name of the limit zone. / 限制区域名称
     * @property fromArm The starting arm for accumulation. / 累积的起始力臂
     * @property points The limit curve points. / 限制曲线点
     * @property checkpoints The checkpoints within this zone. / 此区域内的检查点
    */
    data class LimitZone(
        val direction: Direction,
        val name: String,
        val fromArm: Quantity<Flt64>,
        val points: List<Point>,
        val checkpoints: List<CheckPoint>
    ) {
        companion object {
            operator fun invoke(
                direction: Direction,
                fromArm: Quantity<Flt64>,
                points: List<Point>,
                positions: List<Position>
            ): LimitZone {
                TODO("not implemented yet")
            }
        }
    }

    val checkPoints by lazy {
        limitZones.flatMap { it.checkpoints }
    }
}
