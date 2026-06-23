/**
 * MyBatis 列绑定器测试
 * MyBatis column binder tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

/**
 * MyBatis 列绑定器单元测试
 * MyBatis column binder unit tests
 */
@DisplayName("MybatisColumnBinder Tests / MyBatis 列绑定器测试")
class MybatisColumnBinderTest {
    /**
     * 测试用用户实体
     * Test user entity
     */
    data class User(val id: Long, val userName: String, val status: String)

    /**
     * 测试用用户谓词 schema，包含列映射
     * Test user predicate schema with column mapping
     */
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

    /**
     * 验证通过 columnMapping 可正确解析列名
     * Verify column names can be resolved via columnMapping
     */
    @Test
    @DisplayName("Resolve column with mapping / 通过映射解析列")
    fun resolveColumnWithMapping() {
        val binder = MybatisColumnBinder(UserSchema.columnMapping)
        assertEquals("user_id", binder.resolve("id"))
        assertEquals("user_name", binder.resolve("userName"))
        assertEquals("user_status", binder.resolve("status"))
    }

    /**
     * 验证未映射属性回退到 camelCase -> snake_case 转换
     * Verify unmapped property falls back to camelCase -> snake_case conversion
     */
    @Test
    @DisplayName("Unmapped property falls back to snake case / 未映射属性回退到蛇形命名")
    fun unmappedPropertyFallsBackToSnakeCase() {
        val binder = MybatisColumnBinder(emptyMap())
        assertEquals("user_name", binder.resolve("userName"))
        assertEquals("status", binder.resolve("status"))
        assertEquals("id", binder.resolve("id"))
    }

    /**
     * 验证 asMybatisResolver 通过 columnMapping 解析列名
     * Verify asMybatisResolver resolves column names via columnMapping
     */
    @Test
    @DisplayName("asMybatisResolver resolves via columnMapping / asMybatisResolver 通过 columnMapping 解析")
    fun asMybatisResolverResolvesViaColumnMapping() {
        val binder = MybatisColumnBinder(UserSchema.columnMapping)
        val resolver = binder.asMybatisResolver()
        assertEquals("user_id", resolver("id"))
        assertEquals("user_name", resolver("userName"))
        assertEquals("user_status", resolver("status"))
    }

    /**
     * 验证 HasColumnMapping.mybatisResolver 扩展函数正确解析列名
     * Verify HasColumnMapping.mybatisResolver extension function resolves column names correctly
     */
    @Test
    @DisplayName("HasColumnMapping.mybatisResolver resolves columns / HasColumnMapping.mybatisResolver 解析列")
    fun hasColumnMappingMybatisResolverResolvesColumns() {
        val resolver = UserSchema.mybatisResolver()
        assertEquals("user_id", resolver("id"))
        assertEquals("user_name", resolver("userName"))
        assertEquals("user_status", resolver("status"))
    }

    /**
     * 验证使用显式映射创建的 mybatisResolver 正确解析列名
     * Verify mybatisResolver with explicit mapping resolves column names correctly
     */
    @Test
    @DisplayName("mybatisResolver with explicit mapping / 显式映射的 mybatisResolver")
    fun mybatisResolverWithExplicitMapping() {
        val resolver = mybatisResolver(UserSchema.columnMapping)
        assertEquals("user_id", resolver("id"))
        assertEquals("user_name", resolver("userName"))
        assertEquals("user_status", resolver("status"))
    }

    /**
     * 验证 predicate DSL 使用 schema 字段可编译并产生 BooleanExpression
     * Verify predicate DSL with schema fields compiles and produces BooleanExpression
     */
    @Test
    @DisplayName("predicate with schema fields compiles and produces BooleanExpression / predicate 使用 schema 字段构造表达式")
    fun predicateUsingSchemaFields() {
        // 验证 predicate { status eq "active" } 可以编译并产生 BooleanExpression
        val where = UserSchema.predicate { status eq "active" }
        assertNotNull(where) // BooleanExpression 不为 null

        // 验证 resolver 能解析表达式中的路径
        val resolver = UserSchema.mybatisResolver()
        assertEquals("user_status", resolver("status"))
    }
}
