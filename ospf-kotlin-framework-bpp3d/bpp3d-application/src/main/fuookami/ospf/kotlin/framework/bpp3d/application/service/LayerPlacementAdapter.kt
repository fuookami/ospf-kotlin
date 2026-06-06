@file:Suppress("DEPRECATION")

/**
 * 层放置适配器。
 * Layer placement adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.point3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayerView
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.CylinderCapabilityPath
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LayerBin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.binLayerPlacementOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.itemPlacement3Of
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.requireVerticalCylinderAxis

/**
 * 统一创建 BinLayer 的放置对象。
 * Create BinLayer placement via a unified adapter.
 */
internal fun BinLayer.toLayerPlacement(z: Quantity<InfraNumber>? = null): BinLayerPlacement {
    ensureVerticalCylinderAxis(
        layer = this
    )
    return toKnownCoordinateLayerPlacement(z)
}

/**
 * 已知坐标层放置构造，不执行默认候选路径的圆柱轴向门禁。
 * Known-coordinate layer placement builder, without the default candidate-path cylinder-axis guard.
 *
 * @param z 层深度坐标 / layer depth coordinate
 * @return 层放置 / layer placement
 */
internal fun BinLayer.toKnownCoordinateLayerPlacement(z: Quantity<InfraNumber>? = null): BinLayerPlacement {
    val position = if (z == null) {
        point3()
    } else {
        point3(z = z)
    }
    return binLayerPlacementOf(
        view = BinLayerView(copy()),
        position = position
    )
}

/**
 * 统一创建空层箱体，避免业务处散落 QuantityPlacement3 构造。
 * Build a bin with one placed layer via adapter.
 */
internal fun LayerBin.withPlacedLayer(layer: BinLayer, z: Quantity<InfraNumber>? = null): LayerBin {
    return layerBinOf(
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
    return itemPlacement3Of(
        item = this,
        position = position,
        orientation = orientation
    )
}

/**
 * 应用层使用共享圆柱契约校验默认候选能力。
 * Application layer uses the shared cylinder contract to validate default candidate capability.
 */
internal fun ensureVerticalCylinderAxis(layer: BinLayer) {
    for (placement in layer.units) {
        val item = placement.unit as? Item ?: continue
        requireVerticalCylinderAxis(
            shape = item.packingShape,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        )
    }
}
