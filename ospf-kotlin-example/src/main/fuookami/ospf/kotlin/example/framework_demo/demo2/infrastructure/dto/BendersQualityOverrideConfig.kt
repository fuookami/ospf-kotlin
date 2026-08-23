package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Configuration for overriding Benders decomposition quality parameters.
 * Benders分解质量参数覆盖配置。
 *
 * @property weakGapMultiplier 应用于弱收敛间隙的乘数 / multiplier applied to the weak convergence gap
 * @property weakGapFloor 弱收敛间隙的下限值 / floor value for the weak convergence gap
 * @property iterationPressurePercent 控制迭代压力的百分比 / percentage controlling iteration pressure
 * @property cutDensityMinIterations 评估割平面密度前的最小迭代次数 / minimum iterations before cut density is evaluated
 * @property cutDensityThreshold 割平面密度评估的阈值 / threshold for cut density evaluation
 * @property trajectoryMinSnapshots 轨迹快照的最小数量 / minimum number of trajectory snapshots
 * @property trajectoryStepMultiplier 轨迹步长的乘数 / multiplier for trajectory step size
 * @property trajectoryStepFloor 轨迹步长的下限值 / floor value for trajectory step size
 * @property timeGuardMinMs 最小时间保护（毫秒） / minimum time guard in milliseconds
 * @property scoreGapWeight 间隙得分分量的权重 / weight of the gap score component
 * @property scoreTimeWeight 时间得分分量的权重 / weight of the time score component
 * @property scoreIterationWeight 迭代得分分量的权重 / weight of the iteration score component
 * @property scoreCutDensityWeight 割平面密度得分分量的权重 / weight of the cut density score component
 * @property scoreTrajectoryWeight 轨迹得分分量的权重 / weight of the trajectory score component
*/
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