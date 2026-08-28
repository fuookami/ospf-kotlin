package fuookami.ospf.kotlin.example.linear_function

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.ConditionBounds
import fuookami.ospf.kotlin.core.symbol.function.IfFunction
import fuookami.ospf.kotlin.core.symbol.function.IfThenFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Int64

/**
 * 固定边界和范围回退的条件函数求解回归测试。
 * Solve regressions for fixed boundaries and conditional-function range fallback.
 */
class ConditionalFunctionSolveRegressionTest {
    private val converter = IntoValue.Identity
    private val gap = Flt64(0.1)

    @Test
    fun configuredSolverProfileShouldSelectTheRequestedBackend() {
        val cases = conditionalSolverCasesOrSkip()
        val requestedName = System.getProperty("ospf.conditional.solver")
            ?.trim()
            ?.lowercase()

        if (requestedName == null || requestedName.isEmpty()) {
            assertEquals(setOf("scip", "gurobi"), cases.map { it.name }.toSet())
        } else {
            assertEquals(1, cases.size)
            assertEquals(requestedName, cases.single().name)
        }

        cases.forEach { solverCase ->
            assertEquals(solverCase.name, solverCase.create().name)
        }
    }

    @Test
    fun fixedBoundaryInputsCoverAllSupportedRelations() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            listOf(
                FixedRelationCase("gt_true_boundary", Comparison.GT, Flt64(0.1), Flt64.one),
                FixedRelationCase("gt_false_boundary", Comparison.GT, Flt64.zero, Flt64.zero),
                FixedRelationCase("ge_true_boundary", Comparison.GE, Flt64.zero, Flt64.one),
                FixedRelationCase("ge_false_boundary", Comparison.GE, Flt64(-0.1), Flt64.zero),
                FixedRelationCase("lt_true_boundary", Comparison.LT, Flt64(-0.1), Flt64.one),
                FixedRelationCase("lt_false_boundary", Comparison.LT, Flt64.zero, Flt64.zero),
                FixedRelationCase("le_true_boundary", Comparison.LE, Flt64.zero, Flt64.one),
                FixedRelationCase("le_false_boundary", Comparison.LE, Flt64(0.1), Flt64.zero)
            ).forEach { testCase ->
                assertNumericallyEqual(
                    testCase.expected,
                    solveFixedIf(testCase, solverCase),
                    "${solverCase.name}: ${testCase.name}"
                )
            }
        }
    }

    @Test
    fun continuousGapIsInfeasibleWhenTheInputIsFixedInsideTheInterval() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            val source = LinearFunctionSymbolAdapter(
                IfThenFunction(
                    condition = constant(Flt64.one),
                    thenPoly = constant(Flt64(0.05)),
                    converter = converter,
                    strictBoundary = gap,
                    name = "conditional_gap_source_${solverCase.name}"
                ),
                converter = converter
            )
            val condition = LinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64.one, source)),
                constant = Flt64.zero
            )
            val function = IfFunction(
                condition = condition,
                converter = converter,
                relation = Comparison.GT,
                strictBoundary = gap,
                conditionBounds = ConditionBounds(Flt64(-1.0), Flt64.one),
                name = "conditional_gap_outer_${solverCase.name}"
            )
            val symbol = LinearFunctionSymbolAdapter(function, converter)
            val model = LinearMetaModel<Flt64>(
                name = "conditional-gap-solve-${solverCase.name}",
                converter = converter
            )

            try {
                assertTrue(model.add(source) is Ok, "${solverCase.name}: source should be accepted")
                assertTrue(model.add(symbol) is Ok, "${solverCase.name}: outer condition should be accepted")
                assertTrue(model.minimize(symbol) is Ok, "${solverCase.name}: objective should be accepted")

                val result = runBlocking { solveConditionalMetaModel(solverCase.create(), model) }
                val report = result.value ?: error("${solverCase.name}: solver should return an infeasible report")
                assertEquals(ProblemStatus.Infeasible, report.problemStatus, solverCase.name)
                assertTrue(report.solution == null, "${solverCase.name}: infeasible model must not have an incumbent")
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun ifThenSolvePreservesPositiveZeroNegativeAndFalseResults() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            listOf(
                Triple("positive", Flt64.one, Flt64(2.0)),
                Triple("zero", Flt64.one, Flt64.zero),
                Triple("negative", Flt64.one, Flt64(-2.0)),
                Triple("false", Flt64.zero, Flt64(2.0))
            ).forEach { (name, conditionValue, thenValue) ->
                val function = IfThenFunction(
                    condition = constant(conditionValue),
                    thenPoly = constant(thenValue),
                    converter = converter,
                    strictBoundary = gap,
                    name = "conditional_then_${solverCase.name}_$name"
                )
                val symbol = LinearFunctionSymbolAdapter(function, converter)
                val model = LinearMetaModel<Flt64>(
                    name = "conditional-then-${solverCase.name}-$name",
                    converter = converter
                )

                try {
                    assertTrue(model.add(symbol) is Ok, "${solverCase.name}: $name")
                    assertTrue(model.minimize(symbol) is Ok, "${solverCase.name}: $name")

                    val result = runBlocking { solveConditionalMetaModel(solverCase.create(), model) }
                    val report = result.value
                        ?: error("${solverCase.name}: solver should return a feasible report for $name")
                    assertEquals(ProblemStatus.Feasible, report.problemStatus, "${solverCase.name}: $name")
                    val resultVariable = function.helperVariables.first {
                        it.name == "conditional_then_${solverCase.name}_${name}_y"
                    }
                    val actual = model.tokens.find(resultVariable)?.result
                    val expected = if (name == "false") Flt64.zero else thenValue
                    assertNumericallyEqual(expected, actual, "${solverCase.name}: $name")
                } finally {
                    model.close()
                }
            }
        }
    }

    @Test
    fun ifThenUsesExplicitThenBoundsWhenAdapterRangeIsUnbounded() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            val sourceFunction = IfThenFunction(
                condition = constant(Flt64.one),
                thenPoly = constant(Flt64(2.0)),
                converter = converter,
                strictBoundary = gap,
                name = "conditional_range_source_${solverCase.name}"
            )
            val source = LinearFunctionSymbolAdapter(sourceFunction, converter)
            val targetFunction = IfThenFunction(
                condition = constant(Flt64.one),
                thenPoly = LinearPolynomial(
                    monomials = listOf(LinearMonomial(Flt64.one, source)),
                    constant = Flt64.zero
                ),
                converter = converter,
                strictBoundary = gap,
                thenBounds = ConditionBounds(Flt64(2.0), Flt64(2.0)),
                name = "conditional_range_target_${solverCase.name}"
            )
            val target = LinearFunctionSymbolAdapter(targetFunction, converter)
            val model = LinearMetaModel<Flt64>(
                name = "conditional-range-solve-${solverCase.name}",
                converter = converter
            )

            try {
                assertTrue(model.add(source) is Ok, solverCase.name)
                assertTrue(model.add(target) is Ok, solverCase.name)
                assertTrue(model.minimize(target) is Ok, solverCase.name)

                val result = runBlocking { solveConditionalMetaModel(solverCase.create(), model) }
                val report = result.value ?: error("${solverCase.name}: solver should return a feasible report")
                assertEquals(ProblemStatus.Feasible, report.problemStatus, solverCase.name)
                val resultVariable = targetFunction.helperVariables.first {
                    it.name == "conditional_range_target_${solverCase.name}_y"
                }
                assertNumericallyEqual(
                    Flt64(2.0),
                    resultVariable.upperBound!!.value.unwrap(),
                    "${solverCase.name}: target result range should use explicit then bounds"
                )
                assertNumericallyEqual(
                    Flt64(2.0),
                    model.tokens.find(resultVariable)?.result,
                    "${solverCase.name}: target result should preserve the explicit then bound"
                )
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun integerDeltaFiveUsesIntVarAtThresholdNeighborsForAllRelations() {
        conditionalSolverCasesOrSkip().forEach { solverCase ->
            listOf(
                IntegerThresholdCase("threshold_lower", 1, Flt64(-5.0)),
                IntegerThresholdCase("threshold", 2, Flt64.zero),
                IntegerThresholdCase("threshold_upper", 3, Flt64(5.0))
            ).forEach { thresholdCase ->
                listOf(
                    IntegerRelationCase(
                        "gt",
                        Comparison.GT,
                        if (thresholdCase.value >= 3) Flt64.one else Flt64.zero
                    ),
                    IntegerRelationCase(
                        "ge",
                        Comparison.GE,
                        if (thresholdCase.value >= 2) Flt64.one else Flt64.zero
                    ),
                    IntegerRelationCase(
                        "lt",
                        Comparison.LT,
                        if (thresholdCase.value <= 1) Flt64.one else Flt64.zero
                    ),
                    IntegerRelationCase(
                        "le",
                        Comparison.LE,
                        if (thresholdCase.value <= 2) Flt64.one else Flt64.zero
                    )
                ).forEach { relationCase ->
                    // The integer variable and coefficient establish a 5-unit condition lattice.
                    // 整数变量与系数共同建立 5 单位的条件离散格点。
                    val x = IntVar(
                        "conditional_integer_${solverCase.name}_${relationCase.name}_${thresholdCase.name}_x"
                    )
                    x.range.geq(Int64.zero)
                    x.range.leq(Int64(4))
                    val condition = LinearPolynomial(
                        monomials = listOf(LinearMonomial(Flt64(5.0), x)),
                        constant = Flt64(-10.0)
                    )
                    val function = IfFunction(
                        condition = condition,
                        converter = converter,
                        relation = relationCase.relation,
                        strictBoundary = gap,
                        // delta is expressed in the same units as condition = 5 * x - 10.
                        // delta 与 condition = 5 * x - 10 使用相同单位。
                        delta = Flt64(5.0),
                        conditionBounds = ConditionBounds(Flt64(-10.0), Flt64(10.0)),
                        name = "conditional_integer_${solverCase.name}_${relationCase.name}_${thresholdCase.name}"
                    )
                    val symbol = LinearFunctionSymbolAdapter(function, converter)
                    val model = LinearMetaModel<Flt64>(
                        name = "conditional-integer-${solverCase.name}-${relationCase.name}-${thresholdCase.name}",
                        converter = converter
                    )

                    try {
                        assertTrue(model.add(x) is Ok, "${solverCase.name}: x should be accepted")
                        assertTrue(
                            model.addConstraint(
                                LinearInequality(
                                    lhs = LinearPolynomial(
                                        monomials = listOf(LinearMonomial(Flt64.one, x)),
                                        constant = Flt64.zero
                                    ),
                                    rhs = LinearPolynomial(
                                        monomials = emptyList(),
                                        constant = Flt64(thresholdCase.value.toDouble())
                                    ),
                                    comparison = Comparison.EQ,
                                    name = "${function.name}_fixed_input"
                                )
                            ) is Ok,
                            "${solverCase.name}: fixed integer input should be accepted"
                        )
                        assertTrue(model.add(symbol) is Ok, "${solverCase.name}: function should be accepted")
                        assertTrue(model.minimize(symbol) is Ok, "${solverCase.name}: objective should be accepted")

                        val result = runBlocking { solveConditionalMetaModel(solverCase.create(), model) }
                        val report = result.value
                            ?: error("${solverCase.name}: solver should return a report")
                        assertEquals(
                            ProblemStatus.Feasible,
                            report.problemStatus,
                            "${solverCase.name}: ${relationCase.name}/${thresholdCase.name}"
                        )
                        assertNumericallyEqual(
                            relationCase.expected,
                            model.tokens.find(function.resultVar)?.result,
                            "${solverCase.name}: ${relationCase.name}/${thresholdCase.name} " +
                                "condition=${thresholdCase.conditionValue.toDouble()} result"
                        )
                    } finally {
                        model.close()
                    }
                }
            }
        }
    }

    private fun solveFixedIf(testCase: FixedRelationCase, solverCase: ConditionalSolverCase): Flt64? {
        val x = RealVar("conditional_relation_${testCase.name}_x")
        x.range.geq(testCase.value)
        x.range.leq(testCase.value)
        val function = IfFunction(
            condition = linear(x),
            converter = converter,
            relation = testCase.relation,
            strictBoundary = gap,
            name = "conditional_relation_${testCase.name}"
        )
        val symbol = LinearFunctionSymbolAdapter(function, converter)
        val model = LinearMetaModel<Flt64>(
            name = "conditional-relation-${testCase.name}",
            converter = converter
        )

        try {
            assertTrue(model.add(x) is Ok, testCase.name)
            assertTrue(model.add(symbol) is Ok, testCase.name)
            assertTrue(model.minimize(symbol) is Ok, testCase.name)

            val result = runBlocking { solveConditionalMetaModel(solverCase.create(), model) }
            val report = result.value
                ?: error("${solverCase.name}: solver should return a feasible report for ${testCase.name}")
            assertEquals(ProblemStatus.Feasible, report.problemStatus, "${solverCase.name}: ${testCase.name}")
            return model.tokens.find(function.resultVar)?.result
        } finally {
            model.close()
        }
    }

    private fun linear(symbol: Symbol): LinearPolynomial<Flt64> {
        return LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, symbol)),
            constant = Flt64.zero
        )
    }

    private fun constant(value: Flt64): LinearPolynomial<Flt64> {
        return LinearPolynomial(emptyList(), value)
    }

    private fun assertNumericallyEqual(expected: Flt64, actual: Flt64?, message: String) {
        val actualValue = actual ?: error(message)
        assertTrue(
            abs(actualValue.toDouble() - expected.toDouble()) <= 1e-6,
            "$message: expected=${expected.toDouble()}, actual=${actualValue.toDouble()}"
        )
    }

    private data class FixedRelationCase(
        val name: String,
        val relation: Comparison,
        val value: Flt64,
        val expected: Flt64
    )

    private data class IntegerThresholdCase(
        val name: String,
        val value: Int,
        val conditionValue: Flt64
    )

    private data class IntegerRelationCase(
        val name: String,
        val relation: Comparison,
        val expected: Flt64
    )
}
