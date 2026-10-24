package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

class GenerationWidthCacheConcurrencyTest {
    @Test
    fun customWidthCallbacksRemainSerializedAcrossDifferentColdKeys() {
        val product = Product(
            id = productIdOf("p"),
            name = "p",
            width = listOf(Quantity(Flt64(0.3), Meter))
        )
        val firstEntered = CountDownLatch(1)
        val secondAttempted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondEntered = CountDownLatch(1)
        val calls = AtomicInteger()
        val cache = GenerationMaterialWidthIndexCache(
            baseIndex = GenerationWidthIndex.fromDemands(listOf(ProductDemand.roll(product, Quantity(Flt64.one, RollCountUnit)))),
            maxOverProduceLength = null,
            widthCheck = { _, _, _ ->
                if (calls.incrementAndGet() == 1) {
                    firstEntered.countDown()
                    assertTrue(release.await(10, TimeUnit.SECONDS))
                } else {
                    secondEntered.countDown()
                }
                true
            }
        )
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit<GenerationWidthIndex<Flt64>> { cache.get(material("a", 1.2), collector()) }
            assertTrue(firstEntered.await(10, TimeUnit.SECONDS))
            val second = pool.submit<GenerationWidthIndex<Flt64>> {
                secondAttempted.countDown()
                cache.get(material("b", 1.3), collector())
            }
            assertTrue(secondAttempted.await(10, TimeUnit.SECONDS))
            assertFalse(secondEntered.await(100, TimeUnit.MILLISECONDS))
            release.countDown()
            assertEquals(1, first.get(10, TimeUnit.SECONDS).entries.size)
            assertEquals(1, second.get(10, TimeUnit.SECONDS).entries.size)
            assertEquals(2, calls.get())
            val hitCollector = collector()
            cache.get(material("same-width", 1.2), hitCollector)
            assertEquals(Int64.one, hitCollector.report().statistics.materialWidthIndexCacheHits)
            assertEquals(2, calls.get())
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }

    private fun collector() = GenerationCollector<Flt64>(maxPlans = Int64(100), deadline = null)

    private fun material(id: String, width: Double) = Material(
        id = materialIdOf(id),
        name = id,
        widthRange = WidthRange(
            width = QuantityRange(
                lowerBound = Quantity(Flt64.zero, Meter),
                upperBound = Quantity(Flt64(width), Meter)
            ),
            step = Quantity(Flt64(0.01), Meter)
        )
    )
}
