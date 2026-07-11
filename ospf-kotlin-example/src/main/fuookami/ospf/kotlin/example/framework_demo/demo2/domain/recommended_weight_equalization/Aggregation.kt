package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Aggregates recommended weight equalization data including priority appointments and stowage assignments.
 * 聚合推荐重量均衡数据（包括优先级预约和装载分配）。
 *
 * @property aircraftModel The aircraft model for weight equalization / 用于重量均衡的飞机模型
 * @property items The list of cargo items / 货物项目列表
 * @property positions The list of stowage positions / 装载位置列表
 * @property appointment The item-to-position appointment mapping / 项目到位置的预约映射
 * @property stowage The stowage assignment matrix / 装载分配矩阵
 * @property load The load distribution data / 载荷分布数据
 * @property priorityAppointment The priority appointment model derived from the aggregation / 从聚合中派生的优先级预约模型
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
