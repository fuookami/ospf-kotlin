
package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
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
        constraint: LinearIntermediateSymbol<*>,
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
        symbols: Iterable<LinearIntermediateSymbol<*>>,
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

    @Deprecated(
        message = "Use partition with MathLinearInequality instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun AbstractLinearMetaModelF64.partition(
        polynomial: ToMathLinearInequality,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation = polynomial.toMathLinearInequality(),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun AbstractQuadraticMetaModelF64.addConstraint(
        constraint: QuadraticIntermediateSymbol<*>,
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
        symbols: Iterable<QuadraticIntermediateSymbol<*>>,
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

    @Deprecated(
        message = "Use partition with MathQuadraticInequality instead. Will be removed in E7.",
        level = DeprecationLevel.WARNING
    )
    fun AbstractQuadraticMetaModelF64.partition(
        polynomial: ToMathQuadraticInequality,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(
            relation = polynomial.toMathQuadraticInequality(),
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

@Deprecated("Use MathConstraint instead", ReplaceWith("MathConstraint"))
data class MetaConstraint<Ineq>(
    val constraint: Ineq,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    override fun isTrue(solution: List<Flt64>, tokenTable: LegacyAbstractTokenTable, zeroIfNone: Boolean): Boolean? {
        @Suppress("UNCHECKED_CAST")
        val c = constraint as? MathConstraint ?: return null
        return c.isTrue(solution, tokenTable, zeroIfNone)
    }

    override fun toString(): String {
        return constraint.toString()
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
    fun isTrue(solution: List<Flt64>, tokenTable: LegacyAbstractTokenTable, zeroIfNone: Boolean = false): Boolean?
}

/**
 * LinearInequalityConstraint - ConstraintF64 using math LinearInequality
 *
 * This type uses LinearFlattenDataF64 directly, avoiding dependency on frontend/inequality.
 */
data class LinearInequalityConstraint(
    val inequality: MathLinearInequality,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    val flattenData: LinearFlattenDataF64 get() = inequality.flattenData
    val sign: Comparison get() = inequality.comparison
    val name: String = ""
    val displayName: String? = null

    override fun isTrue(solution: List<Flt64>, tokenTable: LegacyAbstractTokenTable, zeroIfNone: Boolean): Boolean? {
        val lhsValue = evaluateFlattenData(flattenData, tokenTable, zeroIfNone)
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
 * This type uses QuadraticFlattenDataF64 directly, avoiding dependency on frontend/inequality.
 */
data class QuadraticInequalityConstraint(
    val inequality: MathQuadraticInequality,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint {
    val flattenData: QuadraticFlattenDataF64 get() = inequality.flattenData
    val sign: Comparison get() = inequality.comparison
    val name: String = ""
    val displayName: String? = null

    override fun isTrue(solution: List<Flt64>, tokenTable: LegacyAbstractTokenTable, zeroIfNone: Boolean): Boolean? {
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
 * LinearFlattenSubObject - SubObject using LinearFlattenDataF64 (new API)
 *
 * This type uses LinearFlattenDataF64 directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class LinearFlattenSubObject(
    val category: ObjectCategory,
    val flattenData: LinearFlattenDataF64,
    val name: String = "",
    val displayName: String? = null
)

/**
 * QuadraticFlattenSubObject - SubObject using QuadraticFlattenDataF64 (new API)
 *
 * This type uses QuadraticFlattenDataF64 directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class QuadraticFlattenSubObject(
    val category: ObjectCategory,
    val flattenData: QuadraticFlattenDataF64,
    val name: String = "",
    val displayName: String? = null
)




