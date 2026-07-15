package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Longitudinal balance slack variables for MAC range optimization across stowage modes.
 * 跨装载模式的 MAC 范围优化的纵向平衡松弛变量。
 *
 * @property slack Map of MAC range types to their corresponding slack symbols / MAC 范围类型到对应松弛符号的映射
*/
class LongitudinalBalance(
    private val aircraftModel: AircraftModel,
    private val macRange: MACRange,
    private val torque: Torque
) {
    lateinit var slack: Map<MACRange.Type, QuantityLinearIntermediateSymbol<Flt64>>

    /**
     * Registers the longitudinal balance slack symbols into the optimization model.
     * 将纵向平衡松弛符号注册到优化模型中。
     *
     * @param stowageMode The stowage mode determining which slack types to register / 决定注册哪些松弛类型的装载模式
     * @param model The linear meta-model to register symbols into / 要注册符号的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::slack.isInitialized) {
            slack = when (stowageMode) {
                StowageMode.FullLoad -> {
                    MACRange.Type.entries.associateWithNotNull { type ->
                        when (type) {
                            MACRange.Type.OPT, MACRange.Type.C -> {
                                null
                            }

                            else -> {
                                Quantity(
                                    LinearExpressionSymbol(Flt64, name = "longitudinal_balance_slack_${type.name}"),
                                    aircraftModel.torqueUnit
                                )
                            }
                        }
                    }
                }

                StowageMode.Predistribution -> {
                    mapOf(
                        MACRange.Type.OPT to Quantity(
                            LinearExpressionSymbol(Flt64, name = "longitudinal_balance_slack_opt"),
                            aircraftModel.torqueUnit
                        )
                    )
                }

                StowageMode.WeightRecommendation -> {
                    emptyMap()
                }
            }
        }
        slack.values.forEach {
            when (val result = model.add(it)) {
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
