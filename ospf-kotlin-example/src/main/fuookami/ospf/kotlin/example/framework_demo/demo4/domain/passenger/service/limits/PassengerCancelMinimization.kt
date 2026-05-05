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

class PassengerCancelMinimization(
    private val passengers: List<FlightPassenger>,
    private val cancel: PassengerCancel,
    private val coefficient: (FlightPassenger) -> Flt64 = { _ -> Flt64.one },
    override val name: String = "passenger_cancel_minimization"
) : CGPipeline {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val poly = MutableLinearPolynomial()
        for (passenger in passengers) {
            poly += LinearMonomial(coefficient(passenger), cancel.passengerCancel[passenger])
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "passenger cancel"
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












