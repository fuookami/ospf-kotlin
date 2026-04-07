package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.minus
import fuookami.ospf.kotlin.math.symbol.polynomial.plus
import fuookami.ospf.kotlin.math.symbol.polynomial.times
import fuookami.ospf.kotlin.math.symbol.polynomial.div
import fuookami.ospf.kotlin.quantities.unit.*

// ============================================================================
// 符号多项式物理量的单位转换
// Unit conversion for symbol polynomial quantities
// ============================================================================

/**
 * 线性多项式物理量的单位转换
 * Unit conversion for linear polynomial quantities
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
 * 二次多项式物理量的单位转换
 * Unit conversion for quadratic polynomial quantities
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
 * 规范多项式物理量的单位转换
 * Unit conversion for canonical polynomial quantities
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

/**
 * 线性多项式物理量的加法
 * Addition for linear polynomial quantities
 */
@JvmName("plusQuantityLinearFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>> {
    if (this.unit.quantity != other.unit.quantity) {
        throw DimensionMismatchException(
            expected = this.unit.quantity.dimensionSymbol(),
            actual = other.unit.quantity.dimensionSymbol(),
            operation = "addition"
        )
    }

    // 转换到相同单位
    val otherConverted = if (this.unit != other.unit) {
        other.to(this.unit) ?: throw UnitConversionException("Cannot convert units")
    } else {
        other
    }

    return Quantity(
        value = this.value + otherConverted.value,
        unit = this.unit
    )
}

@JvmName("minusQuantityLinearFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>> {
    if (this.unit.quantity != other.unit.quantity) {
        throw DimensionMismatchException(
            expected = this.unit.quantity.dimensionSymbol(),
            actual = other.unit.quantity.dimensionSymbol(),
            operation = "subtraction"
        )
    }

    val otherConverted = if (this.unit != other.unit) {
        other.to(this.unit) ?: throw UnitConversionException("Cannot convert units")
    } else {
        other
    }

    return Quantity(
        value = this.value - otherConverted.value,
        unit = this.unit
    )
}

@JvmName("plusQuantityLinearFltX")
operator fun Quantity<LinearPolynomial<FltX>>.plus(
    other: Quantity<LinearPolynomial<FltX>>
): Quantity<LinearPolynomial<FltX>> {
    if (this.unit.quantity != other.unit.quantity) {
        throw DimensionMismatchException(
            expected = this.unit.quantity.dimensionSymbol(),
            actual = other.unit.quantity.dimensionSymbol(),
            operation = "addition"
        )
    }

    val otherConverted = if (this.unit != other.unit) {
        other.to(this.unit) ?: throw UnitConversionException("Cannot convert units")
    } else {
        other
    }

    return Quantity(
        value = this.value + otherConverted.value,
        unit = this.unit
    )
}

@JvmName("minusQuantityLinearFltX")
operator fun Quantity<LinearPolynomial<FltX>>.minus(
    other: Quantity<LinearPolynomial<FltX>>
): Quantity<LinearPolynomial<FltX>> {
    if (this.unit.quantity != other.unit.quantity) {
        throw DimensionMismatchException(
            expected = this.unit.quantity.dimensionSymbol(),
            actual = other.unit.quantity.dimensionSymbol(),
            operation = "subtraction"
        )
    }

    val otherConverted = if (this.unit != other.unit) {
        other.to(this.unit) ?: throw UnitConversionException("Cannot convert units")
    } else {
        other
    }

    return Quantity(
        value = this.value - otherConverted.value,
        unit = this.unit
    )
}