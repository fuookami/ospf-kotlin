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
    private var bestDualObj = Flt64.Companion.minimum
    private var lowerBound = Flt64.Companion.zero
    private var upperBound: Flt64? = null
    /** 最优率，值域 [0, 1]，越接近 1 越优 / Optimal rate in [0, 1]; closer to 1 means more optimal */
    val optimalRate: Flt64
        get() {
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
        val bestReducedCost = HashMap<E, Flt64>()
        for (bunch in newBunches) {
            val thisReducedCost = reducedCost(bunch)
            bestReducedCost[bunch.executor] =
                bestReducedCost[bunch.executor]?.let { min(thisReducedCost, it) } ?: thisReducedCost
        }

        val currentDualObj = prevLpObj + bestReducedCost.values.sum(Flt64)
        if (bestDualObj ls currentDualObj && currentDualObj ls bestObj) {
            logger.debug { "best dual obj: $bestDualObj -> $currentDualObj" }
            bestDualObj = currentDualObj
            if (lowerBound ls currentDualObj) {
                logger.debug { "lower bound: $lowerBound -> $bestDualObj" }
                lowerBound = currentDualObj
                logger.debug { "optimal rate: ${String.format("%.2f", (optimalRate * Flt64(100.0)).toDouble())}%" }
            }
        }
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
            if (bestLpObj ls lowerBound) {
                logger.debug { "lower bound: $lowerBound -> $bestLpObj" }
                lowerBound = bestLpObj
                logger.debug { "optimal rate: ${String.format("%.2f", (optimalRate * Flt64(100.0)).toDouble())}%" }
            }
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
            if (bestObj ls lowerBound) {
                logger.debug { "lower bound: $lowerBound -> $bestObj" }
                lowerBound = bestObj
                logger.debug { "optimal rate: ${String.format("%.2f", (optimalRate * Flt64(100.0)).toDouble())}%" }
            }
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
            bestDualObjective = quantity(bestDualObj, adapter, unit),
            lowerBound = quantity(lowerBound, adapter, unit),
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
     * 中文将 Flt64 标量值转换为带类型的物理量
     *
     * @param value Flt64 scalar value / Flt64 标量值
     * @param adapter Solver value adapter / 求解器数值适配器
     * @param unit Physical unit / 物理单位
     * @return Typed physical quantity / 带类型的物理量
     */
    private fun <R : RealNumber<R>> quantity(
        value: Flt64,
        adapter: SchedulingSolverValueAdapter<R>,
        unit: PhysicalUnit
    ): Quantity<R> {
        return Quantity(adapter.intoValue(value), unit)
    }
}
