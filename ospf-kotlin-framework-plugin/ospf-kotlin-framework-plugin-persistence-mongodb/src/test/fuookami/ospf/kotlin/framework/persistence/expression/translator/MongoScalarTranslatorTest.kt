/**
 * MongoDB 标量表达式翻译器测试
 * MongoDB Scalar Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

@DisplayName("MongoScalarTranslator Tests / MongoDB 标量翻译器测试")
class MongoScalarTranslatorTest {
    private val resolver: MongoFieldNameResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "price" -> "price"
            "quantity" -> "quantity"
            else -> null
        }
    }

    @Test
    @DisplayName("should translate reference constant and arithmetic / 应翻译引用常量与算术表达式")
    fun shouldTranslateReferenceConstantAndArithmetic() {
        val translator = MongoScalarTranslator(resolver)
        val reference = translator.translate(ScalarReference<Int>(PropertyPath.parse("price"))).valueOrFail()
        val constant = translator.translate(ScalarConstant(100)).valueOrFail()
        val arithmetic = translator.translate(
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            )
        ).valueOrFail() as Document

        assertEquals("\$price", reference)
        assertEquals(100, constant)
        assertEquals(listOf("\$price", "\$quantity"), arithmetic["\$multiply"])
    }

    @Test
    @DisplayName("unsupported scalar should follow policy / 不支持标量应遵循策略")
    fun unsupportedScalarShouldFollowPolicy() {
        val alwaysFalseTranslator = MongoScalarTranslator(resolver)
        val failFastTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unresolved = alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown")))
        val failed = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(unresolved.ok)
        assertNull(unresolved.value)
        assertTrue(failed.failed)
    }

    @Test
    @DisplayName("should translate standard functions / 应翻译标准函数")
    fun shouldTranslateStandardFunctions() {
        val translator = MongoScalarTranslator(resolver)
        val absExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference<Int>(PropertyPath.parse("price")))
            )
        ).valueOrFail() as Document
        val lowerExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Lower,
                listOf(ScalarReference<String>(PropertyPath.parse("price")))
            )
        ).valueOrFail() as Document
        val coalesceExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Coalesce,
                listOf(ScalarReference<String>(PropertyPath.parse("price")), ScalarConstant("fallback"))
            )
        ).valueOrFail() as Document

        assertEquals("\$price", absExpr["\$abs"])
        assertEquals("\$price", lowerExpr["\$toLower"])
        assertEquals(listOf("\$price", "fallback"), coalesceExpr["\$ifNull"])
    }

    @Test
    @DisplayName("unknown function should follow policy / 未知函数应遵循策略")
    fun unknownFunctionShouldFollowPolicy() {
        val alwaysFalseTranslator = MongoScalarTranslator(resolver)
        val failFastTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unknown = ScalarFunction("unknown", listOf(ScalarConstant(1)))
        val unsupported = alwaysFalseTranslator.translate(unknown)
        val failed = failFastTranslator.translate(unknown)

        assertTrue(unsupported.ok)
        assertNull(unsupported.value)
        assertTrue(failed.failed)
    }
}
