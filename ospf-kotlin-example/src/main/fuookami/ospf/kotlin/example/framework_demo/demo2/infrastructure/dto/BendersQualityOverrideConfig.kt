package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

@Serializable
data class BendersQualityOverrideConfig(
    val weakGapMultiplier: Double? = null,
    val weakGapFloor: Double? = null,
    val iterationPressurePercent: Int? = null,
    val cutDensityMinIterations: Int? = null,
    val cutDensityThreshold: Double? = null,
    val trajectoryMinSnapshots: Int? = null,
    val trajectoryStepMultiplier: Double? = null,
    val trajectoryStepFloor: Double? = null,
    val timeGuardMinMs: Long? = null,
    val scoreGapWeight: Double? = null,
    val scoreTimeWeight: Double? = null,
    val scoreIterationWeight: Double? = null,
    val scoreCutDensityWeight: Double? = null,
    val scoreTrajectoryWeight: Double? = null
)
