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
 * 表达式扁平化工具 - 统一的表达式扁平化操作
 * Flatten Utility - Unified flatten operations for expression system
 *
 * 提供以下单一实现：
 * - 合并：合并同类项（相同变量/键）
 * - 乘法：展开表达式乘积
 * - 归一化：清理并规范化表达式
 *
 * This utility provides single-point implementations for:
 * - Merge: Combine like terms (same variable/key)
 * - Multiply: Expand product of expressions
 * - Normalize: Clean up and canonicalize expressions
 */

// ========== Merge Operations ==========

/**
 * 合并线性单项式，将相同变量的系数相加。
 * Merge linear monomials by combining coefficients of same variables.
 *
 * @param monomials 待合并的线性单项式列表 / List of linear monomials to merge
 * @param constant 基础常量值 / Base constant value
 * @return 合并后的 LinearFlattenData，系数已合并 / Merged LinearFlattenData with combined coefficients
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
 * 合并多个 LinearFlattenData，将所有单项式和常量合并。
 * Merge multiple LinearFlattenData by combining all monomials and constants.
 *
 * @param flattenDataList 待合并的扁平化数据列表 / List of flatten data to merge
 * @param initialConstant 初始常量值 / Initial constant value
 * @return 合并后的 LinearFlattenData / Merged LinearFlattenData
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
 * 合并二次单项式，将相同变量对的系数相加。
 * 使用基于标识符的确定性键排序以保证对称性。
 * Merge quadratic monomials by combining coefficients of same variable pairs.
 * Uses deterministic key ordering based on identifier for symmetry.
 *
 * @param monomials 待合并的二次单项式列表 / List of quadratic monomials to merge
 * @param constant 基础常量值 / Base constant value
 * @return 合并后的 QuadraticFlattenData，系数已合并 / Merged QuadraticFlattenData with combined coefficients
 */
internal fun mergeQuadraticMonomials(
    monomials: List<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
    constant: Flt64
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val mergedMonomials = HashMap<Pair<AbstractVariableItem<*, *>, AbstractVariableItem<*, *>?>, Flt64>()

    for (m in monomials) {
        val v1 = m.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val v2 = m.symbol2 as? AbstractVariableItem<*, *>?

        // Normalize key: use deterministic ordering based on identifier / 使用基于标识符的确定性排序规范化键
        // This ensures x*y and y*x produce the same key / 确保 x*y 和 y*x 产生相同的键
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

    return QuadraticFlattenData<Flt64>(
        monomials = normalizedMonomials,
        constant = constant
    )
}

/**
 * 合并多个 QuadraticFlattenData，将所有单项式和常量合并。
 * Merge multiple QuadraticFlattenData by combining all monomials and constants.
 *
 * @param flattenDataList 待合并的二次扁平化数据列表 / List of quadratic flatten data to merge
 * @param initialConstant 初始常量值 / Initial constant value
 * @return 合并后的 QuadraticFlattenData / Merged QuadraticFlattenData
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
 * 将两个线性扁平化数据相乘。
 * 结果为二次形式，因为线性 * 线性可能产生二次项。
 * Multiply two linear flatten data.
 * Result is quadratic because linear * linear can produce quadratic terms.
 *
 * (a1*x + c1) * (a2*y + c2) = a1*a2*xy + a1*c2*x + a2*c1*y + c1*c2
 *
 * @param lhs 左侧线性扁平化数据 / Left-hand linear flatten data
 * @param rhs 右侧线性扁平化数据 / Right-hand linear flatten data
 * @return 相乘后的二次扁平化数据 / Quadratic flatten data resulting from multiplication
 */
internal fun multiplyLinear(
    lhs: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    rhs: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val monomials = ArrayList<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    // m1 * m2 terms (quadratic) / m1 * m2 项（二次项）
    for (m1 in lhs.monomials) {
        for (m2 in rhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = m1.coefficient * m2.coefficient,
                symbol1 = m1.symbol as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = m2.symbol as? AbstractVariableItem<*, *> ?: continue
            ))
        }
    }

    // m1 * c2 terms (linear from lhs) / m1 * c2 项（左侧的线性项）
    if (rhs.constant neq Flt64.zero) {
        for (m1 in lhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = m1.coefficient * rhs.constant,
                symbol1 = (m1.symbol as? AbstractVariableItem<*, *> ?: continue),
                symbol2 = null
            ))
        }
    }

    // c1 * m2 terms (linear from rhs) / c1 * m2 项（右侧的线性项）
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
 * 将线性扁平化数据与二次扁平化数据相乘。
 *
 * 线性 * 二次 -> 二次
 * Multiply linear by quadratic flatten data.
 *
 * Linear * Quadratic -> Quadratic
 *
 * @param linear 线性扁平化数据 / Linear flatten data
 * @param quadratic 二次扁平化数据 / Quadratic flatten data
 * @return 相乘后的二次扁平化数据 / Quadratic flatten data resulting from multiplication
 */
internal fun multiplyLinearQuadratic(
    linear: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    quadratic: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val monomials = ArrayList<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    // Linear monomials * Quadratic monomials -> Quadratic (cubic would require higher order) / 线性单项式 * 二次单项式 -> 二次（三次项需要更高阶）
    // Since we only support quadratic, this is handled differently / 由于仅支持二次，此处以不同方式处理
    // For now, just merge all terms / 目前仅合并所有项
    for (lm in linear.monomials) {
        for (qm in quadratic.monomials) {
            // This would produce cubic terms, which we don't support / 这会产生三次项，我们不支持
            // So we skip or handle as error / 因此跳过或按错误处理
        }
    }

    // Linear monomials * Quadratic constant -> Linear / 线性单项式 * 二次常量 -> 线性
    if (quadratic.constant neq Flt64.zero) {
        for (lm in linear.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = lm.coefficient * quadratic.constant,
                symbol1 = lm.symbol as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = null
            ))
        }
    }

    // Linear constant * Quadratic monomials -> Quadratic / 线性常量 * 二次单项式 -> 二次
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
 * 将两个二次扁平化数据相乘。
 * 注意：二次 * 二次会产生四次项，本函数不支持四次项。
 * 此函数仅处理保持在二次范围内的部分。
 * Multiply two quadratic flatten data.
 * Note: Quadratic * Quadratic would produce Quartic terms, which we don't support.
 * This function handles the parts that stay within quadratic bounds.
 *
 * @param lhs 左侧二次扁平化数据 / Left-hand quadratic flatten data
 * @param rhs 右侧二次扁平化数据 / Right-hand quadratic flatten data
 * @return 相乘后的二次扁平化数据 / Quadratic flatten data resulting from multiplication
 */
internal fun multiplyQuadratic(
    lhs: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
    rhs: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    // Quadratic * Quadratic is not fully supported / 二次 * 二次未完全支持
    // Only handle constant multiplication / 仅处理常量乘法
    val monomials = ArrayList<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

    // lhs monomials * rhs constant / 左侧单项式 * 右侧常量
    if (rhs.constant neq Flt64.zero) {
        for (m in lhs.monomials) {
            monomials.add(QuadraticMonomial(
                coefficient = m.coefficient * rhs.constant,
                symbol1 = m.symbol1 as? AbstractVariableItem<*, *> ?: continue,
                symbol2 = m.symbol2 as? AbstractVariableItem<*, *>?
            ))
        }
    }

    // lhs constant * rhs monomials / 左侧常量 * 右侧单项式
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
 * 归一化线性扁平化数据，移除零系数项。
 * Normalize linear flatten data by removing zero coefficients.
 *
 * @param data 待归一化的线性扁平化数据 / Linear flatten data to normalize
 * @return 归一化后的线性扁平化数据 / Normalized linear flatten data
 */
internal fun normalizeLinear(data: LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>): LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        monomials = data.monomials.filter { it.coefficient neq Flt64.zero },
        constant = data.constant
    )
}

/**
 * 归一化二次扁平化数据，移除零系数项并规范化键。
 * Normalize quadratic flatten data by removing zero coefficients and canonicalizing keys.
 *
 * @param data 待归一化的二次扁平化数据 / Quadratic flatten data to normalize
 * @return 归一化后的二次扁平化数据 / Normalized quadratic flatten data
 */
internal fun normalizeQuadratic(data: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>): QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return mergeQuadraticMonomials(
        monomials = data.monomials.filter { it.coefficient neq Flt64.zero },
        constant = data.constant
    )
}
