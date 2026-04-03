@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.solver.config

import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlin.time.Duration

data class CoptSolverConfig(
    val server: String? = null,
    val port: UInt64? = null,
    val password: String? = null,
    val connectionTime: Duration? = null
)



