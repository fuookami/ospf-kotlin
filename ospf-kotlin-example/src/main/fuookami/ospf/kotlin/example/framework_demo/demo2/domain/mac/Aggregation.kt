package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.MAC
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates MAC (Mean Aerodynamic Chord), torque, and horizontal stabilizer computations.
 * 聚合 MAC（平均气动弦）、扭矩和水平安定面计算。
 *
 * @property torque Torque computation for all flight phases / 所有飞行阶段的扭矩计算
 * @property mac MAC percentage computation / MAC 百分比计算
 * @property horizontalStabilizers Horizontal stabilizer computations keyed by stabilizer configuration / 按安定面配置索引的水平安定面计算
*/
class Aggregation(
    aircraftModel: AircraftModel,
    fuselage: Fuselage,
    fuel: Map<FlightPhase, FuelConstant>,
    formula: Formula,
    positions: List<Position>,
    load: Load,
    totalWeight: TotalWeight,
    horizontalStabilizers: HashMap<HorizontalStabilizer.Key, Pair<List<HorizontalStabilizer.Point>, HorizontalStabilizer.Limit>>
) {
    val torque = Torque(
        aircraftModel = aircraftModel,
        fuselage = fuselage,
        fuel = fuel,
        formula = formula,
        positions = positions,
        load = load
    )

    val mac = MAC(
        aircraftModel = aircraftModel,
        formula = formula,
        totalWeight = totalWeight,
        torque = torque
    )

    val horizontalStabilizers = horizontalStabilizers.mapValues {
        HorizontalStabilizer(
            aircraftModel = aircraftModel,
            key = it.key,
            points = it.value.first,
            limit = it.value.second,
            totalWeight = totalWeight,
            mac = mac
        )
    }

    /**
     * Registers torque, MAC, and horizontal stabilizer symbols into the optimization model.
     * 将扭矩、MAC 和水平安定面符号注册到优化模型中。
     *
     * @param stowageMode The stowage mode controlling which symbols are registered / 控制注册哪些符号的装载模式
     * @param model The linear meta-model to register symbols into / 要注册符号的线性元模型
     * @return [Try] indicating success or failure / 表示成功或失败
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = torque.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = mac.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        horizontalStabilizers.values.forEach {
            when (val result = it.register(stowageMode, model)) {
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
