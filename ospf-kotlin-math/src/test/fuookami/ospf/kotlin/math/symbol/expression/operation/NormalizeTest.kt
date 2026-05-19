/**
 * 表达式规范化测试
 * Expression Normalization Tests
 *
 * 验收标准：
 * 1. normalize 规则覆盖关键重写场景
 */
package fuookami.ospf.kotlin.math.symbol.expression.operation

import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@DisplayName("Normalize Tests / 规范化测试")
class NormalizeTest {

    private fun boolConst(value: Boolean): BooleanConstant = BooleanConstant(Trivalent(value))

    @Nested
    @DisplayName("Flatten Tests / 扁平化测试")
    inner class FlattenTests {

        @Test
        @DisplayName("Flatten nested And / 扁平化嵌套 And")
        fun testFlattenNestedAnd() {
            // And(A, And(B, C)) -> And(A, B, C)
            val nested = AndExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                AndExpression(listOf(
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2)),
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("c")), ScalarConstant(3))
                ))
            ))

            val flattened = flatten(nested)

            assertTrue(flattened is AndExpression)
            assertEquals(3, (flattened as AndExpression).operands.size)
        }

        @Test
        @DisplayName("Flatten nested Or / 扁平化嵌套 Or")
        fun testFlattenNestedOr() {
            val nested = OrExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                OrExpression(listOf(
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2)),
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("c")), ScalarConstant(3))
                ))
            ))

            val flattened = flatten(nested)

            assertTrue(flattened is OrExpression)
            assertEquals(3, (flattened as OrExpression).operands.size)
        }

        @Test
        @DisplayName("Single operand after flatten / 扁平化后单操作数")
        fun testFlattenSingleOperand() {
            val nested = AndExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))
            ))

            val flattened = flatten(nested)

            // 单操作数应该直接返回该操作数
            // Single operand should return the operand directly
            assertTrue(flattened is Comparison<*>)
        }
    }

    @Nested
    @DisplayName("Constant Folding Tests / 常量折叠测试")
    inner class ConstantFoldingTests {

        @Test
        @DisplayName("A and true -> A")
        fun testAndTrue() {
            val expr = AndExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                boolConst(true)
            ))

            val folded = constantFold(expr)

            assertTrue(folded is Comparison<*>)
        }

        @Test
        @DisplayName("A and false -> false")
        fun testAndFalse() {
            val expr = AndExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                boolConst(false)
            ))

            val folded = constantFold(expr)

            assertTrue(folded is BooleanConstant)
            assertTrue((folded as BooleanConstant).isFalse)
        }

        @Test
        @DisplayName("A or true -> true")
        fun testOrTrue() {
            val expr = OrExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                boolConst(true)
            ))

            val folded = constantFold(expr)

            assertTrue(folded is BooleanConstant)
            assertTrue((folded as BooleanConstant).isTrue)
        }

        @Test
        @DisplayName("A or false -> A")
        fun testOrFalse() {
            val expr = OrExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                boolConst(false)
            ))

            val folded = constantFold(expr)

            assertTrue(folded is Comparison<*>)
        }

        @Test
        @DisplayName("not true -> false")
        fun testNotTrue() {
            val expr = NotExpression(boolConst(true))

            val folded = constantFold(expr)

            assertTrue(folded is BooleanConstant)
            assertTrue((folded as BooleanConstant).isFalse)
        }

        @Test
        @DisplayName("not false -> true")
        fun testNotFalse() {
            val expr = NotExpression(boolConst(false))

            val folded = constantFold(expr)

            assertTrue(folded is BooleanConstant)
            assertTrue((folded as BooleanConstant).isTrue)
        }
    }

    @Nested
    @DisplayName("Deduplicate Tests / 去重测试")
    inner class DeduplicateTests {

        @Test
        @DisplayName("Remove duplicate in And / And 中去重")
        fun testDeduplicateAnd() {
            val a = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))

            val expr = AndExpression(listOf(a, a))

            val deduped = deduplicate(expr)

            assertTrue(deduped is AndExpression)
            assertEquals(1, (deduped as AndExpression).operands.size)
        }

        @Test
        @DisplayName("Remove duplicate in Or / Or 中去重")
        fun testDeduplicateOr() {
            val a = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))

            val expr = OrExpression(listOf(a, a, a))

            val deduped = deduplicate(expr)

            assertTrue(deduped is OrExpression)
            assertEquals(1, (deduped as OrExpression).operands.size)
        }

        @Test
        @DisplayName("Keep distinct operands / 保留不同操作数")
        fun testDeduplicateKeepDistinct() {
            val a = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))
            val b = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))

            val expr = AndExpression(listOf(a, b))

            val deduped = deduplicate(expr)

            assertTrue(deduped is AndExpression)
            assertEquals(2, (deduped as AndExpression).operands.size)
        }

        @Test
        @DisplayName("Do not deduplicate different In candidates / 不同 In 候选值不应被去重")
        fun testDeduplicateDifferentInCandidates() {
            val in1 = InExpression(
                ScalarReference<String>(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("pending"))
            )
            val in2 = InExpression(
                ScalarReference<String>(PropertyPath.parse("status")),
                listOf(ScalarConstant("active"), ScalarConstant("archived"))
            )

            val expr = AndExpression(listOf(in1, in2))
            val deduped = deduplicate(expr)

            assertTrue(deduped is AndExpression)
            assertEquals(2, (deduped as AndExpression).operands.size)
        }

        @Test
        @DisplayName("Do not deduplicate different PatternMatch pattern / 不同 PatternMatch 模式串不应被去重")
        fun testDeduplicateDifferentPatternMatchPattern() {
            val m1 = PatternMatch(
                ScalarReference<String>(PropertyPath.parse("name")),
                ScalarConstant("A%"),
                PatternMatchMode.Like
            )
            val m2 = PatternMatch(
                ScalarReference<String>(PropertyPath.parse("name")),
                ScalarConstant("B%"),
                PatternMatchMode.Like
            )

            val expr = OrExpression(listOf(m1, m2))
            val deduped = deduplicate(expr)

            assertTrue(deduped is OrExpression)
            assertEquals(2, (deduped as OrExpression).operands.size)
        }
    }

    @Nested
    @DisplayName("Double Negation Tests / 双重否定测试")
    inner class DoubleNegationTests {

        @Test
        @DisplayName("not(not(x)) -> x")
        fun testDoubleNegation() {
            val inner = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))

            val expr = NotExpression(NotExpression(inner))

            val eliminated = eliminateDoubleNegation(expr)

            assertTrue(eliminated is Comparison<*>)
        }

        @Test
        @DisplayName("not(not(not(x))) -> not(x)")
        fun testTripleNegation() {
            val inner = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))

            val expr = NotExpression(NotExpression(NotExpression(inner)))

            val eliminated = eliminateDoubleNegation(expr)

            assertTrue(eliminated is NotExpression)
            assertTrue((eliminated as NotExpression).operand is Comparison<*>)
        }
    }

    @Nested
    @DisplayName("De Morgan Tests / 德摩根定律测试")
    inner class DeMorganTests {

        @Test
        @DisplayName("not(A and B) -> not(A) or not(B)")
        fun testDeMorganAnd() {
            val a = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))
            val b = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))

            val expr = NotExpression(AndExpression(listOf(a, b)))

            val transformed = applyDeMorgan(expr)

            assertTrue(transformed is OrExpression)
            val or = transformed as OrExpression
            assertEquals(2, or.operands.size)
            assertTrue(or.operands[0] is NotExpression)
            assertTrue(or.operands[1] is NotExpression)
        }

        @Test
        @DisplayName("not(A or B) -> not(A) and not(B)")
        fun testDeMorganOr() {
            val a = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))
            val b = Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("b")), ScalarConstant(2))

            val expr = NotExpression(OrExpression(listOf(a, b)))

            val transformed = applyDeMorgan(expr)

            assertTrue(transformed is AndExpression)
            val and = transformed as AndExpression
            assertEquals(2, and.operands.size)
            assertTrue(and.operands[0] is NotExpression)
            assertTrue(and.operands[1] is NotExpression)
        }
    }

    @Nested
    @DisplayName("Full Normalize Tests / 完整规范化测试")
    inner class FullNormalizeTests {

        @Test
        @DisplayName("Normalize complex expression / 规范化复杂表达式")
        fun testFullNormalize() {
            val expr = AndExpression(listOf(
                Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1)),
                boolConst(true),
                AndExpression(listOf(
                    Comparison(ComparisonOperator.Eq, ScalarReference(PropertyPath.parse("a")), ScalarConstant(1))
                ))
            ))

            val normalized = normalize(expr)

            // 应该去重、折叠常量、扁平化
            // Should deduplicate, fold constants, flatten
            assertTrue(normalized is Comparison<*>)
        }
    }
}