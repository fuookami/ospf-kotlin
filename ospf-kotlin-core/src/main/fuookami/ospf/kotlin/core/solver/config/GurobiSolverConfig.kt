@file:OptIn(kotlin.time.ExperimentalTime::class)

/** Gurobi 求解器配置 / Gurobi solver configuration */
package fuookami.ospf.kotlin.core.solver.config

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.solver.report.BackendConfiguration
import fuookami.ospf.kotlin.core.solver.report.BackendParameter
import fuookami.ospf.kotlin.core.solver.report.BackendParameterValue

/**
 * Gurobi 求解器的连接配置。 / Connection configuration for Gurobi solver.
 *
 * @property server 服务器地址 / Server address
 * @property password 密码 / Password
 * @property connectionTime 连接超时 / Connection timeout
*/
data class GurobiSolverConfig(
    val server: String? = null,
    val password: String? = null,
    val connectionTime: Duration? = null
) : BackendConfiguration {
    override val type: String = "gurobi"

    override fun parameters(): List<BackendParameter> {
        return buildList {
            server?.let { add(BackendParameter("server", BackendParameterValue.Text(it))) }
            password?.let {
                add(
                    BackendParameter(
                        name = "password",
                        value = BackendParameterValue.Text("<redacted>"),
                        sensitive = true
                    )
                )
            }
            connectionTime?.let { add(BackendParameter("connectionTime", BackendParameterValue.Text(it.toString()))) }
        }.sortedBy { it.name }
    }
}
