package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

// ========== Conversion interfaces (replacing frontend.inequality.ToLinearInequality etc.) ==========

/**
 * Interface for types that can be converted to a MathLinearInequality.
 * Replaces the old frontend.inequality.ToLinearInequality interface.
 */
interface ToMathLinearInequality {
    fun toMathLinearInequality(): MathLinearInequality
}

/**
 * Interface for types that can be converted to a MathQuadraticInequality.
 * Replaces the old frontend.inequality.ToQuadraticInequality interface.
 */
interface ToMathQuadraticInequality {
    fun toMathQuadraticInequality(): MathQuadraticInequality
}

// ========== Extension properties on math inequality types ==========

/** Alias for comparison, matching the old Relation.sign property */
val MathLinearInequality.sign: Comparison get() = comparison

/** Alias for comparison, matching the old Relation.sign property */
val MathQuadraticInequality.sign: Comparison get() = comparison

/**
 * Compute LinearFlattenData from math LinearInequality.
 * Flattens lhs - rhs into a single linear form: sum(lhs.monomials) - sum(rhs.monomials) <= lhs.constant - rhs.constant
 */
val MathLinearInequality.flattenData: LinearFlattenData
    get() {
        val merged = HashMap<VariableItemKey, UtilsLinearMonomial<Flt64>>()

        for (mono in lhs.monomials) {
            val key = (mono.symbol as AbstractVariableItem<*, *>).key
            merged[key] = UtilsLinearMonomial(mono.coefficient, mono.symbol)
        }
        for (mono in rhs.monomials) {
            val key = (mono.symbol as AbstractVariableItem<*, *>).key
            val existing = merged[key]
            merged[key] = if (existing != null) {
                UtilsLinearMonomial(existing.coefficient - mono.coefficient, existing.symbol)
            } else {
                UtilsLinearMonomial(-mono.coefficient, mono.symbol)
            }
        }

        return LinearFlattenData(
            monomials = merged.values.toList(),
            constant = lhs.constant - rhs.constant
        )
    }

/**
 * Compute QuadraticFlattenData from math QuadraticInequality.
 * Flattens lhs - rhs into a single quadratic form.
 */
val MathQuadraticInequality.flattenData: QuadraticFlattenData
    get() {
        val merged = HashMap<QuadraticMonomialKey, UtilsQuadraticMonomial<Flt64>>()

        for (mono in lhs.monomials) {
            val key = QuadraticMonomialKey.from(mono)
            merged[key] = UtilsQuadraticMonomial(
                coefficient = mono.coefficient,
                symbol1 = mono.symbol1,
                symbol2 = mono.symbol2
            )
        }
        for (mono in rhs.monomials) {
            val key = QuadraticMonomialKey.from(mono)
            val existing = merged[key]
            merged[key] = if (existing != null) {
                UtilsQuadraticMonomial(
                    coefficient = existing.coefficient - mono.coefficient,
                    symbol1 = existing.symbol1,
                    symbol2 = existing.symbol2
                )
            } else {
                UtilsQuadraticMonomial(
                    coefficient = -mono.coefficient,
                    symbol1 = mono.symbol1,
                    symbol2 = mono.symbol2
                )
            }
        }

        return QuadraticFlattenData(
            monomials = merged.values.toList(),
            constant = lhs.constant - rhs.constant
        )
    }

/** Internal key for merging quadratic monomials (handles commutativity of x*y = y*x) */
private data class QuadraticMonomialKey(
    val sym1Id: Int,
    val sym2Id: Int?
) {
    companion object {
        fun from(mono: UtilsQuadraticMonomial<Flt64>): QuadraticMonomialKey {
            val id1 = System.identityHashCode(mono.symbol1)
            val id2 = mono.symbol2?.let { System.identityHashCode(it) }
            return if (id2 != null && id1 > id2) {
                QuadraticMonomialKey(id2, id1)
            } else {
                QuadraticMonomialKey(id1, id2)
            }
        }
    }
}

/**
 * Create LinearFlattenData directly from math LinearPolynomial.
 * Used when only one side of the inequality is needed.
 */
fun fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64>.toFlattenData(): LinearFlattenData {
    return LinearFlattenData(
        monomials = monomials.map { UtilsLinearMonomial(it.coefficient, it.symbol) },
        constant = constant
    )
}

/**
 * Create QuadraticFlattenData directly from math QuadraticPolynomial.
 * Used when only one side of the inequality is needed.
 */
fun fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial<Flt64>.toFlattenData(): QuadraticFlattenData {
    return QuadraticFlattenData(
        monomials = monomials.map {
            UtilsQuadraticMonomial(
                coefficient = it.coefficient,
                symbol1 = it.symbol1,
                symbol2 = it.symbol2
            )
        },
        constant = constant
    )
}

// ========== Conversion from math types to frontend types ==========

/** Convert math LinearPolynomial to frontend LinearPolynomial */
fun fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64>.toFrontendPolynomial(): LinearPolynomial {
    return LinearPolynomial(
        monomials = monomials.map { LinearMonomial(it.coefficient, it.symbol as AbstractVariableItem<*, *>) },
        constant = constant
    )
}
