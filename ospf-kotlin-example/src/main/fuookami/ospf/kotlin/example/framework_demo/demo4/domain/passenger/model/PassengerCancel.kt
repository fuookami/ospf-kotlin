@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class PassengerCancel(
    private val passengers: List<FlightPassenger>
) {
    lateinit var passengerCancel: UIntVariable1

    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::passengerCancel.isInitialized) {
            passengerCancel = UIntVariable1(
                "passenger_cancel",
                Shape1(passengers.size)
            )
            for (passenger in passengers) {
                passengerCancel[passenger].range.leq(passenger.amount)
            }
        }
        when (val result = model.add(passengerCancel)) {
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












