package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto

import kotlinx.serialization.*

/**
 * Policy configuration for solver strategy selection.
 * 求解器策略选择的策略配置。
 *
 * @property preferBenders 是否优先使用Benders分解而非直接MILP / whether to prefer Benders decomposition over direct MILP
 * @property bendersFallbackToMilp whether to fall back to MILP if Benders fails / Benders失败时是否回退到MILP
*/
@Serializable
data class SolvePolicy(
    val preferBenders: Boolean = false,
    val bendersFallbackToMilp: Boolean = true,
    /** 独立的分区组合列生成路径，不与 Benders 联合。 / Independent partition CG path, not combined with Benders. */
    val partitionColumnGeneration: Boolean = false
)
