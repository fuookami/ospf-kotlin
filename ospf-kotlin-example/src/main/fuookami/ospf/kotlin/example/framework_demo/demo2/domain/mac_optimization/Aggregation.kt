package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates MAC optimization models for longitudinal and lateral balance.
 * 聚合纵向和横向平衡的 MAC 优化模型。
 *
 * @property macRange The MAC range model for balance optimization / 用于平衡优化的 MAC 范围模型
 * @property longitudinalBalance The longitudinal balance slack model / 纵向平衡松弛模型
 * @property lateralBalance The lateral balance slack model (null for narrow-body) / 横向平衡松弛模型（窄体机为 null）
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    formula: Formula,
    totalWeight: TotalWeight,
    torque: Torque,
    internal val horizontalStabilizers: Map<HorizontalStabilizer.Key, HorizontalStabilizer>
) {
    val macRange = MACRange(
        aircraftModel = aircraftModel,
        formula = formula,
        totalWeight = totalWeight
    )

    val longitudinalBalance = LongitudinalBalance(
        aircraftModel = aircraftModel,
        macRange = macRange,
        torque = torque
    )

    val lateralBalance = if (aircraftModel.wideBody) {
        LateralBalance(
            aircraftModel = aircraftModel,
            torque = torque
        )
    } else {
        null
    }

    /**
     * Registers longitudinal and lateral balance symbols into the optimization model.
     * 将纵向和横向平衡符号注册到优化模型中。
     *
     * @param stowageMode The stowage mode controlling which symbols are registered / 控制注册哪些符号的装载模式
     * @param model The linear meta-model to register symbols into / 要注册符号的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = longitudinalBalance.register(stowageMode, model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (lateralBalance != null) {
            when (val result = lateralBalance.register(model)) {
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

    /**
     * Registers all symbols for the Benders master problem using full-load stowage mode.
     * 使用满载装载模式为 Benders 主问题注册所有符号。
     *
     * @param model The linear meta-model for the master problem / Benders 主问题的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    /**
     * Registers symbols for the Benders sub-problem.
     * 为 Benders 子问题注册符号。
     *
     * @param model The linear meta-model for the sub-problem / Benders 子问题的线性元模型
     * @param solution The solution from the master problem / 来自主问题的解
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes state for the Benders sub-problem after solving.
     * 求解后刷新 Benders 子问题的状态。
     *
     * @param model The linear meta-model for the sub-problem / Benders 子问题的线性元模型
     * @param solution The solution from the master problem / 来自主问题的解
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
