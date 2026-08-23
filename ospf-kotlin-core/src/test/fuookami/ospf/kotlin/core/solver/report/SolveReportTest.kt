package fuookami.ospf.kotlin.core.solver.report

import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ConstraintSource
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Objective
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation as ModelConstraintRelation
import fuookami.ospf.kotlin.core.model.intermediate.BasicLinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.BasicQuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.LinearConstraintBatch
import fuookami.ospf.kotlin.core.model.intermediate.LinearObjectiveCell
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticObjectiveCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.SparseMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticMatrix
import fuookami.ospf.kotlin.core.model.intermediate.SparseQuadraticVector
import fuookami.ospf.kotlin.core.model.intermediate.SparseVector
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.solver.toSolveReport
import fuookami.ospf.kotlin.core.solver.output.LinearInfeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.variable.Continuous
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

class SolveReportTest {
    @Test
    fun shouldKeepProblemTerminationAndSolutionPresenceOrthogonal() {
        val report = SolveReport<Double>(
            problemStatus = ProblemStatus.Feasible,
            terminationReason = TerminationReason.TimeLimit,
            solutionPresence = SolutionPresence.Incumbent,
            solution = SolveSolution(
                values = listOf(1.0),
                objective = 1.0
            )
        )

        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(TerminationReason.TimeLimit, report.terminationReason)
        assertEquals(SolutionPresence.Incumbent, report.solutionPresence)
    }

    @Test
    fun legacyOutputShouldMapToReportAndPreserveSolutionPoolAndIisState() {
        val output = SolverStatus.Feasible.toSolveReport(
            objective = Flt64(10.0),
            values = listOf(Flt64.one, Flt64.two),
            solveTime = 2.seconds,
            bestBound = Flt64(8.0),
            gap = Flt64(0.25)
        )
        val report = output.toSolveReport(
            solutionPool = listOf(
                listOf(Flt64.one, Flt64.two),
                listOf(Flt64.zero, Flt64.one)
            )
        )

        assertEquals(ProblemStatus.Feasible, report.problemStatus)
        assertEquals(TerminationReason.Completed, report.terminationReason)
        assertEquals(SolutionPresence.Incumbent, report.solutionPresence)
        assertEquals(Flt64(0.25), report.statistics.gap)
        assertEquals(2, report.solution?.pool?.size)

        val iis = LinearInfeasibleSolverOutput(
            iis = BasicLinearTriadModel(
                variables = emptyList(),
                constraints = LinearConstraintBatch(
                    sparseLhs = SparseMatrix(),
                    signs = emptyList(),
                    rhs = emptyList(),
                    names = emptyList(),
                    sources = emptyList()
                ),
                name = "legacy-iis"
            ),
            iisAvailable = false
        )
        val iisReport = iis.toSolveReport()
        assertEquals(ProblemStatus.Infeasible, iisReport.problemStatus)
        assertEquals(SolutionPresence.None, iisReport.solutionPresence)
        assertEquals(EvidenceCompleteness.Unavailable, iisReport.diagnostics.infeasibilityEvidence?.completeness)
    }

    @Test
    fun shouldCancelOnlyOnceAndKeepFirstCancellationFact() {
        var interruptionCount = 0
        val handle = SolveHandle.create {
            interruptionCount += 1
            ok
        }

        handle.cancel(CancellationSource.Remote, "用户取消")
        handle.cancel(CancellationSource.Timeout, "超时")

        assertEquals(1, interruptionCount)
        assertTrue(handle.token.isCancellationRequested)
        assertEquals(CancellationSource.Remote, handle.token.record?.source)
        assertEquals("用户取消", handle.token.record?.reason)
    }

    @Test
    fun shouldProduceStableCryptographicFingerprints() {
        val first = SolveFingerprinting.configuration(mapOf("threads" to "4", "seed" to "7"))
        val reordered = SolveFingerprinting.configuration(mapOf("seed" to "7", "threads" to "4"))
        val changed = SolveFingerprinting.configuration(mapOf("seed" to "8", "threads" to "4"))

        assertEquals(first, reordered)
        assertNotEquals(first, changed)
        assertEquals("SHA-256", first.algorithm)
        assertEquals(64, first.value.length)
    }

    @Test
    fun reportNormalizationPreservesExistingAuditMetadataByDefault() {
        val provenance = SolverProvenance(
            descriptor = SolverDescriptor(
                solverId = "fixture",
                backendName = "fixture",
                capabilities = SolverCapabilities(modelTypes = setOf(SolverModelType.LP))
            )
        )
        val fingerprints = SolveFingerprints(
            model = AuditFingerprint("1.0", "SHA-256", "model-fingerprint"),
            configuration = AuditFingerprint("1.0", "SHA-256", "configuration-fingerprint"),
            solver = AuditFingerprint("1.0", "SHA-256", "solver-fingerprint")
        )
        val report = SolveReport<Flt64>(
            problemStatus = ProblemStatus.Unknown,
            terminationReason = TerminationReason.Completed,
            solutionPresence = SolutionPresence.None,
            provenance = provenance,
            fingerprints = fingerprints
        )

        val normalized = report.toSolveReport()

        assertEquals(provenance, normalized.provenance)
        assertEquals(fingerprints, normalized.fingerprints)
    }

    /**
     * 验证规范化模型的身份元数据参与稳定指纹，并可跨独立重建复现。
     * Verifies normalized identity metadata participates in stable fingerprints and survives independent rebuilds.
     */
    @Test
    fun normalizedModelFingerprintCarriesIdentityMetadata() {
        fun model(provenance: List<ModelElementOrigin>): NormalizedMathematicalModel {
            return NormalizedMathematicalModel(
                modelType = SolverModelType.MIP,
                identityNamespace = "stable-model",
                identitySchemaVersion = "1.0",
                variables = listOf(
                    NormalizedVariable(
                        id = VariableId("variable:x"),
                        type = "Integer",
                        scope = ModelElementScope.Stable,
                        origin = ModelElementOrigin("pipeline", "capacity"),
                        identityProvenance = provenance
                    )
                ),
                constraints = emptyList(),
                objective = NormalizedObjective(
                    id = ObjectiveId("objective:cost"),
                    category = "Minimum",
                    constant = "0",
                    linearTerms = emptyList()
                )
            )
        }

        val capacity = ModelElementOrigin("pipeline", "capacity")
        val source = ModelElementOrigin("source", "capacity")
        val first = model(listOf(capacity, source)).fingerprint()
        val rebuilt = model(listOf(source, capacity)).fingerprint()
        val changed = model(listOf(capacity, ModelElementOrigin("pipeline", "other"))).fingerprint()

        assertEquals(first, rebuilt)
        assertNotEquals(first, changed)
    }

    @Test
    fun normalizedModelFactoriesPreserveLinearAndQuadraticIdentityMetadata() {
        val variable = fuookami.ospf.kotlin.core.model.basic.Variable(
            index = 0,
            lowerBound = Flt64.zero,
            upperBound = Flt64.one,
            type = Continuous,
            origin = null,
            name = "x",
            id = VariableId("variable:x"),
            identityScope = ModelElementScope.Stable,
            identityOrigin = ModelElementOrigin("pipeline", "x"),
            identityProvenance = listOf(
                ModelElementOrigin("pipeline", "x"),
                ModelElementOrigin("source", "x")
            )
        )
        val linearConstraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix(),
            signs = emptyList(),
            rhs = emptyList(),
            names = emptyList(),
            sources = emptyList(),
            identityNamespace = "stable-model",
            identitySchemaVersion = "1.0"
        )
        val linear = LinearTriadModel(
            impl = BasicLinearTriadModel(listOf(variable), linearConstraints, "linear"),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Minimum,
                objective = listOf(LinearObjectiveCell(0, Flt64.one)),
                constant = Flt64(2.0),
                id = ObjectiveId("objective:cost"),
                identityScope = ModelElementScope.Stable,
                identityOrigin = ModelElementOrigin("pipeline", "cost"),
                identityProvenance = listOf(
                    ModelElementOrigin("pipeline", "cost"),
                    ModelElementOrigin("source", "cost")
                )
            )
        )
        val normalizedLinear = linear.toNormalizedMathematicalModel()
        assertEquals("stable-model", normalizedLinear.identityNamespace)
        assertEquals(ModelElementScope.Stable, normalizedLinear.variables.single().scope)
        assertEquals("variable:x", normalizedLinear.objective.linearTerms.single().variableId.value)
        assertEquals(
            listOf(
                ModelElementOrigin("pipeline", "x"),
                ModelElementOrigin("source", "x")
            ),
            normalizedLinear.variables.single().identityProvenance
        )
        assertEquals(
            listOf(
                ModelElementOrigin("pipeline", "cost"),
                ModelElementOrigin("source", "cost")
            ),
            normalizedLinear.objective.identityProvenance
        )

        val quadraticConstraints = fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also { it.addRow(SparseQuadraticVector()) },
            signs = listOf(ModelConstraintRelation.Equal),
            rhs = listOf(Flt64.zero),
            names = listOf("c"),
            sources = listOf(ConstraintSource.Origin),
            ids = listOf(ConstraintId("constraint:c")),
            identityNamespace = "stable-model",
            identitySchemaVersion = "1.0"
        )
        val quadratic = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(listOf(variable), quadraticConstraints, "quadratic"),
            tokensInSolver = emptyList(),
            objective = Objective(
                category = ObjectCategory.Maximum,
                objective = listOf(QuadraticObjectiveCell(0, 0, Flt64.one)),
                id = ObjectiveId("objective:profit")
            )
        )
        val normalizedQuadratic = quadratic.toNormalizedMathematicalModel()
        assertEquals(SolverModelType.QP, normalizedQuadratic.modelType)
        assertEquals("Maximum", normalizedQuadratic.objective.category)
        assertEquals(1, normalizedQuadratic.objective.quadraticTerms.size)
    }

    @Test
    fun canonicalEncodingEscapesNestedTermDelimiters() {
        val model = NormalizedMathematicalModel(
            modelType = SolverModelType.LP,
            variables = listOf(
                NormalizedVariable(VariableId("variable:a,b"), "Continuous")
            ),
            constraints = emptyList(),
            objective = NormalizedObjective(
                id = ObjectiveId("objective:cost"),
                category = "Minimum",
                constant = "0",
                linearTerms = listOf(
                    NormalizedLinearTerm(VariableId("variable:a,b"), "coefficient:c:d")
                )
            )
        )

        val canonical = model.canonicalText()
        assertTrue("variable\\:a\\,b" in canonical)
        assertTrue("coefficient\\:c\\:d" in canonical)
        assertEquals(model.fingerprint(), model.copy().fingerprint())
    }

    @Test
    fun normalizedFingerprintSortsDuplicateTermsByCompleteTuplesAndKeepsElementMetadata() {
        fun model(
            linearTerms: List<NormalizedLinearTerm>,
            quadraticTerms: List<NormalizedQuadraticTerm>,
            variableNamespace: String = "variable-namespace",
            objectiveNamespace: String = "objective-namespace"
        ): NormalizedMathematicalModel {
            return NormalizedMathematicalModel(
                modelType = SolverModelType.QP,
                variables = listOf(
                    NormalizedVariable(
                        id = VariableId("variable:x"),
                        type = "Continuous",
                        identityNamespace = variableNamespace,
                        identitySchemaVersion = "1.0"
                    )
                ),
                constraints = listOf(
                    NormalizedConstraint(
                        id = ConstraintId("constraint:duplicate"),
                        relation = ConstraintRelation.Equal,
                        rhs = "0",
                        linearTerms = linearTerms,
                        quadraticTerms = quadraticTerms
                    )
                ),
                objective = NormalizedObjective(
                    id = ObjectiveId("objective:duplicate"),
                    category = "Minimum",
                    constant = "0",
                    linearTerms = linearTerms,
                    quadraticTerms = quadraticTerms,
                    identityNamespace = objectiveNamespace,
                    identitySchemaVersion = "1.0"
                )
            )
        }

        val linearTerms = listOf(
            NormalizedLinearTerm(VariableId("variable:x"), "2"),
            NormalizedLinearTerm(VariableId("variable:x"), "1")
        )
        val quadraticTerms = listOf(
            NormalizedQuadraticTerm(VariableId("variable:x"), VariableId("variable:x"), "3"),
            NormalizedQuadraticTerm(VariableId("variable:x"), VariableId("variable:x"), "1")
        )
        val first = model(linearTerms, quadraticTerms)
        val reordered = model(linearTerms.reversed(), quadraticTerms.reversed())

        assertEquals(first.fingerprint(), reordered.fingerprint())
        assertNotEquals(
            first.fingerprint(),
            model(linearTerms, quadraticTerms, variableNamespace = "other-variable-namespace").fingerprint()
        )
        assertNotEquals(
            first.fingerprint(),
            model(linearTerms, quadraticTerms, objectiveNamespace = "other-objective-namespace").fingerprint()
        )
    }

    @Test
    fun normalizedQuadraticFingerprintCanonicalizesCommutativeVariablePairs() {
        fun model(
            firstVariableId: VariableId,
            secondVariableId: VariableId,
            objectiveNamespace: String = "model"
        ): NormalizedMathematicalModel {
            val quadraticTerms = listOf(
                NormalizedQuadraticTerm(firstVariableId, secondVariableId, "3")
            )
            return NormalizedMathematicalModel(
                modelType = SolverModelType.QCP,
                variables = listOf(
                    NormalizedVariable(
                        id = VariableId("variable:x"),
                        type = "Continuous",
                        identityNamespace = "model",
                        identitySchemaVersion = "1.0"
                    ),
                    NormalizedVariable(
                        id = VariableId("variable:y"),
                        type = "Continuous",
                        identityNamespace = "model",
                        identitySchemaVersion = "1.0"
                    )
                ),
                constraints = listOf(
                    NormalizedConstraint(
                        id = ConstraintId("constraint:product"),
                        relation = ConstraintRelation.Equal,
                        rhs = "0",
                        linearTerms = emptyList(),
                        quadraticTerms = quadraticTerms
                    )
                ),
                objective = NormalizedObjective(
                    id = ObjectiveId("objective:product"),
                    category = "Minimum",
                    constant = "0",
                    linearTerms = emptyList(),
                    quadraticTerms = quadraticTerms,
                    scope = ModelElementScope.Stable,
                    identityNamespace = objectiveNamespace,
                    identitySchemaVersion = "1.0"
                ),
                identityNamespace = "model",
                identitySchemaVersion = "1.0"
            )
        }

        val first = model(VariableId("variable:x"), VariableId("variable:y"))
        val swapped = model(VariableId("variable:y"), VariableId("variable:x"))
        assertEquals(first.fingerprint(), swapped.fingerprint())
        assertTrue(first.validateIdentity().ok)
        assertTrue(model(VariableId("variable:x"), VariableId("variable:y"), "other").validateIdentity().failed)
    }

    @Test
    fun constraintEvaluationReportsSlackViolationAndMissingValues() {
        val linearConstraints = LinearConstraintBatch(
            sparseLhs = SparseMatrix<Flt64>().also {
                it.addRow(SparseVector<Flt64>().also { row -> row.add(0, Flt64.one) })
            },
            signs = listOf(ModelConstraintRelation.LessEqual),
            rhs = listOf(Flt64.one),
            names = listOf("bound"),
            sources = listOf(ConstraintSource.Origin),
            ids = listOf(ConstraintId("constraint:bound"))
        )
        val linear = LinearTriadModel(
            impl = BasicLinearTriadModel(emptyList(), linearConstraints, "evaluation-linear"),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList())
        )

        val violated = linear.evaluateConstraints(listOf(Flt64(2.0)))
        assertTrue(violated is Ok)
        assertEquals(Flt64(2.0), violated.value.single().lhs)
        assertEquals(Flt64.one, violated.value.single().violation)
        assertFalse(violated.value.single().satisfied)
        assertTrue(linear.evaluateConstraints(emptyList()) is fuookami.ospf.kotlin.utils.functional.Failed)

        val quadraticConstraints = fuookami.ospf.kotlin.core.model.intermediate.QuadraticConstraintBatch(
            sparseLhs = SparseQuadraticMatrix().also {
                it.addRow(SparseQuadraticVector().also { row -> row.add(0, 1, Flt64.one) })
            },
            signs = listOf(ModelConstraintRelation.Equal),
            rhs = listOf(Flt64(6.0)),
            names = listOf("product"),
            sources = listOf(ConstraintSource.Origin),
            ids = listOf(ConstraintId("constraint:product"))
        )
        val quadratic = QuadraticTetradModel(
            impl = BasicQuadraticTetradModel(emptyList(), quadraticConstraints, "evaluation-quadratic"),
            tokensInSolver = emptyList(),
            objective = Objective(ObjectCategory.Minimum, emptyList())
        )
        val satisfied = quadratic.evaluateConstraints(listOf(Flt64(2.0), Flt64(3.0)))
        assertTrue(satisfied is Ok)
        assertEquals(Flt64(6.0), satisfied.value.single().lhs)
        assertTrue(satisfied.value.single().satisfied)
    }
}
