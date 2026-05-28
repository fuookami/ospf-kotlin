package fuookami.ospf.kotlin.framework.bpp3d.application.service.compat

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * application service 标量兼容别名。
 * Scalar compatibility alias for application services.
 */
typealias ApplicationScalar = Flt64

fun applicationZero(): ApplicationScalar = Flt64.zero

fun applicationOne(): ApplicationScalar = Flt64.one

fun applicationScalar(value: Double): ApplicationScalar = Flt64(value)
