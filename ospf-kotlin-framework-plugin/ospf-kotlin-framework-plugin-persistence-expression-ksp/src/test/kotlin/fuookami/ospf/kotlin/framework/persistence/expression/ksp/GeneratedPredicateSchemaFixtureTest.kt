/**
 * 生成 schema 形状 fixture 测试
 * Generated schema shape fixture test
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import fuookami.ospf.kotlin.math.symbol.expression.AndExpression
import fuookami.ospf.kotlin.math.symbol.expression.Comparison
import fuookami.ospf.kotlin.math.symbol.expression.PropertyPath
import fuookami.ospf.kotlin.math.symbol.expression.ScalarReference
import fuookami.ospf.kotlin.math.symbol.expression.dsl.PredicateSchema
import fuookami.ospf.kotlin.math.symbol.expression.dsl.and
import fuookami.ospf.kotlin.math.symbol.expression.dsl.gt
import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCompilerApi::class)
@DisplayName("Generated Predicate Schema Fixture Tests / 生成谓词 Schema Fixture 测试")
class GeneratedPredicateSchemaFixtureTest {
    data class User(
        val id: Long,
        val status: String,
        val age: Int,
        val deletedAt: String?
    )

    object Users : PredicateSchema<User>() {
        val id = field(User::id)
        val status = field(User::status)
        val age = field(User::age)
        val deletedAt = field(User::deletedAt)

        val resolver: (String) -> String? = { path ->
            when (path) {
                "id" -> "user_id"
                "status" -> "user_status"
                "age" -> "age"
                "deletedAt" -> "deleted_at"
                else -> null
            }
        }
    }

    @Test
    @DisplayName("Generated schema shape builds predicate / 生成 schema 形状可构造谓词")
    fun generatedSchemaShapeBuildsPredicate() {
        val where = Users.predicate {
            (status eq "active") and (age gt 18)
        }

        assertTrue(where is AndExpression)
        val operands = (where as AndExpression).operands
        val status = operands[0] as Comparison<*>
        assertEquals(PropertyPath.parse("status"), (status.left as ScalarReference<*>).path)
        assertEquals("user_status", Users.resolver("status"))
        assertEquals("deleted_at", Users.resolver("deletedAt"))
    }

    @Test
    @DisplayName("KSP generates and compiles schema / KSP 生成并编译 schema")
    fun kspGeneratesAndCompilesSchema() {
        val entitySource = """
            package fixture

            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity
            import fuookami.ospf.kotlin.framework.persistence.expression.PredicateField

            @PredicateEntity(schemaName = "Users")
            data class User(
                @PredicateField("user_id")
                val id: Long,
                @PredicateField("user_status")
                val status: String,
                val age: Int
            )
            """.trimIndent()
        val output = ByteArrayOutputStream()
        val result = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("User.kt", entitySource))
            configureKsp {
                symbolProcessorProviders.add(PredicateSchemaProcessorProvider())
                withCompilation = true
            }
            inheritClassPath = true
            jvmTarget = "17"
            messageOutputStream = output
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages + output.toString())
        val generated = result.sourcesGeneratedBySymbolProcessor
            .first { it.name == "Users.kt" }
            .readText()
        assertTrue(generated.contains("object Users : PredicateSchema<User>()"))
        assertTrue(generated.contains("\"status\" -> \"user_status\""))

        val usageResult = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin("User.kt", entitySource),
                SourceFile.kotlin("Users.kt", generated),
                SourceFile.kotlin(
                    "Usage.kt",
                    """
                    package fixture

                    import fuookami.ospf.kotlin.math.symbol.expression.dsl.and
                    import fuookami.ospf.kotlin.math.symbol.expression.dsl.gt
                    import fuookami.ospf.kotlin.math.symbol.expression.dsl.predicate

                    fun buildPredicate() = Users.predicate {
                        (status eq "active") and (age gt 18)
                    }

                    fun resolveStatus() = Users.resolver("status")
                    """.trimIndent()
                )
            )
            inheritClassPath = true
            jvmTarget = "17"
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, usageResult.exitCode, usageResult.messages)
        assertEquals("user_status", usageResult.classLoader.loadClass("fixture.UsageKt")
            .getDeclaredMethod("resolveStatus")
            .invoke(null))
    }
}
