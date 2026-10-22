package fuookami.ospf.kotlin.framework.bpp.application

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.toSolverStatus
import fuookami.ospf.kotlin.core.solver.output.SolverStatus
import fuookami.ospf.kotlin.core.solver.report.SolveReport
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.*

/** 完整领域模型装配结果。 / Complete domain model registration.
 * @property context 离散上下文 / Discrete context
 * @property model 完整模型 / Full model
 * @property additionalWeights 解提取时归一到问题单位的附加重量 / Extracted additional weights in the problem unit
 * @property bendersStages 扩展变量的显式主问题或连续追索阶段 / Explicit master or continuous-recourse stages for extension variables
 */
data class RegisteredDiscreteLoading<V : FloatingNumber<V>>(
    val context: DiscreteLoadingContext<V>,
    val model: LinearMetaModel<Flt64>,
    val additionalWeights: (List<Flt64>) -> Ret<Map<String, Flt64>> = { Ok(emptyMap()) },
    val bendersStages: Map<AbstractVariableItem<*, *>, LinearBendersStage> = emptyMap()
)

/** 可注入应用配置。 / Injectable application configuration.
 * @property preferBenders 是否启用 Benders / Whether Benders is preferred
 * @property fallbackToMilp 是否在 Benders 失败后重新装配完整 MILP / Whether to rebuild MILP after Benders failure
 * @property benders Benders 预算 / Benders budget
 */
data class DiscreteLoadingSolveConfig(
    val preferBenders: Boolean = false,
    val fallbackToMilp: Boolean = true,
    val benders: LinearBendersConfig = LinearBendersConfig(),
    val qualityGuard: (LinearBendersResult) -> Try = { ok }
)

/** 已复核的应用结果。 / Verified application result.
 * @property registration 实际成功路径的注册结果 / Registration of the successful path
 * @property values 完整模型自身 token 顺序的结果 / Values in full-model token order
 * @property solution 已复核领域结果 / Verified domain solution
 * @property status 真实求解状态 / Actual solver status
 * @property objective 领域目标 / Domain objective
 * @property path 实际路径 / Actual path
 * @property trace Benders 轨迹 / Benders trace
 * @property fallbackReason 可选回退原因 / Optional fallback reason
 * @property termination 后端终止原因 / Backend termination reason
 */
data class DiscreteLoadingSolveResult<V : FloatingNumber<V>>(
    val registration: RegisteredDiscreteLoading<V>,
    val values: List<Flt64>,
    val solution: DiscreteLoadingSolution,
    val status: String,
    val objective: Flt64,
    val path: String,
    val trace: List<LinearBendersIteration> = emptyList(),
    val fallbackReason: String? = null,
    val termination: String? = null
)

/** 离散装载 MILP/Benders 统一编排，不依赖任何商业求解器。 / Unified MILP/Benders orchestration without commercial dependencies.
 * @property milpSolver MILP 执行器 / MILP executor
 * @property bendersSolver 可选 Benders 后端 / Optional Benders backend
 */
class DiscreteLoadingApplicationService<V : FloatingNumber<V>>(
    private val milpSolver: suspend (LinearMetaModel<Flt64>) -> Ret<SolveReport<Flt64>>,
    private val bendersSolver: LinearBendersDecompositionSolver? = null
) {
    /** 求解；每次调用工厂都必须返回全新的变量、上下文及模型。 / Solve; each factory invocation must return fresh variables, context and model.
     * @param factory 完整模型工厂 / Full-model factory
     * @param config 求解配置 / Solve configuration
     * @return 已复核结果或失败 / Verified result or failure
     */
    suspend fun solve(
        factory: () -> Ret<RegisteredDiscreteLoading<V>>,
        config: DiscreteLoadingSolveConfig = DiscreteLoadingSolveConfig()
    ): Ret<DiscreteLoadingSolveResult<V>> {
        var fallback: String? = null
        var rejectedRegistration: RegisteredDiscreteLoading<V>? = null
        if (config.preferBenders) {
            val registration = when (val result = factory()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            rejectedRegistration = registration
            val models = LinearBendersPartition.build(registration.model, registration.bendersStages)
            val solved = when (models) {
                is Ok -> bendersSolver?.let { LinearBendersAlgorithm(it).solve(models.value, config.benders) }
                    ?: Failed(ErrorCode.IllegalArgument, "未配置 Benders 后端 / Benders backend is not configured")
                is Failed -> Failed(models.error)
                is Fatal -> Fatal(models.errors)
            }
            val result: Ret<LinearBendersResult> = if (solved is Ok) {
                when (val quality = config.qualityGuard(solved.value)) {
                    is Ok -> solved
                    is Failed -> Failed(quality.error)
                    is Fatal -> Fatal(quality.errors)
                }
            } else solved
            if (result is Ok) {
                val values = registration.model.tokens.tokensInSolver.map { token ->
                    result.value.values[token.variable] ?: return Failed(ErrorCode.DataNotFound, "缺少合并结果 / Missing merged value: ${token.name}")
                }
                registration.model.tokens.setSolution(result.value.values)
                return finish(
                    registration = registration,
                    values = values,
                    status = result.value.status,
                    objective = if (registration.model.objectCategory == ObjectCategory.Maximum) -result.value.objective else result.value.objective,
                    path = "benders",
                    trace = result.value.iterations,
                    fallbackReason = null,
                    termination = result.value.termination
                )
            }
            if (!config.fallbackToMilp) return when (result) {
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
                is Ok -> Failed(ErrorCode.ApplicationError, "无效 Benders 状态 / Invalid Benders state")
            }
            fallback = when (result) {
                is Failed -> result.error.message
                is Fatal -> result.errors.joinToString { it.message }
                is Ok -> null
            }
        }
        val registration = when (val result = factory()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (registration.model === rejectedRegistration?.model || registration.context === rejectedRegistration?.context) {
            return Failed(ErrorCode.IllegalArgument, "回退必须创建新模型与上下文 / Fallback requires a fresh model and context")
        }
        val output = when (val result = milpSolver(registration.model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val objective = output.solution?.objective ?: return if (output.toSolverStatus() == SolverStatus.Infeasible) {
            Failed(ErrorCode.ORModelInfeasible, "完整装载模型已证明不可行 / Full loading model is proven infeasible")
        } else {
            Failed(ErrorCode.OREngineSolvingException, "求解器缺少可行解 / Solver has no incumbent: ${output.toSolverStatus()}")
        }
        return finish(
            registration = registration,
            values = output.values,
            status = output.toSolverStatus().name,
            objective = objective,
            path = if (fallback == null) "milp_direct" else "milp_fallback_after_benders",
            trace = emptyList(),
            fallbackReason = fallback
        )
    }

    private suspend fun finish(
        registration: RegisteredDiscreteLoading<V>,
        values: List<Flt64>,
        status: String,
        objective: Flt64,
        path: String,
        trace: List<LinearBendersIteration>,
        fallbackReason: String?,
        termination: String? = null
    ): Ret<DiscreteLoadingSolveResult<V>> {
        val full = when (val result = LinearBendersPartition.build(registration.model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val byVariable = registration.model.tokens.tokens.associate { token ->
            val index = registration.model.tokens.indexOf(token.variable)
                ?: return Failed(ErrorCode.DataNotFound, "缺少完整模型 token / Missing full-model token")
            token.variable to (values.getOrNull(index) ?: return Failed(ErrorCode.DataNotFound, "缺少完整模型值 / Missing full-model value"))
        }
        val verification = full.verify(byVariable)
        if (verification is Failed) return Failed(verification.error)
        if (verification is Fatal) return Fatal(verification.errors)
        var evaluatedObjective = Flt64.zero
        for (part in registration.model.objectiveSnapshot()) {
            var value = part.constant
            for ((coefficient, variable) in part.linearTerms()) {
                val selected = byVariable[variable]
                    ?: return Failed(ErrorCode.DataNotFound, "目标引用缺少变量值 / Missing objective variable: ${variable.name}")
                value += coefficient * selected
            }
            evaluatedObjective += if (part.category == registration.model.objectCategory) value else -value
        }
        if (!objective.isFinite() || !evaluatedObjective.isFinite() ||
            (objective - evaluatedObjective).abs() > Flt64(1e-6) * maxOf(Flt64.one, evaluatedObjective.abs())) {
            return Failed(ErrorCode.ApplicationError, "求解目标与完整模型不一致 / Solver objective differs from the full model: $objective / $evaluatedObjective")
        }
        val weights = when (val result = registration.additionalWeights(values)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val solution = when (val result = registration.context.extractSolution(
            model = registration.model,
            values = values,
            additionalWeights = weights
        )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return Ok(DiscreteLoadingSolveResult(
            registration = registration,
            values = values,
            solution = solution,
            status = status,
            objective = objective,
            path = path,
            trace = trace,
            fallbackReason = fallbackReason,
            termination = termination
        ))
    }
}
