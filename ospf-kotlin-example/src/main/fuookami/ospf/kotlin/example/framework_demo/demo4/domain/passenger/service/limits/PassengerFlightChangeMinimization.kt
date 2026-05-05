@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.passenger.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class PassengerFlightChangeMinimization(
    private val passengers: List<FlightPassenger>,
    private val change: PassengerChange,
    private val coefficient: (FlightTask, FlightTask, PassengerClass) -> Flt64 = { _, _, _ -> Flt64.one },
    override val name: String = "passenger_flight_change_minimization"
) : CGPipeline {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = MutableLinearPolynomial()
        for (passer in passengers) {
            for (toFlight in change.toFlights[passer.flight] ?: emptyList()) {
                for (cls in PassengerClass.entries) {
                    poly += LinearMonomial(
                        coefficient(passer.flight, toFlight, cls),
                        change.passengerFlightChange[passer, toFlight, cls]!!
                    )
                }
            }
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "passenger flight change"
        )) {
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












