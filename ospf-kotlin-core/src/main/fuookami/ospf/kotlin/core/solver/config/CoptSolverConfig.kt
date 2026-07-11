@file:OptIn(kotlin.time.ExperimentalTime::class)

/** COPT 求解器配置 / COPT solver configuration */
package fuookami.ospf.kotlin.core.solver.config

import kotlin.time.Duration
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * COPT 求解器的连接配置。
 * Connection configuration for COPT solver.
 *
 * @property server 服务器地址 / Server address
 * @property port 端口号 / Port number
 * @property password 密码 / Password
 * @property connectionTime 连接超时 / Connection timeout
*/
data class CoptSolverConfig(
    val server: String? = null,
    val port: UInt64? = null,
    val password: String? = null,
    val connectionTime: Duration? = null
)
