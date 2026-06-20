/**
 * 谓词 schema processor 校验测试
 * Predicate schema processor validation tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

import java.io.ByteArrayOutputStream
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCompilerApi::class)
@DisplayName("Predicate Schema Processor Validation Tests / 谓词 Schema Processor 校验测试")
class PredicateSchemaProcessorValidationTest {
    @Test
    @DisplayName("Reject resolver property conflict / 拒绝 resolver 属性冲突")
    fun rejectResolverPropertyConflict() {
        val result = compile(
            """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity
            data class User(val resolver: String)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("already has a property named 'resolver'"))
    }

    @Test
    @DisplayName("Reject columnMapping property conflict / 拒绝 columnMapping 属性冲突")
    fun rejectColumnMappingPropertyConflict() {
        val result = compile(
            """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity(generateColumnMapping = true)
            data class User(val columnMapping: String)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("already has a property named 'columnMapping'"))
    }

    @Test
    @DisplayName("Reject generic predicate entity / 拒绝泛型谓词实体")
    fun rejectGenericPredicateEntity() {
        val result = compile(
            """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity
            data class Box<T>(val value: T)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("cannot declare type parameters"))
    }

    @Test
    @DisplayName("Reject invalid schema name / 拒绝非法 schema 名")
    fun rejectInvalidSchemaName() {
        val result = compile(
            """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity(schemaName = "1Users")
            data class User(val id: Long)
            """.trimIndent()
        )

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("not a valid Kotlin identifier"))
    }

    @Test
    @DisplayName("Escape keyword property / 转义关键字属性")
    fun escapeKeywordProperty() {
        val entitySource = """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity(schemaName = "Users")
            data class User(val `class`: String)
            """.trimIndent()
        val result = compile(entitySource)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val generated = result.sourcesGeneratedBySymbolProcessor
            .first { it.name == "Users.kt" }
            .readText()
        assertTrue(generated.contains("val `class` = field(User::`class`)"))

        val usageResult = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin("User.kt", entitySource),
                SourceFile.kotlin("Users.kt", generated),
                SourceFile.kotlin(
                    "Usage.kt",
                    """
                    package fixture

                    fun resolveClass() = Users.resolver("class")
                    """.trimIndent()
                )
            )
            inheritClassPath = true
            jvmTarget = "17"
        }.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, usageResult.exitCode, usageResult.messages)
        assertEquals("class", usageResult.classLoader.loadClass("fixture.UsageKt")
            .getDeclaredMethod("resolveClass")
            .invoke(null))
    }

    @Test
    @DisplayName("Generate columnMapping and createBinder / 生成 columnMapping 与 createBinder")
    fun generateColumnMappingAndCreateBinder() {
        val entitySource = """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity(schemaName = "Users", generateColumnMapping = true)
            data class User(val id: Long, val status: String)
            """.trimIndent()
        val result = compile(entitySource)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val generated = result.sourcesGeneratedBySymbolProcessor
            .first { it.name == "Users.kt" }
            .readText()
        assertTrue(generated.contains("HasColumnMapping"), "Should implement HasColumnMapping")
        assertTrue(generated.contains("override val columnMapping: Map<String, String>"), "Should declare columnMapping")
        assertTrue(generated.contains("\"id\" to \"id\""), "Should map id property")
        assertTrue(generated.contains("\"status\" to \"status\""), "Should map status property")
        assertTrue(generated.contains("fun <C> createBinder(resolver: (String) -> C?): ColumnBinder<C>"), "Should declare createBinder")
    }

    @Test
    @DisplayName("Generate columnMapping with SnakeCase naming strategy / SnakeCase 命名策略生成蛇形列名")
    fun generateColumnMappingWithSnakeCaseNamingStrategy() {
        val entitySource = """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity
            import fuookami.ospf.kotlin.framework.persistence.expression.ColumnNamingStrategy

            @PredicateEntity(
                schemaName = "Users",
                generateColumnMapping = true,
                namingStrategy = ColumnNamingStrategy.SnakeCase
            )
            data class User(val id: Long, val userName: String, val firstName: String)
            """.trimIndent()
        val result = compile(entitySource)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val generated = result.sourcesGeneratedBySymbolProcessor
            .first { it.name == "Users.kt" }
            .readText()
        assertTrue(generated.contains("HasColumnMapping"), "Should implement HasColumnMapping")
        assertTrue(generated.contains("\"id\" to \"id\""), "Should map id with identity")
        assertTrue(generated.contains("\"userName\" to \"user_name\""), "Should map userName to user_name")
        assertTrue(generated.contains("\"firstName\" to \"first_name\""), "Should map firstName to first_name")
    }

    @Test
    @DisplayName("Generate columnMapping with Identity naming strategy / Identity 命名策略生成恒等列名")
    fun generateColumnMappingWithIdentityNamingStrategy() {
        val entitySource = """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity

            @PredicateEntity(schemaName = "Users", generateColumnMapping = true)
            data class User(val id: Long, val userName: String)
            """.trimIndent()
        val result = compile(entitySource)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val generated = result.sourcesGeneratedBySymbolProcessor
            .first { it.name == "Users.kt" }
            .readText()
        assertTrue(generated.contains("\"id\" to \"id\""), "Should map id with identity")
        assertTrue(generated.contains("\"userName\" to \"userName\""), "Should map userName with identity")
    }

    @Test
    @DisplayName("PredicateField annotation overrides naming strategy / @PredicateField 注解覆盖命名策略")
    fun predicateFieldOverridesNamingStrategy() {
        val entitySource = """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity
            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateField
            import fuookami.ospf.kotlin.framework.persistence.expression.ColumnNamingStrategy

            @PredicateEntity(
                schemaName = "Users",
                generateColumnMapping = true,
                namingStrategy = ColumnNamingStrategy.SnakeCase
            )
            data class User(@PredicateField("uid") val id: Long, val userName: String)
            """.trimIndent()
        val result = compile(entitySource)

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
        val generated = result.sourcesGeneratedBySymbolProcessor
            .first { it.name == "Users.kt" }
            .readText()
        assertTrue(generated.contains("\"id\" to \"uid\""), "Should use @PredicateField name for id")
        assertTrue(generated.contains("\"userName\" to \"user_name\""), "Should use SnakeCase for userName")
    }

    private fun compile(source: String): JvmCompilationResult {
        val output = ByteArrayOutputStream()
        return KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Fixture.kt", source))
            configureKsp {
                symbolProcessorProviders.add(PredicateSchemaProcessorProvider())
                withCompilation = true
            }
            inheritClassPath = true
            jvmTarget = "17"
            messageOutputStream = output
        }.compile()
    }
}