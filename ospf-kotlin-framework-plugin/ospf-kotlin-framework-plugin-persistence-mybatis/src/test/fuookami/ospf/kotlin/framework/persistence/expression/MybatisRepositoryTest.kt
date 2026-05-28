/**
 * MyBatis 仓储测试
 * MyBatis Repository Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import fuookami.ospf.kotlin.framework.persistence.expression.translator.MybatisColumnNameResolver
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import java.lang.reflect.Proxy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
        resolver: MybatisColumnNameResolver = { it },
        unsupportedPredicatePolicy: UnsupportedPredicatePolicy = UnsupportedPredicatePolicy.AlwaysFalse
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

        val updated = repository.update(where, assignments)

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

        repository.find(where, null, 10, 20)

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

        repository.update(BooleanConstant(Trivalent.False), assignments)

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

        val count = repository.count(where)
        val deleted = repository.delete(where)
        val exists = repository.exists(where)

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

        repository.update(where, UpdateAssignments.set("name", "neo"))

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

        assertThrows(IllegalArgumentException::class.java) {
            failFastRepository.find(BooleanCustom("x"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            clientFilterRepository.find(BooleanCustom("x"))
        }
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
}
