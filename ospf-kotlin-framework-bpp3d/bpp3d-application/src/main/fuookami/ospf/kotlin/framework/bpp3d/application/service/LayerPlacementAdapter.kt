@file:Suppress("DEPRECATION")

/**
 * 层放置适配器。
 * Layer placement adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerView
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.placement3Of
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageShapeSpec
import fuookami.ospf.kotlin.math.geometry.Axis3

/**
 * 统一创建 BinLayer 的放置对象。
 * Create BinLayer placement via a unified adapter.
 */
internal fun BinLayer.toLayerPlacement(z: Quantity<InfraNumber>? = null): BinLayerPlacement {
    ensureVerticalCylinderAxis(
        layer = this,
        source = "LayerPlacementAdapter.toLayerPlacement"
    )
    return toLayerPlacementWithoutAxisGuard(z)
}

/**
 * 不执行圆柱轴向门禁的层放置构造，仅用于测试绕过路径。
 * Build layer placement without axis guard, for bypass-path tests only.
 */
internal fun BinLayer.toLayerPlacementWithoutAxisGuard(z: Quantity<InfraNumber>? = null): BinLayerPlacement {
    val position = if (z == null) {
        point3()
    } else {
        point3(z = z)
    }
    return placement3Of(
        view = BinLayerView(copy()),
        position = position
    )
}

/**
 * 统一创建空层箱体，避免业务处散落 QuantityPlacement3 构造。
 * Build a bin with one placed layer via adapter.
 */
internal fun LayerBin.withPlacedLayer(layer: BinLayer, z: Quantity<InfraNumber>? = null): LayerBin {
    return fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin(
        shape = shape,
        units = listOf(layer.toLayerPlacement(z)),
        batchNo = batchNo
    )
}

/**
 * 统一创建 item 放置对象，避免测试夹具散落构造。
 * Create item placement via a unified adapter for fixtures.
 */
internal fun Item.toItemPlacement(
    x: Quantity<InfraNumber>? = null,
    y: Quantity<InfraNumber>? = null,
    z: Quantity<InfraNumber>? = null,
    orientation: Orientation = Orientation.Upright
): ItemPlacement3 {
    val origin = point3()
    val position = point3(
        x = x ?: origin.x,
        y = y ?: origin.y,
        z = z ?: origin.z
    )
    return placement3Of(
        view = view(orientation),
        position = position
    )
}

/**
 * 应用层圆柱门禁：仅允许 Axis3.Y。
 * Application-level cylinder guard: only Axis3.Y is allowed.
 */
internal fun ensureVerticalCylinderAxis(layer: BinLayer, source: String) {
    for (placement in layer.units) {
        val item = placement.unit as? Item ?: continue
        val spec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder ?: continue
        if (spec.axis != Axis3.Y) {
            throw IllegalArgumentException(
                "Unsupported cylinder axis in $source: only Axis3.Y is allowed, but got ${spec.axis}."
            )
        }
    }
}
