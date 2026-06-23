/**
 * Ktorm 布尔表达式翻译器测试
 * Ktorm Boolean Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.expression.InListExpression
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.int
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed

@DisplayName("KtormBooleanTranslator Tests / Ktorm 布尔翻译器测试")
class KtormBooleanTranslatorTest {
    data class Entity(val age: Int)

    private object Users : Table<Nothing>("users") {
        val id = int("id")
        val age = int("age")
        val name = varchar("name")
        val status = varchar("status")
        val widthValue = int("width_value")
        val widthUnitSymbol = varchar("width_unit_symbol")
    }

    private val resolver: KtormColumnResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "id" -> Users.id
            "age" -> Users.age
            "name" -> Users.name
            "status" -> Users.status
            "widthValue" -> Users.widthValue
            "widthUnitSymbol" -> Users.widthUnitSymbol
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

            val gt = translator.translate(gtExpr).valueOrFail().orFail() as BinaryExpression<Boolean>
            val le = translator.translate(leExpr).valueOrFail().orFail() as BinaryExpression<Boolean>

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

            val translated = translator.translate(expr).valueOrFail().orFail() as BinaryExpression<Boolean>
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

            val columnColumnResult = translator.translate(columnColumn).valueOrFail().orFail() as BinaryExpression<Boolean>
            val arithmeticResult = translator.translate(arithmetic).valueOrFail().orFail() as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.GREATER_THAN, columnColumnResult.type)
            assertEquals(BinaryExpressionType.GREATER_THAN, arithmeticResult.type)
            assertEquals(BinaryExpressionType.TIMES, (arithmeticResult.left as BinaryExpression<*>).type)
        }

        @Test
        @DisplayName("should translate function comparison / 应翻译函数比较")
        fun shouldTranslateFunctionComparison() {
            val expr = Comparison(
                ComparisonOperator.Gt,
                ScalarFunction(
                    ScalarFunctionNames.Abs,
                    listOf(ScalarReference<Int>(PropertyPath.parse("age")))
                ),
                ScalarConstant(10)
            )

            val result = translator.translate(expr).valueOrFail().orFail() as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.GREATER_THAN, result.type)
            assertEquals("ABS", (result.left as FunctionExpression<*>).functionName)
        }

        @Test
        @DisplayName("should translate property predicate / 应翻译属性谓词")
        fun shouldTranslatePropertyPredicate() {
            val expr = prop(Entity::age) gt 18

            val result = translator.translate(expr).valueOrFail().orFail() as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.GREATER_THAN, result.type)
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

            val inResult = translator.translate(inExpr).valueOrFail().orFail() as InListExpression
            val notInResult = translator.translate(notInExpr).valueOrFail().orFail() as InListExpression

            assertEquals(false, inResult.notInList)
            assertEquals(true, notInResult.notInList)
            assertEquals(2, inResult.values?.size)
        }
    }

    @Nested
    @DisplayName("Fallback Tests / 降级策略测试")
    inner class FallbackTests {
        @Test
        @DisplayName("should require explicit PO field path / 应仅接受显式 PO 字段路径")
        fun shouldRequireExplicitPoFieldPath() {
            val poExpr = Comparison(
                ComparisonOperator.Gt,
                ScalarReference(PropertyPath.parse("widthValue")),
                ScalarConstant(10)
            )
            val domainExpr = Comparison(
                ComparisonOperator.Gt,
                ScalarReference(PropertyPath.parse("width")),
                ScalarConstant(10)
            )

            val poTranslated = translator.translate(poExpr).valueOrFail().orFail() as BinaryExpression<Boolean>
            val domainTranslated = translator.translate(domainExpr).valueOrFail().orFail() as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.GREATER_THAN, poTranslated.type)
            assertEquals(BinaryExpressionType.EQUAL, domainTranslated.type)
        }

        @Test
        @DisplayName("unsupported path should become always false / 未知路径应转为恒假条件")
        fun unsupportedPathShouldBecomeAlwaysFalse() {
            val expr = Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("unknown")),
                ScalarConstant(1)
            )

            val translated = translator.translate(expr).valueOrFail().orFail() as BinaryExpression<Boolean>
            assertEquals(BinaryExpressionType.EQUAL, translated.type)
        }

        @Test
        @DisplayName("boolean false and custom should become always false / false 与 custom 应转恒假")
        fun booleanFalseAndCustomShouldBecomeAlwaysFalse() {
            val falseExpr = BooleanConstant(Trivalent.False)
            val customExpr = BooleanCustom("x")

            val falseResult = translator.translate(falseExpr).valueOrFail().orFail() as BinaryExpression<Boolean>
            val customResult = translator.translate(customExpr).valueOrFail().orFail() as BinaryExpression<Boolean>

            assertEquals(BinaryExpressionType.EQUAL, falseResult.type)
            assertEquals(BinaryExpressionType.EQUAL, customResult.type)
        }

        @Test
        @DisplayName("fail fast should return failed for unsupported predicate / FailFast 应对不支持谓词返回失败")
        fun failFastShouldReturnFailedForUnsupportedPredicate() {
            val failFastTranslator = KtormBooleanTranslator(
                resolver,
                unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
            )

            val result = failFastTranslator.translate(BooleanCustom("x"))
            assertTrue(result.failed)
        }

        @Test
        @DisplayName("fail fast detail should contain correct fields / FailFast detail 应包含正确字段")
        fun failFastDetailShouldContainCorrectFields() {
            val failFastTranslator = KtormBooleanTranslator(
                resolver,
                unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
            )
            val result = failFastTranslator.translate(BooleanCustom("x"))

            assertTrue(result.failed)
            assertTrue(result is Failed<*, *, *>)

            val failed = result as Failed<*, *, *>
            val error = failed.error
            assertTrue(error is ExErr<*, *>)
            val exErr = error as ExErr<*, UnsupportedPredicateDetail>
            val detail = exErr.value

            assertTrue(detail.expressionType.contains("Custom"))
            assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
            assertEquals("Ktorm", detail.backendName)
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

            val andResult = translator.translate(andExpr).valueOrFail().orFail() as BinaryExpression<Boolean>
            val orResult = translator.translate(orExpr).valueOrFail().orFail() as BinaryExpression<Boolean>
            val notResult = translator.translate(notExpr).valueOrFail().orFail() as UnaryExpression<Boolean>

            assertEquals(BinaryExpressionType.AND, andResult.type)
            assertEquals(BinaryExpressionType.OR, orResult.type)
            assertEquals(UnaryExpressionType.NOT, notResult.type)
        }

        @Test
        @DisplayName("null check should use unary operators / null 检查应使用一元表达式")
        fun nullCheckShouldUseUnaryOperators() {
            val isNull = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNull)
            val isNotNull = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNotNull)

            val left = translator.translate(isNull).valueOrFail().orFail() as UnaryExpression<Boolean>
            val right = translator.translate(isNotNull).valueOrFail().orFail() as UnaryExpression<Boolean>

            assertNotNull(left)
            assertNotNull(right)
            assertTrue(left.type == UnaryExpressionType.IS_NULL)
            assertTrue(right.type == UnaryExpressionType.IS_NOT_NULL)
        }
    }
}
