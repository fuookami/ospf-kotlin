@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 求解器输出数据结构 / Solver output data structures */
package fuookami.ospf.kotlin.core.solver.output

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 求解器输出的密封接口。
 * Sealed interface for solver output.
*/
sealed interface SolverOutput {}

/**
 * 统一求解器输出接口，包含通用的求解统计信息。
 * Unified solver output interface, containing common solving statistics.
*/
sealed interface UnifiedSolverOutput : SolverOutput {

    /** 迭代次数（可选） / Iteration count (optional) */
    val iterations: UInt64?

    /** 节点数（可选） / Node count (optional) */
    val nodeCount: UInt64?

    /** 最优界（可选） / Best bound (optional) */
    val bestBound: Flt64?

    /** MIP 间隙（可选） / MIP gap (optional) */
    val mipGap: Flt64?

    /** 求解时间（可选） / Solve time (optional) */
    val solveTime: Duration?
}

/**
 * 线性求解器输出接口。
 * Linear solver output interface.
*/
sealed interface LinearSolverOutput : SolverOutput {}

/**
 * 二次求解器输出接口。
 * Quadratic solver output interface.
*/
sealed interface QuadraticSolverOutput : SolverOutput {}

/** 将 Flt64 默认值转换为 V 类型；非 Flt64 解返回 null / Convert a Flt64 default value to V type; returns null for non-Flt64 solutions */
@Suppress("UNCHECKED_CAST")
private fun <V> castSolverFlt64FallbackToValueOrNull(value: Flt64, solution: Solution<V>): V? {
    if (solution.any { it !is Flt64 }) {
        return null
    }
    return value as V
}

/**
 * 可行求解器输出，包含目标值、解和求解统计信息。
 * Feasible solver output, containing objective value, solution, and solving statistics.
 *
 * @param V 值类型 / Value type
 * @property obj 目标值（Flt64）/ Objective value (Flt64)
 * @property solution 解 / Solution
 * @property time 求解时间 / Solve time
 * @property possibleBestObj 可能的最优目标值 / Possible best objective value
 * @property gap 间隙 / Gap
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙 / MIP gap
 * @property solveTime 求解时间 / Solve time
 * @property objValueOrNull 目标值（V 类型，可为 null）/ Objective value (V type, nullable)
 * @property possibleBestObjValueOrNull 可能的最优目标值（V 类型，可为 null）/ Possible best objective value (V type, nullable)
 * @property bestBoundValueOrNull 最优界（V 类型，可为 null）/ Best bound (V type, nullable)
*/
data class FeasibleSolverOutput<V>(
    val obj: Flt64,
    val solution: Solution<V>,
    val time: Duration,
    val possibleBestObj: Flt64,
    val gap: Flt64,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64 = gap,
    override val solveTime: Duration = time,
    val objValueOrNull: V? = castSolverFlt64FallbackToValueOrNull(obj, solution),
    val possibleBestObjValueOrNull: V? = castSolverFlt64FallbackToValueOrNull(possibleBestObj, solution),
    val bestBoundValueOrNull: V? = bestBound?.let { castSolverFlt64FallbackToValueOrNull(it, solution) }
) : LinearSolverOutput, QuadraticSolverOutput, UnifiedSolverOutput {

    /**
     * 获取目标值，缺失时返回失败 / Get objective value, returning failure when missing
     * @return 目标值，缺失时返回失败 / Objective value, or failure when missing
    */
    fun objValue(): Ret<V> {
        return objValueOrNull
            ?.let { ok(it) }
            ?: Failed(
                ErrorCode.IllegalArgument,
                "FeasibleSolverOutput.objValue is unavailable. Provide explicit objValueOrNull for non-Flt64 solution."
            )
    }

    /**
     * 获取可能的最优目标值，缺失时返回失败 / Get possible best objective value, returning failure when missing
     * @return 可能的最优目标值，缺失时返回失败 / Possible best objective value, or failure when missing
    */
    fun possibleBestObjValue(): Ret<V> {
        return possibleBestObjValueOrNull
            ?.let { ok(it) }
            ?: Failed(
                ErrorCode.IllegalArgument,
                "FeasibleSolverOutput.possibleBestObjValue is unavailable. Provide explicit possibleBestObjValueOrNull for non-Flt64 solution."
            )
    }

    /**
     * 获取最优界，缺失时返回失败 / Get best bound value, returning failure when missing
     * @return 最优界值，缺失时返回失败 / Best bound value, or failure when missing
    */
    fun bestBoundValue(): Ret<V> {
        return bestBoundValueOrNull
            ?.let { ok(it) }
            ?: Failed(
                ErrorCode.IllegalArgument,
                "FeasibleSolverOutput.bestBoundValue is unavailable. Provide explicit bestBoundValueOrNull when bestBound is present."
            )
    }
}

/**
 * 将 Flt64 可行求解器输出转换为目标值类型的输出。
 * Convert a Flt64 feasible solver output to the target value type.
 *
 * @param V 目标值类型 / Target value type
 * @param converter 值转换器 / Value converter
 * @return 转换后的可行求解器输出 / Converted feasible solver output
*/
fun <V> FeasibleSolverOutput<Flt64>.convertTo(converter: IntoValue<V>): FeasibleSolverOutput<V>
        where V : RealNumber<V>, V : NumberField<V> {
    return FeasibleSolverOutput(
        obj = obj,
        solution = solution.map { converter.intoValue(it) },
        time = time,
        possibleBestObj = possibleBestObj,
        gap = gap,
        iterations = iterations,
        nodeCount = nodeCount,
        bestBound = bestBound,
        mipGap = mipGap,
        solveTime = solveTime,
        objValueOrNull = converter.intoValue(obj),
        possibleBestObjValueOrNull = converter.intoValue(possibleBestObj),
        bestBoundValueOrNull = bestBound?.let { converter.intoValue(it) }
    )
}

/**
 * 线性不可行求解器输出，包含 IIS 信息。
 * Linear infeasible solver output, containing IIS information.
 *
 * @property iis 不可行子系统模型视图 / Infeasible subsystem model view
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙（可选）/ MIP gap (optional)
 * @property solveTime 求解时间（可选）/ Solve time (optional)
*/
data class LinearInfeasibleSolverOutput(
    val iis: BasicLinearTriadModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null
) : LinearSolverOutput, UnifiedSolverOutput

/**
 * 二次不可行求解器输出，包含 IIS 信息。
 * Quadratic infeasible solver output, containing IIS information.
 *
 * @property iis 不可行子系统模型视图 / Infeasible subsystem model view
 * @property iterations 迭代次数（可选）/ Iteration count (optional)
 * @property nodeCount 节点数（可选）/ Node count (optional)
 * @property bestBound 最优界（可选）/ Best bound (optional)
 * @property mipGap MIP 间隙（可选）/ MIP gap (optional)
 * @property solveTime 求解时间（可选）/ Solve time (optional)
*/
data class QuadraticInfeasibleSolverOutput(
    val iis: QuadraticTetradModelView,
    override val iterations: UInt64? = null,
    override val nodeCount: UInt64? = null,
    override val bestBound: Flt64? = null,
    override val mipGap: Flt64? = null,
    override val solveTime: Duration? = null
) : QuadraticSolverOutput, UnifiedSolverOutput

/**
 * 带 IIS 的求解器输出包装。
 * Solver output wrapper with IIS.
 *
 * @param IIS IIS 类型 / IIS type
 * @property output 求解器输出 / Solver output
 * @property iis IIS 信息（可选）/ IIS information (optional)
*/
data class SolverOutputWithIIS<out IIS>(
    val output: SolverOutput,
    val iis: IIS?
)

/**
 * 将求解器输出与 IIS 信息组合。
 * Combine solver output with IIS information.
 *
 * @param IIS IIS 类型 / IIS type
 * @param iis IIS 信息（可为 null） / IIS information (nullable)
 * @return 带 IIS 的求解器输出 / Solver output with IIS
*/
fun <IIS> SolverOutput.withIIS(iis: IIS?): SolverOutputWithIIS<IIS> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}

/**
 * 将求解器输出包装为无 IIS 信息的形式。
 * Wrap solver output without IIS information.
 *
 * @return 无 IIS 的求解器输出 / Solver output without IIS
*/
fun SolverOutput.withoutIIS(): SolverOutputWithIIS<Nothing> {
    return SolverOutputWithIIS(
        output = this,
        iis = null
    )
}

/**
 * 将线性不可行求解器输出与内置 IIS 信息组合。
 * Combine linear infeasible solver output with its built-in IIS information.
 *
 * @return 带 IIS 的求解器输出 / Solver output with IIS
*/
fun LinearInfeasibleSolverOutput.withIIS(): SolverOutputWithIIS<BasicLinearTriadModelView> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}

/**
 * 将二次不可行求解器输出与内置 IIS 信息组合。
 * Combine quadratic infeasible solver output with its built-in IIS information.
 *
 * @return 带 IIS 的求解器输出 / Solver output with IIS
*/
fun QuadraticInfeasibleSolverOutput.withIIS(): SolverOutputWithIIS<QuadraticTetradModelView> {
    return SolverOutputWithIIS(
        output = this,
        iis = iis
    )
}
