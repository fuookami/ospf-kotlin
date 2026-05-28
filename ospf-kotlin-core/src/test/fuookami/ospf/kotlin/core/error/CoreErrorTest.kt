package fuookami.ospf.kotlin.core.error

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed

class CoreErrorTest {
    @Test
    fun structuredErrorsShouldMapToExistingErrorCodeRet() {
        val error = SolverError.PrecisionLoss("BigDecimal cannot round-trip through Flt64")
        val coreError = error.asCoreError()

        assertEquals(ErrorCode.ORSolutionInvalid, coreError.errorCode)
        val failed = coreError.toFailed<Unit>()

        assertTrue(failed is Failed)
        assertEquals(ErrorCode.ORSolutionInvalid, failed.code)
        assertEquals("Precision loss: BigDecimal cannot round-trip through Flt64", failed.message)
    }
}
