package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import kotlinx.coroutines.reactor.mono

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
}

data class MetaConstraint<Ineq : Inequality<*, *>>(
    val constraint: Ineq,
    val group: MetaConstraintGroup? = null,
    val lazy: Boolean = false,
    val args: Any? = null
) {
    override fun toString(): String {
        return constraint.toString()
    }
}
