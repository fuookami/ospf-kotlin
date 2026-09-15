package fuookami.ospf.kotlin.core.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.mechanism.MetaConstraintGroup
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Ok

class ConstraintActivityAnalyzerTest {
    @Test
    fun comparisonsBoundsAndGroupsProduceStableActivityReport() {
        val model = ConstraintProgrammingModel("activity", ObjectCategory.Maximum)
        val x = IntVar("x")
        val y = IntVar("y")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 100).value!!)
            model.registerVariable(y, IntegerDomain.interval(0, 100).value!!)
            val xExpression = ConstraintProgrammingExpression.Variable(x)
            val yExpression = ConstraintProgrammingExpression.Variable(y)
            val sum = ConstraintProgrammingExpression.sum(listOf(xExpression, yExpression)).value!!
            model.registerConstraintGroup(TestConstraintGroup("capacity"))
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(sum, Int64(10)).value!!,
                id = ConstraintId("capacity-bound")
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(xExpression, Int64(7)).value!!,
                id = ConstraintId("near-bound")
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(xExpression, Int64(100)).value!!,
                id = ConstraintId("loose-bound")
            )
            val snapshot = model.snapshot().value!!
            val solution = ConstraintProgrammingSolution(
                values = mapOf(
                    variableId(x) to Int64(5),
                    variableId(y) to Int64(5)
                )
            )

            val report = assertIs<Ok<ConstraintActivityReport, *, *>>(
                ConstraintActivityAnalyzer(
                    tolerance = 0.1,
                    nearlyActiveTolerance = 2.0
                ).analyze(snapshot, solution)
            ).value!!

            assertEquals(ActivityStatus.Active, report.constraint(ConstraintId("capacity-bound"))!!.status)
            assertEquals(0.0, report.constraint(ConstraintId("capacity-bound"))!!.slack)
            assertEquals(ActivityStatus.NearlyActive, report.constraint(ConstraintId("near-bound"))!!.status)
            assertEquals(ActivityStatus.Inactive, report.constraint(ConstraintId("loose-bound"))!!.status)
            assertEquals(7, report.activities.size)
            assertEquals(3, report.totalConstraints)
            assertEquals(4, report.variableActivities.size)
            val summary = report.groupSummaries.single()
            assertEquals("capacity", summary.group)
            assertEquals(3, summary.total)
            assertEquals(1, summary.active)
            assertEquals(1, summary.nearlyActive)
            assertEquals(1, summary.inactive)
            assertEquals("constraint:capacity-bound", report.constraint(ConstraintId("capacity-bound"))!!.source.stableId)
        } finally {
            model.close()
        }
    }

    @Test
    fun equalityActivityUsesExactSatisfiedOrViolatedStatus() {
        val model = ConstraintProgrammingModel("equality-activity")
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.equal(expression, Int64(3)).value!!,
                id = ConstraintId("equal-active")
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.equal(expression, Int64(2)).value!!,
                id = ConstraintId("equal-violated")
            )
            val report = ConstraintActivityAnalyzer().analyze(
                model.snapshot().value!!,
                ConstraintProgrammingSolution(values = mapOf(variableId(x) to Int64(3)))
            ).value!!

            assertEquals(ActivityStatus.Active, report.constraint(ConstraintId("equal-active"))!!.status)
            assertEquals(ActivityStatus.Violated, report.constraint(ConstraintId("equal-violated"))!!.status)
        } finally {
            model.close()
        }
    }

    @Test
    fun allDifferentReportsNaturalMargin() {
        val model = ConstraintProgrammingModel("semantic-activity")
        val first = IntVar("first")
        val second = IntVar("second")
        try {
            model.registerVariable(first, IntegerDomain.interval(0, 5).value!!)
            model.registerVariable(second, IntegerDomain.interval(0, 5).value!!)
            val different = ConstraintProgrammingConstraint.allDifferent(
                listOf(
                    ConstraintProgrammingExpression.Variable(first),
                    ConstraintProgrammingExpression.Variable(second)
                )
            ).value!!
            model.addConstraint(different, id = ConstraintId("different"))
            val snapshot = model.snapshot().value!!
            val analyzer = ConstraintActivityAnalyzer()

            // 相邻取值：间距 1 → margin 0 → 已贴边。 / Adjacent values: gap 1 gives margin 0, i.e.
            // exactly at the boundary.
            val adjacent = analyzer.analyze(
                snapshot,
                ConstraintProgrammingSolution(
                    values = mapOf(variableId(first) to Int64(1), variableId(second) to Int64(2))
                )
            ).value!!
            val adjacentActivity = adjacent.constraint(ConstraintId("different"))!!
            assertEquals(ActivityStatus.Active, adjacentActivity.status)
            assertEquals(0.0, adjacentActivity.slack)
            assertEquals(ActivityEvidenceKind.Semantic, adjacentActivity.evidence.kind)
            assertEquals(true, adjacentActivity.evidence.satisfied)

            // 间距充足：margin 为正 → 不贴边。 / A wide gap gives a positive margin.
            val wide = analyzer.analyze(
                snapshot,
                ConstraintProgrammingSolution(
                    values = mapOf(variableId(first) to Int64(0), variableId(second) to Int64(5))
                )
            ).value!!
            val wideActivity = wide.constraint(ConstraintId("different"))!!
            assertEquals(ActivityStatus.Inactive, wideActivity.status)
            assertEquals(4.0, wideActivity.slack)

            // 取值重复：语义违反且 margin 为负，二者必须一致。
            // A duplicate violates the semantics and gives a negative margin; the two must agree.
            val violated = analyzer.analyze(
                snapshot,
                ConstraintProgrammingSolution(
                    values = mapOf(variableId(first) to Int64(1), variableId(second) to Int64(1))
                )
            ).value!!
            val violatedActivity = violated.constraint(ConstraintId("different"))!!
            assertEquals(ActivityStatus.Violated, violatedActivity.status)
            assertEquals(-1.0, violatedActivity.slack)
            assertEquals(false, violatedActivity.evidence.satisfied)
        } finally {
            model.close()
        }
    }

    @Test
    fun globalConstraintsWithoutNaturalMarginKeepNoFabricatedSlack() {
        // 表约束没有无歧义的标量"边界距离"：满足时无法说"离违反还有多远"。
        // 因此它必须保持 `SatisfiedWithoutSlackMetric` 且不带任何 slack——计划 2.2 的红线。
        //
        // A table constraint has no unambiguous scalar boundary distance, so it must keep
        // `SatisfiedWithoutSlackMetric` with no slack — plan 2.2's rule.
        val model = ConstraintProgrammingModel("semantic-no-margin")
        val first = IntVar("first")
        val second = IntVar("second")
        try {
            model.registerVariable(first, IntegerDomain.interval(0, 3).value!!)
            model.registerVariable(second, IntegerDomain.interval(0, 3).value!!)
            val forbidden = ConstraintProgrammingConstraint.forbiddenAssignments(
                listOf(
                    ConstraintProgrammingExpression.Variable(first),
                    ConstraintProgrammingExpression.Variable(second)
                ),
                listOf(listOf(Int64(0), Int64(0)))
            ).value!!
            model.addConstraint(forbidden, id = ConstraintId("forbidden"))
            val snapshot = model.snapshot().value!!
            val activity = ConstraintActivityAnalyzer().analyze(
                snapshot,
                ConstraintProgrammingSolution(
                    values = mapOf(variableId(first) to Int64(1), variableId(second) to Int64(1))
                )
            ).value!!.constraint(ConstraintId("forbidden"))!!
            assertEquals(ActivityStatus.SatisfiedWithoutSlackMetric, activity.status)
            assertEquals(null, activity.slack)
            assertEquals(null, activity.normalizedSlack)
            assertEquals(ActivityEvidenceKind.Semantic, activity.evidence.kind)
            assertEquals(true, activity.evidence.satisfied)
        } finally {
            model.close()
        }
    }

    @Test
    fun allDifferentMarginDoesNotOverflowAcrossInt64Extremes() {
        val model = ConstraintProgrammingModel("extreme-activity")
        val first = IntVar("first")
        val second = IntVar("second")
        try {
            model.registerVariable(first, IntegerDomain.singleton(Int64.minimum))
            model.registerVariable(second, IntegerDomain.singleton(Int64.maximum))
            val firstExpression = ConstraintProgrammingExpression.Variable(first)
            val secondExpression = ConstraintProgrammingExpression.Variable(second)
            model.addConstraint(
                ConstraintProgrammingConstraint.allDifferent(
                    listOf(firstExpression, secondExpression)
                ).value!!,
                id = ConstraintId("extreme-different")
            )

            val report = ConstraintActivityAnalyzer().analyze(
                model.snapshot().value!!,
                ConstraintProgrammingSolution(
                    values = mapOf(
                        variableId(first) to Int64.minimum,
                        variableId(second) to Int64.maximum
                    )
                )
            ).value!!
            val activity = report.constraint(ConstraintId("extreme-different"))!!

            assertEquals(ActivityStatus.Inactive, activity.status)
            assertTrue(activity.slack!!.isFinite())
            assertTrue(activity.slack > 0.0)
        } finally {
            model.close()
        }
    }

    @Test
    fun sparseDomainsAndMissingValuesRemainSemanticOrUnknown() {
        val model = ConstraintProgrammingModel("domain-activity")
        val value = IntVar("value")
        try {
            model.registerVariable(value, IntegerDomain.values(listOf(Int64(0), Int64(2), Int64(4))).value!!)
            val snapshot = model.snapshot().value!!
            val analyzer = ConstraintActivityAnalyzer()
            val complete = analyzer.analyze(
                snapshot,
                ConstraintProgrammingSolution(values = mapOf(variableId(value) to Int64(2)))
            ).value!!
            val domainActivity = complete.variableActivities.first {
                it.source.kind == "sparse-domain"
            }
            assertEquals(ActivityStatus.SatisfiedWithoutSlackMetric, domainActivity.status)
            assertEquals(null, domainActivity.slack)

            val unknown = analyzer.analyze(snapshot).value!!
            assertTrue(unknown.activities.all { it.status == ActivityStatus.Unknown })
            assertNotNull(unknown.variableActivities.firstOrNull {
                it.source.kind == "variable-lower-bound"
            })
        } finally {
            model.close()
        }
    }

    @Test
    fun analysisDoesNotChangeTheSnapshotAndSessionCachesTheReport() {
        val model = ConstraintProgrammingModel("cached-activity")
        val value = IntVar("value")
        try {
            model.registerVariable(value, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(value)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(10)).value!!,
                id = ConstraintId("bound")
            )
            val snapshot = model.snapshot().value!!
            val session = CriticalConstraintAnalysisSession.fromSnapshot(
                snapshot,
                baselineSolution = ConstraintProgrammingSolution(values = mapOf(variableId(value) to Int64(10)))
            )
            val before = session.snapshot
            val analyzer = ConstraintActivityAnalyzer()
            val first = analyzer.analyze(session).value!!
            val second = analyzer.analyze(session).value!!
            assertEquals(first, second)
            assertEquals(before, session.snapshot)
            assertEquals(1, session.cacheSizes[AnalysisCacheKind.Activity])
            session.close()
        } finally {
            model.close()
        }
    }

    private class TestConstraintGroup(override val name: String) : MetaConstraintGroup

    private fun variableId(variable: IntVar): VariableId {
        return VariableId("${variable.identifier}:${variable.index}")
    }
}
