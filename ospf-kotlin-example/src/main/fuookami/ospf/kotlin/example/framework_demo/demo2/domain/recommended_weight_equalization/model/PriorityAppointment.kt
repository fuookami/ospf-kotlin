package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * Maps cargo priorities to sets of loading order depths for weight equalization appointments.
 * 将货物优先级映射到装载顺序深度集合用于重量均衡预约。
 *
 * @property appointment 从货物优先级到装载顺序深度集合的映射 / The mapping from cargo priority to loading order depth sets
*/
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
            ballast: Ballast?
        ): PriorityAppointment {
            val availableDepths = positions
                .filter { it.status.available }
                .map { it.loadingOrder.precDepth }
                .toSet()
            val appointedDepths = appointment
                .filter { (item, position) -> item in items && position in positions && position.status.available }
                .entries
                .groupBy(
                    keySelector = { it.key.cargo.priority },
                    valueTransform = { it.value.loadingOrder.precDepth }
                )
                .mapValues { (_, depths) -> depths.toSet() }
            val byPriority = HashMap<CargoPriority, Set<UInt8>>()

            for (priority in items.map { it.cargo.priority }.distinct()) {
                byPriority[priority] = appointedDepths[priority]
                    ?.takeIf { it.isNotEmpty() }
                    ?: availableDepths
            }

            return PriorityAppointment(byPriority)
        }
    }

    operator fun invoke(priority: CargoPriority, position: Position): Boolean {
        return appointment[priority]?.contains(position.loadingOrder.precDepth) == true
    }
}
