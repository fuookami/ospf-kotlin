package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class AdviceBallastWeightLimit(
    private val aircraftModel: AircraftModel,
    private val ballast: Ballast,
    private val coefficient: () -> Flt64 = { Flt64.one },
    override val name: String = "advice_ballast_weight_limit",
) : Pipeline<AbstractLinearMetaModelF64> {
    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        if (ballast.adviceBallastWeight != null) {
            val slack = SlackFunction(
                x = LinearPolynomial(ballast.ballastWeight.value),
                threshold = ballast.adviceBallastWeight.to(aircraftModel.weightUnit)!!.value,
                withPositive = false,
                name = "advice_ballast_weight_slack"
            )
            when (val result = model.minimize(
                coefficient() * slack,
                name = "advice ballast weight"
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

        return ok
    }
}




















