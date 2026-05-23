/**
 * Ktorm 更新翻译器测试
 * Ktorm Update Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments
import fuookami.ospf.kotlin.math.symbol.expression.ScalarConstant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.int
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import java.nio.file.Files

@DisplayName("KtormUpdateTranslator Tests / Ktorm 更新翻译器测试")
class KtormUpdateTranslatorTest {
    private object Users : Table<Nothing>("users") {
        val id = int("id")
        val name = varchar("name")
        val age = int("age")
        val status = varchar("status")
    }

    private val resolver: KtormColumnResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "id" -> Users.id
            "name" -> Users.name
            "age" -> Users.age
            "status" -> Users.status
            else -> null
        }
    }

    private fun createDatabase(): Database {
        val dbFile = Files.createTempFile("ktorm-update-test", ".db").toFile().apply { deleteOnExit() }
        val database = Database.connect("jdbc:sqlite:${dbFile.absolutePath}")
        database.useConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("create table users(id integer primary key, name text, age integer, status text)")
                stmt.execute("insert into users(id, name, age, status) values (1, 'a', 10, 'active')")
                stmt.execute("insert into users(id, name, age, status) values (2, 'b', 20, 'pending')")
            }
        }
        return database
    }

    @Test
    @DisplayName("should apply set/setNull/setExpr in one update / 应在一次更新中支持 set/setNull/setExpr")
    fun shouldApplySetSetNullSetExprInOneUpdate() {
        val database = createDatabase()
        val translator = KtormUpdateTranslator(resolver, Users)
        val assignments = UpdateAssignments
            .set("name", "neo")
            .thenSetNull("status")
            .thenSetExpr("age", ScalarConstant(42))

        val updated = translator.executeUpdate(
            database = database,
            whereCondition = Users.id eq 1,
            assignments = assignments
        )

        val row = database.from(Users)
            .select(Users.name, Users.age, Users.status)
            .where { Users.id eq 1 }
            .iterator()
            .next()

        assertEquals(1, updated)
        assertEquals("neo", row[Users.name])
        assertEquals(42, row[Users.age])
        assertNull(row[Users.status])
    }

    @Test
    @DisplayName("unknown assignment path should be ignored / 未知赋值路径应被忽略")
    fun unknownAssignmentPathShouldBeIgnored() {
        val database = createDatabase()
        val translator = KtormUpdateTranslator(resolver, Users)
        val assignments = UpdateAssignments
            .set("name", "updated")
            .thenSet("unknown", "x")

        val updated = translator.executeUpdate(
            database = database,
            whereCondition = Users.id eq 1,
            assignments = assignments
        )

        val row = database.from(Users)
            .select(Users.name)
            .where { Users.id eq 1 }
            .iterator()
            .next()

        assertEquals(1, updated)
        assertEquals("updated", row[Users.name])
    }
}

