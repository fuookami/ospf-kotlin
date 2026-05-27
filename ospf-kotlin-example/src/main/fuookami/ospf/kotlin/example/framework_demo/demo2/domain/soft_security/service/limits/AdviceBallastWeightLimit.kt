package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.exampleThresholdSlack
import fuookami.ospf.kotlin.example.flt64Linear
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class AdviceBallastWeightLimit(
    private val aircraftModel: AircraftModel,
    private val ballast: Ballast,
    private val coefficient: () -> Flt64 = { Flt64.one },
    override val name: String = "advice_ballast_weight_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        if (ballast.adviceBallastWeight != null) {
            val slack = exampleThresholdSlack(
                x = flt64Linear(ballast.ballastWeight.value),
                threshold = ballast.adviceBallastWeight.to(aircraftModel.weightUnit)!!.value,
                withNegative = true,
                withPositive = false,
                name = "advice_ballast_weight_slack"
            )
            when (val result = model.add(slack)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
            when (val result = model.minimize(
                coefficient() * slack,
                name = "advice ballast weight"
            )) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}



















