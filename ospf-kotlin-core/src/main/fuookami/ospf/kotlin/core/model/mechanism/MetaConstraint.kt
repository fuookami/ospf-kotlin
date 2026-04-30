package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolF64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbolF64
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

interface MetaConstraintGroup {
    val lazy: Boolean get() = false
    val name: String

    fun MetaModelF64.registerConstraintGroup() {
        this.registerConstraintGroup(this@MetaConstraintGroup)
    }

    fun MetaModelF64.indicesOfConstraintGroup(): IntRange? {
        return this.indicesOfConstraintGroup(this@MetaConstraintGroup)
    }

    fun MetaModelF64.constraintsOfGroup(): List<MathConstraint> {
        return indicesOfConstraintGroup(this@MetaConstraintGroup)?.let { indices ->
            indices.map { constraints[it] }
        } ?: emptyList()
    }

    fun AbstractLinearMetaModelF64.addConstraint(
        constraint: AbstractVariableItem<*, *>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return this.addConstraint(
            relation = constraint eq true,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun AbstractLinearMetaModelF64.addConstraint(
        constraint: LinearIntermediateSymbolF64,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            relation = constraint eq true,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun AbstractLinearMetaModelF64.partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsLinearPolynomial(
                monomials = variables.map { UtilsLinearMonomial(Flt64.one, it) }.toList(),
                constant = Flt64.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun AbstractLinearMetaModelF64.partition(
        symbols: Iterable<LinearIntermediateSymbolF64>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsLinearPolynomial(
                monomials = symbols.map { UtilsLinearMonomial(Flt64.one, it) }.toList(),
                constant = Flt64.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun AbstractQuadraticMetaModelF64.addConstraint(
        constraint: QuadraticIntermediateSymbolF64,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            relation = constraint eq true,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun AbstractQuadraticMetaModelF64.partition(
        symbols: Iterable<QuadraticIntermediateSymbolF64>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsQuadraticPolynomial(
                monomials = symbols.flatMap { it.toMathQuadraticPolynomial().monomials }.toList(),
                constant = Flt64.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    // ========== Math Inequality-based API ==========

    /**
     * Add constraint using math LinearInequality
     */
    fun AbstractLinearMetaModelF64.addConstraint(
        relation: MathLinearInequality,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return this.addConstraint(
            relation = relation,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * Add constraint using math QuadraticInequality
     */
    fun AbstractQuadraticMetaModelF64.addConstraint(
        relation: MathQuadraticInequality,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return this.addConstraint(
            relation = relation,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }
}

// ========== Math Inequality-based ConstraintF64 Types ==========

/**
 * Common interface for math-based constraints.
 */
interface MathConstraint {
    val group: MetaConstraintGroup?
    val lazy: Boolean
    val args: Any?
    val priority: Int?

    /**
     * Evaluate whether this constraint is satisfied given solution values.
     */
    fun <V> isTrue(
        solution: List<Flt64>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V>
}

/**
 * LinearInequalityConstraint - ConstraintF64 using math LinearInequality
 *
 * This type uses LinearFlattenDataFlt64 directly, avoiding dependency on frontend/inequality.
 */
data class LinearInequalityConstraint(
    val inequality: MathLinearInequality,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    val flattenData: LinearFlattenDataFlt64 get() = inequality.flattenData
    val sign: Comparison get() = inequality.comparison
    val name: String = ""
    val displayName: String? = null

    override fun <V> isTrue(
        solution: List<Flt64>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateFlattenDataWithResults(flattenData, solution, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

/**
 * QuadraticInequalityConstraint - ConstraintF64 using math QuadraticInequality
 *
 * This type uses QuadraticFlattenDataFlt64 directly, avoiding dependency on frontend/inequality.
 */
data class QuadraticInequalityConstraint(
    val inequality: MathQuadraticInequality,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    val flattenData: QuadraticFlattenDataFlt64 get() = inequality.flattenData
    val sign: Comparison get() = inequality.comparison
    val name: String = ""
    val displayName: String? = null

    override fun <V> isTrue(
        solution: List<Flt64>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): Boolean? where V : RealNumber<V>, V : NumberField<V> {
        val lhsValue = evaluateQuadraticFlattenDataWithResults(flattenData, solution, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

// ========== NEW FlattenData-based SubObject Types ==========

/**
 * LinearFlattenSubObject - SubObject using LinearFlattenDataFlt64 (new API)
 *
 * This type uses LinearFlattenDataFlt64 directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class LinearFlattenSubObject(
    val category: ObjectCategory,
    val flattenData: LinearFlattenDataFlt64,
    val name: String = "",
    val displayName: String? = null
)

/**
 * QuadraticFlattenSubObject - SubObject using QuadraticFlattenDataFlt64 (new API)
 *
 * This type uses QuadraticFlattenDataFlt64 directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class QuadraticFlattenSubObject(
    val category: ObjectCategory,
    val flattenData: QuadraticFlattenDataFlt64,
    val name: String = "",
    val displayName: String? = null
)




