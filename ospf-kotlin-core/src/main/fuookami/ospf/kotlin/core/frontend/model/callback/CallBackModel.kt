package fuookami.ospf.kotlin.core.frontend.model.callback

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface CallBackModelPolicy {
    fun compareObjective(lhs: Flt64, rhs: Flt64): Boolean? = lhs ls rhs
    fun initialSolution(variableAmount: UInt64): Solution? = (UInt64.zero until variableAmount).map { Flt64.zero }
}

class FunctionalCallBackModelPolicy(
    val objectiveComparator: PartialComparator<Flt64> = { lhs, rhs -> lhs ls rhs },
    val initialSolutionGenerator: Extractor<Solution?, UInt64> = { (UInt64.zero until it).map { Flt64.zero } }
) : CallBackModelPolicy {
    override fun compareObjective(lhs: Flt64, rhs: Flt64): Boolean? = objectiveComparator(lhs, rhs)
    override fun initialSolution(variableAmount: UInt64): Solution? = initialSolutionGenerator(variableAmount)
}

class CallBackModel internal constructor(
    private val _variables: MutableList<Variable> = ArrayList(),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<Flt64?, Solution>, String>> = ArrayList(),
    private val policy: CallBackModelPolicy
) : CallBackModelInterface {
    companion object {
        private fun dumpObjectiveComparator(category: ObjectCategory) : PartialComparator<Flt64> = when (category) {
            ObjectCategory.Maximum -> { lhs, rhs -> lhs gr rhs }
            ObjectCategory.Minimum -> { lhs, rhs -> lhs ls rhs }
        }

        operator fun invoke(
            category: ObjectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator: Extractor<Solution?, UInt64> = { (UInt64.zero until it).map { Flt64.zero } }
        ) = CallBackModel(policy = FunctionalCallBackModelPolicy(dumpObjectiveComparator(category), initialSolutionGenerator))

        operator fun invoke(
            objectiveComparator: PartialComparator<Flt64>,
            initialSolutionGenerator: Extractor<Solution?, UInt64> = { (UInt64.zero until it).map { Flt64.zero } }
        ) = CallBackModel(policy = FunctionalCallBackModelPolicy(objectiveComparator, initialSolutionGenerator))

        operator fun invoke(
            model: MetaModel<*>,
            initialSolutionGenerator: Extractor<Solution?, UInt64> = { (UInt64.zero until it).map { Flt64.zero } }
        ) : CallBackModel {
            val variables = model.tokens.tokens.map {
                Variable(it.variable)
            }.toMutableList()
            val constraints = model.constraints.map { constraint ->
                Pair<Extractor<Boolean?, Solution>, String>(
                    { solution: Solution -> constraint.isTrue(solution, model.tokens.tokenList) },
                    constraint.name
                )
            }.toMutableList()
            val objectiveFunction = model.subObjects.map { objective ->
                Pair<Extractor<Flt64?, Solution>, String>(
                    { solution: Solution -> objective.value(solution) },
                    objective.name
                )
            }.toMutableList()
            return CallBackModel(
                variables,
                constraints,
                objectiveFunction,
                FunctionalCallBackModelPolicy(
                    dumpObjectiveComparator(model.objectCategory),
                    initialSolutionGenerator
                )
            )
        }
    }

    override val variables by this::_variables
    override val constraints by this::_constraints
    override val objectiveFunctions by this::_objectiveFunctions

    override fun initialSolution(): Solution? = policy.initialSolution(UInt64(variables.size.toULong()))

    // todo: addVar, addConstraint, addObjectiveFunction
}
