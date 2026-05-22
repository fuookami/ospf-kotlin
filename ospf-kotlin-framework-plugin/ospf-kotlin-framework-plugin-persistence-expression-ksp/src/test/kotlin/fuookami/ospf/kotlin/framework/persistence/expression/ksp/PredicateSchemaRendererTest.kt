/**
 * 谓词 schema 渲染器测试
 * Predicate schema renderer tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

import org.junit.jupiter.api.Assertions.assertTrue
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
                properties = listOf(PredicateProperty("age", "age", "age"))
            )
        )

        assertTrue(!code.contains("resolver"))
        assertTrue(code.contains("val age = field(User::age)"))
    }
}
