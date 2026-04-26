package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.model.*

class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val items: List<Item>,
    internal val positions: List<Position>,
    internal val appointment: Map<Item, Position>,
    internal val stowage: Stowage,
    internal val load: Load,
    payload: Payload,
    totalWeight: TotalWeight,
    ballast: Ballast
) {
    val priorityAppointment = PriorityAppointment(
        items = items,
        positions = positions,
        appointment = appointment,
        payload = payload,
        totalWeight = totalWeight,
        ballast = ballast
    )
}
