/** Diagnostic model-element identity helpers. / 诊断模型元素身份辅助函数。 */
package fuookami.ospf.kotlin.core.solver.report

import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView

/**
 * Resolve the variable identity available to a diagnostic provider. /
 * 解析诊断提供者当前可用的变量身份。
 *
 * An explicit variable ID may carry stable identity. Keys synthesized from the process-local origin
 * identifier or variable index are pipeline-local diagnostic keys; they must not be promoted to a
 * cross-rebuild identity. /
 * 显式变量 ID 可以承载稳定身份；由进程内 origin 标识或变量索引生成的键只用于当前流水线诊断，
 * 不得提升为跨重建身份。
 *
 * @return 变量诊断标识 / Diagnostic variable identifier
 */
fun Variable.diagnosticVariableId(): VariableId {
    return id ?: VariableId(
        origin?.let { "${it.identifier}:${it.index}" } ?: "model-local-variable:$index"
    )
}

/**
 * Resolve a linear-row identity for native diagnostics. /
 * 解析原生诊断使用的线性行身份。
 *
 * Row indices are retained only as a model-local disambiguator. Unless an explicit stable ID is
 * available, they must not be presented as a cross-rebuild identity. /
 * 行号只作为模型内消歧信息；除非存在显式稳定 ID，否则不得将其当作跨重建身份。
 *
 * @param index 模型约束行索引 / Model constraint row index
 * @return 线性约束诊断标识 / Diagnostic linear constraint identifier
 */
fun LinearTriadModelView.diagnosticConstraintId(index: Int): ConstraintId {
    constraints.ids.getOrNull(index)?.let { return it }
    val originName = constraints.origins.getOrNull(index)
        ?.name
        ?.takeIf { it.isNotBlank() }
    val displayName = constraints.names.getOrNull(index)?.takeIf { it.isNotBlank() }
    val prefix = originName?.let { "origin-constraint:$it" }
        ?: displayName?.let { "model-local-constraint:$it" }
        ?: "model-local-constraint"
    return ConstraintId("$prefix:$index")
}

/**
 * Resolve a quadratic-row identity for native diagnostics. / 解析原生诊断使用的二次行身份。
 *
 * @param index 模型约束行索引 / Model constraint row index
 * @return 二次约束诊断标识 / Diagnostic quadratic constraint identifier
 */
fun QuadraticTetradModelView.diagnosticConstraintId(index: Int): ConstraintId {
    constraints.ids.getOrNull(index)?.let { return it }
    val originName = constraints.origins.getOrNull(index)
        ?.name
        ?.takeIf { it.isNotBlank() }
    val displayName = constraints.names.getOrNull(index)?.takeIf { it.isNotBlank() }
    val prefix = originName?.let { "origin-constraint:$it" }
        ?: displayName?.let { "model-local-constraint:$it" }
        ?: "model-local-constraint"
    return ConstraintId("$prefix:$index")
}
