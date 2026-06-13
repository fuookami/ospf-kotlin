@file:Suppress("DEPRECATION")
/**
 * FltX 别名与工具函数。
 * FltX aliases and utility functions.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * BPP3D FltX 数值辅助函数。
 * BPP3D FltX numeric helper functions.
 */

fun fltXZero(): FltX = FltX.zero
fun fltXOne(): FltX = FltX.one
fun fltXInfinity(): FltX = FltX.maximum
fun fltXNegativeInfinity(): FltX = FltX.minimum
fun fltXEpsilon(): FltX = FltX.epsilon

fun fltX(value: UInt64): FltX = FltX(value.toULong().toDouble())
fun fltX(value: Double): FltX = FltX(value)
