/**
 * MongoDB 更新翻译器测试
 * MongoDB Update Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.expression.ScalarConstant
import fuookami.ospf.kotlin.math.symbol.expression.ScalarFunction
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments

@DisplayName("MongoUpdateTranslator Tests / MongoDB 更新翻译器测试")
class MongoUpdateTranslatorTest {
    private val resolver = MongoFieldNameResolver { path: String ->
        when (path.substringAfterLast(".")) {
            "name", "deletedAt", "age" -> path.substringAfterLast(".")
            else -> null
        }
    }
    private val codec = MongoClientSettings.getDefaultCodecRegistry()

    @Test
    @DisplayName("should translate set setNull setExpr / 应翻译 set setNull setExpr")
    fun shouldTranslateSetSetNullSetExpr() {
        val translator = MongoUpdateTranslator(resolver)
        val assignments = UpdateAssignments
            .set("name", "neo")
            .thenSetNull("deletedAt")
            .thenSetExpr("age", ScalarConstant(18))

        val update = translator.translate(assignments).valueOrFail().orFail()
        val json = update.toBsonDocument(BsonDocument::class.java, codec).toJson()

        assertNotNull(update)
        assertTrue(json.contains("\"\$set\""))
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"deletedAt\""))
        assertTrue(json.contains("\"age\""))
    }

    @Test
    @DisplayName("unknown fields and unsupported expressions should fail / 未知字段和不支持的表达式应失败")
    fun unknownFieldsAndUnsupportedExpressionsShouldFail() {
        val translator = MongoUpdateTranslator(resolver)
        val unknownField = translator.translate(UpdateAssignments.set("missing", 1))
        val unsupportedExpression = translator.translate(
            UpdateAssignments.set("name", "neo")
                .thenSetExpr("age", ScalarFunction("custom", listOf(ScalarConstant(1))))
        )

        assertTrue(unknownField.failed)
        assertTrue(unsupportedExpression.failed)
    }
}
