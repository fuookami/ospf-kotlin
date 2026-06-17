package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 每个机身区域的最大允许载荷重量。Maximum allowable load weight per fuselage zone.
 *
 * @property private val aircraftModel 参数。
 * @property limitZones 参数。
 * @property private val load 参数。
 */
class MaxZoneLoadWeight(
    private val aircraftModel: AircraftModel,
    val limitZones: List<LimitZone>,
    private val load: Load
) {
    data class LimitPart(
        val position: Position,
        val weight: Flt64
    )

    data class LimitZone(
        val name: String,
        val liferaft: Liferaft?,
        val maxLoadWeight: Quantity<Flt64>,
        val parts: List<LimitPart>
    )
}
