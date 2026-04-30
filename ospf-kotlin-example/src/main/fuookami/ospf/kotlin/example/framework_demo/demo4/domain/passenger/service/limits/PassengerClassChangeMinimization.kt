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
import fuookami.ospf.kotlin.utils.functional.get

class PassengerClassChangeMinimization(
    private val passengers: List<FlightPassenger>,
    private val change: PassengerChange,
    private val coefficient: (FlightPassenger, PassengerClass) -> Flt64 = { _, _ -> Flt64.one },
    override val name: String = "passenger_class_change_minimization"
) : CGPipeline {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        val poly = MutableLinearPolynomial()
        for (passenger in passengers) {
            for (cls in PassengerClass.entries) {
                if (passenger.cls == cls) {
                    continue
                }

                poly += LinearMonomial(
                    coefficient(passenger, cls),
                    change.passengerClassChange[passenger, cls]!!
                )
            }
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "passenger class change"
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












