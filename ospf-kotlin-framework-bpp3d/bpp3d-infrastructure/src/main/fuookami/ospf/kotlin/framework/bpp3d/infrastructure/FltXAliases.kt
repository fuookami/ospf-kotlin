@file:Suppress("DEPRECATION")
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64

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
