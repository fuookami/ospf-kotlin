package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.error.*
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
 * @property ballastPositions 可用于压舱的位置列表 / the positions eligible for ballast
 * @property minBallastWeight 最小所需压舱重量，未指定时为 null / the minimum required ballast weight, or null if not specified
 * @property adviceBallastWeight 建议压舱重量，未指定时为 null / the advised ballast weight, or null if not specified
 * @property load 装载决策变量 / the load decision variables
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
            return Ballast(
                aircraftModel = aircraftModel,
                positions = positions,
                ballastPositions = positions.filter { it.status.available },
                minBallastWeight = minBallastWeight,
                adviceBallastWeight = null,
                load = load
            )
        }
    }

    lateinit var ballastWeight: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var adaptiveMinBallastWeight: QuantityLinearIntermediateSymbol<Flt64>

    /**
     * Registers ballast weight and adaptive minimum ballast weight symbols into the model.
     * 将压舱重量和自适应最小压舱重量符号注册到模型中。
     *
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @return 成功或失败 / success or failure
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::ballastWeight.isInitialized) {
            var poly = LinearPolynomial()
            for (position in ballastPositions) {
                val j = positions.indexOf(position)
                poly += load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value
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
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
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
                    // No minimum was supplied by the request, so expose an explicit zero expression.
                    // 请求未提供最低压舱重量时，显式暴露零表达式，而不是保留空多项式占位。
                    Quantity(
                        LinearExpressionSymbol(
                            Flt64.zero,
                            name = "min_ballast_weight"
                        ),
                        aircraftModel.weightUnit
                    )
                }
            }
            when (val result = model.add(adaptiveMinBallastWeight)) {
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
