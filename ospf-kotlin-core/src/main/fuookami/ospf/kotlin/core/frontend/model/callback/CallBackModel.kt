package fuookami.ospf.kotlin.core.frontend.model.callback

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface CallBackModelPolicy {
    fun compareObjective(lhs: Flt64?, rhs: Flt64?): Order? {
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

    fun initialSolutions(initialSolutionAmount: UInt64, variableAmount: UInt64): List<Solution> {
        return listOf((UInt64.zero until variableAmount).map { Flt64.zero })
    }
}

class FunctionalCallBackModelPolicy(
    val objectiveComparator: PartialComparator<Flt64> = { lhs, rhs -> lhs ls rhs },
    val initialSolutionsGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
) : CallBackModelPolicy {
    override fun compareObjective(lhs: Flt64?, rhs: Flt64?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            if (objectiveComparator(lhs, rhs) == true) {
                Order.Less()
            } else if (objectiveComparator(rhs, lhs) == true) {
                Order.Greater()
            } else {
                Order.Equal
            }
        } else {
            null
        }
    }

    override fun initialSolutions(initialSolutionAmount: UInt64, variableAmount: UInt64): List<Solution> {
        return (UInt64.zero until initialSolutionAmount).map { solution -> (UInt64.zero until variableAmount).map { initialSolutionsGenerator(Pair(solution, it)) } }
    }
}

class CallBackModel internal constructor(
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val tokens: TokenList = ManualAddTokenTokenList(),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<Flt64?, Solution>, String>> = ArrayList(),
    private val policy: CallBackModelPolicy
) : CallBackModelInterface {
    companion object {
        private fun dumpObjectiveComparator(category: ObjectCategory): PartialComparator<Flt64> = when (category) {
            ObjectCategory.Maximum -> { lhs, rhs -> lhs gr rhs }
            ObjectCategory.Minimum -> { lhs, rhs -> lhs ls rhs }
        }

        operator fun invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ) = CallBackModel(objectCategory = objectCategory, policy = FunctionalCallBackModelPolicy(dumpObjectiveComparator(objectCategory), initialSolutionGenerator))

        operator fun invoke(
            objectiveComparator: PartialComparator<Flt64>,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ) = CallBackModel(policy = FunctionalCallBackModelPolicy(objectiveComparator, initialSolutionGenerator))

        operator fun invoke(
            model: SingleObjectModel<*>,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ): CallBackModel {
            val tokens: TokenList = ManualAddTokenTokenList()
            for (token in model.tokens.tokens) {
                tokens.add(token.variable)
            }
            val constraints = model.constraints.map { constraint ->
                Pair<Extractor<Boolean?, Solution>, String>(
                    { solution: Solution -> constraint.isTrue(solution) },
                    constraint.name
                )
            }.toMutableList()
            val objectiveFunction = model.objectFunction.subObjects.map { objective ->
                Pair<Extractor<Flt64?, Solution>, String>(
                    { solution: Solution ->
                        if (objective.category == model.objectFunction.category) {
                            objective.value(solution)
                        } else {
                            -objective.value(solution)
                        }
                    },
                    objective.name
                )
            }.toMutableList()
            return CallBackModel(
                model.objectFunction.category,
                tokens,
                constraints,
                objectiveFunction,
                FunctionalCallBackModelPolicy(
                    dumpObjectiveComparator(model.objectFunction.category),
                    initialSolutionGenerator
                )
            )
        }
    }

    override val constraints by this::_constraints
    override val objectiveFunctions by this::_objectiveFunctions

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution> = policy.initialSolutions(initialSolutionAmount, UInt64(tokens.solverIndexMap.size))

    override fun compareObjective(lhs: Flt64?, rhs: Flt64?): Order? = policy.compareObjective(lhs, rhs)

    override fun addVar(item: Item<*, *>) {
        tokens.add(item)
    }

    override fun addVars(items: Combination<*, *, *>) {
        tokens.add(items)
    }

    override fun addVars(items: CombinationView<*, *>) {
        tokens.add(items)
    }

    override fun remove(item: Item<*, *>) {
        tokens.remove(item)
    }

    override fun addConstraint(inequality: Inequality<*>, name: String?, displayName: String?) {
        _constraints.add(
            Pair<Extractor<Boolean?, Solution>, String>(
                { solution: Solution -> inequality.isTrue(solution, tokens) },
                name ?: String()
            )
        )
    }

    override fun addObject(category: ObjectCategory, polynomial: Polynomial<*>, name: String?, displayName: String?) {
        _objectiveFunctions.add(
            Pair<Extractor<Flt64?, Solution>, String>(
                { solution: Solution ->
                    if (category == objectCategory) {
                        polynomial.value(solution, tokens)
                    } else {
                        -polynomial.value(solution, tokens)
                    }
                },
                name ?: String()
            )
        )
    }

    fun addObject(category: ObjectCategory, func: Extractor<Flt64?, Solution>, name: String? = null, displayName: String? = null) {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution ->
                    if (category == objectCategory) {
                        func(solution)
                    } else {
                        func(solution)?.let { -it }
                    }
                },
                name ?: String()
            )
        )
    }

    override fun setSolution(solution: Solution) {
        tokens.setResults(solution)
    }

    override fun clearSolution() {
        tokens.clearResults()
    }
}
