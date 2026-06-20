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
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema

@DisplayName("KtormColumnBinder Tests / Ktorm 列绑定器测试")
class KtormColumnBinderTest {
    data class User(val id: Int, val name: String, val status: String)

    private object UsersTable : Table<Nothing>("users") {
        val userId = int("user_id")
        val userName = varchar("user_name")
        val userStatus = varchar("user_status")
    }

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

    @Test
    @DisplayName("Resolve column with mapping / 通过映射解析列")
    fun resolveColumnWithMapping() {
        val binder = KtormColumnBinder(UsersTable, UserSchema.columnMapping)
        assertNotNull(binder.resolve("id"))
        assertNotNull(binder.resolve("status"))
    }

    @Test
    @DisplayName("Resolve column without mapping falls back to property name / 无映射时回退到属性名")
    fun resolveColumnWithoutMapping() {
        val binder = KtormColumnBinder(UsersTable, emptyMap())
        assertNotNull(binder.resolve("user_id"))
    }

    @Test
    @DisplayName("Unmapped path returns null / 未映射路径返回 null")
    fun unmappedPathReturnsNull() {
        val binder = KtormColumnBinder(UsersTable, UserSchema.columnMapping)
        assertNull(binder.resolve("unknown"))
    }

    @Test
    @DisplayName("asKtormResolver resolves via columnMapping / asKtormResolver 通过 columnMapping 解析")
    fun asKtormResolverResolvesViaColumnMapping() {
        val binder = KtormColumnBinder(UsersTable, UserSchema.columnMapping)
        val resolver = binder.asKtormResolver()
        assertNotNull(resolver("id"))
        assertNotNull(resolver("status"))
        assertNull(resolver("unknown"))
    }

    @Test
    @DisplayName("HasColumnMapping.ktormResolver resolves columns / HasColumnMapping.ktormResolver 解析列")
    fun hasColumnMappingKtormResolverResolvesColumns() {
        val resolver = UserSchema.ktormResolver(UsersTable)
        assertNotNull(resolver("id"))
        assertNotNull(resolver("status"))
        assertNull(resolver("unknown"))
    }

    @Test
    @DisplayName("ktormResolver with explicit mapping / 显式映射的 ktormResolver")
    fun ktormResolverWithExplicitMapping() {
        val resolver = ktormResolver(UsersTable, UserSchema.columnMapping)
        assertNotNull(resolver("id"))
        assertNotNull(resolver("status"))
        assertNull(resolver("unknown"))
    }

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
