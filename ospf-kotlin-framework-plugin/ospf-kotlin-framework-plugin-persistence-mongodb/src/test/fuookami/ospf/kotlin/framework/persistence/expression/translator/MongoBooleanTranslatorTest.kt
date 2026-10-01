/**
 * MongoDB 布尔翻译器测试
 * MongoDB Boolean Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import java.util.concurrent.CancellationException
import com.mongodb.MongoClientSettings
import org.bson.BsonDocument
import org.bson.conversions.Bson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy

@DisplayName("MongoBooleanTranslator Tests / MongoDB 布尔翻译器测试")
class MongoBooleanTranslatorTest {
    data class Entity(val age: Int)

    private val resolver = MongoFieldNameResolver { path: String ->
        when (path.substringAfterLast(".")) {
            "id", "name", "age", "status", "price", "quantity" -> path.substringAfterLast(".")
            "widthValue" -> "width_value"
            "widthUnitSymbol" -> "width_unit_symbol"
            else -> null
        }
    }

    private val translator = MongoBooleanTranslator(resolver)
    private val codec = MongoClientSettings.getDefaultCodecRegistry()

    private fun json(result: Ret<Bson?>): String {
        val bson = result.valueOrFail().orFail("bson is null")
        return bson.toBsonDocument(BsonDocument::class.java, codec).toJson()
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
        val customJson = json(
            MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.AlwaysFalse)
                .translate(BooleanCustom("x"))
        )

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

        val alwaysFalseTranslator = MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.AlwaysFalse)
        val actual = json(alwaysFalseTranslator.translate(expr))
        assertTrue(actual.contains("\"_id\""))
        assertTrue(actual.contains("\"\$exists\""))
    }

    @Test
    @DisplayName("should require explicit PO field path / 应仅接受显式 PO 字段路径")
    fun shouldRequireExplicitPoFieldPath() {
        val poExpr = Comparison(
            ComparisonOperator.Gt,
            ScalarReference(PropertyPath.parse("widthValue")),
            ScalarConstant(10)
        )
        val domainExpr = Comparison(
            ComparisonOperator.Gt,
            ScalarReference(PropertyPath.parse("width")),
            ScalarConstant(10)
        )

        val poJson = json(translator.translate(poExpr))
        val domainJson = json(
            MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.AlwaysFalse).translate(domainExpr)
        )

        assertTrue(poJson.contains("\"width_value\""))
        assertTrue(poJson.contains("\"\$gt\""))
        assertTrue(domainJson.contains("\"_id\""))
        assertTrue(domainJson.contains("\"\$exists\""))
    }

    @Test
    @DisplayName("column-column comparison should use expr / 列-列比较应使用 expr")
    fun columnColumnComparisonShouldUseExpr() {
        val expr = Comparison<Int>(
            ComparisonOperator.Gt,
            ScalarReference<Int>(PropertyPath.parse("age")),
            ScalarReference<Int>(PropertyPath.parse("id"))
        )

        val actual = json(translator.translate(expr))
        assertTrue(actual.contains("\"\$expr\""))
        assertTrue(actual.contains("\"\$gt\""))
        assertTrue(actual.contains("\"\$age\""))
        assertTrue(actual.contains("\"\$id\""))
    }

    @Test
    @DisplayName("property predicate should translate / 属性谓词应翻译")
    fun propertyPredicateShouldTranslate() {
        val expr = prop(Entity::age) gt 18

        val actual = json(translator.translate(expr))

        assertTrue(actual.contains("\"\$gt\""))
        assertTrue(actual.contains("\"age\""))
    }

    @Test
    @DisplayName("arithmetic comparison should use expr / 算术比较应使用 expr")
    fun arithmeticComparisonShouldUseExpr() {
        val expr = Comparison<Int>(
            ComparisonOperator.Gt,
            ScalarBinary<Int>(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference<Int>(PropertyPath.parse("quantity"))
            ),
            ScalarConstant<Int>(100)
        )

        val actual = json(translator.translate(expr))
        assertTrue(actual.contains("\"\$expr\""))
        assertTrue(actual.contains("\"\$multiply\""))
        assertTrue(actual.contains("100"))
    }

    @Test
    @DisplayName("function comparison should use expr / 函数比较应使用 expr")
    fun functionComparisonShouldUseExpr() {
        val expr = Comparison(
            ComparisonOperator.Gt,
            ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference<Int>(PropertyPath.parse("age")))
            ),
            ScalarConstant(10)
        )

        val actual = json(translator.translate(expr))
        assertTrue(actual.contains("\"\$expr\""))
        assertTrue(actual.contains("\"\$abs\""))
        assertTrue(actual.contains("10"))
    }

    @Test
    @DisplayName("fail fast should return failed for unsupported predicate / FailFast 应对不支持谓词返回失败")
    fun failFastShouldReturnFailedForUnsupportedPredicate() {
        val failFastTranslator = MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.FailFast)

        val result = failFastTranslator.translate(BooleanCustom("x"))
        assertTrue(result.failed)
    }

    @Test
    @DisplayName("fail fast detail should contain correct fields / FailFast detail 应包含正确字段")
    fun failFastDetailShouldContainCorrectFields() {
        val failFastTranslator = MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val result = failFastTranslator.translate(BooleanCustom("x"))

        assertTrue(result.failed)
        assertTrue(result is Failed)

        val failed = result as Failed<*, *, *>
        val error = failed.error
        assertTrue(error is ExErr<*, *>)
        @Suppress("UNCHECKED_CAST")
        val exErr = error as ExErr<*, UnsupportedPredicateDetail>
        val detail = exErr.value

        assertTrue(detail.expressionType.contains("Custom"))
        assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
        assertEquals("MongoDB", detail.backendName)
    }

    @Test
    @DisplayName("default policy should fail fast and preserve nested failure / 默认策略应失败并保留递归错误")
    fun defaultPolicyShouldFailFastAndPreserveNestedFailure() {
        val result = translator.translate(NotExpression(BooleanCustom("x")))

        assertTrue(result.failed)
        assertTrue(result is Failed)
        val error = (result as Failed<*, *, *>).error as ExErr<*, *>
        val detail = error.value as UnsupportedPredicateDetail
        assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
        assertTrue(detail.expressionType.contains("Custom"))
    }

    @Test
    @DisplayName("always-false should fail closed through NOT / AlwaysFalse 应在 NOT 树中保持恒假")
    fun alwaysFalseShouldFailClosedThroughNot() {
        val alwaysFalseTranslator = MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.AlwaysFalse)
        val result = alwaysFalseTranslator.translate(
            NotExpression(AndExpression(listOf(BooleanConstant(Trivalent.True), BooleanCustom("x"))))
        )

        assertTrue(json(result).contains("\"_id\""))
    }

    @Test
    @DisplayName("NOT should preserve three-valued constants through nested logic / NOT 应在嵌套逻辑中保留三值常量语义")
    fun notShouldPreserveThreeValuedConstantsThroughNestedLogic() {
        val notTrue = json(translator.translate(NotExpression(BooleanConstant(Trivalent.True))))
        val notFalse = json(translator.translate(NotExpression(BooleanConstant(Trivalent.False))))
        val notUnknown = json(translator.translate(NotExpression(BooleanConstant(Trivalent.Unknown))))
        val notAndUnknown = json(translator.translate(
            NotExpression(AndExpression(listOf(
                BooleanConstant(Trivalent.True),
                BooleanConstant(Trivalent.Unknown)
            )))
        ))
        val notOrUnknown = json(translator.translate(
            NotExpression(OrExpression(listOf(
                BooleanConstant(Trivalent.False),
                BooleanConstant(Trivalent.Unknown)
            )))
        ))

        assertTrue(notTrue.contains("\"_id\""))
        assertEquals("{}", notFalse)
        assertTrue(notUnknown.contains("\"_id\""))
        assertTrue(notAndUnknown.contains("\"_id\""))
        assertTrue(notOrUnknown.contains("\"_id\""))
    }

    @Test
    @DisplayName("later resolver errors should outrank always-false markers / 后续解析器错误应优先于恒假标记")
    fun laterResolverErrorsShouldOutrankAlwaysFalseMarkers() {
        val translator = MongoBooleanTranslator(
            MongoFieldNameResolver { path: String ->
                if (path == "broken") throw IllegalStateException("resolver")
                path
            },
            UnsupportedPredicatePolicy.AlwaysFalse
        )
        val failingOperand = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("broken")),
            ScalarConstant(1)
        )
        val scalarComparison = Comparison(
            ComparisonOperator.Eq,
            ScalarCustom<Int>("unsupported"),
            ScalarReference<Int>(PropertyPath.parse("broken"))
        )
        val expressions: List<BooleanExpression> = listOf(
            AndExpression(listOf(BooleanCustom("unsupported"), failingOperand)),
            OrExpression(listOf(BooleanCustom("unsupported"), failingOperand)),
            scalarComparison
        )

        for (expression in expressions) {
            assertTrue(translator.translate(expression).failed)
        }
    }

    @Test
    @DisplayName("client filter should return a structured failure / ClientFilter 应返回结构化失败")
    fun clientFilterShouldReturnStructuredFailure() {
        val clientFilterTranslator = MongoBooleanTranslator(resolver, UnsupportedPredicatePolicy.ClientFilter)
        val result = clientFilterTranslator.translate(BooleanCustom("x"))

        assertTrue(result.failed)
        val error = ((result as Failed<*, *, *>).error as ExErr<*, *>).value as UnsupportedPredicateDetail
        assertEquals(UnsupportedPredicatePolicy.ClientFilter, error.policy)
    }

    @Test
    @DisplayName("resolver exceptions should fail and cancellation should pass through / 解析器异常应失败且取消异常继续抛出")
    fun resolverExceptionsShouldFailAndCancellationShouldPassThrough() {
        val expr = Comparison(
            ComparisonOperator.Eq,
            ScalarReference(PropertyPath.parse("age")),
            ScalarConstant(18)
        )
        val failingTranslator = MongoBooleanTranslator(MongoFieldNameResolver { throw IllegalStateException("resolver") })
        val cancellingTranslator = MongoBooleanTranslator(
            MongoFieldNameResolver { throw CancellationException("cancelled") }
        )

        assertTrue(failingTranslator.translate(expr).failed)
        assertThrows(CancellationException::class.java) {
            cancellingTranslator.translate(expr)
        }
    }
}
