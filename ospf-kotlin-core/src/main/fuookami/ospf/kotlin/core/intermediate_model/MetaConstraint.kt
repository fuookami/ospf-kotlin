@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_model.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.intermediate_model.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.intermediate_symbol.function.asMathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as UtilsQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64

interface MetaConstraintGroup {
    val lazy: Boolean get() = false
    val name: String

    fun MetaModel.registerConstraintGroup() {
        this.registerConstraintGroup(this@MetaConstraintGroup)
    }

    fun MetaModel.indicesOfConstraintGroup(): IntRange? {
        return this.indicesOfConstraintGroup(this@MetaConstraintGroup)
    }

    fun MetaModel.constraintsOfGroup(): List<MathConstraint> {
        return indicesOfConstraintGroup(this@MetaConstraintGroup)?.let { indices ->
            indices.map { constraints[it] }
        } ?: emptyList()
    }

    fun AbstractLinearMetaModel.addConstraint(
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

    fun AbstractLinearMetaModel.addConstraint(
        constraint: LinearMonomial,
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

    fun AbstractLinearMetaModel.addConstraint(
        constraint: AbstractLinearPolynomial<*>,
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

    fun AbstractLinearMetaModel.addConstraint(
        constraint: LinearIntermediateSymbol,
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
    fun AbstractLinearMetaModel.partition(
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
    fun AbstractLinearMetaModel.partition(
        symbols: Iterable<LinearIntermediateSymbol>,
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

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun AbstractLinearMetaModel.partition(
        monomials: Iterable<LinearMonomial>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsLinearPolynomial(
                monomials = monomials.map { it.toUtilsMonomial() }.toList(),
                constant = Flt64.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun AbstractLinearMetaModel.partition(
        polynomial: AbstractLinearPolynomial<*>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation = polynomial.asMathLinearPolynomial() eq Flt64.one,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: QuadraticMonomial,
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

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: AbstractQuadraticPolynomial<*>,
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

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: QuadraticIntermediateSymbol,
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
    @JvmName("partitionQuadraticMonomials")
    fun AbstractQuadraticMetaModel.partition(
        monomials: Iterable<QuadraticMonomial>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsQuadraticPolynomial(
                monomials = monomials.map { UtilsQuadraticMonomial(it.coefficient, it.toUtilsMonomial().symbol1, it.toUtilsMonomial().symbol2) }.toList(),
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
    @JvmName("partitionQuadraticSymbols")
    fun AbstractQuadraticMetaModel.partition(
        symbols: Iterable<QuadraticIntermediateSymbol>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = UtilsQuadraticPolynomial(
                monomials = symbols.flatMap { it.toQuadraticPolynomial().monomials }.toList(),
                constant = Flt64.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun AbstractQuadraticMetaModel.partition(
        polynomial: AbstractQuadraticPolynomial<*>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation = polynomial.toUtilsPolynomial() eq Flt64.one,
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
    fun AbstractLinearMetaModel.addConstraint(
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
    fun AbstractQuadraticMetaModel.addConstraint(
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

@Deprecated("Use MathConstraint instead", ReplaceWith("MathConstraint"))
data class MetaConstraint<Ineq>(
    val constraint: Ineq,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    override fun isTrue(solution: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Boolean? {
        @Suppress("UNCHECKED_CAST")
        val c = constraint as? MathConstraint ?: return null
        return c.isTrue(solution, tokenTable, zeroIfNone)
    }

    override fun toString(): String {
        return constraint.toString()
    }
}

// ========== Math Inequality-based Constraint Types ==========

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
    fun isTrue(solution: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Boolean?
}

/**
 * LinearInequalityConstraint - Constraint using math LinearInequality
 *
 * This type uses LinearFlattenData directly, avoiding dependency on frontend/inequality.
 */
data class LinearInequalityConstraint(
    val inequality: MathLinearInequality,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    val flattenData: LinearFlattenData get() = inequality.flattenData
    val sign: Comparison get() = inequality.comparison
    val name: String = ""
    val displayName: String? = null

    override fun isTrue(solution: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Boolean? {
        val lhsValue = evaluateFlattenData(flattenData, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

/**
 * QuadraticInequalityConstraint - Constraint using math QuadraticInequality
 *
 * This type uses QuadraticFlattenData directly, avoiding dependency on frontend/inequality.
 */
data class QuadraticInequalityConstraint(
    val inequality: MathQuadraticInequality,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    val flattenData: QuadraticFlattenData get() = inequality.flattenData
    val sign: Comparison get() = inequality.comparison
    val name: String = ""
    val displayName: String? = null

    override fun isTrue(solution: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Boolean? {
        val lhsValue = evaluateQuadraticFlattenData(flattenData, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue, Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

// Deprecated type aliases for backward compatibility
@Deprecated("Use LinearInequalityConstraint instead", ReplaceWith("LinearInequalityConstraint"))
typealias LinearRelationConstraint = LinearInequalityConstraint

@Deprecated("Use QuadraticInequalityConstraint instead", ReplaceWith("QuadraticInequalityConstraint"))
typealias QuadraticRelationConstraint = QuadraticInequalityConstraint

// ========== Deprecated adapters ==========
// Removed: toRelationConstraint() functions and Sign.toComparison() - no longer needed
// since frontend.inequality types are being deleted.

// ========== NEW FlattenData-based SubObject Types ==========

/**
 * LinearFlattenSubObject - SubObject using LinearFlattenData (new API)
 *
 * This type uses LinearFlattenData directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class LinearFlattenSubObject(
    val category: ObjectCategory,
    val flattenData: LinearFlattenData,
    val name: String = "",
    val displayName: String? = null
)

/**
 * QuadraticFlattenSubObject - SubObject using QuadraticFlattenData (new API)
 *
 * This type uses QuadraticFlattenData directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class QuadraticFlattenSubObject(
    val category: ObjectCategory,
    val flattenData: QuadraticFlattenData,
    val name: String = "",
    val displayName: String? = null
)




