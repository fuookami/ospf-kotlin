/**
 * 组合数学异步作用域
 * Combinatorics Async Scope
 *
 * 为组合数学模块提供协程异步计算的共享作用域。
 * Provides a shared coroutine scope for asynchronous computation in the combinatorics module.
 */
package fuookami.ospf.kotlin.math.combinatorics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val combinatoricsAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
