package fuookami.ospf.kotlin.core.solver

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitCancellation
import fuookami.ospf.kotlin.core.solver.report.CancellationSource
import fuookami.ospf.kotlin.core.solver.report.SolveHandle
import fuookami.ospf.kotlin.utils.functional.ok

/** CompletableFuture cancellation regression tests. / CompletableFuture 取消回归测试。 */
class CoreSolverAsyncTest {
    @Test
    fun futureCancellationRequestsTokenAndInvokesNativeListener() {
        val listenerCalled = CountDownLatch(1)
        var source: CancellationSource? = null
        val handle = SolveHandle.create { record ->
            source = record.source
            listenerCalled.countDown()
            ok
        }
        val future = cancellableSolveFuture(handle.token) {
            awaitCancellation()
        }

        assertTrue(future.cancel(true))
        assertTrue(listenerCalled.await(2, TimeUnit.SECONDS))
        assertTrue(handle.token.isCancellationRequested)
        assertEquals(CancellationSource.Future, source)
    }
}
