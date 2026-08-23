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
 * @property limitZones 区域载荷重量限制列表 / The list of zone load weight limits.
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
     * @property position 货物位置 / The cargo position.
     * @property weight 此部分的重量系数 / The weight coefficient for this part.
    */
    data class LimitPart(
        val position: Position,
        val weight: Flt64
    )

    /**
     * A zone with maximum load weight limits.
     * 具有最大载荷重量限制的区域。
     *
     * @property name 限制区域名称 / The name of the limit zone.
     * @property liferaft 此区域内的救生筏，可为空 / The liferaft in this zone, nullable.
     * @property maxLoadWeight 最大允许载荷重量 / The maximum allowed load weight.
     * @property parts 贡献此区域的部分 / The parts contributing to this zone.
    */
    data class LimitZone(
        val name: String,
        val liferaft: Liferaft?,
        val maxLoadWeight: Quantity<Flt64>,
        val parts: List<LimitPart>
    )
}
