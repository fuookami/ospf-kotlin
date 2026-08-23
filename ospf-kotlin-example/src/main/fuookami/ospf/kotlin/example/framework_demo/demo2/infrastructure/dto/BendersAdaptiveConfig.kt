package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Configuration for adaptive Benders decomposition strategy.
 * Benders自适应分解策略的配置。
 *
 * @property minBinaryVariables 触发Benders分解的最小二值变量数 / the minimum number of binary variables to trigger Benders decomposition
 * @property maxIterations 允许的Benders最大迭代次数 / the maximum number of Benders iterations allowed
 * @property tolerance the convergence tolerance for the Benders algorithm / Benders算法的收敛容差
*/
@Serializable
data class BendersAdaptiveConfig(
    val minBinaryVariables: Int = 4,
    val maxIterations: Int = 64,
    val tolerance: Double = 1e-6
)