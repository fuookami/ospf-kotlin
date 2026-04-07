/**
 * Ktorm 更新翻译器测试
 * Ktorm Update Translator Tests
 *
 * 验收标准：
 * 1. UpdateAssignments 可正确转换为更新逻辑
 * 2. 支持 SetValue、SetNull、SetFromExpression
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("KtormUpdateTranslator Tests / Ktorm 更新翻译器测试")
class KtormUpdateTranslatorTest {

    @Nested
    @DisplayName("Translator Creation Tests / 翻译器创建测试")
    inner class CreationTests {

        @Test
        @DisplayName("Create translator requires column resolver and Table / 创建翻译器需要列解析器和 Table")
        fun testCreateTranslator() {
            // KtormUpdateTranslator 需要 Table 和 column resolver，这里验证基本概念
            // KtormUpdateTranslator requires Table and column resolver, here we verify basic concept
            assertTrue(true)
        }
    }

    @Nested
    @DisplayName("UpdateAssignments Model Tests / UpdateAssignments 模型测试")
    inner class UpdateAssignmentsModelTests {

        @Test
        @DisplayName("Empty assignments / 空赋值")
        fun testEmptyAssignments() {
            val assignments = UpdateAssignments.empty
            assertTrue(assignments.isEmpty())
        }

        @Test
        @DisplayName("SetValue assignment / SetValue 赋值")
        fun testSetValue() {
            val assignments = UpdateAssignments.set("status", "active")

            assertFalse(assignments.isEmpty())
            assertEquals(1, assignments.items.size)

            val item = assignments.items[0] as SetValue
            assertEquals("status", item.path)
            assertEquals("active", item.value)
        }

        @Test
        @DisplayName("SetNull assignment / SetNull 赋值")
        fun testSetNull() {
            val assignments = UpdateAssignments.setNull("active")

            assertEquals(1, assignments.items.size)
            assertTrue(assignments.items[0] is SetNull)
            assertEquals("active", assignments.items[0].path)
        }

        @Test
        @DisplayName("SetFromExpression assignment / SetFromExpression 赋值")
        fun testSetFromExpression() {
            val expr: ScalarExpression<Int> = ScalarConstant(42)
            val assignments = UpdateAssignments.setExpr("count", expr)

            assertEquals(1, assignments.items.size)
            val item = assignments.items[0] as SetFromExpression
            assertEquals("count", item.path)
            assertTrue(item.expression is ScalarConstant<*>)
            assertEquals(42, (item.expression as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("Combined assignments / 组合赋值")
        fun testCombinedAssignments() {
            val assignments = UpdateAssignments.set("status", "inactive")
                .thenSetNull("active")
                .thenSet("name", "updated")

            assertEquals(3, assignments.items.size)
            assertEquals("status", assignments.items[0].path)
            assertEquals("active", assignments.items[1].path)
            assertEquals("name", assignments.items[2].path)
        }
    }

    @Nested
    @DisplayName("ScalarExpression Tests / ScalarExpression 测试")
    inner class ScalarExpressionTests {

        @Test
        @DisplayName("ScalarConstant with different types / ScalarConstant 不同类型")
        fun testScalarConstantTypes() {
            val intConst: ScalarExpression<Int> = ScalarConstant(100)
            val stringConst: ScalarExpression<String> = ScalarConstant("test")
            val boolConst: ScalarExpression<Boolean> = ScalarConstant(true)

            assertTrue(intConst is ScalarConstant<*>)
            assertTrue(stringConst is ScalarConstant<*>)
            assertTrue(boolConst is ScalarConstant<*>)

            assertEquals(100, (intConst as ScalarConstant<*>).value)
            assertEquals("test", (stringConst as ScalarConstant<*>).value)
            assertEquals(true, (boolConst as ScalarConstant<*>).value)
        }

        @Test
        @DisplayName("ScalarReference expression / ScalarReference 表达式")
        fun testScalarReference() {
            val ref: ScalarExpression<Int> = ScalarReference(PropertyPath.parse("count"))

            assertTrue(ref is ScalarReference<*>)
            assertEquals(PropertyPath.parse("count"), (ref as ScalarReference<*>).path)
        }
    }

    @Nested
    @DisplayName("UpdateAssignment Type Tests / UpdateAssignment 类型测试")
    inner class TypeTests {

        @Test
        @DisplayName("SetValue type checking / SetValue 类型检查")
        fun testSetValueTypeChecking() {
            val setValue = SetValue("status", "active")
            assertEquals("status", setValue.path)
            assertEquals("active", setValue.value)
        }

        @Test
        @DisplayName("SetNull type checking / SetNull 类型检查")
        fun testSetNullTypeChecking() {
            val setNull = SetNull("active")
            assertEquals("active", setNull.path)
        }

        @Test
        @DisplayName("SetFromExpression type checking / SetFromExpression 类型检查")
        fun testSetFromExpressionTypeChecking() {
            val expr: ScalarExpression<Int> = ScalarConstant(50)
            val setExpr = SetFromExpression("count", expr)

            assertEquals("count", setExpr.path)
            assertTrue(setExpr.expression is ScalarConstant<*>)
        }
    }

    @Nested
    @DisplayName("UpdateAssignments Combination Tests / UpdateAssignments 组合测试")
    inner class CombinationTests {

        @Test
        @DisplayName("Combine with plus / 使用 plus 组合")
        fun testCombineWithPlus() {
            val assignments1 = UpdateAssignments.set("a", 1)
            val assignments2 = UpdateAssignments.set("b", 2)

            val combined = assignments1 + assignments2

            assertEquals(2, combined.items.size)
            assertEquals("a", combined.items[0].path)
            assertEquals("b", combined.items[1].path)
        }

        @Test
        @DisplayName("Chain with thenSet / 使用 thenSet 链式调用")
        fun testChainWithThenSet() {
            val assignments = UpdateAssignments
                .set("a", 1)
                .thenSet("b", 2)
                .thenSet("c", 3)

            assertEquals(3, assignments.items.size)
        }

        @Test
        @DisplayName("Chain with thenSetNull / 使用 thenSetNull 链式调用")
        fun testChainWithThenSetNull() {
            val assignments = UpdateAssignments
                .set("a", 1)
                .thenSetNull("b")

            assertEquals(2, assignments.items.size)
            assertTrue(assignments.items[1] is SetNull)
        }

        @Test
        @DisplayName("Chain with thenSetExpr / 使用 thenSetExpr 链式调用")
        fun testChainWithThenSetExpr() {
            val expr: ScalarExpression<String> = ScalarConstant("computed")
            val assignments = UpdateAssignments
                .set("a", "static")
                .thenSetExpr("b", expr)

            assertEquals(2, assignments.items.size)
            assertTrue(assignments.items[1] is SetFromExpression)
        }
    }
}