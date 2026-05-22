/**
 * MongoDB 布尔翻译器测试
 * MongoDB Boolean Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("MongoBooleanTranslator Tests / MongoDB 布尔翻译器测试")
class MongoBooleanTranslatorTest {
    private val resolver: MongoFieldNameResolver = { path ->
        when (path.substringAfterLast(".")) {
            "id", "name", "age", "status" -> path.substringAfterLast(".")
            else -> null
        }
    }

    private val translator = MongoBooleanTranslator(resolver)
    private val codec = MongoClientSettings.getDefaultCodecRegistry()

    private fun json(bson: Bson?): String {
        return (bson ?: error("bson is null")).toBsonDocument(BsonDocument::class.java, codec).toJson()
    }

    @Test
    @DisplayName("true should translate to empty filter / true 应翻译为空过滤器")
    fun trueShouldTranslateToEmptyFilter() {
        val actual = json(translator.translate(BooleanConstant(Trivalent.True)))
        assertTrue(actual == "{}")
    }

    @Test
    @DisplayName("false unknown custom should translate to always-false filter / false unknown custom 应翻译为恒假过滤器")
    fun falseUnknownCustomShouldTranslateToAlwaysFalseFilter() {
        val falseJson = json(translator.translate(BooleanConstant(Trivalent.False)))
        val unknownJson = json(translator.translate(BooleanConstant(Trivalent.Unknown)))
        val customJson = json(translator.translate(BooleanCustom("x")))

        assertTrue(falseJson.contains("\"_id\""))
        assertTrue(unknownJson.contains("\"\$exists\""))
        assertTrue(customJson.contains("false"))
    }

    @Test
    @DisplayName("comparison in pattern should translate to bson operators / comparison in pattern 应翻译为 bson 操作符")
    fun comparisonInPatternShouldTranslateToBsonOperators() {
        val cmp = Comparison(
            ComparisonOperator.Gt,
            ScalarReference(PropertyPath.parse("age")),
            ScalarConstant(18)
        )
        val inExpr = InExpression(
            ScalarReference(PropertyPath.parse("status")),
            listOf(ScalarConstant("active"), ScalarConstant("pending"))
        )
        val like = PatternMatch(
            ScalarReference(PropertyPath.parse("name")),
            ScalarConstant("A%"),
            PatternMatchMode.Like
        )

        assertTrue(json(translator.translate(cmp)).contains("\"\$gt\""))
        assertTrue(json(translator.translate(inExpr)).contains("\"\$in\""))
        val likeJson = json(translator.translate(like))
        assertTrue(likeJson.contains("\"\$regex\"") || likeJson.contains("\"\$regularExpression\""))
    }

    @Test
    @DisplayName("null check should distinguish null and missing / null 检查应区分空值与缺失")
    fun nullCheckShouldDistinguishNullAndMissing() {
        val isNull = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNull)
        val isNotNull = NullCheck(PropertyPath.parse("name"), NullCheckType.IsNotNull)

        val left = json(translator.translate(isNull))
        val right = json(translator.translate(isNotNull))

        assertTrue(left.contains("\"\$or\""))
        assertTrue(left.contains("\"\$exists\""))
        assertTrue(right.contains("\"\$and\""))
        assertTrue(right.contains("\"\$ne\""))
    }

    @Test
    @DisplayName("unresolved path should become always-false filter / 未解析路径应转为恒假过滤器")
    fun unresolvedPathShouldBecomeAlwaysFalseFilter() {
        val expr = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("unknown")),
            ScalarConstant(1)
        )

        val actual = json(translator.translate(expr))
        assertTrue(actual.contains("\"_id\""))
        assertTrue(actual.contains("\"\$exists\""))
    }
}
