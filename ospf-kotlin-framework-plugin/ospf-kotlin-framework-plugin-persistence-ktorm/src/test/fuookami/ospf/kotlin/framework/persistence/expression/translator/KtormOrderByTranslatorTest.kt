/**
 * Ktorm 排序翻译器测试
 * Ktorm OrderBy Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.select
import org.ktorm.expression.OrderType
import org.ktorm.expression.SelectExpression
import org.ktorm.expression.UnaryExpression
import org.ktorm.expression.UnaryExpressionType
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrderSupport
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.expression.SortItem

@DisplayName("KtormOrderByTranslator Tests / Ktorm 排序翻译器测试")
class KtormOrderByTranslatorTest {
    private object Users : Table<Nothing>("users") {
        val id = int("id")
        val name = varchar("name")
    }

    private val resolver: KtormColumnResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "id" -> Users.id
            "name" -> Users.name
            else -> null
        }
    }

    private fun newQuery() = Database.connect("jdbc:sqlite::memory:").from(Users).select(Users.id)

    @Test
    @DisplayName("should apply nulls fallback when unsupported / 不支持时应启用 nulls 降级排序")
    fun shouldApplyNullsFallbackWhenUnsupported() {
        val translator = KtormOrderByTranslator(resolver, NullsOrderSupport.Never)
        val sortBy = SortBy(listOf(SortItem("name", SortDirection.Asc, NullsOrder.NullsLast)))

        val query = translator.apply(newQuery(), sortBy).value!!
        val selectExpr = query.expression as SelectExpression

        assertEquals(2, selectExpr.orderBy.size)
        val nullOrder = selectExpr.orderBy[0]
        val directionOrder = selectExpr.orderBy[1]

        assertEquals(OrderType.ASCENDING, directionOrder.orderType)
        val unary = nullOrder.expression as UnaryExpression<*>
        assertEquals(UnaryExpressionType.IS_NULL, unary.type)
        assertEquals(OrderType.ASCENDING, nullOrder.orderType)
    }

    @Test
    @DisplayName("should not add nulls fallback when supported / 支持时不应注入 nulls 降级排序")
    fun shouldNotAddNullsFallbackWhenSupported() {
        val translator = KtormOrderByTranslator(resolver, NullsOrderSupport.Always)
        val sortBy = SortBy.desc("name", NullsOrder.NullsFirst)

        val query = translator.apply(newQuery(), sortBy).value!!
        val selectExpr = query.expression as SelectExpression

        assertEquals(1, selectExpr.orderBy.size)
        assertEquals(OrderType.DESCENDING, selectExpr.orderBy[0].orderType)
    }

    @Test
    @DisplayName("should support multi sort ordering / 应支持多字段排序")
    fun shouldSupportMultiSortOrdering() {
        val translator = KtormOrderByTranslator(resolver, NullsOrderSupport.Never)
        val sortBy = SortBy.asc("id").thenDesc("name", NullsOrder.NullsFirst)

        val query = translator.apply(newQuery(), sortBy).value!!
        val selectExpr = query.expression as SelectExpression

        // id asc + (name null-order fallback + name desc)
        assertEquals(3, selectExpr.orderBy.size)
        val nameNullOrder = selectExpr.orderBy[1].expression as UnaryExpression<*>
        assertTrue(nameNullOrder.type == UnaryExpressionType.IS_NULL)
        assertEquals(OrderType.DESCENDING, selectExpr.orderBy[1].orderType)
        assertEquals(OrderType.DESCENDING, selectExpr.orderBy[2].orderType)
    }
}