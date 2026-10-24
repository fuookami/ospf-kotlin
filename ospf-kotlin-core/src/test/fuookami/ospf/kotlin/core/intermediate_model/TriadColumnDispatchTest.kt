package fuookami.ospf.kotlin.core.intermediate_model

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.model.intermediate.mapTriadColumns

class TriadColumnDispatchTest {
    @Test
    fun boundedChunksKeepColumnOrderForEmptySmallAndWideModels() = runBlocking {
        for (processors in listOf(1, 2, 4)) {
            for (amount in listOf(0, 1, 3, 257)) {
                val active = AtomicInteger()
                val peak = AtomicInteger()
                val result = mapTriadColumns(
                    itemCount = amount,
                    availableProcessors = processors
                ) { index ->
                    val count = active.incrementAndGet()
                    peak.accumulateAndGet(count, ::maxOf)
                    try {
                        if (amount > 3) Thread.sleep(1)
                        index * 3
                    } finally {
                        active.decrementAndGet()
                    }
                }
                assertEquals(List(amount) { it * 3 }, result)
                assertTrue(peak.get() <= maxOf(processors - 1, 1))
            }
        }
    }

    @Test
    fun workerFailuresStillPropagate() = runBlocking {
        assertFailsWith<IllegalStateException> {
            mapTriadColumns(itemCount = 100, availableProcessors = 4) { index ->
                if (index == 11) error("expected column failure")
                index
            }
        }
        Unit
    }
}
