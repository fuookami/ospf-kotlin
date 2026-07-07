package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Policy configuration for solver strategy selection.
 * 求解器策略选择的策略配置。
 *
 * @property preferBenders whether to prefer Benders decomposition over direct MILP / 是否优先使用Benders分解而非直接MILP
 * @property bendersFallbackToMilp whether to fall back to MILP if Benders fails / Benders失败时是否回退到MILP
 */
@Serializable
data class SolvePolicy(
    val preferBenders: Boolean = false,
    val bendersFallbackToMilp: Boolean = true
)