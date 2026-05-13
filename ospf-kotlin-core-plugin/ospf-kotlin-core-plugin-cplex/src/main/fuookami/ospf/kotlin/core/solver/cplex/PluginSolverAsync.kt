package fuookami.ospf.kotlin.core.solver.cplex

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)