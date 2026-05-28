/**
 * LaTeX 运算
 * LaTeX Operations
 *
 * 提供将多项式和不等式转换丌LaTeX 格式字符串的核心实现。
 * 支持紧凑和展开格式，控制是否显示系敌1，以及是否使甌cdot 符号。
 * Provides core implementation for converting polynomials and inequalities
 * to LaTeX format strings. Supports compact and expanded formats,
 * controlling whether to show coefficient 1, and whether to use cdot symbol.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

// ============================================================================
// Latex Operations (Ring-based, no Generic conversion)
// ============================================================================

/**
 * LaTeX 格式化选项
 * LaTeX formatting options
 *
 * @property compact 是否使用紧凑格式（省略空格） / Whether to use compact format (omit spaces)
 * @property showOnes 是否显示系数 1 / Whether to show coefficient 1
 * @property useCdot 是否使用 \cdot 乘法符号 / Whether to use \cdot multiplication symbol
 */
data class LatexOptions(
    val compact: Boolean = true,
    val showOnes: Boolean = false,
    val useCdot: Boolean = false
)

/**
 * LaTeX 数值运算接口
 * LaTeX number operations interface
 *
 * 提供数值判断和格式化所需的回调函数。
 * Provides callbacks for number judgment and formatting.
 *
 * @property isZero 判断值是否为零 / Predicate to check if a value is zero
 * @property isOne 判断值是否为一 / Predicate to check if a value is one
 * @property isNegative 判断值是否为负 / Predicate to check if a value is negative
 * @property abs 取绝对值 / Absolute value function
 * @property format 将值格式化为字符串 / Format a value to string
 */
data class LatexNumberOps<T>(
    val isZero: (T) -> Boolean,
    val isOne: (T) -> Boolean,
    val isNegative: (T) -> Boolean,
    val abs: (T) -> T,
    val format: (T) -> String
)

private data class SignedTerm(
    val body: String,
    val negative: Boolean
)

private fun Symbol.latexName(): String {
    return displayName ?: name
}

private fun mulSymbol(options: LatexOptions): String {
    return if (options.useCdot) {
        if (options.compact) {
            "\\cdot"
        } else {
            " \\cdot "
        }
    } else {
        ""
    }
}

private fun mergeTerms(
    terms: List<SignedTerm>,
    options: LatexOptions
): String {
    if (terms.isEmpty()) {
        return "0"
    }
    val plus = if (options.compact) "+" else " + "
    val minus = if (options.compact) "-" else " - "
    val builder = StringBuilder()
    for ((index, term) in terms.withIndex()) {
        if (index == 0) {
            if (term.negative) {
                builder.append("-")
            }
            builder.append(term.body)
        } else {
            builder.append(if (term.negative) minus else plus)
            builder.append(term.body)
        }
    }
    return builder.toString()
}

private fun <T> formatMonomialTerm(
    coefficient: T,
    variable: String,
    options: LatexOptions,
    ops: LatexNumberOps<T>
): String {
    val absCoefficient = ops.abs(coefficient)
    val coefficientText = if (!options.showOnes && ops.isOne(absCoefficient)) {
        ""
    } else {
        ops.format(absCoefficient)
    }
    if (coefficientText.isEmpty()) {
        return variable
    }
    val multiply = mulSymbol(options)
    return if (multiply.isEmpty()) {
        coefficientText + variable
    } else {
        coefficientText + multiply + variable
    }
}

/**
 * 将线性单项式转换为 LaTeX 字符串
 * Convert a linear monomial to LaTeX string.
 *
 * @param ops 数值运算接口 / Number operations interface
 * @param options LaTeX 格式化选项 / LaTeX formatting options
 * @return LaTeX 格式字符串 / LaTeX format string
 */
fun <T> LinearMonomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = symbol.latexName(),
        options = options,
        ops = ops
    )
}

/**
 * 将二次单项式转换为 LaTeX 字符串
 * Convert a quadratic monomial to LaTeX string.
 *
 * @param ops 数值运算接口 / Number operations interface
 * @param options LaTeX 格式化选项 / LaTeX formatting options
 * @return LaTeX 格式字符串 / LaTeX format string
 */
fun <T> QuadraticMonomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    if (symbol2 == null) {
        return formatMonomialTerm(
            coefficient = coefficient,
            variable = symbol1.latexName(),
            options = options,
            ops = ops
        )
    }
    val variable = if (symbol1 == symbol2) {
        "${symbol1.latexName()}^{2}"
    } else {
        val multiply = mulSymbol(options)
        if (multiply.isEmpty()) {
            symbol1.latexName() + symbol2.latexName()
        } else {
            symbol1.latexName() + multiply + symbol2.latexName()
        }
    }
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = variable,
        options = options,
        ops = ops
    )
}

/**
 * 将规范单项式转换为 LaTeX 字符串
 * Convert a canonical monomial to LaTeX string.
 *
 * @param ops 数值运算接口 / Number operations interface
 * @param options LaTeX 格式化选项 / LaTeX formatting options
 * @return LaTeX 格式字符串 / LaTeX format string
 */
fun <T> CanonicalMonomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    if (powers.isEmpty()) {
        return ops.format(coefficient)
    }
    val multiply = mulSymbol(options)
    val variable = powers.entries.joinToString(separator = multiply) {
        if (it.value.toInt() == 1) {
            it.key.latexName()
        } else {
            "${it.key.latexName()}^{${it.value}}"
        }
    }
    return formatMonomialTerm(
        coefficient = coefficient,
        variable = variable,
        options = options,
        ops = ops
    )
}

/**
 * 将线性多项式转换为 LaTeX 字符串
 * Convert a linear polynomial to LaTeX string.
 *
 * @param ops 数值运算接口 / Number operations interface
 * @param options LaTeX 格式化选项 / LaTeX formatting options
 * @return LaTeX 格式字符串 / LaTeX format string
 */
fun <T> LinearPolynomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatexString(ops, options),
                negative = ops.isNegative(monomial.coefficient)
            )
        )
    }
    if (!ops.isZero(constant)) {
        terms.add(
            SignedTerm(
                body = ops.format(ops.abs(constant)),
                negative = ops.isNegative(constant)
            )
        )
    }
    return mergeTerms(terms, options)
}

/**
 * 将二次多项式转换为 LaTeX 字符串
 * Convert a quadratic polynomial to LaTeX string.
 *
 * @param ops 数值运算接口 / Number operations interface
 * @param options LaTeX 格式化选项 / LaTeX formatting options
 * @return LaTeX 格式字符串 / LaTeX format string
 */
fun <T> QuadraticPolynomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatexString(ops, options),
                negative = ops.isNegative(monomial.coefficient)
            )
        )
    }
    if (!ops.isZero(constant)) {
        terms.add(
            SignedTerm(
                body = ops.format(ops.abs(constant)),
                negative = ops.isNegative(constant)
            )
        )
    }
    return mergeTerms(terms, options)
}

/**
 * 将规范多项式转换为 LaTeX 字符串
 * Convert a canonical polynomial to LaTeX string.
 *
 * @param ops 数值运算接口 / Number operations interface
 * @param options LaTeX 格式化选项 / LaTeX formatting options
 * @return LaTeX 格式字符串 / LaTeX format string
 */
fun <T> CanonicalPolynomial<T>.toLatexString(
    ops: LatexNumberOps<T>,
    options: LatexOptions = LatexOptions()
): String where T : Ring<T> {
    val terms = ArrayList<SignedTerm>(monomials.size + 1)
    for (monomial in monomials) {
        if (ops.isZero(monomial.coefficient)) {
            continue
        }
        terms.add(
            SignedTerm(
                body = monomial.copy(coefficient = ops.abs(monomial.coefficient)).toLatexString(ops, options),
                negative = ops.isNegative(monomial.coefficient)
            )
        )
    }
    if (!ops.isZero(constant)) {
        terms.add(
            SignedTerm(
                body = ops.format(ops.abs(constant)),
                negative = ops.isNegative(constant)
            )
        )
    }
    return mergeTerms(terms, options)
}
