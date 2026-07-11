package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.exampleThresholdSlack
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Models advice loading constraints for amount and weight slack at each position.
 * 建模每个位置的建议装载数量和重量松弛约束。
 *
 * @property aircraftModel The aircraft model providing weight unit information. / 提供重量单位信息的飞行器模型
 * @property positions The list of stowage positions. / 配载位置列表
 * @property load The load model for cargo assignment. / 货物分配的装载模型
*/
class AdviceLoading(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    private val load: Load
) {
    lateinit var amountSlack: LinearIntermediateSymbols1<Flt64>
    lateinit var weightSlack: LinearIntermediateSymbols1<Flt64>

    /**
     * Registers the advice loading slack symbols into the optimization model.
     * 将建议装载松弛符号注册到优化模型中。
     *
     * @param model The linear meta model to register into. / 要注册到的线性元模型
     * @return The result of the registration operation. / 注册操作的结果
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::amountSlack.isInitialized) {
            amountSlack = LinearIntermediateSymbols1<Flt64>("advice_amount_slack", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.ala != null) {
                    exampleThresholdSlack(
                        x = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, load.loadAmount[j])),
                            constant = Flt64.zero
                        ),
                        threshold = position.ala!!.toFlt64(),
                        withNegative = false,
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::weightSlack.isInitialized) {
            weightSlack = LinearIntermediateSymbols1<Flt64>("advice_weight_slack", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.alw != null) {
                    exampleThresholdSlack(
                        x = LinearPolynomial(
                            monomials = listOf(LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)),
                            constant = Flt64.zero
                        ),
                        threshold = position.alw!!.to(aircraftModel.weightUnit)!!.value,
                        withNegative = false,
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}

