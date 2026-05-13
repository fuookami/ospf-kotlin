package fuookami.ospf.kotlin.core.solver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val coreSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)