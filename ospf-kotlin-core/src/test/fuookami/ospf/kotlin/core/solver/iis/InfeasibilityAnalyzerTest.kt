package fuookami.ospf.kotlin.core.solver.iis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.ZERO
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjective
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.SolveOptions
import fuookami.ospf.kotlin.core.solver.solveWithOptionsAndIIS
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.output.SolvingStatus
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.EvidenceExactness
import fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness
import fuookami.ospf.kotlin.core.solver.report.EvidenceValidity
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidence
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityEvidenceSource
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ProblemStatus
import fuookami.ospf.kotlin.core.solver.report.ProofStatus
import fuookami.ospf.kotlin.core.solver.report.SolutionPresence
import fuookami.ospf.kotlin.core.solver.report.SolveIssueCategory
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.TerminationReason
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class InfeasibilityAnalyzerTest {
    @Test
    fun chainShouldReturnFirstAvailableEvidence() = runBlocking {
        val expected = InfeasibilityEvidence(
            source = InfeasibilityEvidenceSource.ConstraintConflict,
            exactness = EvidenceExactness.Exact
        )
        val chain = InfeasibilityAnalyzerChain<String>(
            listOf(
                object : InfeasibilityAnalyzer<String> {
                    override val source = InfeasibilityEvidenceSource.NativeIIS

                    override suspend fun analyze(model: String): Ret<InfeasibilityEvidence> {
                        return Failed(
                            fuookami.ospf.kotlin.utils.error.ErrorCode.Other,
                            "native unavailable"
                        )
                    }
                },
                object : InfeasibilityAnalyzer<String> {
                    override val source = InfeasibilityEvidenceSource.ConstraintConflict

                    override suspend fun analyze(model: String): Ret<InfeasibilityEvidence> {
                        return ok(expected)
                    }
                }
            )
        )
        assertEquals(expected, chain.analyze("model").value)
    }

    @Test
    fun emptyChainShouldReturnStructuredFailure() = runBlocking {
        assertTrue(InfeasibilityAnalyzerChain<String>(emptyList()).analyze("model").failed)
    }

    @Test
    fun orchestratorShouldPreferNativeAndPreserveInfeasibleOnFailure() = runBlocking {
        val nativeEvidence = InfeasibilityEvidence(
            source = InfeasibilityEvidenceSource.NativeIIS,
            exactness = EvidenceExactness.Exact
        )
        val native = NativeIISAnalyzerAdapter<String>(
            modelTypes = emptySet(),
            delegate = { ok(nativeEvidence) }
        )
        val fallback = DelegatingInfeasibilityAnalyzer<String>(
            capabilities = InfeasibilityAnalyzerCapabilities(
                source = InfeasibilityEvidenceSource.ElasticFilter
            ),
            delegate = {
                Failed(
                    fuookami.ospf.kotlin.utils.error.ErrorCode.Other,
                    "fallback unavailable"
                )
            }
        )
        val orchestrator = InfeasibilityDiagnosticOrchestrator(listOf(fallback, native))
        assertEquals(nativeEvidence, orchestrator.analyze("model").value)

        val report = SolveReport<String>(
            problemStatus = ProblemStatus.Infeasible,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.None,
            proof = fuookami.ospf.kotlin.core.solver.report.SolveProof(ProofStatus.Verified)
        )
        val failedOrchestrator = InfeasibilityDiagnosticOrchestrator<String>(
            listOf(
                DelegatingInfeasibilityAnalyzer(
                    capabilities = InfeasibilityAnalyzerCapabilities(
                        source = InfeasibilityEvidenceSource.NativeIIS
                    ),
                    delegate = {
                        Failed(
                            fuookami.ospf.kotlin.utils.error.ErrorCode.Other,
                            "native diagnostic failed"
                        )
                    }
                )
            )
        )
        val preserved = failedOrchestrator.analyzeInto(report, "model")
        assertEquals(ProblemStatus.Infeasible, preserved.problemStatus)
        assertEquals(SolveIssueCategory.Backend, preserved.diagnostics.errors.single().category)
    }

    @Test
    fun orchestratorShouldFilterUnsupportedNativeAndFarkasCapabilities() = runBlocking {
        val conflictEvidence = InfeasibilityEvidence(
            source = InfeasibilityEvidenceSource.ConstraintConflict,
            exactness = EvidenceExactness.Exact
        )
        val native = NativeIISAnalyzerAdapter<String>(
            delegate = { ok(InfeasibilityEvidence(source = InfeasibilityEvidenceSource.NativeIIS)) }
        )
        val farkas = FarkasAnalyzerAdapter<String>(
            delegate = { ok(InfeasibilityEvidence(source = InfeasibilityEvidenceSource.Farkas)) }
        )
        val conflict = DelegatingInfeasibilityAnalyzer<String>(
            capabilities = InfeasibilityAnalyzerCapabilities(
                modelTypes = setOf(SolverModelType.CP),
                exact = true,
                source = InfeasibilityEvidenceSource.ConstraintConflict
            ),
            delegate = { ok(conflictEvidence) }
        )
        val orchestrator = InfeasibilityDiagnosticOrchestrator(
            analyzers = listOf(native, farkas, conflict),
            solverCapabilities = SolverCapabilities(
                modelTypes = setOf(SolverModelType.CP),
                nativeIIS = false,
                farkas = false,
                constraintProgrammingFeatures = mapOf(
                    fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature.ConflictCore to
                        fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel.Native
                )
            )
        )

        assertEquals(listOf(InfeasibilityEvidenceSource.ConstraintConflict), orchestrator.availableAnalyzers().map { it.source })
        assertEquals(conflictEvidence, orchestrator.analyze("model").value)
    }

    @Test
    fun unavailableAnalyzerShouldReturnStructuredEvidence() = runBlocking {
        val result = InfeasibilityDiagnosticOrchestrator<String>(
            analyzers = emptyList(),
            solverCapabilities = SolverCapabilities(modelTypes = setOf(SolverModelType.CP))
        ).analyze("model")

        assertTrue(result.ok)
        assertEquals(InfeasibilityEvidenceSource.None, result.value!!.source)
        assertEquals(fuookami.ospf.kotlin.core.solver.report.EvidenceCompleteness.Unavailable, result.value!!.completeness)
        assertEquals(fuookami.ospf.kotlin.core.solver.report.EvidenceValidity.Unknown, result.value!!.validity)
    }

    @Test
    fun legacyEvidenceMustRemainHeuristicWhenItIsAvailable() = runBlocking {
        val analyzer = LegacyElasticInfeasibilityAnalyzer(
            solver = object : AbstractLinearSolver {
                override val name: String = "legacy-test-solver"

                override suspend fun invoke(
                    model: LinearTriadModelView,
                    solvingStatusCallBack: SolvingStatusCallBack?
                ): Ret<SolveReport<Flt64>> {
                    return ok(
                        SolverStatus.Feasible.toSolveReport(
                            objective = Flt64.zero,
                            values = emptyList<Flt64>(),
                            solveTime = kotlin.time.Duration.ZERO,
                            bestBound = Flt64.zero,
                            gap = Flt64.zero
                        )
                    )
                }

                override suspend fun invoke(
                    model: LinearTriadModelView,
                    solutionAmount: UInt64,
                    solvingStatusCallBack: SolvingStatusCallBack?
                ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
                    return ok(
                        SolverStatus.Feasible.toSolveReport(
                            objective = Flt64.zero,
                            values = emptyList<Flt64>(),
                            solveTime = kotlin.time.Duration.ZERO,
                            bestBound = Flt64.zero,
                            gap = Flt64.zero
                        ) to emptyList<List<Flt64>>()
                    )
                }
            }
        )
        val model = LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "legacy-model"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )

        val result: Ret<InfeasibilityEvidence> = analyzer.analyze(model)
        assertTrue(result.ok)
        assertEquals(EvidenceValidity.Heuristic, result.value!!.validity)
        assertTrue(result.value!!.source == InfeasibilityEvidenceSource.ElasticFilter || result.value!!.source == InfeasibilityEvidenceSource.DeletionFilter)
    }

    @Test
    fun publicComputeIISShouldInvokeNativeProviderBeforeLegacyArtifactFallback() = runBlocking {
        var providerInvocations = 0
        var solverInvocations = 0
        val solver = object : AbstractLinearSolver {
            override val name: String = "public-compute-iis-provider-test"
            override val descriptor = SolverDescriptor(
                solverId = name,
                backendName = name,
                capabilities = SolverCapabilities(
                    modelTypes = setOf(SolverModelType.LP),
                    nativeIIS = true
                )
            )

            override fun diagnosticAnalyzers(
                config: IISConfig
            ): List<InfeasibilityAnalyzer<LinearTriadModelView>> {
                return listOf(
                    DelegatingInfeasibilityAnalyzer(
                        capabilities = InfeasibilityAnalyzerCapabilities(
                            modelTypes = setOf(SolverModelType.LP),
                            exact = true,
                            source = InfeasibilityEvidenceSource.NativeIIS
                        ),
                        delegate = {
                            providerInvocations += 1
                            ok(
                                InfeasibilityEvidence(
                                    source = InfeasibilityEvidenceSource.NativeIIS,
                                    exactness = EvidenceExactness.Exact,
                                    completeness = EvidenceCompleteness.Complete,
                                    constraintIds = setOf(ConstraintId("stable:constraint:constant-row"))
                                )
                            )
                        }
                    )
                )
            }

            override suspend fun invoke(
                model: LinearTriadModelView,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<SolveReport<Flt64>> {
                ++solverInvocations
                return ok(
                    SolverStatus.Feasible.toSolveReport(
                        objective = Flt64.zero,
                        values = emptyList(),
                        solveTime = kotlin.time.Duration.ZERO,
                        bestBound = Flt64.zero,
                        gap = Flt64.zero
                    )
                )
            }

            override suspend fun invoke(
                model: LinearTriadModelView,
                solutionAmount: UInt64,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
                ++solverInvocations
                return ok(
                    SolverStatus.Feasible.toSolveReport(
                        objective = Flt64.zero,
                        values = emptyList(),
                        solveTime = kotlin.time.Duration.ZERO,
                        bestBound = Flt64.zero,
                        gap = Flt64.zero
                ) to emptyList<List<Flt64>>()
                )
            }
        }
        val model = LinearTriadModel(
                impl = BasicLinearTriadModel(
                    variables = emptyList(),
                    constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix<Flt64>().also {
                        it.addRow(SparseVector())
                        it.addRow(SparseVector())
                    },
                    signs = listOf(ConstraintRelation.Equal, ConstraintRelation.Equal),
                    rhs = listOf(Flt64.one, Flt64.zero),
                    names = listOf("constant-row", "unselected-row"),
                    sources = listOf(ConstraintSource.Origin, ConstraintSource.Origin),
                     ids = listOf(
                         ConstraintId("stable:constraint:constant-row"),
                         ConstraintId("model-local-constraint:unselected-row")
                     ),
                    identityScopes = listOf(ModelElementScope.Stable, ModelElementScope.ModelLocal),
                    identityOrigins = listOf(
                        ModelElementOrigin("domain", "constant-row"),
                        null
                    )
                ),
                name = "public-compute-iis-model"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )

        val result = computeIIS(model, solver, IISConfig())
        assertEquals(1, providerInvocations)
        assertEquals(0, solverInvocations)
        assertTrue(result.ok)
        assertEquals(1, result.value?.constraints?.size)
        assertEquals("stable:constraint:constant-row", result.value?.constraints?.ids?.single()?.value)
        assertEquals(ModelElementScope.Stable, result.value?.constraints?.identityScopeAt(0))
        assertEquals(
            ModelElementOrigin("domain", "constant-row"),
            result.value?.constraints?.identityOriginAt(0)
        )
    }

    @Test
    fun solvingStatusCallbackFailureMustNotBeDiscardedByIisFacade() = runBlocking {
        val callbackError: Try = Failed(ErrorCode.ApplicationError, "callback failed")
        val solver = object : AbstractLinearSolver {
            override val name: String = "callback-failure-test"

            override suspend fun invoke(
                model: LinearTriadModelView,
                solvingStatusCallBack: SolvingStatusCallBack?
            ): Ret<SolveReport<Flt64>> {
                // Deliberately ignore the callback result to exercise the facade guard.
                // 故意忽略回调返回值，以验证 facade 自身的失败传播门禁。
                solvingStatusCallBack?.invoke(
                    SolvingStatus(
                        solver = name,
                        solverConfig = SolverConfig(),
                        objectCategory = ObjectCategory.Minimum,
                        time = ZERO,
                        obj = Flt64.zero,
                        possibleBestObj = Flt64.zero,
                        initialBestObj = Flt64.zero,
                        gap = Flt64.zero
                    )
                )
                return ok(
                    SolverStatus.Feasible.toSolveReport(
                        objective = Flt64.zero,
                        values = emptyList(),
                        solveTime = ZERO,
                        bestBound = Flt64.zero,
                        gap = Flt64.zero
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

        val result = solver.solveWithOptionsAndIIS(
            model = emptyLinearModel(),
            options = SolveOptions(
                solvingStatusCallBack = SolvingStatusCallBack { callbackError }
            ),
            iisConfig = IISConfig()
        )
        val failure = assertIs<Failed<*, *, *>>(result)
        assertEquals(ErrorCode.ApplicationError, failure.error.code)
    }

    private fun emptyLinearModel(): LinearTriadModel {
        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "callback-failure-model"
            ),
            tokensInSolver = emptyList(),
            objective = LinearObjective(
                category = ObjectCategory.Minimum,
                objective = emptyList()
            )
        )
    }
}
