package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

interface MetaConstraintGroup {
    val lazy: Boolean get() = false
    val name: String

    fun <V> MetaModel<V>.registerConstraintGroup() where V : RealNumber<V>, V : NumberField<V> {
        this.registerConstraintGroup(this@MetaConstraintGroup)
    }

    fun <V> MetaModel<V>.indicesOfConstraintGroup(): IntRange? where V : RealNumber<V>, V : NumberField<V> {
        return this.indicesOfConstraintGroup(this@MetaConstraintGroup)
    }

    fun <V> MetaModel<V>.constraintsOfGroup(): List<MathConstraint> where V : RealNumber<V>, V : NumberField<V> {
        return indicesOfConstraintGroup(this@MetaConstraintGroup)?.let { indices ->
            indices.map { constraints[it] }
        } ?: emptyList()
    }

    fun <V> AbstractLinearMetaModel<V>.addConstraint(
        constraint: AbstractVariableItem<*, *>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        val lhs = LinearPolynomial(listOf(LinearMonomial(converter.one, constraint)), converter.zero)
        val rhs = LinearPolynomial(emptyList(), converter.one)
        return this.addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun <V> AbstractLinearMetaModel<V>.addConstraint(
        constraint: LinearIntermediateSymbol<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        val lhs = constraint.toLinearPolynomial()
        val rhs = LinearPolynomial(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
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
    fun <V> AbstractLinearMetaModel<V>.partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        return partition(
            polynomial = LinearPolynomial(
                monomials = variables.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
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
    fun <V> AbstractLinearMetaModel<V>.partition(
        symbols: Iterable<LinearIntermediateSymbol<V>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return partition(
            polynomial = LinearPolynomial(
                monomials = symbols.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun <V> AbstractQuadraticMetaModel<V>.addConstraint(
        constraint: QuadraticIntermediateSymbol<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        val lhs = constraint.toQuadraticPolynomial()
        val rhs = QuadraticPolynomial(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf(lhs, rhs, Comparison.EQ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun <V> AbstractQuadraticMetaModel<V>.partition(
        symbols: Iterable<QuadraticIntermediateSymbol<V>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return partition(
            polynomial = QuadraticPolynomial(
                monomials = symbols.flatMap { it.toQuadraticPolynomial().monomials }.toList(),
                constant = converter.zero
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
     * Add constraint using math LinearInequality<V>
     */
    fun <V> AbstractLinearMetaModel<V>.addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try where V : RealNumber<V>, V : NumberField<V> {
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
     * Add constraint using math QuadraticInequalityOf<V>
     */
    fun <V> AbstractQuadraticMetaModel<V>.addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try where V : RealNumber<V>, V : NumberField<V> {
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

// Group-scoped query helper.
// 约束组作用域查询辅助函数。
fun <V> MetaModel<V>.constraintsOfGroup(group: MetaConstraintGroup): List<MathConstraint>
        where V : RealNumber<V>, V : NumberField<V> {
    return group.run { this@constraintsOfGroup.constraintsOfGroup() }
}

// ========== Math Inequality-based Constraint<fuookami.ospf.kotlin.math.algebra.number.Flt64> Types ==========

/**
 * Common interface for math-based constraints.
 */
interface MathConstraint {
    val group: MetaConstraintGroup?
    val lazy: Boolean
    val args: Any?
    val priority: Int?

    fun <V> isTrue(
        solution: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V>
}

/**
 * LinearInequalityConstraint - Constraint using math LinearInequality<V>
 */
data class LinearInequalityConstraint<V>(
    val inequality: LinearInequality<V>,
    val converter: IntoValue<V>,
    val constraintName: String = "",
    val constraintDisplayName: String? = null,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: LinearFlattenData<V> get() = inequality.toLinearFlattenData().getOrThrow()
    val sign: Comparison get() = inequality.comparison
    val name: String get() = constraintName
    val displayName: String? get() = constraintDisplayName

    override fun <V1> isTrue(
        solution: List<V1>,
        tokenTable: AbstractTokenTable<V1>,
        zeroIfNone: Boolean
    ): Boolean? where V1 : RealNumber<V1>, V1 : NumberField<V1> {
        @Suppress("UNCHECKED_CAST")
        val typedFlattenData = flattenData as LinearFlattenData<V1>
        val lhsValue = evaluateFlattenDataWithResults(typedFlattenData, solution, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue.toFlt64(), Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

/**
 * QuadraticInequalityConstraint - Constraint using math QuadraticInequalityOf<V>
 */
data class QuadraticInequalityConstraint<V>(
    val inequality: QuadraticInequalityOf<V>,
    val converter: IntoValue<V>,
    val constraintName: String = "",
    val constraintDisplayName: String? = null,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: QuadraticFlattenData<V> get() = inequality.toQuadraticFlattenData()
    val sign: Comparison get() = inequality.comparison
    val name: String get() = constraintName
    val displayName: String? get() = constraintDisplayName

    override fun <V1> isTrue(
        solution: List<V1>,
        tokenTable: AbstractTokenTable<V1>,
        zeroIfNone: Boolean
    ): Boolean? where V1 : RealNumber<V1>, V1 : NumberField<V1> {
        @Suppress("UNCHECKED_CAST")
        val typedFlattenData = flattenData as QuadraticFlattenData<V1>
        val lhsValue = evaluateQuadraticFlattenDataWithResults(typedFlattenData, solution, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue.toFlt64(), Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

// ========== NEW FlattenData-based SubObject Types ==========


/**
 * QuadraticFlattenSubObject - SubObject using QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> (new API)
 *
 * This type uses QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> directly for objective functions,
 * avoiding dependency on frontend/expression types.
 */
data class QuadraticFlattenSubObject<V>(
    val category: ObjectCategory,
    val flattenData: QuadraticFlattenData<V>,
    val name: String = "",
    val displayName: String? = null
) where V : RealNumber<V>, V : NumberField<V>



