package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Configuration for weight recommendation objective priorities.
 * 权重推荐目标优先级的配置。
 *
 * @property balancePriority the priority weight for load balance objective / 负载均衡目标的优先权重
 * @property payloadPriority the priority weight for payload optimization objective / 载荷优化目标的优先权重
 */
@Serializable
data class WeightRecommendationObjectiveConfig(
    val balancePriority: Double = 1000.0,
    val payloadPriority: Double = 1.0
)