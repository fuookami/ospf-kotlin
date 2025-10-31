package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*

interface MetaConstraintGroup {
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
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return this.addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractLinearMetaModel.addConstraint(
        constraint: LinearMonomial,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractLinearMetaModel.addConstraint(
        constraint: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractLinearMetaModel.addConstraint(
        constraint: LinearIntermediateSymbol,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractLinearMetaModel.addConstraint(
        constraint: LinearInequality,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        return addConstraint(constraint, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun AbstractLinearMetaModel.partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(sum(variables), this@MetaConstraintGroup, name, displayName, args)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun AbstractLinearMetaModel.partition(
        symbols: Iterable<LinearIntermediateSymbol>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(sum(symbols), this@MetaConstraintGroup, name, displayName, args)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun AbstractLinearMetaModel.partition(
        monomials: Iterable<LinearMonomial>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(sum(monomials), this@MetaConstraintGroup, name, displayName, args)
    }

    fun AbstractLinearMetaModel.partition(
        polynomial: AbstractLinearPolynomial<*>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(polynomial eq true, this@MetaConstraintGroup, name, displayName, args)
    }

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: QuadraticMonomial,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: QuadraticIntermediateSymbol,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint eq true, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    fun AbstractQuadraticMetaModel.addConstraint(
        constraint: QuadraticInequality,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        return addConstraint(constraint, this@MetaConstraintGroup, name, displayName, args, withRangeSet)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticMonomials")
    fun AbstractQuadraticMetaModel.partition(
        monomials: Iterable<QuadraticMonomial>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(qsum(monomials), this@MetaConstraintGroup, name, displayName, args)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun AbstractQuadraticMetaModel.partition(
        symbols: Iterable<QuadraticIntermediateSymbol>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(qsum(symbols), this@MetaConstraintGroup, name, displayName, args)
    }

    fun AbstractQuadraticMetaModel.partition(
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return addConstraint(polynomial eq Flt64.one, this@MetaConstraintGroup, name, displayName, args)
    }
}

data class MetaConstraint<Ineq : Inequality<*, *>>(
    val constraint: Ineq,
    val group: MetaConstraintGroup? = null,
    val args: Any? = null
)
