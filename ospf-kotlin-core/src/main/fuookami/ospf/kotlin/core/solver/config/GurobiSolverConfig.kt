@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.config

import kotlin.time.Duration

/**
 * Gurobi 求解器配置
 * Gurobi solver configuration
 */

/**
 * Gurobi 求解器的连接配置。
 * Connection configuration for Gurobi solver.
 *
 * @property server 服务器地址 / Server address
 * @property password 密码 / Password
 * @property connectionTime 连接超时 / Connection timeout
 */
data class GurobiSolverConfig(
    val server: String? = null,
    val password: String? = null,
    val connectionTime: Duration? = null
)
