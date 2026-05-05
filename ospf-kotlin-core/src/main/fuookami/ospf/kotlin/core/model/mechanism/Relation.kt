package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * LinearRelation - New relation type for linear constraints
 *
 * This type uses LinearFlattenData<Flt64> as the primary data carrier,
 * providing a normalized representation for linear relations.
 *
 * Design goals:
 * - Replace dependency on frontend/inequality types
 * - Use FlattenData directly (no cell conversion needed)
 * - Provide clear, normalized representation
 */
sealed interface LinearRelation {
    val flattenData: LinearFlattenData<Flt64>
    val sign: Comparison
    val name: String
    val displayName: String?

    val constraintRelation: ConstraintRelation get() = ConstraintRelation(sign)

    /**
     * Normalize to canonical form (<= or ==)
     */
    fun normalize(): LinearRelation
}

/**
 * QuadraticRelation - New relation type for quadratic constraints
 */
sealed interface QuadraticRelation {
    val flattenData: QuadraticFlattenData<Flt64>
    val sign: Comparison
    val name: String
    val displayName: String?

    val constraintRelation: ConstraintRelation get() = ConstraintRelation(sign)

    /**
     * Normalize to canonical form (<= or ==)
     */
    fun normalize(): QuadraticRelation
}

/**
 * Default implementation of LinearRelation
 */
data class LinearRelationImpl(
    override val flattenData: LinearFlattenData<Flt64>,
    override val sign: Comparison,
    override val name: String = "",
    override val displayName: String? = null
) : LinearRelation {

    override fun normalize(): LinearRelation {
        return when (sign) {
            Comparison.GT -> LinearRelationImpl(
                flattenData = LinearFlattenData<Flt64>(
                    monomials = flattenData.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LT,
                name = name,
                displayName = displayName
            )
            Comparison.GE -> LinearRelationImpl(
                flattenData = LinearFlattenData<Flt64>(
                    monomials = flattenData.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LE,
                name = name,
                displayName = displayName
            )
            else -> this
        }
    }

}

/**
 * Default implementation of QuadraticRelation
 */
data class QuadraticRelationImpl(
    override val flattenData: QuadraticFlattenData<Flt64>,
    override val sign: Comparison,
    override val name: String = "",
    override val displayName: String? = null
) : QuadraticRelation {

    override fun normalize(): QuadraticRelation {
        return when (sign) {
            Comparison.GT -> QuadraticRelationImpl(
                flattenData = QuadraticFlattenData<Flt64>(
                    monomials = flattenData.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LT,
                name = name,
                displayName = displayName
            )
            Comparison.GE -> QuadraticRelationImpl(
                flattenData = QuadraticFlattenData<Flt64>(
                    monomials = flattenData.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LE,
                name = name,
                displayName = displayName
            )
            else -> this
        }
    }

}
