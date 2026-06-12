/** COPT 插件异步求解协程作用域 / COPT plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.copt

import kotlinx.coroutines.*

/** COPT 插件异步求解协程作用域 / COPT plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)