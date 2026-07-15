@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 乘客域聚合（组合取消、变更和数量跟踪）。Aggregation for passenger domain combining cancel, change, and amount tracking.
 *
 * @property timeWindow Time window for scheduling / 调度时间窗口
 * @property flights List of flight tasks / 航班任务列表
 * @property passengers List of flight-passenger associations / 航班乘客关联列表
 * @property time Task time estimation / 任务时间估算
 * @property capacity Flight capacity / 航班容量
*/
class Aggregation(
    val timeWindow: TimeWindow<*>,
    val flights: List<FlightTask>,
    val passengers: List<FlightPassenger>,
    val time: TaskTime,
    val capacity: FlightCapacity
) {
    val cancel = PassengerCancel(
        passengers = passengers
    )

    val change = PassengerChange(
        flights = flights,
        passengers = passengers
    )

    val amount = PassengerAmount(
        flights = flights,
        passengers = passengers.groupBy { it.flight },
        cancel = cancel,
        change = change
    )

    /**
     * 将取消、变更和数量组件注册到模型中。Registers cancel, change, and amount components with the model.
     *
     * @param model The linear meta model to register with / 要注册的线性元模型
     * @return Registration result / 注册结果
    */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = cancel.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = change.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        when (val result = amount.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}
