@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class SolverOutputCompatibilityTest {
    @Test
    fun feasibleOutputShouldKeepLegacyAndUnifiedFieldsAligned() {
        val output = FeasibleSolverOutput(
            obj = Flt64(100),
            solution = listOf(Flt64.one),
            time = 12.seconds,
            possibleBestObj = Flt64(80),
            gap = Flt64(0.2),
            iterations = UInt64(10),
            nodeCount = UInt64(3),
            bestBound = Flt64(90)
        )

        assertEquals(output.gap, output.mipGap)
        assertEquals(output.time, output.solveTime)
        assertEquals(UInt64(10), output.iterations)
    }

    @Test
    fun solvingStatusShouldKeepLegacyAndUnifiedFieldsAligned() {
        val status = SolvingStatus(
            solver = "test",
            solverConfig = SolverConfig(),
            objectCategory = ObjectCategory.Minimum,
            time = 5.seconds,
            obj = Flt64(10),
            possibleBestObj = Flt64(9),
            initialBestObj = Flt64(11),
            gap = Flt64(0.1)
        )

        assertEquals(status.gap, status.mipGap)
        assertEquals(status.time, status.solveTime)
    }
}
