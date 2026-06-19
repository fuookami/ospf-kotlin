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
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema
import fuookami.ospf.kotlin.math.symbol.expression.dsl.bool

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
    @DisplayName("withKtormTable provides resolveColumn via HasColumnMapping / withKtormTable 通过 HasColumnMapping 提供 resolveColumn")
    fun withKtormTableProvidesResolveColumnViaHasColumnMapping() {
        var resolvedId: Any? = null
        UserSchema.withKtormTable(UsersTable) {
            resolvedId = resolveColumn("id")
            bool(true)
        }
        assertNotNull(resolvedId)
    }

    @Test
    @DisplayName("withKtormTable with explicit mapping / withKtormTable 显式传入映射")
    fun withKtormTableWithExplicitMapping() {
        val mapping = mapOf("id" to "user_id", "name" to "user_name", "status" to "user_status")
        var resolvedId: Any? = null
        UserSchema.withKtormTable(UsersTable, mapping) {
            resolvedId = resolveColumn("id")
            bool(true)
        }
        assertNotNull(resolvedId)
    }

    @Test
    @DisplayName("withKtormTable without HasColumnMapping uses empty mapping / 无 HasColumnMapping 时使用空映射")
    fun withKtormTableWithoutHasColumnMapping() {
        val schema = object : PredicateSchema<User>() {
            val id = field(User::id)
        }
        var resolvedId: Any? = null
        schema.withKtormTable(UsersTable) {
            resolvedId = resolveColumn("id")
            bool(true)
        }
        assertNull(resolvedId)
    }
}