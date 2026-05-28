package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.compat

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * layer generation 标量兼容别名。
 * Scalar compatibility alias for layer generation.
 */
typealias LayerGenerationScalar = Flt64

fun layerGenerationZero(): LayerGenerationScalar = Flt64.zero

fun layerGenerationNegativeInfinity(): LayerGenerationScalar = Flt64.negativeInfinity

fun layerGenerationScalar(value: Double): LayerGenerationScalar = Flt64(value)
