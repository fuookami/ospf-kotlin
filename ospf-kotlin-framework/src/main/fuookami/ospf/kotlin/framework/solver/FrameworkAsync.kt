/**
 * 框架异步作用域
 * Framework async scope
 *
 * 为框架层异步求解操作提供统一的协程作用域。
 * Provides a unified coroutine scope for framework-level async solving operations.
 */
package fuookami.ospf.kotlin.framework.solver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** 框架层共享异步协程作用域 / Shared async coroutine scope for framework layer */
internal val frameworkAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)