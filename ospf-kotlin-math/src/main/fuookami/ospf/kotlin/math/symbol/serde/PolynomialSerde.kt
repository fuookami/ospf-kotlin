/**
 * 多项式序列化与反序列化
 * Polynomial Serialization and Deserialization
 *
 * 提供多项式对象与 JSON 之间的双向转换功能。
 * Provides bidirectional conversion between polynomial objects and JSON.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.combineCanonicalPolynomialTerms
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.utils.serialization.writeJson
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream

// ============================================================================
// DTO data classes for polynomial JSON serialization
// ============================================================================

/**
 * 规范多项式 DTO
 * Canonical polynomial DTO
 */
@Serializable
data class CanonicalPolynomialData(
    val monomials: List<CanonicalMonomialData> = emptyList(),
    val constant: Double
)

/**
 * 规范单项式 DTO
 * Canonical monomial DTO
 */
@Serializable
data class CanonicalMonomialData(
    val coefficient: Double,
    val powers: Map<String, Int> = emptyMap()
)

/**
 * 线性多项式 DTO
 * Linear polynomial DTO
 */
@Serializable
data class LinearPolynomialData(
    val monomials: List<LinearMonomialData> = emptyList(),
    val constant: Double
)

/**
 * 线性单项式 DTO
 * Linear monomial DTO
 */
@Serializable
data class LinearMonomialData(
    val coefficient: Double,
    val symbol: String
)

/**
 * 二次多项式 DTO
 * Quadratic polynomial DTO
 */
@Serializable
data class QuadraticPolynomialData(
    val monomials: List<QuadraticMonomialData> = emptyList(),
    val constant: Double
)

/**
 * 二次单项式 DTO
 * Quadratic monomial DTO
 */
@Serializable
data class QuadraticMonomialData(
    val coefficient: Double,
    val symbol1: String,
    val symbol2: String? = null
)

// ============================================================================
// Polynomial -> DTO conversions
// ============================================================================

private fun Symbol.toDtoIdentifier(): String {
    return toSymbolIdentityExpr().toSerializedIdentifier()
}

private fun CanonicalMonomial<F64>.toDto(): CanonicalMonomialData {
    return CanonicalMonomialData(
        coefficient = coefficient.value,
        powers = powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }
    )
}

private fun LinearMonomial<F64>.toDto(): LinearMonomialData {
    return LinearMonomialData(
        coefficient = coefficient.value,
        symbol = symbol.toDtoIdentifier()
    )
}

private fun QuadraticMonomial<F64>.toDto(): QuadraticMonomialData {
    return QuadraticMonomialData(
        coefficient = coefficient.value,
        symbol1 = symbol1.toDtoIdentifier(),
        symbol2 = symbol2?.toDtoIdentifier()
    )
}

private fun CanonicalPolynomial<F64>.toDto(): CanonicalPolynomialData {
    return CanonicalPolynomialData(
        monomials = monomials.map { it.toDto() },
        constant = constant.value
    )
}

private fun LinearPolynomial<F64>.toDto(): LinearPolynomialData {
    return LinearPolynomialData(
        monomials = monomials.map { it.toDto() },
        constant = constant.value
    )
}

private fun QuadraticPolynomial<F64>.toDto(): QuadraticPolynomialData {
    return QuadraticPolynomialData(
        monomials = monomials.map { it.toDto() },
        constant = constant.value
    )
}

// ============================================================================
// DTO -> Polynomial conversions
// ============================================================================

private fun CanonicalMonomialData.toDomain(symbolOf: (String) -> Symbol): CanonicalMonomial<F64> {
    return CanonicalMonomial(
        coefficient = Flt64(coefficient),
        powers = powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }
    )
}

private fun LinearMonomialData.toDomain(symbolOf: (String) -> Symbol): LinearMonomial<F64> {
    return LinearMonomial(
        coefficient = Flt64(coefficient),
        symbol = symbolOf(symbol)
    )
}

private fun QuadraticMonomialData.toDomain(symbolOf: (String) -> Symbol): QuadraticMonomial<F64> {
    return QuadraticMonomial(
        coefficient = Flt64(coefficient),
        symbol1 = symbolOf(symbol1),
        symbol2 = symbol2?.let { symbolOf(it) }
    )
}

private fun CanonicalPolynomialData.toDomain(symbolOf: (String) -> Symbol): CanonicalPolynomial<F64> {
    return CanonicalPolynomial(
        monomials = monomials.map { it.toDomain(symbolOf) },
        constant = Flt64(constant)
    )
}

private fun LinearPolynomialData.toDomain(symbolOf: (String) -> Symbol): LinearPolynomial<F64> {
    return LinearPolynomial(
        monomials = monomials.map { it.toDomain(symbolOf) },
        constant = Flt64(constant)
    )
}

private fun QuadraticPolynomialData.toDomain(symbolOf: (String) -> Symbol): QuadraticPolynomial<F64> {
    return QuadraticPolynomial(
        monomials = monomials.map { it.toDomain(symbolOf) },
        constant = Flt64(constant)
    )
}

// ============================================================================
// Public API: toJsonString
// ============================================================================

fun CanonicalPolynomial<F64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return writeJson(combineTerms(symbolComparator).toDto())
}

fun LinearPolynomial<F64>.toJsonString(): String {
    return writeJson(toDto())
}

fun QuadraticPolynomial<F64>.toJsonString(): String {
    return writeJson(toDto())
}

// ============================================================================
// Public API: fromJson
// ============================================================================

fun canonicalPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalPolynomial<F64> {
    val dto = readFromJson<CanonicalPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toDomain(symbolOf).combineTerms()
}

fun linearPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearPolynomial<F64>? {
    val dto = readFromJson<LinearPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toDomain(symbolOf).toLinearPolynomialOrNull()
}

fun quadraticPolynomialFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): QuadraticPolynomial<F64>? {
    val dto = readFromJson<QuadraticPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toDomain(symbolOf).toQuadraticPolynomialOrNull()
}