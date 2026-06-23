package fuookami.ospf.kotlin.framework.persistence.expression

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr

/**
 * 不支持谓词详情测试。
 * Unsupported predicate detail tests.
 */
class UnsupportedPredicateDetailTest {
    @Test
    /** 验证 FailFast 详情包含正确字段 / Verify FailFast detail has correct fields */
    fun failFastDetailShouldHaveCorrectFields() {
        val detail = UnsupportedPredicateDetail.failFast(
            expressionType = "BooleanCustom",
            reason = "Custom expressions not supported",
            backendName = "MyBatis"
        )

        assertEquals("BooleanCustom", detail.expressionType)
        assertEquals("Custom expressions not supported", detail.reason)
        assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
        assertEquals("MyBatis", detail.backendName)
    }

    @Test
    /** 验证 ClientFilter 详情包含正确字段 / Verify ClientFilter detail has correct fields */
    fun clientFilterDetailShouldHaveCorrectFields() {
        val detail = UnsupportedPredicateDetail.clientFilter(
            expressionType = "PatternMatch",
            reason = "Regex not supported",
            backendName = "MongoDB"
        )

        assertEquals("PatternMatch", detail.expressionType)
        assertEquals("Regex not supported", detail.reason)
        assertEquals(UnsupportedPredicatePolicy.ClientFilter, detail.policy)
        assertEquals("MongoDB", detail.backendName)
    }

    @Test
    /** 验证 FailFast 转错误返回 IllegalArgument / Verify FailFast to error returns IllegalArgument */
    fun failFastToErrorShouldReturnIllegalArgument() {
        val detail = UnsupportedPredicateDetail.failFast(
            expressionType = "ScalarFunction",
            reason = "ABS not supported",
            backendName = "Ktorm"
        )
        val error = detail.toError()

        assertEquals(ErrorCode.IllegalArgument, error.code)
        assertTrue(error.message.contains("ScalarFunction"))
        assertTrue(error.message.contains("ABS not supported"))
        assertTrue(error.message.contains("Ktorm"))
        assertTrue(error.message.contains("FailFast"))
    }

    @Test
    /** 验证 ClientFilter 转错误返回 ApplicationFailed / Verify ClientFilter to error returns ApplicationFailed */
    fun clientFilterToErrorShouldReturnApplicationFailed() {
        val detail = UnsupportedPredicateDetail.clientFilter(
            expressionType = "PatternMatch",
            reason = "Regex not supported",
            backendName = "MyBatis"
        )
        val error = detail.toError()

        assertEquals(ErrorCode.ApplicationFailed, error.code)
        assertTrue(error.message.contains("PatternMatch"))
        assertTrue(error.message.contains("Regex not supported"))
        assertTrue(error.message.contains("MyBatis"))
        assertTrue(error.message.contains("ClientFilter"))
    }

    @Test
    /** 验证错误包含详情作为值 / Verify error contains detail as value */
    fun errorShouldContainDetailAsValue() {
        val detail = UnsupportedPredicateDetail.failFast(
            expressionType = "BooleanCustom",
            reason = "Not supported",
            backendName = "MongoDB"
        )
        val error = detail.toError()

        assertTrue(error is ExErr<*, *>)
        assertEquals(detail, (error as ExErr<*, *>).value)
    }

    @Test
    /** 验证 AlwaysFalse 转错误返回 ApplicationError / Verify AlwaysFalse to error returns ApplicationError */
    fun alwaysFalseToErrorShouldReturnApplicationError() {
        val detail = UnsupportedPredicateDetail(
            expressionType = "BooleanCustom",
            reason = "Not supported",
            policy = UnsupportedPredicatePolicy.AlwaysFalse,
            backendName = "Ktorm"
        )
        val error = detail.toError()

        assertEquals(ErrorCode.ApplicationError, error.code)
        assertTrue(error.message.contains("BooleanCustom"))
        assertTrue(error.message.contains("Not supported"))
        assertTrue(error.message.contains("Ktorm"))
        assertTrue(error.message.contains("AlwaysFalse"))
    }

    @Test
    /** 验证所有策略可测试 / Verify all policies are testable */
    fun allPoliciesShouldBeTestable() {
        // Verify that all three policies can create details
        val failFast = UnsupportedPredicateDetail.failFast("Expr", "reason", "backend")
        assertEquals(UnsupportedPredicatePolicy.FailFast, failFast.policy)

        val clientFilter = UnsupportedPredicateDetail.clientFilter("Expr", "reason", "backend")
        assertEquals(UnsupportedPredicatePolicy.ClientFilter, clientFilter.policy)

        val alwaysFalse = UnsupportedPredicateDetail("Expr", "reason", UnsupportedPredicatePolicy.AlwaysFalse, "backend")
        assertEquals(UnsupportedPredicatePolicy.AlwaysFalse, alwaysFalse.policy)
    }
}
