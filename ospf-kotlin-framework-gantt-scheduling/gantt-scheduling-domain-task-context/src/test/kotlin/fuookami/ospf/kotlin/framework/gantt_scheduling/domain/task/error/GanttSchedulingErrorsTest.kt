package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.error

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.utils.error.ErrorCode

class GanttSchedulingErrorsTest {

    @Test
    fun capabilityErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingCapabilityError("parallel execution")
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported: parallel execution", error.message)
        assertEquals("parallel execution", error.capability)
    }

    @Test
    fun capabilityErrorShouldWorkWithoutCapability() {
        val error = GanttSchedulingCapabilityError()
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported.", error.message)
        assertNull(error.capability)
    }

    @Test
    fun capabilityErrorFactoryOfShouldCreateError() {
        val error = GanttSchedulingCapabilityError.of("batch processing")
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported: batch processing", error.message)
        assertEquals("batch processing", error.capability)
    }

    @Test
    fun capabilityErrorFactoryUnsupportedShouldCreateGenericError() {
        val error = GanttSchedulingCapabilityError.unsupported()
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Capability not supported.", error.message)
        assertNull(error.capability)
    }

    @Test
    fun lifecycleErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingLifecycleError("Task not initialized")
        assertEquals(ErrorCode.ApplicationError, error.code)
        assertEquals("Task not initialized", error.message)
        assertEquals("Task not initialized", error.detail)
    }

    @Test
    fun lifecycleErrorShouldWorkWithoutDetail() {
        val error = GanttSchedulingLifecycleError()
        assertEquals(ErrorCode.ApplicationError, error.code)
        assertEquals("Gantt Scheduling lifecycle error.", error.message)
        assertNull(error.detail)
    }

    @Test
    fun solvingErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingSolvingError("No feasible solution")
        assertEquals(ErrorCode.ApplicationFailed, error.code)
        assertEquals("No feasible solution", error.message)
        assertEquals("No feasible solution", error.detail)
    }

    @Test
    fun solvingErrorShouldWorkWithoutDetail() {
        val error = GanttSchedulingSolvingError()
        assertEquals(ErrorCode.ApplicationFailed, error.code)
        assertEquals("Gantt Scheduling solving error.", error.message)
        assertNull(error.detail)
    }

    @Test
    fun validationErrorShouldHaveCorrectCodeAndMessage() {
        val error = GanttSchedulingValidationError("Invalid time range")
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Invalid time range", error.message)
        assertEquals("Invalid time range", error.detail)
    }

    @Test
    fun validationErrorShouldWorkWithoutDetail() {
        val error = GanttSchedulingValidationError()
        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertEquals("Gantt Scheduling validation error.", error.message)
        assertNull(error.detail)
    }
}
