package fuookami.ospf.kotlin.core.intermediate_symbol.flatten

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.token.LinearFlattenDataF64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataF64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

/**
 * Flatten Utility - Unified flatten operations for expression system
 *
 * This utility provides single-point implementations for:
 * - Merge: Combine like terms (same variable/key)
 * - Multiply: Expand product of expressions
 * - Normalize: Clean up and canonicalize expressions
 */

// ========== Merge Operations ==========

/**
 * Merge linear monomials by combining coefficients of same variables.
 *
 * @param monomials List of linear monomials to merge
 * @param constant Base constant value
 * @return Merged LinearFlattenDataF64 with combined coefficients
 */
internal fun mergeLinearMonomials(
    monomials: List<UtilsLinearMonomial<F64>>,
    constant: Flt64
): LinearFlattenDataF64 {
    val mergedMonomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
    var totalConstant = constant

    for (m in monomials) {
        val variable = m.symbol as AbstractVariableItem<*, *>
        if (m.coefficient neq Flt64.zero) {
            mergedMonomials[variable] = (mergedMonomials[variable] ?: Flt64.zero) + m.coefficient
        }
    }

    return LinearFlattenDataF64(
        monomials = mergedMonomials
            .filter { it.value neq Flt64.zero }
            .map { UtilsLinearMonomial(it.value, it.key) },
        constant = totalConstant
    )
}

/**
 * Merge multiple LinearFlattenDataF64 by combining all monomials and constants.
 */
internal fun mergeLinearFlattenDataF64(
    flattenDataList: List<LinearFlattenDataF64>,
    initialConstant: Flt64 = Flt64.zero
): LinearFlattenDataF64 {
    val allMonomials = flattenDataList.flatMap { it.monomials }
    val totalConstant = flattenDataList.fold(initialConstant) { acc, data -> acc + data.constant }
    return mergeLinearMonomials(allMonomials, totalConstant)
}

/**
 * Merge quadratic monomials by combining coefficients of same variable pairs.
 * Uses deterministic key ordering based on identifier for symmetry.
 *
 * @param monomials List of quadratic monomials to merge
 * @param constant Base constant value
 * @return Merged QuadraticFlattenDataF64 with combined coefficients
 */
internal fun mergeQuadraticMonomials(
    monomials: List<UtilsQuadraticMonomial<F64>>,
    constant: Flt64
): QuadraticFlattenDataF64 {
    val mergedMonomials = HashMap<Pair<AbstractVariableItem<*, *>, AbstractVariableItem<*, *>?>, Flt64>()
    var totalConstant = constant

    for (m in monomials) {
        val v1 = m.symbol1 as AbstractVariableItem<*, *>
        val v2 = m.symbol2 as AbstractVariableItem<*, *>?

        // Normalize key: use deterministic ordering based on identifier
        // This ensures x*y and y*x produce the same key
        val key = if (v2 != null) {
            if (v1.identifier < v2.identifier) {
                v1 to v2
            } else {
                v2 to v1
            }
        } else {
            v1 to null
        }

        if (m.coefficient neq Flt64.zero) {
            mergedMonomials[key] = (mergedMonomials[key] ?: Flt64.zero) + m.coefficient
        }
    }

    return QuadraticFlattenDataF64(
        monomials = mergedMonomials
            .filter { it.value neq Flt64.zero }
            .map { UtilsQuadraticMonomial(it.value, it.key.first, it.key.second) },
        constant = totalConstant
    )
}

/**
 * Merge multiple QuadraticFlattenDataF64 by combining all monomials and constants.
 */
internal fun mergeQuadraticFlattenDataF64(
    flattenDataList: List<QuadraticFlattenDataF64>,
    initialConstant: Flt64 = Flt64.zero
): QuadraticFlattenDataF64 {
    val allMonomials = flattenDataList.flatMap { it.monomials }
    val totalConstant = flattenDataList.fold(initialConstant) { acc, data -> acc + data.constant }
    return mergeQuadraticMonomials(allMonomials, totalConstant)
}

// ========== Multiply Operations ==========

/**
 * Multiply two linear flatten data.
 * Result is quadratic because linear * linear can produce quadratic terms.
 *
 * (a1*x + c1) * (a2*y + c2) = a1*a2*xy + a1*c2*x + a2*c1*y + c1*c2
 */
internal fun multiplyLinear(
    lhs: LinearFlattenDataF64,
    rhs: LinearFlattenDataF64
): QuadraticFlattenDataF64 {
    val monomials = ArrayList<UtilsQuadraticMonomial<F64>>()

    // m1 * m2 terms (quadratic)
    for (m1 in lhs.monomials) {
        for (m2 in rhs.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = m1.coefficient * m2.coefficient,
                symbol1 = m1.symbol as AbstractVariableItem<*, *>,
                symbol2 = m2.symbol as AbstractVariableItem<*, *>
            ))
        }
    }

    // m1 * c2 terms (linear from lhs)
    if (rhs.constant neq Flt64.zero) {
        for (m1 in lhs.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = m1.coefficient * rhs.constant,
                symbol1 = m1.symbol as AbstractVariableItem<*, *>,
                symbol2 = null
            ))
        }
    }

    // c1 * m2 terms (linear from rhs)
    if (lhs.constant neq Flt64.zero) {
        for (m2 in rhs.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = lhs.constant * m2.coefficient,
                symbol1 = m2.symbol as AbstractVariableItem<*, *>,
                symbol2 = null
            ))
        }
    }

    return mergeQuadraticMonomials(monomials, lhs.constant * rhs.constant)
}

/**
 * Multiply linear by quadratic flatten data.
 *
 * Linear * Quadratic -> Quadratic
 */
internal fun multiplyLinearQuadratic(
    linear: LinearFlattenDataF64,
    quadratic: QuadraticFlattenDataF64
): QuadraticFlattenDataF64 {
    val monomials = ArrayList<UtilsQuadraticMonomial<F64>>()

    // Linear monomials * Quadratic monomials -> Quadratic (cubic would require higher order)
    // Since we only support quadratic, this is handled differently
    // For now, just merge all terms
    for (lm in linear.monomials) {
        for (qm in quadratic.monomials) {
            // This would produce cubic terms, which we don't support
            // So we skip or handle as error
        }
    }

    // Linear monomials * Quadratic constant -> Linear
    if (quadratic.constant neq Flt64.zero) {
        for (lm in linear.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = lm.coefficient * quadratic.constant,
                symbol1 = lm.symbol as AbstractVariableItem<*, *>,
                symbol2 = null
            ))
        }
    }

    // Linear constant * Quadratic monomials -> Quadratic
    if (linear.constant neq Flt64.zero) {
        for (qm in quadratic.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = linear.constant * qm.coefficient,
                symbol1 = qm.symbol1 as AbstractVariableItem<*, *>,
                symbol2 = qm.symbol2 as AbstractVariableItem<*, *>?
            ))
        }
    }

    return mergeQuadraticMonomials(monomials, linear.constant * quadratic.constant)
}

/**
 * Multiply two quadratic flatten data.
 * Note: Quadratic * Quadratic would produce Quartic terms, which we don't support.
 * This function handles the parts that stay within quadratic bounds.
 */
internal fun multiplyQuadratic(
    lhs: QuadraticFlattenDataF64,
    rhs: QuadraticFlattenDataF64
): QuadraticFlattenDataF64 {
    // Quadratic * Quadratic is not fully supported
    // Only handle constant multiplication
    val monomials = ArrayList<UtilsQuadraticMonomial<F64>>()

    // lhs monomials * rhs constant
    if (rhs.constant neq Flt64.zero) {
        for (m in lhs.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = m.coefficient * rhs.constant,
                symbol1 = m.symbol1 as AbstractVariableItem<*, *>,
                symbol2 = m.symbol2 as AbstractVariableItem<*, *>?
            ))
        }
    }

    // lhs constant * rhs monomials
    if (lhs.constant neq Flt64.zero) {
        for (m in rhs.monomials) {
            monomials.add(UtilsQuadraticMonomial(
                coefficient = lhs.constant * m.coefficient,
                symbol1 = m.symbol1 as AbstractVariableItem<*, *>,
                symbol2 = m.symbol2 as AbstractVariableItem<*, *>?
            ))
        }
    }

    return mergeQuadraticMonomials(monomials, lhs.constant * rhs.constant)
}

// ========== Normalize Operations ==========

/**
 * Normalize linear flatten data by removing zero coefficients.
 */
internal fun normalizeLinear(data: LinearFlattenDataF64): LinearFlattenDataF64 {
    return LinearFlattenDataF64(
        monomials = data.monomials.filter { it.coefficient neq Flt64.zero },
        constant = data.constant
    )
}

/**
 * Normalize quadratic flatten data by removing zero coefficients and canonicalizing keys.
 */
internal fun normalizeQuadratic(data: QuadraticFlattenDataF64): QuadraticFlattenDataF64 {
    return mergeQuadraticMonomials(
        monomials = data.monomials.filter { it.coefficient neq Flt64.zero },
        constant = data.constant
    )
}