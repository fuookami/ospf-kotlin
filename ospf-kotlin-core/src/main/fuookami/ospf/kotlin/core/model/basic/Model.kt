@file:Suppress("unused", "DEPRECATION")

package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.core.model.mechanism.eq
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbolFlt64
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
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

typealias Solution<V> = List<V>
typealias SolutionFlt64 = Solution<Flt64>

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
    fun addConstraint(
        constraint: AbstractVariableItem<*, *>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        // Adapter boundary: Symbol.eq(Boolean) returns Flt64LinearInequality; safe when V=Flt64.
        @Suppress("UNCHECKED_CAST")
        val relation = (constraint as fuookami.ospf.kotlin.math.symbol.Symbol).eq(true) as LinearInequality<V>
        return addConstraint(
            relation = relation,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

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
            flattenData = LinearFlattenDataFlt64(listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero),
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
            flattenData = LinearFlattenDataFlt64(emptyList(), constant.toFlt64()),
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * Add objective using LinearFlattenDataFlt64 (new API)
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenDataFlt64,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads ==========

    fun minimize(
        polynomial: LinearPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenDataFlt64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: LinearPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenDataFlt64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun minimize(
        monomial: LinearMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenDataFlt64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        monomial: LinearMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenDataFlt64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== Unified entry points ==========

    fun minimize(
        symbol: LinearIntermediateSymbol<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        @Suppress("UNCHECKED_CAST")
        return minimize(symbol.toLinearPolynomial() as LinearPolynomial<Flt64>, name, displayName)
    }

    fun maximize(
        symbol: LinearIntermediateSymbol<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        @Suppress("UNCHECKED_CAST")
        return maximize(symbol.toLinearPolynomial() as LinearPolynomial<Flt64>, name, displayName)
    }

    fun addConstraint(
        obj: LinearIntermediateSymbol<*>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        @Suppress("UNCHECKED_CAST")
        return addConstraint(obj.toMathLinearInequality() as LinearInequality<V>, lazy, name, displayName, withRangeSet)
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
            flattenData = QuadraticFlattenDataFlt64(listOf(QuadraticMonomial(Flt64.one, variable)), Flt64.zero),
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
            flattenData = QuadraticFlattenDataFlt64(emptyList(), constant.toFlt64()),
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * Add objective using QuadraticFlattenDataFlt64 (new API)
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenDataFlt64,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads for Quadratic ==========

    fun minimize(
        polynomial: QuadraticPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenDataFlt64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: QuadraticPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenDataFlt64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun minimize(
        monomial: QuadraticMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenDataFlt64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        monomial: QuadraticMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenDataFlt64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== Unified entry points ==========

    fun minimize(
        symbol: QuadraticIntermediateSymbol<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        @Suppress("UNCHECKED_CAST")
        return minimize(symbol.toQuadraticPolynomial() as QuadraticPolynomial<Flt64>, name, displayName)
    }

    fun maximize(
        symbol: QuadraticIntermediateSymbol<*>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        @Suppress("UNCHECKED_CAST")
        return maximize(symbol.toQuadraticPolynomial() as QuadraticPolynomial<Flt64>, name, displayName)
    }

    fun addConstraint(
        obj: QuadraticIntermediateSymbol<*>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        @Suppress("UNCHECKED_CAST")
        return addConstraint(obj.toMathQuadraticInequality() as QuadraticInequalityOf<V>, lazy, name, displayName, withRangeSet)
    }
}




