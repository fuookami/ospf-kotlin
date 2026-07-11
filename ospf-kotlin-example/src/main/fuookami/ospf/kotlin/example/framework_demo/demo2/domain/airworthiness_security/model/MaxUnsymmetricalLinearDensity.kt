package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Limits on unsymmetrical linear density between left and right sides of the aircraft.
 * 飞机左右两侧不对称线性密度的限制。
 *
 * @property limitZones The list of unsymmetrical linear density limit zones. / 不对称线性密度限制区域列表
*/
class MaxUnsymmetricalLinearDensity(
    val limitZones: List<LimitZone>
) {

    /**
     * A limit on unsymmetrical linear density.
     * 不对称线性密度限制。
     *
     * @property leftCoefficient Coefficient for the left side, nullable. / 左侧系数，可为空
     * @property rightCoefficient Coefficient for the right side, nullable. / 右侧系数，可为空
     * @property maxSum The maximum allowed sum. / 最大允许和
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
     * @property lhs The left-hand side value. / 左侧值
     * @property rhs The right-hand side value. / 右侧值
    */
    data class LimitPoint(
        val lhs: Quantity<Flt64>,
        val rhs: Quantity<Flt64>
    )

    /**
     * A limit line within an unsymmetrical linear density zone.
     * 不对称线性密度区域内的限制线。
     *
     * @property zone The parent limit zone. / 父限制区域
     * @property arm The arm position of this line. / 此线的力臂位置
     * @property positions The positions covered by this line. / 此线覆盖的位置
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
     * @property name The name of the limit zone. / 限制区域名称
     * @property frontArm The front arm of the zone. / 区域的前力臂
     * @property backArm The back arm of the zone. / 区域的后力臂
     * @property lines The limit lines within this zone. / 此区域内的限制线
     * @property limits The limits within this zone. / 此区域内的限制
    */
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
