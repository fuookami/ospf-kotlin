@file:Suppress("unused", "DEPRECATION")

package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.utils.functional.MultiMap2
import fuookami.ospf.kotlin.utils.functional.MultiMap3
import fuookami.ospf.kotlin.utils.functional.MultiMap4
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue

typealias Solution<V> = List<V>

interface Model<V> : AddableTokenCollection<V> where V : RealNumber<V>, V : NumberField<V> {
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

    fun setSolution(solution: List<V>)
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>)
    fun clearSolution()
}

interface LinearModel<V> : Model<V> where V : RealNumber<V>, V : NumberField<V> {
    val converter: IntoValue<V>

    /**
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = LinearFlattenData<V>(listOf(LinearMonomial(converter.one, variable)), converter.zero),
            name = name ?: "",
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
            flattenData = LinearFlattenData<V>(emptyList(), converter.intoValue(constant.toFlt64())),
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * Add objective using LinearFlattenData<V>.
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads ==========

    fun minimize(
        polynomial: LinearPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: LinearPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun minimize(
        monomial: LinearMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        monomial: LinearMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== Unified entry points ==========

    fun minimize(
        symbol: LinearIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toLinearPolynomial()
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        symbol: LinearIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toLinearPolynomial()
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }
}

interface QuadraticModel<V> : LinearModel<V> where V : RealNumber<V>, V : NumberField<V> {
    /**
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = QuadraticFlattenData<V>(listOf(QuadraticMonomial(converter.one, variable)), converter.zero),
            name = name ?: "",
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
            flattenData = QuadraticFlattenData<V>(emptyList(), converter.intoValue(constant.toFlt64())),
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * Add objective using QuadraticFlattenData<V>.
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads for Quadratic ==========

    fun minimize(
        polynomial: QuadraticPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: QuadraticPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun minimize(
        monomial: QuadraticMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        monomial: QuadraticMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== Unified entry points ==========

    fun minimize(
        symbol: QuadraticIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toQuadraticPolynomial()
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        symbol: QuadraticIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toQuadraticPolynomial()
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }
}

@kotlin.Deprecated("Use addObject(category, flattenData: LinearFlattenData<V>) instead.", level = DeprecationLevel.WARNING)
fun <V> LinearModel<V>.addObject(
    category: ObjectCategory,
    flattenData: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    name: String = "",
    displayName: String? = null
): Try where V : RealNumber<V>, V : NumberField<V> {
    return addObject(
        category = category,
        flattenData = LinearFlattenData(
            monomials = flattenData.monomials.map { LinearMonomial(converter.intoValue(it.coefficient), it.symbol) },
            constant = converter.intoValue(flattenData.constant)
        ),
        name = name,
        displayName = displayName
    )
}

@kotlin.Deprecated("Use addObject(category, flattenData: QuadraticFlattenData<V>) instead.", level = DeprecationLevel.WARNING)
fun <V> QuadraticModel<V>.addObject(
    category: ObjectCategory,
    flattenData: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    name: String = "",
    displayName: String? = null
): Try where V : RealNumber<V>, V : NumberField<V> {
    return addObject(
        category = category,
        flattenData = QuadraticFlattenData(
            monomials = flattenData.monomials.map {
                QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2)
            },
            constant = converter.intoValue(flattenData.constant)
        ),
        name = name,
        displayName = displayName
    )
}




