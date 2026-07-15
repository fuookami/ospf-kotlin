package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates redundancy and experimental longitudinal balance models for weight distribution analysis.
 * 聚合冗余和实验纵向平衡模型用于重量分布分析。
 *
 * @property aircraftModel The aircraft model reference / 飞机模型引用
 * @property redundancy The redundancy model for spare capacity analysis / 用于备用容量分析的冗余模型
 * @property experimentalLongitudinalBalance The experimental longitudinal balance model / 实验纵向平衡模型
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    flight: Flight,
    items: List<Item>,
    positions: List<Position>,
    stowage: Stowage,
    load: Load,
    payload: Payload
) {
    val redundancy = Redundancy(
        aircraftModel = aircraftModel,
        flight = flight,
        items = items,
        positions = positions,
        stowage = stowage,
        load = load,
        payload = payload
    )

    val experimentalLongitudinalBalance = ExperimentalLongitudinalBalance(
        aircraftModel = aircraftModel,
        positions = positions,
        load = load,
        payload = payload,
        redundancy = redundancy
    )

    /**
     * Registers redundancy and longitudinal balance constraints into the optimization model.
     * 将冗余和纵向平衡约束注册到优化模型中。
     *
     * @param stowageMode The stowage mode for the optimization / 优化的装载模式
     * @param model The linear meta model to register into / 要注册到的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = redundancy.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = experimentalLongitudinalBalance.register(model)) {
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

    /**
     * Registers redundancy constraints for the Benders master problem.
     * 为 Benders 主问题注册冗余约束。
     *
     * @param model The linear meta model for the master problem / 主问题的线性元模型
     * @return Success or failure result / 成功或失败结果
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    /**
     * Registers redundancy constraints for the Benders sub-problem.
     * 为 Benders 子问题注册冗余约束。
     *
     * @param model The linear meta model for the sub-problem / 子问题的线性元模型
     * @param solution The solution values from the sub-problem / 子问题的解值
     * @return Success or failure result / 成功或失败结果
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }

    /**
     * Flushes the Benders sub-problem solution into the redundancy context.
     * 将 Benders 子问题解刷新到冗余上下文中。
     *
     * @param model The linear meta model for the sub-problem / 子问题的线性元模型
     * @param solution The solution values from the sub-problem / 子问题的解值
     * @return Success or failure result / 成功或失败结果
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
