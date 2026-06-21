package fuookami.ospf.kotlin.framework.persistence.expression

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.error.ExErr

class UnsupportedPredicateDetailTest {
    @Test
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
