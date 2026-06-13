/**
 * Ktorm 标量表达式翻译器测试
 * Ktorm Scalar Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.Table
import org.ktorm.schema.int
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

@DisplayName("KtormScalarTranslator Tests / Ktorm 标量翻译器测试")
class KtormScalarTranslatorTest {
    private object Items : Table<Nothing>("items") {
        val price = int("price")
        val quantity = int("quantity")
    }

    private val resolver: KtormColumnResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "price" -> Items.price
            "quantity" -> Items.quantity
            else -> null
        }
    }

    @Test
    @DisplayName("should translate reference constant and arithmetic / 应翻译引用常量与算术表达式")
    fun shouldTranslateReferenceConstantAndArithmetic() {
        val translator = KtormScalarTranslator(resolver)
        val reference = translator.translate(ScalarReference<Int>(PropertyPath.parse("price")))
        val constant = translator.translate(ScalarConstant(100))
        val arithmetic = translator.translate(
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            )
        ) as BinaryExpression<*>

        assertNotNull(reference)
        assertTrueArgument(constant)
        assertEquals(BinaryExpressionType.TIMES, arithmetic.type)
    }

    @Test
    @DisplayName("unsupported scalar should follow policy / 不支持标量应遵循策略")
    fun unsupportedScalarShouldFollowPolicy() {
        val alwaysFalseTranslator = KtormScalarTranslator(resolver)
        val failFastTranslator = KtormScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)

        assertNull(alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown"))))
        assertThrows(IllegalArgumentException::class.java) {
            failFastTranslator.translate(ScalarCustom<Int>("x"))
        }
    }

    @Test
    @DisplayName("should translate standard functions / 应翻译标准函数")
    fun shouldTranslateStandardFunctions() {
        val translator = KtormScalarTranslator(resolver)
        val absExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference<Int>(PropertyPath.parse("price")))
            )
        ) as FunctionExpression<*>
        val lowerExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Lower,
                listOf(ScalarConstant("ABC"))
            )
        ) as FunctionExpression<*>

        assertEquals("ABS", absExpr.functionName)
        assertEquals("LOWER", lowerExpr.functionName)
    }

    @Test
    @DisplayName("unknown function should follow policy / 未知函数应遵循策略")
    fun unknownFunctionShouldFollowPolicy() {
        val alwaysFalseTranslator = KtormScalarTranslator(resolver)
        val failFastTranslator = KtormScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unknown = ScalarFunction("unknown", listOf(ScalarConstant(1)))

        assertNull(alwaysFalseTranslator.translate(unknown))
        assertThrows(IllegalArgumentException::class.java) {
            failFastTranslator.translate(unknown)
        }
    }

    private fun assertTrueArgument(expr: org.ktorm.expression.ScalarExpression<*>?) {
        assertNotNull(expr)
        assertEquals(ArgumentExpression::class, expr!!::class)
    }
}
