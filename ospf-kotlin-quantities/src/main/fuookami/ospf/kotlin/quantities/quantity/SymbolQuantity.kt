package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// 符号多项式物理量类型别名
// Symbol polynomial quantity type aliases
// ============================================================================

/**
 * 线性多项式物理量 (Flt64)
 * Linear polynomial quantity (Flt64)
 *
 * 形如 (c₁x₁ + c₂x₂ + ... + b) unit
 */
typealias QuantityLinearFlt64 = Quantity<LinearPolynomial<Flt64>>

/**
 * 二次多项式物理量 (Flt64)
 * Quadratic polynomial quantity (Flt64)
 *
 * 形如 (c₁x₁² + c₂x₁x₂ + ... + b) unit
 */
typealias QuantityQuadraticFlt64 = Quantity<QuadraticPolynomial<Flt64>>

/**
 * 规范多项式物理量 (Flt64)
 * Canonical polynomial quantity (Flt64)
 *
 * 形如 (c₁x₁^p₁ * x₂^p₂ * ... + ...) unit
 */
typealias QuantityCanonicalFlt64 = Quantity<CanonicalPolynomial<Flt64>>

/**
 * 线性多项式物理量 (FltX)
 * Linear polynomial quantity (FltX)
 */
typealias QuantityLinearFltX = Quantity<LinearPolynomial<FltX>>

/**
 * 二次多项式物理量 (FltX)
 * Quadratic polynomial quantity (FltX)
 */
typealias QuantityQuadraticFltX = Quantity<QuadraticPolynomial<FltX>>

/**
 * 规范多项式物理量 (FltX)
 * Canonical polynomial quantity (FltX)
 */
typealias QuantityCanonicalFltX = Quantity<CanonicalPolynomial<FltX>>