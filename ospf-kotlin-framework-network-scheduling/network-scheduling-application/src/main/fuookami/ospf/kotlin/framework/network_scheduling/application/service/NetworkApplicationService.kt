package fuookami.ospf.kotlin.framework.network_scheduling.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.design.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.resilience.*

/** 独立求解终态；无 incumbent 时保留报告而不尝试提取业务量。 / Standalone outcome, retaining reports without an incumbent.
 * @property report 求解器边界报告 / Solver-boundary report
 * @property solution 可选业务解 / Optional business solution
 */
data class NetworkSolveResult<S>(val report: SolveReport<Flt64>, val solution: S?)

/** 共用装配入口，不在 application 内重复领域约束。 / Shared orchestration without duplicating domain constraints. */
private suspend fun <S> solveNetwork(
    name: String,
    solver: ColumnGenerationSolver,
    register: (LinearMetaModel<Flt64>) -> Try,
    extract: (LinearMetaModel<Flt64>) -> Ret<S>
): Ret<NetworkSolveResult<S>> {
    val model = LinearMetaModel(name = name)
    try {
        when (val result = register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val report = when (val result = solver.solveMILPWithStatus(name, model)) {
            is Ok -> when (val outcome = result.value) {
                is ColumnGenerationSolver.MILPSolveResult.Feasible -> outcome.output
                is ColumnGenerationSolver.MILPSolveResult.Infeasible -> outcome.output.toSolveReport()
            }
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (report.solutionPresence == SolutionPresence.None) {
            return ok(NetworkSolveResult(report, null))
        }
        val values = report.solution?.values
            ?: return networkSchedulingFailure("求解器宣称有解但未提供数值 / Solver claims an incumbent without values")
        if (values.size != model.tokens.tokensInSolver.size) {
            return networkSchedulingFailure("求解器解维度不匹配 / Solver solution dimension mismatch")
        }
        model.setSolution(values)
        return extract(model).map { NetworkSolveResult(report, it) }
    } finally {
        model.close()
    }
}

/** 独立运输分配与中转求解入口。 / Standalone transportation and transshipment entry.
 * @property solver 现有求解器抽象 / Existing solver abstraction
 * @property adapter 数值转换边界 / Numeric conversion boundary
 */
class NetworkFlowApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 每次求解创建独立注册态；嵌入业务应直接使用 FlowContext。 / Create fresh state; embedded callers use FlowContext.
     * @param problem 流问题 / Flow problem
     * @param extra 追加模型规则 / Additional model rules
     * @return 结构化终态 / Structured outcome
     */
    suspend fun solve(
        problem: FlowProblem<V>,
        extra: (FlowContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<FlowSolution<V>>> {
        val context = FlowContext(problem, adapter)
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}

/** 独立设施、通道与扩容求解入口。 / Standalone facility, service and expansion entry.
 * @property solver 求解器 / Solver
 * @property adapter 数值边界 / Numeric boundary
 */
class NetworkDesignApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 创建共享领域注册路径。 / Use the shared domain registration path.
     * @param problem 建设问题 / Design problem
     * @param extra 追加模型规则 / Additional model rules
     * @return 结构化终态 / Structured outcome
     */
    suspend fun solve(
        problem: NetworkDesignProblem<V>,
        extra: (NetworkDesignContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<NetworkDesignSolution<V>>> {
        val context = NetworkDesignContext(problem, adapter)
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}

/** 独立连通及显式故障场景求解入口。 / Standalone connectivity and explicit-failure entry.
 * @property solver 求解器 / Solver
 * @property adapter 数值边界 / Numeric boundary
 */
class NetworkResilienceApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 各场景复用建设决策，但不复用流变量。 / Share construction, not recourse flow variables.
     * @param problem 建设及名义流问题 / Design and nominal-flow problem
     * @param scenarios 显式场景集合 / Explicit scenarios
     * @param connectivity 连通要求 / Connectivity requirements
     * @param extra 追加模型规则 / Additional model rules
     * @return 结构化终态 / Structured outcome
     */
    suspend fun solve(
        problem: NetworkDesignProblem<V>,
        scenarios: List<FailureScenario<V>>,
        connectivity: List<ConnectivityRequirement> = emptyList(),
        extra: (NetworkResilienceContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<NetworkResilienceSolution<V>>> {
        val context = NetworkResilienceContext(
            design = NetworkDesignContext(problem, adapter),
            scenarios = scenarios,
            connectivity = connectivity
        )
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}
