package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.SimpleInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.canonicalKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.CuttingPlanUsage
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dProblem
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolveConfig
import fuookami.ospf.kotlin.framework.csp1d.application.model.Csp1dSolution

/**
 * 恢复求解状态 / Recovery solve status
 */
enum class Csp1dRecoveryStatus {
    /** 普通恢复求解完成 / Normal recovery solve completed */
    Solved,
    /** warm start 无法使用，已退回普通求解 / Warm start was unusable and normal solve was used */
    RetriedWithoutWarmStart,
    /** 禁用 fallback，恢复流程未进入求解 / Fallback was disabled and solve was not attempted */
    FallbackDisabled,
    /** 普通求解失败 / Normal solve failed */
    SolveFailed
}

/**
 * warm start 处理状态 / Warm start handling status
 */
enum class Csp1dWarmStartStatus {
    /** 未提供 warm start / Warm start was not provided */
    NotProvided,
    /** warm start 输入为空，未形成可复用上下文 / Warm start input is empty and has no reusable context */
    Ignored,
    /** 当前 solver 适配层不支持消费 warm start / Current solver adapter does not support warm start */
    AdapterUnsupported,
    /** warm start 已被 adapter 应用 / Warm start was applied by adapter */
    Applied,
    /** warm start 与当前问题不匹配 / Warm start does not match the current problem */
    Invalid
}

/**
 * CSP1D warm start 输入 / CSP1D warm start input
 *
 * @param V 数值类型 / Numeric value type
 * @property cuttingPlans 预热方案池 / Warm-start cutting plan pool
 * @property previousSolution 上一轮解，会提取与当前问题兼容的方案和使用量 / Previous solution whose current-problem-compatible plans and usages are extracted
 */
data class Csp1dWarmStart<V : RealNumber<V>>(
    val cuttingPlans: List<CuttingPlan<V>> = emptyList(),
    val previousSolution: Csp1dSolution<V>? = null
)

/**
 * CSP1D 恢复选项 / CSP1D recovery options
 *
 * @property retryWithoutWarmStart warm start 不可用时是否退回普通求解 / Whether to retry normal solve when warm start is unusable
 */
data class Csp1dRecoveryOptions(
    val retryWithoutWarmStart: Boolean = true
)

/**
 * CSP1D 恢复输入 / CSP1D recovery input
 *
 * @param V 数值类型 / Numeric value type
 * @property problem 问题定义 / Problem definition
 * @property solveConfig 显式求解配置 / Explicit solve configuration
 * @property warmStart warm start 输入 / Warm start input
 * @property options 恢复选项 / Recovery options
 */
data class Csp1dRecoveryInput<V : RealNumber<V>>(
    val problem: Csp1dProblem<V>,
    val solveConfig: Csp1dSolveConfig<V>? = null,
    val warmStart: Csp1dWarmStart<V>? = null,
    val options: Csp1dRecoveryOptions = Csp1dRecoveryOptions()
)

/**
 * CSP1D 恢复追踪 / CSP1D recovery trace
 *
 * @property status 恢复状态 / Recovery status
 * @property warmStartStatus warm start 状态 / Warm start status
 * @property attemptCount 求解尝试次数 / Solve attempt count
 * @property warmStartPlanCount 可复用 warm start 方案数 / Reusable warm-start plan count
 * @property appliedWarmStartPlanCount 已应用 warm start 方案数 / Applied warm-start plan count
 * @property appliedWarmStartUsageCount 已应用 warm start 使用量条目数 / Applied warm-start usage entry count
 * @property message 补充说明 / Additional message
 */
data class Csp1dRecoveryTrace(
    val status: Csp1dRecoveryStatus,
    val warmStartStatus: Csp1dWarmStartStatus,
    val attemptCount: Int,
    val warmStartPlanCount: Int = 0,
    val appliedWarmStartPlanCount: Int = 0,
    val appliedWarmStartUsageCount: Int = 0,
    val message: String? = null
)

/**
 * CSP1D 恢复结果 / CSP1D recovery result
 *
 * @param V 数值类型 / Numeric value type
 * @property solution 求解结果 / Solution
 * @property trace 恢复追踪 / Recovery trace
 */
data class Csp1dRecoveryResult<V : RealNumber<V>>(
    val solution: Csp1dSolution<V>,
    val trace: Csp1dRecoveryTrace
)

/**
 * CSP1D warm start adapter 输入 / CSP1D warm start adapter input
 *
 * @param V 数值类型 / Numeric value type
 * @property problem 问题定义 / Problem definition
 * @property solveConfig 显式求解配置 / Explicit solve configuration
 * @property warmStart warm start 输入 / Warm start input
 * @property cuttingPlans 已校验兼容的 warm start 方案池 / Compatible warm-start cutting plan pool
 */
data class Csp1dWarmStartAdapterInput<V : RealNumber<V>>(
    val problem: Csp1dProblem<V>,
    val solveConfig: Csp1dSolveConfig<V>?,
    val warmStart: Csp1dWarmStart<V>,
    val cuttingPlans: List<CuttingPlan<V>>
)

/**
 * CSP1D warm start adapter 结果 / CSP1D warm start adapter result
 *
 * @param V 数值类型 / Numeric value type
 * @property initialGenerator 应用 warm start 后的初始方案生成器 / Initial plan generator after applying warm start
 * @property initialPlanUsages 应用 warm start 后的初始方案使用量 / Initial plan usages after applying warm start
 * @property appliedPlanCount 已应用方案数 / Applied plan count
 * @property appliedUsageCount 已应用使用量条目数 / Applied usage entry count
 * @property message 补充说明 / Additional message
 */
data class Csp1dWarmStartAdapterResult<V : RealNumber<V>>(
    val initialGenerator: Csp1dInitialCuttingPlanGenerator<V>?,
    val initialPlanUsages: List<CuttingPlanUsage<V>> = emptyList(),
    val appliedPlanCount: Int = 0,
    val appliedUsageCount: Int = initialPlanUsages.size,
    val message: String? = null
)

/**
 * CSP1D warm start adapter / CSP1D warm start adapter
 *
 * @param V 数值类型 / Numeric value type
 */
fun interface Csp1dWarmStartAdapter<V : RealNumber<V>> {
    /**
     * 应用 warm start / Apply warm start
     *
     * @param input adapter 输入 / Adapter input
     * @return adapter 结果 / Adapter result
     */
    fun apply(input: Csp1dWarmStartAdapterInput<V>): Csp1dWarmStartAdapterResult<V>

    companion object {
        /**
         * 不支持 warm start 的默认 adapter / Default adapter that does not support warm start
         *
         * @param V 数值类型 / Numeric value type
         * @return warm start adapter / Warm start adapter
         */
        fun <V : RealNumber<V>> unsupported(): Csp1dWarmStartAdapter<V> {
            return Csp1dWarmStartAdapter {
                Csp1dWarmStartAdapterResult(
                    initialGenerator = null,
                    initialPlanUsages = emptyList(),
                    appliedPlanCount = 0,
                    appliedUsageCount = 0,
                    message = "Warm start adapter is not configured"
                )
            }
        }
    }
}

/**
 * 方案池 warm start adapter / Cutting-plan-pool warm start adapter
 *
 * @param V 数值类型 / Numeric value type
 * @property fallbackGenerator 追加的普通初始方案生成器 / Appended normal initial plan generator
 * @property appendFallbackPlans 是否追加普通初始方案 / Whether to append normal initial plans
 */
class Csp1dWarmStartPlanPoolAdapter<V : RealNumber<V>>(
    private val fallbackGenerator: Csp1dInitialCuttingPlanGenerator<V> = SimpleInitialCuttingPlanGenerator(),
    private val appendFallbackPlans: Boolean = true
) : Csp1dWarmStartAdapter<V> {
    override fun apply(input: Csp1dWarmStartAdapterInput<V>): Csp1dWarmStartAdapterResult<V> {
        val initialPlanUsages = warmStartPlanUsages(input)
        return Csp1dWarmStartAdapterResult(
            initialGenerator = Csp1dInitialCuttingPlanGenerator { generationInput ->
                val fallbackPlans = if (appendFallbackPlans) {
                    fallbackGenerator.generate(generationInput)
                } else {
                    emptyList()
                }
                input.cuttingPlans + fallbackPlans
            },
            initialPlanUsages = initialPlanUsages,
            appliedPlanCount = input.cuttingPlans.size,
            appliedUsageCount = initialPlanUsages.size,
            message = "Warm start cutting plan pool was applied as initial plan pool"
        )
    }

    private fun warmStartPlanUsages(input: Csp1dWarmStartAdapterInput<V>): List<CuttingPlanUsage<V>> {
        val previousSolution = input.warmStart.previousSolution ?: return emptyList()
        val compatiblePlanKeys = input.cuttingPlans.map { it.canonicalKey() }.toSet()
        return previousSolution.produce.cuttingPlans.filter { usage ->
            usage.plan.canonicalKey() in compatiblePlanKeys
        }
    }
}

/**
 * CSP1D recovery fallback 禁用异常 / CSP1D recovery fallback-disabled exception
 *
 * @param message 异常信息 / Exception message
 * @property trace 失败时的恢复追踪 / Recovery trace at failure
 */
class Csp1dRecoveryFallbackDisabledException(
    message: String,
    val trace: Csp1dRecoveryTrace
) : IllegalArgumentException(message)

/**
 * CSP1D recovery 求解异常 / CSP1D recovery solve exception
 *
 * @param message 异常信息 / Exception message
 * @property trace 失败时的恢复追踪 / Recovery trace at failure
 * @param cause 原始异常 / Original exception
 */
class Csp1dRecoverySolveException(
    message: String,
    val trace: Csp1dRecoveryTrace,
    cause: Throwable
) : IllegalStateException(message, cause)

/**
 * CSP1D 恢复求解入口 / CSP1D recovery entry point
 *
 * @param V 数值类型 / Numeric value type
 */
class Csp1dRecovery<V : RealNumber<V>>(
    private val solver: ColumnGenerationSolver,
    private val milp: Csp1dMilp<V> = Csp1dMilp(solver),
    private val warmStartAdapter: Csp1dWarmStartAdapter<V> = Csp1dWarmStartAdapter.unsupported()
) {
    /**
     * 在异常恢复场景下重新求解 / Re-solve for recovery scenarios
     *
     * @param problem 问题定义 / Problem definition
     * @param solveConfig 显式求解配置 / Explicit solve configuration
     * @return 求解结果 / Solution
     */
    suspend fun solve(
        problem: Csp1dProblem<V>,
        solveConfig: Csp1dSolveConfig<V>? = null
    ): Csp1dSolution<V> {
        return solveWithTrace(
            input = Csp1dRecoveryInput(
                problem = problem,
                solveConfig = solveConfig
            )
        ).solution
    }

    /**
     * 带恢复追踪求解 / Solve with recovery trace
     *
     * @param input 恢复输入 / Recovery input
     * @return 恢复结果 / Recovery result
     */
    suspend fun solveWithTrace(input: Csp1dRecoveryInput<V>): Csp1dRecoveryResult<V> {
        val warmStart = input.warmStart
        val warmStartPlanSelection = warmStartPlanSelection(input)
        val warmStartPlans = warmStartPlanSelection.plans
        val initialWarmStartStatus = warmStartStatus(
            input = input,
            plans = warmStartPlans,
            selectedStatus = warmStartPlanSelection.status
        )
        val adapterResult = if (initialWarmStartStatus == Csp1dWarmStartStatus.AdapterUnsupported && warmStart != null) {
            warmStartAdapter.apply(
                Csp1dWarmStartAdapterInput(
                    problem = input.problem,
                    solveConfig = input.solveConfig,
                    warmStart = warmStart,
                    cuttingPlans = warmStartPlans
                )
            )
        } else {
            null
        }
        val warmStartStatus = if (adapterResult?.initialGenerator != null) {
            Csp1dWarmStartStatus.Applied
        } else {
            initialWarmStartStatus
        }
        if (requiresFallback(warmStartStatus) && !input.options.retryWithoutWarmStart) {
            val trace = Csp1dRecoveryTrace(
                status = Csp1dRecoveryStatus.FallbackDisabled,
                warmStartStatus = warmStartStatus,
                attemptCount = 0,
                warmStartPlanCount = warmStartPlans.size,
                appliedWarmStartPlanCount = 0,
                appliedWarmStartUsageCount = 0,
                message = fallbackDisabledMessage(warmStartStatus)
            )
            throw Csp1dRecoveryFallbackDisabledException(
                message = "Warm start cannot be applied and fallback is disabled: $warmStartStatus",
                trace = trace
            )
        }

        val solution = try {
            val activeMilp = adapterResult?.initialGenerator?.let { initialGenerator ->
                Csp1dMilp(
                    solver = solver,
                    initialGenerator = initialGenerator,
                    warmStartPlanUsages = adapterResult.initialPlanUsages
                )
            } ?: milp
            activeMilp.solve(
                problem = input.problem,
                solveConfig = input.solveConfig
            )
        } catch (error: Exception) {
            val trace = Csp1dRecoveryTrace(
                status = Csp1dRecoveryStatus.SolveFailed,
                warmStartStatus = warmStartStatus,
                attemptCount = 1,
                warmStartPlanCount = warmStartPlans.size,
                appliedWarmStartPlanCount = adapterResult?.appliedPlanCount ?: 0,
                appliedWarmStartUsageCount = adapterResult?.appliedUsageCount ?: 0,
                message = error.message ?: "Recovery solve failed"
            )
            throw Csp1dRecoverySolveException(
                message = trace.message ?: "Recovery solve failed",
                trace = trace,
                cause = error
            )
        }
        val status = if (requiresFallback(warmStartStatus)) {
            Csp1dRecoveryStatus.RetriedWithoutWarmStart
        } else {
            Csp1dRecoveryStatus.Solved
        }
        return Csp1dRecoveryResult(
            solution = solution,
            trace = Csp1dRecoveryTrace(
                status = status,
                warmStartStatus = warmStartStatus,
                attemptCount = 1,
                warmStartPlanCount = warmStartPlans.size,
                appliedWarmStartPlanCount = adapterResult?.appliedPlanCount ?: 0,
                appliedWarmStartUsageCount = adapterResult?.appliedUsageCount ?: 0,
                message = adapterResult?.message ?: warmStartMessage(warmStartStatus)
            )
        )
    }

    private fun warmStartStatus(
        input: Csp1dRecoveryInput<V>,
        plans: List<CuttingPlan<V>>,
        selectedStatus: Csp1dWarmStartStatus?
    ): Csp1dWarmStartStatus {
        if (selectedStatus != null) {
            return selectedStatus
        }
        if (input.warmStart == null) {
            return Csp1dWarmStartStatus.NotProvided
        }
        if (plans.isEmpty()) {
            return Csp1dWarmStartStatus.Ignored
        }
        if (!isWarmStartCompatible(plans, input.problem)) {
            return Csp1dWarmStartStatus.Invalid
        }
        return Csp1dWarmStartStatus.AdapterUnsupported
    }

    private fun warmStartPlanSelection(
        input: Csp1dRecoveryInput<V>
    ): WarmStartPlanSelection<V> {
        val warmStart = input.warmStart ?: return WarmStartPlanSelection(
            plans = emptyList(),
            status = Csp1dWarmStartStatus.NotProvided
        )
        if (warmStart.cuttingPlans.isNotEmpty()) {
            if (!isWarmStartCompatible(warmStart.cuttingPlans, input.problem)) {
                return WarmStartPlanSelection(
                    plans = warmStart.cuttingPlans,
                    status = Csp1dWarmStartStatus.Invalid
                )
            }
            return WarmStartPlanSelection(
                plans = warmStart.cuttingPlans
            )
        }
        val compatiblePlans = warmStart.previousSolution
            ?.generatedPlans
            ?.filter { plan -> isWarmStartCompatible(plan, input.problem) }
            .orEmpty()
        return WarmStartPlanSelection(
            plans = compatiblePlans
        )
    }

    private fun isWarmStartCompatible(
        plans: List<CuttingPlan<V>>,
        problem: Csp1dProblem<V>
    ): Boolean {
        return plans.all { plan -> isWarmStartCompatible(plan, problem) }
    }

    private fun isWarmStartCompatible(
        plan: CuttingPlan<V>,
        problem: Csp1dProblem<V>
    ): Boolean {
        val materialById = problem.materials.associateBy { it.id }
        val machineIds = problem.machines.map { it.id }.toSet()
        val productIds = problem.products.map { it.id }.toSet()
        val material = materialById[plan.material.id] ?: return false
        if (!material.enabled(plan, problem.machines)) {
            return false
        }
        val machineId = plan.machineId
        if (machineId != null && machineIds.isNotEmpty() && machineId !in machineIds) {
            return false
        }
        if (plan.demandContributions.any { it.product.id !in productIds }) {
            return false
        }
        return true
    }

    private fun requiresFallback(status: Csp1dWarmStartStatus): Boolean {
        return status == Csp1dWarmStartStatus.Invalid ||
                status == Csp1dWarmStartStatus.AdapterUnsupported
    }

    private fun warmStartMessage(status: Csp1dWarmStartStatus): String? {
        return when (status) {
            Csp1dWarmStartStatus.NotProvided -> null
            Csp1dWarmStartStatus.Ignored -> "Warm start was provided without reusable cutting plans; normal solve was used"
            Csp1dWarmStartStatus.AdapterUnsupported -> "Warm start is compatible but current MILP adapter does not support applying it; normal solve was used"
            Csp1dWarmStartStatus.Applied -> "Warm start was applied by adapter"
            Csp1dWarmStartStatus.Invalid -> "Warm start is incompatible with current problem; normal solve was used"
        }
    }

    private fun fallbackDisabledMessage(status: Csp1dWarmStartStatus): String {
        return when (status) {
            Csp1dWarmStartStatus.AdapterUnsupported -> "Warm start is compatible but current MILP adapter does not support applying it; fallback is disabled"
            Csp1dWarmStartStatus.Invalid -> "Warm start is incompatible with current problem; fallback is disabled"
            else -> "Warm start cannot be applied and fallback is disabled"
        }
    }

    private data class WarmStartPlanSelection<V : RealNumber<V>>(
        val plans: List<CuttingPlan<V>>,
        val status: Csp1dWarmStartStatus? = null
    )
}
