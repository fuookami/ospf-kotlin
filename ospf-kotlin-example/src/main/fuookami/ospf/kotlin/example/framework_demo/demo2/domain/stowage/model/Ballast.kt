package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.functional.*
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
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Ballast model for aircraft weight balance, managing ballast weight variables
 * and minimum ballast weight constraints.
 * 飞机重量平衡的压舱模型，管理压舱重量变量和最小压舱重量约束。
 *
 * @property ballastPositions the positions eligible for ballast / 可用于压舱的位置列表
 * @property minBallastWeight the minimum required ballast weight, or null if not specified / 最小所需压舱重量，未指定时为 null
 * @property adviceBallastWeight the advised ballast weight, or null if not specified / 建议压舱重量，未指定时为 null
 * @property load the load decision variables / 装载决策变量
*/
class Ballast(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    val ballastPositions: List<Position>,
    val minBallastWeight: Quantity<Flt64>?,
    val adviceBallastWeight: Quantity<Flt64>?,
    val load: Load
) {
    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            positions: List<Position>,
            minBallastWeight: Quantity<Flt64>?,
            load: Load
        ): Ballast {
            TODO("not implemented yet")
        }
    }

    lateinit var ballastWeight: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var adaptiveMinBallastWeight: QuantityLinearIntermediateSymbol<Flt64>

    /**
     * Registers ballast weight and adaptive minimum ballast weight symbols into the model.
     * 将压舱重量和自适应最小压舱重量符号注册到模型中。
     *
     * @param stowageMode the stowage mode to use / 使用的装载模式
     * @param model the linear meta-model to register into / 要注册到的线性元模型
     * @return success or failure / 成功或失败
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::ballastWeight.isInitialized) {
            val poly = MutableLinearPolynomial()
            for (position in ballastPositions) {
                val j = positions.indexOf(position)
                poly += LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
            }
            ballastWeight = Quantity(
                LinearExpressionSymbol(
                    poly,
                    name = "ballast_weight"
                ),
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(ballastWeight)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        if (stowageMode.withSoftSecurity) {
            if (!::adaptiveMinBallastWeight.isInitialized) {
                adaptiveMinBallastWeight = if (minBallastWeight != null) {
                    Quantity(
                        LinearExpressionSymbol(
                            minBallastWeight.to(aircraftModel.weightUnit)!!.value,
                            name = "min_ballast_weight"
                        ),
                        aircraftModel.weightUnit
                    )
                } else {
                    val poly = MutableLinearPolynomial()
                    // todo
                    Quantity(
                        LinearExpressionSymbol(
                            poly,
                            name = "min_ballast_weight"
                        ),
                        aircraftModel.weightUnit
                    )
                }
            }
            when (val result = model.add(adaptiveMinBallastWeight)) {
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
