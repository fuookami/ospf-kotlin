/**
 * MyBatis 更新翻译器测试
 * MyBatis Update Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments

@DisplayName("MybatisUpdateTranslator Tests / MyBatis 更新翻译器测试")
class MybatisUpdateTranslatorTest {
    data class TestEntity(val id: Long, val name: String?)

    private val resolver = MybatisColumnNameResolver { path: String -> path.substringAfterLast(".") }

    @Test
    @DisplayName("should translate set setNull setExpr / 应翻译 set setNull setExpr")
    fun shouldTranslateSetSetNullSetExpr() {
        val translator = MybatisUpdateTranslator<TestEntity>(resolver)
        val assignments = UpdateAssignments
            .set("name", "neo")
            .thenSetNull("deletedAt")
            .thenSetExpr("age", ScalarConstant(18))

        val wrapper = translator.translate(UpdateWrapper(), assignments).value!!
        val sqlSet = wrapper.sqlSet

        assertNotNull(sqlSet)
        assertTrue(sqlSet!!.contains("name"))
        assertTrue(sqlSet.contains("deletedAt"))
        assertTrue(sqlSet.contains("age"))
    }

    @Test
    @DisplayName("complex update expressions should bind constants / 复杂更新表达式应绑定常量参数")
    fun complexUpdateExpressionsShouldBindConstants() {
        val translator = MybatisUpdateTranslator<TestEntity>(resolver)
        val assignments = UpdateAssignments.setExpr(
            "age",
            ScalarBinary(
                BinaryOperator.Add,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarConstant(1)
            )
        )

        val result = translator.translate(UpdateWrapper(), assignments)
        val wrapper = result.value!!
        val sqlSet = wrapper.sqlSet!!

        assertTrue(sqlSet.contains("age"))
        assertFalse(sqlSet.contains("+ 1"))
        assertTrue(wrapper.paramNameValuePairs.values.contains(1))
    }

    @Test
    @DisplayName("invalid assignment should return failed result / 无效更新字段应返回失败")
    fun invalidAssignmentShouldReturnFailedResult() {
        val translator = MybatisUpdateTranslator<TestEntity>(MybatisColumnNameResolver { null })

        val result = translator.translate(UpdateWrapper(), UpdateAssignments.set("missing", "value"))

        assertTrue(result.failed)
    }
}
