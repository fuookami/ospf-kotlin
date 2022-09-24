package fuookami.ospf.kotlin.core.backend.solver.config

import kotlin.time.Duration

data class GurobiSolverConfig(
    val server: String? = null,
    val password: String? = null,
    val connectionTime: Duration? = null
)
