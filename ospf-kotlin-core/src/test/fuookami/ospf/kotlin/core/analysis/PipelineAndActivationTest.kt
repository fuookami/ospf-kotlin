package fuookami.ospf.kotlin.core.analysis

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.InfeasibilityMember
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.IntVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

class PipelineAndActivationTest {
    @Test
    fun activationSetTracksOriginalEvidenceAndRemapsBackendMembers() {
        val model = ConstraintProgrammingModel("activation", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 5).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(5)).value!!,
                id = ConstraintId("capacity")
            )
            val snapshot = model.snapshot().value!!

            val set = DiagnosticActivationSet.fromSnapshot(snapshot)
            val constraint = DiagnosticSource.Constraint(ConstraintId("capacity"))
            assertTrue(set.isActive(constraint))
            assertEquals(setOf(ConstraintId("capacity")), set.activeConstraintIds())

            set.deactivate(constraint)
            assertFalse(set.isActive(constraint))
            assertTrue(set.activeConstraintIds().isEmpty())

            // Every public identity stays inside the original-model evidence boundary.
            set.activations.forEach { activation ->
                val id = activation.source.stableId
                assertFalse(id.contains("row"), "activation leaked a solver row: $id")
                assertFalse(id.contains("column"), "activation leaked a solver column: $id")
            }

            // Backend evidence remaps only onto active original evidence.
            val remapped = set.remapBackendEvidence(
                listOf(InfeasibilityMember.Constraint(ConstraintId("capacity")))
            )
            assertTrue(remapped.isEmpty(), "a deactivated source must not be remapped back")

            set.activate(constraint)
            assertEquals(
                listOf(constraint),
                set.remapBackendEvidence(listOf(InfeasibilityMember.Constraint(ConstraintId("capacity"))))
            )
            // An unknown backend member is dropped rather than surfaced.
            assertTrue(
                set.remapBackendEvidence(
                    listOf(InfeasibilityMember.Constraint(ConstraintId("lowering-aux-row-7")))
                ).isEmpty()
            )
            assertTrue(set.validate() is Ok)
        } finally {
            model.close()
        }
    }

    @Test
    fun extractionTierFollowsDeclaredBackendCapability() {
        // Nothing declared: no tier may be claimed.
        assertEquals(
            ConflictExtractionTier.Unavailable,
            ConflictExtractionTier.select(CapabilityMatrix())
        )

        // A backend with a native core is preferred over an assumption path.
        val native = CapabilityMatrix(
            nativeAssumptionSolving = true,
            nativeUnsatCore = true,
            fallbackSatisfactionOnly = true
        )
        assertEquals(ConflictExtractionTier.NativeUnsatCore, ConflictExtractionTier.select(native))
        assertTrue(ConflictExtractionTier.select(native).isNative)

        // An assumption-only backend must not be reported as native.
        val assumptionOnly = CapabilityMatrix(
            nativeAssumptionSolving = true,
            fallbackSatisfactionOnly = true
        )
        assertEquals(
            ConflictExtractionTier.AssumptionExtraction,
            ConflictExtractionTier.select(assumptionOnly)
        )
        assertFalse(ConflictExtractionTier.select(assumptionOnly).isNative)

        // A satisfaction-only backend falls back to repeated solving.
        val fallback = CapabilityMatrix(fallbackSatisfactionOnly = true)
        assertEquals(ConflictExtractionTier.RepeatedSolving, ConflictExtractionTier.select(fallback))
    }

    private fun activity(
        id: String,
        group: String?,
        status: ActivityStatus
    ): ConstraintActivity {
        return ConstraintActivity(
            constraintId = ConstraintId(id),
            group = group,
            status = status,
            slack = 0.0,
            normalizedSlack = 0.0,
            tolerance = 1e-6,
            evidence = ActivityEvidence(
                source = DiagnosticSource.Constraint(ConstraintId(id)),
                kind = ActivityEvidenceKind.Comparison
            )
        )
    }

    private fun activityReport(vararg records: ConstraintActivity): ConstraintActivityReport {
        return ConstraintActivityReport(
            activities = records.toList(),
            groupSummaries = emptyList(),
            tolerance = 1e-6,
            nearlyActiveTolerance = 1e-5
        )
    }

    private fun sensitivity(vararg duals: Pair<String, Flt64?>): FixedIntegerLpSensitivityReport {
        return FixedIntegerLpSensitivityReport(
            status = AnalysisStatus.Reachable,
            baselineObjective = Flt64(10.0),
            sensitivities = duals.map { (id, dual) ->
                LocalConstraintSensitivity(
                    constraintId = ConstraintId(id),
                    dualValue = dual,
                    localEffective = dual?.let { kotlin.math.abs(it.toDouble()) > 1e-9 }
                )
            }
        )
    }

    @Test
    fun funnelTiersFollowActivityAndDualAndKeepEveryCandidate() {
        val report = activityReport(
            activity("strong", "Capacity", ActivityStatus.Active),
            activity("degenerate", "Capacity", ActivityStatus.Active),
            activity("slack", "Slot", ActivityStatus.Inactive),
            activity("semantic", "Slot", ActivityStatus.SatisfiedWithoutSlackMetric)
        )
        val report2 = sensitivity(
            "strong" to Flt64(12.5),
            "degenerate" to Flt64(0.0),
            "slack" to Flt64(0.0),
            "semantic" to null
        )

        val ranking = assertIs<Ok<CandidateFunnelRanking, *, *>>(
            buildCandidateFunnel(report, report2)
        ).value!!

        assertEquals(4, ranking.candidates.size, "Tier C must never be dropped")
        assertEquals(1, ranking.tierA)
        assertEquals(1, ranking.tierB)
        assertEquals(2, ranking.tierC)
        assertEquals(0, ranking.unclassified)
        assertEquals(ConstraintId("strong"), ranking.candidates[0].constraintId)
        assertEquals(CandidateTier.TierA, ranking.candidates[0].tier)

        // A saturated shortlist still falls back to lower tiers rather than losing them.
        assertEquals(1, ranking.shortlist(1).size)
        assertEquals(4, ranking.shortlist(10).size)
        assertTrue(ranking.validate() is Ok)
    }

    @Test
    fun missingDualEvidenceNeverBecomesATier() {
        val report = activityReport(activity("x", null, ActivityStatus.Active))

        // No sensitivity report at all: tiering is impossible and must stay unclassified.
        val without = assertIs<Ok<CandidateFunnelRanking, *, *>>(
            buildCandidateFunnel(report, null)
        ).value!!
        assertEquals(1, without.unclassified)
        assertEquals(0, without.tierA)
        assertEquals(CandidateTier.Unclassified, without.candidates[0].tier)
        assertFalse(without.candidates[0].tier.isShortlistDefault)

        // A candidate whose dual is unknown stays unclassified even when it is active.
        val withUnknown = assertIs<Ok<CandidateFunnelRanking, *, *>>(
            buildCandidateFunnel(report, sensitivity("x" to null))
        ).value!!
        assertEquals(CandidateTier.Unclassified, withUnknown.candidates[0].tier)
    }

    private fun perturbation(
        id: String,
        baseline: Double,
        points: List<Triple<Double, Double?, PerturbationOutcome>>
    ): ConstraintPerturbationReport {
        return ConstraintPerturbationReport(
            constraintId = ConstraintId(id),
            status = if (points.any { it.third == PerturbationOutcome.Unknown }) {
                AnalysisStatus.Unknown
            } else {
                AnalysisStatus.Reachable
            },
            baselineObjective = Flt64(baseline),
            observations = points.map { (delta, objective, outcome) ->
                ConstraintPerturbationObservation(
                    constraintId = ConstraintId(id),
                    kind = ConstraintPerturbationKind.Rhs,
                    delta = Flt64(delta),
                    newObjective = objective?.let { Flt64(it) },
                    objectiveImprovement = objective?.let { Flt64(it - baseline) },
                    status = if (outcome == PerturbationOutcome.Unknown) {
                        AnalysisStatus.Unknown
                    } else {
                        AnalysisStatus.Reachable
                    },
                    outcome = outcome
                )
            }
        )
    }

    @Test
    fun effectivenessRankingUsesOnlyProvenObservations() {
        val reports = listOf(
            perturbation("a", 10.0, listOf(Triple(1.0, 12.0, PerturbationOutcome.Effective))),
            // An Unknown observation must not contribute an improvement.
            perturbation("b", 10.0, listOf(Triple(1.0, 99.0, PerturbationOutcome.Unknown)))
        )
        val ranking = assertIs<Ok<EffectivenessRanking, *, *>>(
            buildEffectivenessRanking(Flt64(10.0), null, reports)
        ).value!!

        val a = ranking.entry(ConstraintId("a"))!!
        assertEquals(Flt64(2.0), a.maxImprovement)
        assertEquals(Flt64(1.0), a.firstEffectiveDelta)
        assertTrue(a.isGloballyEffective)

        val b = ranking.entry(ConstraintId("b"))!!
        assertNull(b.maxImprovement)
        assertNull(b.firstEffectiveDelta)
        assertFalse(b.isGloballyEffective)

        assertEquals(ConstraintId("a"), ranking.entries[0].constraintId)
        assertEquals(1, ranking.globallyEffective.size)
        assertTrue(ranking.validate() is Ok)
    }

    @Test
    fun effectivenessRankingNeverClaimsGlobalIrrelevance() {
        // A constraint that is neither locally effective nor removal-effective must not be
        // labelled irrelevant anywhere in the public structure.
        val reports = listOf(
            perturbation("quiet", 10.0, listOf(Triple(1.0, 10.0, PerturbationOutcome.NoObservedEffect)))
        )
        val ranking = assertIs<Ok<EffectivenessRanking, *, *>>(
            buildEffectivenessRanking(Flt64(10.0), null, reports)
        ).value!!
        val entry = ranking.entries.single()
        assertEquals(Flt64(0.0), entry.maxImprovement)
        assertFalse(entry.isGloballyEffective)
        assertTrue(ranking.globallyEffective.isEmpty())
        assertFalse(entry.toString().lowercase().contains("irrelevant"))
        assertFalse(entry.toString().lowercase().contains("ineffective"))
    }

    @Test
    fun effectivenessRankingIncludesAProvenRemovalObservation() {
        val report = ConstraintPerturbationReport(
            constraintId = ConstraintId("removed"),
            status = AnalysisStatus.Reachable,
            baselineObjective = Flt64(10.0),
            removal = ConstraintPerturbationObservation(
                constraintId = ConstraintId("removed"),
                kind = ConstraintPerturbationKind.Removal,
                status = AnalysisStatus.Reachable,
                baselineObjective = Flt64(10.0),
                newObjective = Flt64(13.0),
                objectiveImprovement = Flt64(3.0),
                outcome = PerturbationOutcome.Effective
            )
        )

        val ranking = assertIs<Ok<EffectivenessRanking, *, *>>(
            buildEffectivenessRanking(Flt64(10.0), null, listOf(report))
        ).value!!
        val entry = ranking.entry(ConstraintId("removed"))!!
        assertEquals(Flt64(3.0), entry.maxImprovement)
        assertTrue(entry.isGloballyEffective)
        assertEquals(true, entry.removalEffective)
    }

    @Test
    fun groupSummaryReportsInvolvedActiveAndBlockingInstances() {
        val report = activityReport(
            activity("c1", "Cargo Transfer Time", ActivityStatus.Active),
            activity("c2", "Cargo Transfer Time", ActivityStatus.Inactive),
            activity("c3", "Cargo Transfer Time", ActivityStatus.Active),
            activity("c4", "Airport Slot", ActivityStatus.Active)
        )
        val summaries = summarizeGroups(report, null, null)
        assertEquals(2, summaries.size)
        val cargo = summaries.single { it.group == "Cargo Transfer Time" }
        assertEquals(3, cargo.involvedInstances)
        assertEquals(2, cargo.activeInstances)
        assertEquals(0, cargo.blockingInstances)
        assertFalse(cargo.isBlocking)
    }

    @Test
    fun combinedStatusKeepsTheWeakestStage() {
        assertEquals(
            AnalysisStatus.Unknown,
            combineStatus(AnalysisStatus.Reachable, AnalysisStatus.Unknown)
        )
        assertEquals(
            AnalysisStatus.Unsupported,
            combineStatus(AnalysisStatus.Unknown, AnalysisStatus.Unsupported)
        )
        assertEquals(
            AnalysisStatus.Reachable,
            combineStatus(AnalysisStatus.Reachable, null)
        )
        assertEquals(
            AnalysisStatus.Unsupported,
            combineStatus(AnalysisStatus.Unsupported, AnalysisStatus.Reachable)
        )
    }

    @Test
    fun reportBuilderAggregatesStagesAndDegradesOnUnprovenBaseline() {
        val report = activityReport(
            activity("strong", "Capacity", ActivityStatus.Active),
            activity("loose", "Slot", ActivityStatus.Inactive)
        )
        val built = CriticalConstraintAnalysisReportBuilder()
            .baseline(
                AnalysisBaselineSummary(
                    objectiveId = "payload",
                    sense = "maximize",
                    objectiveValue = Flt64(10.0),
                    status = AnalysisStatus.Reachable,
                    provenOptimal = true
                )
            )
            .activity(report)
            .localSensitivity(sensitivity("strong" to Flt64(12.5), "loose" to Flt64(0.0)))
            .perturbation(listOf(perturbation("strong", 10.0, listOf(Triple(1.0, 12.0, PerturbationOutcome.Effective)))))
            .build()

        val result = assertIs<Ok<CriticalConstraintAnalysisReport, *, *>>(built).value!!
        assertEquals(2, result.funnel.candidates.size)
        assertEquals(1, result.funnel.tierA)
        assertEquals(1, result.funnel.tierC)
        assertEquals(1, result.effectiveness!!.globallyEffective.size)
        assertTrue(result.groupSummaries.any { it.group == "Capacity" && it.activeInstances == 1 })
        assertTrue(result.validate() is Ok)
        // No solver-generated artifact may appear in the public report.
        assertFalse(result.toString().contains("solver_row"))
        assertFalse(result.toString().contains("auxiliary_variable"))

        // An unproven baseline must not be reported as a proven overall conclusion.
        val unproven = assertIs<Ok<CriticalConstraintAnalysisReport, *, *>>(
            CriticalConstraintAnalysisReportBuilder()
                .baseline(
                    AnalysisBaselineSummary(
                        objectiveId = "payload",
                        sense = "maximize",
                        objectiveValue = Flt64(4.0),
                        status = AnalysisStatus.Unknown,
                        provenOptimal = false
                    )
                )
                .activity(report)
                .build()
        ).value!!
        assertEquals(AnalysisStatus.Unknown, unproven.status)
        assertFalse(unproven.status.isProven)

        val unavailable = assertIs<Ok<CriticalConstraintAnalysisReport, *, *>>(
            CriticalConstraintAnalysisReportBuilder()
                .baseline(
                    AnalysisBaselineSummary(
                        objectiveId = "payload",
                        sense = "maximize",
                        objectiveValue = Flt64(10.0),
                        status = AnalysisStatus.Reachable,
                        provenOptimal = true
                    )
                )
                .activity(report)
                .noteUnavailable("LP backend missing")
                .build()
        ).value!!
        assertEquals(AnalysisStatus.Unsupported, unavailable.status)
        assertTrue(unavailable.unavailableReasons.any { it.contains("LP backend missing") })
    }

    @Test
    fun activityReportSummaryCountsMatchRecords() {
        // Guards the invariant that a report's summary is derived from its own records.
        val report = activityReport(
            activity("a", "g", ActivityStatus.Active),
            activity("b", "g", ActivityStatus.Inactive)
        )
        assertEquals(2, report.totalConstraints)
        assertEquals(2, report.constraintActivities.size)
        assertEquals(0, report.variableActivities.size)
    }

    @Test
    fun pipelineInjectsProvidedLpCapabilityButHonorsExplicitUnsupported() = runBlocking {
        val model = ConstraintProgrammingModel("pipeline-lp-capability", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 1).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64.one).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val baseline = ConstraintProgrammingSolution(
                values = mapOf(VariableId("${x.identifier}:${x.index}") to Int64.one)
            )
            var backendCalls = 0
            val backend = FixedIntegerLpBackend { request ->
                ++backendCalls
                assertEquals(baseline.values, request.fixedValues)
                ok(
                    FixedIntegerLpSolveResult(
                        status = AnalysisStatus.Reachable,
                        objectiveValue = Flt64.one,
                        duals = mapOf(ConstraintId("capacity") to Flt64.one)
                    )
                )
            }
            val options = CriticalConstraintAnalysisOptions(
                runPerturbation = false,
                runTargetAnalysis = false
            )

            val injected = assertIs<Ok<CriticalConstraintAnalysisReport, *, *>>(
                CriticalConstraintAnalysisPipeline(
                    solver = FakeConstraintProgrammingSolver(),
                    lpBackend = backend,
                    capabilityMatrix = CapabilityMatrix()
                ).analyze(
                    snapshot = snapshot,
                    baselineSolution = baseline,
                    baselineObjective = Flt64.one,
                    baselineProvenOptimal = true,
                    objectiveId = "payload",
                    options = options
                )
            ).value!!
            assertEquals(AnalysisStatus.Reachable, injected.localSensitivity!!.status)
            assertEquals(1, backendCalls)

            val explicitlyUnsupported = assertIs<Ok<CriticalConstraintAnalysisReport, *, *>>(
                CriticalConstraintAnalysisPipeline(
                    solver = FakeConstraintProgrammingSolver(),
                    lpBackend = backend,
                    capabilityMatrix = CapabilityMatrix(
                        analysisCapabilities = mapOf(
                            AnalysisCapability.FixedIntegerLpSensitivity to CapabilitySupport.Unsupported
                        )
                    )
                ).analyze(
                    snapshot = snapshot,
                    baselineSolution = baseline,
                    baselineObjective = Flt64.one,
                    baselineProvenOptimal = true,
                    objectiveId = "payload",
                    options = options
                )
            ).value!!
            assertEquals(AnalysisStatus.Unsupported, explicitlyUnsupported.localSensitivity!!.status)
            assertEquals(1, backendCalls)
        } finally {
            model.close()
        }
    }
}
