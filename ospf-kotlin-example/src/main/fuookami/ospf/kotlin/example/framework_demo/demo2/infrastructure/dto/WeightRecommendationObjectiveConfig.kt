package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

@Serializable
data class WeightRecommendationObjectiveConfig(
    val balancePriority: Double = 1000.0,
    val payloadPriority: Double = 1.0
)
