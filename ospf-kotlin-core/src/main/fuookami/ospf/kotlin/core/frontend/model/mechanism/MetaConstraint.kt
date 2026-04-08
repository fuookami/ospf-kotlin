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
import fuookami.ospf.kotlin.core.frontend.inequality.eq
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearRelation
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticRelation
import fuookami.ospf.kotlin.core.frontend.model.mechanism.toRelation
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

    // ========== NEW Relation-based API ==========

    /**
     * Add constraint using LinearRelation (new API)
     */
    fun AbstractLinearMetaModel.addConstraint(
        relation: LinearRelation,
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
     * Add constraint using QuadraticRelation (new API)
     */
    fun AbstractQuadraticMetaModel.addConstraint(
        relation: QuadraticRelation,
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

// ========== NEW Relation-based Constraint Types ==========

/**
 * LinearRelationConstraint - Constraint using LinearRelation (new API)
 *
 * This type uses LinearFlattenData directly, avoiding dependency on frontend/inequality.
 */
data class LinearRelationConstraint(
    val relation: LinearRelation,
    val group: MetaConstraintGroup? = null,
    val lazy: Boolean = false,
    val args: Any? = null,
    val priority: Int? = null
) {
    val flattenData: LinearFlattenData get() = relation.flattenData
    val sign: Sign get() = relation.sign
    val name: String get() = relation.name
    val displayName: String? get() = relation.displayName

    override fun toString(): String {
        return relation.toString()
    }
}

/**
 * QuadraticRelationConstraint - Constraint using QuadraticRelation (new API)
 *
 * This type uses QuadraticFlattenData directly, avoiding dependency on frontend/inequality.
 */
data class QuadraticRelationConstraint(
    val relation: QuadraticRelation,
    val group: MetaConstraintGroup? = null,
    val lazy: Boolean = false,
    val args: Any? = null,
    val priority: Int? = null
) {
    val flattenData: QuadraticFlattenData get() = relation.flattenData
    val sign: Sign get() = relation.sign
    val name: String get() = relation.name
    val displayName: String? get() = relation.displayName

    override fun toString(): String {
        return relation.toString()
    }
}

// ========== Deprecated adapters ==========

/**
 * Convert MetaConstraint<LinearInequality> to LinearRelationConstraint
 */
@Suppress("DEPRECATION")
fun MetaConstraint<LinearInequality>.toRelationConstraint(): LinearRelationConstraint {
    return LinearRelationConstraint(
        relation = constraint.toRelation(),
        group = group,
        lazy = lazy,
        args = args,
        priority = priority
    )
}

/**
 * Convert MetaConstraint<QuadraticInequality> to QuadraticRelationConstraint
 */
@Suppress("DEPRECATION")
fun MetaConstraint<QuadraticInequality>.toRelationConstraint(): QuadraticRelationConstraint {
    return QuadraticRelationConstraint(
        relation = constraint.toRelation(),
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




