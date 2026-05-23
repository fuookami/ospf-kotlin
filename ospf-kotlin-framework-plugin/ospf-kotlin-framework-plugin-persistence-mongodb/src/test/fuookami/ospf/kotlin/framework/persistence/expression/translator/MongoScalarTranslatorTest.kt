/**
 * MongoDB 标量表达式翻译器测试
 * MongoDB Scalar Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.math.symbol.expression.BinaryOperator
import fuookami.ospf.kotlin.math.symbol.expression.PropertyPath
import fuookami.ospf.kotlin.math.symbol.expression.ScalarBinary
import fuookami.ospf.kotlin.math.symbol.expression.ScalarConstant
import fuookami.ospf.kotlin.math.symbol.expression.ScalarCustom
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunction
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunctionNames
import fuookami.ospf.kotlin.math.symbol.expression.ScalarReference
import org.bson.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
        val reference = translator.translate(ScalarReference<Int>(PropertyPath.parse("price")))
        val constant = translator.translate(ScalarConstant(100))
        val arithmetic = translator.translate(
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            )
        ) as Document

        assertEquals("\$price", reference)
        assertEquals(100, constant)
        assertEquals(listOf("\$price", "\$quantity"), arithmetic["\$multiply"])
    }

    @Test
    @DisplayName("unsupported scalar should follow policy / 不支持标量应遵循策略")
    fun unsupportedScalarShouldFollowPolicy() {
        val alwaysFalseTranslator = MongoScalarTranslator(resolver)
        val failFastTranslator = MongoScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)

        assertNull(alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown"))))
        assertThrows(IllegalArgumentException::class.java) {
            failFastTranslator.translate(ScalarCustom<Int>("x"))
        }
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
        ) as Document
        val lowerExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Lower,
                listOf(ScalarReference<String>(PropertyPath.parse("price")))
            )
        ) as Document
        val coalesceExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Coalesce,
                listOf(ScalarReference<String>(PropertyPath.parse("price")), ScalarConstant("fallback"))
            )
        ) as Document

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

        assertNull(alwaysFalseTranslator.translate(unknown))
        assertThrows(IllegalArgumentException::class.java) {
            failFastTranslator.translate(unknown)
        }
    }
}

