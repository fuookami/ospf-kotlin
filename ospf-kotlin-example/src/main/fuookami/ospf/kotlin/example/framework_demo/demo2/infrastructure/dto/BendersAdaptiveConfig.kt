package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

@Serializable
data class BendersAdaptiveConfig(
    val minBinaryVariables: Int = 4,
    val maxIterations: Int = 64,
    val tolerance: Double = 1e-6
)
