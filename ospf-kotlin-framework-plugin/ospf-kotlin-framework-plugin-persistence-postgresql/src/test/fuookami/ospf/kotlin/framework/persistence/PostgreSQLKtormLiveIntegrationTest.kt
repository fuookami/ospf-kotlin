package fuookami.ospf.kotlin.framework.persistence

import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.framework.persistence.expression.KtormRepository
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormColumnResolver
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.functional.Ret

class PostgreSQLKtormLiveIntegrationTest {
    private object Records : Table<Nothing>("records") {
        val id = int("id")
        val displayName = varchar("display_name")
        val age = int("age")
    }

    private data class Record(val id: Int, val displayName: String?, val age: Int)

    private val resolver = KtormColumnResolver { path ->
        when (path.substringAfterLast('.')) {
            "id" -> Records.id
            "displayName" -> Records.displayName
            "age" -> Records.age
            else -> null
        }
    }

    private class RecordRepository(backend: KtormBackend, resolver: KtormColumnResolver) :
        KtormRepository<Record>(backend, Records, resolver) {
        override fun mapToEntity(row: QueryRowSet): Record {
            return Record(
                id = row[Records.id] ?: 0,
                displayName = row[Records.displayName],
                age = row[Records.age] ?: 0
            )
        }
    }

    @Test
    fun repositoryUsesManagedPostgreSQLBackendForCrudPagingScalarAndUnknownSemantics() {
        val configuredHost = System.getenv("OSPF_TEST_POSTGRESQL_HOST")
        Assumptions.assumeTrue(configuredHost != null, "Set OSPF_TEST_POSTGRESQL_HOST to enable the live PostgreSQL Ktorm test")
        val host = configuredHost!!
        check(host.isNotBlank()) { "OSPF_TEST_POSTGRESQL_HOST must not be blank" }
        val port = System.getenv("OSPF_TEST_POSTGRESQL_PORT")?.let { it.toIntOrNull() ?: error("OSPF_TEST_POSTGRESQL_PORT must be numeric") } ?: 5432
        val userName = System.getenv("OSPF_TEST_POSTGRESQL_USER") ?: error("OSPF_TEST_POSTGRESQL_USER is required")
        val password = System.getenv("OSPF_TEST_POSTGRESQL_PASSWORD") ?: error("OSPF_TEST_POSTGRESQL_PASSWORD is required")
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val database = "ospf_ktorm_$suffix"
        val name = "ospf-ktorm-postgresql-$suffix"
        val key = PostgreSQLClientKey(name, database)
        val admin = DriverManager.getConnection("jdbc:postgresql://$host:$port/postgres", userName, password)
        var databaseCreated = false

        try {
            admin.createStatement().use { statement ->
                statement.execute("CREATE DATABASE \"$database\"")
                databaseCreated = true
            }
            val initialized = PostgreSQL.initKtorm {
                url = "$host:$port"
                this.name = name
                this.database = database
                this.userName = userName
                this.password = password
            }
            val createdBackend = value(initialized)
            assertEquals(RelationalQueryDialect.PostgreSQL, createdBackend.dialect)
            createdBackend.database.useConnection { connection -> createRecords(connection) }

            val byKey = value(PostgreSQL.getKtormBackend(key))
            assertEquals(RelationalQueryDialect.PostgreSQL, value(PostgreSQL.getKtormBackend(name, database)).dialect)
            val repository = RecordRepository(byKey, resolver)
            val jdbcClient = value(PostgreSQL.getJdbcClient(key))
            assertEquals(
                4,
                jdbcClient.client.sql("SELECT COUNT(*) FROM records")
                    .query(Int::class.javaObjectType)
                    .single()
            )
            assertCrudAndExpressionSemantics(repository)
            byKey.database.useConnection { connection ->
                connection.createStatement().use { it.execute("DROP TABLE records") }
            }
            assertTrue(repository.find(BooleanConstant(Trivalent.True)).failed)
        } finally {
            assertTrue(PostgreSQL.close(key).ok)
            try {
                if (databaseCreated) {
                    admin.createStatement().use { it.execute("DROP DATABASE \"$database\"") }
                }
            } finally {
                admin.close()
            }
        }
    }

    private fun createRecords(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                "CREATE TABLE records (id INTEGER PRIMARY KEY, display_name VARCHAR(100), age INTEGER NOT NULL)"
            )
        }
        connection.prepareStatement("INSERT INTO records (id, display_name, age) VALUES (?, ?, ?)").use { statement ->
            listOf(Record(1, "Ada", 10), Record(2, "Linus", 20), Record(3, null, 30), Record(4, "Grace", 40))
                .forEach { row ->
                    statement.setInt(1, row.id)
                    statement.setString(2, row.displayName)
                    statement.setInt(3, row.age)
                    statement.addBatch()
                }
            statement.executeBatch()
        }
    }

    private fun assertCrudAndExpressionSemantics(repository: RecordRepository) {
        val ordered = value(repository.find(
            BooleanConstant(Trivalent.True),
            SortBy.asc("displayName", NullsOrder.NullsLast),
            null,
            null
        ))
        assertEquals(listOf(1, 4, 2, 3), ordered.map { it.id })
        val nullsFirst = value(repository.find(
            BooleanConstant(Trivalent.True),
            SortBy.asc("displayName", NullsOrder.NullsFirst),
            null,
            null
        ))
        assertEquals(listOf(3, 1, 4, 2), nullsFirst.map { it.id })
        assertEquals(listOf(4, 2), value(repository.find(
            BooleanConstant(Trivalent.True),
            SortBy.asc("displayName", NullsOrder.NullsLast),
            2,
            1
        )).map { it.id })
        assertEquals(3, value(repository.find(
            NullCheck(PropertyPath.parse("displayName"), NullCheckType.IsNull)
        )).single().id)
        assertEquals(2L, value(repository.count(Comparison(
            ComparisonOperator.Gt,
            ScalarFunction(ScalarFunctionNames.Length, listOf(ScalarReference<String>(PropertyPath.parse("displayName")))),
            ScalarConstant(3)
        ))))
        assertEquals(0L, value(repository.count(BooleanConstant(Trivalent.Unknown))))
        assertEquals(0L, value(repository.count(NotExpression(BooleanConstant(Trivalent.Unknown)))))
        assertEquals(4L, value(repository.count(NotExpression(AndExpression(listOf(
            BooleanConstant(Trivalent.False), BooleanConstant(Trivalent.Unknown)
        ))))))

        assertEquals(1, value(repository.update(
            equal("id", 1),
            UpdateAssignments.setExpr("age", ScalarBinary(
                BinaryOperator.Add,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarConstant(5)
            ))
        )))
        assertEquals(15, value(repository.find(equal("id", 1))).single().age)

        val unknownWritePredicates = listOf(
            BooleanConstant(Trivalent.Unknown),
            NotExpression(BooleanConstant(Trivalent.Unknown))
        )
        for (predicate in unknownWritePredicates) {
            assertEquals(0, value(repository.update(predicate, UpdateAssignments.set("age", 99))))
            assertEquals(0, value(repository.delete(predicate)))
        }
        assertEquals(4L, value(repository.count(BooleanConstant(Trivalent.True))))
        assertEquals(
            mapOf(1 to 15, 2 to 20, 3 to 30, 4 to 40),
            value(repository.find(BooleanConstant(Trivalent.True))).associate { it.id to it.age }
        )

        val unsupported = BooleanCustom("unsupported")
        assertTrue(repository.delete(AndExpression(listOf(equal("id", 1), unsupported))).failed)
        assertTrue(repository.update(
            OrExpression(listOf(equal("id", 1), unsupported)),
            UpdateAssignments.set("age", 99)
        ).failed)
        assertEquals(4L, value(repository.count(BooleanConstant(Trivalent.True))))
        assertEquals(1, value(repository.delete(equal("id", 4))))
        assertEquals(3L, value(repository.count(BooleanConstant(Trivalent.True))))
    }

    private fun equal(path: String, value: Any?): BooleanExpression {
        return Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Any?>(PropertyPath.parse(path)),
            ScalarConstant(value)
        )
    }

    private fun <T> value(result: Ret<T>): T {
        return result.value ?: error("Expected PostgreSQL Ktorm success but got $result")
    }
}
