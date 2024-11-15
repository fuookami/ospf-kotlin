package fuookami.ospf.kotlin.core.backend.solver.config

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*

data class CoptSolverConfig(
    val server: String? = null,
    val port: UInt64? = null,
    val password: String? = null,
    val connectionTime: Duration? = null
)
