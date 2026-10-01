/**
 * MongoDB 标量表达式翻译器测试
 * MongoDB Scalar Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

@DisplayName("MongoScalarTranslator Tests / MongoDB 标量翻译器测试")
class MongoScalarTranslatorTest {
    private val resolver = MongoFieldNameResolver { path: String ->
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
        val alwaysFalseTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.AlwaysFalse)
        val failFastTranslator = MongoScalarTranslator(resolver)
        val unresolved = alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown")))
        val failed = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(unresolved.failed)
        val detail = (((unresolved as Failed<*, *, *>).error as ExErr<*, *>).value as UnsupportedPredicateDetail)
        assertEquals(UnsupportedPredicatePolicy.AlwaysFalse, detail.policy)
        assertTrue(failed.failed)
    }

    @Test
    @DisplayName("fail fast detail should contain correct fields / FailFast detail 应包含正确字段")
    fun failFastDetailShouldContainCorrectFields() {
        val failFastTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val result = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(result.failed)
        assertTrue(result is Failed)

        val failed = result as Failed<*, *, *>
        val error = failed.error
        assertTrue(error is ExErr<*, *>)
        @Suppress("UNCHECKED_CAST")
        val exErr = error as ExErr<*, UnsupportedPredicateDetail>
        val detail = exErr.value

        assertEquals("ScalarExpression", detail.expressionType)
        assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
        assertEquals("MongoDB", detail.backendName)
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
        val alwaysFalseTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.AlwaysFalse)
        val failFastTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unknown = ScalarFunction("unknown", listOf(ScalarConstant(1)))
        val unsupported = alwaysFalseTranslator.translate(unknown)
        val failed = failFastTranslator.translate(unknown)

        assertTrue(unsupported.failed)
        assertTrue(failed.failed)
    }

    @Test
    @DisplayName("recursive unsupported scalar should preserve its failure / 递归不支持标量应保留失败")
    fun recursiveUnsupportedScalarShouldPreserveFailure() {
        val translator = MongoScalarTranslator(resolver)
        val expression = ScalarBinary(
            BinaryOperator.Multiply,
            ScalarCustom<Int>("x"),
            ScalarConstant(2)
        )

        assertTrue(translator.translate(expression).failed)
    }

    @Test
    @DisplayName("resolver exceptions should fail and cancellation should pass through / 解析器异常应失败且取消异常继续抛出")
    fun resolverExceptionsShouldFailAndCancellationShouldPassThrough() {
        val expression = ScalarReference<Int>(PropertyPath.parse("price"))
        val failingTranslator = MongoScalarTranslator(MongoFieldNameResolver { throw IllegalStateException("resolver") })
        val cancellingTranslator = MongoScalarTranslator(
            MongoFieldNameResolver { throw CancellationException("cancelled") }
        )

        assertTrue(failingTranslator.translate(expression).failed)
        assertThrows(CancellationException::class.java) {
            cancellingTranslator.translate(expression)
        }
    }

    @Test
    @DisplayName("later resolver errors should outrank always-false markers / 后续解析器错误应优先于恒假标记")
    fun laterResolverErrorsShouldOutrankAlwaysFalseMarkers() {
        val translator = MongoScalarTranslator(
            MongoFieldNameResolver { path: String ->
                if (path == "broken") throw IllegalStateException("resolver")
                path
            },
            UnsupportedPredicatePolicy.AlwaysFalse
        )
        val brokenReference = ScalarReference<Int>(PropertyPath.parse("broken"))
        val binary = ScalarBinary(
            BinaryOperator.Add,
            ScalarCustom<Int>("unsupported"),
            brokenReference
        )
        val function = ScalarFunction(
            ScalarFunctionNames.Coalesce,
            listOf(ScalarCustom<Int>("unsupported"), brokenReference)
        )

        assertTrue(translator.translate(binary).failed)
        assertTrue(translator.translate(function).failed)
    }
}
