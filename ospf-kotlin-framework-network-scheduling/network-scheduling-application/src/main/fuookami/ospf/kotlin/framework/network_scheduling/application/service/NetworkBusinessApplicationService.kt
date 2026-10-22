package fuookami.ospf.kotlin.framework.network_scheduling.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.allocation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.structure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.generalized_flow.model.*

/** 分配、运输及中转的独立求解编排。 / Standalone orchestration for allocation, transportation and transshipment.
 * @property solver 现有求解器抽象 / Existing solver abstraction
 * @property adapter 数值边界 / Numeric boundary
 */
class NetworkAllocationApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 创建独立上下文并调用共享领域注册路径。 / Create fresh context and use the shared domain registration path.
     * @param problem 分配问题 / Allocation problem
     * @param extra 额外业务规则 / Extra business rules
     * @return 结构化求解终态 / Structured solve outcome
     */
    suspend fun solve(
        problem: AllocationProblem<V>,
        extra: (AllocationContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<AllocationSolution<V>>> {
        val context = when (val result = AllocationContext.create(problem, adapter)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}

/** 通用整条路径选择，不在应用层重新构造覆盖或资源约束。 / Whole-path selection without rebuilding domain rows in application.
 * @property solver 求解器 / Solver
 * @property adapter 数值边界 / Numeric boundary
 */
class NetworkPathApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 求解有限候选集；solver 最优状态不证明全图或定价域最优。 / Solve a finite pool; solver optimality is not full-network optimality.
     * @param problem 当前候选集合与业务行 / Current candidate pool and business rows
     * @param extra 额外装配钩子；参与定价的行应声明在 problem 中 / Extra assembly hook; priced rows belong in problem
     * @return 只对最终候选集合有效的结果 / Outcome valid only for the final candidate pool
     */
    suspend fun <C> solve(
        problem: PathCompilationProblem<V, C>,
        extra: (PathCompilationContext<V, C>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<PathCompilationSolution<V, C>>> {
        val context = PathCompilationContext(problem, adapter)
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}

/** 有根网络结构及设施归属的独立求解入口。 / Standalone rooted-network structure and facility ownership entry.
 * @property solver 求解器 / Solver
 * @property adapter 数值边界 / Numeric boundary
 */
class NetworkStructureApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 结构约束与建设费用继续由 context 管理。 / Context retains structural constraints and construction cost.
     * @param problem 根树及覆盖问题 / Rooted-tree and coverage problem
     * @param extra 额外模型规则 / Extra model rules
     * @return 结构化终态 / Structured outcome
     */
    suspend fun solve(
        problem: NetworkStructureProblem<V>,
        extra: (NetworkStructureContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<NetworkStructureSolution<V>>> {
        val context = NetworkStructureContext(problem, adapter)
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}

/** 单输入产率转换、运输损耗与回收的独立求解入口。 / Standalone single-input conversion, transport loss and recovery entry.
 * @property solver 求解器 / Solver
 * @property adapter 数值边界 / Numeric boundary
 */
class NetworkGeneralizedFlowApplicationService<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val adapter: NetworkSchedulingSolverValueAdapter<V>
) {
    /** 不将多输入 BOM 或全局加工排程塞入广义流。 / Does not reinterpret multi-input BOM or global scheduling as generalized flow.
     * @param problem 有限容量广义流问题 / Finite-capacity generalized-flow problem
     * @param extra 额外装配规则 / Extra assembly rules
     * @return 结构化终态 / Structured outcome
     */
    suspend fun solve(
        problem: GeneralizedFlowProblem<V>,
        extra: (GeneralizedFlowContext<V>, LinearMetaModel<Flt64>) -> Try = { _, _ -> ok }
    ): Ret<NetworkSolveResult<GeneralizedFlowSolution<V>>> {
        val context = GeneralizedFlowContext(problem, adapter)
        return solveNetwork(
            name = context.name,
            solver = solver,
            register = { model -> run({ context.register(model) }, { extra(context, model) }) },
            extract = context::extractSolution
        )
    }
}
