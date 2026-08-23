/**
 * Ktorm 标量表达式翻译器测试
 * Ktorm Scalar Translator Tests
 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.ktorm.expression.ArgumentExpression
import org.ktorm.expression.BinaryExpression
import org.ktorm.expression.BinaryExpressionType
import org.ktorm.expression.FunctionExpression
import org.ktorm.schema.int
import org.ktorm.schema.Table
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicateDetail
import fuookami.ospf.kotlin.framework.persistence.expression.UnsupportedPredicatePolicy
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed

@DisplayName("KtormScalarTranslator Tests / Ktorm 标量翻译器测试")
class KtormScalarTranslatorTest {
    private object Items : Table<Nothing>("items") {
        val price = int("price")
        val quantity = int("quantity")
    }

    private val resolver = KtormColumnResolver { path: String ->
        when (path.substringAfterLast(".")) {
            "price" -> Items.price
            "quantity" -> Items.quantity
            else -> null
        }
    }

    @Test
    @DisplayName("should translate reference constant and arithmetic / 应翻译引用常量与算术表达式")
    fun shouldTranslateReferenceConstantAndArithmetic() {
        val translator = KtormScalarTranslator(resolver)
        val reference = translator.translate(ScalarReference<Int>(PropertyPath.parse("price"))).valueOrFail().orFail()
        val constant = translator.translate(ScalarConstant(100)).valueOrFail().orFail()
        val arithmetic = translator.translate(
            ScalarBinary(
                BinaryOperator.Multiply,
                ScalarReference<Int>(PropertyPath.parse("price")),
                ScalarReference(PropertyPath.parse("quantity"))
            )
        ).valueOrFail().orFail() as BinaryExpression<*>

        assertNotNull(reference)
        assertTrueArgument(constant)
        assertEquals(BinaryExpressionType.TIMES, arithmetic.type)
    }

    @Test
    @DisplayName("should bind OSPF numeric constants without column context / 无列上下文时应绑定 OSPF 数值常量")
    fun shouldBindNumericConstantsWithoutColumnContext() {
        val translator = KtormScalarTranslator(resolver)
        val uint64 = translator.translate(ScalarConstant(UInt64(7UL))).valueOrFail().orFail()
        val fltx = translator.translate(ScalarConstant(FltX("1.25"))).valueOrFail().orFail()

        assertEquals(7L, (uint64 as ArgumentExpression<*>).value)
        assertEquals(java.math.BigDecimal("1.250000000000000000"), (fltx as ArgumentExpression<*>).value)
    }

    @Test
    @DisplayName("target binders handle custom SQL types explicitly / 目标绑定器显式处理自定义 SQL 类型")
    fun targetBindersHandleCustomSqlTypesExplicitly() {
        data class CustomPayload(val value: String)
        val customType = object : org.ktorm.schema.SqlType<CustomPayload>(java.sql.Types.OTHER, "CUSTOM") {
            override fun doSetParameter(ps: java.sql.PreparedStatement, index: Int, parameter: CustomPayload) {
                ps.connection.createArrayOf("CUSTOM", arrayOf(parameter.value))
            }

            override fun doGetResult(rs: java.sql.ResultSet, index: Int): CustomPayload {
                return CustomPayload(rs.getString(index))
            }
        }
        val value = CustomPayload("payload")
        val translator = KtormScalarTranslator(
            resolveColumn = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
            targetConstantBinder = { candidate, target ->
                if (candidate === value && target == customType) {
                    KtormScalarBinding.forTarget(value, target)
                } else {
                    null
                }
            }
        )

        val result = translator.translateConstant(value, customType)

        assertTrue(result.ok)
        assertEquals(value, (result.valueOrFail().orFail() as ArgumentExpression<*>).value)
    }

    @Test
    @DisplayName("unregistered custom SQL values are rejected / 未注册自定义 SQL 值应拒绝")
    fun unregisteredCustomSqlValuesAreRejected() {
        data class CustomPayload(val value: String)
        val customType = object : org.ktorm.schema.SqlType<CustomPayload>(java.sql.Types.OTHER, "CUSTOM") {
            override fun doSetParameter(ps: java.sql.PreparedStatement, index: Int, parameter: CustomPayload) {
                ps.setObject(index, parameter.value)
            }

            override fun doGetResult(rs: java.sql.ResultSet, index: Int): CustomPayload {
                return CustomPayload(rs.getString(index))
            }
        }
        val translator = KtormScalarTranslator(
            resolveColumn = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast
        )

        val result = translator.translateConstant(object {}, customType)

        assertTrue(result.failed)
    }

    @Test
    @DisplayName("target binder exceptions become binding failures / 目标绑定器异常应转换为绑定失败")
    fun targetBinderExceptionsBecomeBindingFailures() {
        data class CustomPayload(val value: String)
        val customType = object : org.ktorm.schema.SqlType<CustomPayload>(java.sql.Types.OTHER, "CUSTOM") {
            override fun doSetParameter(ps: java.sql.PreparedStatement, index: Int, parameter: CustomPayload) {
                ps.setObject(index, parameter.value)
            }

            override fun doGetResult(rs: java.sql.ResultSet, index: Int): CustomPayload {
                return CustomPayload(rs.getString(index))
            }
        }
        val translator = KtormScalarTranslator(
            resolveColumn = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
            targetConstantBinder = { _, _ ->
                throw IllegalArgumentException("binder rejected value")
            }
        )

        val result = translator.translateConstant(CustomPayload("payload"), customType)

        assertTrue(result.failed)
        val failed = result as Failed<*, *, *>
        assertTrue(failed.error.message.contains("No SQL binding registered"))
    }

    @Test
    @DisplayName("target binder cancellation is propagated / 目标绑定器取消异常应继续传播")
    fun targetBinderCancellationIsPropagated() {
        data class CustomPayload(val value: String)
        val customType = object : org.ktorm.schema.SqlType<CustomPayload>(java.sql.Types.OTHER, "CUSTOM") {
            override fun doSetParameter(ps: java.sql.PreparedStatement, index: Int, parameter: CustomPayload) {
                ps.setObject(index, parameter.value)
            }

            override fun doGetResult(rs: java.sql.ResultSet, index: Int): CustomPayload {
                return CustomPayload(rs.getString(index))
            }
        }
        val translator = KtormScalarTranslator(
            resolveColumn = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
            targetConstantBinder = { _, _ ->
                throw CancellationException("cancelled")
            }
        )

        assertThrows<CancellationException> {
            translator.translateConstant(CustomPayload("payload"), customType)
        }
    }

    @Test
    @DisplayName("target binder errors are propagated / 目标绑定器 Error 应继续传播")
    fun targetBinderErrorsArePropagated() {
        data class CustomPayload(val value: String)
        val customType = object : org.ktorm.schema.SqlType<CustomPayload>(java.sql.Types.OTHER, "CUSTOM") {
            override fun doSetParameter(ps: java.sql.PreparedStatement, index: Int, parameter: CustomPayload) {
                ps.setObject(index, parameter.value)
            }

            override fun doGetResult(rs: java.sql.ResultSet, index: Int): CustomPayload {
                return CustomPayload(rs.getString(index))
            }
        }
        val translator = KtormScalarTranslator(
            resolveColumn = resolver,
            unsupportedPredicatePolicy = UnsupportedPredicatePolicy.FailFast,
            targetConstantBinder = { _, _ ->
                throw AssertionError("binder invariant failed")
            }
        )

        assertThrows<AssertionError> {
            translator.translateConstant(CustomPayload("payload"), customType)
        }
    }

    @Test
    @DisplayName("unsupported scalar should follow policy / 不支持标量应遵循策略")
    fun unsupportedScalarShouldFollowPolicy() {
        val alwaysFalseTranslator = KtormScalarTranslator(resolver)
        val failFastTranslator = KtormScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unresolved = alwaysFalseTranslator.translate(ScalarReference<Int>(PropertyPath.parse("unknown")))
        val failed = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(unresolved.ok)
        assertNull(unresolved.value)
        assertTrue(failed.failed)
    }

    @Test
    @DisplayName("fail fast detail should contain correct fields / FailFast detail 应包含正确字段")
    /** 验证 FailFast 详情包含正确字段 / Verify FailFast detail has correct fields */
    fun failFastDetailShouldContainCorrectFields() {
        val failFastTranslator = KtormScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val result = failFastTranslator.translate(ScalarCustom<Int>("x"))

        assertTrue(result.failed)
        assertTrue(result is Failed)

        val failed = result as Failed<*, *, *>
        val error = failed.error
        assertTrue(error is ExErr<*, *>)
        @Suppress("UNCHECKED_CAST")
        val exErr = error as ExErr<*, UnsupportedPredicateDetail>
        val detail = exErr.value

        assertEquals("ScalarExpression", detail.expressionType)
        assertEquals(UnsupportedPredicatePolicy.FailFast, detail.policy)
        assertEquals("Ktorm", detail.backendName)
    }

    @Test
    @DisplayName("should translate standard functions / 应翻译标准函数")
    fun shouldTranslateStandardFunctions() {
        val translator = KtormScalarTranslator(resolver)
        val absExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Abs,
                listOf(ScalarReference<Int>(PropertyPath.parse("price")))
            )
        ).valueOrFail().orFail() as FunctionExpression<*>
        val lowerExpr = translator.translate(
            ScalarFunction(
                ScalarFunctionNames.Lower,
                listOf(ScalarConstant("ABC"))
            )
        ).valueOrFail().orFail() as FunctionExpression<*>

        assertEquals("ABS", absExpr.functionName)
        assertEquals("LOWER", lowerExpr.functionName)
    }

    @Test
    @DisplayName("unknown function should follow policy / 未知函数应遵循策略")
    fun unknownFunctionShouldFollowPolicy() {
        val alwaysFalseTranslator = KtormScalarTranslator(resolver)
        val failFastTranslator = KtormScalarTranslator(resolver, UnsupportedPredicatePolicy.FailFast)
        val unknown = ScalarFunction("unknown", listOf(ScalarConstant(1)))
        val unsupported = alwaysFalseTranslator.translate(unknown)
        val failed = failFastTranslator.translate(unknown)

        assertTrue(unsupported.ok)
        assertNull(unsupported.value)
        assertTrue(failed.failed)
    }

    private fun assertTrueArgument(expr: org.ktorm.expression.ScalarExpression<*>?) {
        assertNotNull(expr)
        assertEquals(ArgumentExpression::class, expr!!::class)
    }
}
