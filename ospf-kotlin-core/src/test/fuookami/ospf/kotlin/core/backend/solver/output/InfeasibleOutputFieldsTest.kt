@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.core.backend.solver.output

import fuookami.ospf.kotlin.core.backend.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.frontend.model.mechanism.ObjectCategory
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class InfeasibleOutputFieldsTest {
    @Test
    fun shouldUseLatestStatusWhenExists() {
        val latestStatus = SolvingStatus(
            solver = "test",
            solverConfig = SolverConfig(),
            objectCategory = ObjectCategory.Minimum,
            time = 5.seconds,
            obj = Flt64(10),
            possibleBestObj = Flt64(9),
            initialBestObj = Flt64(11),
            gap = Flt64(0.1),
            iterations = UInt64(12),
            nodeCount = UInt64(7),
            bestBound = Flt64(8),
            mipGap = Flt64(0.2),
            solveTime = 4.seconds
        )

        val fields = resolveInfeasibleUnifiedFields(
            latestStatus = latestStatus,
            fallbackSolveTime = 99.seconds
        )

        assertEquals(UInt64(12), fields.iterations)
        assertEquals(UInt64(7), fields.nodeCount)
        assertEquals(Flt64(8), fields.bestBound)
        assertEquals(Flt64(0.2), fields.mipGap)
        assertEquals(4.seconds, fields.solveTime)
    }

    @Test
    fun shouldFallbackToElapsedSolveTimeWithoutStatus() {
        val fields = resolveInfeasibleUnifiedFields(
            latestStatus = null,
            fallbackSolveTime = 33.seconds
        )

        assertEquals(null, fields.iterations)
        assertEquals(null, fields.nodeCount)
        assertEquals(null, fields.bestBound)
        assertEquals(null, fields.mipGap)
        assertEquals(33.seconds, fields.solveTime)
    }
}
