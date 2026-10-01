/**
 * MySQL MyBatis live integration test.
 * MySQL MyBatis 实时集成测试。
 */
package fuookami.ospf.kotlin.framework.persistence

import java.util.UUID
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.persistence.expression.MybatisRepository
import fuookami.ospf.kotlin.framework.persistence.expression.NullsOrder
import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MybatisColumnNameResolver
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisBackend
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisDialect
import fuookami.ospf.kotlin.framework.persistence.mybatis.MybatisSession
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.functional.*

@DisplayName("MySQL MyBatis Integration Tests / MySQL MyBatis 实时集成测试")
class MySQLMybatisIntegrationTest {
    @TableName("mybatis_matrix_records")
    class MatrixRecord(
        @field:TableId(value = "id", type = IdType.INPUT)
        var id: Int = 0,
        @field:TableField(value = "display_name")
        var displayName: String? = null,
        var status: String? = null,
        var score: Int? = null
    )

    interface MatrixRecordMapper : BaseMapper<MatrixRecord>

    private class MatrixRecordRepository(
        mapper: MatrixRecordMapper,
        policy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
    ) : MybatisRepository<MatrixRecord, MatrixRecordMapper>(
        mapper = mapper,
        resolveColumnName = MybatisColumnNameResolver { path ->
            when (path.substringAfterLast(".")) {
                "id" -> "id"
                "displayName" -> "display_name"
                "status" -> "status"
                "score" -> "score"
                else -> null
            }
        },
        unsupportedPredicatePolicy = policy,
        dialect = MybatisDialect.MySQL
    )

    @Test
    @DisplayName("repository should execute against a temporary MySQL database / 仓储应访问临时 MySQL 数据库")
    fun repositoryShouldExecuteAgainstTemporaryMysqlDatabase() {
        val configuredHost = System.getenv("OSPF_TEST_MYSQL_HOST")
        Assumptions.assumeTrue(configuredHost != null, "Set OSPF_TEST_MYSQL_HOST to enable the live MySQL integration test")
        val host = configuredHost!!.takeIf { it.isNotBlank() }
            ?: error("OSPF_TEST_MYSQL_HOST must not be blank")
        val port = System.getenv("OSPF_TEST_MYSQL_PORT")?.let {
            it.toIntOrNull() ?: error("OSPF_TEST_MYSQL_PORT must be numeric")
        } ?: 3306
        val userName = System.getenv("OSPF_TEST_MYSQL_USER") ?: error("OSPF_TEST_MYSQL_USER is required")
        val password = System.getenv("OSPF_TEST_MYSQL_PASSWORD") ?: error("OSPF_TEST_MYSQL_PASSWORD is required")
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val database = "ospf_test_$suffix"
        val adminConfig = MySQLConfig(
            url = "$host:$port",
            name = "ospf-mybatis-mysql-admin-$suffix",
            database = "information_schema",
            userName = userName,
            password = password
        )
        val admin = value(MySQL.getJdbcClient(adminConfig))
        var databaseCreated = false
        var testConfig: MySQLConfig? = null
        var backend: MybatisBackend? = null
        val sessions = mutableListOf<MybatisSession<MatrixRecord, MatrixRecordMapper>>()

        fun openSession(): MybatisSession<MatrixRecord, MatrixRecordMapper> {
            return value(backend!!.openSession<MatrixRecord, MatrixRecordMapper>(MatrixRecordMapper::class.java))
                .also { sessions += it }
        }

        try {
            val serverVersion = admin.client.sql("SELECT VERSION()").query(String::class.java).single()
            assertTrue(serverVersion.startsWith("8.4."), "Expected MySQL 8.4, got $serverVersion")
            admin.client.sql("CREATE DATABASE `${database}`").update()
            databaseCreated = true

            val config = MySQLConfig(
                url = "$host:$port",
                name = "ospf-mybatis-mysql-$suffix",
                database = database,
                userName = userName,
                password = password
            )
            testConfig = config
            backend = value(MySQL.getMybatisBackend(config))
            assertEquals("MySQL", value(MySQL.getMybatisBackend(config.key)).dialect.name)
            assertEquals("MySQL", value(MySQL.getMybatisBackend(config.name)).dialect.name)
            val jdbc = value(MySQL.getJdbcClient(config.key))
            jdbc.client.sql(
                "CREATE TABLE mybatis_matrix_records (" +
                    "id INTEGER NOT NULL PRIMARY KEY, " +
                    "display_name VARCHAR(200) NULL, " +
                    "status VARCHAR(30) NULL, " +
                    "score INTEGER NULL)"
            ).update()
            listOf(
                MatrixRecord(1, "Ada's row", "active", 10),
                MatrixRecord(2, "Bob", "active", 20),
                MatrixRecord(3, null, "pending", null),
                MatrixRecord(4, "Cora", "pending", 30)
            ).forEach { record ->
                jdbc.client.sql(
                    "INSERT INTO mybatis_matrix_records (id, display_name, status, score) VALUES (?, ?, ?, ?)"
                ).params(listOf(record.id, record.displayName, record.status, record.score)).update()
            }

            val session = openSession()
            val mapper = value(session.mapper())
            val repository = MatrixRecordRepository(mapper)
            val active = equal("status", "active")
            val unsupportedNested = AndExpression(listOf(active, BooleanCustom("unsupported")))
            val unknown = BooleanConstant(Trivalent.Unknown)
            val notUnknown = NotExpression(unknown)

            assertEquals(0L, repository.count(unknown).valueOrFail())
            assertFalse(repository.exists(unknown).valueOrFail())
            assertEquals(0, repository.update(unknown, UpdateAssignments.set("status", "unsafe")).valueOrFail())
            assertEquals(0, repository.delete(unknown).valueOrFail())
            assertEquals(0L, repository.count(notUnknown).valueOrFail())
            assertFalse(repository.exists(notUnknown).valueOrFail())
            assertEquals(0, repository.update(notUnknown, UpdateAssignments.set("status", "unsafe")).valueOrFail())
            assertEquals(0, repository.delete(notUnknown).valueOrFail())
            assertEquals(0L, repository.count(equal("status", "unsafe")).valueOrFail())
            assertEquals(4L, repository.count(BooleanConstant(Trivalent.True)).valueOrFail())

            val nullNameNotMatching = AndExpression(listOf(equal("id", 3), NotExpression(equal("displayName", "Bob"))))
            assertEquals(0L, repository.count(nullNameNotMatching).valueOrFail())
            assertFalse(repository.exists(nullNameNotMatching).valueOrFail())
            assertEquals(0, repository.update(nullNameNotMatching, UpdateAssignments.set("status", "unsafe")).valueOrFail())
            assertEquals(0, repository.delete(nullNameNotMatching).valueOrFail())
            assertEquals("pending", repository.find(equal("id", 3)).valueOrFail().single().status)

            assertEquals(1, repository.find(equal("displayName", "Ada's row")).valueOrFail().size)
            assertEquals(2L, repository.count(active).valueOrFail())
            assertTrue(repository.exists(active).valueOrFail())
            assertFalse(repository.exists(equal("status", "missing")).valueOrFail())
            assertEquals(
                listOf(2, 4),
                repository.find(
                    BooleanConstant(Trivalent.True),
                    SortBy.asc("displayName", NullsOrder.NullsLast),
                    limit = 2,
                    offset = 1
                ).valueOrFail().map { it.id }
            )
            assertEquals(
                listOf(2, 4, 3),
                repository.find(
                    BooleanConstant(Trivalent.True),
                    SortBy.asc("displayName", NullsOrder.NullsLast),
                    limit = null,
                    offset = 1
                ).valueOrFail().map { it.id }
            )
            assertEquals(
                listOf(3),
                repository.find(
                    BooleanConstant(Trivalent.True),
                    SortBy.asc("displayName", NullsOrder.NullsFirst),
                    limit = 1,
                    offset = 0
                ).valueOrFail().map { it.id }
            )
            val lengthExpression = Comparison(
                ComparisonOperator.Ge,
                ScalarFunction(
                    ScalarFunctionNames.Length,
                    listOf(ScalarReference<String>(PropertyPath.parse("displayName")))
                ),
                ScalarConstant(3)
            )
            assertEquals(listOf(1, 2, 4), repository.find(lengthExpression, null, null, null).valueOrFail().map { it.id })

            assertTrue(repository.update(unsupportedNested, UpdateAssignments.set("status", "unsafe")).failed)
            assertTrue(repository.delete(unsupportedNested).failed)
            val alwaysFalseRepository = MatrixRecordRepository(mapper, UnsupportedPredicatePolicy.AlwaysFalse)
            assertEquals(0, alwaysFalseRepository.update(unsupportedNested, UpdateAssignments.set("status", "unsafe")).valueOrFail())
            assertEquals(0, alwaysFalseRepository.delete(unsupportedNested).valueOrFail())
            assertEquals(2L, repository.count(active).valueOrFail())

            assertEquals(
                1,
                repository.update(
                    equal("id", 1),
                    UpdateAssignments.set("status", "updated").thenSetNull("score")
                ).valueOrFail()
            )
            val updated = repository.find(equal("id", 1)).valueOrFail().single()
            assertEquals("updated", updated.status)
            assertEquals(null, updated.score)
            assertEquals(2, repository.delete(equal("status", "pending")).valueOrFail())
            assertEquals(0L, repository.count(equal("status", "pending")).valueOrFail())
            session.commit().valueOrFail()
            session.close().valueOrFail()

            val rolledBack = openSession()
            val rolledBackMapper = value(rolledBack.mapper())
            assertEquals(1, rolledBackMapper.insert(MatrixRecord(90, "rollback", "temp", 90)))
            assertNotNull(rolledBackMapper.selectById(90))
            rolledBack.rollback().valueOrFail()
            assertNull(rolledBackMapper.selectById(90))
            val rolledBackRepository = MatrixRecordRepository(rolledBackMapper)
            rolledBack.close().valueOrFail()
            assertEquals(
                0L,
                jdbc.client.sql("SELECT COUNT(*) FROM mybatis_matrix_records WHERE id = ?")
                    .param(90)
                    .query(Long::class.javaObjectType)
                    .single()
            )
            assertTrue(rolledBack.mapper().failed)
            assertTrue(rolledBackRepository.find(BooleanConstant(Trivalent.True)).failed)
            assertTrue(rolledBack.commit().failed)

            val committed = openSession()
            val committedMapper = value(committed.mapper())
            assertEquals(1, committedMapper.insert(MatrixRecord(91, "commit", "temp", 91)))
            assertNotNull(committedMapper.selectById(91))
            committed.commit().valueOrFail()
            committed.close().valueOrFail()
            assertEquals(
                1L,
                jdbc.client.sql("SELECT COUNT(*) FROM mybatis_matrix_records WHERE id = ?")
                    .param(91)
                    .query(Long::class.javaObjectType)
                    .single()
            )

            val datasourceClosed = openSession()
            val staleRepository = MatrixRecordRepository(value(datasourceClosed.mapper()))
            MySQL.close(config.key).valueOrFail()
            assertTrue(staleRepository.find(BooleanConstant(Trivalent.True)).failed)
            datasourceClosed.close().valueOrFail()
        } finally {
            sessions.asReversed().forEach { it.close() }
            testConfig?.let { MySQL.close(it.key) }
            try {
                if (databaseCreated) {
                    admin.client.sql("DROP DATABASE `${database}`").update()
                }
            } finally {
                MySQL.close(adminConfig.key)
            }
        }
    }

    private fun equal(path: String, value: Any): BooleanExpression {
        return Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Any>(PropertyPath.parse(path)),
            ScalarConstant(value)
        )
    }

    private fun <T> value(result: Ret<T>): T {
        return when (result) {
            is Ok -> result.value
            is Failed -> throw AssertionError(result.error.message)
            is Fatal -> throw AssertionError(result.errors.joinToString { it.message })
        }
    }

    private fun <T> Ret<T>.valueOrFail(): T = value(this)
}
