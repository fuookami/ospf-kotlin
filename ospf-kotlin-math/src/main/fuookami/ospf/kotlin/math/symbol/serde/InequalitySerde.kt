/**
 * 不等式序列化与反序列化
 * Inequality Serialization and Deserialization
 *
 * 提供不等式对象与 JSON 之间的双向转换功能。
 * Provides bidirectional conversion between inequality objects and JSON.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.operation.combineTerms
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.utils.serialization.writeJson
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream

// ============================================================================
// DTO data classes for inequality JSON serialization
// ============================================================================

/**
 * 规范不等式 DTO
 * Canonical inequality DTO
 */
@Serializable
data class CanonicalInequalityData(
    val lhs: CanonicalPolynomialData,
    val rhs: CanonicalPolynomialData,
    val comparison: String
)

/**
 * 线性不等式 DTO
 * Linear inequality DTO
 */
@Serializable
data class LinearInequalityData(
    val lhs: LinearPolynomialData,
    val rhs: LinearPolynomialData,
    val comparison: String,
    val name: String = "",
    val displayName: String = ""
)

/**
 * 二次不等式 DTO
 * Quadratic inequality DTO
 */
@Serializable
data class QuadraticInequalityData(
    val lhs: QuadraticPolynomialData,
    val rhs: QuadraticPolynomialData,
    val comparison: String,
    val name: String = "",
    val displayName: String = ""
)

// ============================================================================
// Comparison <-> String conversions
// ============================================================================

private fun Comparison.toDtoString(): String {
    return when (this) {
        Comparison.LT -> "LT"
        Comparison.LE -> "LE"
        Comparison.EQ -> "EQ"
        Comparison.NE -> "NE"
        Comparison.GE -> "GE"
        Comparison.GT -> "GT"
    }
}

private fun comparisonFromDtoString(str: String): Comparison {
    return when (str) {
        "LT" -> Comparison.LT
        "LE" -> Comparison.LE
        "EQ" -> Comparison.EQ
        "NE" -> Comparison.NE
        "GE" -> Comparison.GE
        "GT" -> Comparison.GT
        else -> throw IllegalArgumentException("Unknown comparison operator: $str")
    }
}

// ============================================================================
// Inequality -> DTO conversions
// ============================================================================

private fun Symbol.toDtoIdentifier(): String {
    return toSymbolIdentityExpr().toSerializedIdentifier()
}

private fun CanonicalInequality.toDto(): CanonicalInequalityData {
    return CanonicalInequalityData(
        lhs = lhs.combineTerms().let { CanonicalPolynomialData(
            monomials = it.monomials.map { m -> CanonicalMonomialData(
                coefficient = m.coefficient.value,
                powers = m.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }
            ) },
            constant = it.constant.value
        ) },
        rhs = rhs.combineTerms().let { CanonicalPolynomialData(
            monomials = it.monomials.map { m -> CanonicalMonomialData(
                coefficient = m.coefficient.value,
                powers = m.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }
            ) },
            constant = it.constant.value
        ) },
        comparison = comparison.toDtoString()
    )
}

private fun LinearInequality<Flt64>.toDto(): LinearInequalityData {
    return LinearInequalityData(
        lhs = LinearPolynomialData(
            monomials = lhs.monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) },
            constant = lhs.constant.value
        ),
        rhs = LinearPolynomialData(
            monomials = rhs.monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) },
            constant = rhs.constant.value
        ),
        comparison = comparison.toDtoString(),
        name = name,
        displayName = displayName
    )
}

private fun QuadraticInequalityOf<Flt64>.toDto(): QuadraticInequalityData {
    return QuadraticInequalityData(
        lhs = QuadraticPolynomialData(
            monomials = lhs.monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) },
            constant = lhs.constant.value
        ),
        rhs = QuadraticPolynomialData(
            monomials = rhs.monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) },
            constant = rhs.constant.value
        ),
        comparison = comparison.toDtoString(),
        name = name,
        displayName = displayName
    )
}

// ============================================================================
// DTO -> Inequality conversions
// ============================================================================

private fun CanonicalInequalityData.toDomain(symbolOf: (String) -> Symbol): CanonicalInequality {
    return CanonicalInequality(
        lhs = CanonicalPolynomial(
            monomials = lhs.monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) },
            constant = Flt64(lhs.constant)
        ),
        rhs = CanonicalPolynomial(
            monomials = rhs.monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) },
            constant = Flt64(rhs.constant)
        ),
        comparison = comparisonFromDtoString(comparison)
    )
}

private fun LinearInequalityData.toDomain(symbolOf: (String) -> Symbol): LinearInequality<Flt64> {
    return LinearInequality(
        lhs = LinearPolynomial(
            monomials = lhs.monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) },
            constant = Flt64(lhs.constant)
        ),
        rhs = LinearPolynomial(
            monomials = rhs.monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) },
            constant = Flt64(rhs.constant)
        ),
        comparison = comparisonFromDtoString(comparison),
        name = name,
        displayName = displayName
    )
}

private fun QuadraticInequalityData.toDomain(symbolOf: (String) -> Symbol): QuadraticInequalityOf<Flt64> {
    return QuadraticInequalityOf(
        lhs = QuadraticPolynomial(
            monomials = lhs.monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) },
            constant = Flt64(lhs.constant)
        ),
        rhs = QuadraticPolynomial(
            monomials = rhs.monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) },
            constant = Flt64(rhs.constant)
        ),
        comparison = comparisonFromDtoString(comparison),
        name = name,
        displayName = displayName
    )
}

// ============================================================================
// Public API: toJsonString
// ============================================================================

fun CanonicalInequality.toJsonString(): String {
    return writeJson(toDto())
}

fun LinearInequality<Flt64>.toJsonString(): String {
    return writeJson(toDto())
}

fun QuadraticInequalityOf<Flt64>.toJsonString(): String {
    return writeJson(toDto())
}

// ============================================================================
// Public API: fromJson
// ============================================================================

fun canonicalInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalInequality {
    val dto = readFromJson<CanonicalInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toDomain(symbolOf)
}

fun linearInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearInequality<Flt64> {
    val dto = readFromJson<LinearInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toDomain(symbolOf)
}

fun quadraticInequalityFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): QuadraticInequality {
    val dto = readFromJson<QuadraticInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toDomain(symbolOf)
}
