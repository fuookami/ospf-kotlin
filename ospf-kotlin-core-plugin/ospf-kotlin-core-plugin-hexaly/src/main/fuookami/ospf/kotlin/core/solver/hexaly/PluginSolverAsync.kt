/** Hexaly 插件异步求解协程作用域 / Hexaly plugin async solver coroutine scope */
package fuookami.ospf.kotlin.core.solver.hexaly

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hexaly 插件异步求解协程作用域 / Hexaly plugin async solver coroutine scope */
internal val pluginSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)