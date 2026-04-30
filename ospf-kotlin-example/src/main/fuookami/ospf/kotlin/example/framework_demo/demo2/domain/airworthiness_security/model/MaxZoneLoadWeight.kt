package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

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

