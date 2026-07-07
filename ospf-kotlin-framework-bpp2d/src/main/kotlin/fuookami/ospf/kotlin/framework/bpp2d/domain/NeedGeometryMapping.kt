package fuookami.ospf.kotlin.framework.bpp2d.domain

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.geometry.QuantityBox2
import fuookami.ospf.kotlin.math.geometry.QuantityPlacement2
import fuookami.ospf.kotlin.math.geometry.QuantityProjection2
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2

/**
 * bpp2d 业务模型到 quantity-geometry 的稳定适配层；不依赖 BPP3D 包。
 * Stable adapter from bpp2d domain models to quantity-geometry; no dependency on BPP3D packages.
 */
/** 将投影需求转换为几何投影 / Convert projection need to geometry projection
 * @return 几何投影 / Geometry projection
 */
fun <V : FloatingNumber<V>> Projection2Need<V>.toGeometryProjection2(): QuantityProjection2<V> {
    return QuantityRectangle2(
        width = width,
        height = height
    )
}

/** 将放置需求转换为几何放置 / Convert placement need to geometry placement
 * @return 几何放置 / Geometry placement
 */
fun <V : FloatingNumber<V>> Placement2Need<V>.toGeometryPlacement2(): QuantityPlacement2<V> {
    return QuantityPlacement2(
        x = x,
        y = y,
        shape = projection.toGeometryProjection2()
    )
}

/** 将盒体需求转换为几何盒体 / Convert box need to geometry box
 * @return 几何盒体 / Geometry box
 */
fun <V : FloatingNumber<V>> Box2Need<V>.toGeometryBox2(): QuantityBox2<V> {
    return QuantityBox2(
        x = minX,
        y = minY,
        shape = QuantityRectangle2(
            width = width,
            height = height
        )
    )
}
