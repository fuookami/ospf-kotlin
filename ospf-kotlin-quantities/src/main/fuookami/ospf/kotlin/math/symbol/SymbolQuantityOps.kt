/**
 * 符号多项式物理量运算扩展
 * Symbol Polynomial Quantity Operations Extensions
 *
 * 为符号多项式物理量提供单位转换、加减运算、标量乘除和求值扩展函数。
 * Provides unit conversion, addition/subtraction, scalar multiplication/division,
 * and evaluation extension functions for symbol polynomial quantities.
 *
 * 支持的多项式类型 / Supported polynomial types:
 * - LinearPolynomial: 线性多项式 / Linear polynomial
 * - QuadraticPolynomial: 二次多项式 / Quadratic polynomial
 * - CanonicalPolynomial: 规范多项式 / Canonical polynomial
 *
 * 支持的数值类型 / Supported number types:
 * - Flt64: 64位浮点数 / 64-bit floating-point
 * - FltX: 高精度浮点数 / High-precision floating-point
*/
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Ret

// ============================================================================
// 符号多项式物理量的单位转换
// Unit conversion for symbol polynomial quantities
// ============================================================================

/**
 * 线性多项式物理量的单位转换 (Flt64)
 * Unit conversion for linear polynomial quantities (Flt64)
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = LinearPolynomial(Flt64(100.0), "x") * Meter
 * val km = distance.to(Kilometer)  // Quantity(LinearPolynomial(Flt64(0.1), "x"), Kilometer)
 * ```
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果量纲不匹配 / Converted quantity, or null if dimensions don't match
*/
@JvmName("convertQuantityLinearFlt64")
fun Quantity<LinearPolynomial<Flt64>>.to(unit: PhysicalUnit): Quantity<LinearPolynomial<Flt64>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFlt64 = factor.toFlt64()

    return Quantity(
        value = LinearPolynomial(
            monomials = this.value.monomials.map { monomial ->
                LinearMonomial(monomial.coefficient * factorFlt64, monomial.symbol)
            },
            constant = this.value.constant * factorFlt64
        ),
        unit = unit
    )
}

/**
 * 线性多项式物理量的单位转换 (FltX)
 * Unit conversion for linear polynomial quantities (FltX)
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果量纲不匹配 / Converted quantity, or null if dimensions don't match
*/
@JvmName("convertQuantityLinearFltX")
fun Quantity<LinearPolynomial<FltX>>.to(unit: PhysicalUnit): Quantity<LinearPolynomial<FltX>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFltX = factor.toFltX()

    return Quantity(
        value = LinearPolynomial(
            monomials = this.value.monomials.map { monomial ->
                LinearMonomial(monomial.coefficient * factorFltX, monomial.symbol)
            },
            constant = this.value.constant * factorFltX
        ),
        unit = unit
    )
}

/**
 * 二次多项式物理量的单位转换 (Flt64)
 * Unit conversion for quadratic polynomial quantities (Flt64)
 *
 * 示例 / Example:
 * ```kotlin
 * val energy = QuadraticPolynomial(Flt64(100.0), "x", "x") * Joule
 * val kJ = energy.to(Kilojoule)
 * ```
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果量纲不匹配 / Converted quantity, or null if dimensions don't match
*/
@JvmName("convertQuantityQuadraticFlt64")
fun Quantity<QuadraticPolynomial<Flt64>>.to(unit: PhysicalUnit): Quantity<QuadraticPolynomial<Flt64>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFlt64 = factor.toFlt64()

    return Quantity(
        value = QuadraticPolynomial(
            monomials = this.value.monomials.map { monomial ->
                QuadraticMonomial(
                    coefficient = monomial.coefficient * factorFlt64,
                    symbol1 = monomial.symbol1,
                    symbol2 = monomial.symbol2
                )
            },
            constant = this.value.constant * factorFlt64
        ),
        unit = unit
    )
}

/**
 * 规范多项式物理量的单位转换 (Flt64)
 * Unit conversion for canonical polynomial quantities (Flt64)
 *
 * @param unit 目标单位 / Target unit
 * @return 转换后的物理量，或 null 如果量纲不匹配 / Converted quantity, or null if dimensions don't match
*/
@JvmName("convertQuantityCanonicalFlt64")
fun Quantity<CanonicalPolynomial<Flt64>>.to(unit: PhysicalUnit): Quantity<CanonicalPolynomial<Flt64>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFlt64 = factor.toFlt64()

    return Quantity(
        value = CanonicalPolynomial(
            monomials = this.value.monomials.map { monomial ->
                CanonicalMonomial(
                    coefficient = monomial.coefficient * factorFlt64,
                    powers = monomial.powers
                )
            },
            constant = this.value.constant * factorFlt64
        ),
        unit = unit
    )
}

// ============================================================================
// 符号多项式物理量的加减运算
// Addition and subtraction for symbol polynomial quantities
// ============================================================================

/** 安全转换符号物理量操作数 / Safely convert symbol quantity operand */
private fun <P> Quantity<P>.convertSymbolQuantityOperandSafe(
    other: Quantity<P>,
    operation: String,
    convert: Quantity<P>.(PhysicalUnit) -> Quantity<P>?
): Ret<Quantity<P>> {
    if (this.unit.quantity != other.unit.quantity) {
        return Failed(
            ErrorCode.IllegalArgument,
            "物理量量纲不匹配，无法执行$operation：期望 ${this.unit.quantity.dimensionSymbol()}，实际 ${other.unit.quantity.dimensionSymbol()}。 / " +
                    "Quantity dimension mismatch for $operation: expected ${this.unit.quantity.dimensionSymbol()}, got ${other.unit.quantity.dimensionSymbol()}."
        )
    }

    if (this.unit == other.unit) {
        return ok(other)
    }

    val converted = other.convert(this.unit)
        ?: return Failed(
            ErrorCode.IllegalArgument,
            "物理量单位转换失败，无法执行$operation。 / Quantity unit conversion failed for $operation."
        )
    return ok(converted)
}

/**
 * 线性多项式物理量的加法 (Flt64)
 * Addition for linear polynomial quantities (Flt64)
 *
 * 示例 / Example:
 * ```kotlin
 * val a = LinearPolynomial(Flt64(100.0), "x") * Meter
 * val b = LinearPolynomial(Flt64(50.0), "y") * Meter
 * val sum = a + b  // Quantity(LinearPolynomial(Flt64(100.0)x + Flt64(50.0)y), Meter)
 * ```
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量结果 / Sum quantity result
*/
@JvmName("plusQuantityLinearFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(
    other: Quantity<LinearPolynomial<Flt64>>
): Ret<Quantity<LinearPolynomial<Flt64>>> {
    return plusSafe(other)
}

/**
 * 安全执行线性多项式物理量的加法 (Flt64)
 * Safely add linear polynomial quantities (Flt64)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量结果 / Sum quantity result
*/
@JvmName("plusQuantityLinearFlt64Safe")
fun Quantity<LinearPolynomial<Flt64>>.plusSafe(
    other: Quantity<LinearPolynomial<Flt64>>
): Ret<Quantity<LinearPolynomial<Flt64>>> {
    return convertSymbolQuantityOperandSafe(other, "addition") { unit -> to(unit) }.map { otherConverted ->
        Quantity(
            value = this.value + otherConverted.value,
            unit = this.unit
        )
    }
}

/**
 * 尝试执行线性多项式物理量的加法 (Flt64)
 * Try to add linear polynomial quantities (Flt64)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量，失败时返回 null / Sum quantity, or null on failure
*/
@JvmName("plusQuantityLinearFlt64OrNull")
fun Quantity<LinearPolynomial<Flt64>>.plusOrNull(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>>? {
    return plusSafe(other).value
}

/**
 * 线性多项式物理量的减法 (Flt64)
 * Subtraction for linear polynomial quantities (Flt64)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量结果 / Difference quantity result
*/
@JvmName("minusQuantityLinearFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(
    other: Quantity<LinearPolynomial<Flt64>>
): Ret<Quantity<LinearPolynomial<Flt64>>> {
    return minusSafe(other)
}

/**
 * 安全执行线性多项式物理量的减法 (Flt64)
 * Safely subtract linear polynomial quantities (Flt64)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量结果 / Difference quantity result
*/
@JvmName("minusQuantityLinearFlt64Safe")
fun Quantity<LinearPolynomial<Flt64>>.minusSafe(
    other: Quantity<LinearPolynomial<Flt64>>
): Ret<Quantity<LinearPolynomial<Flt64>>> {
    return convertSymbolQuantityOperandSafe(other, "subtraction") { unit -> to(unit) }.map { otherConverted ->
        Quantity(
            value = this.value - otherConverted.value,
            unit = this.unit
        )
    }
}

/**
 * 尝试执行线性多项式物理量的减法 (Flt64)
 * Try to subtract linear polynomial quantities (Flt64)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量，失败时返回 null / Difference quantity, or null on failure
*/
@JvmName("minusQuantityLinearFlt64OrNull")
fun Quantity<LinearPolynomial<Flt64>>.minusOrNull(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>>? {
    return minusSafe(other).value
}

/**
 * 线性多项式物理量的加法 (FltX)
 * Addition for linear polynomial quantities (FltX)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量结果 / Sum quantity result
*/
@JvmName("plusQuantityLinearFltX")
operator fun Quantity<LinearPolynomial<FltX>>.plus(
    other: Quantity<LinearPolynomial<FltX>>
): Ret<Quantity<LinearPolynomial<FltX>>> {
    return plusSafe(other)
}

/**
 * 安全执行线性多项式物理量的加法 (FltX)
 * Safely add linear polynomial quantities (FltX)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量结果 / Sum quantity result
*/
@JvmName("plusQuantityLinearFltXSafe")
fun Quantity<LinearPolynomial<FltX>>.plusSafe(
    other: Quantity<LinearPolynomial<FltX>>
): Ret<Quantity<LinearPolynomial<FltX>>> {
    return convertSymbolQuantityOperandSafe(other, "addition") { unit -> to(unit) }.map { otherConverted ->
        Quantity(
            value = this.value + otherConverted.value,
            unit = this.unit
        )
    }
}

/**
 * 尝试执行线性多项式物理量的加法 (FltX)
 * Try to add linear polynomial quantities (FltX)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相加后的物理量，失败时返回 null / Sum quantity, or null on failure
*/
@JvmName("plusQuantityLinearFltXOrNull")
fun Quantity<LinearPolynomial<FltX>>.plusOrNull(
    other: Quantity<LinearPolynomial<FltX>>
): Quantity<LinearPolynomial<FltX>>? {
    return plusSafe(other).value
}

/**
 * 线性多项式物理量的减法 (FltX)
 * Subtraction for linear polynomial quantities (FltX)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量结果 / Difference quantity result
*/
@JvmName("minusQuantityLinearFltX")
operator fun Quantity<LinearPolynomial<FltX>>.minus(
    other: Quantity<LinearPolynomial<FltX>>
): Ret<Quantity<LinearPolynomial<FltX>>> {
    return minusSafe(other)
}

/**
 * 安全执行线性多项式物理量的减法 (FltX)
 * Safely subtract linear polynomial quantities (FltX)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量结果 / Difference quantity result
*/
@JvmName("minusQuantityLinearFltXSafe")
fun Quantity<LinearPolynomial<FltX>>.minusSafe(
    other: Quantity<LinearPolynomial<FltX>>
): Ret<Quantity<LinearPolynomial<FltX>>> {
    return convertSymbolQuantityOperandSafe(other, "subtraction") { unit -> to(unit) }.map { otherConverted ->
        Quantity(
            value = this.value - otherConverted.value,
            unit = this.unit
        )
    }
}

/**
 * 尝试执行线性多项式物理量的减法 (FltX)
 * Try to subtract linear polynomial quantities (FltX)
 *
 * @param other 另一个物理量 / Another quantity
 * @return 相减后的物理量，失败时返回 null / Difference quantity, or null on failure
*/
@JvmName("minusQuantityLinearFltXOrNull")
fun Quantity<LinearPolynomial<FltX>>.minusOrNull(
    other: Quantity<LinearPolynomial<FltX>>
): Quantity<LinearPolynomial<FltX>>? {
    return minusSafe(other).value
}

// ============================================================================
// 符号多项式物理量与标量的乘除
// Scalar multiplication/division for symbol polynomial quantities
// ============================================================================

/**
 * 线性多项式物理量与标量的乘法 (Flt64)
 * Multiplication between linear polynomial quantity and scalar (Flt64)
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = LinearPolynomial(Flt64(100.0), "x") * Meter
 * val doubled = distance * Flt64(2.0)
 * ```
 *
 * @param scalar 标量值 / Scalar value
 * @return 相乘后的物理量 / Product quantity
*/
@JvmName("timesQuantityLinearFlt64Scalar")
operator fun Quantity<LinearPolynomial<Flt64>>.times(scalar: Flt64): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(
        value = this.value * scalar,
        unit = this.unit
    )
}

/**
 * 标量与线性多项式物理量的乘法 (Flt64)
 * Multiplication between scalar and linear polynomial quantity (Flt64)
 *
 * @param quantity 物理量 / Quantity
 * @return 相乘后的物理量 / Product quantity
*/
@JvmName("timesScalarQuantityLinearFlt64")
operator fun Flt64.times(quantity: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return quantity * this
}

/**
 * 线性多项式物理量与标量的除法 (Flt64)
 * Division between linear polynomial quantity and scalar (Flt64)
 *
 * @param scalar 标量值 / Scalar value
 * @return 相除后的物理量 / Quotient quantity
*/
@JvmName("divQuantityLinearFlt64Scalar")
operator fun Quantity<LinearPolynomial<Flt64>>.div(scalar: Flt64): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(
        value = this.value / scalar,
        unit = this.unit
    )
}

/**
 * 线性多项式物理量与标量的乘法 (FltX)
 * Multiplication between linear polynomial quantity and scalar (FltX)
 *
 * @param scalar 标量值 / Scalar value
 * @return 相乘后的物理量 / Product quantity
*/
@JvmName("timesQuantityLinearFltXScalar")
operator fun Quantity<LinearPolynomial<FltX>>.times(scalar: FltX): Quantity<LinearPolynomial<FltX>> {
    return Quantity(
        value = this.value * scalar,
        unit = this.unit
    )
}

/**
 * 标量与线性多项式物理量的乘法 (FltX)
 * Multiplication between scalar and linear polynomial quantity (FltX)
 *
 * @param quantity 物理量 / Quantity
 * @return 相乘后的物理量 / Product quantity
*/
@JvmName("timesScalarQuantityLinearFltX")
operator fun FltX.times(quantity: Quantity<LinearPolynomial<FltX>>): Quantity<LinearPolynomial<FltX>> {
    return quantity * this
}

/**
 * 线性多项式物理量与标量的除法 (FltX)
 * Division between linear polynomial quantity and scalar (FltX)
 *
 * @param scalar 标量值 / Scalar value
 * @return 相除后的物理量 / Quotient quantity
*/
@JvmName("divQuantityLinearFltXScalar")
operator fun Quantity<LinearPolynomial<FltX>>.div(scalar: FltX): Quantity<LinearPolynomial<FltX>> {
    return Quantity(
        value = this.value / scalar,
        unit = this.unit
    )
}

// ============================================================================
// 符号多项式物理量的求值
// Evaluation for symbol polynomial quantities
// ============================================================================

/**
 * 求值线性多项式物理量 (Flt64)
 * Evaluate linear polynomial quantity (Flt64)
 *
 * 将符号变量替换为具体数值，得到一个普通物理量。
 * Substitutes symbol variables with concrete values to get a plain quantity.
 *
 * 示例 / Example:
 * ```kotlin
 * val distance = LinearPolynomial(Flt64(100.0), "x") * Meter
 * val values = mapOf(Symbol("x") to Flt64(5.0))
 * val result = distance.evaluate(values)  // Quantity(Flt64(500.0), Meter)
 * ```
 *
 * @param values 符号到值的映射 / Symbol to value mapping
 * @return 求值后的物理量，如果无法求值返回 null / Evaluated quantity, or null if evaluation failed
*/
@JvmName("evaluateQuantityLinearFlt64")
fun Quantity<LinearPolynomial<Flt64>>.evaluate(values: Map<Symbol, Flt64>): Quantity<Flt64>? {
    val evaluated = this.value.evaluate(MapValueProvider(values)) ?: return null
    return Quantity(evaluated, this.unit)
}
