@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

class PassengerChange(
    private val flights: List<FlightTask>,
    private val passengers: List<FlightPassenger>,
    private val withFlightChange: Boolean = false
) {
    val toFlights: Map<FlightTask, List<FlightTask>> by lazy {
        val toFlights = HashMap<FlightTask, List<FlightTask>>()
        for (flight in passengers.map { it.flight }.distinct()) {
            toFlights[flight] = flights.filter {
                it != flight && it.dep == flight.dep && it.arr == flight.arr
            }
        }
        toFlights
    }

    lateinit var passengerClassChange: Map<FlightPassenger, Map<PassengerClass, UIntVar>>
    lateinit var passengerFlightChange: Map<FlightPassenger, Map<FlightTask, Map<PassengerClass, UIntVar>>>

    fun register(model: AbstractLinearMetaModelF64): Try {
        if (!::passengerClassChange.isInitialized) {
            passengerClassChange = passengers.associateWith { passenger ->
                PassengerClass.entries.filter { it != passenger.cls }.associateWith { cls ->
                    val variable = UIntVar("passenger_class_change_${passenger}_${cls}")
                    variable.range.leq(passenger.amount)
                    variable
                }
            }
        }
        when (val result = model.add(passengerClassChange)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (withFlightChange) {
            if (!::passengerFlightChange.isInitialized) {
                passengerFlightChange = passengers.associateWith { passenger ->
                    (toFlights[passenger.flight] ?: emptyList()).associateWith { toFlight ->
                        PassengerClass.entries.associateWith { cls ->
                            val variable = UIntVar("passenger_class_change_${passenger}_${toFlight}_${cls}")
                            variable.range.leq(passenger.amount)
                            variable
                        }
                    }
                }
            }
            when (val result = model.add(passengerFlightChange)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}












