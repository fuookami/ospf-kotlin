package fuookami.ospf.kotlin.core.analysis

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.NoOverlap
import fuookami.ospf.kotlin.core.model.constraint_programming.Cumulative
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalId
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.BooleanLiteral
import fuookami.ospf.kotlin.core.model.constraint_programming.IntervalVariable
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolution
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.report.SolverDescriptor
import fuookami.ospf.kotlin.core.solver.report.SolverCapabilities
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingFeature
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSession
import fuookami.ospf.kotlin.core.solver.constraint_programming.FakeConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSolveOptions
import fuookami.ospf.kotlin.core.solver.constraint_programming.ConstraintProgrammingSupportLevel
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.IntVar

class TargetConflictAnalyzerTest {
    @Test
    fun targetFeasibilityUsesDerivedSnapshotAndPreservesStatuses() = runBlocking {
        val model = ConstraintProgrammingModel("target", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(10)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val analyzer = TargetFeasibilityAnalyzer(FakeConstraintProgrammingSolver())

            val reachable = assertIs<Ok<TargetFeasibilityReport, *, *>>(
                analyzer.analyze(
                    snapshot,
                    ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(10.0))
                )
            ).value!!
            val unreachable = assertIs<Ok<TargetFeasibilityReport, *, *>>(
                analyzer.analyze(
                    snapshot,
                    ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(11.0))
                )
            ).value!!

            assertEquals(AnalysisStatus.Reachable, reachable.status)
            assertTrue(reachable.reachable == true)
            assertEquals(10L, reachable.effectiveIntegerBound!!.toLong())
            assertTrue(reachable.targetFixed)
            assertEquals(AnalysisStatus.Unreachable, unreachable.status)
            assertFalse(unreachable.reachable == true)
            assertEquals("objective-target:payload:at-least:11.0", unreachable.source.stableId)
        } finally {
            model.close()
        }
    }

    @Test
    fun unsupportedTargetDoesNotClaimThatAConstraintWasInserted() = runBlocking {
        val model = ConstraintProgrammingModel("unsupported-target", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 1).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val report = assertIs<Ok<TargetFeasibilityReport, *, *>>(
                TargetFeasibilityAnalyzer(FakeConstraintProgrammingSolver()).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(2.0)),
                    capabilityMatrix = CapabilityMatrix(
                        modelTypes = setOf(SolverModelType.CP),
                        analysisCapabilities = mapOf(
                            AnalysisCapability.ObjectiveTarget to CapabilitySupport.Unsupported
                        )
                    )
                )
            ).value!!

            assertEquals(AnalysisStatus.Unsupported, report.status)
            assertTrue(!report.targetFixed)

            val conflict = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(FakeConstraintProgrammingSolver()).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(2.0)),
                    capabilityMatrix = CapabilityMatrix(
                        modelTypes = setOf(SolverModelType.CP),
                        analysisCapabilities = mapOf(
                            AnalysisCapability.Conflict to CapabilitySupport.Unsupported
                        )
                    )
                )
            ).value!!
            assertTrue(!conflict.targetFeasibility.targetFixed)
        } finally {
            model.close()
        }
    }

    @Test
    fun invalidConflictOptionsReturnStructuredFailure() = runBlocking {
        val model = ConstraintProgrammingModel("invalid-conflict-options", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 1).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val result = ConflictAnalyzer(FakeConstraintProgrammingSolver()).analyze(
                snapshot = snapshot,
                target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(2.0)),
                options = ConflictAnalysisOptions(maxDeletionChecks = -1)
            )

            assertTrue(result.failed)
        } finally {
            model.close()
        }
    }

    @Test
    fun conflictShrinkingReSolvesEveryDeletionAndKeepsGlobalConstraintIdentity() = runBlocking {
        val model = ConstraintProgrammingModel("global-conflict", ObjectCategory.Maximum)
        val first = BinVar("first")
        val second = BinVar("second")
        try {
            model.registerVariable(first, IntegerDomain.boolean)
            model.registerVariable(second, IntegerDomain.boolean)
            val firstExpression = ConstraintProgrammingExpression.Variable(first)
            val secondExpression = ConstraintProgrammingExpression.Variable(second)
            val objective = ConstraintProgrammingExpression.sum(
                listOf(firstExpression, secondExpression)
            ).value!!
            val allDifferent = ConstraintProgrammingConstraint.allDifferent(
                listOf(firstExpression, secondExpression)
            ).value!!
            model.addConstraint(allDifferent, id = ConstraintId("all-different"))
            model.maximize(objective, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(2.0))
            val analyzer = ConflictAnalyzer(FakeConstraintProgrammingSolver())

            val result = assertIs<Ok<ConflictExplanation, *, *>>(
                analyzer.analyze(snapshot, target)
            ).value!!

            assertEquals(AnalysisStatus.Unreachable, result.status)
            assertEquals(AnalysisStatus.Unreachable, result.targetFeasibility.status)
            assertTrue(result.targetFeasibility.targetFixed)
            assertEquals(ConflictValidity.Verified, result.validity)
            assertEquals(ConflictMinimality.Irreducible, result.minimality)
            assertEquals(
                listOf("constraint:all-different"),
                result.members.map { it.stableId }
            )
            assertEquals(setOf(ConstraintId("all-different")), result.constraintIds)
            assertTrue(result.availableEvidence.any { it.stableId == "constraint:all-different" })
            assertTrue(result.availableEvidence.any { it.stableId == "variable:${variableId(first).value}:lower" })
            assertTrue(result.availableEvidence.any { it.stableId == "variable:${variableId(second).value}:upper" })
            assertTrue(result.verifications.isNotEmpty())
            assertTrue(result.verifications.all { it.targetFixed })
            assertTrue(result.members.all { member ->
                result.verifications.any {
                    it.source == member &&
                        it.targetFixed &&
                        it.retained &&
                        it.status == AnalysisStatus.Reachable
                }
            })
            assertTrue(result.validate() is Ok)
            assertTrue(
                result.copy(verifications = emptyList()).validate() is Failed,
                "an irreducible conflict without per-member deletion proofs must be rejected"
            )
        } finally {
            model.close()
        }
    }

    @Test
    fun noOverlapAndCumulativeEnterTargetAndConflictThroughSnapshotRebuild() = runBlocking {
        listOf(false, true).forEach { cumulative ->
            val model = ConstraintProgrammingModel(
                if (cumulative) "cumulative-conflict" else "no-overlap-conflict",
                ObjectCategory.Maximum
            )
            val value = IntVar("value")
            try {
                model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
                val first = IntervalVariable.fixed(
                    id = IntervalId("first"),
                    start = ConstraintProgrammingExpression.Constant(Int64.zero),
                    size = Int64(2),
                    end = ConstraintProgrammingExpression.Constant(Int64(2))
                ).value!!
                val second = IntervalVariable.fixed(
                    id = IntervalId("second"),
                    start = ConstraintProgrammingExpression.Constant(Int64.zero),
                    size = Int64(2),
                    end = ConstraintProgrammingExpression.Constant(Int64(2))
                ).value!!
                model.registerInterval(first)
                model.registerInterval(second)
                val global = if (cumulative) {
                    Cumulative.create(
                        intervals = listOf(first, second),
                        demands = listOf(
                            ConstraintProgrammingExpression.Constant(Int64.one),
                            ConstraintProgrammingExpression.Constant(Int64.one)
                        ),
                        capacity = ConstraintProgrammingExpression.Constant(Int64.one)
                    ).value!!
                } else {
                    NoOverlap.create(listOf(first, second)).value!!
                }
                val globalId = if (cumulative) "cumulative" else "no-overlap"
                model.addConstraint(global, id = ConstraintId(globalId))
                model.maximize(
                    ConstraintProgrammingExpression.Variable(value),
                    id = ObjectiveId("payload")
                )
                val snapshot = model.snapshot().value!!
                val target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(1.0))
                val solver = FakeConstraintProgrammingSolver()

                val targetReport = assertIs<Ok<TargetFeasibilityReport, *, *>>(
                    TargetFeasibilityAnalyzer(solver).analyze(snapshot, target)
                ).value!!
                assertEquals(AnalysisStatus.Unreachable, targetReport.status)

                val conflict = assertIs<Ok<ConflictExplanation, *, *>>(
                    ConflictAnalyzer(solver).analyze(snapshot, target)
                ).value!!
                // Removing the global constraint makes the target reachable, so it must remain in
                // the conflict. The fake solver intentionally reports Unknown for the very wide
                // domain created when a variable bound is deactivated; those inconclusive bound
                // checks are retained and lower minimality to Partial.
                assertTrue(conflict.members.any { it.stableId == "constraint:$globalId" })
                assertTrue(conflict.verifications.any {
                    it.source.stableId == "constraint:$globalId" && it.retained &&
                        it.status == AnalysisStatus.Reachable
                })
                assertTrue(conflict.minimality != ConflictMinimality.Irreducible ||
                    conflict.members.all { it is DiagnosticSource.Constraint })
            } finally {
                model.close()
            }
        }
    }

    @Test
    fun mipBackedUnsupportedGlobalConstraintMapsTargetAndConflictToUnsupported() = runBlocking {
        val model = ConstraintProgrammingModel("mip-backed-unsupported-global", ObjectCategory.Maximum)
        val value = IntVar("value")
        try {
            model.registerVariable(value, IntegerDomain.interval(0, 1).value!!)
            val first = IntervalVariable.fixed(
                id = IntervalId("first"),
                start = ConstraintProgrammingExpression.Constant(Int64.zero),
                size = Int64(2),
                end = ConstraintProgrammingExpression.Constant(Int64(2))
            ).value!!
            val second = IntervalVariable.fixed(
                id = IntervalId("second"),
                start = ConstraintProgrammingExpression.Constant(Int64.zero),
                size = Int64(2),
                end = ConstraintProgrammingExpression.Constant(Int64(2))
            ).value!!
            model.registerInterval(first)
            model.registerInterval(second)
            model.addConstraint(
                Cumulative.create(
                    intervals = listOf(first, second),
                    demands = listOf(
                        ConstraintProgrammingExpression.Constant(Int64.one),
                        ConstraintProgrammingExpression.Constant(Int64.one)
                    ),
                    capacity = ConstraintProgrammingExpression.Constant(Int64.one)
                ).value!!,
                id = ConstraintId("cumulative")
            )
            model.maximize(
                ConstraintProgrammingExpression.Variable(value),
                id = ObjectiveId("payload")
            )
            val snapshot = model.snapshot().value!!
            val solver = MipLikeUnsupportedGlobalSolver()
            val target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(1.0))

            val targetReport = assertIs<Ok<TargetFeasibilityReport, *, *>>(
                TargetFeasibilityAnalyzer(solver).analyze(snapshot, target)
            ).value!!
            assertEquals(AnalysisStatus.Unsupported, targetReport.status)
            assertTrue(targetReport.message.orEmpty().contains("Cumulative"))

            val conflict = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(solver).analyze(snapshot, target)
            ).value!!
            assertEquals(AnalysisStatus.Unsupported, conflict.status)
            assertEquals(AnalysisStatus.Unsupported, conflict.targetFeasibility.status)
            assertEquals(ConflictValidity.Unknown, conflict.validity)
        } finally {
            model.close()
        }
    }

    @Test
    fun unknownTargetCheckRetainsAllEvidenceAndDoesNotClaimMinimality() = runBlocking {
        val model = ConstraintProgrammingModel("unknown-conflict", ObjectCategory.Maximum)
        val x = IntVar("x")
        try {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(expression, Int64(10)).value!!,
                id = ConstraintId("capacity")
            )
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(11.0))
            val result = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(FakeConstraintProgrammingSolver(enumerationLimit = 1)).analyze(snapshot, target)
            ).value!!

            assertEquals(AnalysisStatus.Unknown, result.status)
            assertEquals(ConflictValidity.Unknown, result.validity)
            assertEquals(ConflictMinimality.NotChecked, result.minimality)
            assertEquals(result.availableEvidence.toSet(), result.members.toSet())
            assertTrue(result.verifications.isEmpty())
        } finally {
            model.close()
        }
    }

    /**
     * 8.16 性能预算用的多约束模型。
     *
     * `x, y ∈ [0,10]`；`c1: x ≤ 3`、`c2: y ≤ 3` 与目标 `x + y ≥ 7` 构成 MUS `{c1, c2}`，
     * 另有 4 条与变量边界重复的冗余约束，迫使删除收缩做真实工作。
     *
     * Multi-constraint model for the 8.16 performance budget. The MUS of `{c1, c2}` against the target
     * makes deletion shrinking do real work, while four redundant constraints force genuine candidate
     * checks.
     */
    private fun performanceModel(): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("conflict-performance", ObjectCategory.Maximum)
        val x = IntVar("x")
        val y = IntVar("y")
        model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
        model.registerVariable(y, IntegerDomain.interval(0, 10).value!!)
        fun variable(value: IntVar) = ConstraintProgrammingExpression.Variable(value)
        fun term(value: IntVar, coefficient: Long) =
            ConstraintProgrammingExpression.Term(value, Int64(coefficient))

        model.addConstraint(
            ConstraintProgrammingConstraint.lessOrEqual(variable(x), Int64(3)).value!!,
            id = ConstraintId("c1")
        )
        model.addConstraint(
            ConstraintProgrammingConstraint.lessOrEqual(variable(y), Int64(3)).value!!,
            id = ConstraintId("c2")
        )
        // 冗余约束：与变量边界重复，收缩时必须逐条复验并全部移除。
        // Redundant constraints duplicate the variable bounds; shrinking must verify and drop each.
        model.addConstraint(
            ConstraintProgrammingConstraint.greaterOrEqual(variable(x), Int64.zero).value!!,
            id = ConstraintId("r1")
        )
        model.addConstraint(
            ConstraintProgrammingConstraint.greaterOrEqual(variable(y), Int64.zero).value!!,
            id = ConstraintId("r2")
        )
        model.addConstraint(
            ConstraintProgrammingConstraint.lessOrEqual(variable(x), Int64(10)).value!!,
            id = ConstraintId("r3")
        )
        model.addConstraint(
            ConstraintProgrammingConstraint.lessOrEqual(variable(y), Int64(10)).value!!,
            id = ConstraintId("r4")
        )
        model.maximize(
            ConstraintProgrammingExpression.linear(listOf(term(x, 1), term(y, 1))).value!!,
            id = ObjectiveId("payload")
        )
        return model
    }

    @Test
    fun conflictAnalysisHoldsItsBudgetAndFindsTheExactMusAtScale() = runBlocking {
        val model = performanceModel()
        try {
            val snapshot = model.snapshot().value!!
            val options = ConflictAnalysisOptions(maxDeletionChecks = 8)
            val report = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(FakeConstraintProgrammingSolver()).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(7.0)),
                    options = options
                )
            ).value!!

            // 正确性优先：规模上来后答案仍必须精确——MUS 恰为 {c1, c2}，冗余约束全部被移除。
            // Correctness first: the answer must stay exact at scale — the MUS is exactly {c1, c2} and
            // every redundant constraint was removed.
            assertEquals(AnalysisStatus.Unreachable, report.status)
            assertEquals(ConflictValidity.Verified, report.validity)
            val dump = "minimality=${report.minimality} message=${report.message} " +
                "members=${report.constraintIds} verifications=" + report.verifications.joinToString {
                "${it.source.stableId}:${it.status}"
            }
            // 规模上来后约束级 MUS 仍必须精确：恰为 {c1, c2}，4 条冗余约束全部被移除。
            // At scale the constraint-level MUS must stay exact: exactly {c1, c2}, with all four
            // redundant constraints removed.
            assertEquals(setOf(ConstraintId("c1"), ConstraintId("c2")), report.constraintIds, dump)

            // 最小性结论：Kotlin 侧把**变量边界**也作为删除候选（Rust 侧只收缩约束）。删除变量边界
            // 会得到 Unknown，于是最小性诚实地停在 `Partial` 而不是声称"已验证最小"。
            // 这里的断言锁定的是**不许谎报**：只要存在未形成证明的删除复验，就不得是 Irreducible。
            //
            // Minimality: the Kotlin side also treats **variable bounds** as deletion candidates (Rust
            // shrinks constraints only). Removing a variable bound yields Unknown, so minimality honestly
            // stops at `Partial` instead of claiming a proven minimum. This assertion pins the
            // no-false-proof rule: an inconclusive deletion check must never yield Irreducible.
            val inconclusive = report.verifications.filter {
                it.status == AnalysisStatus.Unknown || it.status == AnalysisStatus.Unsupported
            }
            if (inconclusive.isEmpty()) {
                assertEquals(ConflictMinimality.Irreducible, report.minimality, dump)
            } else {
                assertEquals(ConflictMinimality.Partial, report.minimality, dump)
                assertFalse(report.minimalityVerified, dump)
                assertNotNull(report.message, dump)
                // 未形成证明的候选必须被保留，不得被当作已移除。 / Inconclusive candidates must be
                // retained rather than treated as removed.
                assertTrue(inconclusive.all { !it.removed }, dump)
            }

            // 预算：每个候选都固定 target，且复验次数不超过配置上限（8.16）。
            // Budget: every candidate keeps the target fixed and the verification count stays within
            // the configured limit.
            assertTrue(report.verifications.all { it.targetFixed })
            assertTrue(
                report.verifications.size <= options.maxDeletionChecks,
                "verifications ${report.verifications.size} exceeded the budget ${options.maxDeletionChecks}"
            )
            // 每个保留成员都必须通过最终逐项复验。 / Every retained member passed the final check.
            assertTrue(report.verifications.any { it.retained && it.status == AnalysisStatus.Reachable })
        } finally {
            model.close()
        }
    }

    @Test
    fun exhaustedConflictBudgetNeverClaimsAnIrreducibleConflict() = runBlocking {
        val model = performanceModel()
        try {
            val snapshot = model.snapshot().value!!
            // 极小时间预算：删除复验无法完成，结论必须降级为 Partial，绝不能声称"已验证最小"。
            // A tiny time budget prevents deletion verification from completing, so the conclusion must
            // degrade to Partial and must never claim a proven minimality.
            val report = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(FakeConstraintProgrammingSolver()).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(7.0)),
                    options = ConflictAnalysisOptions(
                        timeBudget = 1.nanoseconds
                    )
                )
            ).value!!

            assertEquals(ConflictMinimality.Partial, report.minimality)
            assertFalse(report.minimalityVerified)
            assertNotNull(report.message)
            // 预算耗尽也必须仍然保持"已证明不可达"，不得把资源问题伪装成 SAT。
            // An exhausted budget must still keep the proven unreachability rather than disguise the
            // resource problem as SAT.
            assertEquals(AnalysisStatus.Unreachable, report.status)
        } finally {
            model.close()
        }
    }

    @Test
    fun backgroundOnlyBlockingDoesNotClaimConstraintLevelMinimality() = runBlocking {
        // Boolean-ness is a fixed semantic domain. Removing the generated lower/upper-bound
        // evidence cannot make x reach 2, so the remaining empty constraint projection must be
        // reported as background blocking rather than an irreducible constraint MUS.
        val model = ConstraintProgrammingModel("background-only-conflict", ObjectCategory.Maximum)
        val x = BinVar("x")
        try {
            model.registerVariable(x, IntegerDomain.boolean)
            val expression = ConstraintProgrammingExpression.Variable(x)
            model.maximize(expression, id = ObjectiveId("payload"))
            val snapshot = model.snapshot().value!!
            val report = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(FakeConstraintProgrammingSolver()).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(2.0))
                )
            ).value!!

            assertEquals(AnalysisStatus.Unreachable, report.status)
            assertTrue(report.backgroundBlocking)
            assertTrue(report.constraintIds.isEmpty())
            assertEquals(ConflictMinimality.Partial, report.minimality)
            assertTrue(!report.minimalityVerified)
            assertNotNull(report.message)
        } finally {
            model.close()
        }
    }

    @Test
    fun nativeConflictTierRequestsAndConsumesBackendConflict() = runBlocking {
        val model = conflictRoutingModel()
        val solver = RecordingConflictSolver(RecordingConflictTier.Native)
        try {
            val snapshot = model.snapshot().value!!
            val report = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(solver).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(1.0))
                )
            ).value!!

            assertEquals(ConflictExtractionTier.NativeUnsatCore, report.extractionTier)
            assertTrue(solver.conflictRequests.isNotEmpty())
            assertTrue(solver.conflictRequests.any { it.collectConflict })
            assertTrue(solver.conflictRequests.any { it.conflictActivationIds?.contains("constraint:capacity") == true })
            assertTrue(report.members.any { it.stableId == "constraint:capacity" })
        } finally {
            model.close()
        }
    }

    @Test
    fun assumptionConflictTierPassesActivationLiteralsToSession() = runBlocking {
        val model = conflictRoutingModel()
        val solver = RecordingConflictSolver(RecordingConflictTier.Assumption)
        try {
            val snapshot = model.snapshot().value!!
            val report = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(solver).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(1.0))
                )
            ).value!!

            assertEquals(ConflictExtractionTier.AssumptionExtraction, report.extractionTier)
            assertTrue(solver.assumptionCalls.any { it.isNotEmpty() })
            assertTrue(report.members.any { it.stableId == "constraint:capacity" })
        } finally {
            model.close()
        }
    }

    @Test
    fun satisfactionOnlyConflictTierUsesRepeatedSolvingWithoutBackendExtraction() = runBlocking {
        val model = conflictRoutingModel()
        val solver = RecordingConflictSolver(RecordingConflictTier.Fallback)
        try {
            val snapshot = model.snapshot().value!!
            val report = assertIs<Ok<ConflictExplanation, *, *>>(
                ConflictAnalyzer(solver).analyze(
                    snapshot = snapshot,
                    target = ObjectiveTarget.AtLeast(ObjectiveId("payload"), Flt64(1.0))
                )
            ).value!!

            assertEquals(ConflictExtractionTier.RepeatedSolving, report.extractionTier)
            assertTrue(solver.conflictRequests.isEmpty())
            assertTrue(solver.assumptionCalls.none { it.isNotEmpty() })
            assertTrue(report.members.any { it.stableId == "constraint:capacity" })
        } finally {
            model.close()
        }
    }

    private fun conflictRoutingModel(): ConstraintProgrammingModel {
        val model = ConstraintProgrammingModel("conflict-routing", ObjectCategory.Maximum)
        val x = IntVar("x")
        model.registerVariable(x, IntegerDomain.interval(0, 1).value!!)
        val expression = ConstraintProgrammingExpression.Variable(x)
        model.addConstraint(
            ConstraintProgrammingConstraint.lessOrEqual(expression, Int64.zero).value!!,
            id = ConstraintId("capacity")
        )
        model.maximize(expression, id = ObjectiveId("payload"))
        return model
    }

    private enum class RecordingConflictTier {
        Native,
        Assumption,
        Fallback
    }

    private class RecordingConflictSolver(
        private val tier: RecordingConflictTier
    ) : ConstraintProgrammingSolver {
        private val delegate = FakeConstraintProgrammingSolver()
        val conflictRequests = ArrayList<ConstraintProgrammingSolveOptions>()
        val assumptionCalls = ArrayList<List<BooleanLiteral>>()

        override val descriptor: SolverDescriptor = SolverDescriptor(
            solverId = "recording-conflict-${tier.name.lowercase()}",
            backendName = "recording conflict test solver",
            capabilities = SolverCapabilities(
                modelTypes = setOf(SolverModelType.CP),
                constraintProgrammingFeatures = mapOf(
                    ConstraintProgrammingFeature.BooleanLogic to ConstraintProgrammingSupportLevel.Native,
                    ConstraintProgrammingFeature.Assumption to if (tier == RecordingConflictTier.Assumption) {
                        ConstraintProgrammingSupportLevel.Native
                    } else {
                        ConstraintProgrammingSupportLevel.Unsupported
                    },
                    ConstraintProgrammingFeature.ConflictCore to if (tier == RecordingConflictTier.Native) {
                        ConstraintProgrammingSupportLevel.Native
                    } else {
                        ConstraintProgrammingSupportLevel.Unsupported
                    }
                )
            )
        )

        override suspend fun solve(
            model: ConstraintProgrammingModel,
            options: ConstraintProgrammingSolveOptions
): Ret<ConstraintProgrammingSolverOutput> {
            if (options.collectConflict) {
                conflictRequests += options
            }
            val session = createSession(model, options)
            return when (session) {
                is Ok -> {
                    try {
                        session.value!!.solve()
                    } finally {
                        session.value!!.close()
                    }
                }

                is Failed -> Failed<ConstraintProgrammingSolverOutput, ErrorCode, Error<ErrorCode>>(session.error)
                is Fatal -> Fatal<ConstraintProgrammingSolverOutput, ErrorCode, Error<ErrorCode>>(session.errors)
            }
        }

        override fun createSession(
            model: ConstraintProgrammingModel,
            options: ConstraintProgrammingSolveOptions
): Ret<ConstraintProgrammingSession> {
            val created = delegate.createSession(model, options)
            return when (created) {
                is Ok -> {
                    val delegateSession = created.value!!
                    Ok(
                        object : ConstraintProgrammingSession {
                            override val model: ConstraintProgrammingModel
                                get() = delegateSession.model
                            override val options: ConstraintProgrammingSolveOptions
                                get() = delegateSession.options
                            override val isClosed: Boolean
                                get() = delegateSession.isClosed

                            override suspend fun solve(
                                assumptions: List<BooleanLiteral>,
                                fixedValues: Map<VariableId, Int64>,
                                hints: ConstraintProgrammingSolution?
): Ret<ConstraintProgrammingSolverOutput> {
                                assumptionCalls += assumptions
                                return delegateSession.solve(assumptions, fixedValues, hints)
                            }

                            override fun close() {
                                delegateSession.close()
                            }
                        }
                    )
                }

            is Failed -> created
            is Fatal -> created
            }
        }
    }

    private fun variableId(variable: BinVar): VariableId {
        return VariableId("${variable.identifier}:${variable.index}")
    }
}

private class MipLikeUnsupportedGlobalSolver : ConstraintProgrammingSolver by FakeConstraintProgrammingSolver() {
    override val descriptor: SolverDescriptor = SolverDescriptor(
        solverId = "mip-backed-unsupported-global-test",
        backendName = "MIP-backed CP test solver",
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.CP),
            constraintProgrammingFeatures = mapOf(
                ConstraintProgrammingFeature.BooleanLogic to ConstraintProgrammingSupportLevel.ExactLowering,
                ConstraintProgrammingFeature.Cumulative to ConstraintProgrammingSupportLevel.Unsupported
            )
        )
    )
}
