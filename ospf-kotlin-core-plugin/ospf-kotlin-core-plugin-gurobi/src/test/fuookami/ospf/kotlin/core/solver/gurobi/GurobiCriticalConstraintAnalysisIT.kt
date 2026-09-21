package fuookami.ospf.kotlin.core.solver.gurobi

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.constraint_programming.IntegerDomain
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModel
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingConstraint
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingExpression
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingSolverOutput
import fuookami.ospf.kotlin.core.solver.output.ConstraintProgrammingFeasibleOutput
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.SolverModelType
import fuookami.ospf.kotlin.core.solver.constraint_programming.MipBackedConstraintProgrammingSolver
import fuookami.ospf.kotlin.core.analysis.AnalysisStatus
import fuookami.ospf.kotlin.core.analysis.ObjectiveTarget
import fuookami.ospf.kotlin.core.analysis.CapabilityMatrix
import fuookami.ospf.kotlin.core.analysis.ConflictValidity
import fuookami.ospf.kotlin.core.analysis.CriticalityProfile
import fuookami.ospf.kotlin.core.analysis.PerturbationOutcome
import fuookami.ospf.kotlin.core.analysis.LocalSensitivityScope
import fuookami.ospf.kotlin.core.analysis.analyzeCriticalityTargets
import fuookami.ospf.kotlin.core.analysis.ConstraintPerturbationPolicy
import fuookami.ospf.kotlin.core.analysis.ConstraintPerturbationReport
import fuookami.ospf.kotlin.core.analysis.ConstraintPerturbationAnalyzer
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSensitivityReport
import fuookami.ospf.kotlin.core.analysis.CriticalityProfileAnalysisResult
import fuookami.ospf.kotlin.core.analysis.CriticalConstraintAnalysisOptions
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSensitivityAnalyzer
import fuookami.ospf.kotlin.core.variable.IntVar

/**
 * 使用真实 Gurobi 验证固定整数 LP 局部有效性与 RHS 敏感性范围。
 *
 * 模型（人工可算）：
 * ```
 * max  3x + 2y
 * s.t. x + y   <= 4      (c1)
 *      x + 3y  <= 6      (c2)
 *      x, y ∈ [0, 10] 整数
 * ```
 * 最优解 `(4, 0)`，目标值 `12`。
 *
 * 独立推导的基准真值：
 * - 顶点 `(0,0)=0`、`(4,0)=12`、`(0,2)=6`、`(3,1)=11`，故最优为 `(4,0)`、`z*=12`；
 * - 只有 `c1` 在最优处紧且对偶可行，因此 `c1` 的对偶值为 `3`（等于 `x` 的目标系数，因为 `x`
 *   完全由 `c1` 定界）；
 * - `c1` 的 RHS 有效区间：下界受 `x >= 0` 限制为 `0`；上界受 `c2` 变为紧的限制，
 *   在 `y = 0` 时 `x <= 6`，故为 `6`。即 `[0, 6]`。
 *
 * Verifies fixed-integer LP local effectiveness and RHS sensitivity ranges with real Gurobi. The
 * model is hand-computable; the ground truth above is derived independently of the implementation.
 */
class GurobiCriticalConstraintAnalysisIT {
    private fun capability(): CapabilityMatrix {
        return CapabilityMatrix(
            modelTypes = setOf(SolverModelType.MIP),
            dual = true
        )
    }

    private class Fixture(
        private val firstCapacity: Long = 4,
        private val secondCapacity: Long = 6
    ) : AutoCloseable {
        val model = ConstraintProgrammingModel("gurobi-fixed-integer-lp", ObjectCategory.Maximum)
        val x = IntVar("x")
        val y = IntVar("y")

        init {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            model.registerVariable(y, IntegerDomain.interval(0, 10).value!!)
            val capacity = ConstraintProgrammingExpression.linear(
                listOf(
                    ConstraintProgrammingExpression.Term(x, Int64.one),
                    ConstraintProgrammingExpression.Term(y, Int64.one)
                )
            ).value!!
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(capacity, Int64(firstCapacity)).value!!,
                id = ConstraintId("c1")
            )
            val second = ConstraintProgrammingExpression.linear(
                listOf(
                    ConstraintProgrammingExpression.Term(x, Int64.one),
                    ConstraintProgrammingExpression.Term(y, Int64(3))
                )
            ).value!!
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(second, Int64(secondCapacity)).value!!,
                id = ConstraintId("c2")
            )
            val profit = ConstraintProgrammingExpression.linear(
                listOf(
                    ConstraintProgrammingExpression.Term(x, Int64(3)),
                    ConstraintProgrammingExpression.Term(y, Int64(2))
                )
            ).value!!
            model.maximize(profit, id = ObjectiveId("profit"))
        }

        override fun close() {
            model.close()
        }
    }

    @Test
    fun gurobiFixedIntegerLpProducesHandVerifiedDualAndSensitivityRange() = runBlocking {
        val fixture = Fixture()
        val solver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
        try {
            // 先求解再取 snapshot：取 snapshot 不应成为求解的前提。
            // Solve first, snapshot afterwards: snapshotting must not be a precondition for solving.
            val baselineResult = solver.solve(fixture.model)
            val baseline = assertIs<Ok<ConstraintProgrammingSolverOutput, *, *>>(
                baselineResult,
                (baselineResult as? Failed<*, *, *>)?.error?.message
                    ?: baselineResult.toString()
            ).value!!
            val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(baseline)
            val snapshot = fixture.model.snapshot().value!!

            // 基线必须与独立推导的最优解一致，否则后续断言失去意义。
            // The baseline must match the independently derived optimum, otherwise the following
            // assertions are meaningless.
            assertEquals(12.0, feasible.objective!!.toDouble(), 1e-6)
            assertEquals(4L, feasible.solution.values[fixture.x.let {
                VariableId("${it.identifier}:${it.index}")
            }]!!.toLong())

            val report = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                FixedIntegerLpSensitivityAnalyzer(gurobiFixedIntegerLpBackend()).analyze(
                    snapshot = snapshot,
                    solution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability()
                )
            ).value!!

            assertEquals(AnalysisStatus.Reachable, report.status)
            // 全部对偶值必须明确标注为固定整数 incumbent 作用域。
            // Every dual value must be explicitly scoped to the fixed-integer incumbent.
            assertEquals(LocalSensitivityScope.FixedIntegerIncumbent, report.scope)

            // 局部对偶：这里断言的是**该后端在退化顶点上返回的对偶解**，而不是影子价格。
            //
            // 关键区别（实测得出，见 Gap List G17）：本阶段的整数被 lowerer 以**等式行**固定，
            // 因此 `x + y ≤ 4` 在 `x = 4, y = 0` 处虽然紧，但把 RHS 从 4 提到 5 **不会改变目标值**
            // （整数已被钉死）——真实单侧边际是 0，而不是 3。`3.0` 是该退化最优面上一个合法的对偶解
            // （对偶可行 + 互补松弛成立），但**不可解读为 ∂z*/∂b**。
            //
            // 该断言的价值在于"锁定后端行为可复现"，不在于"证明 3 是正确影子价格"；报告本身也已把
            // 作用域标注为 `FixedIntegerIncumbent`，未声称全局影子价格。
            //
            // Local dual: this asserts **the dual solution this backend returns at a degenerate vertex**,
            // not a shadow price. The integers are pinned by the lowerer via **equality rows**, so while
            // `x + y ≤ 4` is tight at `x = 4, y = 0`, raising its RHS from 4 to 5 **does not change the
            // objective** (the integer pattern is fixed) — the true one-sided marginal is 0, not 3. The
            // value 3.0 is a legal dual solution on that degenerate optimal face (dual feasible with
            // complementary slackness) but must **not** be read as ∂z*/∂b. This assertion pins
            // reproducible backend behaviour, not the correctness of 3 as a shadow price; the report
            // itself already scopes the value to `FixedIntegerIncumbent`.
            val c1 = assertNotNull(report.constraint(ConstraintId("c1")), "c1 sensitivity must exist")
            val dual = assertNotNull(c1.dualValue, "c1 dual must be available")
            assertTrue(dual.toDouble().isFinite(), "c1 dual must be a finite dual solution")
            assertEquals(3.0, dual.toDouble(), 1e-6)
            assertTrue(c1.localEffective == true)
            assertEquals(LocalSensitivityScope.FixedIntegerIncumbent, c1.scope)

            // RHS 敏感性范围：对照**固定整数 LP**（而不是原始 MILP）的基准真值。
            //
            // 关键区别：该阶段所有整数变量都被固定（x=4, y=0），LP 退化为单点。此时
            //   c1: x + y ≤ r 在 x=4 下要求 r ≥ 4，否则不可行 → 有效范围 [4, ∞)；
            //   c2: x + 3y ≤ r 在 x=4,y=0 下同样要求 r ≥ 4 → [4, ∞)。
            // 因此 c1 的下界是 4 而不是原始 MILP 上的 0——两者是不同模型上的不同量。
            //
            // RHS sensitivity ranges checked against **fixed-integer LP** ground truth rather than the
            // original MILP. At this stage every integer variable is pinned (x=4, y=0), collapsing the LP
            // to a single point: c1 requires r ≥ 4 for feasibility and c2 likewise, so both ranges are
            // [4, ∞). c1's lower bound is therefore 4, not the 0 that holds for the original MILP — the
            // two are different quantities on different models.
            val range = assertNotNull(c1.sensitivityRange, "c1 sensitivity range must be available")
            assertEquals(4.0, assertNotNull(range.lower, "c1 range lower").toDouble(), 1e-6)
            assertTrue(range.upper == null, "c1 range must be unbounded above")

            // c2 在最优处虽紧但不在基中（对偶不可行），不得被冒充为有效约束。
            // c2 is tight at the optimum but not in the basis (dual infeasible) and must not be
            // advertised as effective.
            val c2 = assertNotNull(report.constraint(ConstraintId("c2")))
            val c2Dual = assertNotNull(c2.dualValue, "c2 dual must be available")
            assertEquals(0.0, c2Dual.toDouble(), 1e-6)
            assertTrue(c2.localEffective != true, "c2 must not be reported as locally effective")

            val c2Range = assertNotNull(c2.sensitivityRange, "c2 sensitivity range must be available")
            assertEquals(4.0, assertNotNull(c2Range.lower, "c2 range lower").toDouble(), 1e-6)
            assertTrue(c2Range.upper == null, "c2 range must be unbounded above")
        } finally {
            fixture.close()
        }
    }

    /**
     * 用真实 Gurobi 验证 **Phase 3 扰动路径**——这才是纯整数 CP 模型上局部有效性的适用证据。
     *
     * 固定整数 LP 对偶在纯整数模型上是退化对偶（见 Gap List G17），因此"放宽哪条约束能改善目标"
     * 必须由 RHS 扰动回答。该测试给出可人工推导的对照：
     *
     * - `c1`（`x + y ≤ 4`）放宽 1 单位 → `x + y ≤ 5`，整数最优从 `(4,0)=12` 变为 `(5,0)=15`，
     *   改善 `+3` → 必须判为 `Effective`；
     * - `c2`（`x + 3y ≤ 6`）放宽 1 单位 → `x + 3y ≤ 7`，`x + y ≤ 4` 仍把 `x` 限在 4，
     *   最优不变 → 必须判为 `NoObservedEffect`。
     *
     * Verifies the **Phase 3 perturbation path** with real Gurobi — the applicable evidence for local
     * effectiveness on a pure-integer CP model, since the fixed-integer LP dual is degenerate there
     * (Gap List G17). The hand-derivable contrast: relaxing `c1` by one unit moves the integer optimum
     * from `(4,0)=12` to `(5,0)=15` (improvement `+3`, must be `Effective`), while relaxing `c2` by one
     * unit leaves the optimum unchanged because `x + y ≤ 4` still caps `x` at 4 (must be
     * `NoObservedEffect`).
     */
    @Test
    fun gurobiPerturbationDistinguishesTheEffectiveConstraintFromTheRedundantOne() = runBlocking {
        val fixture = Fixture()
        val solver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
        try {
            val baselineResult = solver.solve(fixture.model)
            val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<ConstraintProgrammingSolverOutput, *, *>>(
                    baselineResult,
                    (baselineResult as? Failed<*, *, *>)?.error?.message
                        ?: baselineResult.toString()
                ).value!!
            )
            val snapshot = fixture.model.snapshot().value!!
            assertEquals(12.0, feasible.objective!!.toDouble(), 1e-6)

            val analyzer = ConstraintPerturbationAnalyzer(gurobiPerturbationBackend())
            val policy = ConstraintPerturbationPolicy(deltas = listOf(Flt64(1.0)))

            val c1 = assertIs<Ok<ConstraintPerturbationReport, *, *>>(
                analyzer.analyze(
                    snapshot = snapshot,
                    constraintId = ConstraintId("c1"),
                    baselineSolution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability(),
                    policy = policy
                )
            ).value!!
            val c1Observation = assertNotNull(c1.observations.singleOrNull(), "c1 must produce one observation")
            // 放宽 c1 一定改善目标。 / Relaxing c1 must improve the objective.
            assertEquals(PerturbationOutcome.Effective, c1Observation.outcome)
            assertEquals(3.0, assertNotNull(c1Observation.objectiveImprovement).toDouble(), 1e-6)
            assertTrue(c1.globallyEffective)

            val c2 = assertIs<Ok<ConstraintPerturbationReport, *, *>>(
                analyzer.analyze(
                    snapshot = snapshot,
                    constraintId = ConstraintId("c2"),
                    baselineSolution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability(),
                    policy = policy
                )
            ).value!!
            val c2Observation = assertNotNull(c2.observations.singleOrNull(), "c2 must produce one observation")
            // 放宽 c2 对目标无影响：`x + y ≤ 4` 仍然限制 x = 4。
            // Relaxing c2 changes nothing: `x + y ≤ 4` still caps x at 4.
            assertEquals(PerturbationOutcome.NoObservedEffect, c2Observation.outcome)
            assertEquals(0.0, assertNotNull(c2Observation.objectiveImprovement).toDouble(), 1e-6)
            assertFalse(c2.globallyEffective)

            // 两条报告都只暴露原始约束身份。 / Both reports expose original constraint identities only.
            assertEquals(ConstraintId("c1"), c1.constraintId)
            assertEquals(ConstraintId("c2"), c2.constraintId)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun gurobiFixedIntegerLpDoesNotExposeLoweringArtifacts() = runBlocking {
        val fixture = Fixture()
        val solver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
        try {
            val baselineResult = solver.solve(fixture.model)
            val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<ConstraintProgrammingSolverOutput, *, *>>(
                    baselineResult,
                    (baselineResult as? Failed<*, *, *>)?.error?.message
                        ?: baselineResult.toString()
                ).value!!
            )
            val snapshot = fixture.model.snapshot().value!!
            val report = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                FixedIntegerLpSensitivityAnalyzer(gurobiFixedIntegerLpBackend()).analyze(
                    snapshot = snapshot,
                    solution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability()
                )
            ).value!!

            // 公开报告只能按原始约束身份编索引，且只包含原始约束。
            // The public report is keyed by original constraint identity only and contains only
            // original constraints.
            val originalIds = snapshot.constraints.map { it.id }.toSet()
            assertEquals(originalIds, report.sensitivities.map { it.constraintId }.toSet())
            report.sensitivities.forEach { sensitivity ->
                val id = sensitivity.constraintId.value
                assertTrue(id in originalIds.map { it.value }, "unexpected identity: $id")
                assertTrue(!id.contains("row"), "lowering row leaked: $id")
                assertTrue(!id.contains("aux"), "auxiliary element leaked: $id")
            }
        } finally {
            fixture.close()

        }
    }

    @Test
    fun gurobiFixedIntegerLpBackendDoesNotReuseSensitivityRangesAcrossCalls() = runBlocking {
        val firstFixture = Fixture()
        val secondFixture = Fixture(firstCapacity = 3)
        val solver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
        val analyzer = FixedIntegerLpSensitivityAnalyzer(gurobiFixedIntegerLpBackend())

        suspend fun report(fixture: Fixture): FixedIntegerLpSensitivityReport {
            val baselineResult = solver.solve(fixture.model)
            val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<ConstraintProgrammingSolverOutput, *, *>>(
                    baselineResult,
                    (baselineResult as? Failed<*, *, *>)?.error?.message
                        ?: baselineResult.toString()
                ).value!!
            )
            val snapshot = fixture.model.snapshot().value!!
            return assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                analyzer.analyze(
                    snapshot = snapshot,
                    solution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability()
                )
            ).value!!
        }

        try {
            val first = report(firstFixture)
            val second = report(secondFixture)

            assertEquals(4.0, assertNotNull(first.constraint(ConstraintId("c1"))!!.sensitivityRange).lower!!.toDouble(), 1e-6)
            assertEquals(3.0, assertNotNull(second.constraint(ConstraintId("c1"))!!.sensitivityRange).lower!!.toDouble(), 1e-6)
        } finally {
            firstFixture.close()
            secondFixture.close()
        }
    }

    @Test
    fun gurobiExecutesMultipleTargetsAndBuildsCriticalityProfile() = runBlocking {
        val fixture = Fixture()
        val solver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
        try {
            val snapshot = fixture.model.snapshot().value!!
            val profile = assertIs<Ok<CriticalityProfile, *, *>>(
                analyzeCriticalityTargets(
                    solver = solver,
                    snapshot = snapshot,
                    targets = listOf(
                        ObjectiveTarget.AtLeast(ObjectiveId("profit"), Flt64(12.0)),
                        ObjectiveTarget.AtLeast(ObjectiveId("profit"), Flt64(13.0))
                    )
                )
            ).value!!
            assertEquals(2, profile.observations.size)
            assertEquals(1, profile.provenTargetCount)
            assertTrue(profile.classifications.isNotEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun gurobiExecutesCompleteMultiTargetPipelineAndKeepsRecommendationsProven() = runBlocking {
        val fixture = Fixture()
        val baselineSolver = MipBackedConstraintProgrammingSolver(GurobiLinearSolver())
        try {
            val baselineResult = baselineSolver.solve(fixture.model)
            val feasible = assertIs<ConstraintProgrammingFeasibleOutput>(
                assertIs<Ok<ConstraintProgrammingSolverOutput, *, *>>(
                    baselineResult,
                    (baselineResult as? Failed<*, *, *>)?.error?.message
                        ?: baselineResult.toString()
                ).value!!
            )
            val snapshot = fixture.model.snapshot().value!!
            val result = assertIs<Ok<CriticalityProfileAnalysisResult, *, *>>(
                analyzeCriticalityTargets(
                    pipeline = gurobiCriticalConstraintAnalysisPipeline(),
                    snapshot = snapshot,
                    baselineSolution = feasible.solution,
                    baselineObjective = feasible.objective,
                    baselineProvenOptimal = true,
                    objectiveId = "profit",
                    targets = listOf(
                        ObjectiveTarget.AtLeast(
                            ObjectiveId("profit"),
                            Flt64(12.0)
                        ),
                        ObjectiveTarget.AtLeast(
                            ObjectiveId("profit"),
                            Flt64(13.0)
                        )
                    ),
                    options = CriticalConstraintAnalysisOptions(
                        perturbation = ConstraintPerturbationPolicy(
                            deltas = listOf(Flt64.one),
                            maxSolves = 4
                        ),
                        candidateLimit = 1
                    )
                )
            ).value!!

            assertEquals(2, result.reports.size)
            assertEquals(AnalysisStatus.Reachable, result.reports[0].target?.status)
            assertEquals(AnalysisStatus.Unreachable, result.reports[1].target?.status)
            assertEquals(AnalysisStatus.Unreachable, result.status)
            assertEquals(1, result.profile.provenTargetCount)
            assertTrue(result.reports[0].effectiveness != null)
            assertTrue(result.reports[1].conflict?.validity == ConflictValidity.Verified)
            // 只有冲突删除检查证明存在 MUS 时才生成建议。 / Recommendations are emitted only when the conflict's deletion checks proved an MUS.
            result.improvementPlans.forEach { plan ->
                assertTrue(plan.correctionSet.minimal)
                assertTrue(plan.correctionSet.members.isNotEmpty())
            }
        } finally {
            fixture.close()
        }
    }
}
