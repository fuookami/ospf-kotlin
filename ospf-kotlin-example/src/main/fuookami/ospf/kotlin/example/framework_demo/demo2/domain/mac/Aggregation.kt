package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac

import fuookami.ospf.kotlin.utils.error.*
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
 * @property torque 所有飞行阶段的扭矩计算 / Torque computation for all flight phases
 * @property mac MAC percentage computation / MAC 百分比计算
 * @property horizontalStabilizers 按安定面配置索引的水平安定面计算 / Horizontal stabilizer computations keyed by stabilizer configuration
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
     * @param stowageMode 控制注册哪些符号的装载模式 / The stowage mode controlling which symbols are registered
     * @param model 要注册符号的线性元模型 / The linear meta-model to register symbols into
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = torque.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = mac.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        horizontalStabilizers.values.forEach {
            when (val result = it.register(stowageMode, model)) {
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

    /**
     * Registers all symbols for the Benders master problem.
     * 为 Benders 主问题注册所有符号。
     *
     * @param stowageMode 控制注册行为的装载模式 / The stowage mode controlling registration
     * @param model The linear meta-model for the master problem / Benders 主问题的线性元模型
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return register(stowageMode = stowageMode, model = model)
    }

    /**
     * Registers symbols for the Benders sub-problem.
     * 为 Benders 子问题注册符号。
     *
     * @param model The linear meta-model for the sub-problem / Benders 子问题的线性元模型
     * @param solution 来自主问题的解 / The solution from the master problem
     * @return 表示成功或失败 / [Try] indicating success or failure
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
     * @param solution 来自主问题的解 / The solution from the master problem
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
