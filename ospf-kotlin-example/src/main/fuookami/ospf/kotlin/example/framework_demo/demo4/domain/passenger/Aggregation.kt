@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class Aggregation(
    val timeWindow: TimeWindow,
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












