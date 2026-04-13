/**
 * 符号表达式序列化
 * Symbol Expression Serialization
 *
 * 提供多项式和不等式与表达式 AST 之间的双向转换，以及 JSON 序列化支持。
 * 支持将表达式解析为线性、二次或规范多项式/不等式，以及反向转换。
 * Provides bidirectional conversion between polynomials/inequalities and expression AST,
 * as well as JSON serialization support.
 * Supports parsing expressions into linear, quadratic, or canonical polynomials/inequalities,
 * and reverse conversion.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.IdentifiedSymbol
import fuookami.ospf.kotlin.math.symbol.OwnedSymbol
import fuookami.ospf.kotlin.math.symbol.OwnedSymbolLike
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.SymbolId
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.parser.BinaryOperator
import fuookami.ospf.kotlin.math.symbol.parser.ComparisonOperator
import fuookami.ospf.kotlin.math.symbol.parser.Expr
import fuookami.ospf.kotlin.math.symbol.parser.Flt64NumberParser
import fuookami.ospf.kotlin.math.symbol.parser.NumberParser
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineCanonicalPolynomialTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalInequality
import fuookami.ospf.kotlin.math.symbol.operation.toCanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.toLinearInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toLinearPolynomialOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequalityOrNull
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomialOrNull
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.utils.serialization.writeJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayInputStream
import java.math.BigDecimal

typealias SymbolExpr = Expr

/**
 * 序列化符号标识符的前缀标记
 * Prefix marker for serialized symbol identifiers
 *
 * 用于区分普通符号名和包含完整身份信息的序列化标识符。
 * 以此后缀开头的字符串会被解析为 [SymbolIdentityExpr]。
 * Used to distinguish plain symbol names from serialized identifiers containing full identity information.
 * Strings starting with this prefix are parsed as [SymbolIdentityExpr].
 */
const val SerializedSymbolIdentityPrefix = "__ospf_symbol_identity__"

private data class DefaultSymbol(
    override val name: String,
    override val displayName: String? = null
) : Symbol

private fun defaultSymbolOf(name: String): Symbol {
    return DefaultSymbol(name)
}

/**
 * 符号身份表达式
 * Symbol Identity Expression
 *
 * 描述符号的完整身份信息，支持简单名称、带 ID 的符号和复合符号。
 * 用于序列化时保留符号的完整身份信息（包括 name、displayName 和 symbolId）。
 * Describes complete identity information for symbols, supporting simple names,
 * ID-bearing symbols, and composite symbols. Used during serialization to preserve
 * complete symbol identity (including name, displayName, and symbolId).
 */
@Serializable
sealed interface SymbolIdentityExpr {
    val name: String
    val displayName: String?

    /**
     * 简单符号（仅名称和可选显示名）
     * Simple symbol (name and optional display name only)
     */
    @Serializable
    data class Simple(
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 带唯一标识符的符号
     * Symbol with a unique identifier
     */
    @Serializable
    data class WithId(
        override val name: String,
        val id: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 复合符号（单参数运算符包装）
     * Composite symbol (single-argument operator wrapper)
     */
    @Serializable
    data class Composite(
        val operator: String,
        val arg: SymbolIdentityExpr,
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 复合符号（多参数运算符包装）
     * Composite symbol (multi-argument operator wrapper)
     */
    @Serializable
    data class CompositeMulti(
        val operator: String,
        val args: List<SymbolIdentityExpr>,
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr
}

private data class SerializedIdentitySymbol(
    val identityExpr: SymbolIdentityExpr
) : Symbol, IdentifiedSymbol {
    override val name: String = identityExpr.name
    override val displayName: String? = identityExpr.displayName
    override val symbolId: String = identityExpr.toSerializedIdentifier()
}

private fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { value -> "%02x".format(value) }
}

private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0) {
        return null
    }
    return try {
        ByteArray(length / 2) { index ->
            val start = index * 2
            substring(start, start + 2).toInt(16).toByte()
        }
    } catch (_: NumberFormatException) {
        null
    }
}

private val symbolIdentityJson = Json {
    ignoreUnknownKeys = true
}

private fun JsonObject.stringOrNull(key: String): String? {
    val element = this[key] ?: return null
    return try {
        element.jsonPrimitive.content
    } catch (_: Exception) {
        null
    }
}

private fun identityExprFromJsonElement(element: JsonElement): SymbolIdentityExpr? {
    val objectValue = element as? JsonObject ?: return null
    val name = objectValue.stringOrNull("name")
    val displayName = objectValue.stringOrNull("displayName")
    val operator = objectValue.stringOrNull("operator")
    val id = objectValue.stringOrNull("id")
    val args = objectValue["args"] as? JsonArray
    val arg = objectValue["arg"]
    return when {
        operator != null && args != null -> {
            val parsedArgs = args.mapNotNull(::identityExprFromJsonElement)
            if (parsedArgs.size != args.size) {
                null
            } else {
                SymbolIdentityExpr.CompositeMulti(
                    operator = operator,
                    args = parsedArgs,
                    name = name ?: operator,
                    displayName = displayName
                )
            }
        }

        operator != null && arg != null -> {
            val parsedArg = identityExprFromJsonElement(arg) ?: return null
            SymbolIdentityExpr.Composite(
                operator = operator,
                arg = parsedArg,
                name = name ?: operator,
                displayName = displayName
            )
        }

        id != null && name != null -> {
            SymbolIdentityExpr.WithId(
                name = name,
                id = id,
                displayName = displayName
            )
        }

        name != null -> {
            SymbolIdentityExpr.Simple(
                name = name,
                displayName = displayName
            )
        }

        else -> null
    }
}

private fun parseIdentityExprOrNull(json: String): SymbolIdentityExpr? {
    return try {
        identityExprFromJsonElement(symbolIdentityJson.parseToJsonElement(json))
    } catch (_: Exception) {
        null
    }
}

/**
 * 将符号身份表达式序列化为字符串标识符
 * Serialize a symbol identity expression to a string identifier
 *
 * 对于简单的 [SymbolIdentityExpr.Simple]（无 displayName 且不以 [SerializedSymbolIdentityPrefix] 开头），
 * 直接返回名称。其他情况将表达式序列化为 JSON 并进行 hex 编码，添加前缀标记。
 * For simple [SymbolIdentityExpr.Simple] (no displayName and not starting with [SerializedSymbolIdentityPrefix]),
 * returns the name directly. Otherwise, serializes the expression to JSON, hex-encodes it, and adds the prefix marker.
 *
 * @return 字符串标识符 / String identifier
 */
fun SymbolIdentityExpr.toSerializedIdentifier(): String {
    if (this is SymbolIdentityExpr.Simple && displayName == null && !name.startsWith(SerializedSymbolIdentityPrefix)) {
        return name
    }
    val payload = writeJson(this).toByteArray(Charsets.UTF_8).toHexString()
    return "$SerializedSymbolIdentityPrefix$payload"
}

/**
 * 将 Symbol 转换为其身份表达式表示
 * Convert a Symbol to its identity expression representation
 *
 * 根据 Symbol 的具体类型，提取完整的身份信息并构造对应的 [SymbolIdentityExpr]。
 * 支持 [SerializedIdentitySymbol]、[OwnedSymbolLike]、[IdentifiedSymbol] 和普通 Symbol。
 * Extracts complete identity information based on the Symbol's concrete type
 * and constructs the corresponding [SymbolIdentityExpr].
 * Supports [SerializedIdentitySymbol], [OwnedSymbolLike], [IdentifiedSymbol], and plain Symbol.
 *
 * @return 符号身份表达式 / Symbol identity expression
 */
fun Symbol.toSymbolIdentityExpr(): SymbolIdentityExpr {
    return when (this) {
        is SerializedIdentitySymbol -> identityExpr
        is OwnedSymbolLike -> SymbolIdentityExpr.WithId(
            name = name,
            id = id.value,
            displayName = displayName
        )
        is IdentifiedSymbol -> SymbolIdentityExpr.WithId(
            name = name,
            id = symbolId,
            displayName = displayName
        )
        else -> SymbolIdentityExpr.Simple(
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 从序列化标识符还原 Symbol
 * Reconstruct a Symbol from a serialized identifier
 *
 * 如果标识符以 [SerializedSymbolIdentityPrefix] 开头，则解析其中包含的 JSON 身份信息
 * 并构造对应的 Symbol；否则作为普通符号名创建默认 Symbol。
 * 这是 [Symbol.toSymbolIdentityExpr] 的逆操作。
 * If the identifier starts with [SerializedSymbolIdentityPrefix], parses the embedded JSON identity
 * information and constructs the corresponding Symbol; otherwise creates a default Symbol with
 * the identifier as a plain name. This is the inverse operation of [Symbol.toSymbolIdentityExpr].
 *
 * @param identifier 序列化标识符 / Serialized identifier
 * @return 还原的符号 / Reconstructed symbol
 */
fun symbolOfSerializedIdentifier(identifier: String): Symbol {
    if (!identifier.startsWith(SerializedSymbolIdentityPrefix)) {
        return defaultSymbolOf(identifier)
    }
    val payload = identifier.removePrefix(SerializedSymbolIdentityPrefix)
    val bytes = payload.hexToByteArrayOrNull() ?: return defaultSymbolOf(identifier)
    val rawJson = bytes.toString(Charsets.UTF_8)
    val identityExpr = parseIdentityExprOrNull(rawJson) ?: return defaultSymbolOf(identifier)
    return when (identityExpr) {
        is SymbolIdentityExpr.Simple -> defaultSymbolOf(identityExpr.name).let { symbol ->
            if (identityExpr.displayName == null) symbol else DefaultSymbol(identityExpr.name, identityExpr.displayName)
        }

        is SymbolIdentityExpr.WithId -> OwnedSymbol(
            id = SymbolId(identityExpr.id),
            name = identityExpr.name,
            displayName = identityExpr.displayName
        )

        is SymbolIdentityExpr.Composite,
        is SymbolIdentityExpr.CompositeMulti -> SerializedIdentitySymbol(identityExpr)
    }
}

private fun formatNumber(value: Flt64): String {
    return BigDecimal.valueOf(value.toDouble()).stripTrailingZeros().toPlainString()
}

private fun numberExpr(value: Flt64): Expr.NumberLiteral {
    return Expr.NumberLiteral(formatNumber(value))
}

private fun symbolExpr(symbol: Symbol): Expr.Identifier {
    return Expr.Identifier(symbol.toSymbolIdentityExpr().toSerializedIdentifier())
}

private fun powerExpr(
    base: Expr,
    exponent: Int
): Expr {
    return if (exponent == 1) {
        base
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

private fun canonicalMonomialToExpr(monomial: CanonicalMonomial<Flt64>): Expr {
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

fun CanonicalPolynomial<Flt64>.toExpr(symbolComparator: Comparator<Symbol>? = null): SymbolExpr {
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

fun LinearInequality<Flt64>.toExpr(): Expr.Comparison {
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
    lhs: CanonicalPolynomial<Flt64>,
    rhs: CanonicalPolynomial<Flt64>
): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial<Flt64>(
        monomials = lhs.monomials + rhs.monomials,
        constant = lhs.constant + rhs.constant
    ).combineTerms()
}

private fun negateCanonical(polynomial: CanonicalPolynomial<Flt64>): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial<Flt64>(
        monomials = polynomial.monomials.map { it.copy(coefficient = -it.coefficient) },
        constant = -polynomial.constant
    )
}

private fun subtractCanonical(
    lhs: CanonicalPolynomial<Flt64>,
    rhs: CanonicalPolynomial<Flt64>
): CanonicalPolynomial<Flt64> {
    return addCanonical(lhs, negateCanonical(rhs))
}

private fun multiplyCanonical(
    lhs: CanonicalPolynomial<Flt64>,
    rhs: CanonicalPolynomial<Flt64>
): CanonicalPolynomial<Flt64> {
    val monomials = ArrayList<CanonicalMonomial<Flt64>>(lhs.monomials.size * rhs.monomials.size + lhs.monomials.size + rhs.monomials.size)
    for (left in lhs.monomials) {
        for (right in rhs.monomials) {
            monomials.add(
                CanonicalMonomial<Flt64>(
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
    return CanonicalPolynomial<Flt64>(
        monomials = monomials,
        constant = lhs.constant * rhs.constant
    ).combineTerms()
}

private fun parseExponent(text: String): Int {
    val doubleValue = text.toDoubleOrNull()
        ?: throw IllegalArgumentException("Invalid exponent '$text'.")
    val rounded = doubleValue.toInt()
    require(rounded.toDouble() == doubleValue) {
        "Exponent must be an integer."
    }
    return rounded
}

// ============================================================================
// Generic Typed Canonical Polynomial Operations (Ring-based)
// ============================================================================

private fun <T> addTypedCanonical(
    lhs: CanonicalPolynomial<T>,
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return CanonicalPolynomial(
        monomials = lhs.monomials + rhs.monomials,
        constant = lhs.constant + rhs.constant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

private fun <T> negateTypedCanonical(
    polynomial: CanonicalPolynomial<T>
): CanonicalPolynomial<T> where T : Ring<T> {
    return CanonicalPolynomial(
        monomials = polynomial.monomials.map { it.copy(coefficient = -it.coefficient) },
        constant = -polynomial.constant
    )
}

private fun <T> subtractTypedCanonical(
    lhs: CanonicalPolynomial<T>,
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return addTypedCanonical(lhs, negateTypedCanonical(rhs), zero, isZero, symbolComparator)
}

private fun <T> multiplyTypedCanonical(
    lhs: CanonicalPolynomial<T>,
    rhs: CanonicalPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    val monomials = ArrayList<CanonicalMonomial<T>>(lhs.monomials.size * rhs.monomials.size + lhs.monomials.size + rhs.monomials.size)
    for (left in lhs.monomials) {
        for (right in rhs.monomials) {
            monomials.add(
                CanonicalMonomial(
                    coefficient = left.coefficient * right.coefficient,
                    powers = mergePowers(left.powers, right.powers)
                )
            )
        }
        if (!isZero(rhs.constant)) {
            monomials.add(
                left.copy(coefficient = left.coefficient * rhs.constant)
            )
        }
    }
    for (right in rhs.monomials) {
        if (!isZero(lhs.constant)) {
            monomials.add(
                right.copy(coefficient = right.coefficient * lhs.constant)
            )
        }
    }
    return CanonicalPolynomial(
        monomials = monomials,
        constant = lhs.constant * rhs.constant
    ).combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

private fun <T> powTypedCanonical(
    base: CanonicalPolynomial<T>,
    exponent: Int,
    zero: T,
    one: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    require(exponent >= 0) {
        "Negative exponent is not supported for polynomial conversion."
    }
    var result = CanonicalPolynomial<T>(constant = one)
    repeat(exponent) {
        result = multiplyTypedCanonical(result, base, zero, isZero, symbolComparator)
    }
    return result
}

fun <T> Expr.toCanonicalPolynomialTyped(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): CanonicalPolynomial<T> where T : Ring<T> {
    return when (this) {
        is Expr.NumberLiteral -> {
            val number = numberParser.parse(text)
                ?: throw IllegalArgumentException("Invalid number literal '$text' for target type.")
            CanonicalPolynomial<T>(constant = number)
        }

        is Expr.Identifier -> {
            CanonicalPolynomial(
                monomials = listOf(
                    CanonicalMonomial(
                        coefficient = one,
                        powers = mapOf(symbolOf(name) to Int32.one)
                    )
                ),
                constant = zero
            )
        }

        is Expr.UnaryMinus -> {
            negateTypedCanonical(
                operand.toCanonicalPolynomialTyped(
                    numberParser = numberParser,
                    zero = zero,
                    one = one,
                    symbolOf = symbolOf,
                    isZero = isZero,
                    symbolComparator = symbolComparator
                )
            )
        }

        is Expr.Binary -> {
            val leftPolynomial = left.toCanonicalPolynomialTyped(
                numberParser = numberParser,
                zero = zero,
                one = one,
                symbolOf = symbolOf,
                isZero = isZero,
                symbolComparator = symbolComparator
            )
            val rightPolynomial = right.toCanonicalPolynomialTyped(
                numberParser = numberParser,
                zero = zero,
                one = one,
                symbolOf = symbolOf,
                isZero = isZero,
                symbolComparator = symbolComparator
            )
            when (operator) {
                BinaryOperator.Add -> addTypedCanonical(leftPolynomial, rightPolynomial, zero, isZero, symbolComparator)
                BinaryOperator.Subtract -> subtractTypedCanonical(leftPolynomial, rightPolynomial, zero, isZero, symbolComparator)
                BinaryOperator.Multiply -> multiplyTypedCanonical(leftPolynomial, rightPolynomial, zero, isZero, symbolComparator)
                BinaryOperator.Power -> {
                    val exponent = when (right) {
                        is Expr.NumberLiteral -> parseExponent(right.text)
                        else -> throw IllegalArgumentException("Exponent must be number literal.")
                    }
                    powTypedCanonical(leftPolynomial, exponent, zero, one, isZero, symbolComparator)
                }
            }
        }

        is Expr.FunctionCall -> {
            throw IllegalArgumentException("Cannot convert function call '${name}' to canonical polynomial.")
        }

        is Expr.Comparison -> {
            throw IllegalArgumentException("Cannot convert comparison expression to canonical polynomial.")
        }
    }.combineCanonicalPolynomialTerms(zero, isZero, symbolComparator)
}

fun <T> Expr.toLinearPolynomialTypedOrNull(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero }
): LinearPolynomial<T>? where T : Ring<T> = toCanonicalPolynomialTyped(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero
).toLinearPolynomialOrNull(
    zero = zero,
    isZero = isZero
)

fun <T> Expr.toQuadraticPolynomialTypedOrNull(
    numberParser: NumberParser<T>,
    zero: T,
    one: T,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<T>? where T : Ring<T> = toCanonicalPolynomialTyped(
    numberParser = numberParser,
    zero = zero,
    one = one,
    symbolOf = symbolOf,
    isZero = isZero,
    symbolComparator = symbolComparator
).toQuadraticPolynomialOrNull(
    zero = zero,
    isZero = isZero,
    symbolComparator = symbolComparator
)

private fun powCanonical(
    base: CanonicalPolynomial<Flt64>,
    exponent: Int
): CanonicalPolynomial<Flt64> {
    require(exponent >= 0) {
        "Negative exponent is not supported for polynomial conversion."
    }
    var result = CanonicalPolynomial<Flt64>(constant = Flt64.one)
    repeat(exponent) {
        result = multiplyCanonical(result, base)
    }
    return result
}

fun Expr.toCanonicalPolynomial(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): CanonicalPolynomial<Flt64> {
    return toCanonicalPolynomialTyped(
        numberParser = Flt64NumberParser,
        zero = Flt64.zero,
        one = Flt64.one,
        symbolOf = symbolOf,
        isZero = { it == Flt64.zero }
    ).combineTerms()
}

fun Expr.toLinearPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearPolynomial<Flt64>? {
    return toCanonicalPolynomial(symbolOf).toLinearPolynomialOrNull()
}

fun Expr.toQuadraticPolynomialOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return toCanonicalPolynomial(symbolOf).toQuadraticPolynomialOrNull(symbolComparator)
}

fun Expr.Comparison.toCanonicalInequality(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): CanonicalInequality {
    return CanonicalInequality(
        lhs = left.toCanonicalPolynomial(symbolOf),
        rhs = right.toCanonicalPolynomial(symbolOf),
        comparison = exprOperatorToComparison(operator)
    )
}

fun Expr.Comparison.toLinearInequalityOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): LinearInequality<Flt64>? {
    return toCanonicalInequality(symbolOf).toLinearInequalityOrNull()
}

fun Expr.Comparison.toQuadraticInequalityOrNull(
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
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

// ============================================================================
// Polynomial 便捷序列化方法 / Polynomial convenience serialization methods
// ============================================================================

/**
 * 将 LinearPolynomial 序列化为 JSON 字符串
 * Serialize LinearPolynomial to JSON string
 */
fun LinearPolynomial<Flt64>.toJsonString(): String {
    return this.toExpr().toJsonString()
}

/**
 * 将 QuadraticPolynomial 序列化为 JSON 字符串
 * Serialize QuadraticPolynomial to JSON string
 */
fun QuadraticPolynomial<Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toExpr(symbolComparator).toJsonString()
}

/**
 * 将 CanonicalPolynomial 序列化为 JSON 字符串
 * Serialize CanonicalPolynomial to JSON string
 */
fun CanonicalPolynomial<Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toExpr(symbolComparator).toJsonString()
}

// ============================================================================
// Inequality 便捷序列化方法 / Inequality convenience serialization methods
// ============================================================================

/**
 * 将 LinearInequality 序列化为 JSON 字符串
 * Serialize LinearInequality to JSON string
 */
fun LinearInequality<Flt64>.toJsonString(): String {
    return this.toExpr().toJsonString()
}

/**
 * 将 QuadraticInequality 序列化为 JSON 字符串
 * Serialize QuadraticInequality to JSON string
 */
fun QuadraticInequality.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toExpr(symbolComparator).toJsonString()
}

/**
 * 将 CanonicalInequality 序列化为 JSON 字符串
 * Serialize CanonicalInequality to JSON string
 */
fun CanonicalInequality.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return this.toExpr(symbolComparator).toJsonString()
}

// ============================================================================
// 从 JSON 反序列化 / Deserialize from JSON
// ============================================================================

/**
 * 从 JSON 字符串解析为 LinearPolynomial
 * Parse LinearPolynomial from JSON string
 */
fun linearPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearPolynomial<Flt64>? {
    return symbolExprFromJson(json).toLinearPolynomialOrNull(symbolOf)
}

/**
 * 从 JSON 字符串解析为 QuadraticPolynomial
 * Parse QuadraticPolynomial from JSON string
 */
fun quadraticPolynomialFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticPolynomial<Flt64>? {
    return symbolExprFromJson(json).toQuadraticPolynomialOrNull(symbolOf, symbolComparator)
}

/**
 * 从 JSON 字符串解析为 CanonicalPolynomial
 * Parse CanonicalPolynomial from JSON string
 */
fun canonicalPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalPolynomial<Flt64> {
    return symbolExprFromJson(json).toCanonicalPolynomial(symbolOf)
}

/**
 * 从 JSON 字符串解析为 LinearInequality
 * Parse LinearInequality from JSON string
 */
fun linearInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearInequality<Flt64>? {
    val expr = symbolExprFromJson(json)
    return (expr as? Expr.Comparison)?.toLinearInequalityOrNull(symbolOf)
}

/**
 * 从 JSON 字符串解析为 QuadraticInequality
 * Parse QuadraticInequality from JSON string
 */
fun quadraticInequalityFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier,
    symbolComparator: Comparator<Symbol>? = null
): QuadraticInequality? {
    val expr = symbolExprFromJson(json)
    return (expr as? Expr.Comparison)?.toQuadraticInequalityOrNull(symbolOf, symbolComparator)
}

/**
 * 从 JSON 字符串解析为 CanonicalInequality
 * Parse CanonicalInequality from JSON string
 */
fun canonicalInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalInequality? {
    val expr = symbolExprFromJson(json)
    return (expr as? Expr.Comparison)?.toCanonicalInequality(symbolOf)
}
