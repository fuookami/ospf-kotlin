/**
 * Ktorm 列绑定器测试
 * Ktorm column binder tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.ktorm.schema.int
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

/**
 * Ktorm 列绑定器单元测试
 * Ktorm column binder unit tests
 */
@DisplayName("KtormColumnBinder Tests / Ktorm 列绑定器测试")
class KtormColumnBinderTest {
    /**
     * 测试用用户实体
     * Test user entity
     */
    data class User(val id: Int, val name: String, val status: String)

    /**
     * 测试用用户表定义
     * Test user table definition
     */
    private object UsersTable : Table<Nothing>("users") {
        val userId = int("user_id")
        val userName = varchar("user_name")
        val userStatus = varchar("user_status")
    }

    /**
     * 测试用用户谓词 schema，包含列映射
     * Test user predicate schema with column mapping
     */
    private object UserSchema : PredicateSchema<User>(), HasColumnMapping {
        val id = field(User::id)
        val name = field(User::name)
        val status = field(User::status)

        override val columnMapping: Map<String, String> = mapOf(
            "id" to "user_id",
            "name" to "user_name",
            "status" to "user_status"
        )
    }

    /**
     * 验证通过 columnMapping 可正确解析列
     * Verify columns can be resolved via columnMapping
     */
    @Test
    @DisplayName("Resolve column with mapping / 通过映射解析列")
    fun resolveColumnWithMapping() {
        val binder = KtormColumnBinder(UsersTable, UserSchema.columnMapping)
        assertNotNull(binder.resolve("id"))
        assertNotNull(binder.resolve("status"))
    }

    /**
     * 验证无映射时回退到表列名直接匹配
     * Verify fallback to direct table column name matching when no mapping exists
     */
    @Test
    @DisplayName("Resolve column without mapping falls back to property name / 无映射时回退到属性名")
    fun resolveColumnWithoutMapping() {
        val binder = KtormColumnBinder(UsersTable, emptyMap())
        assertNotNull(binder.resolve("user_id"))
    }

    /**
     * 验证未映射路径返回 null
     * Verify unmapped path returns null
     */
    @Test
    @DisplayName("Unmapped path returns null / 未映射路径返回 null")
    fun unmappedPathReturnsNull() {
        val binder = KtormColumnBinder(UsersTable, UserSchema.columnMapping)
        assertNull(binder.resolve("unknown"))
    }

    /**
     * 验证 asKtormResolver 通过 columnMapping 解析列
     * Verify asKtormResolver resolves columns via columnMapping
     */
    @Test
    @DisplayName("asKtormResolver resolves via columnMapping / asKtormResolver 通过 columnMapping 解析")
    fun asKtormResolverResolvesViaColumnMapping() {
        val binder = KtormColumnBinder(UsersTable, UserSchema.columnMapping)
        val resolver = binder.asKtormResolver()
        assertNotNull(resolver("id"))
        assertNotNull(resolver("status"))
        assertNull(resolver("unknown"))
    }

    /**
     * 验证 HasColumnMapping.ktormResolver 扩展函数正确解析列
     * Verify HasColumnMapping.ktormResolver extension function resolves columns correctly
     */
    @Test
    @DisplayName("HasColumnMapping.ktormResolver resolves columns / HasColumnMapping.ktormResolver 解析列")
    fun hasColumnMappingKtormResolverResolvesColumns() {
        val resolver = UserSchema.ktormResolver(UsersTable)
        assertNotNull(resolver("id"))
        assertNotNull(resolver("status"))
        assertNull(resolver("unknown"))
    }

    /**
     * 验证使用显式映射创建的 ktormResolver 正确解析列
     * Verify ktormResolver with explicit mapping resolves columns correctly
     */
    @Test
    @DisplayName("ktormResolver with explicit mapping / 显式映射的 ktormResolver")
    fun ktormResolverWithExplicitMapping() {
        val resolver = ktormResolver(UsersTable, UserSchema.columnMapping)
        assertNotNull(resolver("id"))
        assertNotNull(resolver("status"))
        assertNull(resolver("unknown"))
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
        assertNotNull(where)

        // 验证 resolver 能解析表达式中的路径
        val resolver = UserSchema.ktormResolver(UsersTable)
        assertNotNull(resolver("status"))
    }
}
