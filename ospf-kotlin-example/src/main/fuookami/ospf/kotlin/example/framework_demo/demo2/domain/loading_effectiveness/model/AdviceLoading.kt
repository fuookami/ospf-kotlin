package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

class AdviceLoading(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    private val load: Load
) {
    lateinit var amountSlack: LinearIntermediateSymbols1Flt64
    lateinit var weightSlack: LinearIntermediateSymbols1Flt64

    fun register(
        model: AbstractLinearMetaModelF64
    ): Try {
        if (!::amountSlack.isInitialized) {
            amountSlack = LinearIntermediateSymbols1Flt64("advice_amount_slack", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.ala != null) {
                    SlackFunction(
                        x = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, load.loadAmount[j])),
                            constant = Flt64.zero
                        ),
                        threshold = position.ala!!.toFlt64(),
                        withPositive = true,
                        name = "advice_amount_slack_${position}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "advice_amount_slack_${position}",
                    )
                }
            }
        }
        when (val result = model.add(amountSlack)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::weightSlack.isInitialized) {
            weightSlack = LinearIntermediateSymbols1Flt64("advice_weight_slack", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.alw != null) {
                    SlackFunction(
                        x = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)),
                            constant = Flt64.zero
                        ),
                        threshold = position.alw!!.to(aircraftModel.weightUnit)!!.value,
                        withPositive = true,
                        name = "advice_weight_slack_${position}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "advice_weight_slack_${position}",
                    )
                }
            }
        }
        when (val result = model.add(weightSlack)) {
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













