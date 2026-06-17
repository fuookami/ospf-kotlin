package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 聚合推荐重量均衡数据（包括优先级预约和装载分配）。Aggregates recommended weight equalization data including priority appointments and stowage assignments.
 *
 * @property internal val aircraftModel 参数。
 * @property internal val items 参数。
 * @property internal val positions 参数。
 * @property internal val appointment 参数。
 * @property internal val stowage 参数。
 * @property internal val load 参数。
 * @property payload 参数。
 * @property totalWeight 参数。
 * @property ballast 参数。
 */
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
