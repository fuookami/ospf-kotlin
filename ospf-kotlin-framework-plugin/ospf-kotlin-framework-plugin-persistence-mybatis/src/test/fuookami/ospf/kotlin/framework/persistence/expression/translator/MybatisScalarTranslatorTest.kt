/**
 * MyBatis 标量表达式翻译器测试
 * MyBatis Scalar Translator Tests
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
        val reference = translator.translate(ScalarReference<Int>(PropertyPath.parse("price")))!!
        val constant = translator.translate(ScalarConstant(100))!!
        val arithmetic = translator.translate(
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            )
        )!!

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

        assertNull(alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown"))))
        assertThrows(IllegalArgumentException::class.java) {
            failFastTranslator.translate(ScalarCustom<Int>("x"))
        }
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
        )!!
        val lowerExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Lower,
                listOf(ScalarConstant("ABC"))
            )
        )!!

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

        assertNull(alwaysFalseTranslator.translate(unknown))
        assertThrows(IllegalArgumentException::class.java) {
            failFastTranslator.translate(unknown)
        }
    }
}

