package fuookami.ospf.kotlin.framework.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.framework.persistence.expression.KtormRepository
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments
import fuookami.ospf.kotlin.framework.persistence.query.RelationalQueryDialect
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormColumnResolver
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.functional.Ret

class H2KtormIntegrationTest {
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
    fun repositoryUsesH2BackendForCrudPagingScalarAndUnknownSemantics() {
        val name = "ktorm-h2-${System.nanoTime()}"
        val key = H2ClientKey(name)
        val backendResult = H2.initKtorm {
            this.name = name
            url = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
        }

        try {
            val backend = value(backendResult)
            assertEquals(RelationalQueryDialect.H2, backend.dialect)
            assertEquals(RelationalQueryDialect.H2, value(H2.getKtormBackend(name)).dialect)
            backend.database.useConnection { connection ->
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE TABLE records (id INTEGER PRIMARY KEY, display_name VARCHAR(100), age INTEGER NOT NULL)"
                    )
                }
                connection.prepareStatement(
                    "INSERT INTO records (id, display_name, age) VALUES (?, ?, ?)"
                ).use { statement ->
                    val rows = listOf(
                        Record(1, "Ada", 10),
                        Record(2, "Linus", 20),
                        Record(3, null, 30),
                        Record(4, "Grace", 40)
                    )
                    rows.forEach { row ->
                        statement.setInt(1, row.id)
                        statement.setString(2, row.displayName)
                        statement.setInt(3, row.age)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }

            val repository = RecordRepository(backend, resolver)
            val jdbcClient = value(H2.getJdbcClient(key))
            val sharedCount = jdbcClient.client.sql("SELECT COUNT(*) FROM records")
                .query(Int::class.javaObjectType)
                .single()
            assertEquals(4, sharedCount)
            val ordered = value(
                repository.find(
                    where = BooleanConstant(Trivalent.True),
                    sortBy = SortBy.asc("displayName", NullsOrder.NullsLast),
                    limit = null,
                    offset = null
                )
            )
            assertEquals(listOf(1, 4, 2, 3), ordered.map { it.id })
            val nullsFirst = value(repository.find(
                where = BooleanConstant(Trivalent.True),
                sortBy = SortBy.asc("displayName", NullsOrder.NullsFirst),
                limit = null,
                offset = null
            ))
            assertEquals(listOf(3, 1, 4, 2), nullsFirst.map { it.id })
            assertEquals(listOf(4, 2), value(repository.find(
                BooleanConstant(Trivalent.True),
                SortBy.asc("displayName", NullsOrder.NullsLast),
                limit = 2,
                offset = 1
            )).map { it.id })
            assertEquals(
                3,
                value(repository.find(NullCheck(PropertyPath.parse("displayName"), NullCheckType.IsNull))).single().id
            )
            assertEquals(2L, value(repository.count(
                Comparison(
                    ComparisonOperator.Gt,
                    ScalarFunction(ScalarFunctionNames.Length, listOf(ScalarReference<String>(PropertyPath.parse("displayName")))),
                    ScalarConstant(3)
                )
            )))
            assertEquals(0L, value(repository.count(BooleanConstant(Trivalent.Unknown))))
            assertEquals(0L, value(repository.count(NotExpression(BooleanConstant(Trivalent.Unknown)))))
            assertEquals(
                4L,
                value(repository.count(NotExpression(AndExpression(listOf(
                    BooleanConstant(Trivalent.False),
                    BooleanConstant(Trivalent.Unknown)
                )))))
            )

            assertEquals(
                1,
                value(repository.update(
                    Comparison(
                        ComparisonOperator.Eq,
                        ScalarReference<Int>(PropertyPath.parse("id")),
                        ScalarConstant(1)
                    ),
                    UpdateAssignments.setExpr(
                        "age",
                        ScalarBinary(
                            BinaryOperator.Add,
                            ScalarReference<Int>(PropertyPath.parse("age")),
                            ScalarConstant(5)
                        )
                    )
                ))
            )
            assertEquals(15, value(repository.find(Comparison(
                ComparisonOperator.Eq,
                ScalarReference<Int>(PropertyPath.parse("id")),
                ScalarConstant(1)
            ))).single().age)

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
            assertTrue(repository.delete(AndExpression(listOf(
                Comparison(
                    ComparisonOperator.Eq,
                    ScalarReference<Int>(PropertyPath.parse("id")),
                    ScalarConstant(1)
                ),
                unsupported
            ))).failed)
            assertTrue(repository.update(
                OrExpression(listOf(
                    Comparison(
                        ComparisonOperator.Eq,
                        ScalarReference<Int>(PropertyPath.parse("id")),
                        ScalarConstant(1)
                    ),
                    unsupported
                )),
                UpdateAssignments.set("age", 99)
            ).failed)
            assertEquals(4L, value(repository.count(BooleanConstant(Trivalent.True))))
            assertEquals(1, value(repository.delete(Comparison(
                ComparisonOperator.Eq,
                ScalarReference<Int>(PropertyPath.parse("id")),
                ScalarConstant(4)
            ))))
            assertEquals(3L, value(repository.count(BooleanConstant(Trivalent.True))))

            backend.database.useConnection { connection ->
                connection.createStatement().use { statement -> statement.execute("DROP TABLE records") }
            }
            assertTrue(repository.find(BooleanConstant(Trivalent.True)).failed)
        } finally {
            assertTrue(H2.close(key).ok)
        }

        assertTrue(H2.getKtormBackend(key).failed)
    }

    private fun <T> value(result: Ret<T>): T {
        return result.value ?: error("Expected Ktorm success but got $result")
    }
}
