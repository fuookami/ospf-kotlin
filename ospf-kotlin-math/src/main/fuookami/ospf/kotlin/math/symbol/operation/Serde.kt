/**
 * Flt64 序列化与反序列化
 * Flt64 Serialization and Deserialization
 *
 * 提供 Flt64 多项式和不等式的 JSON 序列化与反序列化功能。
 * Provides JSON serialization and deserialization for Flt64 polynomials and inequalities.
 */
package fuookami.ospf.kotlin.math.symbol.operation

import java.io.ByteArrayInputStream
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.serde.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.serialization.*

/**
 * 将 Flt64 规范多项式序列化为 JSON 字符串
 * Serialize a Flt64 canonical polynomial to a JSON string
 *
 * @param symbolComparator 符号比较器 / Symbol comparator
 * @return JSON 字符串 / JSON string
 */
fun CanonicalPolynomial<Flt64>.toJsonString(symbolComparator: Comparator<Symbol>? = null): String {
    return writeJson(combineTerms(symbolComparator).toFlt64Dto())
}

/**
 * 将 Flt64 线性多项式序列化为 JSON 字符串
 * Serialize a Flt64 linear polynomial to a JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun LinearPolynomial<Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

/**
 * 将 Flt64 二次多项式序列化为 JSON 字符串
 * Serialize a Flt64 quadratic polynomial to a JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun QuadraticPolynomial<Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

/**
 * 将 Flt64 线性不等式序列化为 JSON 字符串
 * Serialize a Flt64 linear inequality to a JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun LinearInequality<Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

/**
 * 将 Flt64 二次不等式序列化为 JSON 字符串
 * Serialize a Flt64 quadratic inequality to a JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun QuadraticInequalityOf<Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

/**
 * 将 Flt64 规范不等式序列化为 JSON 字符串
 * Serialize a Flt64 canonical inequality to a JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun CanonicalInequality<Flt64>.toJsonString(): String {
    return writeJson(toFlt64Dto())
}

/**
 * 从 JSON 字符串反序列化为 Flt64 规范多项式
 * Deserialize a JSON string to a Flt64 canonical polynomial
 *
 * @param json JSON 字符串 / JSON string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 规范多项式 / Canonical polynomial
 */
fun canonicalPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): CanonicalPolynomial<Flt64> {
    val dto = readFromJson<CanonicalPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf).combineTerms()
}

/**
 * 从 JSON 字符串反序列化为 Flt64 线性多项式
 * Deserialize a JSON string to a Flt64 linear polynomial
 *
 * @param json JSON 字符串 / JSON string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 线性多项式，若不可转换则返回 null / Linear polynomial, or null if not convertible
 */
fun linearPolynomialFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): LinearPolynomial<Flt64>? {
    val dto = readFromJson<LinearPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf).toLinearPolynomialOrNull()
}

/**
 * 从 JSON 字符串反序列化为 Flt64 二次多项式
 * Deserialize a JSON string to a Flt64 quadratic polynomial
 *
 * @param json JSON 字符串 / JSON string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 二次多项式，若不可转换则返回 null / Quadratic polynomial, or null if not convertible
 */
fun quadraticPolynomialFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): QuadraticPolynomial<Flt64>? {
    val dto = readFromJson<QuadraticPolynomialData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf).toQuadraticPolynomialOrNull()
}

/**
 * 从 JSON 字符串反序列化为 Flt64 线性不等式
 * Deserialize a JSON string to a Flt64 linear inequality
 *
 * @param json JSON 字符串 / JSON string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 线性不等式 / Linear inequality
 */
fun linearInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): Ret<LinearInequality<Flt64>> {
    val dto = readFromJson<LinearInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf)
}

/**
 * 从 JSON 字符串反序列化为 Flt64 二次不等式
 * Deserialize a JSON string to a Flt64 quadratic inequality
 *
 * @param json JSON 字符串 / JSON string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 二次不等式 / Quadratic inequality
 */
fun quadraticInequalityFromJson(
    json: String,
    symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier
): Ret<QuadraticInequalityOf<Flt64>> {
    val dto = readFromJson<QuadraticInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf)
}

/**
 * 从 JSON 字符串反序列化为 Flt64 规范不等式
 * Deserialize a JSON string to a Flt64 canonical inequality
 *
 * @param json JSON 字符串 / JSON string
 * @param symbolOf 符号解析函数 / Symbol resolution function
 * @return 规范不等式 / Canonical inequality
 */
fun canonicalInequalityFromJson(json: String, symbolOf: (String) -> Symbol = ::symbolOfSerializedIdentifier): Ret<CanonicalInequality<Flt64>> {
    val dto = readFromJson<CanonicalInequalityData>(ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)))
    return dto.toFlt64Domain(symbolOf)
}

// ============================================================================
// Flt64-specific DTO conversions (internal to adapter)
// ============================================================================

/** 将符号转换为 DTO 标识符字符串 / Convert a symbol to a DTO identifier string */
private fun Symbol.toDtoIdentifier(): String {
    return toSymbolIdentityExpr().toSerializedIdentifier()
}

/** 将比较运算符转换为 DTO 字符串 / Convert a comparison operator to a DTO string */
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

/**
 * 从 DTO 字符串解析比较运算符
 * Parse a comparison operator from a DTO string
 *
 * @param str 比较运算符字符串 / Comparison operator string
 * @return 比较运算符结果 / Comparison operator result
 */
private fun comparisonFromDtoString(str: String): Ret<Comparison> {
    return when (str) {
        "LT" -> Ok(Comparison.LT)
        "LE" -> Ok(Comparison.LE)
        "EQ" -> Ok(Comparison.EQ)
        "NE" -> Ok(Comparison.NE)
        "GE" -> Ok(Comparison.GE)
        "GT" -> Ok(Comparison.GT)
        else -> Failed(ErrorCode.IllegalArgument, "Unknown comparison operator: $str")
    }
}

/** 转换 Flt64 规范多项式为 DTO / Convert Flt64 canonical polynomial to DTO */
internal fun CanonicalPolynomial<Flt64>.toFlt64Dto(): CanonicalPolynomialData {
    return CanonicalPolynomialData(
        monomials = monomials.map { CanonicalMonomialData(it.coefficient.value, it.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }) },
        constant = constant.value
    )
}

/** 转换 Flt64 线性多项式为 DTO / Convert Flt64 linear polynomial to DTO */
internal fun LinearPolynomial<Flt64>.toFlt64Dto(): LinearPolynomialData {
    return LinearPolynomialData(
        monomials = monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) },
        constant = constant.value
    )
}

/** 转换 Flt64 二次多项式为 DTO / Convert Flt64 quadratic polynomial to DTO */
internal fun QuadraticPolynomial<Flt64>.toFlt64Dto(): QuadraticPolynomialData {
    return QuadraticPolynomialData(
        monomials = monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) },
        constant = constant.value
    )
}

/** 转换 Flt64 线性不等式为 DTO / Convert Flt64 linear inequality to DTO */
internal fun LinearInequality<Flt64>.toFlt64Dto(): LinearInequalityData {
    return LinearInequalityData(
        lhs = LinearPolynomialData(lhs.monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) }, lhs.constant.value),
        rhs = LinearPolynomialData(rhs.monomials.map { LinearMonomialData(it.coefficient.value, it.symbol.toDtoIdentifier()) }, rhs.constant.value),
        comparison = comparison.toDtoString(),
        name = name,
        displayName = displayName
    )
}

/** 转换 Flt64 二次不等式为 DTO / Convert Flt64 quadratic inequality to DTO */
internal fun QuadraticInequalityOf<Flt64>.toFlt64Dto(): QuadraticInequalityData {
    return QuadraticInequalityData(
        lhs = QuadraticPolynomialData(lhs.monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) }, lhs.constant.value),
        rhs = QuadraticPolynomialData(rhs.monomials.map { QuadraticMonomialData(it.coefficient.value, it.symbol1.toDtoIdentifier(), it.symbol2?.toDtoIdentifier()) }, rhs.constant.value),
        comparison = comparison.toDtoString(),
        name = name,
        displayName = displayName
    )
}

/** 转换 Flt64 规范不等式为 DTO / Convert Flt64 canonical inequality to DTO */
internal fun CanonicalInequality<Flt64>.toFlt64Dto(): CanonicalInequalityData {
    val combinedLhs = lhs.combineTerms()
    val combinedRhs = rhs.combineTerms()
    return CanonicalInequalityData(
        lhs = CanonicalPolynomialData(combinedLhs.monomials.map { m -> CanonicalMonomialData(m.coefficient.value, m.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }) }, combinedLhs.constant.value),
        rhs = CanonicalPolynomialData(combinedRhs.monomials.map { m -> CanonicalMonomialData(m.coefficient.value, m.powers.mapKeys { it.key.toDtoIdentifier() }.mapValues { it.value.toInt() }) }, combinedRhs.constant.value),
        comparison = comparison.toDtoString()
    )
}

/** 转换规范多项式数据为 Flt64 域 / Convert canonical polynomial data to Flt64 domain */
internal fun CanonicalPolynomialData.toFlt64Domain(symbolOf: (String) -> Symbol): CanonicalPolynomial<Flt64> {
    return CanonicalPolynomial(
        monomials = monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) },
        constant = Flt64(constant)
    )
}

/** 转换线性多项式数据为 Flt64 域 / Convert linear polynomial data to Flt64 domain */
internal fun LinearPolynomialData.toFlt64Domain(symbolOf: (String) -> Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(
        monomials = monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) },
        constant = Flt64(constant)
    )
}

/** 转换二次多项式数据为 Flt64 域 / Convert quadratic polynomial data to Flt64 domain */
internal fun QuadraticPolynomialData.toFlt64Domain(symbolOf: (String) -> Symbol): QuadraticPolynomial<Flt64> {
    return QuadraticPolynomial(
        monomials = monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) },
        constant = Flt64(constant)
    )
}

/** 转换线性不等式数据为 Flt64 域 / Convert linear inequality data to Flt64 domain */
internal fun LinearInequalityData.toFlt64Domain(symbolOf: (String) -> Symbol): Ret<LinearInequality<Flt64>> {
    return comparisonFromDtoString(comparison).map { comparison ->
        LinearInequality(
            lhs = LinearPolynomial(lhs.monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) }, Flt64(lhs.constant)),
            rhs = LinearPolynomial(rhs.monomials.map { LinearMonomial(Flt64(it.coefficient), symbolOf(it.symbol)) }, Flt64(rhs.constant)),
            comparison = comparison,
            name = name,
            displayName = displayName
        )
    }
}

/** 转换二次不等式数据为 Flt64 域 / Convert quadratic inequality data to Flt64 domain */
internal fun QuadraticInequalityData.toFlt64Domain(symbolOf: (String) -> Symbol): Ret<QuadraticInequalityOf<Flt64>> {
    return comparisonFromDtoString(comparison).map { comparison ->
        QuadraticInequalityOf(
            lhs = QuadraticPolynomial(lhs.monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) }, Flt64(lhs.constant)),
            rhs = QuadraticPolynomial(rhs.monomials.map { QuadraticMonomial(Flt64(it.coefficient), symbolOf(it.symbol1), it.symbol2?.let { symbolOf(it) }) }, Flt64(rhs.constant)),
            comparison = comparison,
            name = name,
            displayName = displayName
        )
    }
}

/** 转换标准不等式数据为 Flt64 域 / Convert canonical inequality data to Flt64 domain */
internal fun CanonicalInequalityData.toFlt64Domain(symbolOf: (String) -> Symbol): Ret<CanonicalInequality<Flt64>> {
    return comparisonFromDtoString(comparison).map { comparison ->
        CanonicalInequality<Flt64>(
            lhs = CanonicalPolynomial(lhs.monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) }, Flt64(lhs.constant)),
            rhs = CanonicalPolynomial(rhs.monomials.map { CanonicalMonomial(Flt64(it.coefficient), it.powers.mapKeys { symbolOf(it.key) }.mapValues { Int32(it.value) }) }, Flt64(rhs.constant)),
            comparison = comparison
        )
    }
}
