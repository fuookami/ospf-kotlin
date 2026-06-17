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
 * @property timeWindow 参数。
 * @property flights 参数。
 * @property passengers 参数。
 * @property time 参数。
 * @property capacity 参数。
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
     * Registers cancel, change, and amount components with the model.
 *
     * @param model 参数。
     * @return 返回结果。
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
