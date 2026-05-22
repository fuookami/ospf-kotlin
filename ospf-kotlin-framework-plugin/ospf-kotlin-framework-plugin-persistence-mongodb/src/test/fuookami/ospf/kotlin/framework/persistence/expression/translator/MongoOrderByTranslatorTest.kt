/**
 * MongoDB 排序翻译器测试
 * MongoDB OrderBy Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.framework.persistence.expression.SortBy
import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MongoOrderByTranslator Tests / MongoDB 排序翻译器测试")
class MongoOrderByTranslatorTest {
    private val resolver: MongoFieldNameResolver = { it.substringAfterLast(".") }
    private val codec = MongoClientSettings.getDefaultCodecRegistry()

    @Test
    @DisplayName("should translate multi-field sort / 应翻译多字段排序")
    fun shouldTranslateMultiFieldSort() {
        val translator = MongoOrderByTranslator(resolver)
        val sort = SortBy.desc("age").thenAsc("name")

        val bson = translator.translate(sort)
        val json = bson!!.toBsonDocument(BsonDocument::class.java, codec).toJson()

        assertTrue(json.contains("\"age\""))
        assertTrue(json.contains("-1"))
        assertTrue(json.contains("\"name\""))
        assertTrue(json.contains("1"))
    }
}
