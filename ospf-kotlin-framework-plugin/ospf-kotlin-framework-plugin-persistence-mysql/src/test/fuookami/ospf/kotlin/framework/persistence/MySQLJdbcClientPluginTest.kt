/**
 * MySQL JdbcClient 插件测试 / MySQL JdbcClient plugin tests
*/
package fuookami.ospf.kotlin.framework.persistence

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.apache.commons.dbcp2.BasicDataSource
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.RowMapper
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortDirection

class MySQLJdbcClientPluginTest {
    @Test
    fun builderReportsMissingConfigurationWithoutExposingPassword() {
        val result = MySQLConfigBuilder(password = "do-not-report-this").buildRet()

        assertTrue(result.failed)
        val failure = result as Failed<*, *, *>
        assertTrue(failure.error.message.contains("URL"))
        assertTrue(!failure.error.message.contains("do-not-report-this"))
    }

    @Test
    fun jdbcClientFactoryUsesManagedDatasourceAndKeepsKtormEntryPoint() {
        val config = MySQLConfig(
            url = "localhost:3306",
            name = "plugin-test-${System.nanoTime()}",
            database = "test",
            userName = "user",
            password = "password"
        )
        val clients = mysqlClients()
        val dataSource = StubDataSource()
        val previousDataSource = synchronized(MySQL) {
            clients.put(config.key, dataSource)
        }
        try {
            assertTrue(previousDataSource == null)

            val backendResult = MySQL.getJdbcClient(config)
            assertTrue(backendResult.ok)
            val backend = backendResult.value ?: error("MySQL JdbcClient backend was not created")
            assertTrue(MySQL.getJdbcClient(config.key).ok)

            val beforeKtorm = dataSource.connectionRequests.get()
            assertNotNull(MySQL(config))
            assertTrue(dataSource.connectionRequests.get() > beforeKtorm)

            val beforeJdbcClient = dataSource.connectionRequests.get()
            val queried = backend.client.sql("SELECT 1")
                .query(RowMapper { row, _ -> row.getInt(1) })
                .single()
            assertEquals(1, queried)
            assertTrue(dataSource.connectionRequests.get() > beforeJdbcClient)
            assertTrue(dataSource.preparedSql.contains("SELECT 1"))

            val missing = MySQL.getJdbcClient(MySQLClientKey(config.name, "missing"))
            assertTrue(missing.failed)
        } finally {
            synchronized(MySQL) {
                try {
                    assertTrue(MySQL.close(config.key).ok)
                    assertTrue(MySQL.close(config.key).ok)
                } finally {
                    if (previousDataSource == null) {
                        clients.remove(config.key)
                    } else {
                        clients[config.key] = previousDataSource
                    }
                }
            }
        }
        assertEquals(1, dataSource.closeRequests.get())
        assertTrue(MySQL.getJdbcClient(config.key).failed)
    }

    @Test
    fun mysqlDialectQuotesIdentifiersAndOrdersBoundPaginationParameters() {
        assertEquals("`odd``name`", MySQLJdbcClientDialect.quoteIdentifier("odd`name").value)

        val page = MySQLJdbcClientDialect.pagination(limit = 17, offset = 4)
        assertTrue(page.ok)
        assertEquals("LIMIT ? OFFSET ?", page.value?.sql)
        assertEquals(listOf(17, 4), page.value?.parameters)
        assertTrue(MySQLJdbcClientDialect.pagination(limit = null, offset = 4).failed)

        val nullsFirst = MySQLJdbcClientDialect.orderNulls("`value`", SortDirection.Desc, NullsOrder.NullsFirst)
        assertTrue(nullsFirst.ok)
        assertEquals("CASE WHEN (`value`) IS NULL THEN 0 ELSE 1 END ASC, `value` DESC", nullsFirst.value?.sql)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mysqlClients(): MutableMap<MySQLClientKey, BasicDataSource> {
        val field = MySQL::class.java.getDeclaredField("clients").apply { isAccessible = true }
        return field.get(MySQL) as MutableMap<MySQLClientKey, BasicDataSource>
    }

    private inner class StubDataSource : BasicDataSource() {
        val connectionRequests = AtomicInteger()
        val closeRequests = AtomicInteger()
        val preparedSql = mutableListOf<String>()

        override fun getConnection(): Connection {
            connectionRequests.incrementAndGet()
            return jdbcProxy(Connection::class.java) { method, args ->
                when (method.name) {
                    "getMetaData" -> metadata()
                    "getAutoCommit" -> true
                    "isReadOnly" -> false
                    "getTransactionIsolation" -> Connection.TRANSACTION_READ_COMMITTED
                    "getCatalog", "getSchema" -> "test"
                    "isClosed" -> false
                    "isValid" -> true
                    "prepareStatement" -> preparedStatement(args?.getOrNull(0) as String)
                    else -> jdbcDefault(method.returnType)
                }
            }
        }

        override fun close() {
            closeRequests.incrementAndGet()
        }

        private fun metadata(): DatabaseMetaData = jdbcProxy(DatabaseMetaData::class.java) { method, _ ->
            when (method.name) {
                "getDatabaseProductName" -> "MySQL"
                "getDatabaseProductVersion" -> "8.0.0"
                "getDatabaseMajorVersion" -> 8
                "getDatabaseMinorVersion" -> 0
                "getIdentifierQuoteString" -> "`"
                "supportsTransactions" -> true
                else -> jdbcDefault(method.returnType)
            }
        }

        private fun preparedStatement(sql: String): PreparedStatement {
            preparedSql += sql
            return jdbcProxy(PreparedStatement::class.java) { method, _ ->
                when (method.name) {
                    "executeQuery" -> resultSet()
                    "isClosed" -> false
                    else -> jdbcDefault(method.returnType)
                }
            }
        }

        private fun resultSet(): ResultSet {
            val emittedRow = AtomicBoolean(false)
            return jdbcProxy(ResultSet::class.java) { method, _ ->
                when (method.name) {
                    "next" -> !emittedRow.getAndSet(true)
                    "getInt" -> 1
                    "wasNull" -> false
                    "isClosed" -> false
                    else -> jdbcDefault(method.returnType)
                }
            }
        }
    }

    private fun <T> jdbcProxy(
        type: Class<T>,
        handler: (Method, Array<out Any?>?) -> Any?
    ): T {
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "toString" -> "${type.simpleName}TestDouble"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                else -> handler(method, args)
            }
        } as T
    }

    private fun jdbcDefault(returnType: Class<*>): Any? = when (returnType) {
        java.lang.Boolean.TYPE -> false
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Character.TYPE -> '\u0000'
        else -> null
    }
}
