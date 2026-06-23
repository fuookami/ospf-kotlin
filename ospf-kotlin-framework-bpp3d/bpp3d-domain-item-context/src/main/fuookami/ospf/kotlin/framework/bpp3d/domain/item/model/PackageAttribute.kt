/**
 * 包装属性模型。
 * Package attribute model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Vector
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter

/**
 * 获取物品视图的支撑包装形状，圆柱件不满足直立垂直支撑条件时返回 null。
 * Get the support packing shape of the item view; returns null when a cylinder item does not satisfy upright-vertical support conditions.
 *
 * @return 支撑包装形状或 null / the support packing shape or null
 */
private fun ItemView.supportPackingShapeOrNull(): PackingShape3<FltX>? {
    val itemShape = placementPackingShape ?: unit.packingShape
    if (itemShape is CylinderPackingShape3) {
        if (requireUprightVerticalCylinderSupport(
            shape = itemShape,
            orientation = orientation,
            path = CylinderCapabilityPath.PackageAttributeSupport
        ).failed) {
            return null
        }
        return itemShape
    }
    return asPackingShape3()
}

/**
 * 计算物品视图底部投影面积，若无法获取支撑包装形状则返回 null。
 * Calculate the bottom footprint area of the item view; returns null if the support packing shape is unavailable.
 *
 * @return 底部投影面积或 null / the bottom footprint area or null
 */
private fun ItemView.bottomFootprintAreaOrNull(): Quantity<FltX>? {
    val packingShape = supportPackingShapeOrNull() ?: return null
    val shapePlacement = ShapePlacement3(
        shape = packingShape,
        position = point3FltX()
    )
    return shapePlacement.footprintOverlapArea(shapePlacement)
}

/**
 * 获取物品视图底部投影的最小跨度（圆柱取直径、矩形取宽深较小值），无法获取形状时返回 null。
 * Get the minimum span of the item view's bottom footprint (diameter for circles, lesser of width/depth for rectangles); returns null if the shape is unavailable.
 *
 * @return 底部投影最小跨度或 null / the minimum bottom footprint span or null
 */
private fun ItemView.bottomFootprintMinSpanOrNull(): Quantity<FltX>? {
    return when (val footprint = supportPackingShapeOrNull()?.footprint() ?: return null) {
        is ShapeFootprint2.Circle -> footprint.radius + footprint.radius
        is ShapeFootprint2.Rectangle -> if (footprint.width leq footprint.depth) footprint.width else footprint.depth
    }
}

interface AbstractWeightAttribute {
    val maxLayer: UInt64
}

data class WeightAttribute(
    override val maxLayer: UInt64 = UInt64.maximum
) : AbstractWeightAttribute {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeightAttribute

        return maxLayer == other.maxLayer
    }

    override fun hashCode(): Int {
        return maxLayer.hashCode()
    }
}

interface AbstractDeformationAttribute {
    fun deformationQuantity(volume: FltX): Vector<Dim3, FltX>
    fun deformationQuantity(item: Item) = deformationQuantity(item.volume.value)
    fun deformationQuantity(item: ItemView) = deformationQuantity(item.volume.value)
}

data class LinearDeformationAttribute(
    val deformationCoefficient: FltX
) : AbstractDeformationAttribute {
    override fun deformationQuantity(volume: FltX): Vector<Dim3, FltX> = Vector(
        volume * deformationCoefficient,
        volume * deformationCoefficient,
        volume * deformationCoefficient
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LinearDeformationAttribute

        return deformationCoefficient == other.deformationCoefficient
    }

    override fun hashCode(): Int {
        return deformationCoefficient.hashCode()
    }
}

interface AbstractHangingPolicy {
    /** 启用堆叠标志 / Enabled stacking flag */
    fun enabledStackingOn(
        item: ItemView,
        bottomSupport: BottomSupport
    ): Boolean
}

data class AbsoluteHangingPolicy(
    private val maxDifference: FltX,
    private val withWeight: Boolean = true
) : AbstractHangingPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomSupport: BottomSupport
    ): Boolean {
        if (withWeight && bottomSupport.weight ls item.weight) {
            return false
        }

        val footprintArea = item.bottomFootprintAreaOrNull() ?: return false
        val footprintMinSpan = item.bottomFootprintMinSpanOrNull() ?: return false
        val maxDifferenceSpan = maxDifference * (FltX.one * footprintMinSpan.unit)
        val maxHangingArea = maxDifferenceSpan * footprintMinSpan
        val hangingArea = footprintArea - bottomSupport.area
        return hangingArea leq maxHangingArea
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbsoluteHangingPolicy

        if (maxDifference != other.maxDifference) return false
        if (withWeight != other.withWeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxDifference.hashCode()
        result = 31 * result + withWeight.hashCode()
        return result
    }
}

data class RelativeHangingPolicy(
    private val hangingPercentage: FltX,
    private val withWeight: Boolean = true
) : AbstractHangingPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomSupport: BottomSupport
    ): Boolean {
        if (withWeight && bottomSupport.weight ls item.weight) {
            return false
        }

        val footprintArea = item.bottomFootprintAreaOrNull() ?: return false
        val maxHangingArea = footprintArea * hangingPercentage
        val hangingArea = footprintArea - bottomSupport.area
        return hangingArea leq maxHangingArea
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RelativeHangingPolicy

        if (hangingPercentage != other.hangingPercentage) return false
        if (withWeight != other.withWeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hangingPercentage.hashCode()
        result = 31 * result + withWeight.hashCode()
        return result
    }
}

interface AbstractStackingOnPolicy {
    fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64 = UInt64.zero,
        height: Quantity<FltX> = FltX.zero * Meter
    ): Boolean
}

data class BoxStackingOnPolicy(
    val maxDifference: FltX,
    private val maxOverWeight: FltX = FltX(10.0),
    private val extraStackingOnRule: ((ItemView, ItemView) -> Boolean)? = null
) : AbstractStackingOnPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64,
        height: Quantity<FltX>
    ): Boolean {
        if (!bottomItem.overPackageTypes.contains(item.packageType)) {
            return false
        }
        if (extraStackingOnRule?.invoke(item, bottomItem) == false) {
            return false
        }
        if (bottomItem.packageCategory == PackageCategory.Pallet && item.packageCategory != PackageCategory.Filler) {
            if (!bottomItem.topFlat) {
                return false
            }
        }
        if (bottomItem.packageCategory != PackageCategory.Filler && item.packageCategory != PackageCategory.Filler) {
            val difference = (item.width - bottomItem.width).abs() + (item.depth - bottomItem.depth).abs()
            if (difference gr (maxDifference * difference.unit)) {
                return false
            }
        }
        if ((item.weight - bottomItem.weight) gr (maxOverWeight * item.weight.unit)) {
            return false
        }
        return layer < item.maxLayer && (height + item.height) leq item.maxHeight
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BoxStackingOnPolicy

        if (maxDifference != other.maxDifference) return false
        if (extraStackingOnRule != other.extraStackingOnRule) return false

        return true
    }

    override fun hashCode(): Int {
        var result = maxDifference.hashCode()
        result = 31 * result + (extraStackingOnRule?.hashCode() ?: 0)
        return result
    }
}

data class CartonContainerStackingOnPolicy(
    val maxDifference: FltX,
    private val maxOverWeight: FltX = FltX(10.0),
    private val extraStackingOnRule: ((ItemView, ItemView) -> Boolean)? = null
) : AbstractStackingOnPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64,
        height: Quantity<FltX>
    ): Boolean {
        if (!bottomItem.overPackageTypes.contains(item.packageType)) {
            return false
        }
        if (extraStackingOnRule?.invoke(item, bottomItem) == false) {
            return false
        }
        if (bottomItem.packageCategory == PackageCategory.SoftBox && item.packageCategory == PackageCategory.SoftBox) {
            val difference = item.unit.width + item.unit.depth - bottomItem.unit.width - bottomItem.unit.depth
            if (difference gr (maxDifference * difference.unit)) {
                return false
            }
        }
        if ((item.weight - bottomItem.weight) gr (maxOverWeight * item.weight.unit)) {
            return false
        }
        return layer < item.maxLayer && (height + item.height) leq item.maxHeight
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CartonContainerStackingOnPolicy

        return maxDifference == other.maxDifference
    }

    override fun hashCode(): Int {
        return maxDifference.hashCode()
    }
}

data class FilterStackingOnPolicy(
    private val maxOverWeight: FltX = FltX(10.0),
    private val extraStackingOnRule: ((ItemView, ItemView) -> Boolean)? = null
) : AbstractStackingOnPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64,
        height: Quantity<FltX>
    ): Boolean {
        if (!bottomItem.overPackageTypes.contains(item.packageType)) {
            return false
        }
        if (extraStackingOnRule?.invoke(item, bottomItem) == false) {
            return false
        }
        if ((item.weight - bottomItem.weight) gr (maxOverWeight * item.weight.unit)) {
            return false
        }
        return layer < item.maxLayer && (height + item.height) leq item.maxHeight
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilterStackingOnPolicy

        return maxOverWeight == other.maxOverWeight
    }

    override fun hashCode(): Int {
        return maxOverWeight.hashCode()
    }
}

/** 货物属性接口 / Cargo attribute interface */
interface AbstractCargoAttribute

/**
 * 包装属性，定义货物的堆叠、变形、悬挂等约束规则。
 * Package attribute, defining stacking, deformation, hanging and other constraint rules for items.
 *
 * @property packageType 包装类型 / package type
 * @property packageMaxLayer 最大堆叠层数 / maximum stacking layer count
 * @property maxHeight 最大高度 / maximum height
 * @property minDepth 最小深度 / minimum depth
 * @property maxDepth 最大深度 / maximum depth
 * @property overPackageTypes 可堆叠在上方的包装类型列表 / list of package types that can be stacked on top
 * @property bottomOnly 是否仅允许底层放置 / whether only bottom placement is allowed
 * @property topFlat 顶部是否平整 / whether the top is flat
 * @property sideOnTopLayer 侧放允许的顶层层数 / allowed top layer count for side placement
 * @property lieOnTopLayer 平放允许的顶层层数 / allowed top layer count for lie placement
 * @property cargoAttribute 货物属性 / cargo attribute
 * @property weightAttribute 重量属性 / weight attribute
 * @property deformationAttribute 变形属性 / deformation attribute
 * @property hangingPolicy 悬挂策略 / hanging policy
 * @property stackingOnPolicy 堆叠策略 / stacking on policy
 * @property extraOrientationRule 额外朝向规则 / extra orientation rule
 * @property extraStackingOnRule 额外堆叠规则 / extra stacking on rule
 */
data class PackageAttribute(
    val packageType: PackageType,
    val packageMaxLayer: UInt64 = UInt64.maximum,
    val maxHeight: Quantity<FltX> = FltX.maximum * Meter,
    val minDepth: Quantity<FltX> = FltX.zero * Meter,
    val maxDepth: Quantity<FltX> = FltX.maximum * Meter,
    val overPackageTypes: List<PackageType> = PackageType.entries.toList(),
    val bottomOnly: Boolean = false,
    val topFlat: Boolean = true,

    val sideOnTopLayer: UInt64 = UInt64.zero,
    val lieOnTopLayer: UInt64 = UInt64.zero,

    val cargoAttribute: AbstractCargoAttribute? = null,
    val weightAttribute: AbstractWeightAttribute,
    val deformationAttribute: AbstractDeformationAttribute,
    val hangingPolicy: AbstractHangingPolicy,
    val stackingOnPolicy: AbstractStackingOnPolicy,

    val extraOrientationRule: ((AbstractContainer3Shape, Orientation) -> Boolean)? = null,
    val extraStackingOnRule: ((QuantityPlacement3<Item, FltX>, List<QuantityPlacement3<Item, FltX>>, List<QuantityPlacement3<Item, FltX>>) -> Boolean)? = null
) {
    val packageCategory by packageType::category

    val enabledSideOnTop: Boolean get() = sideOnTopLayer != UInt64.zero
    val enabledLieOnTop: Boolean get() = lieOnTopLayer != UInt64.zero

    val maxLayer = min(packageMaxLayer, weightAttribute.maxLayer)

    companion object {
        private suspend fun layerLayer(
            item: QuantityPlacement3<Item, FltX>,
            bottomItems: List<QuantityPlacement3<Item, FltX>>,
        ): UInt64 {
            return coroutineScope {
                val directBottomItems = topItemPlacements(bottomItems)
                val indirectBottomItems = bottomItems.filter { !directBottomItems.contains(it) }

                val promises = ArrayList<Deferred<UInt64>>()
                for (bottomItem in directBottomItems) {
                    if (bottomItem.type == item.type) {
                        promises.add(async(Dispatchers.Default) {
                            val thisBottomPlacements = indirectBottomItems.filterBottomOverlapped(bottomItem).filterIsInstance<QuantityPlacement3<Item, FltX>>()
                            if (thisBottomPlacements.isNotEmpty()) {
                                UInt64.one + layerLayer(bottomItem, thisBottomPlacements)
                            } else {
                                UInt64.one
                            }
                        })
                    }
                }
                var maxLayer = UInt64.zero
                for (promise in promises) {
                    val value = promise.await()
                    if (value gr maxLayer) {
                        maxLayer = value
                    }
                }
                maxLayer
            }
        }
        private suspend fun layerHeight(
            item: QuantityPlacement3<Item, FltX>,
            bottomItems: List<QuantityPlacement3<Item, FltX>>,
        ): Quantity<FltX> {
            return coroutineScope {
                val directBottomItems = topItemPlacements(bottomItems)
                val indirectBottomItems = bottomItems.filter { !directBottomItems.contains(it) }

                val promises = ArrayList<Deferred<Quantity<FltX>>>()
                for (bottomItem in directBottomItems) {
                    if (bottomItem.type == item.type) {
                        promises.add(async(Dispatchers.Default) {
                            val thisBottomPlacements = indirectBottomItems.filterBottomOverlapped(bottomItem).filterIsInstance<QuantityPlacement3<Item, FltX>>()
                            if (thisBottomPlacements.isNotEmpty()) {
                                bottomItem.height + layerHeight(bottomItem, thisBottomPlacements)
                            } else {
                                bottomItem.height
                            }
                        })
                    }
                }
                var maxHeight = FltX.zero * item.height.unit
                for (promise in promises) {
                    val value = promise.await()
                    if (value gr maxHeight) {
                        maxHeight = value
                    }
                }
                maxHeight
            }
        }

        suspend fun layer(
            item: QuantityPlacement3<Item, FltX>,
            bottomItems: List<QuantityPlacement3<Item, FltX>>,
        ): Pair<UInt64, Quantity<FltX>> {
            return coroutineScope {
                val layer = async(Dispatchers.Default) {
                    layerLayer(item, bottomItems)
                }
                val height = async(Dispatchers.Default) {
                    layerHeight(item, bottomItems)
                }
                Pair(layer.await(), height.await())
            }
        }
    }

    fun enabledStackingOn(
        item: ItemView,
        bottomSupport: BottomSupport
    ): Boolean {
        return hangingPolicy.enabledStackingOn(
            item = item,
            bottomSupport = bottomSupport
        )
    }

    fun enabledStackingOn(
        item: Item,
        bottomItem: Item?,
        layer: UInt64 = UInt64.zero,
        height: Quantity<FltX> = FltX.zero * Meter,
        space: AbstractContainer3Shape = Container3Shape()
    ): Boolean {
        return enabledStackingOn(
            item = item.view(),
            bottomItem = bottomItem?.view(),
            layer = layer,
            height = height,
            space = space
        )
    }

    fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView? = null,
        layer: UInt64 = UInt64.zero,
        height: Quantity<FltX> = FltX.zero * Meter,
        space: AbstractContainer3Shape = Container3Shape()
    ): Boolean {
        if (bottomItem != null) {
            if (item.bottomOnly && !bottomItem.bottomOnly) {
                return false
            }

            if ((!bottomItem.topFlat || !bottomItem.unit.enabledOrientations.contains(bottomItem.orientation)) && item.packageCategory != PackageCategory.Filler) {
                return false
            }
        }

        if (!item.unit.enabledOrientationsAt(space).contains(item.orientation)) {
            return false
        }

        if (!item.unit.enabledOrientations.contains(item.orientation)) {
            if (item.orientation.category == OrientationCategory.Side && layer >= item.unit.sideOnTopLayer) {
                return false
            }
            if (item.orientation.category == OrientationCategory.Lie && layer >= item.unit.lieOnTopLayer) {
                return false
            }
        }

        return bottomItem == null || stackingOnPolicy.enabledStackingOn(
            item = item,
            bottomItem = bottomItem,
            layer = layer,
            height = height
        )
    }
    suspend fun enabledStackingOn(
        item: QuantityPlacement3<Item, FltX>,
        bottomItems: List<QuantityPlacement3<Item, FltX>>,
        space: AbstractContainer3Shape = Container3Shape()
    ): Boolean {
        val directBottomItems = topItemPlacements(bottomItems)
        val indirectBottomItems = bottomItems.filter { !directBottomItems.contains(it) }

        if (extraStackingOnRule?.invoke(item, directBottomItems, indirectBottomItems) == false) {
            return false
        }

        // If the material is not at the bottom (y coordinate is not 0) and it is required to be at the bottom, there cannot be any non-bottom-required material below it.
        // 如果货物不在底部（y坐标不为0）且要求放在底部，则其下方不能有任何非底部要求的货物。
        if (item.bottomOnly && bottomItems.any { !it.bottomOnly }) {
            return false
        }

        if (!item.unit.enabledOrientationsAt(space).contains(item.orientation)) {
            return false
        }

        val (layer, height) = layer(item, bottomItems)

        for (bottomItem in directBottomItems) {
            if (!bottomItem.topFlat) {
                if (!bottomItem.unit.enabledOrientations.contains(bottomItem.orientation) && !item.unit.enabledOrientations.contains(item.orientation)) {
                    if (item.orientation.category == OrientationCategory.Side && layer >= item.unit.sideOnTopLayer) {
                        return false
                    }
                    if (item.orientation.category == OrientationCategory.Lie && layer >= item.unit.lieOnTopLayer) {
                        return false
                    }
                } else if (item.packageCategory != PackageCategory.Filler) {
                    return false
                }
            }
        }

        for (bottomItem in directBottomItems) {
            if (!stackingOnPolicy.enabledStackingOn(
                    item = item.view as ItemView,
                    bottomItem = bottomItem.view as ItemView,
                    layer = layer,
                    height = height
                )
            ) {
                return false
            }
        }

        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackageAttribute

        if (packageType != other.packageType) return false
        if (packageMaxLayer != other.packageMaxLayer) return false
        if (maxHeight != other.maxHeight) return false
        if (minDepth != other.minDepth) return false
        if (maxDepth != other.maxDepth) return false
        if (overPackageTypes != other.overPackageTypes) return false
        if (bottomOnly != other.bottomOnly) return false
        if (topFlat != other.topFlat) return false
        if (sideOnTopLayer != other.sideOnTopLayer) return false
        if (lieOnTopLayer != other.lieOnTopLayer) return false
        if (cargoAttribute != other.cargoAttribute) return false
        if (weightAttribute != other.weightAttribute) return false
        if (deformationAttribute != other.deformationAttribute) return false
        if (hangingPolicy != other.hangingPolicy) return false
        if (stackingOnPolicy != other.stackingOnPolicy) return false
        if (extraOrientationRule != other.extraOrientationRule) return false
        if (extraStackingOnRule != other.extraStackingOnRule) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageType.hashCode()
        result = 31 * result + packageMaxLayer.hashCode()
        result = 31 * result + maxHeight.hashCode()
        result = 31 * result + minDepth.hashCode()
        result = 31 * result + maxDepth.hashCode()
        result = 31 * result + overPackageTypes.hashCode()
        result = 31 * result + bottomOnly.hashCode()
        result = 31 * result + topFlat.hashCode()
        result = 31 * result + sideOnTopLayer.hashCode()
        result = 31 * result + lieOnTopLayer.hashCode()
        result = 31 * result + (cargoAttribute?.hashCode() ?: 0)
        result = 31 * result + weightAttribute.hashCode()
        result = 31 * result + deformationAttribute.hashCode()
        result = 31 * result + hangingPolicy.hashCode()
        result = 31 * result + stackingOnPolicy.hashCode()
        result = 31 * result + (extraOrientationRule?.hashCode() ?: 0)
        result = 31 * result + (extraStackingOnRule?.hashCode() ?: 0)
        return result
    }
}
