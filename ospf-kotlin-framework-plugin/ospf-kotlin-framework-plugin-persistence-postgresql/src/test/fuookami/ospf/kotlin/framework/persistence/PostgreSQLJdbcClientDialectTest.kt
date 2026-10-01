/**
 * PostgreSQL JdbcClient 方言测试 / PostgreSQL JdbcClient dialect tests
*/
package fuookami.ospf.kotlin.framework.persistence

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection

class PostgreSQLJdbcClientDialectTest {
    @Test
    fun pluginCreatesLazyClientAndSupportsExactLookupLifecycle() {
        val missing = PostgreSQL.initJdbcClient { password = "must-not-appear" }
        assertTrue(missing.failed)
        assertTrue(!(missing as Failed<*, *, *>).error.message.contains("must-not-appear"))

        val name = "postgres-plugin-test-${System.nanoTime()}"
        val initialized = PostgreSQL.initJdbcClient {
            url = "localhost:5432"
            this.name = name
            database = "test"
            userName = "user"
            password = "password"
        }
        val key = PostgreSQLClientKey(name, "test")
        assertTrue(initialized.ok)
        assertTrue(PostgreSQL.getJdbcClient(key).ok)
        assertTrue(PostgreSQL.getJdbcClient(name).ok)
        assertTrue(PostgreSQL.close(key).ok)
        assertTrue(PostgreSQL.getJdbcClient(key).failed)
        assertTrue(PostgreSQL.close(key).ok)
    }

    @Test
    fun dialectQuotesIdentifiersAndBindsPagination() {
        assertEquals("\"odd\"\"name\"", PostgreSQLJdbcClientDialect.quoteIdentifier("odd\"name").value)

        val page = PostgreSQLJdbcClientDialect.pagination(limit = 9, offset = 3)
        assertTrue(page.ok)
        assertEquals("LIMIT ? OFFSET ?", page.value?.sql)
        assertEquals(listOf(9, 3), page.value?.parameters)
        assertTrue(PostgreSQLJdbcClientDialect.pagination(limit = 2, offset = -1).failed)

        val order = PostgreSQLJdbcClientDialect.orderNulls("\"rank\"", SortDirection.Asc, NullsOrder.NullsLast)
        assertTrue(order.ok)
        assertEquals("\"rank\" ASC NULLS LAST", order.value?.sql)
    }
}
