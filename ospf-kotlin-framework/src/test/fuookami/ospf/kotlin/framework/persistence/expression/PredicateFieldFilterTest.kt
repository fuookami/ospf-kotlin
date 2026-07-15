/**
 * 谓词字段过滤提取测试
 * Predicate field filter extraction tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.expression.*

@DisplayName("Predicate Field Filter Tests / 谓词字段过滤提取测试")
class PredicateFieldFilterTest {
    data class User(
        val age: Int,
        val status: String,
        val deletedAt: String?
    )

    @Test
    @DisplayName("Eq filters parse AND equality predicates / 等值过滤解析 AND 等值谓词")
    fun eqFiltersShouldParseAndEqualityPredicates() {
        val expr = and(
            eq(User::status, "active"),
            eq(User::age, 18)
        )

        assertEquals(mapOf("status" to "active", "age" to 18), expr.eqFilters())
    }

    @Test
    @DisplayName("Eq filters reject range predicates / 等值过滤拒绝范围谓词")
    fun eqFiltersShouldRejectRangePredicates() {
        val expr = and(
            eq(User::status, "active"),
            ge(User::age, 18)
        )

        assertNull(expr.eqFilters())
    }

    @Test
    @DisplayName("Eq or in filters parse equality and IN predicates / 等值或 IN 过滤解析等值和 IN 谓词")
    fun eqOrInFiltersShouldParseEqualityAndInPredicates() {
        val expr = and(
            inValues(User::status, listOf("active", "pending")),
            eq(User::age, 18)
        )

        assertEquals(
            mapOf(
                "status" to listOf("active", "pending"),
                "age" to 18
            ),
            expr.eqOrInFilters()
        )
    }

    @Test
    @DisplayName("Eq or in filters reject not-in predicates / 等值或 IN 过滤拒绝 not-in 谓词")
    fun eqOrInFiltersShouldRejectNotInPredicates() {
        val expr = notInValues(User::status, listOf("deleted"))

        assertNull(expr.eqOrInFilters())
    }

    @Test
    @DisplayName("Field filters parse range, IN and null checks / 字段过滤解析范围、IN 和空值检查")
    fun fieldFiltersShouldParseRangeInAndNullChecks() {
        val expr = and {
            ge(User::age, 18)
            le(User::age, 65)
            inValues(User::status, listOf("active", "pending"))
            isNull(User::deletedAt)
        }

        val filters = expr.fieldFilters()

        assertNotNull(filters)
        assertEquals(18, filters!!["age"]?.ge)
        assertEquals(65, filters["age"]?.le)
        assertEquals(listOf("active", "pending"), filters["status"]?.inValues)
        assertEquals(true, filters["deletedAt"]?.isNull)
    }

    @Test
    @DisplayName("Field filters flip comparison when constant is on left / 常量在左侧时字段过滤反转比较方向")
    fun fieldFiltersShouldFlipComparisonWhenConstantIsOnLeft() {
        val expr = Comparison(
            ComparisonOperator.Le,
            ScalarConstant(18),
            ScalarReference(PropertyPath.parse("age"))
        )

        val filters = expr.fieldFilters()

        assertEquals(18, filters?.get("age")?.ge)
    }
}
