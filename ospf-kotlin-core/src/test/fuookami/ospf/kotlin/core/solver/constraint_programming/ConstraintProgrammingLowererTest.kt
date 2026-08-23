package fuookami.ospf.kotlin.core.solver.constraint_programming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.model.constraint_programming.ReificationDirection
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweredLinearModel
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingLoweringPolicy
import fuookami.ospf.kotlin.core.solver.constraint_programming.lowering.ConstraintProgrammingToLinearModelLowerer
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

class ConstraintProgrammingLowererTest {
    @Test
    fun shouldLowerEmptyBooleanOrAndXorAsFalseConstraints() {
        val model = ConstraintProgrammingModel("empty-boolean-constraints")
        try {
            val emptyOr = ConstraintProgrammingConstraint.boolOr(emptyList()).value!!
            val emptyXor = ConstraintProgrammingConstraint.boolXor(emptyList()).value!!
            assertEquals(false, emptyOr.isSatisfied(emptyMap()).value)
            assertEquals(false, emptyXor.isSatisfied(emptyMap()).value)
            model.addConstraint(emptyOr)
            model.addConstraint(emptyXor)

            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(model)
            ).value
            try {
                assertTrue(lowered.model.relationConstraints.size >= 2)
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldLowerIntegerBooleanAndReifiedConstraints() {
        val model = ConstraintProgrammingModel("lower-logic", ObjectCategory.Minimum)
        try {
            val x = IntVar("x")
            val a = BinVar("a")
            val b = BinVar("b")
            val xExpression = register(model, x, IntegerDomain.interval(0, 10).value!!)
            register(model, a, IntegerDomain.boolean)
            register(model, b, IntegerDomain.boolean)
            model.addConstraint(
                ConstraintProgrammingConstraint.greaterOrEqual(xExpression, Int64(3)).value!!
            )
            model.addConstraint(ConstraintProgrammingConstraint.boolAnd(listOf(BooleanLiteral(a), BooleanLiteral(b, true))).value!!)
            model.addConstraint(ConstraintProgrammingConstraint.boolOr(listOf(BooleanLiteral(a), BooleanLiteral(b))).value!!)
            model.addConstraint(ConstraintProgrammingConstraint.boolXor(listOf(BooleanLiteral(a), BooleanLiteral(b))).value!!)
            model.addConstraint(
                ConstraintProgrammingConstraint.implies(
                    BooleanLiteral(a),
                    ConstraintProgrammingConstraint.lessOrEqual(xExpression, Int64(7)).value!!
                ).value!!
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.reified(
                    BooleanLiteral(b),
                    ConstraintProgrammingConstraint.equal(xExpression, Int64(5)).value!!,
                    ReificationDirection.Equivalent
                ).value!!
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.reified(
                    BooleanLiteral(a),
                    ConstraintProgrammingConstraint.greaterOrEqual(xExpression, Int64(4)).value!!,
                    ReificationDirection.ImpliedBy
                ).value!!
            )
            model.minimize(xExpression)

            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(model)
            ).value
            try {
                assertEquals(3, lowered.variables.size)
                assertTrue(lowered.model.relationConstraints.size >= 8)
                assertEquals(1, lowered.model.subObjects.size)
                val sourceVariableId = "${x.identifier}:${x.index}"
                assertTrue(
                    lowered.artifacts.values.any {
                        it.role == "source-variable" && it.originId == sourceVariableId
                    }
                )
                assertTrue(lowered.artifacts.values.any { it.role == "auxiliary-variable" })
                assertTrue(lowered.artifacts.values.any { it.role == "compiled-constraint" })
                assertEquals(lowered.artifacts.size, lowered.artifacts.keys.toSet().size)
                assertTrue(
                    lowered.artifacts.values.none {
                        it.role == "auxiliary-variable" && it.artifactId == "variable:$sourceVariableId"
                    }
                )
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldLowerSparseAllDifferentElementTableAndNoOverlap() {
        val model = ConstraintProgrammingModel("lower-global", ObjectCategory.Minimum)
        try {
            val x = IntVar("x")
            val y = IntVar("y")
            val index = IntVar("index")
            val target = IntVar("target")
            val firstStart = IntVar("first-start")
            val firstEnd = IntVar("first-end")
            val secondStart = IntVar("second-start")
            val secondEnd = IntVar("second-end")
            val xExpression = register(model, x, IntegerDomain.values(listOf(1, 3, 5)).value!!)
            val yExpression = register(model, y, IntegerDomain.interval(0, 10).value!!)
            val indexExpression = register(model, index, IntegerDomain.interval(0, 2).value!!)
            val targetExpression = register(model, target, IntegerDomain.interval(0, 30).value!!)
            val firstStartExpression = register(model, firstStart, IntegerDomain.interval(0, 10).value!!)
            val firstEndExpression = register(model, firstEnd, IntegerDomain.interval(0, 12).value!!)
            val secondStartExpression = register(model, secondStart, IntegerDomain.interval(0, 10).value!!)
            val secondEndExpression = register(model, secondEnd, IntegerDomain.interval(0, 12).value!!)
            model.addConstraint(ConstraintProgrammingConstraint.allDifferent(listOf(xExpression, yExpression)).value!!)
            model.addConstraint(
                ConstraintProgrammingConstraint.element(
                    indexExpression,
                    listOf(Int64(10), Int64(20), Int64(30)),
                    targetExpression
                ).value!!
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.allowedAssignments(
                    listOf(xExpression, yExpression),
                    listOf(listOf(1, 2), listOf(3, 4), listOf(5, 6))
                ).value!!
            )
            val first = IntervalVariable.fixed(IntervalId("first"), firstStartExpression, Int64(2), firstEndExpression).value!!
            val second = IntervalVariable.fixed(IntervalId("second"), secondStartExpression, Int64(2), secondEndExpression).value!!
            model.registerInterval(first)
            model.registerInterval(second)
            model.addConstraint(NoOverlap.create(listOf(first, second)).value!!)

            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(model)
            ).value
            try {
                assertEquals(8, lowered.variables.size)
                assertTrue(lowered.model.relationConstraints.size >= 12)
            } finally {
                lowered.close()
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun shouldSupportOptionalAndVariableDurationIntervals() {
        val optionalModel = ConstraintProgrammingModel("optional")
        try {
            val start = IntVar("start")
            val end = IntVar("end")
            val present = BinVar("present")
            val startExpression = register(optionalModel, start, IntegerDomain.interval(0, 10).value!!)
            val endExpression = register(optionalModel, end, IntegerDomain.interval(0, 12).value!!)
            register(optionalModel, present, IntegerDomain.boolean)
            val interval = IntervalVariable.fixed(
                IntervalId("optional"),
                startExpression,
                Int64(2),
                endExpression,
                BooleanLiteral(present)
            ).value!!
            optionalModel.registerInterval(interval)
            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(optionalModel)
            ).value
            try {
                assertTrue(
                    lowered.model.relationConstraints.none { it.name == "cp-interval-optional-link" },
                    "optional interval must not impose an unconditional end = start + size link"
                )
                assertTrue(
                    lowered.model.relationConstraints.any { it.name == "cp-interval-optional-link-upper" }
                )
            } finally {
                lowered.close()
            }
        } finally {
            optionalModel.close()
        }

        val variableDurationModel = ConstraintProgrammingModel("variable-duration")
        try {
            val start = IntVar("start")
            val size = IntVar("size")
            val end = IntVar("end")
            val startExpression = register(variableDurationModel, start, IntegerDomain.interval(0, 10).value!!)
            val sizeExpression = register(variableDurationModel, size, IntegerDomain.interval(1, 4).value!!)
            val endExpression = register(variableDurationModel, end, IntegerDomain.interval(0, 14).value!!)
            val interval = IntervalVariable.create(
                IntervalId("variable-duration"),
                startExpression,
                sizeExpression,
                endExpression
            ).value!!
            variableDurationModel.registerInterval(interval)
            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(variableDurationModel)
            ).value
            lowered.close()
        } finally {
            variableDurationModel.close()
        }

        val cumulativeModel = ConstraintProgrammingModel("cumulative")
        try {
            val start = IntVar("start")
            val end = IntVar("end")
            val startExpression = register(cumulativeModel, start, IntegerDomain.interval(0, 10).value!!)
            val endExpression = register(cumulativeModel, end, IntegerDomain.interval(0, 12).value!!)
            val interval = IntervalVariable.fixed(
                IntervalId("cumulative"),
                startExpression,
                Int64(2),
                endExpression
            ).value!!
            cumulativeModel.registerInterval(interval)
            cumulativeModel.addConstraint(
                Cumulative.create(
                    listOf(interval),
                    listOf(ConstraintProgrammingExpression.Constant(Int64.one)),
                    ConstraintProgrammingExpression.Constant(Int64.one)
                ).value!!
            )
            assertTrue(ConstraintProgrammingToLinearModelLowerer().lower(cumulativeModel).failed)
        } finally {
            cumulativeModel.close()
        }

        val largeModel = ConstraintProgrammingModel("large-bound")
        try {
            val value = IntVar("large")
            largeModel.registerVariable(
                value,
                IntegerDomain.interval(Int64.zero, Int64(Long.MAX_VALUE)).value!!
            )
            val expression = ConstraintProgrammingExpression.Variable(value)
            largeModel.minimize(expression)
            val result = ConstraintProgrammingToLinearModelLowerer().lower(largeModel)
            assertIs<Failed<*, *, *>>(result)
        } finally {
            largeModel.close()
        }
    }

    @Test
    fun sourceArtifactProjectionIsStableAcrossRegistrationOrderAndDuplicateNames() {
        val first = stableProjectionModel(reverseRegistration = false)
        val rebuilt = stableProjectionModel(reverseRegistration = true)
        try {
            val firstLowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(first)
            ).value
            val rebuiltLowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(rebuilt)
            ).value
            try {
                val firstProjection = firstLowered.artifacts.values
                    .filter { it.originId != null }
                    .map { "${it.role}|${it.originId}" }
                    .sorted()
                val rebuiltProjection = rebuiltLowered.artifacts.values
                    .filter { it.originId != null }
                    .map { "${it.role}|${it.originId}" }
                    .sorted()
                assertEquals(firstProjection, rebuiltProjection)
                assertTrue(firstProjection.any { it == "source-variable|stable:x" })
                assertTrue(firstProjection.any { it == "source-variable|stable:y" })
                assertTrue(firstProjection.any { it == "compiled-constraint|stable:x-lower" })
                assertTrue(firstProjection.any { it == "compiled-constraint|stable:y-lower" })
            } finally {
                firstLowered.close()
                rebuiltLowered.close()
            }
        } finally {
            first.close()
            rebuilt.close()
        }
    }

    @Test
    fun policyShouldGateSparseDomainSize() {
        val model = ConstraintProgrammingModel("sparse-limit")
        try {
            val value = IntVar("value")
            val domain = IntegerDomain.values((0..3).toList()).value!!
            val expression = register(model, value, domain)
            model.addConstraint(ConstraintProgrammingConstraint.equal(expression, Int64.zero).value!!)
            val policy = ConstraintProgrammingLoweringPolicy(sparseDomainLimit = 2)
            assertTrue(ConstraintProgrammingToLinearModelLowerer(policy).lower(model).failed)
        } finally {
            model.close()
        }
    }

    @Test
    fun fixedValuesLowerToExplicitEqualityConstraints() {
        val model = ConstraintProgrammingModel("fixed-value-lowering")
        try {
            val variable = IntVar("fixed")
            model.registerVariable(variable, IntegerDomain.interval(0, 3).value!!)
            val id = VariableId("${variable.identifier}:${variable.index}")
            val lowered = assertIs<Ok<ConstraintProgrammingLoweredLinearModel, *, *>>(
                ConstraintProgrammingToLinearModelLowerer().lower(
                    model,
                    fixedValues = mapOf(id to Int64(2))
                )
            ).value
            try {
                assertTrue(lowered.model.relationConstraints.any { it.name == "cp-fixed-${id.value.replace(Regex("[^A-Za-z0-9_]+"), "_")}" })
            } finally {
                lowered.close()
            }
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

    private fun stableProjectionModel(reverseRegistration: Boolean): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("stable-projection", ObjectCategory.Minimum)
        val x = IntVar("duplicate-display")
        val y = IntVar("duplicate-display")
        val variables = listOf(
            VariableId("stable:x") to x,
            VariableId("stable:y") to y
        )
        (if (reverseRegistration) variables.asReversed() else variables).forEach { (id, variable) ->
            model.registerVariable(
                id = id,
                variable = variable,
                domain = IntegerDomain.interval(0, 1).value!!,
                scope = "stable",
                origin = "fixture/${id.value}"
            )
        }
        val constraints = listOf(
            ConstraintId("stable:x-lower") to ConstraintProgrammingConstraint.greaterOrEqual(
                ConstraintProgrammingExpression.Variable(x),
                Int64.zero
            ).value!!,
            ConstraintId("stable:y-lower") to ConstraintProgrammingConstraint.greaterOrEqual(
                ConstraintProgrammingExpression.Variable(y),
                Int64.zero
            ).value!!
        )
        (if (reverseRegistration) constraints.asReversed() else constraints).forEach { (id, constraint) ->
            model.addConstraint(
                constraint = constraint,
                id = id,
                name = "duplicate-display",
                scope = "stable",
                origin = "fixture/${id.value}"
            )
        }
        model.minimize(
            expression = ConstraintProgrammingExpression.Variable(x),
            id = ObjectiveId("stable:objective"),
            name = "duplicate-display",
            scope = "stable",
            origin = "fixture/stable:objective"
        )
        return model
    }
}
