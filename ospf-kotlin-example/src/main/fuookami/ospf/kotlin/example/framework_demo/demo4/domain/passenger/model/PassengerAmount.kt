@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.multiarray.*

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*

/**
 * Tracks passenger amount expressions per flight and class,
 * accounting for cancellations and class/flight changes.
 */
class PassengerAmount(
    private val flights: List<FlightTask>,
    private val passengers: Map<FlightTask, List<FlightPassenger>>,
    private val cancel: PassengerCancel,
    private val change: PassengerChange
) {
    lateinit var passengerAmount: Map<FlightTask, Map<PassengerClass, LinearIntermediateSymbol<Flt64>>>

    /** Registers passenger amount expressions with the model. */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::passengerAmount.isInitialized) {
            passengerAmount = flights.associateWith { flight ->
                PassengerClass.entries.associateWith { cls ->
                    val poly = MutableLinearPolynomial()
                    for (passenger in (passengers[flight] ?: emptyList())) {
                        if (passenger.cls == cls) {
                            poly += passenger.amount.toFlt64()
                        }
                        poly -= LinearMonomial(Flt64.one, cancel.passengerCancel[passenger])
                        if (passenger.cls == cls) {
                            poly -= sum(change.passengerClassChange[passenger, _a])
                            poly -= sum(change.passengerFlightChange[passenger, _a, _a])
                        } else {
                            poly += LinearMonomial(Flt64.one, change.passengerClassChange[passenger, cls]!!)
                        }
                    }
                    for (passenger in passengers.values.flatten()) {
                        val toFlights = change.toFlights[passenger.flight] ?: emptyList()
                        if (toFlights.contains(flight)) {
                            poly += LinearMonomial(Flt64.one, change.passengerFlightChange[passenger, flight, cls]!!)
                        }
                    }
                    LinearExpressionSymbol(
                        poly,
                        name = "passenger_amount_${flight}_${cls}"
                    )
                }
            }
        }
        when (val result = model.add(passengerAmount)) {
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
