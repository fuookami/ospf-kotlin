/**
 * 表达式扁平化工具 / Expression flatten utilities
 *
 * 提供线性与二次表达式的合并、乘法和归一化操作，用于模型构建时的表达式展开。
 *
 * Provides merge, multiply, and normalize operations for linear and quadratic
 * expressions, used during model construction for expression expansion.
 */
package fuookami.ospf.kotlin.core.symbol.flatten

import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

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
 * @return Merged LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> with combined coefficients
 */
internal fun mergeLinearMonomials(
    monomials: List<LinearMonomial<Flt64>>,
    constant: Flt64
): LinearFlattenData<Flt64> {
    val mergedMonomials = HashMap<AbstractVariableItem<*, *>, Flt64>()

    for (m in monomials) {
        val variable = m.symbol as? AbstractVariableItem<*, *> ?: continue
        if (m.coefficient neq Flt64.zero) {
            mergedMonomials[variable] = (mergedMonomials[variable] ?: Flt64.zero) + m.coefficient
        }
    }

    val normalizedMonomials = ArrayList<LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>(mergedMonomials.size)
    for ((variable, coefficient) in mergedMonomials) {
        if (coefficient neq Flt64.zero) {
            normalizedMonomials.add(LinearMonomial(coefficient, variable))
        }
    }

    return LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = normalizedMonomials,
        constant = constant
    )
}

/**
 * Merge multiple LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> by combining all monomials and constants.
 */
internal fun mergeLinearFlattenDataFlt64(
    flattenDataList: List<LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
    initialConstant: Flt64 = Flt64.zero
): LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val mergedMonomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
    var totalConstant = initialConstant
    for (flattenData in flattenDataList) {
        totalConstant += flattenData.constant
        for (monomial in flattenData.monomials) {
            val variable = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
            if (monomial.coefficient neq Flt64.zero) {
                mergedMonomials[variable] = (mergedMonomials[variable] ?: Flt64.zero) + monomial.coefficient
            }
        }
    }

    val normalizedMonomials = ArrayList<LinearMonomial<Flt64>>(mergedMonomials.size)
    for ((variable, coefficient) in mergedMonomials) {
        if (coefficient neq Flt64.zero) {
            normalizedMonomials.add(LinearMonomial(coefficient, variable))
        }
    }

    return LinearFlattenData(
        monomials = normalizedMonomials,
        constant = totalConstant
    )
}

/**
 * Merge quadratic monomials by combining coefficients of same variable pairs.
 * Uses deterministic key ordering based on identifier for symmetry.
 *
 * @param monomials List of quadratic monomials to merge
 * @param constant Base constant value
 * @return Merged QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> with combined coefficients
 */
internal fun mergeQuadraticMonomials(
    monomials: List<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
    constant: Flt64
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val mergedMonomials = HashMap<Pair<AbstractVariableItem<*, *>, AbstractVariableItem<*, *>?>, Flt64>()

    for (m in monomials) {
        val v1 = m.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val v2 = m.symbol2 as? AbstractVariableItem<*, *>?

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

    val normalizedMonomials = ArrayList<QuadraticMonomial<Flt64>>(mergedMonomials.size)
    for ((key, coefficient) in mergedMonomials) {
        if (coefficient neq Flt64.zero) {
            normalizedMonomials.add(QuadraticMonomial(coefficient, key.first, key.second))
        }
    }

    return QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = normalizedMonomials,
        constant = constant
    )
}

/**
 * Merge multiple QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> by combining all monomials and constants.
 */
internal fun mergeQuadraticFlattenDataFlt64(
    flattenDataList: List<QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
    initialConstant: Flt64 = Flt64.zero
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val mergedMonomials = HashMap<Pair<AbstractVariableItem<*, *>, AbstractVariableItem<*, *>?>, Flt64>()
    var totalConstant = initialConstant
    for (flattenData in flattenDataList) {
        totalConstant += flattenData.constant
        for (monomial in flattenData.monomials) {
            val v1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: continue
            val v2 = monomial.symbol2 as? AbstractVariableItem<*, *>?
            val key = if (v2 != null) {
                if (v1.identifier < v2.identifier) {
                    v1 to v2
                } else {
                    v2 to v1
                }
            } else {
                v1 to null
            }
            if (monomial.coefficient neq Flt64.zero) {
                mergedMonomials[key] = (mergedMonomials[key] ?: Flt64.zero) + monomial.coefficient
            }
        }
    }

    val normalizedMonomials = ArrayList<QuadraticMonomial<Flt64>>(mergedMonomials.size)
    for ((key, coefficient) in mergedMonomials) {
        if (coefficient neq Flt64.zero) {
            normalizedMonomials.add(QuadraticMonomial(coefficient, key.first, key.second))
        }
    }

    return QuadraticFlattenData(
        monomials = normalizedMonomials,
        constant = totalConstant
    )
}

// ========== Multiply Operations ==========

/**
 * Multiply two linear flatten data.
 * Result is quadratic because linear * linear can produce quadratic terms.
 *
 * (a1*x + c1) * (a2*y + c2) = a1*a2*xy + a1*c2*x + a2*c1*y + c1*c2
 */
internal fun multiplyLinear(
    lhs: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    rhs: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val monomials = ArrayList<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    // m1 * m2 terms (quadratic)
    for (m1 in lhs.monomials) {
        for (m2 in rhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = m1.coefficient * m2.coefficient,
                symbol1 = m1.symbol as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = m2.symbol as? AbstractVariableItem<*, *> ?: continue
            ))
        }
    }

    // m1 * c2 terms (linear from lhs)
    if (rhs.constant neq Flt64.zero) {
        for (m1 in lhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = m1.coefficient * rhs.constant,
                symbol1 = (m1.symbol as? AbstractVariableItem<*, *> ?: continue),
                symbol2 = null
            ))
        }
    }

    // c1 * m2 terms (linear from rhs)
    if (lhs.constant neq Flt64.zero) {
        for (m2 in rhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = lhs.constant * m2.coefficient,
                symbol1 = (m2.symbol as? AbstractVariableItem<*, *> ?: continue),
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
    linear: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    quadratic: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val monomials = ArrayList<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

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
            monomials.add(QuadraticMonomial(
                coefficient = lm.coefficient * quadratic.constant,
                symbol1 = lm.symbol as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = null
            ))
        }
    }

    // Linear constant * Quadratic monomials -> Quadratic
    if (linear.constant neq Flt64.zero) {
        for (qm in quadratic.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = linear.constant * qm.coefficient,
                symbol1 = qm.symbol1 as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = qm.symbol2 as? AbstractVariableItem<*, *>?
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
    lhs: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    rhs: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    // Quadratic * Quadratic is not fully supported
    // Only handle constant multiplication
    val monomials = ArrayList<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    // lhs monomials * rhs constant
    if (rhs.constant neq Flt64.zero) {
        for (m in lhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = m.coefficient * rhs.constant,
                symbol1 = m.symbol1 as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = m.symbol2 as? AbstractVariableItem<*, *>?
            ))
        }
    }

    // lhs constant * rhs monomials
    if (lhs.constant neq Flt64.zero) {
        for (m in rhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = lhs.constant * m.coefficient,
                symbol1 = m.symbol1 as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = m.symbol2 as? AbstractVariableItem<*, *>?
            ))
        }
    }

    return mergeQuadraticMonomials(monomials, lhs.constant * rhs.constant)
}

// ========== Normalize Operations ==========

/**
 * Normalize linear flatten data by removing zero coefficients.
 */
internal fun normalizeLinear(data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = data.monomials.filter { it.coefficient neq Flt64.zero },
        constant = data.constant
    )
}

/**
 * Normalize quadratic flatten data by removing zero coefficients and canonicalizing keys.
 */
internal fun normalizeQuadratic(data: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return mergeQuadraticMonomials(
        monomials = data.monomials.filter { it.coefficient neq Flt64.zero },
        constant = data.constant
    )
}
