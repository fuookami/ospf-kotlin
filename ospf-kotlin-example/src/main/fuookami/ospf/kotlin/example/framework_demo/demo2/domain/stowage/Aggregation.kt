package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregation root for the stowage domain, assembling all sub-models
 * (stowage, load, payload, totalWeight, maxLoadWeight, ballast) and
 * providing registration entry points for the full model and Benders decomposition.
 * 配载域的聚合根，组装所有子模型（装载、载量、业载、总重、最大装载重量、压舱）
 * 并提供完整模型和 Benders 分解的注册入口。
 *
 * @property aircraftModel 机型规格 / the aircraft model specification
 * @property flight 航班信息 / the flight information
 * @property items 待装载的货物项目列表 / the list of cargo items to be stowed
 * @property positions 可用装载位置列表 / the list of available stowage positions
 * @property withMultiLoadingSchema 是否启用多装载方案 / whether multi-loading schema is enabled
 * @property appointment 货物与舱位之间的预约约束 / the appointment constraints between items and positions
 * @property biologicalLimit 生物货物兼容性规则 / the biological cargo compatibility rules
 * @property neighbours 舱位之间的邻接关系 / the adjacency relationships between positions
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    formula: Formula,
    fuselage: Fuselage,
    fuel: Map<FlightPhase, FuelConstant>,
    val flight: Flight,
    val items: List<Item>,
    val positions: List<Position>,
    plannedPayload: Quantity<Flt64>,
    maxPayload: Quantity<Flt64>,
    computedPayload: Quantity<Flt64>?,
    maxTotalWeight: Map<FlightPhase, Quantity<Flt64>>,
    computedTotalWeight: Map<FlightPhase, Quantity<Flt64>>,
    val withMultiLoadingSchema: Boolean,
    val appointment: Appointment,
    val biologicalLimit: BiologicalLimit,
    internal val neighbours: HashMap<NeighbourType, List<Neighbour>>
) {
    val stowage = Stowage(
        items = items,
        positions = positions,
    )

    val load = Load(
        aircraftModel = aircraftModel,
        formula = formula,
        items = items,
        positions = positions,
        withMultiLoadingSchema = withMultiLoadingSchema,
        stowage = stowage
    )

    val payload = Payload(
        plannedPayload = plannedPayload,
        maxPayload = maxPayload,
        computedPayload = computedPayload,
        aircraftModel = aircraftModel,
        items = items,
        positions = positions,
        load = load
    )

    val totalWeight = TotalWeight(
        maxTotalWeight = maxTotalWeight,
        computedTotalWeight = computedTotalWeight,
        aircraftModel = aircraftModel,
        fuselage = fuselage,
        fuel = fuel,
        payload = payload,
    )

    val maxLoadWeight = MaxLoadWeight(
        aircraftModel = aircraftModel,
        fuselage = fuselage,
        items = items,
        positions = positions,
        totalWeight = totalWeight
    )

    val ballast = if (aircraftModel.ballastNeeded) {
        Ballast(
            aircraftModel = aircraftModel,
            positions = positions,
            minBallastWeight = null,
            load = load
        )
    } else {
        null
    }

    /**
     * Registers all sub-models into the given linear meta-model for full optimization.
     * 将所有子模型注册到给定的线性元模型中以进行完整优化。
     *
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @return 成功或失败 / success or failure
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = stowage.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = load.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = payload.register(stowageMode, model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = totalWeight.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = maxLoadWeight.register(model)) {
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

    /**
     * Registers sub-models for the Benders master problem (stowage assignment + load linking).
     * 为 Benders 主问题注册子模型（装载分配 + 载量链接约束）。
     *
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @return 成功或失败 / success or failure
    */
    fun registerForBendersMP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        // Master problem: stowage assignment variables + load linking constraints
        when (val result = stowage.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = load.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = payload.register(stowageMode, model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    /**
     * Registers sub-models for the Benders sub-problem (airworthiness constraints: payload, totalWeight, maxLoadWeight).
     * 为 Benders 子问题注册子模型（适航约束：业载、总重、最大装载重量）。
     *
     * @param stowageMode 使用的装载模式 / the stowage mode to use
     * @param model 要注册到的线性元模型 / the linear meta-model to register into
     * @param solution 用于固定变量的主问题解 / the master problem solution used to fix variables
     * @return 成功或失败 / success or failure
    */
    fun registerForBendersSP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // Sub problem: airworthiness constraints (payload, totalWeight, maxLoadWeight)
        // Variables are fixed from master solution
        when (val result = payload.register(stowageMode, model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = totalWeight.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = maxLoadWeight.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }

    /**
     * Flushes master problem variable values into the Benders sub-problem model.
     * 将主问题变量值刷新到 Benders 子问题模型中。
     *
     * @param model 线性元模型 / the linear meta-model
     * @param solution 主问题解 / the master problem solution
     * @return 成功或失败 / success or failure
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        // Fix master variables in sub problem to their solution values
        // This is handled by the BendersSolver via fixedVariables parameter
        return ok
    }
}
