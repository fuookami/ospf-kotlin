@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_model.*
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_model.ObjectCategory
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomial
import fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.AddableTokenCollectionF64
import fuookami.ospf.kotlin.utils.functional.MultiMap2
import fuookami.ospf.kotlin.utils.functional.MultiMap3
import fuookami.ospf.kotlin.utils.functional.MultiMap4
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

typealias Solution = List<Flt64>

interface Model : AddableTokenCollectionF64 {
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
            relation = (constraint as fuookami.ospf.kotlin.math.symbol.Symbol).eq(true),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    @Deprecated(
        message = "Use addConstraint(relation: MathLinearInequality) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addConstraint(constraint.toMathLinearInequality(), lazy, name, displayName, withRangeSet)",
            "fuookami.ospf.kotlin.core.intermediate_model.MathInequalityDslKt"
        )
    )
    fun addConstraint(
        constraint: LinearMonomial,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation = MathLinearPolynomial(constraint.flattenedMonomials.monomials, Flt64.zero) eq Flt64.one,
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }


    @Deprecated(
        message = "Use addConstraint(relation: MathLinearInequality) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addConstraint(constraint.toMathLinearInequality(), lazy, name, displayName, withRangeSet)",
            "fuookami.ospf.kotlin.core.intermediate_model.MathInequalityDslKt"
        )
    )
    fun addConstraint(
        constraint: LinearIntermediateSymbol<Flt64>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation = constraint.toMathLinearInequality(),
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
        relation: MathLinearInequality,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try

    @Deprecated(
        message = "Use addConstraint(relation: MathLinearInequality) with math.symbol types instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addConstraint(relation = variables.map { MathLinearMonomial(Flt64.one, it) }.sumOf { it }.le(Flt64.one), lazy, name, displayName)",
            "fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial",
            "fuookami.ospf.kotlin.math.symbol.inequality.le",
            "fuookami.ospf.kotlin.math.algebra.number.Flt64"
        )
    )
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = MathLinearPolynomial(variables.map { MathLinearMonomial(Flt64.one, it) }, Flt64.zero)
        return addConstraint(
            relation = polynomial le Flt64.one,
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addConstraint(relation: MathLinearInequality) with math.symbol types instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun partition(
        symbols: Iterable<LinearIntermediateSymbol<*>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = MathLinearPolynomial(symbols.flatMap { it.flattenedMonomials.monomials }, Flt64.zero)
        return addConstraint(
            relation = polynomial le Flt64.one,
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addConstraint(relation: MathLinearInequality) with math.symbol types instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun partition(
        monomials: Iterable<LinearMonomial>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = MathLinearPolynomial(monomials.flatMap { it.flattenedMonomials.monomials }, Flt64.zero)
        return addConstraint(
            relation = polynomial le Flt64.one,
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
            flattenData = LinearFlattenDataF64(listOf(MathLinearMonomial(Flt64.one, variable)), Flt64.zero),
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
            flattenData = LinearFlattenDataF64(emptyList(), constant.toFlt64()),
            name = name ?: "",
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addObject(flattenData: LinearFlattenDataF64) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addObject(category = category, flattenData = LinearFlattenDataF64(listOf(MathLinearMonomial(Flt64.one, monomial)), Flt64.zero), name, displayName)",
            "fuookami.ospf.kotlin.core.intermediate_model.LinearFlattenDataF64"
        )
    )
    fun addObject(
        category: ObjectCategory,
        monomial: LinearMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            flattenData = monomial.flattenedMonomials,
            name = name ?: "",
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addObject(flattenData: LinearFlattenDataF64) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun addObject(
        category: ObjectCategory,
        symbol: LinearIntermediateSymbol<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            flattenData = symbol.flattenedMonomials,
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * Add objective using LinearFlattenDataF64 (new API)
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenDataF64,
        name: String = "",
        displayName: String? = null
    ): Try

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathLinearPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
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

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathLinearPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
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

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathLinearPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun minimize(
        symbol: LinearIntermediateSymbol<Flt64>,
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

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathLinearPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun maximize(
        symbol: LinearIntermediateSymbol<Flt64>,
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

    // ========== math.symbol type overloads ==========

    fun minimize(
        polynomial: MathLinearPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenDataF64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: MathLinearPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenDataF64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun minimize(
        monomial: MathLinearMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenDataF64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        monomial: MathLinearMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenDataF64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }
}

interface QuadraticModel : LinearModel {
    @Deprecated(
        message = "Use addConstraint(relation: MathQuadraticInequality) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun addConstraint(
        constraint: QuadraticMonomial,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            relation = (constraint as fuookami.ospf.kotlin.core.intermediate_model.ToQuadraticPolynomial).toMathQuadraticInequality(),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    override fun addConstraint(
        relation: MathLinearInequality,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            relation = MathQuadraticInequality(
                lhs = relation.lhs.toQuadraticPolynomial(),
                rhs = relation.rhs.toQuadraticPolynomial(),
                comparison = relation.comparison
            ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    @Deprecated(
        message = "Use addConstraint(relation: MathQuadraticInequality) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun addConstraint(
        constraint: QuadraticIntermediateSymbol<Flt64>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            relation = (constraint as fuookami.ospf.kotlin.core.intermediate_model.ToQuadraticPolynomial).toMathQuadraticInequality(),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: MathQuadraticInequality,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try

    @Deprecated(
        message = "Use addConstraint(relation: MathQuadraticInequality) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addConstraint(relation = monomials.map { MathQuadraticMonomial(it.coefficient, it.symbol1, it.symbol2) }.sumOf { it }.le(Flt64.one), lazy, name, displayName)",
            "fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial",
            "fuookami.ospf.kotlin.math.symbol.inequality.le",
            "fuookami.ospf.kotlin.math.algebra.number.Flt64"
        )
    )
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticMonomials")
    fun partition(
        monomials: Iterable<QuadraticMonomial>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = MathQuadraticPolynomial(monomials.flatMap { it.flattenedMonomials.monomials }, Flt64.zero)
        return addConstraint(
            relation = polynomial le Flt64.one,
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addConstraint(relation: MathQuadraticInequality) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addConstraint(relation = symbols.map { MathQuadraticMonomial(Flt64.one, it) }.sumOf { it }.le(Flt64.one), lazy, name, displayName)",
            "fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial",
            "fuookami.ospf.kotlin.math.symbol.inequality.le",
            "fuookami.ospf.kotlin.math.algebra.number.Flt64"
        )
    )
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol<*>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = MathQuadraticPolynomial(symbols.flatMap { it.flattenedMonomials.monomials }, Flt64.zero)
        return addConstraint(
            relation = polynomial le Flt64.one,
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
            flattenData = QuadraticFlattenDataF64(listOf(MathQuadraticMonomial(Flt64.one, variable)), Flt64.zero),
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
            flattenData = QuadraticFlattenDataF64(emptyList(), constant.toFlt64()),
            name = name ?: "",
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addObject(flattenData: QuadraticFlattenDataF64) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addObject(category = category, flattenData = QuadraticFlattenDataF64(monomial.toQuadraticMonomialCells(), Flt64.zero), name, displayName)",
            "fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64"
        )
    )
    override fun addObject(
        category: ObjectCategory,
        monomial: LinearMonomial,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = monomial.flattenedMonomials.toQuadraticFlattenData(),
            name = name ?: "",
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addObject(flattenData: QuadraticFlattenDataF64) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    override fun addObject(
        category: ObjectCategory,
        symbol: LinearIntermediateSymbol<Flt64>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = symbol.flattenedMonomials.toQuadraticFlattenData(),
            name = name ?: "",
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addObject(flattenData: QuadraticFlattenDataF64) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith(
            "addObject(category = category, flattenData = QuadraticFlattenDataF64(listOf(MathQuadraticMonomial(monomial.coefficient, monomial.symbol)), monomial.constant), name, displayName)",
            "fuookami.ospf.kotlin.core.intermediate_model.QuadraticFlattenDataF64"
        )
    )
    fun addObject(
        category: ObjectCategory,
        monomial: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            flattenData = monomial.flattenedMonomials,
            name = name ?: "",
            displayName = displayName
        )
    }

    @Deprecated(
        message = "Use addObject(flattenData: QuadraticFlattenDataF64) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun addObject(
        category: ObjectCategory,
        symbol: QuadraticIntermediateSymbol<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = category,
            flattenData = symbol.flattenedMonomials,
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * Add objective using QuadraticFlattenDataF64 (new API)
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenDataF64,
        name: String = "",
        displayName: String? = null
    ): Try

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathQuadraticPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
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

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathQuadraticPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
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

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathQuadraticPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun minimize(
        symbol: QuadraticIntermediateSymbol<Flt64>,
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

    @Deprecated(
        message = "Use minimize/maximize(polynomial: MathQuadraticPolynomial<Flt64>) instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun maximize(
        symbol: QuadraticIntermediateSymbol<Flt64>,
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

    // ========== math.symbol type overloads for Quadratic ==========

    fun minimize(
        polynomial: MathQuadraticPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenDataF64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        polynomial: MathQuadraticPolynomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenDataF64(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun minimize(
        monomial: MathQuadraticMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenDataF64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    fun maximize(
        monomial: MathQuadraticMonomial<Flt64>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenDataF64(listOf(monomial), Flt64.zero),
            name = name ?: "",
            displayName = displayName
        )
    }
}




