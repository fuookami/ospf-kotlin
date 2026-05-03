package fuookami.ospf.kotlin.core.solver.output

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

class SolverOutputWithIISTest {
    @Test
    fun solverOutputShouldWrapOptionalIis() {
        val output = FeasibleSolverOutput<Flt64>(
            obj = Flt64.one,
            solution = emptyList(),
            time = 1.seconds,
            possibleBestObj = Flt64.one,
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
