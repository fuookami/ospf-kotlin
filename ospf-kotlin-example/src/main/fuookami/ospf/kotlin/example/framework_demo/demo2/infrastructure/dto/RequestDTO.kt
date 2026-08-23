package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Input data transfer object for a cargo item.
 * 货物项的输入数据传输对象。
 *
 * @property weight 货物重量 / the weight of the cargo
 * @property priority 货物优先级 / the priority level of the cargo
 * @property source 货物起始位置 / the origin location of the cargo
 * @property destination 货物目的位置 / the destination location of the cargo
 * @property requiresSeparation 此货物是否必须与某些其他货物分离 / whether this cargo must be separated from certain other cargos
 * @property order 货物装载顺序 / the requested cargo loading order
*/
@Serializable
data class CargoInput(
    val name: String,
    val weight: Double,
    val priority: Int,
    val source: String,
    val destination: String,
    val requiresSeparation: Boolean = false,
    val order: Int? = null
)

/**
 * Input data transfer object for a cargo position (compartment).
 * 货物舱位的输入数据传输对象。
 *
 * @property maxWeight 舱位最大承重 / the maximum weight capacity of the position
 * @property longitudinalArm 舱位的纵向力臂 / the longitudinal arm (moment arm) of the position
 * @property lateralArm 舱位的横向力臂 / the lateral arm (moment arm) of the position
 * @property area 舱位可用面积 / the usable area of the position
 * @property length 舱位纵向长度 / the longitudinal length of the position
 * @property maxLoadCount 舱位最大装载件数 / the maximum number of items for the position
 * @property loadedItems 已经装载的货物名称 / names of items already loaded in the position
 * @property predicateLoadWeightMin 预配载最小载荷重量 / the minimum predicate load weight
 * @property ala 实际装载件数 / the actual loaded item count
 * @property alw 实际装载重量 / the actual loaded weight
*/
@Serializable
data class PositionInput(
    val name: String,
    val maxWeight: Double,
    val longitudinalArm: Double,
    val lateralArm: Double,
    val area: Double = 5.0,
    val length: Double = 2.0,
    val maxLoadCount: Int = 3,
    val loadedItems: List<String> = emptyList(),
    val predicateLoadWeightMin: Double? = null,
    val ala: Int? = null,
    val alw: Double? = null
)

/** Input trailer type used by loading-effectiveness initialization. / 装车效能初始化使用的拖车类型。 */
@Serializable
enum class TrailerTypeInput {
    Hardstand,
    Transit,
    Warehouse
}

/** Input data transfer object for a trailer and its cargo names. / 拖车及其货物名称的输入数据传输对象。 */
@Serializable
data class TrailerInput(
    val type: TrailerTypeInput,
    val order: Int,
    val name: String,
    val items: List<String> = emptyList()
)

/**
 * Enumeration of supported aircraft types.
 * 支持的飞机类型枚举。
*/
@Serializable
enum class AircraftTypeInput {
    B737, B757, B767, B747, Unknown
}

/**
 * Data transfer object for the optimization request.
 * 优化请求的数据传输对象。
 *
 * @property cargos 待装载的货物输入列表 / the list of cargo inputs to be loaded
 * @property positions 可用舱位输入列表 / the list of available position inputs
 * @property aircraftType 优化所用的飞机类型 / the type of aircraft for the optimization
 * @property solvePolicy 求解器策略配置 / the solver strategy policy configuration
 * @property bendersAdaptive the Benders adaptive strategy configuration / Benders自适应策略配置
 * @property bendersQualityOverrides 可选的Benders质量覆盖参数 / optional Benders quality override parameters
 * @property weightRecommendationObjective 权重推荐目标配置 / the weight recommendation objective configuration
 * @property payloadUpperBound 总载荷重量上限 / the upper bound for total payload weight
 * @property minPayloadRatio 最小载荷利用率 / the minimum payload utilization ratio
 * @property maxAdjacentLoadGap 相邻舱位间最大重量差 / the maximum weight gap between adjacent positions
 * @property maxCumulativeForwardLoad 最大累积前向载荷 / the maximum cumulative forward load
 * @property maxCumulativeBackwardLoad 最大累积后向载荷 / the maximum cumulative backward load
 * @property envelopeLongitudinalMomentMin 包线最小纵向力矩 / the minimum longitudinal moment in the envelope
 * @property envelopeLongitudinalMomentMax 包线最大纵向力矩 / the maximum longitudinal moment in the envelope
 * @property targetLongitudinalMoment 目标纵向力矩 / the target longitudinal moment
 * @property maxLongitudinalMomentDeviation 允许的最大纵向力矩偏差 / the maximum allowed deviation from the target longitudinal moment
 * @property maxLateralImbalance 允许的最大横向不平衡 / the maximum allowed lateral imbalance
 * @property trailers 拖车输入列表 / the list of trailer inputs
*/
@Serializable
data class RequestDTO(
    val id: String,
    val cargos: List<CargoInput> = emptyList(),
    val positions: List<PositionInput> = emptyList(),
    val aircraftType: AircraftTypeInput = AircraftTypeInput.B737,
    val solvePolicy: SolvePolicy = SolvePolicy(),
    val bendersAdaptive: BendersAdaptiveConfig = BendersAdaptiveConfig(),
    val bendersQualityOverrides: BendersQualityOverrideConfig? = null,
    val weightRecommendationObjective: WeightRecommendationObjectiveConfig = WeightRecommendationObjectiveConfig(),
    val payloadUpperBound: Double = 20.0,
    val minPayloadRatio: Double = 0.6,
    val maxAdjacentLoadGap: Double = 8.0,
    val maxCumulativeForwardLoad: Double = 20.0,
    val maxCumulativeBackwardLoad: Double = 20.0,
    val envelopeLongitudinalMomentMin: Double = -20.0,
    val envelopeLongitudinalMomentMax: Double = 20.0,
    val targetLongitudinalMoment: Double = 0.0,
    val maxLongitudinalMomentDeviation: Double = 20.0,
    val maxLateralImbalance: Double = 12.0,
    val trailers: List<TrailerInput> = emptyList()
) {

    /** Derives the Parameter instance from this request. / 从此请求派生Parameter实例。 */
    val parameter: Parameter get() {
        return Parameter(
            weightRecommendationBalance = Flt64(weightRecommendationObjective.balancePriority),
            weightRecommendationPayload = Flt64(weightRecommendationObjective.payloadPriority)
        )
    }

    companion object {
        /**
         * Creates a sample RequestDTO with predefined cargo and position data.
         * 使用预定义的货物和舱位数据创建示例RequestDTO。
         *
         * @return 用于测试的示例RequestDTO / a sample RequestDTO for testing
        */
        fun sample(): RequestDTO = RequestDTO(
            id = "sample-001",
            cargos = listOf(
                CargoInput(name = "C1", weight = 8.0, priority = 10, source = "S1", destination = "D1", requiresSeparation = true, order = 0),
                CargoInput(name = "C2", weight = 6.0, priority = 6, source = "S2", destination = "D1", requiresSeparation = false, order = 1),
                CargoInput(name = "C3", weight = 4.0, priority = 4, source = "S1", destination = "D2", requiresSeparation = true, order = 2)
            ),
            positions = listOf(
                PositionInput(name = "P1", maxWeight = 10.0, longitudinalArm = -1.0, lateralArm = -0.5, predicateLoadWeightMin = 1.0),
                PositionInput(name = "P2", maxWeight = 10.0, longitudinalArm = 1.0, lateralArm = 0.5, predicateLoadWeightMin = 1.0)
            ),
            aircraftType = AircraftTypeInput.B737,
            solvePolicy = SolvePolicy(preferBenders = false, bendersFallbackToMilp = true),
            bendersAdaptive = BendersAdaptiveConfig(minBinaryVariables = 4, maxIterations = 64, tolerance = 1e-6),
            payloadUpperBound = 20.0,
            minPayloadRatio = 0.6,
            maxAdjacentLoadGap = 8.0,
            maxCumulativeForwardLoad = 20.0,
            maxCumulativeBackwardLoad = 20.0,
            envelopeLongitudinalMomentMin = -20.0,
            envelopeLongitudinalMomentMax = 20.0,
            targetLongitudinalMoment = 0.0,
            maxLongitudinalMomentDeviation = 20.0,
            maxLateralImbalance = 12.0
        )
    }
}
