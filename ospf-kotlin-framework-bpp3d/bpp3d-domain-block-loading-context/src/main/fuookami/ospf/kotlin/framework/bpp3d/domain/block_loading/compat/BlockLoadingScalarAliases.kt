package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.compat

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyScalar
import fuookami.ospf.kotlin.math.algebra.number.UInt64

typealias BlockLoadingScalar = LegacyScalar

fun blockLoadingInfinity(): BlockLoadingScalar {
    return legacyInfinity()
}

fun blockLoadingScalar(value: UInt64): BlockLoadingScalar {
    return legacyScalar(value)
}
