package fuookami.ospf.kotlin.math.symbol.expression.operation

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.expression.*

class NormalizationMemoTest {
    private fun comparison(index: Int): BooleanExpression {
        return Comparison(
            operator = ComparisonOperator.Eq,
            left = ScalarReference<Int>(PropertyPath.parse("p$index")),
            right = ScalarConstant(index % 3)
        )
    }

    @Test
    fun allConfigurationsMatchFrozenReferenceIncludingStableDedupAndSortOrder() {
        val random = Random(70524)
        fun expression(depth: Int): BooleanExpression {
            if (depth == 0) return comparison(random.nextInt(5))
            return when (random.nextInt(5)) {
                0 -> BooleanConstant(Trivalent.True)
                1 -> NotExpression(expression(depth - 1))
                2 -> AndExpression(List(3) { expression(depth - 1) })
                3 -> OrExpression(List(3) { expression(depth - 1) })
                else -> comparison(random.nextInt(5))
            }
        }
        val fixtures = List(12) { expression(3) }
        for (mask in 0 until 64) {
            val config = NormalizeConfig(
                flatten = mask and 1 != 0,
                constantFolding = mask and 2 != 0,
                deduplicate = mask and 4 != 0,
                eliminateDoubleNegation = mask and 8 != 0,
                applyDeMorgan = mask and 16 != 0,
                sortOperands = mask and 32 != 0
            )
            for (fixture in fixtures) {
                assertEquals(NormalizationReference.normalize(fixture, config), normalize(fixture, config))
                assertEquals(NormalizationReference.deduplicate(fixture), deduplicate(fixture))
                assertEquals(NormalizationReference.sortOperands(fixture), sortOperands(fixture))
            }
        }
    }

    @Test
    fun shallowAtomicOperandsRetainOriginalKeyResultsWithCompactMemoPreparation() {
        val fixture = AndExpression(List(128) { comparison(it % 64) })
        for (sort in listOf(false, true)) {
            val config = NormalizeConfig(sortOperands = sort)
            assertEquals(NormalizationReference.normalize(fixture, config), normalize(fixture, config))
        }
        assertEquals(NormalizationReference.deduplicate(fixture), deduplicate(fixture))
        assertEquals(NormalizationReference.sortOperands(fixture), sortOperands(fixture))
    }

    @Test
    fun sharedScalarDagAndMutableListsAreNeverCachedAcrossCalls() {
        val arguments = mutableListOf<ScalarExpression<Int>>(ScalarReference(PropertyPath.parse("root")))
        var shared: ScalarExpression<Int> = ScalarFunction(name = "leaf", arguments = arguments)
        repeat(9) { shared = ScalarFunction(name = "shared", arguments = listOf(shared, shared)) }
        val candidates = mutableListOf<ScalarExpression<Int>>(ScalarConstant(1), ScalarConstant(2))
        val operands = mutableListOf<BooleanExpression>(
            Comparison(
                operator = ComparisonOperator.Eq,
                left = shared,
                right = ScalarConstant(1)
            ),
            InExpression(value = shared, candidates = candidates),
            comparison(3)
        )
        val fixture = AndExpression(operands + operands)
        val config = NormalizeConfig(sortOperands = true)
        val before = normalize(fixture, config).structuralKey()
        assertEquals(NormalizationReference.normalize(fixture, config), normalize(fixture, config))
        arguments.add(ScalarConstant(7))
        candidates[0] = ScalarConstant(99)
        val after = normalize(fixture, config).structuralKey()
        assertNotEquals(before, after)
        assertEquals(NormalizationReference.normalize(fixture, config), normalize(fixture, config))
    }

    @Test
    fun unknownConstantFormattingRetainsOriginalReadCountAndOrdering() {
        class StatefulValue {
            var calls = 0
            override fun toString(): String = "value_${calls++ % 3}"
        }
        val value = StatefulValue()
        val left = Comparison(
            operator = ComparisonOperator.Eq,
            left = ScalarReference<StatefulValue>(PropertyPath.parse("a")),
            right = ScalarConstant(value)
        )
        val right = Comparison(
            operator = ComparisonOperator.Eq,
            left = ScalarReference<StatefulValue>(PropertyPath.parse("b")),
            right = ScalarConstant(value)
        )
        val config = NormalizeConfig(
            flatten = false,
            constantFolding = false,
            eliminateDoubleNegation = false,
            sortOperands = true
        )
        for (amount in listOf(4, 16)) {
            val fixture = AndExpression(List(amount) { if (it % 2 == 0) left else right })
            value.calls = 0
            val expected = NormalizationReference.normalize(fixture, config)
            val originalReads = value.calls
            value.calls = 0
            val actual = normalize(fixture, config)
            assertEquals(originalReads, value.calls)
            assertEquals(expected, actual)
        }
    }
}
