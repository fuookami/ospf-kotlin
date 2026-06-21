package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.test.*
import org.junit.jupiter.api.Test

class RemoteSolverFailureDetailTest {
    @Test
    fun detailShouldContainAllFields() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.TASK_FAILED_HARD_TIMEOUT,
            message = "Task exceeded hard timeout",
            metadata = mapOf("traceId" to "abc-123"),
            httpStatus = 200,
            taskId = "task-1",
            sliceId = "slice-1",
            requestId = "req-1"
        )

        assertEquals(RemoteSolverErrorCode.TASK_FAILED_HARD_TIMEOUT, detail.code)
        assertEquals("Task exceeded hard timeout", detail.message)
        assertEquals("abc-123", detail.metadata["traceId"])
        assertEquals(200, detail.httpStatus)
        assertEquals("task-1", detail.taskId)
        assertEquals("slice-1", detail.sliceId)
        assertEquals("req-1", detail.requestId)
    }

    @Test
    fun detailShouldHaveDefaultEmptyMetadata() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.INTERNAL_ERROR,
            message = "Internal error"
        )

        assertTrue(detail.metadata.isEmpty())
        assertNull(detail.httpStatus)
        assertNull(detail.taskId)
        assertNull(detail.sliceId)
        assertNull(detail.requestId)
    }

    @Test
    fun toApiCodeShouldReturnCodeName() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.NO_ELIGIBLE_NODE_AVAILABLE,
            message = "No eligible node"
        )

        assertEquals("NO_ELIGIBLE_NODE_AVAILABLE", detail.toApiCode())
    }

    @Test
    fun toReasonCodeShouldReturnCodeName() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.STORAGE_IO_FAILED,
            message = "Storage IO failed"
        )

        assertEquals("STORAGE_IO_FAILED", detail.toReasonCode())
    }

    @Test
    fun allRemoteSolverErrorCodesShouldBeCovered() {
        // Verify that all error codes can be used in detail
        RemoteSolverErrorCode.entries.forEach { code ->
            val detail = RemoteSolverFailureDetail(
                code = code,
                message = "Test message for $code"
            )
            assertEquals(code, detail.code)
            assertEquals(code.name, detail.toApiCode())
        }
    }
}
