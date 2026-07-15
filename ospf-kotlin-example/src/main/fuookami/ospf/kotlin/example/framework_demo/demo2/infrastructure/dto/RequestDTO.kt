package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Input data transfer object for a cargo item.
 * 货物项的输入数据传输对象。
 *
 * @property weight the weight of the cargo / 货物重量
 * @property priority the priority level of the cargo / 货物优先级
 * @property source the origin location of the cargo / 货物起始位置
 * @property destination the destination location of the cargo / 货物目的位置
 * @property requiresSeparation whether this cargo must be separated from certain other cargos / 此货物是否必须与某些其他货物分离
*/
@Serializable
data class CargoInput(
    val name: String,
    val weight: Double,
    val priority: Int,
    val source: String,
    val destination: String,
    val requiresSeparation: Boolean = false
)

/**
 * Input data transfer object for a cargo position (compartment).
 * 货物舱位的输入数据传输对象。
 *
 * @property maxWeight the maximum weight capacity of the position / 舱位最大承重
 * @property longitudinalArm the longitudinal arm (moment arm) of the position / 舱位的纵向力臂
 * @property lateralArm the lateral arm (moment arm) of the position / 舱位的横向力臂
*/
@Serializable
data class PositionInput(
    val name: String,
    val maxWeight: Double,
    val longitudinalArm: Double,
    val lateralArm: Double
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
 * @property cargos the list of cargo inputs to be loaded / 待装载的货物输入列表
 * @property positions the list of available position inputs / 可用舱位输入列表
 * @property aircraftType the type of aircraft for the optimization / 优化所用的飞机类型
 * @property solvePolicy the solver strategy policy configuration / 求解器策略配置
 * @property bendersAdaptive the Benders adaptive strategy configuration / Benders自适应策略配置
 * @property bendersQualityOverrides optional Benders quality override parameters / 可选的Benders质量覆盖参数
 * @property weightRecommendationObjective the weight recommendation objective configuration / 权重推荐目标配置
 * @property payloadUpperBound the upper bound for total payload weight / 总载荷重量上限
 * @property minPayloadRatio the minimum payload utilization ratio / 最小载荷利用率
 * @property maxAdjacentLoadGap the maximum weight gap between adjacent positions / 相邻舱位间最大重量差
 * @property maxCumulativeForwardLoad the maximum cumulative forward load / 最大累积前向载荷
 * @property maxCumulativeBackwardLoad the maximum cumulative backward load / 最大累积后向载荷
 * @property envelopeLongitudinalMomentMin the minimum longitudinal moment in the envelope / 包线最小纵向力矩
 * @property envelopeLongitudinalMomentMax the maximum longitudinal moment in the envelope / 包线最大纵向力矩
 * @property targetLongitudinalMoment the target longitudinal moment / 目标纵向力矩
 * @property maxLongitudinalMomentDeviation the maximum allowed deviation from the target longitudinal moment / 允许的最大纵向力矩偏差
 * @property maxLateralImbalance the maximum allowed lateral imbalance / 允许的最大横向不平衡
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
    val maxLateralImbalance: Double = 12.0
) {

    /** Derives the Parameter instance from this request. Not yet implemented. / 从此请求派生Parameter实例。尚未实现。 */
    val parameter: Parameter get() {
        TODO("Not yet implemented")
    }

    companion object {
        /**
         * Creates a sample RequestDTO with predefined cargo and position data.
         * 使用预定义的货物和舱位数据创建示例RequestDTO。
         *
         * @return a sample RequestDTO for testing / 用于测试的示例RequestDTO
        */
        fun sample(): RequestDTO = RequestDTO(
            id = "sample-001",
            cargos = listOf(
                CargoInput(name = "C1", weight = 8.0, priority = 10, source = "S1", destination = "D1", requiresSeparation = true),
                CargoInput(name = "C2", weight = 6.0, priority = 6, source = "S2", destination = "D1", requiresSeparation = false),
                CargoInput(name = "C3", weight = 4.0, priority = 4, source = "S1", destination = "D2", requiresSeparation = true)
            ),
            positions = listOf(
                PositionInput(name = "P1", maxWeight = 10.0, longitudinalArm = -1.0, lateralArm = -0.5),
                PositionInput(name = "P2", maxWeight = 10.0, longitudinalArm = 1.0, lateralArm = 0.5)
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