package fuookami.ospf.kotlin.core.error

import java.time.Duration
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

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

    // ========================================================================
    // 命名错误子类型测试
    // Named error subclass tests
    // ========================================================================

    @Test
    /** 验证 SolverNotFoundError 包含正确的错误码和消息 / Verify SolverNotFoundError has correct code and message */
    fun solverNotFoundErrorShouldHaveCorrectCodeAndMessage() {
        val error = SolverNotFoundError("gurobi")
        assertEquals(ErrorCode.SolverNotFound, error.code)
        assertEquals("No solver valid: gurobi", error.message)
        assertEquals("gurobi", error.solver)
    }

    @Test
    /** 验证无 solver 名称时 SolverNotFoundError 正常工作 / Verify SolverNotFoundError works without solver name */
    fun solverNotFoundErrorShouldWorkWithoutSolverName() {
        val error = SolverNotFoundError()
        assertEquals(ErrorCode.SolverNotFound, error.code)
        assertEquals("No solver valid.", error.message)
        assertNull(error.solver)
    }

    @Test
    /** 验证 SolverNotFoundError 可通过 when 断言 / Verify SolverNotFoundError is assertable via when */
    fun solverNotFoundErrorShouldBeAssertableViaWhen() {
        val error: Error<*> = SolverNotFoundError()
        assertTrue(error is SolverNotFoundError)
    }

    @Test
    /** 验证 SolverEnvironmentLostError 包含正确的错误码和消息 / Verify SolverEnvironmentLostError has correct code and message */
    fun solverEnvironmentLostErrorShouldHaveCorrectCodeAndMessage() {
        val error = SolverEnvironmentLostError("GRB license expired")
        assertEquals(ErrorCode.OREngineEnvironmentLost, error.code)
        assertEquals("GRB license expired", error.message)
        assertEquals("GRB license expired", error.detail)
    }

    @Test
    /** 验证无详情时 SolverEnvironmentLostError 正常工作 / Verify SolverEnvironmentLostError works without detail */
    fun solverEnvironmentLostErrorShouldWorkWithoutDetail() {
        val error = SolverEnvironmentLostError()
        assertEquals(ErrorCode.OREngineEnvironmentLost, error.code)
        assertEquals("Solver environment lost.", error.message)
        assertNull(error.detail)
    }

    @Test
    /** 验证 SolverSolvingError 包含正确的错误码和消息 / Verify SolverSolvingError has correct code and message */
    fun solverSolvingErrorShouldHaveCorrectCodeAndMessage() {
        val error = SolverSolvingError("Numerical instability")
        assertEquals(ErrorCode.OREngineSolvingException, error.code)
        assertEquals("Numerical instability", error.message)
        assertEquals("Numerical instability", error.detail)
    }

    @Test
    /** 验证 SolverModelingError 包含正确的错误码和消息 / Verify SolverModelingError has correct code and message */
    fun solverModelingErrorShouldHaveCorrectCodeAndMessage() {
        val error = SolverModelingError("Invalid constraint")
        assertEquals(ErrorCode.OREngineModelingException, error.code)
        assertEquals("Invalid constraint", error.message)
        assertEquals("Invalid constraint", error.detail)
    }

    @Test
    /** 验证 SolverTerminatedError 包含正确的错误码 / Verify SolverTerminatedError has correct code */
    fun solverTerminatedErrorShouldHaveCorrectCode() {
        val error = SolverTerminatedError()
        assertEquals(ErrorCode.OREngineTerminated, error.code)
        assertEquals("Solver terminated.", error.message)
    }

    @Test
    /** 验证 SolverTimeoutError 包含正确的错误码和时长 / Verify SolverTimeoutError has correct code and duration */
    fun solverTimeoutErrorShouldHaveCorrectCodeAndDuration() {
        val duration = Duration.ofSeconds(30)
        val error = SolverError.Timeout(duration)
        assertEquals(ErrorCode.OREngineTerminated, error.errorCode)
        assertEquals(duration, error.duration)
        assertTrue(error.message.contains("30"))
        assertTrue(error.message.contains("PT30S"))
    }

    // ========================================================================
    // SolverFailureSupport 工厂函数测试
    // SolverFailureSupport factory function tests
    // ========================================================================

    @Test
    /** 验证 solverNotFound 工厂方法返回命名错误 / Verify solverNotFound factory returns named error */
    fun solverNotFoundFactoryShouldReturnNamedError() {
        val result = solverNotFound("cplex")
        assertTrue(result is Failed)
        val error = (result as Failed).error
        assertTrue(error is SolverNotFoundError)
        assertEquals("cplex", (error as SolverNotFoundError).solver)
    }

    @Test
    /** 验证 solverEnvironmentLost 工厂方法返回命名错误 / Verify solverEnvironmentLost factory returns named error */
    fun solverEnvironmentLostFactoryShouldReturnNamedError() {
        val result = solverEnvironmentLost("Connection timeout")
        assertTrue(result is Failed)
        val error = (result as Failed).error
        assertTrue(error is SolverEnvironmentLostError)
        assertEquals("Connection timeout", (error as SolverEnvironmentLostError).detail)
    }

    @Test
    /** 验证 solverSolvingException 工厂方法返回命名错误 / Verify solverSolvingException factory returns named error */
    fun solverSolvingExceptionFactoryShouldReturnNamedError() {
        val result = solverSolvingException("MIP gap not reached")
        assertTrue(result is Failed)
        val error = (result as Failed).error
        assertTrue(error is SolverSolvingError)
        assertEquals("MIP gap not reached", (error as SolverSolvingError).detail)
    }

    @Test
    /** 验证 solverModelingException 工厂方法返回命名错误 / Verify solverModelingException factory returns named error */
    fun solverModelingExceptionFactoryShouldReturnNamedError() {
        val result = solverModelingException("Duplicate variable")
        assertTrue(result is Failed)
        val error = (result as Failed).error
        assertTrue(error is SolverModelingError)
        assertEquals("Duplicate variable", (error as SolverModelingError).detail)
    }

    @Test
    /** 验证 solverTerminated 工厂方法返回命名错误 / Verify solverTerminated factory returns named error */
    fun solverTerminatedFactoryShouldReturnNamedError() {
        val result = solverTerminated()
        assertTrue(result is Failed)
        val error = (result as Failed).error
        assertTrue(error is SolverTerminatedError)
    }
}
