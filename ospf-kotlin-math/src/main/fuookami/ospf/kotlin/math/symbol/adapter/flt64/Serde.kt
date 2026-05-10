package fuookami.ospf.kotlin.math.symbol.adapter.flt64

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int32
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.CanonicalInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.serde.*
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.utils.serialization.writeJson
import java.io.ByteArrayInputStream

fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return writeJson(combineTerms(symbolComparator).toFlt64Dto())
}

fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

fun QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

fun canonicalPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val dto = readFromJson<CanonicalPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf).combineTerms()
}

fun linearPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    val dto = readFromJson<LinearPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf).toLinearPolynomialOrNull()
}

fun quadraticPolynomialFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>? {
    val dto = readFromJson<QuadraticPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf).toQuadraticPolynomialOrNull()
}

fun linearInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val dto = readFromJson<LinearInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf)
}

fun quadraticInequalityFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val dto = readFromJson<QuadraticInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf)
}

fun canonicalInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    val dto = readFromJson<CanonicalInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf)
}

// ============================================================================
// Flt64-specific DTO conversions (internal to adapter)
// ============================================================================

private fun Symbol.toDtoIdentifier(): String {
    return toSymbolIdentityExpr().toSerializedIdentifier()
}

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

internal fun CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64Dto(): CanonicalPolynomialData {
    return CanonicalPolynomialData(
        monomials = monomials.map { CanonicalMonomialData(it.coefficient.value, it.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }) },
        constant = constant.value
    )
}

internal fun LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64Dto(): LinearPolynomialData {
    return LinearPolynomialData(
        monomials = monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) },
        constant = constant.value
    )
}

internal fun QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64Dto(): QuadraticPolynomialData {
    return QuadraticPolynomialData(
        monomials = monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) },
        constant = constant.value
    )
}

internal fun LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64Dto(): LinearInequalityData {
    return LinearInequalityData(
        lhs = LinearPolynomialData(lhs.monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) }, lhs.constant.value),
        rhs = LinearPolynomialData(rhs.monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) }, rhs.constant.value),
        comparison = comparison.toDtoString(),
        name = name,
        displayName = displayName
    )
}

internal fun QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64Dto(): QuadraticInequalityData {
    return QuadraticInequalityData(
        lhs = QuadraticPolynomialData(lhs.monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) }, lhs.constant.value),
        rhs = QuadraticPolynomialData(rhs.monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) }, rhs.constant.value),
        comparison = comparison.toDtoString(),
        name = name,
        displayName = displayName
    )
}

internal fun CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>.toFlt64Dto(): CanonicalInequalityData {
    val combinedLhs = lhs.combineTerms()
    val combinedRhs = rhs.combineTerms()
    return CanonicalInequalityData(
        lhs = CanonicalPolynomialData(combinedLhs.monomials.map { m -> CanonicalMonomialData(m.coefficient.value, m.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }) }, combinedLhs.constant.value),
        rhs = CanonicalPolynomialData(combinedRhs.monomials.map { m -> CanonicalMonomialData(m.coefficient.value, m.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }) }, combinedRhs.constant.value),
        comparison = comparison.toDtoString()
    )
}

internal fun CanonicalPolynomialData.toFlt64Domain(symbolOf: (String) -> Symbol): CanonicalPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return CanonicalPolynomial(
        monomials = monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) },
        constant = Flt64(constant)
    )
}

internal fun LinearPolynomialData.toFlt64Domain(symbolOf: (String) -> Symbol): LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearPolynomial(
        monomials = monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) },
        constant = Flt64(constant)
    )
}

internal fun QuadraticPolynomialData.toFlt64Domain(symbolOf: (String) -> Symbol): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return QuadraticPolynomial(
        monomials = monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) },
        constant = Flt64(constant)
    )
}

internal fun LinearInequalityData.toFlt64Domain(symbolOf: (String) -> Symbol): LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return LinearInequality(
        lhs = LinearPolynomial(lhs.monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) }, Flt64(lhs.constant)),
        rhs = LinearPolynomial(rhs.monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) }, Flt64(rhs.constant)),
        comparison = comparisonFromDtoString(comparison),
        name = name,
        displayName = displayName
    )
}

internal fun QuadraticInequalityData.toFlt64Domain(symbolOf: (String) -> Symbol): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return QuadraticInequalityOf(
        lhs = QuadraticPolynomial(lhs.monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) }, Flt64(lhs.constant)),
        rhs = QuadraticPolynomial(rhs.monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) }, Flt64(rhs.constant)),
        comparison = comparisonFromDtoString(comparison),
        name = name,
        displayName = displayName
    )
}

internal fun CanonicalInequalityData.toFlt64Domain(symbolOf: (String) -> Symbol): CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
    return CanonicalInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
        lhs = CanonicalPolynomial(lhs.monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) }, Flt64(lhs.constant)),
        rhs = CanonicalPolynomial(rhs.monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) }, Flt64(rhs.constant)),
        comparison = comparisonFromDtoString(comparison)
    )
}
