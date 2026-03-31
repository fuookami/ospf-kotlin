package fuookami.ospf.kotlin.utils.math.symbol.serde

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.utils.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.utils.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.utils.math.symbol.parser.BinaryOperator
import fuookami.ospf.kotlin.utils.math.symbol.parser.ComparisonOperator
import fuookami.ospf.kotlin.utils.math.symbol.parser.Expr
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.utils.math.symbol.operation.toCanonicalInequality
import fuookami.ospf.kotlin.utils.math.symbol.operation.toCanonicalPolynomial
import fuookami.ospf.kotlin.utils.math.symbol.operation.toLinearInequalityOrNull
import fuookami.ospf.kotlin.utils.math.symbol.operation.toLinearPolynomialOrNull
import fuookami.ospf.kotlin.utils.math.symbol.operation.toQuadraticInequalityOrNull
import fuookami.ospf.kotlin.utils.math.symbol.operation.toQuadraticPolynomialOrNull
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.utils.serialization.writeJson
import java.io.ByteArrayInputStream
import java.math.BigDecimal

typealias SymbolExpr = Expr

private data class DefaultSymbol(
    override val name: String,
    override val displayName: String? = null
) : Symbol

private fun defaultSymbolOf(name: String): Symbol {
    return DefaultSymbol(name)
}

private fun formatNumber(value: Flt64): String {
    return BigDecimal.valueOf(value.toDouble()).stripTrailingZeros().toPlainString()
}

private fun numberExpr(value: Flt64): Expr.NumberLiteral {
    return Expr.NumberLiteral(formatNumber(value))
}

private fun symbolExpr(symbol: Symbol): Expr.Identifier {
    return Expr.Identifier(symbol.name)
}

private fun powerExpr(
    base: Expr,
    exponent: Int
): Expr {
    return if (exponent == 1) {
        base
    } else if (exponent == -1) {
        // 负指数：1/x 形式
        Expr.Binary(
            left = Expr.NumberLiteral("1"),
            operator = BinaryOperator.Divide,
            right = base
        )
    } else if (exponent < 0) {
        // 负指数：1/x^n 形式
        Expr.Binary(
            left = Expr.NumberLiteral("1"),
            operator = BinaryOperator.Divide,
            right = Expr.Binary(
                left = base,
                operator = BinaryOperator.Power,
                right = Expr.NumberLiteral((-exponent).toString())
            )
        )
    } else {
        Expr.Binary(
            left = base,
            operator = BinaryOperator.Power,
            right = Expr.NumberLiteral(exponent.toString())
        )
    }
}

private fun multiplyExpr(
    left: Expr,
    right: Expr
): Expr {
    return Expr.Binary(
        left = left,
        operator = BinaryOperator.Multiply,
        right = right
    )
}

private fun scaleExpr(
    coefficient: Flt64,
    body: Expr
): Expr {
    return if (coefficient == Flt64.one) {
        body
    } else {
        multiplyExpr(numberExpr(coefficient), body)
    }
}

private fun combineSignedTerms(terms: List<Pair<Boolean, Expr>>): Expr {
    if (terms.isEmpty()) {
        return Expr.NumberLiteral("0")
    }
    var expression = if (terms.first().first) {
        Expr.UnaryMinus(terms.first().second)
    } else {
        terms.first().second
    }
    for (i in 1 until terms.size) {
        val (negative, term) = terms[i]
        expression = Expr.Binary(
            left = expression,
            operator = if (negative) BinaryOperator.Subtract else BinaryOperator.Add,
            right = term
        )
    }
    return expression
}

private fun canonicalMonomialToExpr(monomial: CanonicalMonomial<Flt64, Int32>): Expr {
    if (monomial.powers.isEmpty()) {
        return numberExpr(monomial.coefficient)
    }
    val factorExpressions = monomial.powers.map { (symbol, exp) ->
        powerExpr(symbolExpr(symbol), exp.toInt())
    }
    val product = factorExpressions.reduce { left, right -> multiplyExpr(left, right) }
    val absCoefficient = if (monomial.coefficient < Flt64.zero) {
        -monomial.coefficient
    } else {
        monomial.coefficient
    }
    return scaleExpr(absCoefficient, product)
}

fun LinearPolynomial<Flt64>.toExpr(): SymbolExpr {
    return this.toCanonicalPolynomial().toExpr()
}

fun QuadraticPolynomial<Flt64>.toExpr(symbolComparator: Comparator<Symbol>? = null): SymbolExpr {
    return this.toCanonicalPolynomial(symbolComparator).toExpr()
}

fun CanonicalPolynomial<Flt64, Int32>.toExpr(symbolComparator: Comparator<Symbol>? = null): SymbolExpr {
    val source = this.combineTerms(symbolComparator)
    val terms = ArrayList<Pair<Boolean, Expr>>(source.monomials.size + 1)
    for (monomial in source.monomials) {
        if (monomial.coefficient == Flt64.zero) {
            continue
        }
        val negative = monomial.coefficient < Flt64.zero
        val normalized = if (negative) {
            monomial.copy(coefficient = -monomial.coefficient)
        } else {
            monomial
        }
        terms.add(negative to canonicalMonomialToExpr(normalized))
    }
    if (source.constant != Flt64.zero) {
        val negative = source.constant < Flt64.zero
        val normalized = if (negative) -source.constant else source.constant
        terms.add(negative to numberExpr(normalized))
    }
    return combineSignedTerms(terms)
}

private fun comparisonToExprOperator(comparison: Comparison): ComparisonOperator {
    return when (comparison) {
        Comparison.LT -> ComparisonOperator.Less
        Comparison.LE -> ComparisonOperator.LessEqual
        Comparison.EQ -> ComparisonOperator.Equal
        Comparison.NE -> ComparisonOperator.NotEqual
        Comparison.GE -> ComparisonOperator.GreaterEqual
        Comparison.GT -> ComparisonOperator.Greater
    }
}

private fun exprOperatorToComparison(operator: ComparisonOperator): Comparison {
    return when (operator) {
        ComparisonOperator.Less -> Comparison.LT
        ComparisonOperator.LessEqual -> Comparison.LE
        ComparisonOperator.Equal -> Comparison.EQ
        ComparisonOperator.NotEqual -> Comparison.NE
        ComparisonOperator.GreaterEqual -> Comparison.GE
        ComparisonOperator.Greater -> Comparison.GT
    }
}

fun LinearInequality.toExpr(): Expr.Comparison {
    return this.toCanonicalInequality().toExpr()
}

fun QuadraticInequality.toExpr(symbolComparator: Comparator<Symbol>? = null): Expr.Comparison {
    return this.toCanonicalInequality(symbolComparator).toExpr()
}

fun CanonicalInequality.toExpr(symbolComparator: Comparator<Symbol>? = null): Expr.Comparison {
    return Expr.Comparison(
        left = lhs.toExpr(symbolComparator),
        operator = comparisonToExprOperator(comparison),
        right = rhs.toExpr(symbolComparator)
    )
}

/**
 * 合并两个 powers Map
 * Merge two powers maps
 */
private fun mergePowers(left: Map<Symbol, Int32>, right: Map<Symbol, Int32>): Map<Symbol, Int32> {
    val result = LinkedHashMap<Symbol, Int32>(left)
    for ((symbol, exp) in right) {
        result[symbol] = (result[symbol] ?: Int32.zero) + exp
    }
    return result
}

private fun addCanonical(
    lhs: CanonicalPolynomial<Flt64, Int32>,
    rhs: CanonicalPolynomial<Flt64, Int32>
): CanonicalPolynomial<Flt64, Int32> {
    return CanonicalPolynomial<Flt64, Int32>(
        monomials = lhs.monomials + rhs.monomials,
        constant = lhs.constant + rhs.constant
    ).combineTerms()
}

private fun negateCanonical(polynomial: CanonicalPolynomial<Flt64, Int32>): CanonicalPolynomial<Flt64, Int32> {
    return CanonicalPolynomial<Flt64, Int32>(
        monomials = polynomial.monomials.map { it.copy(coefficient = -it.coefficient) },
        constant = -polynomial.constant
    )
}

private fun subtractCanonical(
    lhs: CanonicalPolynomial<Flt64, Int32>,
    rhs: CanonicalPolynomial<Flt64, Int32>
): CanonicalPolynomial<Flt64, Int32> {
    return addCanonical(lhs, negateCanonical(rhs))
}

private fun multiplyCanonical(
    lhs: CanonicalPolynomial<Flt64, Int32>,
    rhs: CanonicalPolynomial<Flt64, Int32>
): CanonicalPolynomial<Flt64, Int32> {
    val monomials = ArrayList<CanonicalMonomial<Flt64, Int32>>(lhs.monomials.size * rhs.monomials.size + lhs.monomials.size + rhs.monomials.size)
    for (left in lhs.monomials) {
        for (right in rhs.monomials) {
            monomials.add(
                CanonicalMonomial<Flt64, Int32>(
                    coefficient = left.coefficient * right.coefficient,
                    powers = mergePowers(left.powers, right.powers)
                )
            )
        }
        if (rhs.constant != Flt64.zero) {
            monomials.add(
                left.copy(coefficient = left.coefficient * rhs.constant)
            )
        }
    }
    for (right in rhs.monomials) {
        if (lhs.constant != Flt64.zero) {
            monomials.add(
                right.copy(coefficient = right.coefficient * lhs.constant)
            )
        }
    }
    return CanonicalPolynomial<Flt64, Int32>(
        monomials = monomials,
        constant = lhs.constant * rhs.constant
    ).combineTerms()
}

private fun parseNonNegativeExponent(text: String): Int {
    val doubleValue = text.toDoubleOrNull()
        ?: throw IllegalArgumentException("Invalid exponent '$text'.")
    require(doubleValue >= 0.0) {
        "Exponent must be non-negative."
    }
    val rounded = doubleValue.toInt()
    require(rounded.toDouble() == doubleValue) {
        "Exponent must be an integer."
    }
    return rounded
}

private fun powCanonical(
    base: CanonicalPolynomial<Flt64, Int32>,
    exponent: Int
): CanonicalPolynomial<Flt64, Int32> {
    var result = CanonicalPolynomial<Flt64, Int32>(constant = Flt64.one)
    repeat(exponent) {
        result = multiplyCanonical(result, base)
    }
    return result
}

fun Expr.toCanonicalPolynomial(
    symbolOf: (String) -> Symbol = ::defaultSymbolOf
): CanonicalPolynomial<Flt64, Int32> {
    return when (this) {
        is Expr.NumberLiteral -> {
            CanonicalPolynomial<Flt64, Int32>(constant = Flt64(text.toDouble()))
        }

        is Expr.Identifier -> {
            CanonicalPolynomial<Flt64, Int32>(
                monomials = listOf(
                    CanonicalMonomial<Flt64, Int32>(
                        coefficient = Flt64.one,
                        powers = mapOf(symbolOf(name) to Int32.one)
                    )
                ),
                constant = Flt64.zero
            )
        }

        is Expr.UnaryMinus -> {
            negateCanonical(operand.toCanonicalPolynomial(symbolOf))
        }

        is Expr.Binary -> {
            val leftPolynomial = left.toCanonicalPolynomial(symbolOf)
            val rightPolynomial = right.toCanonicalPolynomial(symbolOf)
            when (operator) {
                BinaryOperator.Add -> addCanonical(leftPolynomial, rightPolynomial)
                BinaryOperator.Subtract -> subtractCanonical(leftPolynomial, rightPolynomial)
                BinaryOperator.Multiply -> multiplyCanonical(leftPolynomial, rightPolynomial)
                BinaryOperator.Power -> {
                    val exponent = when (right) {
                        is Expr.NumberLiteral -> parseNonNegativeExponent(right.text)
                        else -> throw IllegalArgumentException("Exponent must be number literal.")
                    }
                    powCanonical(leftPolynomial, exponent)
                }
            }
        }

        is Expr.FunctionCall -> {
            throw IllegalArgumentException("Cannot convert function call '${name}' to canonical polynomial.")
        }

        is Expr.Comparison -> {
            throw IllegalArgumentException("Cannot convert comparison expression to canonical polynomial.")
        }
    }.combineTerms()
}

fun Expr.toLinearPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::defaultSymbolOf
): LinearPolynomial<Flt64>? {
    return toCanonicalPolynomial(symbolOf).toLinearPolynomialOrNull()
}

fun Expr.toQuadraticPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::defaultSymbolOf,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return toCanonicalPolynomial(symbolOf).toQuadraticPolynomialOrNull(symbolComparator)
}

fun Expr.Comparison.toCanonicalInequality(
    symbolOf: (String) -> Symbol = ::defaultSymbolOf
): CanonicalInequality {
    return CanonicalInequality(
        lhs = left.toCanonicalPolynomial(symbolOf),
        rhs = right.toCanonicalPolynomial(symbolOf),
        comparison = exprOperatorToComparison(operator)
    )
}

fun Expr.Comparison.toLinearInequalityOrNull(
    symbolOf: (String) -> Symbol = ::defaultSymbolOf
): LinearInequality? {
    return toCanonicalInequality(symbolOf).toLinearInequalityOrNull()
}

fun Expr.Comparison.toQuadraticInequalityOrNull(
    symbolOf: (String) -> Symbol = ::defaultSymbolOf,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequality? {
    return toCanonicalInequality(symbolOf).toQuadraticInequalityOrNull(symbolComparator)
}

fun SymbolExpr.toJsonString(): String {
    return writeJson(this)
}

fun symbolExprFromJson(json: String): SymbolExpr {
    return readFromJson(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
}