/**
 * Ktorm 布尔表达式翻译器测试
 * Ktorm Boolean Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.InListExpression
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

@DisplayName("KtormBooleanTranslator Tests / Ktorm 布尔翻译器测试")
class KtormBooleanTranslatorTest {
    private object Users : Table<Nothing>("users") {
        val id = int("id")
        val age = int("age")
        val name = varchar("name")
        val status = varchar("status")
    }

    private val resolver: KtormColumnResolver = { path ->
        when (path.substringAfterLast(".")) {
            "id" -> Users.id
            "age" -> Users.age
            "name" -> Users.name
            "status" -> Users.status
            else -> null
        }
    }

    private val translator = KtormBooleanTranslator(resolver)

    @Nested
    @DisplayName("Comparison Tests / 比较翻译测试")
    inner class ComparisonTests {
        @Test
        @DisplayName("should support lt/le/gt/ge / 应支持 lt/le/gt/ge")
        fun shouldSupportLtLeGtGe() {
            val gtExpr = Comparison(
                ComparisonOperator.Gt,
                ScalarReference(PropertyPath.parse("age")),
                ScalarConstant(18)
            )
            val leExpr = Comparison(
                ComparisonOperator.Le,
                ScalarReference(PropertyPath.parse("age")),
                ScalarConstant(65)
            )

            val gt = translator.translate(gtExpr) as BinaryExpression<Boolean>
            val le = translator.translate(leExpr) as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.GREATER_THAN, gt.type)
            assertEquals(BinaryExpressionType.LESS_THAN_OR_EQUAL, le.type)
        }

        @Test
        @DisplayName("should preserve operator for constant-column / 常量-列比较应保留操作符")
        fun shouldPreserveOperatorForConstantColumn() {
            val expr = Comparison(
                ComparisonOperator.Lt,
                ScalarConstant(10),
                ScalarReference(PropertyPath.parse("age"))
            )

            val translated = translator.translate(expr) as BinaryExpression<Boolean>
            assertEquals(BinaryExpressionType.LESS_THAN, translated.type)
        }

        @Test
        @DisplayName("should translate column-column and arithmetic comparison / 应翻译列-列与算术比较")
        fun shouldTranslateColumnColumnAndArithmeticComparison() {
            val columnColumn = Comparison<Int>(
                ComparisonOperator.Gt,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarReference<Int>(PropertyPath.parse("id"))
            )
            val arithmetic = Comparison<Int>(
                ComparisonOperator.Gt,
                ScalarBinary<Int>(
                    BinaryOperator.Multiply,
                    ScalarReference<Int>(PropertyPath.parse("age")),
                    ScalarReference<Int>(PropertyPath.parse("id"))
                ),
                ScalarConstant<Int>(25)
            )

            val columnColumnResult = translator.translate(columnColumn) as BinaryExpression<Boolean>
            val arithmeticResult = translator.translate(arithmetic) as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.GREATER_THAN, columnColumnResult.type)
            assertEquals(BinaryExpressionType.GREATER_THAN, arithmeticResult.type)
            assertEquals(BinaryExpressionType.TIMES, (arithmeticResult.left as BinaryExpression<*>).type)
        }
    }

    @Nested
    @DisplayName("In Tests / In 翻译测试")
    inner class InTests {
        @Test
        @DisplayName("should translate in and not in / 应翻译 in 与 not in")
        fun shouldTranslateInAndNotIn() {
            val inExpr = InExpression(
                ScalarReference(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("pending")),
                negated = false
            )
            val notInExpr = inExpr.copy(negated = true)

            val inResult = translator.translate(inExpr) as InListExpression
            val notInResult = translator.translate(notInExpr) as InListExpression

            assertEquals(false, inResult.notInList)
            assertEquals(true, notInResult.notInList)
            assertEquals(2, inResult.values?.size)
        }
    }

    @Nested
    @DisplayName("Fallback Tests / 降级策略测试")
    inner class FallbackTests {
        @Test
        @DisplayName("unsupported path should become always false / 未知路径应转为恒假条件")
        fun unsupportedPathShouldBecomeAlwaysFalse() {
            val expr = Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("unknown")),
                ScalarConstant(1)
            )

            val translated = translator.translate(expr) as BinaryExpression<Boolean>
            assertEquals(BinaryExpressionType.EQUAL, translated.type)
        }

        @Test
        @DisplayName("boolean false and custom should become always false / false 与 custom 应转恒假")
        fun booleanFalseAndCustomShouldBecomeAlwaysFalse() {
            val falseExpr = BooleanConstant(Trivalent.False)
            val customExpr = BooleanCustom("x")

            val falseResult = translator.translate(falseExpr) as BinaryExpression<Boolean>
            val customResult = translator.translate(customExpr) as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.EQUAL, falseResult.type)
            assertEquals(BinaryExpressionType.EQUAL, customResult.type)
        }

        @Test
        @DisplayName("fail fast should throw for unsupported predicate / FailFast 应对不支持谓词抛异常")
        fun failFastShouldThrowForUnsupportedPredicate() {
            val failFastTranslator = KtormBooleanTranslator(
                resolver,
                unsupportedPredicatePolicy = fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy.FailFast
            )

            assertThrows(IllegalArgumentException::class.java) {
                failFastTranslator.translate(BooleanCustom("x"))
            }
        }
    }

    @Nested
    @DisplayName("Logical Tests / 逻辑组合测试")
    inner class LogicalTests {
        @Test
        @DisplayName("and/or/not should build expression tree / and/or/not 应生成组合表达式")
        fun andOrNotShouldBuildExpressionTree() {
            val a = Comparison(
                ComparisonOperator.Gt,
                ScalarReference(PropertyPath.parse("age")),
                ScalarConstant(18)
            )
            val b = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNotNull)
            val andExpr = AndExpression(listOf(a, b))
            val orExpr = OrExpression(listOf(andExpr, BooleanConstant(Trivalent.True)))
            val notExpr = NotExpression(orExpr)

            val andResult = translator.translate(andExpr) as BinaryExpression<Boolean>
            val orResult = translator.translate(orExpr) as BinaryExpression<Boolean>
            val notResult = translator.translate(notExpr) as UnaryExpression<Boolean>

            assertEquals(BinaryExpressionType.AND, andResult.type)
            assertEquals(BinaryExpressionType.OR, orResult.type)
            assertEquals(UnaryExpressionType.NOT, notResult.type)
        }

        @Test
        @DisplayName("null check should use unary operators / null 检查应使用一元表达式")
        fun nullCheckShouldUseUnaryOperators() {
            val isNull = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNull)
            val isNotNull = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNotNull)

            val left = translator.translate(isNull) as UnaryExpression<Boolean>
            val right = translator.translate(isNotNull) as UnaryExpression<Boolean>

            assertNotNull(left)
            assertNotNull(right)
            assertTrue(left.type == UnaryExpressionType.IS_NULL)
            assertTrue(right.type == UnaryExpressionType.IS_NOT_NULL)
        }
    }
}
