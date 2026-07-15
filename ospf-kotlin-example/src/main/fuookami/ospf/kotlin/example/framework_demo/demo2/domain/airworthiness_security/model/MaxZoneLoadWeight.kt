package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Maximum allowable load weight per fuselage zone.
 * 每个机身区域的最大允许载荷重量。
 *
 * @property limitZones The list of zone load weight limits. / 区域载荷重量限制列表
*/
class MaxZoneLoadWeight(
    private val aircraftModel: AircraftModel,
    val limitZones: List<LimitZone>,
    private val load: Load
) {

    /**
     * A part contributing to zone load weight.
     * 对区域载荷重量有贡献的部分。
     *
     * @property position The cargo position. / 货物位置
     * @property weight The weight coefficient for this part. / 此部分的重量系数
    */
    data class LimitPart(
        val position: Position,
        val weight: Flt64
    )

    /**
     * A zone with maximum load weight limits.
     * 具有最大载荷重量限制的区域。
     *
     * @property name The name of the limit zone. / 限制区域名称
     * @property liferaft The liferaft in this zone, nullable. / 此区域内的救生筏，可为空
     * @property maxLoadWeight The maximum allowed load weight. / 最大允许载荷重量
     * @property parts The parts contributing to this zone. / 贡献此区域的部分
    */
    data class LimitZone(
        val name: String,
        val liferaft: Liferaft?,
        val maxLoadWeight: Quantity<Flt64>,
        val parts: List<LimitPart>
    )
}
