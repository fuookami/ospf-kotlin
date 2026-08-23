/**
 * SCIP 求解器配置 / SCIP solver configuration
*/
package fuookami.ospf.kotlin.core.solver.config

import fuookami.ospf.kotlin.core.solver.report.BackendConfiguration
import fuookami.ospf.kotlin.core.solver.report.BackendParameter
import fuookami.ospf.kotlin.core.solver.report.BackendParameterValue

/**
 * SCIP 求解器配置。 / SCIP solver configuration.
 *
 * @property presolve 是否显式启用 presolve / Whether presolve is explicitly enabled
 * @property randomSeed 原生随机种子 / Native random seed
 * @property deterministic 是否要求确定性模式 / Whether deterministic mode is requested
 * @property nativeParameters 额外的已脱敏原生参数 / Additional already-redacted native parameters
*/
data class SCIPSolverConfig(
    val presolve: Boolean? = null,
    val randomSeed: Long? = null,
    val deterministic: Boolean? = null,
    val nativeParameters: Map<String, BackendParameterValue> = emptyMap()
) : BackendConfiguration {
    override val type: String = "scip"

    override fun parameters(): List<BackendParameter> {
        val parameters = buildList {
            presolve?.let { add(BackendParameter("presolve", BackendParameterValue.BooleanValue(it))) }
            randomSeed?.let { add(BackendParameter("randomSeed", BackendParameterValue.Integer(it))) }
            deterministic?.let { add(BackendParameter("deterministic", BackendParameterValue.BooleanValue(it))) }
            nativeParameters.toSortedMap().forEach { (name, value) ->
                add(BackendParameter(name, value))
            }
        }
        return parameters.sortedBy { it.name }
    }
}
