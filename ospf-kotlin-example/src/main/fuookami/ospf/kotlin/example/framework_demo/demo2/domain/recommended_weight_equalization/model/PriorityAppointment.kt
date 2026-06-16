package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.model

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Maps cargo priorities to sets of loading order depths for weight equalization appointments. */
data class PriorityAppointment(
    private val appointment: HashMap<CargoPriority, Set<UInt8>>
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>,
            appointment: Map<Item, Position>,
            payload: Payload,
            totalWeight: TotalWeight,
            ballast: Ballast
        ): PriorityAppointment {
            TODO("not implemented yet")
        }
    }

    operator fun invoke(priority: CargoPriority, position: Position): Boolean {
        return appointment[priority]?.contains(position.loadingOrder.precDepth) == true
    }
}
