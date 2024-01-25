package fuookami.ospf.kotlin.core.frontend.model.callback

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.*

interface AbstractCallBackModelInterface<Obj, V> : ModelInterface {
    val tokens: MutableTokenList
    val constraints: List<Pair<Extractor<Boolean?, Solution>, String>>

    val objectiveFunctions: List<Pair<Extractor<Obj?, Solution>, String>>

    fun initialSolutions(initialSolutionAmount: UInt64 = UInt64.one): List<Solution> {
        return emptyList()
    }

    fun operation(lhs: V, rhs: V): V

    fun objectiveValue(): V
    fun objectiveValue(obj: Obj): V

    fun objective(solution: Solution): V? {
        var obj = objectiveValue()
        for ((objectiveFunction, _) in objectiveFunctions) {
            val thisObj = objectiveFunction(solution) ?: return null
            obj = operation(obj, objectiveValue(thisObj))
        }
        return obj
    }

    fun compareObjective(lhs: V, rhs: V): Order?

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("comparePartialObjective")
    fun compareObjective(lhs: V?, rhs: V?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            compareObjective(lhs, rhs)
        } else {
            null
        }
    }

    fun constraintSatisfied(solution: Solution): Boolean? {
        for (token in tokens.tokens) {
            val index = tokens.tokenIndexMap[token] ?: continue
            if (!token.range.contains(solution[index])) {
                return false
            }
        }
        for ((constraint, _) in constraints) {
            when (constraint(solution)) {
                true -> {}
                false -> {
                    return false
                }

                null -> {
                    return null
                }
            }
        }
        return true
    }
}

interface CallBackModelInterface : AbstractCallBackModelInterface<Flt64, Flt64> {
    override fun objectiveValue(): Flt64 {
        return Flt64.zero
    }

    override fun objectiveValue(obj: Flt64): Flt64 {
        return obj
    }

    override fun operation(lhs: Flt64, rhs: Flt64): Flt64 {
        return lhs + rhs
    }
}

interface MultiObjectiveModelInterface : AbstractCallBackModelInterface<MulObj, List<Flt64>> {
    val objectiveSize: Int

    override fun objectiveValue(): List<Flt64> {
        return (0 until objectiveSize).map { Flt64.zero }
    }

    override fun objectiveValue(obj: List<Pair<MultiObjectLocation, Flt64>>): List<Flt64> {
        return (0 until objectiveSize).map {
            obj.sumOf(Flt64) { (loc, v) ->
                if (it == loc.priority.toInt()) {
                    v * loc.weight
                } else {
                    Flt64.zero
                }
            }
        }
    }

    override fun operation(lhs: List<Flt64>, rhs: List<Flt64>): List<Flt64> {
        return (0 until objectiveSize).map { lhs[it] + rhs[it] }
    }
}
