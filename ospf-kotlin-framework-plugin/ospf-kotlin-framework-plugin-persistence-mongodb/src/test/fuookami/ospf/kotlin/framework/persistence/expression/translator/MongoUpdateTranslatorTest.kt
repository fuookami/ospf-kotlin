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
import fuookami.ospf.kotlin.framework.persistence.expression.UpdateAssignments

@DisplayName("MongoUpdateTranslator Tests / MongoDB 更新翻译器测试")
class MongoUpdateTranslatorTest {
    private val resolver: MongoFieldNameResolver = { path: String -> path.substringAfterLast(".") }
    private val codec = MongoClientSettings.getDefaultCodecRegistry()

    @Test
    @DisplayName("should translate set setNull setExpr / 应翻译 set setNull setExpr")
    fun shouldTranslateSetSetNullSetExpr() {
        val translator = MongoUpdateTranslator(resolver)
        val assignments = UpdateAssignments
            .set("name", "neo")
            .thenSetNull("deletedAt")
            .thenSetExpr("age", ScalarConstant(18))

        val update = translator.translate(assignments)
        val json = update!!.toBsonDocument(BsonDocument::class.java, codec).toJson()

        assertNotNull(update)
        assertTrue(json.contains("\"\$set\""))
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("\"deletedAt\""))
        assertTrue(json.contains("\"age\""))
    }
}
