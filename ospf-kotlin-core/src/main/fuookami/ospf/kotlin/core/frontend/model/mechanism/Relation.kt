package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomialCell
import fuookami.ospf.kotlin.core.frontend.expression.monomial.QuadraticMonomialCell
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

/**
 * LinearRelation - New relation type for linear constraints
 *
 * This type uses LinearFlattenData as the primary data carrier,
 * providing a normalized representation for linear relations.
 *
 * Design goals:
 * - Replace dependency on frontend/inequality types
 * - Use FlattenData directly (no cell conversion needed)
 * - Provide clear, normalized representation
 */
sealed interface LinearRelation {
    val flattenData: LinearFlattenData
    val sign: Comparison
    val name: String
    val displayName: String?

    /**
     * Normalize to canonical form (<= or ==)
     */
    fun normalize(): LinearRelation

    /**
     * Convert to legacy LinearInequality for compatibility
     */
    @Deprecated(
        message = "Use LinearRelation directly. This is for backward compatibility only. Will be removed in M9.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this", "fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearRelation")
    )
    fun toInequality(): MathLinearInequality
}

/**
 * QuadraticRelation - New relation type for quadratic constraints
 */
sealed interface QuadraticRelation {
    val flattenData: QuadraticFlattenData
    val sign: Comparison
    val name: String
    val displayName: String?

    /**
     * Normalize to canonical form (<= or ==)
     */
    fun normalize(): QuadraticRelation

    /**
     * Convert to legacy QuadraticInequality for compatibility
     */
    @Deprecated(
        message = "Use QuadraticRelation directly. This is for backward compatibility only. Will be removed in M9.",
        level = DeprecationLevel.WARNING,
        replaceWith = ReplaceWith("this", "fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticRelation")
    )
    fun toInequality(): MathQuadraticInequality
}

/**
 * Default implementation of LinearRelation
 */
data class LinearRelationImpl(
    override val flattenData: LinearFlattenData,
    override val sign: Comparison,
    override val name: String = "",
    override val displayName: String? = null
) : LinearRelation {

    override fun normalize(): LinearRelation {
        return when (sign) {
            Comparison.GT -> LinearRelationImpl(
                flattenData = LinearFlattenData(
                    monomials = flattenData.monomials.map { UtilsLinearMonomial(-it.coefficient, it.symbol) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LT,
                name = name,
                displayName = displayName
            )
            Comparison.GE -> LinearRelationImpl(
                flattenData = LinearFlattenData(
                    monomials = flattenData.monomials.map { UtilsLinearMonomial(-it.coefficient, it.symbol) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LE,
                name = name,
                displayName = displayName
            )
            else -> this
        }
    }

    @Suppress("DEPRECATION")
    override fun toInequality(): MathLinearInequality {
        val cells = flattenData.toLinearMonomialCells()
        val nonConstantCells = cells.filter { !it.isConstant }
        val constantCell = cells.find { it.isConstant }

        val lhsCells = nonConstantCells + listOfNotNull(constantCell)
        throw NotImplementedError("toInequality() is deprecated and should not be used. Use LinearRelation directly.")
    }

    companion object {
        /**
         * Create LinearRelation from MathLinearInequality (adapter)
         */
        @Suppress("DEPRECATION")
        fun from(inequality: MathLinearInequality): LinearRelationImpl {
            return LinearRelationImpl(
                flattenData = inequality.flattenData,
                sign = inequality.comparison,
                name = "",
                displayName = null
            )
        }
    }
}

/**
 * Default implementation of QuadraticRelation
 */
data class QuadraticRelationImpl(
    override val flattenData: QuadraticFlattenData,
    override val sign: Comparison,
    override val name: String = "",
    override val displayName: String? = null
) : QuadraticRelation {

    override fun normalize(): QuadraticRelation {
        return when (sign) {
            Comparison.GT -> QuadraticRelationImpl(
                flattenData = QuadraticFlattenData(
                    monomials = flattenData.monomials.map { UtilsQuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LT,
                name = name,
                displayName = displayName
            )
            Comparison.GE -> QuadraticRelationImpl(
                flattenData = QuadraticFlattenData(
                    monomials = flattenData.monomials.map { UtilsQuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                    constant = -flattenData.constant
                ),
                sign = Comparison.LE,
                name = name,
                displayName = displayName
            )
            else -> this
        }
    }

    @Suppress("DEPRECATION")
    override fun toInequality(): MathQuadraticInequality {
        throw NotImplementedError("toInequality() is deprecated and should not be used. Use QuadraticRelation directly.")
    }

    companion object {
        /**
         * Create QuadraticRelation from MathQuadraticInequality (adapter)
         */
        @Suppress("DEPRECATION")
        fun from(inequality: MathQuadraticInequality): QuadraticRelationImpl {
            return QuadraticRelationImpl(
                flattenData = inequality.flattenData,
                sign = inequality.comparison,
                name = "",
                displayName = null
            )
        }
    }
}

// ========== Extension functions for conversion ==========

/**
 * Convert MathLinearInequality to LinearRelation
 */
@Suppress("DEPRECATION")
fun MathLinearInequality.toRelation(): LinearRelation = LinearRelationImpl.from(this)

/**
 * Convert MathQuadraticInequality to QuadraticRelation
 */
@Suppress("DEPRECATION")
fun MathQuadraticInequality.toRelation(): QuadraticRelation = QuadraticRelationImpl.from(this)
