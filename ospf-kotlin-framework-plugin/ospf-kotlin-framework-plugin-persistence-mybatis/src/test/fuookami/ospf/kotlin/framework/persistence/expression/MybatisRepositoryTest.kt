/**
 * MyBatis 仓储测试
 * MyBatis Repository Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import java.lang.reflect.Proxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MybatisColumnNameResolver
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.utils.functional.*
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper

@DisplayName("MybatisRepository Tests / MyBatis 仓储测试")
class MybatisRepositoryTest {
    data class TestEntity(
        val id: Long,
        val name: String?,
        val status: String?
    )

    private data class MapperCallRecorder<E : Any>(
        var lastQueryWrapper: QueryWrapper<E>? = null,
        var lastUpdateWrapper: UpdateWrapper<E>? = null,
        var lastCountWrapper: QueryWrapper<E>? = null,
        var lastDeleteWrapper: QueryWrapper<E>? = null,
        var updateCallCount: Int = 0
    )

    private class TestRepository(
        mapper: BaseMapper<TestEntity>,
        resolver: MybatisColumnNameResolver = MybatisColumnNameResolver { it },
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
    ) : MybatisRepository<TestEntity, BaseMapper<TestEntity>>(
        mapper,
        resolver,
        unsupportedPredicatePolicy = unsupportedPredicatePolicy
    )

    @Test
    @DisplayName("update should apply where condition / update 应应用 where 条件")
    fun testUpdateShouldApplyWhereCondition() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val where = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("status")),
            ScalarConstant("active")
        )
        val assignments = UpdateAssignments.set("name", "neo")

        val updated = repository.update(where, assignments).valueOrFail()

        assertEquals(1, updated)
        assertEquals(1, recorder.updateCallCount)
        val updateWrapper = recorder.lastUpdateWrapper
        assertNotNull(updateWrapper)
        assertTrue(updateWrapper!!.sqlSet!!.contains("name"))
        assertTrue(updateWrapper.customSqlSegment.contains("status"))
    }

    @Test
    @DisplayName("find should combine limit and offset in one last clause / find 应单次合并 limit 与 offset")
    fun testFindShouldCombineLimitAndOffset() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val where = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("status")),
            ScalarConstant("active")
        )

        repository.find(where, null, 10, 20).valueOrFail()

        val queryWrapper = recorder.lastQueryWrapper
        assertNotNull(queryWrapper)
        assertTrue(queryWrapper!!.sqlSegment.uppercase().contains("LIMIT 10 OFFSET 20"))
    }

    @Test
    @DisplayName("update with false constant should become impossible condition / false 常量应转为不可能条件")
    fun testUpdateWithFalseConstantShouldBecomeImpossibleCondition() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val assignments = UpdateAssignments.set("name", "neo")

        repository.update(BooleanConstant(Trivalent.False), assignments).valueOrFail()

        val updateWrapper = recorder.lastUpdateWrapper
        assertNotNull(updateWrapper)
        assertTrue(updateWrapper!!.customSqlSegment.contains("1 = 0"))
    }

    @Test
    @DisplayName("count delete exists should keep where condition / count delete exists 应保留 where 条件")
    fun testCountDeleteExistsShouldKeepWhereCondition() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val where = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("status")),
            ScalarConstant("active")
        )

        val count = repository.count(where).valueOrFail()
        val deleted = repository.delete(where).valueOrFail()
        val exists = repository.exists(where).valueOrFail()

        assertEquals(2L, count)
        assertEquals(1, deleted)
        assertTrue(exists)
        assertTrue(recorder.lastCountWrapper!!.customSqlSegment.contains("status"))
        assertTrue(recorder.lastDeleteWrapper!!.customSqlSegment.contains("status"))
    }

    @Test
    @DisplayName("complex where should keep update condition / 复杂 where 应保留 update 条件")
    fun testComplexWhereShouldKeepUpdateCondition() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val where = Comparison(
            ComparisonOperator.Gt,
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarReference(PropertyPath.parse("id"))
            ),
            ScalarConstant(100)
        )

        repository.update(where, UpdateAssignments.set("name", "neo")).valueOrFail()

        val updateWrapper = recorder.lastUpdateWrapper
        assertNotNull(updateWrapper)
        assertTrue(updateWrapper!!.customSqlSegment.contains("(age * id) >"))
        assertTrue(updateWrapper.sqlSet!!.contains("name"))
    }

    @Test
    @DisplayName("unsupported policy should fail explicitly / 不支持策略应明确失败")
    fun testUnsupportedPolicyShouldFailExplicitly() {
        val failFastRepository = TestRepository(
            createMapperProxy(MapperCallRecorder()),
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
        )
        val clientFilterRepository = TestRepository(
            createMapperProxy(MapperCallRecorder()),
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.ClientFilter
        )

        assertTrue(failFastRepository.find(BooleanCustom("x")).failed)
        assertTrue(failFastRepository.count(BooleanCustom("x")).failed)
        assertTrue(clientFilterRepository.find(BooleanCustom("x")).failed)
    }

    @Test
    @DisplayName("AlwaysFalse should cover unsupported nodes under boolean operators / AlwaysFalse 应覆盖布尔组合中的不支持节点")
    fun alwaysFalseShouldCoverNestedUnsupportedNodes() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(
            createMapperProxy(recorder),
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
        )
        val supported = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("status")),
            ScalarConstant("active")
        )
        val nestedPredicates = listOf(
            AndExpression(listOf(supported, BooleanCustom("x"))),
            OrExpression(listOf(supported, BooleanCustom("x"))),
            NotExpression(BooleanCustom("x")),
            NotExpression(AndExpression(listOf(supported, BooleanCustom("x"))))
        )

        for (predicate in nestedPredicates) {
            assertTrue(repository.find(predicate).valueOrFail().isEmpty())
        }
        assertTrue(recorder.lastQueryWrapper!!.customSqlSegment.contains("1 = 0"))
    }

    @Test
    @DisplayName("failed update translation should not call mapper / 更新翻译失败时不调用 mapper")
    fun failedUpdateTranslationShouldNotCallMapper() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val predicate = AndExpression(listOf(
            Comparison(
                ComparisonOperator.Eq,
                ScalarReference(PropertyPath.parse("status")),
                ScalarConstant("active")
            ),
            BooleanCustom("x")
        ))

        assertTrue(repository.update(predicate, UpdateAssignments.set("name", "neo")).failed)
        assertEquals(0, recorder.updateCallCount)

        val resolver = MybatisColumnNameResolver { path ->
            if (path == "unknown") null else path
        }
        val invalidAssignmentRepository = TestRepository(createMapperProxy(recorder), resolver)
        assertTrue(invalidAssignmentRepository.update(
            BooleanConstant(Trivalent.True),
            UpdateAssignments.set("unknown", "neo")
        ).failed)
        assertEquals(0, recorder.updateCallCount)
    }

    @Test
    @DisplayName("NOT UNKNOWN must not broaden update or delete / NOT UNKNOWN 不得扩大更新或删除范围")
    fun notUnknownMustNotBroadenUpdateOrDelete() {
        val recorder = MapperCallRecorder<TestEntity>()
        val repository = TestRepository(createMapperProxy(recorder))
        val predicates = listOf(
            NotExpression(BooleanConstant(Trivalent.Unknown)),
            NotExpression(AndExpression(listOf(
                BooleanConstant(Trivalent.True),
                BooleanConstant(Trivalent.Unknown)
            )))
        )

        for (predicate in predicates) {
            repository.update(predicate, UpdateAssignments.set("name", "neo")).valueOrFail()
            val updateWhere = recorder.lastUpdateWrapper!!.customSqlSegment
            assertTrue(updateWhere.contains("NOT"))
            assertTrue(updateWhere.contains("1 = NULL"))

            repository.delete(predicate).valueOrFail()
            val deleteWhere = recorder.lastDeleteWrapper!!.customSqlSegment
            assertTrue(deleteWhere.contains("NOT"))
            assertTrue(deleteWhere.contains("1 = NULL"))
        }
    }

    @Test
    @DisplayName("mapper exceptions should become failed results / mapper 异常应返回失败结果")
    fun mapperExceptionsShouldBecomeFailedResults() {
        val mapper = Proxy.newProxyInstance(
            BaseMapper::class.java.classLoader,
            arrayOf(BaseMapper::class.java)
        ) { _, method, _ ->
            if (method.name == "selectList") throw IllegalStateException("database unavailable")
            defaultValue(method.returnType)
        } as BaseMapper<TestEntity>
        val repository = TestRepository(mapper)

        assertTrue(repository.find(BooleanConstant(Trivalent.True)).failed)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Any> createMapperProxy(recorder: MapperCallRecorder<E>): BaseMapper<E> {
        return Proxy.newProxyInstance(
            BaseMapper::class.java.classLoader,
            arrayOf(BaseMapper::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "selectList" -> {
                    recorder.lastQueryWrapper = args?.getOrNull(0) as? QueryWrapper<E>
                    emptyList<E>()
                }

                "selectCount" -> {
                    recorder.lastCountWrapper = args?.getOrNull(0) as? QueryWrapper<E>
                    2L
                }
                "update" -> {
                    recorder.updateCallCount += 1
                    recorder.lastUpdateWrapper = args?.getOrNull(1) as? UpdateWrapper<E>
                    1
                }

                "delete" -> {
                    recorder.lastDeleteWrapper = args?.getOrNull(0) as? QueryWrapper<E>
                    1
                }
                "toString" -> "BaseMapperProxy"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.getOrNull(0)
                else -> defaultValue(method.returnType)
            }
        } as BaseMapper<E>
    }

    private fun defaultValue(type: Class<*>): Any? {
        return when (type) {
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

    private fun <T> Ret<T>.valueOrFail(): T {
        return when (this) {
            is Ok -> value
            is Failed -> throw AssertionError(error.message)
            is Fatal -> throw AssertionError(errors.joinToString { it.message })
        }
    }
}
