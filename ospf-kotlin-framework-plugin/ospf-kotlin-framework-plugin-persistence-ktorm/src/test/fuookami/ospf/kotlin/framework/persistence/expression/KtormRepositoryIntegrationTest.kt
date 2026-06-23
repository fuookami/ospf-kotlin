/**
 * Ktorm 仓储集成测试
 * Ktorm Repository Integration Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.QueryRowSet
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.int
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.framework.persistence.expression.translator.KtormColumnResolver
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.dsl.and
import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

/**
 * Ktorm 仓储集成测试
 * Ktorm repository integration tests
 */
@DisplayName("KtormRepository Integration Tests / Ktorm 仓储集成测试")
class KtormRepositoryIntegrationTest {
    /**
     * 测试用用户表定义
     * Test user table definition
     */
    private object Users : Table<Nothing>("users") {
        val id = int("id")
        val name = varchar("name")
        val age = int("age")
        val status = varchar("status")
    }

    /**
     * 测试用用户实体
     * Test user entity
     */
    private data class User(
        val id: Int,
        val name: String?,
        val age: Int?,
        val status: String?
    )

    /**
     * 列解析器，将属性路径映射到表列
     * Column resolver, maps property paths to table columns
     */
    private val resolver: KtormColumnResolver = { path: String ->
        when (path.substringAfterLast(".")) {
            "id" -> Users.id
            "name" -> Users.name
            "age" -> Users.age
            "status" -> Users.status
            else -> null
        }
    }

    /**
     * 测试用用户仓储实现
     * Test user repository implementation
     */
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

    /**
     * 创建内存 SQLite 测试数据库并插入测试数据
     * Create in-memory SQLite test database and insert test data
     *
     * @return 测试数据库实例 / Test database instance
     */
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

    /**
     * 验证仓储支持 where 条件、排序、分页、更新和删除操作
     * Verify repository supports where condition, sorting, paging, update and delete operations
     */
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

    /**
     * 验证仓储支持列-列比较、算术表达式和函数比较谓词
     * Verify repository supports column-column comparison, arithmetic expression and function comparison predicates
     */
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

    /**
     * 验证不支持的谓词不会退化为全表扫描
     * Verify unsupported predicates do not degrade to full table scan
     */
    @Test
    @DisplayName("unsupported predicate does not degrade to full scan / 不支持谓词不退化为全表扫描")
    fun unsupportedPredicateDoesNotDegradeToFullScan() {
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

        // FailFast / ClientFilter 遇到不支持的谓词时返回空结果，而非退化为全表扫描。
        // FailFast / ClientFilter return empty results for unsupported predicates instead of degrading to a full scan.
        assertEquals(emptyList<User>(), failFastRepository.find(BooleanCustom("x")))
        assertEquals(emptyList<User>(), clientFilterRepository.find(BooleanCustom("x")))
    }

    /**
     * 强类型 schema（模拟 KSP 生成的 schema）
     * Strongly-typed schema (simulates KSP generated schema)
     */
    private object UserSchema : PredicateSchema<User>(), HasColumnMapping {
        val id = field(User::id)
        val name = field(User::name)
        val age = field(User::age)
        val status = field(User::status)

        override val columnMapping: Map<String, String> = mapOf(
            "id" to "id",
            "name" to "name",
            "age" to "age",
            "status" to "status"
        )
    }

    /**
     * 端到端验证：predicate DSL + ktormResolver + repository 协同工作
     /** 端到端谓词与 Ktorm 解析器集成测试 / End-to-end predicate with Ktorm resolver integration test */
     * End-to-end verification: predicate DSL + ktormResolver + repository work together
     */
    @Test
    @DisplayName("end-to-end: predicate DSL + ktormResolver + repository / 端到端：强类型谓词 + resolver + 仓储")
    fun endToEndPredicateWithKtormResolver() {
        val database = createDatabase()

        // 使用 HasColumnMapping.ktormResolver() 创建 resolver
        val schemaResolver = UserSchema.ktormResolver(Users)
        val repository = UserRepository(database, schemaResolver)

        // 使用强类型 predicate DSL 构造谓词
        val activeWhere = UserSchema.predicate { status eq "active" }
        val activeUsers = repository.find(activeWhere)
        assertEquals(2, activeUsers.size)
        assertTrue(activeUsers.all { it.status == "active" })

        // 使用复合谓词
        val compoundWhere = UserSchema.predicate { (status eq "active") and (name eq "a") }
        val compoundUsers = repository.find(compoundWhere)
        assertEquals(1, compoundUsers.size)
        assertEquals("a", compoundUsers[0].name)

        // 使用显式映射的 ktormResolver
        val explicitResolver = ktormResolver(Users, UserSchema.columnMapping)
        val explicitRepository = UserRepository(database, explicitResolver)
        val explicitUsers = explicitRepository.find(UserSchema.predicate { status eq "active" })
        assertEquals(2, explicitUsers.size)
    }
 /** Snake 用户表 / Snake users table */

    // ========== 非恒等映射端到端测试 / Non-identity mapping end-to-end test ==========

    /**
     * snake_case 列名表定义（模拟真实数据库）
     * snake_case column name table definition (simulates real database)
     */
    private object SnakeUsers : Table<Nothing>("snake_users") {
        /** Snake 用户谓词模式 / Snake user predicate schema */
        val userId = int("user_id")
        val userName = varchar("user_name")
        val userAge = int("user_age")
        val userStatus = varchar("user_status")
    }

    /**
     * snake_case 列名的强类型 schema
     * Strongly-typed schema with snake_case column names
     */
    private object SnakeUserSchema : PredicateSchema<User>(), HasColumnMapping {
        val id = field(User::id)
        val name = field(User::name)
        val age = field(User::age)
        /** 创建 Snake 数据库 / Create snake database */
        val status = field(User::status)

        override val columnMapping: Map<String, String> = mapOf(
            "id" to "user_id",
            "name" to "user_name",
            "age" to "user_age",
            "status" to "user_status"
        )
    }

    /**
     * 创建 snake_case 列名的内存 SQLite 测试数据库
     * Create in-memory SQLite test database with snake_case column names
     *
     /** Snake 用户仓储 / Snake user repository */
     * @return 测试数据库实例 / Test database instance
     */
    private fun createSnakeDatabase(): Database {
        val dbFile = Files.createTempFile("ktorm-snake-test", ".db").toFile().apply { deleteOnExit() }
        val database = Database.connect("jdbc:sqlite:${dbFile.absolutePath}")
        database.useConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("create table snake_users(user_id integer primary key, user_name text, user_age integer, user_status text)")
                stmt.execute("insert into snake_users(user_id, user_name, user_age, user_status) values (1, 'a', 10, 'active')")
                stmt.execute("insert into snake_users(user_id, user_name, user_age, user_status) values (2, 'b', 20, 'active')")
                stmt.execute("insert into snake_users(user_id, user_name, user_age, user_status) values (3, 'c', 30, 'pending')")
            }
        }
        return database
    }

    /**
     * snake_case 列名的测试用户仓储实现
     * Test user repository implementation with snake_case column names
     */
    private class SnakeUserRepository(
        database: Database,
        resolveColumn: KtormColumnResolver
    ) : KtormRepository<User>(
        database = database,
        table = SnakeUsers,
        resolveColumn = resolveColumn,
        nullsOrderSupport = NullsOrderSupport.Never
    ) {
        override fun mapToEntity(row: QueryRowSet): User {
            return User(
                id = row[SnakeUsers.userId] ?: 0,
                name = row[SnakeUsers.userName],
                age = row[SnakeUsers.userAge],
                status = row[SnakeUsers.userStatus]
            )
        }
    }

    /**
     * 端到端验证非恒等映射（snake_case 列名）
     * End-to-end verification of non-identity mapping (snake_case column names)
     */
    @Test
    @DisplayName("end-to-end: non-identity mapping (snake_case columns) / 端到端：非恒等映射（snake_case 列名）")
    fun endToEndNonIdentityMapping() {
        val database = createSnakeDatabase()

        // 属性名(id/name/age/status) != 列名(user_id/user_name/user_age/user_status)
        val schemaResolver = SnakeUserSchema.ktormResolver(SnakeUsers)
        val repository = SnakeUserRepository(database, schemaResolver)

        // 强类型谓词使用属性名（schema 字段），resolver 映射到 snake_case 列名
        val activeWhere = SnakeUserSchema.predicate { status eq "active" }
        val activeUsers = repository.find(activeWhere)
        assertEquals(2, activeUsers.size)
        assertTrue(activeUsers.all { it.status == "active" })

        // 复合谓词
        val compoundWhere = SnakeUserSchema.predicate { (status eq "active") and (name eq "a") }
        val compoundUsers = repository.find(compoundWhere)
        assertEquals(1, compoundUsers.size)
        assertEquals("a", compoundUsers[0].name)

        // 排序 + 分页
        val page = repository.find(
            where = SnakeUserSchema.predicate { status eq "active" },
            sortBy = SortBy.desc("age"),
            limit = 1,
            offset = 0
        )
        assertEquals(1, page.size)
        assertEquals(2, page[0].id)
    }
}
