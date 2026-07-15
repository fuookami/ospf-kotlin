package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Configuration for overriding Benders decomposition quality parameters.
 * Benders分解质量参数覆盖配置。
 *
 * @property weakGapMultiplier multiplier applied to the weak convergence gap / 应用于弱收敛间隙的乘数
 * @property weakGapFloor floor value for the weak convergence gap / 弱收敛间隙的下限值
 * @property iterationPressurePercent percentage controlling iteration pressure / 控制迭代压力的百分比
 * @property cutDensityMinIterations minimum iterations before cut density is evaluated / 评估割平面密度前的最小迭代次数
 * @property cutDensityThreshold threshold for cut density evaluation / 割平面密度评估的阈值
 * @property trajectoryMinSnapshots minimum number of trajectory snapshots / 轨迹快照的最小数量
 * @property trajectoryStepMultiplier multiplier for trajectory step size / 轨迹步长的乘数
 * @property trajectoryStepFloor floor value for trajectory step size / 轨迹步长的下限值
 * @property timeGuardMinMs minimum time guard in milliseconds / 最小时间保护（毫秒）
 * @property scoreGapWeight weight of the gap score component / 间隙得分分量的权重
 * @property scoreTimeWeight weight of the time score component / 时间得分分量的权重
 * @property scoreIterationWeight weight of the iteration score component / 迭代得分分量的权重
 * @property scoreCutDensityWeight weight of the cut density score component / 割平面密度得分分量的权重
 * @property scoreTrajectoryWeight weight of the trajectory score component / 轨迹得分分量的权重
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