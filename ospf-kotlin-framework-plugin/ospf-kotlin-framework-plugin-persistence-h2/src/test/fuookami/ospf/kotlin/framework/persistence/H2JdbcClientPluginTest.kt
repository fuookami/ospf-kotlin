/**
 * H2 JdbcClient 插件测试 / H2 JdbcClient plugin tests
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.RowMapper
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.JdbcClientColumnNameResolver
import fuookami.ospf.kotlin.framework.persistence.expression.JdbcClientRepository
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments
import fuookami.ospf.kotlin.math.symbol.expression.*

class H2JdbcClientPluginTest {
    private data class Record(val id: Int, val displayName: String?)

    private fun equal(path: String, value: Any?): BooleanExpression {
        return Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Any?>(PropertyPath.parse(path)),
            ScalarConstant(value)
        )
    }

    private fun <T> value(result: fuookami.ospf.kotlin.utils.functional.Ret<T>): T {
        assertTrue(result is fuookami.ospf.kotlin.utils.functional.Ok, "Expected Ok but got $result")
        return (result as fuookami.ospf.kotlin.utils.functional.Ok).value
    }

    @Test
    fun repositoryUsesManagedH2DialectForQuotedPathsNullOrderPaginationAndBindings() {
        val name = "h2-repository-test-${System.nanoTime()}"
        val initialized = H2.initJdbcClient {
            url = "jdbc:h2:mem:$name"
            this.name = name
        }
        val key = H2ClientKey(name)
        try {
            assertTrue(initialized.ok)
            val backend = initialized.value ?: error("H2 JdbcClient was not created")
            val client = backend.client
            client.sql("CREATE SCHEMA \"app\"").update()
            client.sql("CREATE TABLE \"app\".\"records\" (\"id\" INTEGER NOT NULL, \"select\" VARCHAR(100))").update()
            client.sql("INSERT INTO \"app\".\"records\" (\"id\", \"select\") VALUES (?, ?)")
                .params(1, "O'Reilly")
                .update()
            client.sql("INSERT INTO \"app\".\"records\" (\"id\", \"select\") VALUES (?, ?)")
                .params(listOf(2, null))
                .update()
            client.sql("INSERT INTO \"app\".\"records\" (\"id\", \"select\") VALUES (?, ?)")
                .params(3, "Alice")
                .update()

            val repository = JdbcClientRepository(
                backend = backend,
                tableName = "app.records",
                rowMapper = RowMapper { row, _ -> Record(row.getInt("id"), row.getString("select")) },
                resolveColumnName = JdbcClientColumnNameResolver { path ->
                    when (path) {
                        "id" -> "id"
                        "displayName" -> "select"
                        else -> null
                    }
                }
            )
            val unknownPredicates = listOf<BooleanExpression>(
                BooleanConstant.unknown(),
                NotExpression(BooleanConstant.unknown())
            )
            for (predicate in unknownPredicates) {
                assertEquals(0, value(repository.update(predicate, UpdateAssignments.set("displayName", "unsafe"))))
                assertEquals(0, value(repository.delete(predicate)))
            }
            assertEquals(3L, value(repository.count(BooleanConstant.true_())))
            assertEquals(
                listOf(1, 2, 3),
                value(
                    repository.find(
                        where = BooleanConstant.true_(),
                        sortBy = SortBy.asc("id"),
                        limit = null,
                        offset = null
                    )
                ).map { it.id }
            )
            assertEquals("O'Reilly", value(repository.find(equal("displayName", "O'Reilly"))).single().displayName)
            assertNull(value(repository.find(equal("id", 2))).single().displayName)

            val nullsLast = value(
                repository.find(
                    BooleanConstant.true_(),
                    SortBy.asc("displayName", NullsOrder.NullsLast),
                    null,
                    null
                )
            )
            assertEquals(listOf("Alice", "O'Reilly", null), nullsLast.map { it.displayName })

            val page = value(
                repository.find(
                    BooleanConstant.true_(),
                    SortBy.asc("displayName", NullsOrder.NullsLast),
                    1,
                    1
                )
            )
            assertEquals(listOf(Record(1, "O'Reilly")), page)
            assertEquals(listOf(Record(1, "O'Reilly")), value(repository.find(equal("displayName", "O'Reilly"))))
            assertTrue(value(repository.find(equal("displayName", "O'Reilly' OR 1=1 --"))).isEmpty())
            assertEquals(3L, value(repository.count(BooleanConstant.true_())))
            assertTrue(value(repository.exists(equal("id", 2))))
            assertFalse(value(repository.exists(equal("id", 99))))

            assertEquals(
                1,
                value(repository.update(equal("id", 1), UpdateAssignments.set("displayName", "Ada's record")))
            )
            assertEquals("Ada's record", value(repository.find(equal("id", 1))).single().displayName)
            assertEquals(1, value(repository.delete(equal("id", 2))))
            assertEquals(2L, value(repository.count(BooleanConstant.true_())))

            val ktorm = H2.getKtormBackend(key).value ?: error("H2 Ktorm backend was not found")
            val sharedCount = ktorm.database.useConnection { connection ->
                connection.prepareStatement("SELECT COUNT(*) FROM \"app\".\"records\"").use { statement ->
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getInt(1)
                    }
                }
            }
            assertEquals(2, sharedCount)
        } finally {
            assertTrue(H2.close(key).ok)
        }

        assertTrue(H2.getJdbcClient(key).failed)
    }

    @Test
    fun h2ClientConnectsSupportsLookupAndClosesPool() {
        val name = "h2-plugin-test-${System.nanoTime()}"
        val config = H2Config(
            url = "jdbc:h2:mem:$name",
            name = name
        )
        try {
            val initialized = H2.getJdbcClient(config)
            assertTrue(initialized.ok)
            val first = initialized.value ?: error("H2 JdbcClient was not created")
            first.client.sql("CREATE TABLE cache_probe (id INTEGER NOT NULL)").update()
            first.client.sql("INSERT INTO cache_probe (id) VALUES (1)").update()

            val byKey = H2.getJdbcClient(config.key)
            val byName = H2.getJdbcClient(name)
            assertTrue(byKey.ok)
            assertTrue(byName.ok)
            val count = byKey.value!!.client.sql("SELECT COUNT(*) FROM cache_probe")
                .query(RowMapper { row, _ -> row.getInt(1) })
                .single()
            assertEquals(1, count)
        } finally {
            assertTrue(H2.close(config.key).ok)
        }

        assertTrue(H2.getJdbcClient(config.key).failed)
        assertTrue(H2.close(config.key).ok)
    }

    @Test
    fun builderReturnsStructuredConfigurationFailures() {
        val missing = H2.initJdbcClient { password = "secret-value" }
        assertTrue(missing.failed)
        assertTrue(!(missing as Failed<*, *, *>).error.message.contains("secret-value"))

        val invalid = H2.initJdbcClient {
            url = "jdbc:postgresql://localhost/database"
            name = "invalid-h2-url"
        }
        assertTrue(invalid.failed)
    }

    @Test
    fun h2DialectQuotesIdentifiersAndCompilesNullOrdering() {
        assertEquals("\"odd\"\"name\"", H2JdbcClientDialect.quoteIdentifier("odd\"name").value)
        val page = H2JdbcClientDialect.pagination(limit = 6, offset = 2)
        assertTrue(page.ok)
        assertEquals("LIMIT ? OFFSET ?", page.value?.sql)
        assertEquals(listOf(6, 2), page.value?.parameters)
        val order = H2JdbcClientDialect.orderNulls("\"rank\"", SortDirection.Desc, NullsOrder.NullsFirst)
        assertTrue(order.ok)
        assertEquals("\"rank\" DESC NULLS FIRST", order.value?.sql)
    }
}
