/**
 * 核心求解器异步作用域 / Core solver async scope
*/
package fuookami.ospf.kotlin.core.solver

import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
import fuookami.ospf.kotlin.core.solver.report.CancellationToken

/**
 * 核心求解器共享的异步协程作用域，使用 [SupervisorJob] 和 [Dispatchers.Default]。
 * Shared async coroutine scope for core solvers, using [SupervisorJob] and [Dispatchers.Default].
*/
internal val coreSolverAsyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Create a future whose cancellation is forwarded to the solve token. /
 * 创建一个将取消动作转发给求解令牌的 Future。
 *
 * The token is requested before the coroutine cancellation callback completes, so native
 * terminate/interrupt listeners can stop a blocking backend promptly. /
 * 令牌会在协程取消回调完成前被请求，原生 terminate/interrupt 监听器可以及时停止阻塞后端。
 *
 * @param token 求解取消令牌 / Solve cancellation token
 * @param block 异步求解块 / Asynchronous solve block
 * @return 可取消求解 Future / Cancellable solve future
 */
internal fun <T> cancellableSolveFuture(
    token: CancellationToken,
    block: suspend () -> T
): CompletableFuture<T> {
    val future = coreSolverAsyncScope.future { block() }
    future.whenComplete { _, _ ->
        if (future.isCancelled) {
            token.request(
                source = CancellationSource.Future,
                reason = "CompletableFuture.cancel()"
            )
        }
    }
    return future
}
