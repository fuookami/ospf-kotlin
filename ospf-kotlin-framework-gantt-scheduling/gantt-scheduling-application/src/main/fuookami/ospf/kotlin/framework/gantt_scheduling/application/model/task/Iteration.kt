@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 任务迭代器 / Task iterator */
package fuookami.ospf.kotlin.framework.gantt_scheduling.application.model.task

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.IterativeAbstractTask
import fuookami.ospf.kotlin.utils.functional.sum
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.math.operator.abs
import org.apache.logging.log4j.kotlin.logger
import kotlin.time.Clock

/**
 * 任务迭代器 / Task iterator
 *
 * @param T 迭代任务类型 / Iterative task type
 * @param E 执行器类型 / Executor type
 * @param A 分配策略类型 / Assignment policy type
 * @param initialSlowLpImprovementStep 初始慢LP改进步长 / Initial slow LP improvement step
 * @param relativeImprovementStep 相对改进步长 / Relative improvement step
 * @param improvementSlowCount 改进缓慢计数 / Improvement slow count
 */
class Iteration<T : IterativeAbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val initialSlowLpImprovementStep: Flt64 = Flt64(100.0),
    val relativeImprovementStep: Flt64 = Flt64(0.01),
    val improvementSlowCount: UInt64 = UInt64(5UL)
) {
    private val logger = logger()

    private var _iteration = UInt64.Companion.zero
    val iteration get() = _iteration
    private var beginTime = Clock.System.now()
    val runTime get() = Clock.System.now() - beginTime

    private var slowLpImprovementStep: Flt64 = Flt64.Companion.infinity
    private var slowLpImprovementCount = UInt64.Companion.zero
    private var slowIpImprovementCount = UInt64.Companion.zero
    val isImprovementSlow get() = slowIpImprovementCount >= improvementSlowCount

    private var prevLpObj = Flt64.Companion.maximum
    private var prevIpObj = Flt64.Companion.maximum

    private var bestObj = Flt64.Companion.maximum
    private var bestLpObj = Flt64.Companion.maximum
    private var bestDualObj = Flt64.Companion.minimum
    private var lowerBound = Flt64.Companion.zero
    private var upperBound: Flt64? = null
    val optimalRate: Flt64
        get() {
            val actualOptimalRate = ((lowerBound + Flt64.Companion.one) / (bestObj + Flt64.Companion.one)).sqrt()
            return min(Flt64.Companion.one, upperBound?.let { max(actualOptimalRate, (it - bestObj) / it) } ?: actualOptimalRate)
        }

    /**
     * 刷新下界 / Refresh lower bound
     *
     * @param newTasks 新任务列表 / List of new tasks
     * @param reducedCost 约简成本函数 / Reduced cost function
     */
    fun refreshLowerBound(
        newTasks: List<T>,
        reducedCost: (T) -> Flt64
    ) {
        val bestReducedCost = HashMap<E, Flt64>()
        for (task in newTasks) {
            val thisReducedCost = reducedCost(task)
            bestReducedCost[task.executor!!] =
                bestReducedCost[task.executor!!]?.let { min(thisReducedCost, it) } ?: thisReducedCost
        }

        val currentDualObj = prevLpObj + bestReducedCost.values.sum()
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
     * 刷新LP目标值 / Refresh LP objective
     *
     * @param obj LP目标值 / LP objective value
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
     * 刷新IP目标值 / Refresh IP objective
     *
     * @param obj IP目标值 / IP objective value
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

    operator fun inc(): Iteration<T, E, A> {
        ++_iteration
        return this
    }

    operator fun dec(): Iteration<T, E, A> {
        --_iteration
        return this
    }

    override fun toString(): String {
        return "$iteration"
    }
}

