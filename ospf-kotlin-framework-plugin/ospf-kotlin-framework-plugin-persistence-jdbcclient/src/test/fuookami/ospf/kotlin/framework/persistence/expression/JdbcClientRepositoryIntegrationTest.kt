/** JdbcClient repository integration tests / JdbcClient 仓储集成测试 */
package fuookami.ospf.kotlin.framework.persistence.expression

import java.util.concurrent.CancellationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

class JdbcClientRepositoryIntegrationTest {
    private data class User(val id: Int, val name: String?, val age: Int?, val status: String?)

    private class H2TestDialect : JdbcClientDialect {
        override val name: String = "h2-test"

        override fun quoteIdentifier(identifier: String): Ret<String> {
            if (identifier.isBlank()) return Failed(ErrorCode.IllegalArgument, "标识符为空 / Identifier is blank")
            return Ok("\"${identifier.replace("\"", "\"\"")}\"")
        }

        override fun pagination(limit: Int?, offset: Int?): Ret<JdbcClientSqlFragment> {
            val sql = when {
                limit != null && offset != null -> "LIMIT ? OFFSET ?"
                limit != null -> "LIMIT ?"
                offset != null -> "OFFSET ?"
                else -> ""
            }
            return Ok(JdbcClientSqlFragment(sql, listOfNotNull(limit, offset)))
        }

        override fun orderNulls(
            expressionSql: String,
            direction: SortDirection,
            nulls: NullsOrder?
        ): Ret<JdbcClientSqlFragment> {
            val directionSql = if (direction == SortDirection.Asc) "ASC" else "DESC"
            val nullsSql = when (nulls) {
                NullsOrder.NullsFirst -> " NULLS FIRST"
                NullsOrder.NullsLast -> " NULLS LAST"
                null -> ""
            }
            return Ok(JdbcClientSqlFragment("$expressionSql $directionSql$nullsSql"))
        }
    }

    private val client: JdbcClient by lazy {
        val dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:jdbcclient_repo_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )
        JdbcClient.create(dataSource).also { jdbc ->
            jdbc.sql("CREATE TABLE \"users\" (\"id\" INT PRIMARY KEY, \"name\" VARCHAR(100), \"age\" INT, \"status\" VARCHAR(30))").update()
            jdbc.sql("INSERT INTO \"users\" (\"id\", \"name\", \"age\", \"status\") VALUES (?, ?, ?, ?)")
                .params(1, "Ada", 36, "active").update()
            jdbc.sql("INSERT INTO \"users\" (\"id\", \"name\", \"age\", \"status\") VALUES (?, ?, ?, ?)")
                .params(2, "Linus", 54, "active").update()
            jdbc.sql("INSERT INTO \"users\" (\"id\", \"name\", \"age\", \"status\") VALUES (?, ?, ?, ?)")
                .params(listOf(3, null, 22, "pending")).update()
        }
    }

    private val rowMapper = RowMapper<User> { result, _ ->
        User(result.getInt("id"), result.getString("name"), result.getInt("age"), result.getString("status"))
    }
    private val resolver = JdbcClientColumnNameResolver { path -> path.substringAfterLast('.') }
    private val repository by lazy {
        JdbcClientRepository(
            client = client,
            dialect = H2TestDialect(),
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = resolver
        )
    }

    private fun equal(path: String, value: Any?): BooleanExpression {
        return Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Any?>(PropertyPath.parse(path)),
            ScalarConstant(value)
        )
    }

    private fun <T> value(result: Ret<T>): T {
        assertTrue(result is Ok, "expected Ok but got $result")
        return (result as Ok).value
    }

    @Test
    fun querySortPaginationCountAndExistsUseTheH2Database() {
        val active = equal("status", "active")
        val page = value(repository.find(active, SortBy.desc("age"), 1, 0))
        assertEquals(listOf(2), page.map { it.id })
        val nullsLast = value(repository.find(BooleanConstant.true_(), SortBy.asc("name", NullsOrder.NullsLast), null, null))
        assertEquals(3, nullsLast.size)
        assertNull(nullsLast.last().name)
        assertEquals(2L, value(repository.count(active)))
        assertTrue(value(repository.exists(active)))
        assertFalse(value(repository.exists(equal("status", "missing"))))
        assertTrue(value(repository.find(active, null, 0, null)).isEmpty())
        assertEquals(3L, value(repository.count(InExpression(
            ScalarReference<String>(PropertyPath.parse("status")),
            listOf(ScalarConstant("active"), ScalarConstant("pending"))
        ))))
        assertEquals(1L, value(repository.count(PatternMatch(
            ScalarReference<String>(PropertyPath.parse("name")),
            ScalarConstant("d"),
            PatternMatchMode.Contains
        ))))

        assertTrue(repository.find(active, null, -1, null) is Failed)
        assertTrue(repository.find(active, null, null, -1) is Failed)
    }

    @Test
    fun updateDeleteAndSpecialCharactersUseBoundParameters() {
        val updated = value(
            repository.update(
                equal("id", 1),
                UpdateAssignments.setExpr(
                    "age",
                    ScalarBinary(
                        BinaryOperator.Add,
                        ScalarReference<Int>(PropertyPath.parse("age")),
                        ScalarConstant(7)
                    )
                ) + UpdateAssignments.set("name", "Ada's row")
            )
        )
        assertEquals(1, updated)
        val changed = value(repository.find(equal("id", 1))).single()
        assertEquals(43, changed.age)
        assertEquals("Ada's row", changed.name)

        assertEquals(0L, value(repository.count(equal("name", "Ada' OR 1=1 --"))))
        assertEquals(1, value(repository.delete(equal("status", "pending"))))
        assertEquals(1, value(repository.update(equal("id", 1), UpdateAssignments.setNull("name"))))
        assertNull(value(repository.find(equal("id", 1))).single().name)
    }

    @Test
    fun unsupportedPredicatesAndAssignmentsCannotBroadenWrites() {
        val alwaysFalseRepository = JdbcClientRepository(
            backend = JdbcClientBackend(client, H2TestDialect()),
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
        )
        val predicate = NotExpression(BooleanCustom("unknown"))
        assertTrue(value(alwaysFalseRepository.find(predicate)).isEmpty())
        assertEquals(0, value(alwaysFalseRepository.delete(predicate)))
        assertEquals(0, value(alwaysFalseRepository.update(BooleanCustom("unknown"), UpdateAssignments.set("status", "changed"))))
        assertEquals(0, value(alwaysFalseRepository.delete(AndExpression(listOf(equal("id", 1), BooleanCustom("unknown"))))))
        assertEquals(0, value(alwaysFalseRepository.delete(OrExpression(listOf(BooleanCustom("unknown"), equal("id", 1))))))
        assertEquals(0, value(alwaysFalseRepository.update(AndExpression(listOf(equal("id", 1), BooleanCustom("unknown"))), UpdateAssignments.set("status", "changed"))))
        assertEquals(0, value(alwaysFalseRepository.update(OrExpression(listOf(BooleanCustom("unknown"), equal("id", 1))), UpdateAssignments.set("status", "changed"))))

        val partialAssignments = UpdateAssignments.set("status", "changed") + UpdateAssignments.set("missing", "bad")
        assertTrue(repository.update(equal("id", 1), partialAssignments) is Failed)
        assertEquals("active", value(repository.find(equal("id", 1))).single().status)

        val failFast = JdbcClientRepository(
            backend = JdbcClientBackend(client, H2TestDialect()),
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = resolver
        )
        val unsafe = BooleanCustom("unknown")
        assertTrue(failFast.delete(NotExpression(unsafe)) is Failed)
        assertTrue(failFast.delete(AndExpression(listOf(equal("id", 1), unsafe))) is Failed)
        assertTrue(failFast.delete(OrExpression(listOf(equal("id", 1), unsafe))) is Failed)
        assertTrue(failFast.update(NotExpression(unsafe), UpdateAssignments.set("status", "changed")) is Failed)
        assertTrue(failFast.update(AndExpression(listOf(equal("id", 1), unsafe)), UpdateAssignments.set("status", "changed")) is Failed)
        assertTrue(failFast.update(OrExpression(listOf(equal("id", 1), unsafe)), UpdateAssignments.set("status", "changed")) is Failed)
        assertEquals(2L, value(repository.count(equal("status", "active"))))
    }

    @Test
    fun unknownBooleanConstantsKeepSqlThreeValuedNotSemantics() {
        val notUnknown = NotExpression(BooleanConstant.unknown())
        assertTrue(value(repository.find(notUnknown)).isEmpty())
        assertEquals(0L, value(repository.count(notUnknown)))
        assertEquals(0, value(repository.update(notUnknown, UpdateAssignments.set("status", "changed"))))
        assertEquals(0, value(repository.delete(notUnknown)))
        assertEquals(3L, value(repository.count(BooleanConstant.true_())))

        val notTrueAndUnknown = NotExpression(
            AndExpression(listOf(BooleanConstant.true_(), BooleanConstant.unknown()))
        )
        assertTrue(value(repository.find(notTrueAndUnknown)).isEmpty())
        assertEquals(0, value(repository.update(notTrueAndUnknown, UpdateAssignments.set("status", "changed"))))
        assertEquals(0, value(repository.delete(notTrueAndUnknown)))

        val notFalseOrUnknown = NotExpression(
            OrExpression(listOf(BooleanConstant.false_(), BooleanConstant.unknown()))
        )
        assertTrue(value(repository.find(notFalseOrUnknown)).isEmpty())
        assertEquals(0, value(repository.update(notFalseOrUnknown, UpdateAssignments.set("status", "changed"))))
        assertEquals(0, value(repository.delete(notFalseOrUnknown)))

        val notFalseAndUnknown = NotExpression(
            AndExpression(listOf(BooleanConstant.false_(), BooleanConstant.unknown()))
        )
        assertEquals(3L, value(repository.count(notFalseAndUnknown)))
    }

    @Test
    fun jdbcExceptionsBecomeFailedResults() {
        val missingTableRepository = JdbcClientRepository(
            client = client,
            dialect = H2TestDialect(),
            tableName = "missing_table",
            rowMapper = rowMapper,
            resolveColumnName = resolver
        )
        assertTrue(missingTableRepository.find(BooleanConstant.true_()) is Failed)
        val zeroLimit = missingTableRepository.find(BooleanConstant.true_(), null, 0, null)
        assertTrue(zeroLimit is Ok)
        assertTrue(zeroLimit.value!!.isEmpty())
        val negativeLimit = missingTableRepository.find(BooleanConstant.true_(), null, -1, null)
        assertTrue(negativeLimit is Failed)
        assertTrue((negativeLimit as Failed).error.message.contains("分页参数"))

        val throwingMapperRepository = JdbcClientRepository(
            client = client,
            dialect = H2TestDialect(),
            tableName = "users",
            rowMapper = RowMapper { _, _ -> error("row mapping failed") },
            resolveColumnName = resolver
        )
        assertTrue(throwingMapperRepository.find(BooleanConstant.true_()) is Failed)

        val cancellingRepository = JdbcClientRepository(
            client = client,
            dialect = H2TestDialect(),
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = JdbcClientColumnNameResolver { throw CancellationException("cancelled") }
        )
        assertThrows(CancellationException::class.java) { cancellingRepository.find(equal("status", "active")) }
    }

    @Test
    fun identifierInputIsQuotedAsOneIdentifier() {
        val injectionResolver = JdbcClientColumnNameResolver { "id; DROP TABLE users;--" }
        val injectionRepository = JdbcClientRepository(
            client = client,
            dialect = H2TestDialect(),
            tableName = "users",
            rowMapper = rowMapper,
            resolveColumnName = injectionResolver
        )
        val invalidColumn = injectionRepository.count(equal("anything", 1))
        assertTrue(invalidColumn is Failed)
        assertEquals(3L, value(repository.count(BooleanConstant.true_())))
    }
}
