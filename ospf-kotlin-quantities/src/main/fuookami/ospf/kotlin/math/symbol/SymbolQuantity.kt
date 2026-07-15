/**
 * 符号多项式物理量类型别名
 * Symbol Polynomial Quantity Type Aliases
 *
 * 为符号多项式物理量提供便捷的类型别名，简化代码编写。
 * Provides convenient type aliases for symbol polynomial quantities, simplifying code writing.
 *
 * 支持的多项式类型 / Supported polynomial types:
 * - LinearPolynomial: 线性多项式 (c₁x₁ + c₂x₂ + ... + b) / Linear polynomial
 * - QuadraticPolynomial: 二次多项式 (c₁x₁² + c₂x₁x₂ + ... + b) / Quadratic polynomial
 * - CanonicalPolynomial: 规范多项式 (c₁x₁^p₁ * x₂^p₂ * ... + ...) / Canonical polynomial
 *
 * 支持的数值类型 / Supported number types:
 * - Flt64: 64位浮点数 / 64-bit floating-point
 * - FltX: 高精度浮点数 / High-precision floating-point
*/
package fuookami.ospf.kotlin.math.symbol

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

// ============================================================================
// 符号多项式物理量类型别名
// Symbol polynomial quantity type aliases
// ============================================================================

/**
 * 线性多项式物理量 (Flt64)
 * Linear polynomial quantity (Flt64)
 *
 * 形如 (c₁x₁ + c₂x₂ + ... + b) unit
 * Form: (c₁x₁ + c₂x₂ + ... + b) unit
 *
 * 用于表示含有符号变量的线性表达式物理量。
 * Used to represent linear expression quantities with symbol variables.
*/
typealias QuantityLinearFlt64 = Quantity<LinearPolynomial<Flt64>>

/**
 * 二次多项式物理量 (Flt64)
 * Quadratic polynomial quantity (Flt64)
 *
 * 形如 (c₁x₁² + c₂x₁x₂ + ... + b) unit
 * Form: (c₁x₁² + c₂x₁x₂ + ... + b) unit
 *
 * 用于表示含有符号变量的二次表达式物理量。
 * Used to represent quadratic expression quantities with symbol variables.
*/
typealias QuantityQuadraticFlt64 = Quantity<QuadraticPolynomial<Flt64>>

/**
 * 规范多项式物理量 (Flt64)
 * Canonical polynomial quantity (Flt64)
 *
 * 形如 (c₁x₁^p₁ * x₂^p₂ * ... + ...) unit
 * Form: (c₁x₁^p₁ * x₂^p₂ * ... + ...) unit
 *
 * 用于表示含有符号变量的规范多项式物理量。
 * Used to represent canonical polynomial quantities with symbol variables.
*/
typealias QuantityCanonicalFlt64 = Quantity<CanonicalPolynomial<Flt64>>

/**
 * 线性多项式物理量 (FltX)
 * Linear polynomial quantity (FltX)
 *
 * 高精度版本的线性多项式物理量。
 * High-precision version of linear polynomial quantity.
*/
typealias QuantityLinearFltX = Quantity<LinearPolynomial<FltX>>

/**
 * 二次多项式物理量 (FltX)
 * Quadratic polynomial quantity (FltX)
 *
 * 高精度版本的二次多项式物理量。
 * High-precision version of quadratic polynomial quantity.
*/
typealias QuantityQuadraticFltX = Quantity<QuadraticPolynomial<FltX>>

/**
 * 规范多项式物理量 (FltX)
 * Canonical polynomial quantity (FltX)
 *
 * 高精度版本的规范多项式物理量。
 * High-precision version of canonical polynomial quantity.
*/
typealias QuantityCanonicalFltX = Quantity<CanonicalPolynomial<FltX>>
