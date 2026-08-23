package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security

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
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Aggregates airworthiness and safety constraints including density limits, envelopes, and weight constraints.
 * 聚合适航和安全约束（包括密度限制、包络线和重量约束）。
 *
 * @property aircraftModel 飞机型号规格 / The aircraft model specification.
 * @property fuselage 机身配置 / The fuselage configuration.
 * @property positions 货物位置列表 / The list of cargo positions.
 * @property maxZoneLoadWeight 每个机身区域的最大载荷重量 / Maximum load weight per fuselage zone.
 * @property maxCumulativeLoadWeight 最大累积载荷重量约束 / Maximum cumulative load weight constraints.
 * @property minPayload 最小总业载要求 / Minimum total payload requirement.
 * @property envelopeLongitudinalMomentMin 包线纵向力矩下界 / Envelope longitudinal moment lower bound.
 * @property envelopeLongitudinalMomentMax 包线纵向力矩上界 / Envelope longitudinal moment upper bound.
 * @property targetLongitudinalMoment 目标纵向力矩 / Target longitudinal moment.
 * @property maxLongitudinalMomentDeviation 目标纵向力矩允许偏差 / Allowed target longitudinal moment deviation.
 * @property maxLateralImbalance 最大横向不平衡 / Maximum lateral imbalance.
 * @property maxAdjacentLoadGap 相邻位置之间允许的最大载荷间隙，可为空 / Maximum allowed load gap between adjacent positions, nullable.
 * @property load 载荷模型 / The load model.
 * @property payload 载荷量模型 / The payload model.
 * @property totalWeight 总重量模型 / The total weight model.
 * @property ballast 配重模型，可为空 / The ballast model, nullable.
 * @property torque 扭矩模型 / The torque model.
 * @property horizontalStabilizers 水平安定面键值到安定面的映射 / Map of horizontal stabilizer keys to stabilizers.
 * @property stowage 装载模型，可为空 / The stowage model, nullable.
*/
class Aggregation(
    internal val aircraftModel: AircraftModel,
    internal val fuselage: Fuselage,
    internal val positions: List<Position>,
    linearDensityLimitZones: List<LinearDensity.LimitZone>,
    surfaceDensityLimitZones: List<SurfaceDensity.LimitZone>,
    val maxZoneLoadWeight: MaxZoneLoadWeight,
    val maxCumulativeLoadWeight: MaxCumulativeLoadWeight,
    val minPayload: Quantity<Flt64>,
    val envelopeLongitudinalMomentMin: Quantity<Flt64>,
    val envelopeLongitudinalMomentMax: Quantity<Flt64>,
    val targetLongitudinalMoment: Quantity<Flt64>,
    val maxLongitudinalMomentDeviation: Quantity<Flt64>,
    val maxLateralImbalance: Quantity<Flt64>,
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
     * @param stowageMode 使用的装载模式 / The stowage mode to use.
     * @param model 要注册约束的线性元模型 / The linear meta model to register constraints with.
     * @return 成功或失败结果 / Success or failure result.
    */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = linearDensity.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = surfaceDensity.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (maxCLIM != null) {
            when (val result = maxCLIM.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        when (val result = minLowPayload.register(model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        envelopes.values.forEach { envelopes ->
            envelopes.forEach { envelope ->
                when (val result = envelope.register(model)) {
                    is Ok -> {}

                    is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
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
     * @return 成功或失败结果 / Success or failure result.
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
     * @param stowageMode 使用的装载模式 / The stowage mode to use.
     * @param model The linear meta model for the sub-problem. / Benders 子问题的线性元模型
     * @param solution 主问题的解 / The solution from the master problem.
     * @return 成功或失败结果 / Success or failure result.
    */
    fun registerForBendersSP(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return register(stowageMode = stowageMode, model = model)
    }

    /**
     * Flushes state for the Benders decomposition sub-problem.
     * 为 Benders 分解子问题刷新状态。
     *
     * @param model The linear meta model for the sub-problem. / Benders 子问题的线性元模型
     * @param solution 主问题的解 / The solution from the master problem.
     * @return 成功或失败结果 / Success or failure result.
    */
    private fun flushForBendersSP(
        model: AbstractLinearMetaModel<Flt64>,
        solution: List<Flt64>
    ): Try {
        return ok
    }
}
