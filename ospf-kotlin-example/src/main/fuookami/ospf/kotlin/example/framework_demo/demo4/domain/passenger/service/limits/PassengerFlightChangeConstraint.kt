@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.utils.functional.get

class PassengerFlightChangeConstraint(
    private val timeWindow: TimeWindow,
    private val passengers: List<FlightPassenger>,
    private val time: TaskTime,
    private val change: PassengerChange,
    override val name: String = "passenger_flight_change_constraint"
) : CGPipeline {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        for (passenger in passengers) {
            if (passenger.prev != null) {
                for (toFlight in change.toFlights[passenger.flight] ?: emptyList()) {
                    val earliestStartTime = LinearPolynomial(
                        listOf(
                            LinearMonomial(Flt64.one, time.estimateEndTime[passenger.prev.flight]),
                            LinearMonomial(Flt64(-1.0), time.estimateStartTime[passenger.flight])
                        ),
                        timeWindow.valueOf(passenger.prev.flight.arr.passengerTransferTime)
                    )
                    val estCondition = IfFunction(
                        condition = -earliestStartTime,
                        name = "${passenger}_${toFlight}_est"
                    )
                    when (val result = model.add(estCondition.helperVariables)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    when (val result = model.add(LinearFunctionSymbolAdapter(estCondition, IntoValue.Flt64))) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }

                    for (cls in PassengerClass.entries) {
                        val rhs = LinearPolynomial(
                            listOf(LinearMonomial(passenger.amount.toFlt64(), estCondition.resultVar)),
                            Flt64.zero
                        )
                        when (val result = model.addConstraint(
                            relation = change.passengerFlightChange[passenger, toFlight, cls]!! leq rhs,
                            name = "${name}_${passenger}_${toFlight}"
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
            }

            val next = passengers.find { it.prev == passenger }
            if (next != null) {
                for (toFlight in change.toFlights[passenger.flight] ?: emptyList()) {
                    val lastestEndTime = LinearPolynomial(
                        listOf(
                            LinearMonomial(Flt64.one, time.estimateEndTime[passenger.flight]),
                            LinearMonomial(Flt64(-1.0), time.estimateStartTime[next.flight])
                        ),
                        timeWindow.valueOf(next.flight.dep.passengerTransferTime)
                    )
                    val eetCondition = IfFunction(
                        condition = -lastestEndTime,
                        name = "${passenger}_${toFlight}_eet"
                    )
                    when (val result = model.add(eetCondition.helperVariables)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    when (val result = model.add(LinearFunctionSymbolAdapter(eetCondition, IntoValue.Flt64))) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }

                    for (cls in PassengerClass.entries) {
                        val rhs = LinearPolynomial(
                            listOf(LinearMonomial(passenger.amount.toFlt64(), eetCondition.resultVar)),
                            Flt64.zero
                        )
                        when (val result = model.addConstraint(
                            relation = change.passengerFlightChange[passenger, toFlight, cls]!! leq rhs,
                            name = "${name}_${passenger}_${toFlight}"
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
            }
        }

        return ok
    }
}











