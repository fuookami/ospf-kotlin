package fuookami.ospf.kotlin.core.analysis

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.SolveProof
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.SolveSolution
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.core.solver.LinearSolver
import fuookami.ospf.kotlin.core.variable.IntVar

class FixedIntegerLpAndPerturbationAnalyzerTest {
    @Test
    fun explicitProvenanceMappingSkipsAmbiguousLoweredRows() {
        val firstOrigin = Any()
        val duplicateOrigin = Any()
        val uniqueOrigin = Any()
        val mapped = mapExplicitProvenanceDuals(
            rowProvenance = listOf(
                ConstraintId("global"),
                ConstraintId("global"),
                ConstraintId("capacity"),
                null
            ),
            rowOrigins = listOf(firstOrigin, duplicateOrigin, uniqueOrigin, Any()),
            dualsByOrigin = mapOf(
                firstOrigin to Flt64(1.0),
                duplicateOrigin to Flt64(2.0),
                uniqueOrigin to Flt64(3.0)
            ),
            originalIds = setOf(ConstraintId("global"), ConstraintId("capacity"))
        )

        assertEquals(mapOf(ConstraintId("capacity") to Flt64(3.0)), mapped)
    }

    @Test
    fun fixedIntegerLpFixesEveryVariableAndCachesBySession() = runBlocking {
        val model = ConstraintProgrammingModel("fixed-lp", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(5)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val baseline = ConstraintProgrammingSolution(
                values = mapOf(variableId(x) to Int64(5))
            )
            var calls = 0
            var fixedValues: Map<VariableId, Int64>? = null
            val backend = FixedIntegerLpBackend { request ->
                calls += 1
                fixedValues = request.fixedValues
                Ok(
                    FixedIntegerLpSolveResult(
                        status = AnalysisStatus.Reachable,
                        objectiveValue = Flt64(5.0),
                        duals = mapOf(ConstraintId("capacity") to Flt64(2.5))
                    )
                )
            }
            val session = CriticalConstraintAnalysisSession.fromSnapshot(
                snapshot = snapshot,
                baselineSolution = baseline,
                baselineObjectiveValue = Flt64(5.0),
                capabilityMatrix = CapabilityMatrix(
                    analysisCapabilities = mapOf(
                        AnalysisCapability.FixedIntegerLpSensitivity to CapabilitySupport.Supported
                    )
                )
            )

            val analyzer = FixedIntegerLpSensitivityAnalyzer(backend)
            val first = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(analyzer.analyze(session)).value!!
            val second = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(analyzer.analyze(session)).value!!

            assertEquals(first, second)
            assertEquals(1, calls)
            assertEquals(mapOf(variableId(x) to Int64(5)), fixedValues)
            assertEquals(AnalysisStatus.Reachable, first.status)
            assertEquals(Flt64(2.5), first.constraint(ConstraintId("capacity"))!!.dualValue)
            assertTrue(first.constraint(ConstraintId("capacity"))!!.localEffective == true)
            assertEquals(LocalSensitivityScope.FixedIntegerIncumbent, first.scope)
            assertEquals(1, session.cacheSizes[AnalysisCacheKind.FixedIntegerLp])
            session.close()
        } finally {
            model.close()
        }
    }

    @Test
    fun dualFailureDegradesFixedIntegerLpToUnsupported() = runBlocking {
        val model = ConstraintProgrammingModel("fixed-lp-dual-failure", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 2).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(2)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val baseline = ConstraintProgrammingSolution(
                values = mapOf(variableId(x) to Int64(2))
            )

            val result = LinearSolverFixedIntegerLpBackend(
                linearSolver = PrimaryThenDualFailureLinearSolver()
            ).solve(
                FixedIntegerLpRequest(
                    snapshot = snapshot,
                    fixedValues = baseline.values,
                    baselineSolution = baseline,
                    options = FixedIntegerLpSensitivityOptions()
                )
            )
            val solved = assertIs<Ok<FixedIntegerLpSolveResult, *, *>>(result).value!!

            assertEquals(AnalysisStatus.Unsupported, solved.status)
            assertTrue(solved.duals.isEmpty())
            assertTrue(solved.message.orEmpty().contains("dual", ignoreCase = true))
        } finally {
            model.close()
        }
    }

    @Test
    fun missingBaselineRemainsUnknownAndUnsupportedRemainsDistinct() = runBlocking {
        val model = ConstraintProgrammingModel("fixed-lp-unknown")
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(5)).value!!,
                id = ConstraintId("capacity")
            )
            val snapshot = model.snapshot().value!!
            val backend = FixedIntegerLpBackend {
                Ok(FixedIntegerLpSolveResult(AnalysisStatus.Reachable))
            }
            val unknown = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                FixedIntegerLpSensitivityAnalyzer(backend).analyze(snapshot, null)
            ).value!!
            val unsupported = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                FixedIntegerLpSensitivityAnalyzer().analyze(
                    snapshot = snapshot,
                    solution = ConstraintProgrammingSolution(
                        values = mapOf(variableId(x) to Int64(1))
                    )
                )
            ).value!!

            assertEquals(AnalysisStatus.Unknown, unknown.status)
            assertEquals(AnalysisStatus.Unsupported, unsupported.status)
        } finally {
            model.close()
        }
    }

    @Test
    fun adaptivePerturbationFindsFirstEffectiveDelta() = runBlocking {
        val model = ConstraintProgrammingModel("adaptive", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 100).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(10)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val baseline = ConstraintProgrammingSolution(values = mapOf(variableId(x) to Int64(10)))
            val backend = ConstraintPerturbationBackend { request ->
                val delta = request.delta?.toDouble() ?: 0.0
                val improved = delta >= 8.0
                Ok(
                    ConstraintPerturbationSolveResult(
                        status = AnalysisStatus.Reachable,
                        objectiveValue = Flt64(if (improved) 11.0 else 10.0),
                        solution = ConstraintProgrammingSolution(
                            values = mapOf(variableId(x) to Int64(if (improved) 11 else 10))
                        )
                    )
                )
            }
            val report = assertIs<Ok<ConstraintPerturbationReport, *, *>>(
                ConstraintPerturbationAnalyzer(backend).analyze(
                    snapshot = snapshot,
                    constraintId = ConstraintId("capacity"),
                    baselineSolution = baseline,
                    baselineObjective = Flt64(10.0),
                    capabilityMatrix = CapabilityMatrix(
                        analysisCapabilities = mapOf(
                            AnalysisCapability.RhsPerturbation to CapabilitySupport.Supported
                        )
                    ),
                    policy = ConstraintPerturbationPolicy(
                        deltas = emptyList(),
                        adaptive = AdaptivePerturbationPolicy(
                            initialDelta = Flt64(1.0),
                            maxDelta = Flt64(8.0),
                            maxSteps = 4
                        )
                    )
                )
            ).value!!

            assertEquals(AnalysisStatus.Reachable, report.status)
            assertEquals(4, report.observations.size)
            assertEquals(listOf(1.0, 2.0, 4.0, 8.0), report.observations.map { it.delta!!.toDouble() })
            assertEquals(PerturbationOutcome.Effective, report.observations.last().outcome)
            assertEquals(AdaptivePerturbationOutcome.ThresholdInterval, report.adaptive!!.outcome)
            assertEquals(4.0, report.adaptive!!.lowerBound!!.toDouble())
            assertEquals(8.0, report.adaptive!!.upperBound!!.toDouble())
            assertTrue(report.observations.last().integerPatternChanged == true)
            assertNotNull(report.observations.last().perturbedRhs)
            assertEquals(10L, snapshot.constraint(ConstraintId("capacity"))!!.let {
                (it.constraint as ConstraintProgrammingConstraint.IntegerComparison).rhs.toLong()
            })
        } finally {
            model.close()
        }
    }

    @Test
    fun removalTestUsesDerivedSnapshotAndCanBeCached() = runBlocking {
        val model = ConstraintProgrammingModel("removal", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 100).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(10)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val baseline = ConstraintProgrammingSolution(values = mapOf(variableId(x) to Int64(10)))
            var calls = 0
            val backend = ConstraintPerturbationBackend { request ->
                calls += 1
                val removal = request.kind == ConstraintPerturbationKind.Removal
                assertFalse(request.baselineSnapshot.constraints.isEmpty())
                if (removal) {
                    assertTrue(request.derivedSnapshot!!.constraint(ConstraintId("capacity")) == null)
                }
                Ok(
                    ConstraintPerturbationSolveResult(
                        status = AnalysisStatus.Reachable,
                        objectiveValue = Flt64(if (removal) 11.0 else 10.0),
                        solution = baseline
                    )
                )
            }
            val session = CriticalConstraintAnalysisSession.fromSnapshot(
                snapshot = snapshot,
                baselineSolution = baseline,
                baselineObjectiveValue = Flt64(10.0),
                capabilityMatrix = CapabilityMatrix(
                    analysisCapabilities = mapOf(
                        AnalysisCapability.RhsPerturbation to CapabilitySupport.Supported
                    )
                )
            )
            val analyzer = ConstraintPerturbationAnalyzer(backend)
            val policy = ConstraintPerturbationPolicy(
                deltas = listOf(Flt64.one),
                removalTest = true,
                maxSolves = 2
            )
            val first = assertIs<Ok<ConstraintPerturbationReport, *, *>>(
                analyzer.analyze(session, ConstraintId("capacity"), policy)
            ).value!!
            val second = assertIs<Ok<ConstraintPerturbationReport, *, *>>(
                analyzer.analyze(session, ConstraintId("capacity"), policy)
            ).value!!

            assertEquals(first, second)
            assertEquals(2, calls)
            assertEquals(PerturbationOutcome.NoObservedEffect, first.observations.single().outcome)
            assertEquals(PerturbationOutcome.Effective, first.removal!!.outcome)
            assertEquals(true, first.removalEffective)
            assertEquals(1, session.cacheSizes[AnalysisCacheKind.Perturbation])
            assertEquals(1, snapshot.constraints.size)
            session.close()
        } finally {
            model.close()
        }
    }

    @Test
    fun removalOnlyDoesNotRequireRhsCapability() = runBlocking {
        val model = ConstraintProgrammingModel("removal-only", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 100).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(10)).value!!,
                id = ConstraintId("capacity")
            )
            val snapshot = model.snapshot().value!!
            val backend = ConstraintPerturbationBackend { request ->
                assertEquals(ConstraintPerturbationKind.Removal, request.kind)
                Ok(
                    ConstraintPerturbationSolveResult(
                        status = AnalysisStatus.Reachable,
                        objectiveValue = Flt64(11.0)
                    )
                )
            }
            val report = assertIs<Ok<ConstraintPerturbationReport, *, *>>(
                ConstraintPerturbationAnalyzer(backend).analyze(
                    snapshot = snapshot,
                    constraintId = ConstraintId("capacity"),
                    baselineObjective = Flt64(10.0),
                    capabilityMatrix = CapabilityMatrix(
                        analysisCapabilities = mapOf(
                            AnalysisCapability.RemovalTest to CapabilitySupport.Supported
                        )
                    ),
                    policy = ConstraintPerturbationPolicy(
                        deltas = emptyList(),
                        removalTest = true
                    )
                )
            ).value!!

            assertEquals(AnalysisStatus.Reachable, report.status)
            assertTrue(report.observations.isEmpty())
            assertEquals(PerturbationOutcome.Effective, report.removal!!.outcome)
        } finally {
            model.close()
        }
    }

    private fun variableId(variable: IntVar): VariableId {
        return VariableId("${variable.identifier}:${variable.index}")
    }
}

private class PrimaryThenDualFailureLinearSolver : LinearSolver {
    override val config: SolverConfig = SolverConfig()
    override val name: String = "primary-then-dual-failure"

    private var calls = 0

    override suspend fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        if (calls++ > 0) {
            return Failed(ErrorCode.Other, "dual solve unavailable")
        }
        return ok(
            SolveReport(
                problemStatus = ProblemStatus.Feasible,
                terminationReason = TerminationReason.Completed,
                solutionPresence = SolutionPresence.Optimal,
                solution = SolveSolution(
                    values = model.variables.indices.map { Flt64.zero },
                    objective = Flt64(2.0)
                ),
                proof = SolveProof(ProofStatus.Verified)
            )
        )
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return invoke(model, solvingStatusCallBack).map { it to emptyList() }
    }
}
