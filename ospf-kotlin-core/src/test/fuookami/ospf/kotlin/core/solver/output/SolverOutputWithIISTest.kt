package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.toSolveReport
import kotlin.test.*
import fuookami.ospf.kotlin.core.solver.report.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.core.solver.report.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class SolverOutputWithIISTest {
    @Test
    fun solverOutputShouldWrapOptionalIis() {
        val output = SolverStatus.Feasible.toSolveReport(
            objective = Flt64.one,
            values = emptyList(),
            solveTime = 1.seconds,
            bestBound = Flt64.one,
            gap = Flt64.zero
        )

        val wrapped = output.withIIS("iis")
        val withoutIIS = output.withoutIIS()

        assertSame(output, wrapped.output)
        assertEquals("iis", wrapped.iis)
        assertSame(output, withoutIIS.output)
        assertNull(withoutIIS.iis)
    }
}
