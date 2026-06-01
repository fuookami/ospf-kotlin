@file:Suppress("DEPRECATION")

/**
 * 放置构造与底面重叠辅助。
 * Placement factory and bottom-overlap helpers.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Bottom
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CuboidView
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ProjectivePlane
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Projection
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint3

fun <T : Cuboid<T>, P : ProjectivePlane> placement2Of(
    projection: Projection<T, P>,
    position: QuantityPoint2
): QuantityPlacement2<T, P> {
    return QuantityPlacement2(
        projection = projection,
        position = position
    )
}

fun <T : Cuboid<T>> placement3Of(
    view: CuboidView<T>,
    position: QuantityPoint3
): QuantityPlacement3<T> {
    return QuantityPlacement3(
        view = view,
        position = position
    )
}

fun <T : Cuboid<T>> QuantityPlacement3<T>.bottomPlacement(): QuantityPlacement2<T, Bottom> {
    return QuantityPlacement2(this, Bottom)
}

fun AnyPlacement3.overlappedOnBottom(other: AnyPlacement3): Boolean {
    return bottomPlacement().overlapped(other.bottomPlacement())
}

fun Iterable<AnyPlacement3>.filterBottomOverlapped(
    target: AnyPlacement3
): List<AnyPlacement3> {
    val targetBottomPlacement = target.bottomPlacement()
    return filter { placement ->
        placement.bottomPlacement().overlapped(targetBottomPlacement)
    }
}
