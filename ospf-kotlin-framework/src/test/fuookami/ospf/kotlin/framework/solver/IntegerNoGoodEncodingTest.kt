package fuookami.ospf.kotlin.framework.solver

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.operation.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.utils.functional.Failed

class IntegerNoGoodEncodingTest {
    @Test
    fun boundedIntegerEncodingHasBranchAndAtLeastOneConstraints() {
        val variable = IntVar("integer-master")
        val result = IntegerNoGoodCutEncoder.encode(
            variables = listOf(
                IntegerNoGoodVariable(
                    key = "x",
                    variable = variable,
                    value = Int64(2),
                    domain = IntegerDomain.interval(0, 3).value!!
                )
            )
        )
        assertTrue(result is Ok)
        assertEquals(2, result.value!!.auxiliaryVariables.size)
        assertTrue(result.value!!.constraints.size >= 4)

        val model = LinearMetaModel(name = "integer-master", converter = IntoValue.Identity)
        assertTrue(result.value!!.install(model) is Ok)
    }

    @Test
    fun encodingExcludesOnlyTheRequestedInteriorValue() {
        val variable = IntVar("integer-master-exact")
        val encoding = assertIs<Ok<IntegerNoGoodEncoding, *, *>>(
            IntegerNoGoodCutEncoder.encode(
                variables = listOf(
                    IntegerNoGoodVariable(
                        key = "x",
                        variable = variable,
                        value = Int64(2),
                        domain = IntegerDomain.interval(0, 3).value!!
                    )
                )
            )
        ).value
        val feasible = (0L..3L).filter { value ->
            (0..1).any { positive ->
                (0..1).any { negative ->
                    val values = linkedMapOf<Symbol, Flt64>(
                        variable to Flt64(value.toDouble()),
                        encoding.auxiliaryVariables[0] to Flt64(positive.toDouble()),
                        encoding.auxiliaryVariables[1] to Flt64(negative.toDouble())
                    )
                    encoding.constraints.all { constraint ->
                        val lhs = constraint.lhs.evaluate(MapValueProvider(values)) ?: return@all false
                        val rhs = constraint.rhs.evaluate(MapValueProvider(values)) ?: return@all false
                        when (constraint.comparison) {
                            Comparison.LE -> lhs <= rhs
                            Comparison.EQ -> lhs == rhs
                            Comparison.GE -> lhs >= rhs
                            Comparison.LT -> lhs < rhs
                            Comparison.GT -> lhs > rhs
                            Comparison.NE -> lhs != rhs
                        }
                    }
                }
            }
        }.toSet()
        assertEquals(setOf(0L, 1L, 3L), feasible)
    }

    @Test
    fun encodingExcludesLowerAndUpperBoundaryExactly() {
        for (excluded in listOf(0L, 3L)) {
            val variable = IntVar("integer-master-boundary-$excluded")
            val encoding = assertIs<Ok<IntegerNoGoodEncoding, *, *>>(
                IntegerNoGoodCutEncoder.encode(
                    variables = listOf(
                        IntegerNoGoodVariable(
                            key = "x",
                            variable = variable,
                            value = Int64(excluded),
                            domain = IntegerDomain.interval(0, 3).value!!
                        )
                    )
                )
            ).value
            val feasible = feasibleValues(variable, encoding)
            assertEquals((0L..3L).filter { it != excluded }.toSet(), feasible)
        }
    }

    @Test
    fun encodingExcludesOnlyTheRequestedMultiVariableAssignment() {
        val x = IntVar("integer-master-x")
        val y = IntVar("integer-master-y")
        val encoding = assertIs<Ok<IntegerNoGoodEncoding, *, *>>(
            IntegerNoGoodCutEncoder.encode(
                variables = listOf(
                    IntegerNoGoodVariable("x", x, Int64.one, IntegerDomain.interval(0, 2).value!!),
                    IntegerNoGoodVariable("y", y, Int64(2), IntegerDomain.interval(0, 2).value!!)
                )
            )
        ).value
        val feasible = linkedSetOf<Pair<Long, Long>>()
        for (xValue in 0L..2L) {
            for (yValue in 0L..2L) {
                val hasAuxiliaryAssignment = (0 until (1 shl encoding.auxiliaryVariables.size)).any { bits ->
                    val values = linkedMapOf<Symbol, Flt64>(
                        x to Flt64(xValue.toDouble()),
                        y to Flt64(yValue.toDouble())
                    )
                    encoding.auxiliaryVariables.forEachIndexed { index, auxiliary ->
                        values[auxiliary] = Flt64(((bits shr index) and 1).toDouble())
                    }
                    encoding.constraints.all { constraint ->
                        val lhs = constraint.lhs.evaluate(MapValueProvider(values)) ?: return@all false
                        val rhs = constraint.rhs.evaluate(MapValueProvider(values)) ?: return@all false
                        when (constraint.comparison) {
                            Comparison.LE -> lhs <= rhs
                            Comparison.EQ -> lhs == rhs
                            Comparison.GE -> lhs >= rhs
                            Comparison.LT -> lhs < rhs
                            Comparison.GT -> lhs > rhs
                            Comparison.NE -> lhs != rhs
                        }
                    }
                }
                if (hasAuxiliaryAssignment) feasible += xValue to yValue
            }
        }
        assertEquals(
            (0L..2L).flatMap { xValue -> (0L..2L).map { yValue -> xValue to yValue } }
                .filterNot { it == (1L to 2L) }
                .toSet(),
            feasible
        )
    }

    @Test
    fun encodingRejectsBigMOutsideExactFlt64IntegerRange() {
        val variable = IntVar("integer-master-large-domain")
        val domain = IntegerDomain.interval(Int64.zero, Int64(Long.MAX_VALUE))
        val result = IntegerNoGoodCutEncoder.encode(
            variables = listOf(
                IntegerNoGoodVariable(
                    key = "x",
                    variable = variable,
                    value = Int64.zero,
                    domain = assertIs<Ok<IntegerDomain, *, *>>(domain).value
                )
            )
        )
        assertIs<Failed<*, *, *>>(result)
    }

    private fun feasibleValues(
        variable: IntVar,
        encoding: IntegerNoGoodEncoding
    ): Set<Long> {
        return (0L..3L).filter { value ->
            (0 until (1 shl encoding.auxiliaryVariables.size)).any { bits ->
                val values = linkedMapOf<Symbol, Flt64>(
                    variable to Flt64(value.toDouble())
                )
                encoding.auxiliaryVariables.forEachIndexed { index, auxiliary ->
                    values[auxiliary] = Flt64(((bits shr index) and 1).toDouble())
                }
                encoding.constraints.all { constraint ->
                    val lhs = constraint.lhs.evaluate(MapValueProvider(values)) ?: return@all false
                    val rhs = constraint.rhs.evaluate(MapValueProvider(values)) ?: return@all false
                    when (constraint.comparison) {
                        Comparison.LE -> lhs <= rhs
                        Comparison.EQ -> lhs == rhs
                        Comparison.GE -> lhs >= rhs
                        Comparison.LT -> lhs < rhs
                        Comparison.GT -> lhs > rhs
                        Comparison.NE -> lhs != rhs
                    }
                }
            }
        }.toSet()
    }
}
