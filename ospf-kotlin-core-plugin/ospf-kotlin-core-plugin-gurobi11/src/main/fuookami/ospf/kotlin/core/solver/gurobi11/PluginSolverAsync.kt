/** Gurobi 11 插件异步求解协程作用域 / Gurobi 11 plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Gurobi 11 插件异步求解协程作用域 / Gurobi 11 plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)