/**
 * MyBatis 更新翻译器测试
 * MyBatis Update Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments
import fuookami.ospf.kotlin.math.symbol.expression.ScalarConstant
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MybatisUpdateTranslator Tests / MyBatis 更新翻译器测试")
class MybatisUpdateTranslatorTest {
    data class TestEntity(val id: Long, val name: String?)

    private val resolver: MybatisColumnNameResolver = { it.substringAfterLast(".") }

    @Test
    @DisplayName("should translate set setNull setExpr / 应翻译 set setNull setExpr")
    fun shouldTranslateSetSetNullSetExpr() {
        val translator = MybatisUpdateTranslator<TestEntity>(resolver)
        val assignments = UpdateAssignments
            .set("name", "neo")
            .thenSetNull("deletedAt")
            .thenSetExpr("age", ScalarConstant(18))

        val wrapper = translator.apply(UpdateWrapper(), assignments)
        val sqlSet = wrapper.sqlSet

        assertNotNull(sqlSet)
        assertTrue(sqlSet!!.contains("name"))
        assertTrue(sqlSet.contains("deletedAt"))
        assertTrue(sqlSet.contains("age"))
    }
}
