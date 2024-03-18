package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_selection.model

import kotlinx.datetime.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

class Iteration<T : IterativeAbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    val initialSlowLpImprovementStep: Flt64 = Flt64(100.0),
    val relativeImprovementStep: Flt64 = Flt64(0.01),
    val improvementSlowCount: UInt64 = UInt64(5UL)
) {
    private val logger = logger()

    private var _iteration = UInt64.zero
    val iteration get() = _iteration
    private var beginTime = Clock.System.now()
    val runTime get() = Clock.System.now() - beginTime

    private var slowLpImprovementStep: Flt64 = Flt64.infinity
    private var slowLpImprovementCount = UInt64.zero
    private var slowIpImprovementCount = UInt64.zero
    val isImprovementSlow get() = slowIpImprovementCount >= improvementSlowCount

    private var prevLpObj = Flt64.maximum
    private var prevIpObj = Flt64.maximum

    private var bestObj = Flt64.maximum
    private var bestLpObj = Flt64.maximum
    private var bestDualObj = Flt64.minimum
    private var lowerBound = Flt64.zero
    private var upperBound: Flt64? = null
    val optimalRate: Flt64
        get() {
            val actualOptimalRate = ((lowerBound + Flt64.one) / (bestObj + Flt64.one)).sqrt()
            return min(Flt64.one, upperBound?.let { max(actualOptimalRate, (it - bestObj) / it) } ?: actualOptimalRate)
        }

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

    fun refreshLpObj(obj: Flt64): Boolean {
        if (slowLpImprovementStep.isInfinity()) {
            slowLpImprovementStep = max(initialSlowLpImprovementStep, obj * relativeImprovementStep)
        } else {
            if ((prevLpObj - obj ls slowLpImprovementStep) || (((bestObj - obj) / bestLpObj) ls Flt64.one)) {
                ++slowLpImprovementCount
            } else {
                slowLpImprovementCount = UInt64.zero
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

    fun refreshIpObj(obj: Flt64): Boolean {
        if ((abs(prevIpObj - obj) ls Flt64(0.01)) || (((bestObj - obj) / bestObj) ls Flt64(0.01))) {
            ++slowIpImprovementCount
        } else {
            slowIpImprovementCount = UInt64.zero
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

    fun halveStep() {
        slowLpImprovementStep /= Flt64.two
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
