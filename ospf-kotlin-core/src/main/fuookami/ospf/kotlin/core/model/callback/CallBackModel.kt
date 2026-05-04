@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.core.token.AbstractMutableTokenTable
import fuookami.ospf.kotlin.core.token.TokenTable
import fuookami.ospf.kotlin.core.token.ManualTokenTable
import fuookami.ospf.kotlin.core.token.ConcurrentManualAddTokenTable
import fuookami.ospf.kotlin.core.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.model.mechanism.AbstractMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractMetaModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.ConstraintImpl
import fuookami.ospf.kotlin.core.model.mechanism.SubObject
import fuookami.ospf.kotlin.core.model.mechanism.SingleObjectMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.SingleObjectMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.MulObj
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Nonlinear
import fuookami.ospf.kotlin.utils.functional.Order

interface CallBackModelPolicy<V> where V : RealNumber<V>, V : NumberField<V> {
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

    fun initialSolutions(initialSolutionAmount: UInt64, variableAmount: UInt64): List<Solution<V>> {
        return listOf((UInt64.zero until variableAmount).map { _ -> throw UnsupportedOperationException("no initialSolutionsGenerator provided") })
    }
}

class FunctionalCallBackModelPolicy<V>(
    val objectiveComparator: PartialComparator<V>,
    private val _initialSolutionsGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null
) : CallBackModelPolicy<V> where V : RealNumber<V>, V : NumberField<V> {

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
    ): List<Solution<V>> {
        val gen = _initialSolutionsGenerator ?: return emptyList()
        return (UInt64.zero until initialSolutionAmount).map { solution ->
            (UInt64.zero until variableAmount).map {
                gen(Pair(solution, it))
            }
        }
    }
}

class CallBackModel<V> internal constructor(
    category: Category = Nonlinear,
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val tokens: AbstractMutableTokenTable<V> = ManualTokenTable(category),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution<V>>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<V?, Solution<V>>, String>> = ArrayList(),
    private val policy: CallBackModelPolicy<V>,
    private val _converter: IntoValue<V>
) : CallBackModelInterfaceV<V> where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        private fun <V> dumpObjectiveComparator(
            category: ObjectCategory,
            converter: IntoValue<V>
        ): PartialComparator<V> where V : RealNumber<V>, V : NumberField<V> = when (category) {
            ObjectCategory.Maximum -> { lhs, rhs -> converter.fromValue(lhs) geq converter.fromValue(rhs) }
            ObjectCategory.Minimum -> { lhs, rhs -> converter.fromValue(lhs) leq converter.fromValue(rhs) }
        }

        operator fun <V> invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ) where V : RealNumber<V>, V : NumberField<V> = CallBackModel(
            objectCategory = objectCategory,
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = dumpObjectiveComparator(objectCategory, converter),
                _initialSolutionsGenerator = initialSolutionGenerator
            ),
            _converter = converter
        )

        operator fun <V> invoke(
            objectiveComparator: PartialComparator<V>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ) where V : RealNumber<V>, V : NumberField<V> = CallBackModel(
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = objectiveComparator,
                _initialSolutionsGenerator = initialSolutionGenerator
            ),
            _converter = converter
        )

        operator fun invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ): CallBackModel<Flt64> = CallBackModel(
            objectCategory = objectCategory,
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = dumpObjectiveComparator(objectCategory, IntoValue.Flt64),
                _initialSolutionsGenerator = initialSolutionGenerator
            ),
            _converter = IntoValue.Flt64
        )

        operator fun invoke(
            objectiveComparator: PartialComparator<Flt64>,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ): CallBackModel<Flt64> = CallBackModel(
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = objectiveComparator,
                _initialSolutionsGenerator = initialSolutionGenerator
            ),
            _converter = IntoValue.Flt64
        )

        operator fun <V> invoke(
            model: AbstractMetaModel<V>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>> = { _ -> throw UnsupportedOperationException("no initialSolutionsGenerator provided") },
            converter: IntoValue<V>
        ): CallBackModel<V> where V : RealNumber<V>, V : NumberField<V> {
            val tokens = model.tokens.copy()
            val constraints = model.constraints.map { constraint ->
                Pair(
                    { solution: Solution<V> -> constraint.isTrue(solution, converter, tokens) },
                    constraint.toString()
                )
            }.toMutableList()
            val objectiveFunction = model.subObjects.map { objective ->
                Pair(
                    { solution: Solution<V> ->
                        if (objective.category == model.objectCategory) {
                            objective.evaluate(solution)
                        } else {
                            objective.evaluate(solution)?.let { -it }
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
                    dumpObjectiveComparator(model.objectCategory, converter),
                    _initialSolutionsGenerator = initialSolutionGenerator
                ),
                _converter = converter
            )
        }

        operator fun invoke(
            model: AbstractMetaModelFlt64,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ): CallBackModel<Flt64> = invoke(model, initialSolutionGenerator, IntoValue.Flt64)

        operator fun <V> invoke(
            model: SingleObjectMechanismModel<V>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>> = { _ -> throw UnsupportedOperationException("no initialSolutionsGenerator provided") },
            concurrent: Boolean = true,
            converter: IntoValue<V>
        ): CallBackModel<V> where V : RealNumber<V>, V : NumberField<V> {
            val tokens = if (concurrent) {
                ConcurrentManualAddTokenTable<V>(model.tokens.category)
            } else {
                ManualTokenTable<V>(model.tokens.category)
            }
            val constraints = model.constraints.map { constraint ->
                @Suppress("UNCHECKED_CAST")
                val impl = constraint as ConstraintImpl<V, *>
                Pair(
                    { solution: Solution<V> -> impl.isTrue(solution) },
                    constraint.name
                )
            }.toMutableList()
            @Suppress("UNCHECKED_CAST")
            val subObjects = model.objectFunction.subObjects as List<SubObject<V>>
            val objectiveFunction = subObjects.map { objective ->
                Pair(
                    { solution: Solution<V> ->
                        if (objective.category == model.objectFunction.category) {
                            objective.evaluate(solution)
                        } else {
                            objective.evaluate(solution)?.let { -it }
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
                    dumpObjectiveComparator(model.objectFunction.category, converter),
                    _initialSolutionsGenerator = initialSolutionGenerator
                ),
                _converter = converter
            )
        }

        operator fun invoke(
            model: SingleObjectMechanismModelFlt64,
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero },
            concurrent: Boolean = true
        ): CallBackModel<Flt64> = invoke(model, initialSolutionGenerator, concurrent, IntoValue.Flt64)
    }

    override val constraints by ::_constraints
    override val objectiveFunctions by ::_objectiveFunctions

    override fun converter(): IntoValue<V> = _converter

    override fun negativeInfinity(): V = _converter.negativeInfinity

    override fun infinity(): V = _converter.infinity

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution<V>> {
        return policy.initialSolutions(initialSolutionAmount, UInt64(tokens.tokensInSolver.size))
    }

    override fun compareObjective(lhs: V, rhs: V): Order {
        return policy.comparator(lhs, rhs)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("compareObjectiveNullable")
    override fun compareObjective(lhs: V?, rhs: V?): Order? {
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
                { solution: Solution<V> -> inequality.isTrue(solution, _converter, tokens) },
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
            func = { solution: Solution<V> -> tokens.find(variable)?.result },
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
        val vConstant = _converter.intoValue(constant.toFlt64())
        return addObject(
            category = category,
            func = { solution: Solution<V> -> vConstant },
            name = name,
            displayName = displayName
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        func: Extractor<V?, Solution<V>>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution<V> ->
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
        func: Extractor<V?, Solution<V>>,
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
        func: Extractor<V?, Solution<V>>,
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

    override fun setSolution(solution: List<V>) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        tokens.setSolution(solution)
    }

    override fun flush() {
        tokens.flush()
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }
}

typealias CallBackModelFlt64 = CallBackModel<Flt64>

class MultiObjectCallBackModel<V> internal constructor(
    category: Category = Nonlinear,
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val objectiveLocation: List<MultiObjectLocation>,
    override val tokens: AbstractMutableTokenTable<V> = ManualTokenTable(category),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution<V>>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<MulObj?, Solution<V>>, String>> = ArrayList(),
    private val _initialSolutionsGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
    private val _converter: IntoValue<V>
) : MultiObjectiveModelInterfaceV<V> where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            objectiveLocation: List<MultiObjectLocation> = listOf(MultiObjectLocation(UInt64.zero, Flt64.one)),
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ) where V : RealNumber<V>, V : NumberField<V> = MultiObjectCallBackModel(
            objectCategory = objectCategory,
            objectiveLocation = objectiveLocation,
            _initialSolutionsGenerator = initialSolutionGenerator,
            _converter = converter
        )

        operator fun invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            objectiveLocation: List<MultiObjectLocation> = listOf(MultiObjectLocation(UInt64.zero, Flt64.one)),
            initialSolutionGenerator: Extractor<Flt64, Pair<UInt64, UInt64>> = { Flt64.zero }
        ): MultiObjectCallBackModel<Flt64> = MultiObjectCallBackModel(
            objectCategory = objectCategory,
            objectiveLocation = objectiveLocation,
            _initialSolutionsGenerator = { pair -> initialSolutionGenerator(pair) },
            _converter = IntoValue.Flt64
        )
    }

    init {
        require(objectiveLocation.isNotEmpty()) {
            "objectiveLocation can not be empty."
        }
    }

    override val constraints by ::_constraints
    override val objectiveFunctions by ::_objectiveFunctions

    override fun converter(): IntoValue<V> = _converter

    override fun negativeInfinity(): V = _converter.negativeInfinity

    override fun infinity(): V = _converter.infinity

    private val priorityToIndex = objectiveLocation
        .withIndex()
        .associate { (index, location) -> location.priority to index }

  private val defaultLocation: MultiObjectLocation
        get() = objectiveLocation.first()

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution<V>> {
        val gen = _initialSolutionsGenerator ?: return emptyList()
        return (UInt64.zero until initialSolutionAmount).map { solution ->
            (UInt64.zero until UInt64(tokens.tokensInSolver.size)).map { variable ->
                gen(solution to variable)
            }
        }
    }

    override fun objectiveValue(obj: MulObj): List<V> {
        val value = MutableList(objectiveSize) { _converter.zero }
        for ((location, objective) in obj) {
            val index = priorityToIndex[location.priority] ?: continue
            value[index] = value[index] + _converter.intoValue(objective) * _converter.intoValue(location.weight)
        }
        return value
    }

    override fun compareObjective(lhs: List<V>, rhs: List<V>): Order? {
        val size = minOf(lhs.size, rhs.size)
        for (i in 0 until size) {
            val l = _converter.fromValue(lhs[i])
            val r = _converter.fromValue(rhs[i])
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
                { solution: Solution<V> -> inequality.isTrue(solution, _converter, tokens) },
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
            func = { solution: Solution<V> -> tokens.find(variable)?.result },
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
        val vConstant = _converter.intoValue(constant.toFlt64())
        return addObject(
            category = category,
            func = { solution: Solution<V> -> vConstant },
            location = defaultLocation,
            name = name,
            displayName = displayName
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        func: Extractor<V?, Solution<V>>,
        location: MultiObjectLocation,
        name: String? = null,
        displayName: String? = null
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution<V> ->
                    func(solution)?.let {
                        val fltValue = _converter.fromValue(if (category == objectCategory) it else -it)
                        listOf(location to fltValue)
                    }
                },
                name ?: String()
            )
        )
        return ok
    }

    override fun setSolution(solution: List<V>) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        tokens.setSolution(solution)
    }

    override fun flush() {
        tokens.flush()
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }
}

typealias MultiObjectCallBackModelFlt64 = MultiObjectCallBackModel<Flt64>