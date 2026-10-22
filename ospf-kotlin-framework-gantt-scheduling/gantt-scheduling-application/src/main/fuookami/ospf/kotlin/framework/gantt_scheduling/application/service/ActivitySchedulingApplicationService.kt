@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.service

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 有限域求解报告，保留无解、截断和可行但未证明最优的终态。 / Finite-domain outcome retaining no-incumbent, limited and unproven feasible states.
 * @property report 只对应当前有限模型的 solver 报告 / Solver report only for the current finite model
 * @property schedule 可选已复核排程 / Optional validated schedule
 */
data class ActivitySchedulingResult<V : RealNumber<V>>(
    val report: SolveReport<Flt64>,
    val schedule: ActivitySchedule<V>?
)

/** 活动级排程编排，共享独立与嵌入式注册路径。 / Activity orchestration sharing standalone and embedded registration paths.
 * @property solver 现有 solver 抽象，不执行列生成 / Existing solver abstraction, without running column generation
 * @property adapter 数值边界 / Numeric boundary
 */
class ActivitySchedulingApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: SchedulingSolverValueAdapter<V>
) {
    /** 创建新上下文、注册、求解、独立复核和提取。 / Create fresh context, register, solve, validate and extract.
     * @param problem 显式有限域业务输入 / Explicit finite-domain input
     * @param extra 追加变量、约束或目标的装配钩子 / Assembly hook for additional variables, constraints or objectives
     * @return 结构化终态，原生 solver 不可用时返回错误 / Structured outcome, or error when a native solver is unavailable
     */
    suspend fun solve(
        problem: ActivitySchedulingProblem<V>,
        extra: (ActivitySchedulingContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<ActivitySchedulingResult<V>> {
        val context = when (val result = ActivitySchedulingContext.create(problem, adapter)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val model = LinearMetaModel(name = "activity_scheduling")
        try {
            when (val result = run({ context.register(model) }, { extra(context, model) })) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val report = when (val result = solver.solveMILPWithStatus(model.name, model)) {
                is Ok -> when (val outcome = result.value) {
                    is ColumnGenerationSolver.MILPSolveResult.Feasible -> outcome.output
                    is ColumnGenerationSolver.MILPSolveResult.Infeasible -> outcome.output.toSolveReport()
                }
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (report.solutionPresence == SolutionPresence.None) {
                return ok(ActivitySchedulingResult(report, null))
            }
            val values = report.solution?.values
                ?: return Failed(ErrorCode.ApplicationFailed, "求解报告有解但无数值 / Solve report claims an incumbent without values")
            if (values.size != model.tokens.tokensInSolver.size || values.any { !it.toDouble().isFinite() }) {
                return Failed(ErrorCode.ApplicationFailed, "解维度错误或数值非有限 / Invalid solution dimension or non-finite values")
            }
            model.setSolution(values)
            return context.extractSolution(model).map { ActivitySchedulingResult(report, it) }
        } finally {
            model.close()
        }
    }
}
