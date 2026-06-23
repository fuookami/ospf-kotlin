package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode

/**
 * 调度错误类型测试。
 * Gantt scheduling error types tests.
 */
class GanttSchedulingErrorsTest {

    @Test
    /** 验证能力错误包含正确的错误码和消息 / Verify capability error has correct code and message */
    fun capabilityErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingCapabilityError("parallel execution")
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported: parallel execution", error.message)
        assertEquals("parallel execution", error.capability)
    }

    @Test
    /** 验证无能力名称时能力错误正常工作 / Verify capability error works without capability name */
    fun capabilityErrorShouldWorkWithoutCapability() {
        val error = GanttSchedulingCapabilityError()
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported.", error.message)
        assertNull(error.capability)
    }

    @Test
    /** 验证工厂方法 of 创建能力错误 / Verify factory method of creates capability error */
    fun capabilityErrorFactoryOfShouldCreateError() {
        val error = GanttSchedulingCapabilityError.of("batch processing")
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported: batch processing", error.message)
        assertEquals("batch processing", error.capability)
    }

    @Test
    /** 验证 unsupported 工厂方法创建通用错误 / Verify unsupported factory creates generic error */
    fun capabilityErrorFactoryUnsupportedShouldCreateGenericError() {
        val error = GanttSchedulingCapabilityError.unsupported()
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported.", error.message)
        assertNull(error.capability)
    }

    @Test
    /** 验证生命周期错误包含正确的错误码和消息 / Verify lifecycle error has correct code and message */
    fun lifecycleErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingLifecycleError("Task not initialized")
        assertEquals(ErrorCode.ApplicationError, error.code)
        assertEquals("Task not initialized", error.message)
        assertEquals("Task not initialized", error.detail)
    }

    @Test
    /** 验证无详情时生命周期错误正常工作 / Verify lifecycle error works without detail */
    fun lifecycleErrorShouldWorkWithoutDetail() {
        val error = GanttSchedulingLifecycleError()
        assertEquals(ErrorCode.ApplicationError, error.code)
        assertEquals("Gantt Scheduling lifecycle error.", error.message)
        assertNull(error.detail)
    }

    @Test
    /** 验证求解错误包含正确的错误码和消息 / Verify solving error has correct code and message */
    fun solvingErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingSolvingError("No feasible solution")
        assertEquals(ErrorCode.ApplicationFailed, error.code)
        assertEquals("No feasible solution", error.message)
        assertEquals("No feasible solution", error.detail)
    }

    @Test
    /** 验证无详情时求解错误正常工作 / Verify solving error works without detail */
    fun solvingErrorShouldWorkWithoutDetail() {
        val error = GanttSchedulingSolvingError()
        assertEquals(ErrorCode.ApplicationFailed, error.code)
        assertEquals("Gantt Scheduling solving error.", error.message)
        assertNull(error.detail)
    }

    @Test
    /** 验证验证错误包含正确的错误码和消息 / Verify validation error has correct code and message */
    fun validationErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingValidationError("Invalid time range")
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Invalid time range", error.message)
        assertEquals("Invalid time range", error.detail)
    }

    @Test
    /** 验证无详情时验证错误正常工作 / Verify validation error works without detail */
    fun validationErrorShouldWorkWithoutDetail() {
        val error = GanttSchedulingValidationError()
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Gantt Scheduling validation error.", error.message)
        assertNull(error.detail)
    }
}
