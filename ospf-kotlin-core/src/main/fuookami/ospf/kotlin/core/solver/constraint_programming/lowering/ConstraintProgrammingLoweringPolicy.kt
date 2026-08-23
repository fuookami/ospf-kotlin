/**
 * CP 到线性模型降阶策略。 / CP-to-linear lowering policy.
 */
package fuookami.ospf.kotlin.core.solver.constraint_programming.lowering

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * 精确降阶的规模与能力门禁。该策略只允许双向等价的线性化；无法证明等价时，降阶器返回结构化失败。 / Size and capability gates for exact lowering. / The policy only allows bidirectionally equivalent linearizations; unsupported or unproved transformations return a structured failure.
 *
 * @property sparseDomainLimit 稀疏值域展开上限 / Sparse-domain expansion limit
 * @property decompositionLimit 分解规模上限 / Decomposition size limit
 * @property auxiliaryVariableLimit 辅助变量上限 / Auxiliary variable limit
 * @property allowForbiddenAssignments 是否允许禁止表 / Whether forbidden tables are allowed
 * @property allowCumulative 是否允许 Cumulative / Whether Cumulative is allowed
 */
data class ConstraintProgrammingLoweringPolicy(
    val sparseDomainLimit: Int = 128,
    val decompositionLimit: Int = 256,
    val auxiliaryVariableLimit: Int = 4096,
    val allowForbiddenAssignments: Boolean = true,
    val allowCumulative: Boolean = false
) {
    /** 校验策略参数。 / Validate policy parameters.
     *
     * @return Validation result. / 校验结果。
     */
    fun validate(): Try {
        if (sparseDomainLimit <= 0 || decompositionLimit <= 0 || auxiliaryVariableLimit <= 0) {
            return Failed(
                ErrorCode.IllegalArgument,
                "CP 降阶规模上限必须为正 / CP lowering limits must be positive"
            )
        }
        return ok
    }

    companion object {
        /** 严格精确策略。 / Strict exact policy. */
        val Strict: ConstraintProgrammingLoweringPolicy = ConstraintProgrammingLoweringPolicy()
    }
}
