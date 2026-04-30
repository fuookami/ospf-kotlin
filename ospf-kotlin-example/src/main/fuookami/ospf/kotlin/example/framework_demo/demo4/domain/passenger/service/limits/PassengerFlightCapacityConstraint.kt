@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.utils.functional.get

class PassengerFlightCapacityConstraint(
    private val flights: List<FlightTask>,
    private val amount: PassengerAmount,
    private val capacity: FlightCapacity,
    override val name: String = "passenger_flight_capacity_constraint"
) : CGPipeline {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        for (flight in flights) {
            for (cls in PassengerClass.entries) {
                when (val result = model.addConstraint(
                    amount.passengerAmount[flight, cls]!! leq capacity.passenger[flight, cls]!!,
                    name = "passenger_flight_capacity_constraint_${flight}_${cls}"
                )) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
                }
            }
        }

        return ok
    }
}












