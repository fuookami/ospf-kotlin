package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Configuration for weight recommendation objective priorities.
 * 权重推荐目标优先级的配置。
 *
 * @property balancePriority 负载均衡目标的优先权重 / the priority weight for load balance objective
 * @property payloadPriority 载荷优化目标的优先权重 / the priority weight for payload optimization objective
*/
@Serializable
data class WeightRecommendationObjectiveConfig(
    val balancePriority: Double = 1.0,
    val payloadPriority: Double = 1.0
)
