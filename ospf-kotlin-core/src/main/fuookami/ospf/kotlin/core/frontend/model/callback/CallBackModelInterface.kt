package fuookami.ospf.kotlin.core.frontend.model.callback

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.*

interface CallBackModelBaseInterface<Obj> : ModelInterface {
    val tokens: TokenList
    val constraints: List<Pair<Extractor<Boolean?, Solution>, String>>

    fun initialSolutions(initialSolutionAmount: UInt64 = UInt64.one): List<Solution> {
        return emptyList()
    }

    fun objective(solution: Solution): Obj?
    fun compareObjective(lhs: Obj?, rhs: Obj?): Order?

    fun constraintSatisfied(solution: Solution): Boolean? {
        for (token in tokens.tokens) {
            val index = tokens.solverIndexMap[token.solverIndex] ?: continue
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

interface CallBackModelInterface : CallBackModelBaseInterface<Flt64> {
    val objectiveFunctions: List<Pair<Extractor<Flt64?, Solution>, String>>

    override fun objective(solution: Solution): Flt64? {
        var obj = Flt64.zero
        for ((objectiveFunction, _) in objectiveFunctions) {
            obj += objectiveFunction(solution) ?: return null
        }
        return obj
    }

    override fun compareObjective(lhs: Flt64?, rhs: Flt64?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            lhs ord rhs
        } else {
            null
        }
    }
}

interface MultiObjectiveCallBackModelInterface<Obj> : CallBackModelBaseInterface<Obj> {

}
