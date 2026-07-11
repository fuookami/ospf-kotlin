package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates airworthiness and safety constraints including density limits, envelopes, and weight constraints.
 * 聚合适航和安全约束（包括密度限制、包络线和重量约束）。
 *
 * @property aircraftModel The aircraft model specification. / 飞机型号规格
 * @property fuselage The fuselage configuration. / 机身配置
 * @property positions The list of cargo positions. / 货物位置列表
 * @property maxZoneLoadWeight Maximum load weight per fuselage zone. / 每个机身区域的最大载荷重量
 * @property maxCumulativeLoadWeight Maximum cumulative load weight constraints. / 最大累积载荷重量约束
 * @property maxUnsymmetricalLinearDensity Maximum unsymmetrical linear density constraints, nullable. / 最大不对称线性密度约束，可为空
 * @property maxAdjacentLoadGap Maximum allowed load gap between adjacent positions, nullable. / 相邻位置之间允许的最大载荷间隙，可为空
 * @property load The load model. / 载荷模型
 * @property payload The payload model. / 载荷量模型
 * @property totalWeight The total weight model. / 总重量模型
 * @property ballast The ballast model, nullable. / 配重模型，可为空
 * @property torque The torque model. / 扭矩模型
 * @property horizontalStabilizers Map of horizontal stabilizer keys to stabilizers. / 水平安定面键值到安定面的映射
 * @property stowage The stowage model, nullable. / 装载模型，可为空
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val fuselage: Fuselage,
    internal val positions: List<Position>,
    linearDensityLimitZones: List<LinearDensity.LimitZone>,
    surfaceDensityLimitZones: List<SurfaceDensity.LimitZone>,
    val maxZoneLoadWeight: MaxZoneLoadWeight,
    val maxCumulativeLoadWeight: MaxCumulativeLoadWeight,
    val maxUnsymmetricalLinearDensity: MaxUnsymmetricalLinearDensity?,
    maxCLIMPoints: List<MaxCLIM.Point>?,
    minLowPayloadPoints: List<MinLowPayload.Point>,
    envelopeBuilders: (FlightPhase, TotalWeight) -> List<AbstractEnvelope>,
    internal val load: Load,
    internal val payload: Payload,
    internal val totalWeight: TotalWeight,
    internal val ballast: Ballast?,
    internal val torque: Torque,
    internal val horizontalStabilizers: Map<HorizontalStabilizer.Key, HorizontalStabilizer>,
    internal val stowage: Stowage? = null,
    val maxAdjacentLoadGap: Double? = null
) {
    val linearDensity = LinearDensity(
        aircraftModel = aircraftModel,
        limitZones = linearDensityLimitZones,
        load = load,
        positions = positions
    )

    val surfaceDensity = SurfaceDensity(
        aircraftModel = aircraftModel,
        limitsZones = surfaceDensityLimitZones,
        load = load,
        positions = positions
    )

    val maxCLIM = if (aircraftModel.wideBody && !maxCLIMPoints.isNullOrEmpty()) {
        MaxCLIM(
            aircraftModel = aircraftModel,
            points = maxCLIMPoints,
            totalWeight = totalWeight
        )
    } else {
        null
    }

    val minLowPayload = MinLowPayload(
        aircraftModel = aircraftModel,
        points = minLowPayloadPoints,
        totalWeight = totalWeight
    )

    val envelopes = FlightPhase.entries.associateWith { phase ->
        envelopeBuilders(phase, totalWeight)
    }

    /**
     * Registers all airworthiness and safety constraints with the given model.
     * 将所有适航和安全约束注册到给定模型中。
     *
     * @param stowageMode The stowage mode to use. / 使用的装载模式
     * @param model The linear meta model to register constraints with. / 要注册约束的线性元模型
     * @return Success or failure result. / 成功或失败结果
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = linearDensity.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = surfaceDensity.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (maxCLIM != null) {
            when (val result = maxCLIM.register(model)) {
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        when (val result = minLowPayload.register(model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        envelopes.values.forEach { envelopes ->
            envelopes.forEach { envelope ->
                when (val result = envelope.register(model)) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }
            }
        }

        return ok
    }

    /**
     * Registers constraints for the Benders decomposition master problem.
     * 为 Benders 分解主问题注册约束。
     *
     * @param model The linear meta model for the master problem. / Benders 主问题的线性元模型
     * @return Success or failure result. / 成功或失败结果
    */
    fun registerForBendersMP(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        return ok
    }

    /**
     * Registers constraints for the Benders decomposition sub-problem.
     * 为 Benders 分解子问题注册约束。
     *
     * @param model The linear meta model for the sub-problem. / Benders 子问题的线性元模型
     * @param solution The solution from the master problem. / 主问题的解
     * @return Success or failure result. / 成功或失败结果
    */
    fun registerForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return register(stowageMode = StowageMode.FullLoad, model = model)
    }

    /**
     * Flushes state for the Benders decomposition sub-problem.
     * 为 Benders 分解子问题刷新状态。
     *
     * @param model The linear meta model for the sub-problem. / Benders 子问题的线性元模型
     * @param solution The solution from the master problem. / 主问题的解
     * @return Success or failure result. / 成功或失败结果
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
