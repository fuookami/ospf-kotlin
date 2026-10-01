/** SQLite JdbcClient repository integration tests / SQLite JdbcClient 仓储集成测试 */
package fuookami.ospf.kotlin.framework.persistence

import java.nio.file.Files
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.RowMapper
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.JdbcClientBackend

class SqliteJdbcClientIntegrationTest {
    private data class User(
        val id: Int,
        val name: String?,
        val age: Int?,
        val status: String?
    )

    private lateinit var databaseFile: java.io.File
    private lateinit var clientKey: SqliteClientKey
    private lateinit var backend: JdbcClientBackend
    private lateinit var repository: JdbcClientRepository<User>

    private val rowMapper = RowMapper<User> { result, _ ->
        val age = result.getInt("age").let { if (result.wasNull()) null else it }
        User(
            id = result.getInt("id"),
            name = result.getString("name"),
            age = age,
            status = result.getString("status")
        )
    }
    private val resolver = JdbcClientColumnNameResolver { path -> path.substringAfterLast('.') }

    @BeforeEach
    fun setUp() {
        databaseFile = Files.createTempFile("ospf-sqlite-jdbcclient-", ".db").toFile()
        clientKey = SqliteClientKey("jdbcclient-${UUID.randomUUID()}")
        backend = value(
            Sqlite.initJdbcClient {
                url = databaseFile.absolutePath
                name = clientKey.name
            }
        )
        backend.client.sql(
            "CREATE TABLE \"users\" (\"id\" INTEGER PRIMARY KEY, \"name\" TEXT, \"age\" INTEGER, \"status\" TEXT)"
        ).update()
        insertUser(id = 1, name = "Ada", age = 36, status = "active")
        insertUser(id = 2, name = "Linus", age = 54, status = "active")
        insertUser(id = 3, name = null, age = 22, status = "pending")
        repository = JdbcClientRepository(
            backend = backend,
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = resolver
        )
    }

    @AfterEach
    fun tearDown() {
        if (this::clientKey.isInitialized) {
            assertTrue(Sqlite.close(clientKey) is Ok, "expected SQLite datasource to close successfully")
        }
        if (this::databaseFile.isInitialized) {
            Files.deleteIfExists(databaseFile.toPath())
        }
    }

    @Test
    fun clientFactoryQueriesSortsPagesAndUsesPublicScalarFunctions() {
        val active = equal("status", "active")
        val page = value(repository.find(where = active, sortBy = SortBy.desc("age"), limit = 1, offset = 0))
        assertEquals(listOf(2), page.map { it.id })

        val nullsLast = value(
            repository.find(
                where = BooleanConstant.true_(),
                sortBy = SortBy.asc("name", NullsOrder.NullsLast),
                limit = null,
                offset = null
            )
        )
        assertEquals(listOf(1, 2, 3), nullsLast.map { it.id })
        assertNull(nullsLast.last().name)

        val offsetOnly = value(
            repository.find(where = BooleanConstant.true_(), sortBy = SortBy.asc("id"), limit = null, offset = 1)
        )
        assertEquals(listOf(2, 3), offsetOnly.map { it.id })
        assertTrue(value(repository.find(where = active, sortBy = null, limit = 0, offset = null)).isEmpty())
        assertTrue(repository.find(where = active, sortBy = null, limit = -1, offset = null) is Failed)
        assertTrue(repository.find(where = active, sortBy = null, limit = null, offset = -1) is Failed)

        assertEquals(2L, value(repository.count(active)))
        assertTrue(value(repository.exists(active)))
        assertFalse(value(repository.exists(equal("status", "missing"))))
        assertEquals(
            3L,
            value(
                repository.count(
                    InExpression(
                        reference("status"),
                        listOf(ScalarConstant<Any?>("active"), ScalarConstant<Any?>("pending"))
                    )
                )
            )
        )
        assertEquals(
            1L,
            value(
                repository.count(
                    PatternMatch(
                        value = reference("name"),
                        pattern = ScalarConstant<Any?>("d"),
                        mode = PatternMatchMode.Contains
                    )
                )
            )
        )

        val length = ScalarFunction<Any?>(ScalarFunctionNames.Length, listOf(reference("name")))
        assertEquals(1, value(repository.find(equal(length, 3))).single().id)
        val coalesce = function(
            ScalarFunctionNames.Coalesce,
            listOf(reference("name"), ScalarConstant(""))
        )
        assertEquals(1, value(repository.find(equal(coalesce, "Ada"))).single().id)

        assertTrue(value(repository.find(equal("name", "Ada' OR 1=1 --"))).isEmpty())
        assertEquals(1L, value(repository.count(equal("name", null))))
    }

    @Test
    fun updatesAndDeletesBindValuesAndEvaluateArithmeticExpressions() {
        val updated = value(
            repository.update(
                equal("id", 1),
                UpdateAssignments.setExpr(
                    path = "age",
                    expr = ScalarBinary(
                        operator = BinaryOperator.Add,
                        left = ScalarReference<Int>(PropertyPath.parse("age")),
                        right = ScalarConstant(7)
                    )
                ) + UpdateAssignments.set("name", "Ada's row")
            )
        )
        assertEquals(1, updated)
        val changed = value(repository.find(equal("id", 1))).single()
        assertEquals(43, changed.age)
        assertEquals("Ada's row", changed.name)
        assertEquals(0L, value(repository.count(equal("name", "Ada' OR 1=1 --"))))

        assertEquals(1, value(repository.update(equal("id", 1), UpdateAssignments.setNull("name"))))
        assertNull(value(repository.find(equal("id", 1))).single().name)
        assertEquals(2L, value(repository.count(equal("name", null))))
        assertEquals(1, value(repository.delete(equal("status", "pending"))))
        assertEquals(2L, value(repository.count(BooleanConstant.true_())))
    }

    @Test
    fun unsupportedPredicatesAndFunctionsFailOrBlockWrites() {
        val unsupported = BooleanCustom("unknown")
        val predicates = listOf<BooleanExpression>(
            NotExpression(unsupported),
            AndExpression(listOf(equal("id", 1), unsupported)),
            OrExpression(listOf(unsupported, equal("id", 1)))
        )

        val failFast = repository
        val alwaysFalse = JdbcClientRepository(
            backend = backend,
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
        )

        for (predicate in predicates) {
            assertTrue(failFast.find(predicate) is Failed)
            assertTrue(failFast.update(predicate, UpdateAssignments.set("status", "changed")) is Failed)
            assertTrue(failFast.delete(predicate) is Failed)

            assertTrue(value(alwaysFalse.find(predicate)).isEmpty())
            assertEquals(0, value(alwaysFalse.update(predicate, UpdateAssignments.set("status", "changed"))))
            assertEquals(0, value(alwaysFalse.delete(predicate)))
        }
        assertEquals(2L, value(repository.count(equal("status", "active"))))

        val repeat = function("repeat", listOf(reference("name"), ScalarConstant(2)))
        assertTrue(repository.find(equal(repeat, "AdaAda")) is Failed)
        assertTrue(repository.update(equal("id", 1), UpdateAssignments.setExpr("name", repeat)) is Failed)
        assertTrue(
            repository.find(
                PatternMatch(
                    value = reference("name"),
                    pattern = ScalarConstant<Any?>("^Ada$"),
                    mode = PatternMatchMode.Regex
                )
            ) is Failed
        )

        val partialAssignments = UpdateAssignments.set("status", "changed") + UpdateAssignments.set("missing", "bad")
        assertTrue(repository.update(equal("id", 1), partialAssignments) is Failed)
        assertEquals(2L, value(repository.count(equal("status", "active"))))
    }

    @Test
    fun unknownBooleanConstantsKeepSqlThreeValuedSemantics() {
        val notUnknown = NotExpression(BooleanConstant.unknown())
        assertTrue(value(repository.find(notUnknown)).isEmpty())
        assertEquals(0L, value(repository.count(notUnknown)))
        assertEquals(0, value(repository.update(notUnknown, UpdateAssignments.set("status", "changed"))))
        assertEquals(0, value(repository.delete(notUnknown)))

        val notFalseAndUnknown = NotExpression(
            AndExpression(listOf(BooleanConstant.false_(), BooleanConstant.unknown()))
        )
        assertEquals(3L, value(repository.count(notFalseAndUnknown)))
        assertEquals(3, value(repository.update(notFalseAndUnknown, UpdateAssignments.set("status", "three-valued"))))
        assertEquals(3L, value(repository.count(equal("status", "three-valued"))))
        assertEquals(3, value(repository.delete(notFalseAndUnknown)))
    }

    @Test
    fun closedManagedClientFailuresAreReturnedByCountAndExists() {
        assertTrue(Sqlite.close(clientKey) is Ok)

        assertTrue(repository.count(BooleanConstant.true_()) is Failed)
        assertTrue(repository.exists(BooleanConstant.true_()) is Failed)
    }

    private fun insertUser(id: Int, name: String?, age: Int, status: String) {
        backend.client.sql("INSERT INTO \"users\" (\"id\", \"name\", \"age\", \"status\") VALUES (?, ?, ?, ?)")
            .params(listOf(id, name, age, status))
            .update()
    }

    private fun reference(path: String): ScalarReference<Any?> =
        ScalarReference(PropertyPath.parse(path))

    private fun function(name: String, arguments: List<ScalarExpression<*>>): ScalarFunction<Any?> =
        ScalarFunction(name, arguments.map { it.asAnyScalar() })

    private fun equal(path: String, value: Any?): BooleanExpression =
        equal(reference(path), value)

    private fun equal(expression: ScalarExpression<*>, value: Any?): BooleanExpression =
        Comparison(ComparisonOperator.Eq, expression.asAnyScalar(), ScalarConstant<Any?>(value))

    @Suppress("UNCHECKED_CAST")
    private fun ScalarExpression<*>.asAnyScalar(): ScalarExpression<Any?> = this as ScalarExpression<Any?>

    private fun <T> value(result: Ret<T>): T {
        assertTrue(result is Ok, "expected Ok but got $result")
        return (result as Ok).value
    }
}
