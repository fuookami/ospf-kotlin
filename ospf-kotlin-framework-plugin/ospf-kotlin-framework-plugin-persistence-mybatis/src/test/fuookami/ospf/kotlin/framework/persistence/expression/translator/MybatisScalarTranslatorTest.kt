/**
 * MyBatis 标量表达式翻译器测试
 * MyBatis Scalar Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

@DisplayName("MybatisScalarTranslator Tests / MyBatis 标量翻译器测试")
class MybatisScalarTranslatorTest {
    private val resolver: MybatisColumnNameResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "price" -> "price"
            "quantity" -> "quantity"
            else -> null
        }
    }

    @Test
    @DisplayName("should translate reference constant and arithmetic / 应翻译引用常量与算术表达式")
    fun shouldTranslateReferenceConstantAndArithmetic() {
        val translator = MybatisScalarTranslator(resolver)
        val reference = translator.translate(ScalarReference<Int>(PropertyPath.parse("price"))).value!!
        val constant = translator.translate(ScalarConstant(100)).value!!
        val arithmetic = translator.translate(
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            )
        ).value!!

        assertEquals("price", reference.sql)
        assertTrue(reference.isColumnOnly)
        assertEquals("{0}", constant.sql)
        assertEquals(listOf(100), constant.params)
        assertEquals("(price * quantity)", arithmetic.sql)
        assertFalse(arithmetic.sql.contains("100"))
    }

    @Test
    @DisplayName("unsupported scalar should follow policy / 不支持标量应遵循策略")
    fun unsupportedScalarShouldFollowPolicy() {
        val alwaysFalseTranslator = MybatisScalarTranslator(resolver)
        val failFastTranslator = MybatisScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unresolved = alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown")))
        val failed = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(unresolved.ok)
        assertNull(unresolved.value)
        assertTrue(failed.failed)
    }

    @Test
    @DisplayName("fail fast detail should contain correct fields / FailFast detail 应包含正确字段")
    fun failFastDetailShouldContainCorrectFields() {
        val failFastTranslator = MybatisScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val result = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(result.failed)
        assertTrue(result is Failed<*, *, *>)

        val failed = result as Failed<*, *, *>
        val error = failed.error
        assertTrue(error is ExErr<*, *>)
        val exErr = error as ExErr<*, UnsupportedPredicateDetail>
        val detail = exErr.value

        assertEquals("ScalarExpression", detail.expressionType)
        assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
        assertEquals("MyBatis", detail.backendName)
    }

    @Test
    @DisplayName("should translate standard functions safely / 应安全翻译标准函数")
    fun shouldTranslateStandardFunctionsSafely() {
        val translator = MybatisScalarTranslator(resolver)
        val absExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference<Int>(PropertyPath.parse("price")))
            )
        ).value!!
        val lowerExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Lower,
                listOf(ScalarConstant("ABC"))
            )
        ).value!!

        assertEquals("ABS(price)", absExpr.sql)
        assertEquals("LOWER({0})", lowerExpr.sql)
        assertEquals(listOf("ABC"), lowerExpr.params)
        assertFalse(lowerExpr.sql.contains("ABC"))
    }

    @Test
    @DisplayName("unknown function should follow policy / 未知函数应遵循策略")
    fun unknownFunctionShouldFollowPolicy() {
        val alwaysFalseTranslator = MybatisScalarTranslator(resolver)
        val failFastTranslator = MybatisScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unknown = ScalarFunction("unknown", listOf(ScalarConstant(1)))
        val unsupported = alwaysFalseTranslator.translate(unknown)
        val failed = failFastTranslator.translate(unknown)

        assertTrue(unsupported.ok)
        assertNull(unsupported.value)
        assertTrue(failed.failed)
    }
}
