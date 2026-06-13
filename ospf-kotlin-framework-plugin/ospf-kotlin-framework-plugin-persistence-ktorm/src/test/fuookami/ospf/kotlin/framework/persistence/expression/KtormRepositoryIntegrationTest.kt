/**
 * Ktorm 仓储集成测试
 * Ktorm Repository Integration Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormColumnResolver

@DisplayName("KtormRepository Integration Tests / Ktorm 仓储集成测试")
class KtormRepositoryIntegrationTest {
    private object Users : Table<Nothing>("users") {
        val id = int("id")
        val name = varchar("name")
        val age = int("age")
        val status = varchar("status")
    }

    private data class User(
        val id: Int,
        val name: String?,
        val age: Int?,
        val status: String?
    )

    private val resolver: KtormColumnResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "id" -> Users.id
            "name" -> Users.name
            "age" -> Users.age
            "status" -> Users.status
            else -> null
        }
    }

    private class UserRepository(
        database: Database,
        resolveColumn: KtormColumnResolver,
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
    ) : KtormRepository<User>(
        database = database,
        table = Users,
        resolveColumn = resolveColumn,
        nullsOrderSupport = NullsOrderSupport.Never,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy
    ) {
        override fun mapToEntity(row: QueryRowSet): User {
            return User(
                id = row[Users.id] ?: 0,
                name = row[Users.name],
                age = row[Users.age],
                status = row[Users.status]
            )
        }
    }

    private fun createDatabase(): Database {
        val dbFile = Files.createTempFile("ktorm-repo-test", ".db").toFile().apply { deleteOnExit() }
        val database = Database.connect("jdbc:sqlite:${dbFile.absolutePath}")
        database.useConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("create table users(id integer primary key, name text, age integer, status text)")
                stmt.execute("insert into users(id, name, age, status) values (1, 'a', 10, 'active')")
                stmt.execute("insert into users(id, name, age, status) values (2, 'b', 20, 'active')")
                stmt.execute("insert into users(id, name, age, status) values (3, 'c', 30, 'pending')")
            }
        }
        return database
    }

    @Test
    @DisplayName("should support where sort page update delete / 应支持 where sort page update delete")
    fun shouldSupportWhereSortPageUpdateDelete() {
        val database = createDatabase()
        val repository = UserRepository(database, resolver)

        val activeWhere = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("status")),
            ScalarConstant("active")
        )

        val page = repository.find(
            where = activeWhere,
            sortBy = SortBy.desc("age"),
            limit = 1,
            offset = 0
        )
        assertEquals(1, page.size)
        assertEquals(2, page[0].id)

        assertEquals(2L, repository.count(activeWhere))
        assertTrue(repository.exists(activeWhere))

        val updated = repository.update(
            where = Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("id")),
                ScalarConstant(1)
            ),
            assignments = UpdateAssignments.set("status", "inactive")
        )
        assertEquals(1, updated)
        val row = database.from(Users)
            .select(Users.status)
            .where { Users.id eq 1 }
            .iterator()
            .next()
        assertEquals("inactive", row[Users.status])

        val deleted = repository.delete(
            Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("status")),
                ScalarConstant("pending")
            )
        )
        assertEquals(1, deleted)
        assertFalse(repository.exists(
            Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("status")),
                ScalarConstant("pending")
            )
        ))
    }

    @Test
    @DisplayName("should support column-column and arithmetic predicate / 应支持列-列与算术谓词")
    fun shouldSupportColumnColumnAndArithmeticPredicate() {
        val database = createDatabase()
        val repository = UserRepository(database, resolver)

        val columnColumn = Comparison<Int>(
            ComparisonOperator.Gt,
            ScalarReference<Int>(PropertyPath.parse("age")),
            ScalarReference<Int>(PropertyPath.parse("id"))
        )
        val arithmetic = Comparison<Int>(
            ComparisonOperator.Gt,
            ScalarBinary<Int>(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarReference<Int>(PropertyPath.parse("id"))
            ),
            ScalarConstant<Int>(25)
        )
        val function = Comparison(
            ComparisonOperator.Gt,
            ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference<Int>(PropertyPath.parse("age")))
            ),
            ScalarConstant(15)
        )

        assertEquals(3, repository.find(columnColumn).size)
        assertEquals(2, repository.find(arithmetic).size)
        assertEquals(2, repository.find(function).size)
    }

    @Test
    @DisplayName("unsupported policy should fail explicitly / 不支持策略应明确失败")
    fun unsupportedPolicyShouldFailExplicitly() {
        val database = createDatabase()
        val failFastRepository = UserRepository(
            database,
            resolver,
            UnsupportedPredicatePolicy.FailFast
        )
        val clientFilterRepository = UserRepository(
            database,
            resolver,
            UnsupportedPredicatePolicy.ClientFilter
        )

        assertThrows(IllegalArgumentException::class.java) {
            failFastRepository.find(BooleanCustom("x"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            clientFilterRepository.find(BooleanCustom("x"))
        }
    }
}
