/**
 * Ktorm 关系查询方言快照测试 / Ktorm relational query dialect snapshot tests
 */
package fuookami.ospf.kotlin.framework.persistence.query

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.support.mysql.MySqlDialect
import fuookami.ospf.kotlin.framework.persistence.expression.resolveColumnWithDiagnostics
import fuookami.ospf.kotlin.math.symbol.expression.*

@DisplayName("KtormRelationalQueryDialect Tests / Ktorm 关系查询方言测试")
class KtormRelationalQueryDialectTest {
    private object Orders : Table<Nothing>("orders") {
        val id = int("id")
        val status = varchar("status")
    }

    private object Items : Table<Nothing>("order_items") {
        val orderId = int("order_id")
    }

    @Test
    @DisplayName("MySQL dialect compiles join and pagination / MySQL 方言应编译 Join 与分页")
    fun mysqlDialectCompilesJoinAndPagination() {
        val database = Database.connect(
            url = "jdbc:sqlite::memory:",
            dialect = MySqlDialect()
        )
        val sources = mapOf(
            "orders" to KtormQuerySource(
                source = QuerySource("orders"),
                table = Orders,
                resolveColumnDetailed = resolveColumnWithDiagnostics {
                    map("id", Orders.id)
                    map("status", Orders.status)
                },
                defaultColumns = listOf("id", "status")
            ),
            "items" to KtormQuerySource(
                source = QuerySource("items"),
                table = Items,
                resolveColumnDetailed = resolveColumnWithDiagnostics {
                    map("orderId", Items.orderId)
                },
                defaultColumns = listOf("orderId")
            )
        )
        val plan = RelationalQueryPlan(
            root = QuerySource("orders", "o"),
            joins = listOf(
                JoinSpec(
                    type = JoinType.Inner,
                    source = QuerySource("items", "i"),
                    condition = Comparison<Any?>(
                        operator = ComparisonOperator.Eq,
                        left = ScalarReference<Any?>(PropertyPath.parse("o.id")),
                        right = ScalarReference<Any?>(PropertyPath.parse("i.orderId"))
                    ),
                    cardinality = JoinCardinality.OneToMany
                )
            ),
            predicate = Comparison<Any?>(
                operator = ComparisonOperator.Eq,
                left = ScalarReference<Any?>(PropertyPath.parse("o.status")),
                right = ScalarConstant<Any?>("confirmed")
            ),
            projections = listOf(ProjectionSpec(ColumnRef("o", "id"))),
            distinct = true,
            orderBy = listOf(OrderSpec(ColumnRef("o", "id"))),
            page = PageSpec(limit = 10, offset = 5)
        )

        val result = KtormRelationalQueryCompiler(database, sources).compile(plan)

        assertTrue(result.ok)
        val sql = result.value!!.audit.sqlTemplate.lowercase()
        assertTrue(sql.contains("join"))
        assertTrue(sql.contains("limit"))
        assertTrue(
            sql.contains("offset") || Regex("limit\\s+[^\\s]+\\s*,\\s*[^\\s]+").containsMatchIn(sql),
            "MySQL pagination SQL must contain OFFSET or LIMIT offset,limit: $sql"
        )
    }
}
