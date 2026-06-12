/** SCIP 插件异步求解协程作用域 / SCIP plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.scip

import kotlinx.coroutines.*

/** SCIP 插件异步求解协程作用域 / SCIP plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)