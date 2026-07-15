/** Gurobi 插件异步求解协程作用域 / Gurobi plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlinx.coroutines.*

/** Gurobi 插件异步求解协程作用域 / Gurobi plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)