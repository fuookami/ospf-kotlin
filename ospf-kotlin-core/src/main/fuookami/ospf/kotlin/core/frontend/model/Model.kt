package fuookami.ospf.kotlin.core.frontend.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

typealias Solution = List<Flt64>

interface Model : AddableTokenCollection {
    val objectCategory: ObjectCategory

    fun remove(item: AbstractVariableItem<*, *>)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariablesLists")
    fun add(items: Iterable<Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariables")
    fun <K> add(items: Map<K, AbstractVariableItem<*, *>>): Try {
        return add(items.values)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariablesLists")
    fun <K> add(items: Map<K, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2Variables")
    fun <K1, K2> add(items: MultiMap2<K1, K2, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2VariableLists")
    fun <K1, K2> add(items: MultiMap2<K1, K2, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3Variables")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3VariableLists")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4Variables")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4VariableLists")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariable")
    fun add(item: Quantity<AbstractVariableItem<*, *>>): Try {
        return add(item.value)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariables")
    fun add(items: Iterable<Quantity<AbstractVariableItem<*, *>>>): Try {
        return add(items.map { it.value })
    }

    fun remove(item: Quantity<AbstractVariableItem<*, *>>) {
        return remove(item.value)
    }

    fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun minimize(
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            variable = variable,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            variable = variable,
            name = name,
            displayName = displayName
        )
    }

    fun <T : RealNumber<T>> minimize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    fun <T : RealNumber<T>> maximize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    fun setSolution(solution: Solution)
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>)
    fun clearSolution()
}

interface LinearModel : Model {
    fun addConstraint(
        constraint: AbstractVariableItem<*, *>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: LinearMonomial,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: AbstractLinearPolynomial<*>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: LinearIntermediateSymbol,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: LinearInequality,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = sum(variables),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun partition(
        symbols: Iterable<LinearIntermediateSymbol>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = sum(symbols),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun partition(
        monomials: Iterable<LinearMonomial>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = sum(monomials),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    fun partition(
        polynomial: AbstractLinearPolynomial<*>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addConstraint(
            constraint = polynomial eq true,
            lazy = lazy,
            name = name,
            displayName = displayName
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
            polynomial = LinearPolynomial(variable),
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
            polynomial = LinearPolynomial(constant),
            name = name,
            displayName = displayName
        )
    }

    fun addObject(
        category: ObjectCategory,
        monomial: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            polynomial = LinearPolynomial(monomial),
            name = name,
            displayName = displayName
        )
    }

    fun addObject(
        category: ObjectCategory,
        symbol: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            polynomial = LinearPolynomial(symbol),
            name = name,
            displayName = displayName
        )
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun minimize(
        monomial: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            monomial = monomial,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        monomial: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            monomial = monomial,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        symbol: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            symbol = symbol,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        symbol: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            symbol = symbol,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            polynomial = polynomial,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            polynomial = polynomial,
            name = name,
            displayName = displayName
        )
    }
}

interface QuadraticModel : LinearModel {
    fun addConstraint(
        constraint: QuadraticMonomial,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    override fun addConstraint(
        constraint: LinearInequality,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            constraint = QuadraticInequality(constraint),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: AbstractQuadraticPolynomial<*>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: QuadraticIntermediateSymbol,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            constraint = constraint eq true,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: QuadraticInequality,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticMonomials")
    fun partition(
        monomials: Iterable<QuadraticMonomial>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = qsum(monomials),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = qsum(symbols),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    fun partition(
        polynomial: AbstractQuadraticPolynomial<*>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addConstraint(
            constraint = polynomial eq Flt64.one,
            lazy = lazy,
            name = name,
            displayName = displayName
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
            polynomial = QuadraticPolynomial(variable),
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
            polynomial = QuadraticPolynomial(constant),
            name = name,
            displayName = displayName
        )
    }

    override fun addObject(
        category: ObjectCategory,
        monomial: LinearMonomial,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            polynomial = QuadraticPolynomial(monomial),
            name = name,
            displayName = displayName
        )
    }

    override fun addObject(
        category: ObjectCategory,
        symbol: LinearIntermediateSymbol,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            polynomial = QuadraticPolynomial(symbol),
            name = name,
            displayName = displayName
        )
    }

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ) : Try {
        return addObject(
            category = category,
            polynomial = QuadraticPolynomial(polynomial),
            name = name,
            displayName = displayName
        )
    }

    fun addObject(
        category: ObjectCategory,
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            polynomial = QuadraticPolynomial(monomial),
            name = name,
            displayName = displayName
        )
    }

    fun addObject(
        category: ObjectCategory,
        symbol: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            polynomial = QuadraticPolynomial(symbol),
            name = name,
            displayName = displayName
        )
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun minimize(
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            monomial = monomial,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            monomial = monomial,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        symbol: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            symbol = symbol,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        symbol: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            symbol = symbol,
            name = name,
            displayName = displayName
        )
    }

    fun minimize(
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            polynomial = polynomial,
            name = name,
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            polynomial = polynomial,
            name = name,
            displayName = displayName
        )
    }
}
