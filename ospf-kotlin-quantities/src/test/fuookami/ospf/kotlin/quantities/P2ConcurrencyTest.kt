package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.dimension.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull

class P2ConcurrencyTest {
    @Test
    fun `unitSystem_concurrentReadWriteCacheShouldBeSafe`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)

        // Concurrent reads and writes to SI derived cache
        repeat(100) { i ->
            executor.submit {
                try {
                    // Read: get standard unit
                    val forceUnit = SI.standardUnitForDimension(Force)
                    assertNotNull(forceUnit)

                    // Read: derive a new unit (populates cache)
                    val velocityQuantity = Velocity
                    val velocityUnit = SI.unitForDimension(velocityQuantity)
                    assertNotNull(velocityUnit)

                    // Write: set standard unit
                    if (i % 10 == 0) {
                        SI.setStandardUnit(Velocity, MeterPerSecond)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // Verify cache integrity
        val velocityUnit = SI.unitForDimension(Velocity)
        assertNotNull(velocityUnit)
    }

    @Test
    fun `unitSystem_builderShouldBeThreadSafe`() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(50)
        val systems = mutableListOf<UnitSystem>()
        val lock = Any()

        repeat(50) { i ->
            executor.submit {
                try {
                    val builder = UnitSystemBuilder("Custom$i")
                        .withBaseUnit(StandardFundamentalQuantityDimension.Length, Meter)
                        .withBaseUnit(StandardFundamentalQuantityDimension.Mass, Kilogram)
                        .withBaseUnit(StandardFundamentalQuantityDimension.Time, Second)

                    if (i % 2 == 0) {
                        builder.withStandardUnit(Area, SquareMeter)
                    }

                    val system = builder.build()
                    synchronized(lock) {
                        systems.add(system)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        // All systems should be built successfully
        assert(systems.size == 50)
    }
}