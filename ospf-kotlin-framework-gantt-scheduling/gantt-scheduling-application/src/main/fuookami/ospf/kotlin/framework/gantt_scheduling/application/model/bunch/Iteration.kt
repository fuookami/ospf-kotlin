@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务束迭代器 / Bunch iterator */
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.bunch

import kotlin.time.Clock
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.IterationSnapshot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 任务束迭代器 / Bunch iterator
 *
 * @param T 任务类型 / Task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @property initialSlowLpImprovementStep 初始慢 LP 目标标量改进步长 / Initial slow LP objective scalar improvement step
 * @property relativeImprovementStep 相对目标标量改进步长 / Relative objective scalar improvement step
 * @property improvementSlowCount 改进缓慢计数 / Improvement slow count
*/
class Iteration<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>, V : RealNumber<V>>(
    val initialSlowLpImprovementStep: Flt64 = Flt64(100.0),
    val relativeImprovementStep: Flt64 = Flt64(0.01),
    val improvementSlowCount: UInt64 = UInt64(5UL)
) {
    private val logger = logger()

    private var _iteration = UInt64.Companion.zero

    /** 当前迭代次数 / Current iteration count */
    val iteration get() = _iteration
    private var beginTime = Clock.System.now()

    /** 算法运行时长 / Algorithm runtime duration */
    val runTime get() = Clock.System.now() - beginTime

    private var slowLpImprovementStep: Flt64 = Flt64.Companion.infinity
    private var slowLpImprovementCount = UInt64.Companion.zero
    private var slowIpImprovementCount = UInt64.Companion.zero

    /** 是否改进缓慢 / Whether improvement is slow */
    val isImprovementSlow get() = slowIpImprovementCount >= improvementSlowCount

    private var prevLpObj = Flt64.Companion.maximum
    private var prevIpObj = Flt64.Companion.maximum

    private var bestObj = Flt64.Companion.maximum
    private var bestLpObj = Flt64.Companion.maximum
    private var bestDualObj: Flt64? = null
    private var lowerBound = -Flt64.infinity
    private var upperBound: Flt64? = null
    private var generatedColumnsInPricingRound = false
    private var completedPricingCallsInRound = 0

    /** 是否已具备全局整数最优证据；启发式迭代不提供此证据。 / Whether global integer optimality is certified. */
    val hasCertifiedOptimality: Boolean get() = lowerBound.isFinite() && bestObj.isFinite() && (bestObj - lowerBound).abs() leq Flt64(1e-8)

    /** 本轮定价是否返回过候选列；这不是最优性证明。 / Whether this pricing round returned columns; this is not an optimality proof. */
    val hasGeneratedColumnsInPricingRound: Boolean get() = generatedColumnsInPricingRound

    /** 本轮无候选列时停止迭代；这不代表全局最优。 / Stop after a round with no returned columns; this does not imply global optimality. */
    val shouldStopAfterPricingRound: Boolean get() = completedPricingCallsInRound > 0 && !generatedColumnsInPricingRound

    /** 开始新一轮定价跟踪。 / Start tracking a new pricing round. */
    fun beginPricingRound() {
        generatedColumnsInPricingRound = false
        completedPricingCallsInRound = 0
    }

    /** 记录一次定价返回的候选列数量。 / Record the number of candidate columns returned by one pricing call.
     * @param columnCount 返回列数 / Number of returned columns
     */
    fun recordPricingColumnCount(columnCount: Int) {
        ++completedPricingCallsInRound
        if (columnCount > 0) generatedColumnsInPricingRound = true
    }

    /** 启发式进度指标，值域 [0, 1]，不作为最优性证据 / Heuristic progress indicator in [0, 1], not an optimality certificate */
    val optimalRate: Flt64
        get() {
            if (!lowerBound.isFinite() || !bestObj.isFinite()) return Flt64.zero
            val actualOptimalRate = ((lowerBound + Flt64.Companion.one) / (bestObj + Flt64.Companion.one)).sqrt()
            return min(Flt64.Companion.one, upperBound?.let { max(actualOptimalRate, (it - bestObj) / it) } ?: actualOptimalRate)
        }

    /**
     * 刷新算法下界，约简成本为 branch-and-price 内部目标标量 / Refresh lower bound with branch-and-price internal reduced-cost scalar
     *
     * @param newBunches 新任务束列表 / List of new bunches
     * @param reducedCost 约简成本标量函数 / Reduced-cost scalar function
    */
    fun refreshLowerBound(
        newBunches: List<AbstractTaskBunch<T, E, A, V>>,
        reducedCost: (AbstractTaskBunch<T, E, A, V>) -> Flt64
    ) {
        // 候选列仅是可行见证，不能代替穷尽定价的最小缩减成本证明。 / Candidate columns do not certify the minimum reduced cost.
    }

    /**
     * 刷新 LP 目标标量 / Refresh LP objective scalar
     *
     * @param obj LP 目标标量 / LP objective scalar
     * @return 是否有改进 / Whether improved
    */
    fun refreshLpObj(obj: Flt64): Boolean {
        if (slowLpImprovementStep.isInfinity()) {
            slowLpImprovementStep = max(initialSlowLpImprovementStep, obj * relativeImprovementStep)
        } else {
            if ((prevLpObj - obj ls slowLpImprovementStep) || (((bestObj - obj) / bestLpObj) ls Flt64.Companion.one)) {
                ++slowLpImprovementCount
            } else {
                slowLpImprovementCount = UInt64.Companion.zero
            }
        }

        prevLpObj = obj

        var flag = false
        if (obj ls bestLpObj) {
            logger.debug { "best lp obj: $bestLpObj -> $obj" }
            flag = true
            bestLpObj = obj
        }
        return flag
    }

    /**
     * 刷新 IP 目标标量 / Refresh IP objective scalar
     *
     * @param obj IP 目标标量 / IP objective scalar
     * @return 是否有改进 / Whether improved
    */
    fun refreshIpObj(obj: Flt64): Boolean {
        if ((abs(prevIpObj - obj) ls Flt64(0.01)) || (((bestObj - obj) / bestObj) ls Flt64(0.01))) {
            ++slowIpImprovementCount
        } else {
            slowIpImprovementCount = UInt64.Companion.zero
        }
        prevIpObj = obj
        if (upperBound == null) {
            upperBound = obj
        }

        var flag = false
        if (obj ls bestObj) {
            logger.debug { "best obj: $bestObj -> $obj" }
            flag = true
            bestObj = obj
        }
        return flag
    }

    /** 减半步长 / Halve step */
    fun halveStep() {
        slowLpImprovementStep /= Flt64.Companion.two
    }

    /**
     * 生成泛型迭代快照 / Build a generic iteration snapshot
     *
     * @param R 目标数值类型 / Target numeric type
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 目标值单位 / Objective unit
     * @return 泛型迭代快照 / Generic iteration snapshot
    */
    fun <R : RealNumber<R>> snapshot(
        adapter: SchedulingSolverValueAdapter<R>,
        unit: PhysicalUnit = NoneUnit
    ): IterationSnapshot<R> {
        return IterationSnapshot(
            iteration = iteration,
            runTime = runTime,
            bestObjective = quantity(bestObj, adapter, unit),
            bestLpObjective = quantity(bestLpObj, adapter, unit),
            bestDualObjective = bestDualObj?.let { quantity(it, adapter, unit) },
            lowerBound = lowerBound.takeIf { it.isFinite() }?.let { quantity(it, adapter, unit) },
            upperBound = upperBound?.let { quantity(it, adapter, unit) },
            slowLpImprovementStep = if (slowLpImprovementStep.isInfinity()) {
                null
            } else {
                quantity(slowLpImprovementStep, adapter, unit)
            },
            optimalRate = adapter.intoValue(optimalRate),
            isImprovementSlow = isImprovementSlow
        )
    }

    /** 递增迭代次数 / Increment iteration count */
    operator fun inc(): Iteration<T, E, A, V> {
        ++_iteration
        return this
    }

    /** 递减迭代次数 / Decrement iteration count */
    operator fun dec(): Iteration<T, E, A, V> {
        --_iteration
        return this
    }

    /** 返回迭代次数的字符串表示 / Return string representation of iteration count */
    override fun toString(): String {
        return "$iteration"
    }

    /**
     * Convert a Flt64 scalar value into a typed physical quantity.
     * 将 Flt64 标量值转换为带类型的物理量
     *
     * @param value Flt64 scalar value / Flt64 标量值
     * @param adapter 求解器数值适配器 / Solver value adapter
     * @param unit 物理单位 / Physical unit
     * @return 带类型的物理量 / Typed physical quantity
    */
    private fun <R : RealNumber<R>> quantity(
        value: Flt64,
        adapter: SchedulingSolverValueAdapter<R>,
        unit: PhysicalUnit
    ): Quantity<R> {
        return Quantity(adapter.intoValue(value), unit)
    }
}

/**
 * 构造列生成终态的结构化诊断。 / Build structured diagnostics for column-generation termination.
 * @param solveId 求解运行标识 / Solver run identifier
 * @param fallbackTermination 未因空定价轮停止时的终止原因 / Termination reason when no empty pricing round stopped the run
 * @return 最终进度诊断 / Final progress diagnostics
 */
internal fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>, V : RealNumber<V>>
    Iteration<T, E, A, V>.finalProgressDiagnostics(
        solveId: String,
        fallbackTermination: String
    ): Map<String, String> {
    val termination = if (shouldStopAfterPricingRound) "no-pricing-columns" else fallbackTermination
    return mapOf(
        "solve" to solveId,
        "termination" to termination,
        "optimality" to if (hasCertifiedOptimality) "certified" else "not-certified"
    )
}
