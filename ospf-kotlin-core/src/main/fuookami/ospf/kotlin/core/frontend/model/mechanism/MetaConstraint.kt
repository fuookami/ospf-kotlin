package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractQuadraticPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.qsum
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.sum
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.Inequality
import fuookami.ospf.kotlin.core.frontend.inequality.LinearInequality
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.expression.adapter.toUtilsPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.eq
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
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

    fun MetaModel.constraintsOfGroup(): List<MetaConstraint<*>> {
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
            constraint = constraint eq true,
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
            constraint = constraint eq true,
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
            constraint = constraint eq true,
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
            constraint = constraint eq true,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun AbstractLinearMetaModel.addConstraint(
        constraint: LinearInequality,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(
            constraint = constraint,
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
            polynomial = sum(variables),
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
            polynomial = sum(symbols),
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
            polynomial = sum(monomials),
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
            constraint = polynomial eq true,
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
            constraint = constraint eq true,
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
            constraint = constraint eq true,
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
            constraint = constraint eq true,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: QuadraticInequality,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(
            constraint = constraint,
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
            polynomial = qsum(monomials),
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
            polynomial = qsum(symbols),
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
            constraint = polynomial eq Flt64.one,
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

data class MetaConstraint<Ineq : Inequality<*, *>>(
    val constraint: Ineq,
    val group: MetaConstraintGroup? = null,
    val lazy: Boolean = false,
    val args: Any? = null,
    val priority: Int? = null
) {
    override fun toString(): String {
        return constraint.toString()
    }
}

// ========== Math Inequality-based Constraint Types ==========

/**
 * LinearInequalityConstraint - Constraint using math LinearInequality
 *
 * This type uses LinearFlattenData directly, avoiding dependency on frontend/inequality.
 */
data class LinearInequalityConstraint(
    val inequality: MathLinearInequality,
    val group: MetaConstraintGroup? = null,
    val lazy: Boolean = false,
    val args: Any? = null,
    val priority: Int? = null
) {
    val flattenData: LinearFlattenData get() = inequality.flattenData
    val sign: Comparison get() = inequality.sign
    val name: String get() = inequality.name
    val displayName: String? get() = inequality.displayName

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
    val group: MetaConstraintGroup? = null,
    val lazy: Boolean = false,
    val args: Any? = null,
    val priority: Int? = null
) {
    val flattenData: QuadraticFlattenData get() = inequality.flattenData
    val sign: Comparison get() = inequality.sign
    val name: String get() = inequality.name
    val displayName: String? get() = inequality.displayName

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

private fun Sign.toComparison(): Comparison = when (this) {
    Sign.Less -> Comparison.LT
    Sign.LessEqual -> Comparison.LE
    Sign.Greater -> Comparison.GT
    Sign.GreaterEqual -> Comparison.GE
    Sign.Equal -> Comparison.EQ
    Sign.Unequal -> Comparison.NE
}

/**
 * Convert MetaConstraint<LinearInequality> to LinearInequalityConstraint
 */
@Suppress("DEPRECATION")
fun MetaConstraint<LinearInequality>.toRelationConstraint(): LinearInequalityConstraint {
    return LinearInequalityConstraint(
        inequality = MathLinearInequality(
            lhs = constraint.lhs.toUtilsPolynomial(),
            rhs = constraint.rhs.toUtilsPolynomial(),
            comparison = constraint.sign.toComparison(),
            name = constraint.name,
            displayName = constraint.displayName ?: ""
        ),
        group = group,
        lazy = lazy,
        args = args,
        priority = priority
    )
}

/**
 * Convert MetaConstraint<QuadraticInequality> to QuadraticInequalityConstraint
 */
@Suppress("DEPRECATION")
fun MetaConstraint<QuadraticInequality>.toRelationConstraint(): QuadraticInequalityConstraint {
    return QuadraticInequalityConstraint(
        inequality = MathQuadraticInequality(
            lhs = constraint.lhs.toUtilsPolynomial(),
            rhs = constraint.rhs.toUtilsPolynomial(),
            comparison = constraint.sign.toComparison(),
            name = constraint.name,
            displayName = constraint.displayName ?: ""
        ),
        group = group,
        lazy = lazy,
        args = args,
        priority = priority
    )
}

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




