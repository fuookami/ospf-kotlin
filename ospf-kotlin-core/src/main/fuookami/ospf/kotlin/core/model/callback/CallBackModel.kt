package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.core.expression.Expression
import fuookami.ospf.kotlin.core.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.Polynomial
import fuookami.ospf.kotlin.core.model.MulObj
import fuookami.ospf.kotlin.core.model.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.utils.functional.Order

interface CallBackModelPolicy<V> {
    val comparator: ThreeWayComparator<V>

    fun compareObjective(lhs: V?, rhs: V?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            comparator(lhs, rhs)
        } else {
            null
        }
    }

    fun initialSolutions(initialSolutionAmount: UInt64, variableAmount: UInt64): List<Solution> {
        return listOf((UInt64.zero until variableAmount).map { Flt64.zero })
    }
}

class FunctionalCallBackModelPolicy<V>(
    val objectiveComparator: PartialComparator<V>,
    val initialSolutionsGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
) : CallBackModelPolicy<V> {
    override val comparator: ThreeWayComparator<V> = { lhs, rhs ->
        if (objectiveComparator(lhs, rhs) == true || objectiveComparator(rhs, lhs) == false) {
            Order.Less(-1)
        } else if (objectiveComparator(lhs, rhs) == false || objectiveComparator(rhs, lhs) == true) {
            Order.Greater(1)
        } else {
            Order.Equal
        }
    }

    override fun compareObjective(lhs: V?, rhs: V?): Order? {
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

    override fun initialSolutions(
        initialSolutionAmount: UInt64,
        variableAmount: UInt64
    ): List<Solution> {
        return (UInt64.zero until initialSolutionAmount).map { solution ->
            (UInt64.zero until variableAmount).map {
                initialSolutionsGenerator(
                    Pair(solution, it)
                )
            }
        }
    }
}

class CallBackModel internal constructor(
    category: Category = Nonlinear,
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val tokens: AbstractMutableTokenTable = ManualTokenTable(category),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<Flt64?, Solution>, String>> = ArrayList(),
    private val policy: CallBackModelPolicy<Flt64>
) : CallBackModelInterface {
    companion object {
        private fun dumpObjectiveComparator(category: ObjectCategory): PartialComparator<Flt64> = when (category) {
            ObjectCategory.Maximum -> { lhs, rhs -> lhs geq rhs }
            ObjectCategory.Minimum -> { lhs, rhs -> lhs leq rhs }
        }

        operator fun invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ) = CallBackModel(
            objectCategory = objectCategory,
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = dumpObjectiveComparator(objectCategory),
                initialSolutionsGenerator = initialSolutionGenerator
            )
        )

        operator fun invoke(
            objectiveComparator: PartialComparator<Flt64>,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ) = CallBackModel(
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = objectiveComparator,
                initialSolutionsGenerator = initialSolutionGenerator
            )
        )

        operator fun invoke(
            model: AbstractMetaModel,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ): CallBackModel {
            val tokens = model.tokens.copy()
            val constraints = model.constraints.map { constraint ->
                Pair(
                    { solution: Solution -> constraint.isTrue(solution, tokens) },
                    constraint.toString()
                )
            }.toMutableList()
            val objectiveFunction = model.subObjects.map { objective ->
                Pair(
                    { solution: Solution ->
                        if (objective.category == model.objectCategory) {
                            objective.evaluate(solution, tokens)
                        } else {
                            -objective.evaluate(solution, tokens)!!
                        }
                    },
                    objective.name
                )
            }.toMutableList()
            return CallBackModel(
                category = model.category,
                objectCategory = model.objectCategory,
                tokens = tokens,
                _constraints = constraints,
                _objectiveFunctions = objectiveFunction,
                policy = FunctionalCallBackModelPolicy(
                    dumpObjectiveComparator(model.objectCategory),
                    initialSolutionGenerator
                )
            )
        }

        operator fun invoke(
            model: SingleObjectMechanismModel,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero },
            concurrent: Boolean = true
        ): CallBackModel {
            val tokens = if (concurrent) {
                ConcurrentManualAddTokenTable(model.tokens)
            } else {
                ManualTokenTable(model.tokens)
            }
            val constraints = model.constraints.map { constraint ->
                Pair(
                    { solution: Solution -> constraint.isTrue(solution) },
                    constraint.name
                )
            }.toMutableList()
            val objectiveFunction = model.objectFunction.subObjects.map { objective ->
                Pair(
                    { solution: Solution ->
                        if (objective.category == model.objectFunction.category) {
                            objective.evaluate(solution)
                        } else {
                            objective.evaluate(solution)?.let {
                                -it
                            }
                        }
                    },
                    objective.name
                )
            }.toMutableList()
            return CallBackModel(
                category = Nonlinear,
                objectCategory = model.objectFunction.category,
                tokens = tokens,
                _constraints = constraints,
                _objectiveFunctions = objectiveFunction,
                policy = FunctionalCallBackModelPolicy(
                    dumpObjectiveComparator(model.objectFunction.category),
                    initialSolutionGenerator
                )
            )
        }
    }

    override val constraints by ::_constraints
    override val objectiveFunctions by ::_objectiveFunctions

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution> {
        return policy.initialSolutions(initialSolutionAmount, UInt64(tokens.tokensInSolver.size))
    }

    override fun compareObjective(lhs: Flt64, rhs: Flt64): Order {
        return policy.comparator(lhs, rhs)
    }

    override fun compareObjective(lhs: Flt64?, rhs: Flt64?): Order? {
        return policy.compareObjective(lhs, rhs)
    }

    override fun add(item: AbstractVariableItem<*, *>): Try {
        tokens.add(item)
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        tokens.add(items)
        return ok
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    @Suppress("UNUSED_PARAMETER")
    fun addConstraint(
        inequality: LinearConstraintInput,
        name: String?,
        displayName: String?
    ) {
        _constraints.add(
            Pair(
                { solution: Solution -> inequality.isTrue(solution, tokens) },
                name ?: String()
            )
        )
    }

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            expression = LinearPolynomial(variable),
            name = name,
            displayName = displayName
        )
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            expression = LinearPolynomial(constant),
            name = name,
            displayName = displayName
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        expression: Expression,
        name: String?,
        displayName: String?
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution ->
                    if (category == objectCategory) {
                        expression.evaluate(solution, tokens)
                    } else {
                        expression.evaluate(solution, tokens)?.let { -it }
                    }
                },
                name ?: String()
            )
        )
        return ok
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: Polynomial<*, *, *>,
        name: String?
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution ->
                    if (category == objectCategory) {
                        polynomial.evaluate(solution, tokens)
                    } else {
                        polynomial.evaluate(solution, tokens)?.let { -it }
                    }
                },
                name ?: String()
            )
        )
        return ok
    }

    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        func: Extractor<Flt64?, Solution>,
        name: String? = null,
        displayName: String? = null
    ): Try {
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
        return ok
    }

    fun maximize(
        expression: Expression,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            expression = expression,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        func: Extractor<Flt64?, Solution>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            func = func,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        expression: Expression,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            expression = expression,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        func: Extractor<Flt64?, Solution>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            func = func,
            name = name,
            displayName = displayName
        )
    }

    override fun setSolution(solution: Solution) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        tokens.setSolution(solution)
    }

    override fun flush() {
        tokens.flush()
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }
}

class MultiObjectCallBackModel internal constructor(
    category: Category = Nonlinear,
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val objectiveLocation: List<MultiObjectLocation>,
    override val tokens: AbstractMutableTokenTable = ManualTokenTable(category),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<MulObj?, Solution>, String>> = ArrayList(),
    private val initialSolutionsGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
) : MultiObjectiveModelInterface {
    companion object {
        operator fun invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            objectiveLocation: List<MultiObjectLocation> = listOf(MultiObjectLocation(UInt64.zero, Flt64.one)),
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ) = MultiObjectCallBackModel(
            objectCategory = objectCategory,
            objectiveLocation = objectiveLocation,
            initialSolutionsGenerator = initialSolutionGenerator
        )
    }

    init {
        require(objectiveLocation.isNotEmpty()) {
            "objectiveLocation can not be empty."
        }
    }

    override val constraints by ::_constraints
    override val objectiveFunctions by ::_objectiveFunctions

    private val priorityToIndex = objectiveLocation
        .withIndex()
        .associate { (index, location) -> location.priority to index }

    private val defaultLocation: MultiObjectLocation
        get() = objectiveLocation.first()

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution> {
        return (UInt64.zero until initialSolutionAmount).map { solution ->
            (UInt64.zero until UInt64(tokens.tokensInSolver.size)).map { variable ->
                initialSolutionsGenerator(solution to variable)
            }
        }
    }

    override fun objectiveValue(obj: MulObj): List<Flt64> {
        val value = MutableList(objectiveSize) { Flt64.zero }
        for ((location, objective) in obj) {
            val index = priorityToIndex[location.priority] ?: continue
            value[index] = value[index] + objective * location.weight
        }
        return value
    }

    override fun compareObjective(lhs: List<Flt64>, rhs: List<Flt64>): Order? {
        val size = minOf(lhs.size, rhs.size)
        for (i in 0 until size) {
            val l = lhs[i]
            val r = rhs[i]
            if (l eq r) {
                continue
            }

            return when (objectCategory) {
                ObjectCategory.Minimum -> {
                    if (l ls r) {
                        Order.Less()
                    } else {
                        Order.Greater()
                    }
                }

                ObjectCategory.Maximum -> {
                    if (l gr r) {
                        Order.Less()
                    } else {
                        Order.Greater()
                    }
                }
            }
        }

        return if (lhs.size < rhs.size) {
            Order.Less()
        } else if (lhs.size > rhs.size) {
            Order.Greater()
        } else {
            Order.Equal
        }
    }

    override fun add(item: AbstractVariableItem<*, *>): Try {
        tokens.add(item)
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        tokens.add(items)
        return ok
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    @Suppress("UNUSED_PARAMETER")
    fun addConstraint(
        inequality: LinearConstraintInput,
        name: String? = null,
        displayName: String? = null
    ) {
        _constraints.add(
            Pair(
                { solution: Solution -> inequality.isTrue(solution, tokens) },
                name ?: String()
            )
        )
    }

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            expression = LinearPolynomial(variable),
            location = defaultLocation,
            name = name,
            displayName = displayName
        )
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            expression = LinearPolynomial(constant),
            location = defaultLocation,
            name = name,
            displayName = displayName
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        expression: Expression,
        location: MultiObjectLocation,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            func = { solution -> expression.evaluate(solution, tokens) },
            location = location,
            name = name
        )
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: Polynomial<*, *, *>,
        location: MultiObjectLocation,
        name: String? = null
    ): Try {
        return addObject(
            category = category,
            func = { solution -> polynomial.evaluate(solution, tokens) },
            location = location,
            name = name
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        func: Extractor<Flt64?, Solution>,
        location: MultiObjectLocation,
        name: String? = null,
        displayName: String? = null
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution ->
                    func(solution)?.let {
                        if (category == objectCategory) {
                            listOf(location to it)
                        } else {
                            listOf(location to -it)
                        }
                    }
                },
                name ?: String()
            )
        )
        return ok
    }

    fun maximize(
        location: MultiObjectLocation,
        expression: Expression,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            expression = expression,
            location = location,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        location: MultiObjectLocation,
        expression: Expression,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            expression = expression,
            location = location,
            name = name,
            displayName = displayName
        )
    }

    override fun setSolution(solution: Solution) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        tokens.setSolution(solution)
    }

    override fun flush() {
        tokens.flush()
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }
}




