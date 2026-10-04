package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.ReificationDirection
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweringPolicy
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.model.mechanism.LinearInequalityConstraint
import fuookami.ospf.kotlin.core.symbol.function.evaluateWith
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

class CumulativeLoweringPolicyTest {
    @Test
    fun cumulativeWithOnlyZeroDemandOrFixedZeroDurationSkipsTimeHorizonBudget() {
        val model = ConstraintProgrammingModel("cumulative-zero-work")
        try {
            val zeroDurationStart = IntVar("zero_duration_start")
            val zeroDurationEnd = IntVar("zero_duration_end")
            val zeroDuration = IntervalVariable.fixed(
                id = IntervalId("zero-duration"),
                start = register(
                    model = model,
                    variable = zeroDurationStart,
                    domain = IntegerDomain.interval(0, 100_000).value!!
                ),
                size = Int64.zero,
                end = register(
                    model = model,
                    variable = zeroDurationEnd,
                    domain = IntegerDomain.interval(0, 100_000).value!!
                )
            ).value!!
            model.registerInterval(zeroDuration)

            val zeroDemandStart = IntVar("zero_demand_start")
            val zeroDemandEnd = IntVar("zero_demand_end")
            val zeroDemand = IntervalVariable.fixed(
                id = IntervalId("zero-demand"),
                start = register(
                    model = model,
                    variable = zeroDemandStart,
                    domain = IntegerDomain.interval(0, 100_000).value!!
                ),
                size = Int64.one,
                end = register(
                    model = model,
                    variable = zeroDemandEnd,
                    domain = IntegerDomain.interval(0, 100_000).value!!
                )
            ).value!!
            model.registerInterval(zeroDemand)

            model.addConstraint(
                Cumulative(
                    intervals = listOf(zeroDuration, zeroDemand),
                    demands = listOf(
                        ConstraintProgrammingExpression.Constant(Int64.one),
                        ConstraintProgrammingExpression.Constant(Int64.zero)
                    ),
                    capacity = ConstraintProgrammingExpression.Constant(Int64.zero)
                ).value!!
            )

            val lowerer = ConstraintProgrammingToLinearModelLowerer(
                ConstraintProgrammingLoweringPolicy(
                    allowCumulative = true,
                    maxCumulativeTimeSlots = 1,
                    maxCumulativeWork = 1
                )
            )
            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(lowerer.lower(model)).value
            try {
                assertTrue(cumulativeSlotRows(lowered).isEmpty())
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun cumulativeSlotRowsRejectOverlappingFixedIntervals() {
        val model = ConstraintProgrammingModel("cumulative-overload")
        try {
            val first = fixedSingletonInterval(
                model = model,
                id = "first",
                startValue = 0,
                endValue = 1
            )
            val second = fixedSingletonInterval(
                model = model,
                id = "second",
                startValue = 0,
                endValue = 1
            )
            model.addConstraint(
                Cumulative(
                    intervals = listOf(first, second),
                    demands = listOf(
                        ConstraintProgrammingExpression.Constant(Int64.one),
                        ConstraintProgrammingExpression.Constant(Int64.one)
                    ),
                    capacity = ConstraintProgrammingExpression.Constant(Int64.one)
                ).value!!
            )

            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer(
                    ConstraintProgrammingLoweringPolicy(allowCumulative = true)
                ).lower(model)
            ).value
            try {
                val slotRows = cumulativeSlotRows(lowered)
                assertEquals(1, slotRows.size)
                assertFalse(slotRows.all { isSatisfied(it, emptyMap()) })
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun cumulativeSlotRowsTreatAdjacentIntervalsAsHalfOpen() {
        val model = ConstraintProgrammingModel("cumulative-half-open")
        try {
            val first = fixedSingletonInterval(
                model = model,
                id = "first",
                startValue = 0,
                endValue = 1
            )
            val second = fixedSingletonInterval(
                model = model,
                id = "second",
                startValue = 1,
                endValue = 2
            )
            model.addConstraint(
                Cumulative(
                    intervals = listOf(first, second),
                    demands = listOf(
                        ConstraintProgrammingExpression.Constant(Int64.one),
                        ConstraintProgrammingExpression.Constant(Int64.one)
                    ),
                    capacity = ConstraintProgrammingExpression.Constant(Int64.one)
                ).value!!
            )

            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer(
                    ConstraintProgrammingLoweringPolicy(allowCumulative = true)
                ).lower(model)
            ).value
            try {
                val slotRows = cumulativeSlotRows(lowered)
                assertEquals(2, slotRows.size)
                assertTrue(slotRows.all { isSatisfied(it, emptyMap()) })
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun cumulativeLoweringMatchesOptionalIntervalSemanticsAcrossSmallDomains() {
        val model = ConstraintProgrammingModel("cumulative-small-domain")
        try {
            val blocker = fixedSingletonInterval(
                model = model,
                id = "blocker",
                startValue = 1,
                endValue = 2
            )
            val start = IntVar("optional_start")
            val end = IntVar("optional_end")
            val present = BinVar("optional_present")
            val startExpression = register(
                model = model,
                variable = start,
                domain = IntegerDomain.interval(0, 2).value!!
            )
            val endExpression = register(
                model = model,
                variable = end,
                domain = IntegerDomain.interval(1, 3).value!!
            )
            register(
                model = model,
                variable = present,
                domain = IntegerDomain.boolean
            )
            val optional = IntervalVariable.fixed(
                id = IntervalId("optional"),
                start = startExpression,
                size = Int64.one,
                end = endExpression,
                presence = BooleanLiteral(present)
            ).value!!
            model.registerInterval(optional)
            model.addConstraint(
                Cumulative(
                    intervals = listOf(blocker, optional),
                    demands = listOf(
                        ConstraintProgrammingExpression.Constant(Int64.one),
                        ConstraintProgrammingExpression.Constant(Int64.one)
                    ),
                    capacity = ConstraintProgrammingExpression.Constant(Int64.one)
                ).value!!
            )

            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer(
                    ConstraintProgrammingLoweringPolicy(
                        allowCumulative = true,
                        maxCumulativeTimeSlots = 3,
                        maxCumulativeWork = 6
                    )
                ).lower(model)
            ).value
            try {
                val values = linkedMapOf<Symbol, Flt64>(
                    loweredVariable(lowered, start) to Flt64.zero,
                    loweredVariable(lowered, end) to Flt64.zero,
                    loweredVariable(lowered, present) to Flt64.zero,
                    loweredVariable(lowered, blocker.start.let { (it as ConstraintProgrammingExpression.Variable).variable }) to Flt64.one,
                    loweredVariable(lowered, blocker.end.let { (it as ConstraintProgrammingExpression.Variable).variable }) to Flt64(2.0)
                )

                for (startValue in 0..2) {
                    for (endValue in 1..3) {
                        for (presentValue in 0..1) {
                            values[loweredVariable(lowered, start)] = Flt64(startValue.toDouble())
                            values[loweredVariable(lowered, end)] = Flt64(endValue.toDouble())
                            values[loweredVariable(lowered, present)] = Flt64(presentValue.toDouble())

                            val expectedFeasible = presentValue == 0 ||
                                (endValue == startValue + 1 && (endValue <= 1 || startValue >= 2))
                            assertEquals(
                                expectedFeasible,
                                hasFeasibleAuxiliaryAssignment(lowered, values),
                                "start=$startValue end=$endValue present=$presentValue"
                            )
                        }
                    }
                }
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun cumulativeRequiresExplicitOptInAndRespectsTimeAndWorkBudgets() {
        val strictModel = singleCumulativeModel("cumulative-strict-policy")
        try {
            assertIs<Failed<*, *, *>>(ConstraintProgrammingToLinearModelLowerer().lower(strictModel))
        } finally {
            strictModel.close()
        }

        val timeBudgetModel = singleCumulativeModel("cumulative-time-budget")
        try {
            val lowered = ConstraintProgrammingToLinearModelLowerer(
                ConstraintProgrammingLoweringPolicy(
                    allowCumulative = true,
                    maxCumulativeTimeSlots = 1,
                    maxCumulativeWork = 10
                )
            ).lower(timeBudgetModel)
            assertIs<Failed<*, *, *>>(lowered)
        } finally {
            timeBudgetModel.close()
        }

        val workBudgetModel = singleCumulativeModel("cumulative-work-budget")
        try {
            val lowered = ConstraintProgrammingToLinearModelLowerer(
                ConstraintProgrammingLoweringPolicy(
                    allowCumulative = true,
                    maxCumulativeTimeSlots = 2,
                    maxCumulativeWork = 1
                )
            ).lower(workBudgetModel)
            assertIs<Failed<*, *, *>>(lowered)
        } finally {
            workBudgetModel.close()
        }
    }

    @Test
    fun implicationAndReificationRejectWrappedCumulative() {
        assertWrappedCumulativeRejected(reified = false)
        assertWrappedCumulativeRejected(reified = true)
    }

    private fun assertWrappedCumulativeRejected(reified: Boolean) {
        val model = ConstraintProgrammingModel(if (reified) "reified-cumulative" else "implied-cumulative")
        try {
            val start = IntVar("start")
            val end = IntVar("end")
            val interval = IntervalVariable.fixed(
                id = IntervalId("wrapped"),
                start = register(
                    model = model,
                    variable = start,
                    domain = IntegerDomain.interval(0, 2).value!!
                ),
                size = Int64.one,
                end = register(
                    model = model,
                    variable = end,
                    domain = IntegerDomain.interval(0, 3).value!!
                )
            ).value!!
            model.registerInterval(interval)
            val cumulative = Cumulative(
                intervals = listOf(interval),
                demands = listOf(ConstraintProgrammingExpression.Constant(Int64.one)),
                capacity = ConstraintProgrammingExpression.Constant(Int64.one)
            ).value!!
            val gate = BinVar("gate")
            register(
                model = model,
                variable = gate,
                domain = IntegerDomain.boolean
            )
            val literal = BooleanLiteral(gate)
            val wrapped: ConstraintProgrammingConstraint = if (reified) {
                ConstraintProgrammingConstraint.reified(
                    literal = literal,
                    constraint = cumulative,
                    direction = ReificationDirection.Equivalent
                ).value!!
            } else {
                ConstraintProgrammingConstraint.implies(literal, cumulative).value!!
            }
            model.addConstraint(wrapped)

            val lowerer = ConstraintProgrammingToLinearModelLowerer(
                ConstraintProgrammingLoweringPolicy(allowCumulative = true)
            )
            assertIs<Failed<*, *, *>>(lowerer.lower(model))
        } finally {
            model.close()
        }
    }

    private fun register(
        model: ConstraintProgrammingModel,
        variable: AbstractVariableItem<*, *>,
        domain: IntegerDomain
    ): ConstraintProgrammingExpression.Variable {
        model.registerVariable(variable, domain)
        return ConstraintProgrammingExpression.variable(variable, domain).value!!
    }

    private fun fixedSingletonInterval(
        model: ConstraintProgrammingModel,
        id: String,
        startValue: Int,
        endValue: Int
    ): IntervalVariable {
        val start = IntVar("${id}_start")
        val end = IntVar("${id}_end")
        val interval = IntervalVariable.fixed(
            id = IntervalId(id),
            start = register(
                model = model,
                variable = start,
                domain = IntegerDomain.interval(startValue, startValue).value!!
            ),
            size = Int64((endValue - startValue).toLong()),
            end = register(
                model = model,
                variable = end,
                domain = IntegerDomain.interval(endValue, endValue).value!!
            )
        ).value!!
        model.registerInterval(interval)
        return interval
    }

    private fun singleCumulativeModel(name: String): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel(name)
        val interval = fixedSingletonInterval(
            model = model,
            id = "interval",
            startValue = 0,
            endValue = 2
        )
        model.addConstraint(
            Cumulative(
                intervals = listOf(interval),
                demands = listOf(ConstraintProgrammingExpression.Constant(Int64.one)),
                capacity = ConstraintProgrammingExpression.Constant(Int64.one)
            ).value!!
        )
        return model
    }

    private fun loweredVariable(
        lowered: ConstraintProgrammingLoweredLinearModel,
        variable: AbstractVariableItem<*, *>
    ): Symbol = lowered.variables.getValue(VariableId("${variable.identifier}:${variable.index}"))

    private fun cumulativeSlotRows(
        lowered: ConstraintProgrammingLoweredLinearModel
    ): List<LinearInequalityConstraint<Flt64>> =
        lowered.model.relationConstraints.filter { "-slot-" in it.name }

    private fun isSatisfied(
        constraint: LinearInequalityConstraint<Flt64>,
        values: Map<Symbol, Flt64>
    ): Boolean {
        val lhs = constraint.inequality.lhs.evaluateWith(values) ?: return false
        val rhs = constraint.inequality.rhs.evaluateWith(values) ?: return false
        return when (constraint.inequality.comparison) {
            Comparison.LT -> lhs.compareTo(rhs) < 0
            Comparison.LE -> lhs.compareTo(rhs) <= 0
            Comparison.EQ -> lhs.compareTo(rhs) == 0
            Comparison.NE -> lhs.compareTo(rhs) != 0
            Comparison.GE -> lhs.compareTo(rhs) >= 0
            Comparison.GT -> lhs.compareTo(rhs) > 0
        }
    }

    private fun hasFeasibleAuxiliaryAssignment(
        lowered: ConstraintProgrammingLoweredLinearModel,
        fixedValues: Map<Symbol, Flt64>
    ): Boolean {
        val rows = lowered.model.relationConstraints
        val symbols = rows.flatMap { row ->
            row.inequality.lhs.monomials.map { it.symbol } + row.inequality.rhs.monomials.map { it.symbol }
        }.distinct()
        val auxiliaries = symbols.filterNot(fixedValues::containsKey)
        assertTrue(auxiliaries.all { it is BinVar }, "expected only binary lowering auxiliaries: $auxiliaries")
        for (mask in 0 until (1 shl auxiliaries.size)) {
            val values = fixedValues.toMutableMap()
            for ((index, auxiliary) in auxiliaries.withIndex()) {
                values[auxiliary] = if (mask and (1 shl index) == 0) Flt64.zero else Flt64.one
            }
            if (rows.all { isSatisfied(it, values) }) return true
        }
        return false
    }
}
