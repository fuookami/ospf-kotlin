/**
 * 谓词 schema 渲染器测试
 * Predicate schema renderer tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Predicate Schema Renderer Tests / 谓词 Schema 渲染器测试")
class PredicateSchemaRendererTest {
    @Test
    @DisplayName("Render schema and resolver / 渲染 schema 与 resolver")
    fun renderSchemaAndResolver() {
        val code = PredicateSchemaRenderer.render(
            PredicateSchemaModel(
                packageName = "example",
                entityName = "User",
                kotlinEntityName = "User",
                schemaName = "Users",
                generateResolver = true,
                generateColumnMapping = false,
                properties = listOf(
                    PredicateProperty("id", "user_id", "id"),
                    PredicateProperty("status", "user_status", "status"),
                    PredicateProperty("age", "age", "age")
                )
            )
        )

        assertTrue(code.contains("object Users : PredicateSchema<User>()"))
        assertTrue(code.contains("val status = field(User::status)"))
        assertTrue(code.contains("\"status\" -> \"user_status\""))
        assertFalse(code.contains("HasColumnMapping"))
        assertFalse(code.contains("columnMapping"))
        assertFalse(code.contains("createBinder"))
    }

    @Test
    @DisplayName("Skip resolver when disabled / 禁用时不生成 resolver")
    fun skipResolverWhenDisabled() {
        val code = PredicateSchemaRenderer.render(
            PredicateSchemaModel(
                packageName = "",
                entityName = "User",
                kotlinEntityName = "User",
                schemaName = "UserSchema",
                generateResolver = false,
                generateColumnMapping = false,
                properties = listOf(PredicateProperty("age", "age", "age"))
            )
        )

        assertFalse(code.contains("val resolver:"))
        assertTrue(code.contains("val age = field(User::age)"))
    }

    @Test
    @DisplayName("Render columnMapping and createBinder when enabled / 启用时生成 columnMapping 与 createBinder")
    fun renderColumnMappingWhenEnabled() {
        val code = PredicateSchemaRenderer.render(
            PredicateSchemaModel(
                packageName = "example",
                entityName = "User",
                kotlinEntityName = "User",
                schemaName = "Users",
                generateResolver = true,
                generateColumnMapping = true,
                properties = listOf(
                    PredicateProperty("id", "user_id", "id"),
                    PredicateProperty("status", "user_status", "status")
                )
            )
        )

        assertTrue(code.contains("HasColumnMapping"))
        assertTrue(code.contains("override val columnMapping: Map<String, String> = mapOf("))
        assertTrue(code.contains("\"id\" to \"user_id\""))
        assertTrue(code.contains("\"status\" to \"user_status\""))
        assertTrue(code.contains("fun <C> createBinder(resolver: (String) -> C?): ColumnBinder<C>"))
        assertTrue(code.contains("val backendName = columnMapping[path] ?: path"))
        assertTrue(code.contains("import fuookami.ospf.kotlin.framework.persistence.expression.HasColumnMapping"))
        assertTrue(code.contains("import fuookami.ospf.kotlin.framework.persistence.expression.ColumnBinder"))
    }

    @Test
    @DisplayName("Render columnMapping without resolver property / 不生成 resolver 属性时仅生成 columnMapping")
    fun renderColumnMappingWithoutResolverProperty() {
        val code = PredicateSchemaRenderer.render(
            PredicateSchemaModel(
                packageName = "example",
                entityName = "User",
                kotlinEntityName = "User",
                schemaName = "Users",
                generateResolver = false,
                generateColumnMapping = true,
                properties = listOf(PredicateProperty("id", "user_id", "id"))
            )
        )

        assertFalse(code.contains("val resolver: (String) -> String?"))
        assertTrue(code.contains("HasColumnMapping"))
        assertTrue(code.contains("columnMapping"))
        assertTrue(code.contains("createBinder"))
    }
}