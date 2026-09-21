package fuookami.ospf.kotlin.core.solver.scip

import kotlin.test.assertEquals
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
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpRequest
import fuookami.ospf.kotlin.core.analysis.LocalSensitivityScope
import fuookami.ospf.kotlin.core.analysis.analyzeCriticalityTargets
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSolveResult
import fuookami.ospf.kotlin.core.analysis.ConstraintPerturbationPolicy
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSensitivityReport
import fuookami.ospf.kotlin.core.analysis.CriticalityProfileAnalysisResult
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSensitivityOptions
import fuookami.ospf.kotlin.core.analysis.CriticalConstraintAnalysisOptions
import fuookami.ospf.kotlin.core.analysis.FixedIntegerLpSensitivityAnalyzer
import fuookami.ospf.kotlin.core.variable.IntVar

/**
 * 使用真实 SCIP 验证固定整数 LP 局部有效性与跨后端一致性。
 *
 * 与 `GurobiCriticalConstraintAnalysisIT` 使用**同一个**人工可算模型：
 * ```
 * max  3x + 2y
 * s.t. x + y   <= 4      (c1)
 *      x + 3y  <= 6      (c2)
 *      x, y ∈ [0, 10] 整数
 * ```
 * 最优解 `(4, 0)`、`z* = 12`；独立推导的基准真值：`c1` 对偶 `3`、`c2` 对偶 `0`。
 *
 * 两个后端跑同一模型同一期望值，即计划 8.13「Backend consistency」的直接证据。
 *
 * Verifies fixed-integer LP local effectiveness and cross-backend consistency with real SCIP,
 * using the **same** hand-computable model as the Gurobi IT. Running both backends against one
 * model and one set of expected values is direct evidence for plan criterion 8.13.
 */
class ScipCriticalConstraintAnalysisIT {
    private fun capability(): CapabilityMatrix {
        return CapabilityMatrix(
            modelTypes = setOf(SolverModelType.MIP),
            dual = true
        )
    }

    private class Fixture : AutoCloseable {
        val model = ConstraintProgrammingModel("scip-fixed-integer-lp", ObjectCategory.Maximum)
        val x = IntVar("x")
        val y = IntVar("y")

        init {
            model.registerVariable(x, IntegerDomain.interval(0, 10).value!!)
            model.registerVariable(y, IntegerDomain.interval(0, 10).value!!)
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(
                    ConstraintProgrammingExpression.linear(
                        listOf(
                            ConstraintProgrammingExpression.Term(x, Int64.one),
                            ConstraintProgrammingExpression.Term(y, Int64.one)
                        )
                    ).value!!,
                    Int64(4)
                ).value!!,
                id = ConstraintId("c1")
            )
            model.addConstraint(
                ConstraintProgrammingConstraint.lessOrEqual(
                    ConstraintProgrammingExpression.linear(
                        listOf(
                            ConstraintProgrammingExpression.Term(x, Int64.one),
                            ConstraintProgrammingExpression.Term(y, Int64(3))
                        )
                    ).value!!,
                    Int64(6)
                ).value!!,
                id = ConstraintId("c2")
            )
            model.maximize(
                ConstraintProgrammingExpression.linear(
                    listOf(
                        ConstraintProgrammingExpression.Term(x, Int64(3)),
                        ConstraintProgrammingExpression.Term(y, Int64(2))
                    )
                ).value!!,
                id = ObjectiveId("profit")
            )
        }

        override fun close() {
            model.close()
        }
    }

    private suspend fun solveBaseline(fixture: Fixture): ConstraintProgrammingFeasibleOutput {
        val solver = MipBackedConstraintProgrammingSolver(ScipLinearSolver())
        val result = solver.solve(fixture.model)
        val output = assertIs<Ok<ConstraintProgrammingSolverOutput, *, *>>(
            result,
            (result as? Failed<*, *, *>)?.error?.message ?: result.toString()
        ).value!!
        return assertIs<ConstraintProgrammingFeasibleOutput>(output)
    }

    @Test
    fun scipGeneratedDualModelProducesVerifiedDualsOnALoweredLp() = runBlocking {
        val fixture = Fixture()
        try {
            val feasible = solveBaseline(fixture)
            val snapshot = fixture.model.snapshot().value!!

            // 直接驱动生产后端（`ScipFixedIntegerLpBackend`），但不钉死任何整数：此时降阶 LP 非退化，
            // 对偶唯一存在。这是**迁移过来的"自动生成对偶模型并求解"机制确实可用**的正向证据。
            //
            // Drives the production backend directly with no integer pinned. The lowered LP is then
            // non-degenerate and the dual is uniquely defined. This is positive evidence that the
            // migrated "generate the dual model and solve it" mechanism actually works.
            val solved = assertIs<Ok<FixedIntegerLpSolveResult, *, *>>(
                scipFixedIntegerLpBackend().solve(
                    FixedIntegerLpRequest(
                        snapshot = snapshot,
                        fixedValues = emptyMap(),
                        baselineSolution = feasible.solution,
                        options = FixedIntegerLpSensitivityOptions()
                    )
                )
            ).value!!

            val dump = "status=${solved.status} objective=${solved.objectiveValue?.toDouble()} " +
                "duals=" + solved.duals.map { "${it.key.value}->${it.value?.toDouble()}" } + " message=${solved.message}"
            assertEquals(AnalysisStatus.Reachable, solved.status, dump)
            assertEquals(12.0, solved.objectiveValue!!.toDouble(), 1e-6, dump)

            // 与独立推导的基准真值一致，且与 Gurobi 后端同值（跨后端一致性，计划 8.13）。
            //
            // Matches the independently derived ground truth and the Gurobi backend value
            // (cross-backend consistency, plan criterion 8.13).
            assertEquals(3.0, solved.duals[ConstraintId("c1")]!!.toDouble(), 1e-6, dump)
            assertEquals(0.0, solved.duals[ConstraintId("c2")]!!.toDouble(), 1e-6, dump)

            // 强对偶恒等式必须成立：只有满足它的对偶才可能是最优对偶，也正是不满足它的原生
            // `getDual` 值（实测 c1 = 2.0，Σ rhs·y = 8.0）被丢弃的原因。
            //
            // The strong-duality identity must hold: only duals satisfying it can be optimal, and it
            // is precisely why the native `getDual` value (measured c1 = 2.0, Σ rhs·y = 8.0) is
            // discarded.
            val strongDuality = 4.0 * solved.duals[ConstraintId("c1")]!!.toDouble() +
                6.0 * solved.duals[ConstraintId("c2")]!!.toDouble()
            assertEquals(12.0, strongDuality, 1e-6, "Σ rhs·y must equal z* [$dump]")
        } finally {
            fixture.close()
        }
    }

    @Test
    fun scipFixedIntegerLpNeverFabricatesAnUnverifiedDual() = runBlocking {
        val fixture = Fixture()
        try {
            val feasible = solveBaseline(fixture)
            // 基线必须与独立推导的最优解一致：SCIP 的**原始求解**是正确的。
            // The baseline must match the independently derived optimum: SCIP's **primal solve** is
            // correct.
            assertEquals(12.0, feasible.objective!!.toDouble(), 1e-6)
            assertEquals(
                4L,
                feasible.solution.values[
                    VariableId("${fixture.x.identifier}:${fixture.x.index}")
                ]!!.toLong()
            )
            val snapshot = fixture.model.snapshot().value!!

            val report = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                FixedIntegerLpSensitivityAnalyzer(scipFixedIntegerLpBackend()).analyze(
                    snapshot = snapshot,
                    solution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability()
                )
            ).value!!

            // 本环境实测结论（详见 `ScipFixedIntegerLpBackend` 的类文档）：
            //
            // 分析器会把整数全部钉死，而降阶器把 `fixedValues` 落成**变量上下界**（`x ∈ [4,4]`、
            // `y ∈ [0,0]`，实测 triad 只剩 `[c1, c2]` 两行），原始行对偶因此非唯一。此时：
            //   - 原生 `getDual` = `[-0.0, -0.0]`（Σ rhs·y = 0.0 ≠ 12.0）；
            //   - 自动生成的对偶模型可解（数学上最优值为 12）但 SCIP 返回
            //     `objective = 0.0`、取值 `[1e20, 0, -1e20, 0, -1e20, 0]` 的哨兵解，
            //     连对偶模型自身的约束都不满足。
            // 两种配置（单线程/默认、presolve 关/开）结果一致，说明这不是配置问题。
            //
            // 因此正确行为是**整体降级为 Unsupported**，而不是把 1e20 当对偶、也不是留下
            // "c1 无对偶、c2 有对偶"的部分结果让调用方误读为 c1 无效。
            //
            // 该用例锁定的正是这条红线。注意：固定整数 LP 的行对偶本身也不是边际值——把 c1 的
            // RHS 从 4 提到 5 并不会改变 z=12（变量的界已把解钉死），这正是计划 3.5 用扰动法
            // 替代固定整数 LP 对偶的原因。
            //
            // Measured in this environment (see the `ScipFixedIntegerLpBackend` class docs): the
            // analyzer pins every integer, and the lowerer turns `fixedValues` into **variable
            // bounds** (`x ∈ [4,4]`, `y ∈ [0,0]`; the triad keeps only the two rows `[c1, c2]`), so
            // the original row duals are non-unique. Here the native `getDual` yields
            // `[-0.0, -0.0]` (Σ rhs·y = 0.0 ≠ 12.0), and although the auto-generated dual model is
            // solvable (its true optimum is 12) SCIP returns a sentinel solution with
            // `objective = 0.0` and values `[1e20, 0, -1e20, 0, -1e20, 0]` that do not even satisfy
            // the dual model's own constraints. Both configurations (single-threaded/default,
            // presolve off/on) agree, so this is not a settings artifact.
            //
            // The correct behaviour is therefore a whole-result degradation to `Unsupported`, not
            // 1e20 passed off as a dual and not a partial "c1 missing, c2 present" set that a caller
            // would misread as c1 being ineffective.
            //
            // This case pins that rule. Note also that a fixed-integer LP's row duals are not
            // marginal values: raising c1's RHS from 4 to 5 does not change z = 12 because the
            // variable bounds already pin the solution. That is exactly why plan criterion 3.5
            // replaces fixed-integer LP duals with the perturbation method.
            val dump = "status=${report.status} message=${report.message} " +
                "duals=" + report.sensitivities.joinToString { "${it.constraintId.value}->${it.dualValue}" }
            assertEquals(AnalysisStatus.Unsupported, report.status, dump)
            assertNotNull(report.message, "an Unsupported dual result must carry a reason [$dump]")
            // 不得出现任何未经验证的数值：要么缺失，要么是有限的真实对偶。
            // No unverified values may be exposed: every dual must be absent or a finite, genuine value.
            report.sensitivities.forEach { sensitivity ->
                val dual = sensitivity.dualValue
                assertTrue(
                    dual == null || (dual.toDouble().isFinite() && kotlin.math.abs(dual.toDouble()) < 1e20),
                    "SCIP must never report an unverified dual (got ${dual?.toDouble()} for ${sensitivity.constraintId})"
                )
            }
            // 整体降级：不得留下部分对偶集合。
            // Whole-result degradation must not leave a partial dual set.
            assertEquals(
                0,
                report.sensitivities.count { it.dualValue != null },
                "a degraded dual result must not expose a partial dual set [$dump]"
            )
        } finally {
            fixture.close()
        }
    }

    @Test
    fun scipFixedIntegerLpDoesNotExposeLoweringArtifacts() = runBlocking {
        val fixture = Fixture()
        try {
            val feasible = solveBaseline(fixture)
            val snapshot = fixture.model.snapshot().value!!
            val report = assertIs<Ok<FixedIntegerLpSensitivityReport, *, *>>(
                FixedIntegerLpSensitivityAnalyzer(scipFixedIntegerLpBackend()).analyze(
                    snapshot = snapshot,
                    solution = feasible.solution,
                    baselineObjective = feasible.objective,
                    capabilityMatrix = capability()
                )
            ).value!!

            // 公开报告只能按原始约束身份编索引，且只包含原始约束——与对偶是否可用无关。
            // The public report is keyed by original constraint identity only, regardless of whether
            // a dual was available.
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
    fun scipExecutesMultipleTargetsAndBuildsCriticalityProfile() = runBlocking {
        val fixture = Fixture()
        val solver = ScipConstraintProgrammingSolver()
        try {
            val snapshot = fixture.model.snapshot().value!!
            val result = analyzeCriticalityTargets(
                solver = solver,
                snapshot = snapshot,
                targets = listOf(
            ObjectiveTarget.AtLeast(ObjectiveId("profit"), Flt64(12.0)),
            ObjectiveTarget.AtLeast(ObjectiveId("profit"), Flt64(13.0))
                )
            )
        val profile = assertIs<Ok<CriticalityProfile, *, *>>(result).value!!
            assertEquals(2, profile.observations.size)
            assertEquals(1, profile.provenTargetCount)
            assertTrue(profile.classifications.isNotEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun scipExecutesCompleteMultiTargetPipelineAndKeepsRecommendationsProven() = runBlocking {
        val fixture = Fixture()
        try {
            val feasible = solveBaseline(fixture)
            val snapshot = fixture.model.snapshot().value!!
            val result = assertIs<Ok<CriticalityProfileAnalysisResult, *, *>>(
                analyzeCriticalityTargets(
                    pipeline = scipCriticalConstraintAnalysisPipeline(),
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
            // Recommendations are emitted only when the conflict's deletion checks proved an MUS.
            // 仅当冲突删除检查证明了 MUS 时，才会生成修正建议。
            result.improvementPlans.forEach { plan ->
                assertTrue(plan.correctionSet.minimal)
                assertTrue(plan.correctionSet.members.isNotEmpty())
            }
        } finally {
            fixture.close()
        }
    }
}
