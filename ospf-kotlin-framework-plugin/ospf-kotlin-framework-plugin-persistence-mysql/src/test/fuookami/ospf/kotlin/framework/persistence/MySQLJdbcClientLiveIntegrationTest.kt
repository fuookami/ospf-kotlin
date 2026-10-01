package fuookami.ospf.kotlin.framework.persistence

import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.RowMapper
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.JdbcClientColumnNameResolver
import fuookami.ospf.kotlin.framework.persistence.expression.JdbcClientRepository
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments

class MySQLJdbcClientLiveIntegrationTest {
    private data class Record(val id: Int, val name: String?, val status: String, val amount: BigDecimal?)

    @Test
    fun repositoryRunsAgainstLiveMySQLWithScopedCrudAndFailureSafety() {
        val host = System.getenv("OSPF_TEST_MYSQL_HOST")?.takeIf { it.isNotBlank() }
        Assumptions.assumeTrue(host != null, "Set OSPF_TEST_MYSQL_HOST to enable the live MySQL integration test")
        val port = System.getenv("OSPF_TEST_MYSQL_PORT")?.let {
            it.toIntOrNull() ?: error("OSPF_TEST_MYSQL_PORT must be numeric")
        } ?: 3306
        val userName = System.getenv("OSPF_TEST_MYSQL_USER") ?: error("OSPF_TEST_MYSQL_USER is required")
        val password = System.getenv("OSPF_TEST_MYSQL_PASSWORD") ?: error("OSPF_TEST_MYSQL_PASSWORD is required")
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val database = "ospf_test_$suffix"
        val adminConfig = MySQLConfig(
            url = "${host!!}:$port",
            name = "ospf-live-mysql-admin-$suffix",
            database = "information_schema",
            userName = userName,
            password = password
        )
        val admin = value(MySQL.getJdbcClient(adminConfig))
        val quotedDatabase = quoted(database)
        var databaseCreated = false
        var testConfig: MySQLConfig? = null

        try {
            val serverVersion = admin.client.sql("SELECT VERSION()").query(String::class.java).single()
            println("OSPF_LIVE_DATABASE_VERSION=mysql:$serverVersion")
            assertTrue(serverVersion.startsWith("8.4."), "Expected MySQL 8.4, got $serverVersion")

            admin.client.sql("CREATE DATABASE $quotedDatabase").update()
            databaseCreated = true

            val config = MySQLConfig(
                url = "${host}:$port",
                name = "ospf-live-mysql-$suffix",
                database = database,
                userName = userName,
                password = password
            )
            testConfig = config
            val backend = value(MySQL.getJdbcClient(config))
            val quotedTable = quoted("order")
            val quotedName = quoted("select")
            val table = "$quotedDatabase.$quotedTable"
            val columns = listOf(
                "id INTEGER NOT NULL PRIMARY KEY",
                "$quotedName VARCHAR(200)",
                "status VARCHAR(30) NOT NULL",
                "amount DECIMAL(12, 2)"
            ).joinToString(", ")
            backend.client.sql("CREATE TABLE $table ($columns)").update()
            listOf(
                Record(1, "Ada's row", "active", BigDecimal("10.00")),
                Record(2, "Linus", "active", BigDecimal("20.25")),
                Record(3, null, "pending", null),
                Record(4, "Grace_100%", "pending", BigDecimal("1.50"))
            ).forEach { record ->
                backend.client.sql("INSERT INTO $table (id, $quotedName, status, amount) VALUES (?, ?, ?, ?)")
                    .params(listOf(record.id, record.name, record.status, record.amount))
                    .update()
            }

            val repository = JdbcClientRepository(
                backend = backend,
                tableName = "$database.order",
                rowMapper = RowMapper { row, _ ->
                    Record(row.getInt("id"), row.getString("select"), row.getString("status"), row.getBigDecimal("amount"))
                },
                resolveColumnName = JdbcClientColumnNameResolver { path ->
                    when (path) {
                        "id" -> "id"
                        "name" -> "select"
                        "status" -> "status"
                        "amount" -> "amount"
                        else -> null
                    }
                }
            )

            val unknownPredicates = listOf<BooleanExpression>(
                BooleanConstant.unknown(),
                NotExpression(BooleanConstant.unknown())
            )
            for (predicate in unknownPredicates) {
                assertEquals(0, value(repository.update(predicate, UpdateAssignments.set("status", "unsafe"))))
                assertEquals(0, value(repository.delete(predicate)))
            }
            assertEquals(4L, value(repository.count(BooleanConstant.true_())))
            assertEquals(
                listOf(1, 2, 3, 4),
                value(
                    repository.find(
                        where = BooleanConstant.true_(),
                        sortBy = SortBy.asc("id"),
                        limit = null,
                        offset = null
                    )
                ).map { it.id }
            )
            assertEquals("active", value(repository.find(equal("id", 1))).single().status)
            assertEquals("pending", value(repository.find(equal("id", 3))).single().status)

            val ordered = value(
                repository.find(
                    where = BooleanConstant.true_(),
                    sortBy = SortBy.asc("name", NullsOrder.NullsLast),
                    limit = null,
                    offset = null
                )
            )
            assertEquals(listOf(1, 4, 2, 3), ordered.map { it.id })
            val page = value(
                repository.find(
                    where = BooleanConstant.true_(),
                    sortBy = SortBy.asc("name", NullsOrder.NullsLast),
                    limit = 2,
                    offset = 1
                )
            )
            assertEquals(listOf(4, 2), page.map { it.id })
            assertTrue(repository.find(where = BooleanConstant.true_(), sortBy = null, limit = null, offset = 1) is Failed)

            val activeFirstOrSecond = AndExpression(
                listOf(
                    equal("status", "active"),
                    OrExpression(listOf(equal("id", 1), equal("id", 2)))
                )
            )
            assertEquals(2L, value(repository.count(activeFirstOrSecond)))
            assertTrue(value(repository.exists(equal("name", "Ada's row"))))
            assertFalse(value(repository.exists(equal("name", "Ada' OR 1=1 --"))))
            assertEquals(listOf(1, 4), value(repository.find(NotExpression(equal("name", "Linus")))).map { it.id })
            assertEquals(0L, value(repository.count(NotExpression(BooleanConstant.unknown()))))
            assertEquals(
                4L,
                value(repository.count(NotExpression(AndExpression(listOf(BooleanConstant.false_(), BooleanConstant.unknown())))))
            )

            assertTrue(repository.update(equal("id", 1), UpdateAssignments.set("missing", "unsafe")) is Failed)
            assertEquals(
                1,
                value(
                    repository.update(
                        equal("id", 1),
                        UpdateAssignments.setExpr(
                            "amount",
                            ScalarBinary(
                                BinaryOperator.Add,
                                ScalarReference<BigDecimal>(PropertyPath.parse("amount")),
                                ScalarConstant(BigDecimal("0.75"))
                            )
                        ) + UpdateAssignments.set("name", "Ada's row -- ? %")
                    )
                )
            )
            assertEquals(BigDecimal("10.75"), value(repository.find(equal("id", 1))).single().amount)
            assertEquals("Ada's row -- ? %", value(repository.find(equal("id", 1))).single().name)
            assertEquals("active", value(repository.find(equal("id", 2))).single().status)
            assertEquals(1, value(repository.update(equal("id", 2), UpdateAssignments.setNull("name"))))
            assertTrue(value(repository.find(equal("name", null))).any { it.id == 2 })
            val nullRowNotMatching = AndExpression(listOf(equal("id", 3), NotExpression(equal("name", "Linus"))))
            assertEquals(0, value(repository.delete(nullRowNotMatching)))
            assertEquals("pending", value(repository.find(equal("id", 3))).single().status)
            assertEquals(2, value(repository.update(NotExpression(equal("name", "Linus")), UpdateAssignments.set("status", "not-linus"))))
            assertEquals("not-linus", value(repository.find(equal("id", 1))).single().status)
            assertEquals("not-linus", value(repository.find(equal("id", 4))).single().status)
            assertEquals("active", value(repository.find(equal("id", 2))).single().status)
            assertEquals("pending", value(repository.find(equal("id", 3))).single().status)

            val unsupported = BooleanCustom("unknown")
            assertTrue(repository.delete(unsupported) is Failed)
            assertTrue(repository.delete(AndExpression(listOf(equal("id", 1), unsupported))) is Failed)
            assertTrue(
                repository.update(
                    OrExpression(listOf(equal("id", 1), unsupported)),
                    UpdateAssignments.set("status", "unsafe")
                ) is Failed
            )

            val alwaysFalse = JdbcClientRepository(
                backend = backend,
                tableName = "$database.order",
                rowMapper = RowMapper { row, _ ->
                    Record(row.getInt("id"), row.getString("select"), row.getString("status"), row.getBigDecimal("amount"))
                },
                resolveColumnName = JdbcClientColumnNameResolver { path ->
                    when (path) {
                        "id" -> "id"
                        "name" -> "select"
                        "status" -> "status"
                        "amount" -> "amount"
                        else -> null
                    }
                },
                unsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
            )
            assertEquals(0, value(alwaysFalse.delete(unsupported)))
            assertEquals(0, value(alwaysFalse.delete(OrExpression(listOf(equal("id", 1), unsupported)))))
            assertEquals(
                0,
                value(alwaysFalse.update(AndExpression(listOf(equal("id", 1), unsupported)), UpdateAssignments.set("status", "unsafe")))
            )
            assertEquals(4L, value(repository.count(BooleanConstant.true_())))
            assertEquals("not-linus", value(repository.find(equal("id", 1))).single().status)

            assertEquals(
                1,
                value(repository.delete(AndExpression(listOf(equal("id", 3), equal("status", "pending")))))
            )
            assertEquals(3L, value(repository.count(BooleanConstant.true_())))
        } finally {
            testConfig?.let { MySQL.close(it.key) }
            try {
                if (databaseCreated) {
                    admin.client.sql("DROP DATABASE $quotedDatabase").update()
                    val remaining = admin.client.sql("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?")
                        .param(database)
                        .query(Int::class.javaObjectType)
                        .single()
                    assertEquals(0, remaining)
                }
            } finally {
                MySQL.close(adminConfig.key)
            }
        }
    }

    private fun equal(path: String, value: Any?): BooleanExpression {
        return Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Any?>(PropertyPath.parse(path)),
            ScalarConstant(value)
        )
    }

    private fun quoted(identifier: String): String {
        val result = MySQLJdbcClientDialect.quoteIdentifier(identifier)
        assertTrue(result is Ok, "Could not quote MySQL identifier '$identifier': $result")
        return result.value
    }

    private fun <T> value(result: Ret<T>): T {
        assertTrue(result is Ok, "Expected a successful MySQL operation but got $result")
        return result.value
    }
}
