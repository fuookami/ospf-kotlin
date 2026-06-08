package fuookami.ospf.kotlin.framework.csp1d.application.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan
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
    /** warm start 与当前问题不匹配 / Warm start does not match the current problem */
    Invalid
}

/**
 * CSP1D warm start 输入 / CSP1D warm start input
 *
 * @param V 数值类型 / Numeric value type
 * @property cuttingPlans 预热方案池 / Warm-start cutting plan pool
 * @property previousSolution 上一轮解，可用于调用方记录来源 / Previous solution for caller-side provenance
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
 * @property message 补充说明 / Additional message
 */
data class Csp1dRecoveryTrace(
    val status: Csp1dRecoveryStatus,
    val warmStartStatus: Csp1dWarmStartStatus,
    val attemptCount: Int,
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
    solver: ColumnGenerationSolver,
    private val milp: Csp1dMilp<V> = Csp1dMilp(solver)
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
        val warmStartStatus = warmStartStatus(input)
        if (requiresFallback(warmStartStatus) && !input.options.retryWithoutWarmStart) {
            val trace = Csp1dRecoveryTrace(
                status = Csp1dRecoveryStatus.FallbackDisabled,
                warmStartStatus = warmStartStatus,
                attemptCount = 0,
                message = fallbackDisabledMessage(warmStartStatus)
            )
            throw Csp1dRecoveryFallbackDisabledException(
                message = "Warm start cannot be applied and fallback is disabled: $warmStartStatus",
                trace = trace
            )
        }

        val solution = try {
            milp.solve(
                problem = input.problem,
                solveConfig = input.solveConfig
            )
        } catch (error: Exception) {
            val trace = Csp1dRecoveryTrace(
                status = Csp1dRecoveryStatus.SolveFailed,
                warmStartStatus = warmStartStatus,
                attemptCount = 1,
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
                message = warmStartMessage(warmStartStatus)
            )
        )
    }

    private fun warmStartStatus(input: Csp1dRecoveryInput<V>): Csp1dWarmStartStatus {
        val warmStart = input.warmStart ?: return Csp1dWarmStartStatus.NotProvided
        val plans = warmStartPlans(warmStart)
        if (plans.isEmpty()) {
            return Csp1dWarmStartStatus.Ignored
        }
        if (!isWarmStartCompatible(plans, input.problem)) {
            return Csp1dWarmStartStatus.Invalid
        }
        return Csp1dWarmStartStatus.AdapterUnsupported
    }

    private fun isWarmStartCompatible(
        plans: List<CuttingPlan<V>>,
        problem: Csp1dProblem<V>
    ): Boolean {
        val materialIds = problem.materials.map { it.id }.toSet()
        val machineIds = problem.machines.map { it.id }.toSet()
        val productIds = problem.products.map { it.id }.toSet()
        for (plan in plans) {
            if (plan.material.id !in materialIds) {
                return false
            }
            val machineId = plan.machineId
            if (machineId != null && machineIds.isNotEmpty() && machineId !in machineIds) {
                return false
            }
            if (plan.demandContributions.any { it.product.id !in productIds }) {
                return false
            }
        }
        return true
    }

    private fun warmStartPlans(warmStart: Csp1dWarmStart<V>): List<CuttingPlan<V>> {
        return warmStart.cuttingPlans.ifEmpty {
            warmStart.previousSolution?.generatedPlans.orEmpty()
        }
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
}
