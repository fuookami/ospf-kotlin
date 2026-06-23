package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 远程求解器失败详情测试。
 * Remote solver failure detail tests.
 */
class RemoteSolverFailureDetailTest {
    @Test
    /** 验证详情包含所有字段 / Verify detail contains all fields */
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
    /** 验证详情默认空元数据 / Verify detail has default empty metadata */
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
    /** 验证 toApiCode 返回错误码名称 / Verify toApiCode returns code name */
    fun toApiCodeShouldReturnCodeName() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.NO_ELIGIBLE_NODE_AVAILABLE,
            message = "No eligible node"
        )

        assertEquals("NO_ELIGIBLE_NODE_AVAILABLE", detail.toApiCode())
    }

    @Test
    /** 验证 toReasonCode 返回错误码名称 / Verify toReasonCode returns code name */
    fun toReasonCodeShouldReturnCodeName() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.STORAGE_IO_FAILED,
            message = "Storage IO failed"
        )

        assertEquals("STORAGE_IO_FAILED", detail.toReasonCode())
    }

    @Test
    /** 验证所有错误码被覆盖 / Verify all error codes are covered */
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

    @Test
    /** 验证失败结果包含详情 / Verify failed result contains detail */
    fun failedResultShouldContainDetailInExErrValue() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.TASK_FAILED_HARD_TIMEOUT,
            message = "Task exceeded hard timeout",
            metadata = mapOf("traceId" to "abc-123"),
            httpStatus = 200,
            taskId = "task-1",
            sliceId = "slice-1",
            requestId = "req-1"
        )

        val result: Ret<Unit> = Failed(ErrorCode.IllegalArgument, "Remote solver error", detail)

        assertTrue(result.failed)
        val error = (result as Failed<*, *, *>).error
        assertTrue(error is ExErr<*, *>)

        val exErr = error as ExErr<*, RemoteSolverFailureDetail>
        val extractedDetail = exErr.value

        assertEquals(RemoteSolverErrorCode.TASK_FAILED_HARD_TIMEOUT, extractedDetail.code)
        assertEquals("Task exceeded hard timeout", extractedDetail.message)
        assertEquals("abc-123", extractedDetail.metadata["traceId"])
        assertEquals(200, extractedDetail.httpStatus)
        assertEquals("task-1", extractedDetail.taskId)
        assertEquals("slice-1", extractedDetail.sliceId)
        assertEquals("req-1", extractedDetail.requestId)
    }

    @Test
    /** 验证最小详情可提取 / Verify minimal detail is extractable */
    fun failedResultWithMinimalDetailShouldBeExtractable() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.INTERNAL_ERROR,
            message = "Internal error"
        )

        val result: Ret<Unit> = Failed(ErrorCode.IllegalArgument, detail)

        assertTrue(result.failed)
        val error = (result as Failed<*, *, *>).error
        assertTrue(error is ExErr<*, *>)

        val exErr = error as ExErr<*, RemoteSolverFailureDetail>
        val extractedDetail = exErr.value

        assertEquals(RemoteSolverErrorCode.INTERNAL_ERROR, extractedDetail.code)
        assertEquals("Internal error", extractedDetail.message)
        assertTrue(extractedDetail.metadata.isEmpty())
        assertNull(extractedDetail.httpStatus)
        assertNull(extractedDetail.taskId)
        assertNull(extractedDetail.sliceId)
        assertNull(extractedDetail.requestId)
    }

    @Test
    /** 验证自定义错误码包含详情 / Verify custom error code contains detail */
    fun failedResultWithCustomErrorCodeShouldContainDetail() {
        val detail = RemoteSolverFailureDetail(
            code = RemoteSolverErrorCode.NO_ELIGIBLE_NODE_AVAILABLE,
            message = "No eligible node available"
        )

        val result: Ret<Unit> = Failed(
            ErrorCode.SolverNotFound,
            "No solver available",
            detail
        )

        assertTrue(result.failed)
        val error = (result as Failed<*, *, *>).error
        assertTrue(error is ExErr<*, *>)

        val exErr = error as ExErr<*, RemoteSolverFailureDetail>
        val extractedDetail = exErr.value

        assertEquals(RemoteSolverErrorCode.NO_ELIGIBLE_NODE_AVAILABLE, extractedDetail.code)
        assertEquals("No eligible node available", extractedDetail.message)
    }
}
