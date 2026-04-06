/**
 * 矩阵形式
 * Matrix Form
 *
 * 提供多项式与矩阵形式之间的双向转换。
 * 线性多项式可表示为 c'x + d，二次多项式可表示为 x'Qx + c'x + d。
 * 支持泛型 Ring<T> 和 Flt64 特化版本。
 * Provides bidirectional conversion between polynomials and matrix form.
 * Linear polynomials can be represented as c'x + d,
 * quadratic polynomials as x'Qx + c'x + d.
 * Supports generic Ring<T> and Flt64 specialized versions.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.defaultSymbolComparator
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// Typed Matrix Form Data Classes for DoubleArray/Flt64
// ============================================================================

data class LinearMatrixForm(
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

data class QuadraticMatrixForm(
    val q: Array<DoubleArray>,
    val c: DoubleArray,
    val d: Flt64,
    val order: List<Symbol>
)

// ============================================================================
// Typed Matrix Form Data Classes for Generic Ring<T>
// ============================================================================

data class TypedLinearMatrixForm<T>(
    val c: List<T>,
    val d: T,
    val order: List<Symbol>
) where T : Ring<T>

data class TypedQuadraticMatrixForm<T>(
    val q: List<List<T>>,
    val c: List<T>,
    val d: T,
    val order: List<Symbol>
) where T : Ring<T>

// ============================================================================
// Validation Functions
// ============================================================================

private fun validateOrder(order: List<Symbol>) {
    require(order.toSet().size == order.size) { "Symbol order contains duplicated symbols." }
}

private fun validateLinearMatrixDimensions(
    c: DoubleArray,
    order: List<Symbol>
) {
    require(c.size == order.size) {
        "Linear matrix form dimension mismatch: c.size=${c.size}, order.size=${order.size}."
    }
}

private fun <T> validateLinearMatrixDimensions(
    c: List<T>,
    order: List<Symbol>
) {
    require(c.size == order.size) {
        "Linear matrix form dimension mismatch: c.size=${c.size}, order.size=${order.size}."
    }
}

private fun validateQuadraticMatrixDimensions(
    q: Array<DoubleArray>,
    c: DoubleArray,
    order: List<Symbol>
) {
    val n = order.size
    require(q.size == n) {
        "Quadratic matrix form dimension mismatch: q.size=${q.size}, order.size=$n."
    }
    require(q.all { it.size == n }) {
        "Quadratic matrix form requires square q with size order.size=$n."
    }
    require(c.size == n) {
        "Quadratic matrix form dimension mismatch: c.size=${c.size}, order.size=$n."
    }
}

private fun <T> validateQuadraticMatrixDimensions(
    q: List<List<T>>,
    c: List<T>,
    order: List<Symbol>
) {
    val n = order.size
    require(q.size == n) {
        "Quadratic matrix form dimension mismatch: q.size=${q.size}, order.size=$n."
    }
    require(q.all { it.size == n }) {
        "Quadratic matrix form requires square q with size order.size=$n."
    }
    require(c.size == n) {
        "Quadratic matrix form dimension mismatch: c.size=${c.size}, order.size=$n."
    }
}

// ============================================================================
// Typed Linear Polynomial to Matrix Form (Ring<T>)
// ============================================================================

fun <T> LinearPolynomial<T>.toTypedMatrixForm(
    order: List<Symbol>,
    zero: T,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero }
): TypedLinearMatrixForm<T> where T : Ring<T> {
    validateOrder(order)

    val source = if (combineTerms) {
        this.combineLinearTerms(
            zero = zero,
            isZero = isZero
        )
    } else {
        this
    }

    val c = MutableList(order.size) { zero }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in source.monomials) {
        val i = indexOfSymbol[monomial.symbol]
            ?: throw IllegalArgumentException("Symbol ${monomial.symbol.name} not found in order.")
        c[i] = c[i] + monomial.coefficient
    }
    return TypedLinearMatrixForm(
        c = c,
        d = source.constant,
        order = order
    )
}

fun <T> typedLinearPolynomialFromMatrixForm(
    c: List<T>,
    d: T,
    order: List<Symbol>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    validateOrder(order)
    validateLinearMatrixDimensions(c, order)

    val monomials = ArrayList<LinearMonomial<T>>(order.size)
    for (i in order.indices) {
        if (!isZero(c[i])) {
            monomials.add(
                LinearMonomial(
                    coefficient = c[i],
                    symbol = order[i]
                )
            )
        }
    }
    return LinearPolynomial(
        monomials = monomials,
        constant = d
    ).combineLinearTerms(zero, isZero)
}

fun <T> typedLinearPolynomialFromMatrixForm(
    form: TypedLinearMatrixForm<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T> where T : Ring<T> {
    return typedLinearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order,
        zero = zero,
        isZero = isZero
    )
}

// ============================================================================
// Typed Quadratic Polynomial to Matrix Form (Ring<T>)
// ============================================================================

fun <T> QuadraticPolynomial<T>.toTypedMatrixForm(
    order: List<Symbol>,
    zero: T,
    splitOffDiagonal: (T) -> Pair<T, T>,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): TypedQuadraticMatrixForm<T> where T : Ring<T> {
    validateOrder(order)

    val source = if (combineTerms) {
        this.combineQuadraticTerms(
            zero = zero,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
    } else {
        this
    }

    val n = order.size
    val q = MutableList(n) { MutableList(n) { zero } }
    val c = MutableList(n) { zero }
    val indexOfSymbol = order.withIndex().associate { it.value to it.index }
    for (monomial in source.monomials) {
        if (monomial.isQuadratic) {
            val symbol2 = monomial.symbol2!!
            val i = indexOfSymbol[monomial.symbol1]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
            val j = indexOfSymbol[symbol2]
                ?: throw IllegalArgumentException("Symbol ${symbol2.name} not found in order.")
            if (i == j) {
                q[i][j] = q[i][j] + monomial.coefficient
            } else {
                val (left, right) = splitOffDiagonal(monomial.coefficient)
                q[i][j] = q[i][j] + left
                q[j][i] = q[j][i] + right
            }
        } else {
            val i = indexOfSymbol[monomial.symbol1]
                ?: throw IllegalArgumentException("Symbol ${monomial.symbol1.name} not found in order.")
            c[i] = c[i] + monomial.coefficient
        }
    }
    return TypedQuadraticMatrixForm(
        q = q,
        c = c,
        d = source.constant,
        order = order
    )
}

fun <T> CanonicalPolynomial<T>.toTypedMatrixForm(
    order: List<Symbol>,
    zero: T,
    splitOffDiagonal: (T) -> Pair<T, T>,
    combineTerms: Boolean = true,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): TypedQuadraticMatrixForm<T> where T : Ring<T> {
    validateOrder(order)

    val source = if (combineTerms) {
        this.combineCanonicalPolynomialTerms(
            zero = zero,
            isZero = isZero,
            symbolComparator = symbolComparator
        )
    } else {
        this
    }
    val quadratic = source.toQuadraticPolynomialOrNull(
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator
    ) ?: throw IllegalArgumentException("Canonical polynomial is not quadratic.")
    return quadratic.toTypedMatrixForm(
        order = order,
        zero = zero,
        splitOffDiagonal = splitOffDiagonal,
        combineTerms = false,
        isZero = isZero,
        symbolComparator = symbolComparator
    )
}

fun <T> typedQuadraticPolynomialFromMatrixForm(
    q: List<List<T>>,
    c: List<T>,
    d: T,
    order: List<Symbol>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    mergeOffDiagonal: (T, T) -> T = { lhs, rhs -> lhs + rhs },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    validateOrder(order)
    validateQuadraticMatrixDimensions(q, c, order)

    val monomials = ArrayList<QuadraticMonomial<T>>(order.size * (order.size + 1) / 2 + order.size)
    for (i in order.indices) {
        if (!isZero(q[i][i])) {
            monomials.add(
                QuadraticMonomial(
                    coefficient = q[i][i],
                    symbol1 = order[i],
                    symbol2 = order[i]
                )
            )
        }
        if (!isZero(c[i])) {
            monomials.add(
                QuadraticMonomial(
                    coefficient = c[i],
                    symbol1 = order[i],
                    symbol2 = null
                )
            )
        }
        for (j in (i + 1) until order.size) {
            val coefficient = mergeOffDiagonal(q[i][j], q[j][i])
            if (!isZero(coefficient)) {
                monomials.add(
                    QuadraticMonomial(
                        coefficient = coefficient,
                        symbol1 = order[i],
                        symbol2 = order[j]
                    )
                )
            }
        }
    }

    return QuadraticPolynomial(
        monomials = monomials,
        constant = d
    ).combineQuadraticTerms(
        zero = zero,
        isZero = isZero,
        symbolComparator = symbolComparator
    )
}

fun <T> typedQuadraticPolynomialFromMatrixForm(
    form: TypedQuadraticMatrixForm<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    mergeOffDiagonal: (T, T) -> T = { lhs, rhs -> lhs + rhs },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T> where T : Ring<T> {
    return typedQuadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order,
        zero = zero,
        isZero = isZero,
        mergeOffDiagonal = mergeOffDiagonal,
        symbolComparator = symbolComparator
    )
}

// ============================================================================
// Flt64-specific Matrix Form Operations (DoubleArray-based)
// ============================================================================

fun LinearPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): LinearMatrixForm {
    val form = toTypedMatrixForm(
        order = order,
        zero = Flt64.zero,
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )

    return LinearMatrixForm(
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun linearPolynomialFromMatrixForm(
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): LinearPolynomial<Flt64> {
    return typedLinearPolynomialFromMatrixForm(
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero }
    )
}

fun linearPolynomialFromMatrixForm(form: LinearMatrixForm): LinearPolynomial<Flt64> {
    return linearPolynomialFromMatrixForm(
        c = form.c,
        d = form.d,
        order = form.order
    )
}

fun QuadraticPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true
): QuadraticMatrixForm {
    val form = toTypedMatrixForm(
        order = order,
        zero = Flt64.zero,
        splitOffDiagonal = { coefficient ->
            val half = coefficient / Flt64.two
            half to half
        },
        combineTerms = combineTerms,
        isZero = { it == Flt64.zero }
    )

    return QuadraticMatrixForm(
        q = form.q.map { row -> row.map { it.toDouble() }.toDoubleArray() }.toTypedArray(),
        c = form.c.map { it.toDouble() }.toDoubleArray(),
        d = form.d,
        order = form.order
    )
}

fun quadraticPolynomialFromMatrixForm(
    q: Array<DoubleArray>,
    c: DoubleArray,
    d: Flt64,
    order: List<Symbol>
): QuadraticPolynomial<Flt64> {
    return typedQuadraticPolynomialFromMatrixForm(
        q = q.map { row -> row.map { Flt64(it) } },
        c = c.map { Flt64(it) },
        d = d,
        order = order,
        zero = Flt64.zero,
        isZero = { it == Flt64.zero },
        mergeOffDiagonal = { lhs, rhs -> lhs + rhs }
    )
}

fun quadraticPolynomialFromMatrixForm(form: QuadraticMatrixForm): QuadraticPolynomial<Flt64> {
    return quadraticPolynomialFromMatrixForm(
        q = form.q,
        c = form.c,
        d = form.d,
        order = form.order
    )
}

fun CanonicalPolynomial<Flt64>.toMatrixForm(
    order: List<Symbol>,
    combineTerms: Boolean = true,
    symbolComparator: java.util.Comparator<Symbol>? = null
): QuadraticMatrixForm {
    val source = if (combineTerms) {
        this.combineTerms(symbolComparator)
    } else {
        this
    }
    return when (val quadratic = source.toQuadraticPolynomialRet(symbolComparator)) {
        is Ok -> {
            quadratic.value.toMatrixForm(order = order, combineTerms = false)
        }

        is Failed -> {
            throw IllegalArgumentException(
                "Cannot convert canonical polynomial to quadratic matrix form: ${quadratic.error.message}"
            )
        }

        is Fatal -> {
            throw IllegalArgumentException(
                "Cannot convert canonical polynomial to quadratic matrix form: ${quadratic.errors.joinToString { it.message }}"
            )
        }
    }
}