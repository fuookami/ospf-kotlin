/**
 * MyBatis 排序翻译器测试
 * MyBatis OrderBy Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrderSupport
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MybatisOrderByTranslator Tests / MyBatis 排序翻译器测试")
class MybatisOrderByTranslatorTest {
    data class TestEntity(val id: Long, val name: String?)

    private val resolver: MybatisColumnNameResolver = { path: String -> path.substringAfterLast(".") }

    @Test
    @DisplayName("should translate multi-field order by / 应翻译多字段排序")
    fun shouldTranslateMultiFieldOrderBy() {
        val translator = MybatisOrderByTranslator<TestEntity>(resolver, NullsOrderSupport.Never)
        val sortBy = SortBy.asc("id").thenDesc("name", NullsOrder.NullsLast)

        val wrapper = translator.apply(QueryWrapper(), sortBy)
        val sql = wrapper.sqlSegment.uppercase()

        assertTrue(sql.contains("ORDER BY"))
        assertTrue(sql.contains("ID"))
        assertTrue(sql.contains("NAME"))
    }
}

