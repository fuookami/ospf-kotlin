/**
 * 更新赋值模型测试
 * Update Assignment Model Tests
 *
 * 验收标准：
 * 1. UpdateAssignments 支持多字段更新
 * 2. 支持 SetValue、SetNull、SetFromExpression
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("UpdateAssignment Tests / 更新赋值模型测试")
class UpdateAssignmentTest {
    data class User(
        val status: String,
        val deletedAt: String?,
        val count: Int
    )

    @Nested
    @DisplayName("UpdateAssignments Creation Tests / UpdateAssignments 创建测试")
    inner class CreationTests {

        @Test
        @DisplayName("Create empty assignments / 创建空赋值")
        fun testEmptyAssignments() {
            val assignments = UpdateAssignments.empty
            assertTrue(assignments.isEmpty())
            assertFalse(assignments.isNotEmpty())
        }

        @Test
        @DisplayName("Create set value / 创建设置值")
        fun testSetValue() {
            val assignments = UpdateAssignments.set("status", "active")

            assertFalse(assignments.isEmpty())
            assertEquals(1, assignments.items.size)

            val item = assignments.items[0] as SetValue
            assertEquals("status", item.path)
            assertEquals("active", item.value)
        }

        @Test
        @DisplayName("Create set null / 创建设置 NULL")
        fun testSetNull() {
            val assignments = UpdateAssignments.setNull("deletedAt")

            assertEquals(1, assignments.items.size)
            assertTrue(assignments.items[0] is SetNull)
            assertEquals("deletedAt", assignments.items[0].path)
        }

        @Test
        @DisplayName("Create set from expression / 创建从表达式设置")
        fun testSetFromExpression() {
            val expr: ScalarExpression<Int> = ScalarConstant(42)
            val assignments = UpdateAssignments.setExpr("count", expr)

            assertEquals(1, assignments.items.size)
            val item = assignments.items[0] as SetFromExpression
            assertEquals("count", item.path)
            assertTrue(item.expression is ScalarConstant<*>)
        }

        @Test
        @DisplayName("Create assignments from properties / 从属性创建赋值")
        fun testPropertyAssignments() {
            val expr: ScalarExpression<Int> = ScalarConstant(42)
            val set = UpdateAssignments.set(User::status, "inactive")
            val setNull = UpdateAssignments.setNull(User::deletedAt)
            val setExpr = UpdateAssignments.setExpr(User::count, expr)

            assertEquals(UpdateAssignments.set("status", "inactive"), set)
            assertEquals(UpdateAssignments.setNull("deletedAt"), setNull)
            assertEquals(UpdateAssignments.setExpr("count", expr), setExpr)
        }
    }

    @Nested
    @DisplayName("UpdateAssignments Combination Tests / UpdateAssignments 组合测试")
    inner class CombinationTests {

        @Test
        @DisplayName("Combine assignments with plus / 使用 plus 组合赋值")
        fun testCombineWithPlus() {
            val assignments = UpdateAssignments.set("status", "inactive") +
                              UpdateAssignments.setNull("deletedAt")

            assertEquals(2, assignments.items.size)
            assertEquals("status", assignments.items[0].path)
            assertEquals("deletedAt", assignments.items[1].path)
        }

        @Test
        @DisplayName("Chain assignments with then* methods / 使用 then* 方法链式赋值")
        fun testChainAssignments() {
            val assignments = UpdateAssignments.set("name", "test")
                .thenSet("status", "active")
                .thenSetNull("deletedAt")

            assertEquals(3, assignments.items.size)
            assertEquals("name", assignments.items[0].path)
            assertEquals("status", assignments.items[1].path)
            assertEquals("deletedAt", assignments.items[2].path)
        }

        @Test
        @DisplayName("Chain assignments with properties / 使用属性链式赋值")
        fun testChainPropertyAssignments() {
            val expr: ScalarExpression<Int> = ScalarConstant(7)
            val assignments = UpdateAssignments.set(User::status, "inactive")
                .thenSetNull(User::deletedAt)
                .thenSetExpr(User::count, expr)

            assertEquals(3, assignments.items.size)
            assertEquals("status", assignments.items[0].path)
            assertEquals("deletedAt", assignments.items[1].path)
            assertEquals("count", assignments.items[2].path)
        }
    }

    @Nested
    @DisplayName("UpdateAssignment Type Tests / UpdateAssignment 类型测试")
    inner class TypeTests {

        @Test
        @DisplayName("SetValue with different types / SetValue 不同类型")
        fun testSetWithDifferentTypes() {
            val stringAssign = UpdateAssignments.set("name", "test")
            val intAssign = UpdateAssignments.set("age", 25)
            val doubleAssign = UpdateAssignments.set("score", 98.5)
            val boolAssign = UpdateAssignments.set("active", true)

            assertEquals("test", (stringAssign.items[0] as SetValue).value)
            assertEquals(25, (intAssign.items[0] as SetValue).value)
            assertEquals(98.5, (doubleAssign.items[0] as SetValue).value)
            assertEquals(true, (boolAssign.items[0] as SetValue).value)
        }

        @Test
        @DisplayName("SetValue with null / SetValue 空值")
        fun testSetWithNull() {
            val assignments = UpdateAssignments.set("optional", null)

            val item = assignments.items[0] as SetValue
            assertNull(item.value)
        }

        @Test
        @DisplayName("SetFromExpression with constant / SetFromExpression 常量表达式")
        fun testSetExprWithConstant() {
            val expr: ScalarExpression<String> = ScalarConstant("computed")
            val assignments = UpdateAssignments.setExpr("computedField", expr)

            val item = assignments.items[0] as SetFromExpression
            assertTrue(item.expression is ScalarConstant<*>)
            assertEquals("computed", (item.expression as ScalarConstant<*>).value)
        }
    }
}
