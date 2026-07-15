/**
 * 布尔表达式解析器测试
 * Boolean Expression Parser Tests
 *
 * 验收标准：
 * 1. DSL 构造与 parser 解析可表达同一语义树
 * 2. 典型复杂表达式可解析：`(A and B) or not C`、`a.b in (1,2,3)`、`name is not null`
 */
package fuookami.ospf.kotlin.math.symbol.expression.parser

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.symbol.parse.ParseIssue
import fuookami.ospf.kotlin.math.symbol.parse.ParseIssueType
import fuookami.ospf.kotlin.utils.error.ExErr
import fuookami.ospf.kotlin.utils.functional.Failed

@DisplayName("Boolean Parser Tests / 布尔表达式解析器测试")
class BooleanParserTest {

    @Nested
    @DisplayName("Lexer Tests / 词法分析器测试")
    inner class LexerTests {

        @Test
        @DisplayName("Tokenize simple expression / 简单表达式词法分析")
        fun testTokenizeSimple() {
            val tokens = "a = 1".tokenize()

            // tokens 包含 EOF / tokens includes EOF
            assertTrue(tokens.size >= 3)
            assertEquals(TokenType.IDENTIFIER, tokens[0].type)
            assertEquals("a", tokens[0].value)
            assertEquals(TokenType.EQ, tokens[1].type)
            assertEquals(TokenType.NUMBER, tokens[2].type)
            assertEquals("1", tokens[2].value)
        }

        @Test
        @DisplayName("Tokenize path expression / 路径表达式词法分析")
        fun testTokenizePath() {
            val tokens = "user.address.city = 'Beijing'".tokenize()

            assertTrue(tokens.any { it.type == TokenType.IDENTIFIER && it.value == "user.address.city" })
            assertTrue(tokens.any { it.type == TokenType.STRING && it.value == "Beijing" })
        }

        @Test
        @DisplayName("Tokenize keywords / 关键字词法分析")
        fun testTokenizeKeywords() {
            val tokens = "a and b or not c".tokenize()

            assertTrue(tokens.any { it.type == TokenType.AND })
            assertTrue(tokens.any { it.type == TokenType.OR })
            assertTrue(tokens.any { it.type == TokenType.NOT })
        }

        @Test
        @DisplayName("Tokenize comparison operators / 比较操作符词法分析")
        fun testTokenizeComparisonOps() {
            val tokens = "a < b and c >= d".tokenize()

            assertTrue(tokens.any { it.type == TokenType.LT })
            assertTrue(tokens.any { it.type == TokenType.GE })
        }
    }

    @Nested
    @DisplayName("Parser Tests / 解析器测试")
    inner class ParserTests {

        @Test
        @DisplayName("Parse simple comparison / 解析简单比较")
        fun testParseSimpleComparison() {
            val expr = parseBooleanExpression("age > 18").value!!

            assertTrue(expr is Comparison<*>)
            val comp = expr as Comparison<*>
            assertEquals(ComparisonOperator.Gt, comp.operator)
        }

        @Test
        @DisplayName("Parse string comparison / 解析字符串比较")
        fun testParseStringComparison() {
            val expr = parseBooleanExpression("status = 'active'").value!!

            assertTrue(expr is Comparison<*>)
            val comp = expr as Comparison<*>
            assertEquals(ComparisonOperator.Eq, comp.operator)
        }

        @Test
        @DisplayName("Parse path expression / 解析路径表达式")
        fun testParsePathExpression() {
            val expr = parseBooleanExpression("user.address.city = 'Beijing'").value!!

            assertTrue(expr is Comparison<*>)
            val comp = expr as Comparison<*>
            val left = comp.left as? ScalarReference<*>
            assertNotNull(left)
            assertEquals("user.address.city", left?.path?.value)
        }

        @Test
        @DisplayName("Parse and expression / 解析逻辑与")
        fun testParseAndExpression() {
            val expr = parseBooleanExpression("age > 18 and status = 'active'").value!!

            assertTrue(expr is AndExpression)
            val and = expr as AndExpression
            assertEquals(2, and.operands.size)
        }

        @Test
        @DisplayName("Parse or expression / 解析逻辑或")
        fun testParseOrExpression() {
            val expr = parseBooleanExpression("age < 18 or age > 65").value!!

            assertTrue(expr is OrExpression)
            val or = expr as OrExpression
            assertEquals(2, or.operands.size)
        }

        @Test
        @DisplayName("Parse not expression / 解析逻辑非")
        fun testParseNotExpression() {
            val expr = parseBooleanExpression("not status = 'deleted'").value!!

            assertTrue(expr is NotExpression)
            val not = expr as NotExpression
            assertTrue(not.operand is Comparison<*>)
        }

        @Test
        @DisplayName("Parse complex expression (A and B) or not C / 解析复杂表达式")
        fun testParseComplexExpression() {
            val expr = parseBooleanExpression("(age > 18 and status = 'active') or not status = 'deleted'").value!!

            assertTrue(expr is OrExpression)
            val or = expr as OrExpression
            assertEquals(2, or.operands.size)

            // 第一部分应该是 AndExpression
            // First part should be AndExpression
            assertTrue(or.operands[0] is AndExpression)

            // 第二部分应该是 NotExpression
            // Second part should be NotExpression
            assertTrue(or.operands[1] is NotExpression)
        }

        @Test
        @DisplayName("Parse in expression / 解析 in 表达式")
        fun testParseInExpression() {
            val expr = parseBooleanExpression("status in ('active', 'pending')").value!!

            assertTrue(expr is InExpression<*>)
            val inExpr = expr as InExpression<*>
            assertFalse(inExpr.negated)
            assertEquals(2, inExpr.candidates.size)
        }

        @Test
        @DisplayName("Parse not in expression / 解析 not in 表达式")
        fun testParseNotInExpression() {
            val expr = parseBooleanExpression("status not in ('deleted', 'archived')").value!!

            assertTrue(expr is InExpression<*>)
            val inExpr = expr as InExpression<*>
            assertTrue(inExpr.negated)
            assertEquals(2, inExpr.candidates.size)
        }

        @Test
        @DisplayName("Parse is null expression / 解析 is null 表达式")
        fun testParseIsNullExpression() {
            val expr = parseBooleanExpression("name is null").value!!

            assertTrue(expr is NullCheck)
            val nullCheck = expr as NullCheck
            assertTrue(nullCheck.isNull)
            assertEquals("name", nullCheck.path.value)
        }

        @Test
        @DisplayName("Parse is not null expression / 解析 is not null 表达式")
        fun testParseIsNotNullExpression() {
            val expr = parseBooleanExpression("name is not null").value!!

            assertTrue(expr is NullCheck)
            val nullCheck = expr as NullCheck
            assertTrue(nullCheck.isNotNull)
            assertEquals("name", nullCheck.path.value)
        }

        @Test
        @DisplayName("Parse like expression / 解析 like 表达式")
        fun testParseLikeExpression() {
            val expr = parseBooleanExpression("name like 'A%'").value!!

            assertTrue(expr is PatternMatch<*>)
            val match = expr as PatternMatch<*>
            assertEquals(PatternMatchMode.Like, match.mode)
            assertFalse(match.negated)
        }

        @Test
        @DisplayName("Parse not like expression / 解析 not like 表达式")
        fun testParseNotLikeExpression() {
            val expr = parseBooleanExpression("name not like 'A%'").value!!

            assertTrue(expr is PatternMatch<*>)
            val match = expr as PatternMatch<*>
            assertEquals(PatternMatchMode.Like, match.mode)
            assertTrue(match.negated)
        }

        @Test
        @DisplayName("Parse path with is not null / 解析带路径的 is not null")
        fun testParsePathIsNotNull() {
            val expr = parseBooleanExpression("user.email is not null").value!!

            assertTrue(expr is NullCheck)
            val nullCheck = expr as NullCheck
            assertEquals("user.email", nullCheck.path.value)
        }

        @Test
        @DisplayName("Parse flattened and / 解析扁平化的 and")
        fun testParseFlattenedAnd() {
            val expr = parseBooleanExpression("a = 1 and b = 2 and c = 3").value!!

            assertTrue(expr is AndExpression)
            val and = expr as AndExpression
            assertEquals(3, and.operands.size)
        }

        @Test
        @DisplayName("Parse flattened or / 解析扁平化的 or")
        fun testParseFlattenedOr() {
            val expr = parseBooleanExpression("a = 1 or b = 2 or c = 3").value!!

            assertTrue(expr is OrExpression)
            val or = expr as OrExpression
            assertEquals(3, or.operands.size)
        }

        @Test
        @DisplayName("Operator precedence: and before or / 操作符优先级: and 先于 or")
        fun testOperatorPrecedence() {
            // a or b and c 应该解析为 a or (b and c)
            // a or b and c should parse as a or (b and c)
            val expr = parseBooleanExpression("a = 1 or b = 2 and c = 3").value!!

            assertTrue(expr is OrExpression)
            val or = expr as OrExpression
            assertEquals(2, or.operands.size)
            assertTrue(or.operands[1] is AndExpression)
        }

        @Test
        @DisplayName("Parse number values / 解析数字值")
        fun testParseNumberValues() {
            val expr = parseBooleanExpression("age >= 18").value!!

            assertTrue(expr is Comparison<*>)
            val comp = expr as Comparison<*>
            val right = comp.right as? ScalarConstant<*>
            assertNotNull(right)
        }
    }

    @Nested
    @DisplayName("Error Handling Tests / 错误处理测试")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Parse invalid expression returns null / 解析无效表达式返回 null")
        fun testParseInvalid() {
            val expr = parseBooleanExpressionOrNull("invalid")
            // 缺少操作符，应该返回 null
            // Missing operator, should return null
            assertNull(expr)
        }

        @Test
        @DisplayName("Parse empty expression fails / 解析空表达式失败")
        fun testParseEmpty() {
            assertTrue(parseBooleanExpression("").failed)
        }

        @Test
        @DisplayName("Unknown token should fail / 未知字符应失败")
        fun testUnknownTokenThrows() {
            assertTrue(parseBooleanExpression("age @ 18").failed)
        }

        @Test
        @DisplayName("Empty expression detail should contain ParseIssue / 空表达式 detail 应包含 ParseIssue")
        fun testEmptyExpressionDetail() {
            val result = parseBooleanExpression("")
            assertTrue(result is Failed<*, *, *>)

            val failed = result as Failed<*, *, *>
            val error = failed.error
            assertTrue(error is ExErr<*, *>)

            @Suppress("UNCHECKED_CAST")
            val exErr = error as ExErr<*, ParseIssue>
            val issue = exErr.value

            assertEquals(ParseIssueType.Syntax, issue.type)
            assertEquals("", issue.input)
            assertEquals(0, issue.position)
            assertNotNull(issue.message)
        }

        @Test
        @DisplayName("Invalid expression detail should contain ParseIssue with position / 无效表达式 detail 应包含带位置的 ParseIssue")
        fun testInvalidExpressionDetail() {
            val result = parseBooleanExpression("age @ 18")
            assertTrue(result is Failed<*, *, *>)

            val failed = result as Failed<*, *, *>
            val error = failed.error
            assertTrue(error is ExErr<*, *>)

            @Suppress("UNCHECKED_CAST")
            val exErr = error as ExErr<*, ParseIssue>
            val issue = exErr.value

            assertEquals(ParseIssueType.Syntax, issue.type)
            assertEquals("age @ 18", issue.input)
            assertNotNull(issue.position)
            assertTrue(issue.position!! >= 0)
            assertNotNull(issue.message)
        }

        @Test
        @DisplayName("Incomplete expression detail should contain ParseIssue / 不完整表达式 detail 应包含 ParseIssue")
        fun testIncompleteExpressionDetail() {
            val result = parseBooleanExpression("age >")
            assertTrue(result is Failed<*, *, *>)

            val failed = result as Failed<*, *, *>
            val error = failed.error
            assertTrue(error is ExErr<*, *>)

            @Suppress("UNCHECKED_CAST")
            val exErr = error as ExErr<*, ParseIssue>
            val issue = exErr.value

            assertEquals(ParseIssueType.Syntax, issue.type)
            assertEquals("age >", issue.input)
            assertNotNull(issue.position)
            assertNotNull(issue.message)
        }
    }
}
