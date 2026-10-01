/** JdbcClient SQL translator tests / JdbcClient SQL 翻译器测试 */
package fuookami.ospf.kotlin.framework.persistence.expression.translator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.framework.persistence.expression.*
import fuookami.ospf.kotlin.framework.persistence.jdbcclient.*

class JdbcClientTranslatorTest {
    private val resolver = JdbcClientColumnNameResolver { path ->
        when (path) {
            "age" -> "account.age"
            "name" -> "name"
            "missing" -> null
            else -> path
        }
    }

    private class TestDialect(
        private val failingIdentifier: String? = null
    ) : JdbcClientDialect {
        override val name: String = "test"

        override fun quoteIdentifier(identifier: String): Ret<String> {
            if (identifier == failingIdentifier) {
                return Failed(ErrorCode.ApplicationFailed, "quote failed / 引用失败")
            }
            return Ok("\"${identifier.replace("\"", "\"\"")}\"")
        }

        override fun pagination(limit: Int?, offset: Int?): Ret<JdbcClientSqlFragment> {
            val sql = when {
                limit != null && offset != null -> "LIMIT ? OFFSET ?"
                limit != null -> "LIMIT ?"
                offset != null -> "OFFSET ?"
                else -> ""
            }
            val parameters = listOfNotNull(limit, offset)
            return Ok(JdbcClientSqlFragment(sql, parameters))
        }

        override fun orderNulls(
            expressionSql: String,
            direction: SortDirection,
            nulls: NullsOrder?
        ): Ret<JdbcClientSqlFragment> {
            val directionSql = if (direction == SortDirection.Asc) "ASC" else "DESC"
            val nullsSql = when (nulls) {
                NullsOrder.NullsFirst -> " NULLS FIRST"
                NullsOrder.NullsLast -> " NULLS LAST"
                null -> ""
            }
            return Ok(JdbcClientSqlFragment("$expressionSql $directionSql$nullsSql"))
        }
    }

    @Test
    fun comparisonAndInValuesAreBoundInSqlOrder() {
        val translator = JdbcClientBooleanTranslator(resolver, TestDialect())
        val comparison = Comparison(
            ComparisonOperator.Eq,
            ScalarBinary(
                BinaryOperator.Add,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarConstant(2)
            ),
            ScalarConstant(8)
        )
        val comparisonResult = translator.translate(comparison)
        assertTrue(comparisonResult is Ok)
        assertEquals("(\"account\".\"age\" + ?) = ?", comparisonResult.value!!.sql)
        assertEquals(listOf(2, 8), comparisonResult.value!!.parameters)

        val inResult = translator.translate(
            InExpression(
                ScalarReference<Int>(PropertyPath.parse("age")),
                listOf(ScalarConstant(1), ScalarConstant(3))
            )
        )
        assertTrue(inResult is Ok)
        assertEquals("\"account\".\"age\" IN (?, ?)", inResult.value!!.sql)
        assertEquals(listOf(1, 3), inResult.value!!.parameters)
    }

    @Test
    fun unsupportedPredicateIsClosedAtRootAndNotIsNotInverted() {
        val translator = JdbcClientBooleanTranslator(
            resolver,
            TestDialect(),
            UnsupportedPredicatePolicy.AlwaysFalse
        )
        val unsupported = BooleanCustom("custom")
        val nested = NotExpression(AndExpression(listOf(
            Comparison(
                ComparisonOperator.Eq,
                ScalarReference<Int>(PropertyPath.parse("age")),
                ScalarConstant(1)
            ),
            unsupported
        )))
        val result = translator.translate(nested)
        assertTrue(result is Ok)
        assertEquals("1 = 0", result.value!!.sql)

        val supported = Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Int>(PropertyPath.parse("age")),
            ScalarConstant(1)
        )
        for (expression in listOf(
            AndExpression(listOf(unsupported, supported)),
            AndExpression(listOf(supported, unsupported)),
            OrExpression(listOf(unsupported, supported)),
            OrExpression(listOf(supported, unsupported))
        )) {
            val rootClosed = translator.translate(expression)
            assertTrue(rootClosed is Ok)
            assertEquals("1 = 0", rootClosed.value!!.sql)
        }
    }

    @Test
    fun unsupportedSiblingDoesNotHideDialectFailureInAndOr() {
        val translator = JdbcClientBooleanTranslator(
            resolver,
            TestDialect("broken"),
            UnsupportedPredicatePolicy.AlwaysFalse
        )
        val quoteFailure = Comparison(
            ComparisonOperator.Eq,
            ScalarReference<Int>(PropertyPath.parse("broken")),
            ScalarConstant(1)
        )
        val unsupported = BooleanCustom("custom")

        val andResult = translator.translate(AndExpression(listOf(unsupported, quoteFailure)))
        val orResult = translator.translate(OrExpression(listOf(unsupported, quoteFailure)))
        assertTrue(andResult is Failed)
        assertTrue(orResult is Failed)
    }

    @Test
    fun updateAndOrderTranslatorsRetainParameterOrderAndDialectNullOrder() {
        val dialect = TestDialect()
        val updateTranslator = JdbcClientUpdateTranslator(resolver, dialect)
        val assignments = UpdateAssignments.set("name", "O'Reilly") +
            UpdateAssignments.setNull("name") +
            UpdateAssignments.setExpr(
                "age",
                ScalarBinary(
                    BinaryOperator.Add,
                    ScalarReference<Int>(PropertyPath.parse("age")),
                    ScalarConstant(4)
                )
            )
        val updateResult = updateTranslator.translate(assignments)
        assertTrue(updateResult is Ok)
        assertEquals("\"name\" = ?, \"name\" = ?, \"account\".\"age\" = (\"account\".\"age\" + ?)", updateResult.value!!.sql)
        assertEquals(listOf("O'Reilly", null, 4), updateResult.value!!.parameters)

        val orderResult = JdbcClientOrderByTranslator(resolver, dialect).translate(
            SortBy.desc("age", NullsOrder.NullsFirst)
        )
        assertTrue(orderResult is Ok)
        assertEquals("\"account\".\"age\" DESC NULLS FIRST", orderResult.value!!.sql)
    }

    @Test
    fun standardScalarFunctionsKeepTheirArgumentsBound() {
        val result = JdbcClientScalarTranslator(resolver, TestDialect()).translate(
            ScalarFunction(ScalarFunctionNames.Lower, listOf(ScalarConstant("A'B")))
        )
        assertTrue(result is Ok)
        assertEquals("LOWER(?)", result.value!!.sql)
        assertEquals(listOf("A'B"), result.value!!.parameters)
    }

    @Test
    fun columnBinderUsesExplicitMappingsAndSnakeCaseFallback() {
        val binder = JdbcClientColumnBinder(mapOf("displayName" to "display_name"))
        assertEquals("display_name", binder.resolve("displayName"))
        assertEquals("created_at", binder.resolve("createdAt"))
        assertEquals("display_name", binder.asJdbcClientResolver()("displayName"))
    }

    @Test
    fun unsupportedUpdateExpressionFailsBeforeItCanBeExecuted() {
        val result = JdbcClientUpdateTranslator(resolver, TestDialect()).translate(
            UpdateAssignments.setExpr("age", ScalarCustom<Int>("opaque"))
        )
        assertTrue(result is Failed)
    }
}
