/**
 * MyBatis 列绑定器测试
 * MyBatis column binder tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema
import fuookami.ospf.kotlin.math.symbol.expression.dsl.bool

@DisplayName("MybatisColumnBinder Tests / MyBatis 列绑定器测试")
class MybatisColumnBinderTest {
    data class User(val id: Long, val userName: String, val status: String)

    private object UserSchema : PredicateSchema<User>(), HasColumnMapping {
        val id = field(User::id)
        val userName = field(User::userName)
        val status = field(User::status)

        override val columnMapping: Map<String, String> = mapOf(
            "id" to "user_id",
            "userName" to "user_name",
            "status" to "user_status"
        )
    }

    @Test
    @DisplayName("Resolve column with mapping / 通过映射解析列")
    fun resolveColumnWithMapping() {
        val binder = MybatisColumnBinder(UserSchema.columnMapping)
        assertEquals("user_id", binder.resolve("id"))
        assertEquals("user_name", binder.resolve("userName"))
        assertEquals("user_status", binder.resolve("status"))
    }

    @Test
    @DisplayName("Unmapped property falls back to snake case / 未映射属性回退到蛇形命名")
    fun unmappedPropertyFallsBackToSnakeCase() {
        val binder = MybatisColumnBinder(emptyMap())
        assertEquals("user_name", binder.resolve("userName"))
        assertEquals("status", binder.resolve("status"))
        assertEquals("id", binder.resolve("id"))
    }

    @Test
    @DisplayName("withMybatisMapping provides resolveColumn via HasColumnMapping / withMybatisMapping 通过 HasColumnMapping 提供 resolveColumn")
    fun withMybatisMappingProvidesResolveColumnViaHasColumnMapping() {
        var resolvedId: String? = null
        var resolvedUserName: String? = null
        UserSchema.withMybatisMapping {
            resolvedId = resolveColumn("id")
            resolvedUserName = resolveColumn("userName")
            bool(true)
        }
        assertEquals("user_id", resolvedId)
        assertEquals("user_name", resolvedUserName)
    }

    @Test
    @DisplayName("withMybatisMapping with explicit mapping / withMybatisMapping 显式传入映射")
    fun withMybatisMappingWithExplicitMapping() {
        val mapping = mapOf("id" to "user_id", "status" to "user_status")
        var resolvedId: String? = null
        UserSchema.withMybatisMapping(mapping) {
            resolvedId = resolveColumn("id")
            bool(true)
        }
        assertEquals("user_id", resolvedId)
    }

    @Test
    @DisplayName("withMybatisMapping without HasColumnMapping falls back to snake case / 无 HasColumnMapping 时回退到蛇形命名")
    fun withMybatisMappingWithoutHasColumnMapping() {
        val schema = object : PredicateSchema<User>() {
            val userName = field(User::userName)
        }
        var resolved: String? = null
        schema.withMybatisMapping {
            resolved = resolveColumn("userName")
            bool(true)
        }
        assertEquals("user_name", resolved)
    }
}