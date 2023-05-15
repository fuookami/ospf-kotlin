package fuookami.ospf.kotlin.core.frontend.model.callback

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.variable.*

interface CallBackModelBaseInterface<Obj> : ModelInterface {
    val tokens: TokenList
    val constraints: List<Pair<Extractor<Boolean?, Solution>, String>>

    fun initialSolution(): Solution?

    fun objective(solution: Solution): Obj?
    fun compareObjective(lhs: Obj, rhs: Obj): Boolean?

    fun constraintSatisfied(solution: Solution): Boolean? {
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

    override fun compareObjective(lhs: Flt64, rhs: Flt64): Boolean? = lhs ls rhs
}

interface MultiObjectiveCallBackModelInterface<Obj> : CallBackModelBaseInterface<Obj> {

}
