package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

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
    fun deformationQuantity(volume: Flt64): Vector3
    fun deformationQuantity(unit: AbstractCuboid) = deformationQuantity(unit.volume)
}

data class LinearDeformationAttribute(
    val deformationCoefficient: Flt64
) : AbstractDeformationAttribute {
    override fun deformationQuantity(volume: Flt64) = Vector(volume * deformationCoefficient, volume * deformationCoefficient, volume * deformationCoefficient)

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
    fun enabledStackingOn(
        unit: AbstractCuboid,
        bottomSupport: BottomSupport
    ): Boolean
}

data class AbsoluteHangingPolicy(
    private val maxDifference: Flt64,
    private val withWeight: Boolean = true
) : AbstractHangingPolicy {
    override fun enabledStackingOn(
        unit: AbstractCuboid,
        bottomSupport: BottomSupport
    ): Boolean {
        if (withWeight && bottomSupport.weight ls unit.weight) {
            return false
        }

        val length = Bottom.length(unit)
        val width = Bottom.width(unit)
        val maxHangingArea = maxDifference * min(length, width)
        val hangingArea = length * width - bottomSupport.area
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
    private val hangingPercentage: Flt64,
    private val withWeight: Boolean = true
) : AbstractHangingPolicy {
    override fun enabledStackingOn(
        unit: AbstractCuboid,
        bottomSupport: BottomSupport
    ): Boolean {
        if (withWeight && bottomSupport.weight ls unit.weight) {
            return false
        }

        val length = Bottom.length(unit)
        val width = Bottom.width(unit)
        val s = length * width
        val maxHangingArea = s * hangingPercentage
        val hangingArea = s - bottomSupport.area
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
        height: Flt64 = Flt64.zero
    ): Boolean
}

data class BoxStackingOnPolicy(
    val maxDifference: Flt64,
    private val maxOverWeight: Flt64 = Flt64(10.0),
    private val extraStackingOnRule: ((ItemView, ItemView) -> Boolean)? = null
) : AbstractStackingOnPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64,
        height: Flt64
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
            val difference = abs(item.width - bottomItem.width) + abs(item.depth - bottomItem.depth)
            if (difference gr maxDifference) {
                return false
            }
        }
        if ((item.weight - bottomItem.weight) gr maxOverWeight) {
            return false
        }
        return layer < item.maxLayer && (height + item.height) leq item.maxHeight
    }

    @Suppress("UNCHECKED_CAST")
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
    val maxDifference: Flt64,
    private val maxOverWeight: Flt64 = Flt64(10.0),
    private val extraStackingOnRule: ((ItemView, ItemView) -> Boolean)? = null
) : AbstractStackingOnPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64,
        height: Flt64
    ): Boolean {
        if (!bottomItem.overPackageTypes.contains(item.packageType)) {
            return false
        }
        if (extraStackingOnRule?.invoke(item, bottomItem) == false) {
            return false
        }
        if (bottomItem.packageCategory == PackageCategory.SoftBox && item.packageCategory == PackageCategory.SoftBox) {
            val difference = item.unit.width + item.unit.depth - bottomItem.unit.width - bottomItem.unit.depth
            if (difference gr maxDifference) {
                return false
            }
        }
        if ((item.weight - bottomItem.weight) gr maxOverWeight) {
            return false
        }
        return layer < item.maxLayer && (height + item.height) leq item.maxHeight
    }

    @Suppress("UNCHECKED_CAST")
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
    private val maxOverWeight: Flt64 = Flt64(10.0),
    private val extraStackingOnRule: ((ItemView, ItemView) -> Boolean)? = null
) : AbstractStackingOnPolicy {
    override fun enabledStackingOn(
        item: ItemView,
        bottomItem: ItemView,
        layer: UInt64,
        height: Flt64
    ): Boolean {
        if (!bottomItem.overPackageTypes.contains(item.packageType)) {
            return false
        }
        if (extraStackingOnRule?.invoke(item, bottomItem) == false) {
            return false
        }
        if ((item.weight - bottomItem.weight) gr Flt64(10.0)) {
            return false
        }
        return layer < item.maxLayer && (height + item.height) leq item.maxHeight
    }

    @Suppress("UNCHECKED_CAST")
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

interface AbstractCargoAttribute

data class PackageAttribute(
    val packageType: PackageType,
    val packageMaxLayer: UInt64 = UInt64.maximum,
    val maxHeight: Flt64 = Flt64.infinity,
    val minDepth: Flt64 = Flt64.zero,
    val maxDepth: Flt64 = Flt64.infinity,
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
    val extraStackingOnRule: ((ItemPlacement3, List<ItemPlacement3>, List<ItemPlacement3>) -> Boolean)? = null
) {
    val packageCategory by packageType::category

    val enabledSideOnTop: Boolean get() = sideOnTopLayer != UInt64.zero
    val enabledLieOnTop: Boolean get() = lieOnTopLayer != UInt64.zero

    val maxLayer = min(packageMaxLayer, weightAttribute.maxLayer)

    companion object {
        @Suppress("UNCHECKED_CAST")
        private suspend fun  layerLayer(
            item: ItemPlacement3,
            bottomItems: List<ItemPlacement3>,
        ): UInt64 {
            return coroutineScope {
                val directBottomItems = topPlacements(bottomItems) as List<ItemPlacement3>
                val indirectBottomItems = bottomItems.filter { !directBottomItems.contains(it) }

                val promises = ArrayList<Deferred<UInt64>>()
                for (bottomItem in directBottomItems) {
                    if (bottomItem.type == item.type) {
                        promises.add(async(Dispatchers.Default) {
                            val thisBottomPlacement = Placement2(bottomItem, Bottom)
                            val thisBottomPlacements = indirectBottomItems.filter { Placement2(it, Bottom).overlapped(thisBottomPlacement) }
                            if (thisBottomPlacements.isNotEmpty()) {
                                UInt64.one + layerLayer(bottomItem, thisBottomPlacements)
                            } else {
                                UInt64.one
                            }
                        })
                    }
                }
                val maxLayer = promises.maxOfOrNull { it.await() } ?: UInt64.zero
                maxLayer
            }
        }

        @Suppress("UNCHECKED_CAST")
        private suspend fun  layerHeight(
            item: ItemPlacement3,
            bottomItems: List<ItemPlacement3>,
        ): Flt64 {
            return coroutineScope {
                val directBottomItems = topPlacements(bottomItems) as List<ItemPlacement3>
                val indirectBottomItems = bottomItems.filter { !directBottomItems.contains(it) }

                val promises = ArrayList<Deferred<Flt64>>()
                for (bottomItem in directBottomItems) {
                    if (bottomItem.type == item.type) {
                        promises.add(async(Dispatchers.Default) {
                            val thisBottomPlacement = Placement2(bottomItem, Bottom)
                            val thisBottomPlacements = indirectBottomItems.filter { Placement2(it, Bottom).overlapped(thisBottomPlacement) }
                            if (thisBottomPlacements.isNotEmpty()) {
                                bottomItem.height + layerHeight(bottomItem, thisBottomPlacements)
                            } else {
                                bottomItem.height
                            }
                        })
                    }
                }
                promises.maxOfOrNull { it.await() } ?: Flt64.zero
            }
        }

        suspend fun  layer(
            item: ItemPlacement3,
            bottomItems: List<ItemPlacement3>,
        ): Pair<UInt64, Flt64> {
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
        unit: AbstractCuboid,
        bottomSupport: BottomSupport
    ): Boolean {
        return hangingPolicy.enabledStackingOn(
            unit = unit,
            bottomSupport = bottomSupport
        )
    }

    fun enabledStackingOn(
        item: Item,
        bottomItem: Item?,
        layer: UInt64 = UInt64.zero,
        height: Flt64 = Flt64.zero,
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
        height: Flt64 = Flt64.zero,
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

    @Suppress("UNCHECKED_CAST")
    suspend fun enabledStackingOn(
        item: ItemPlacement3,
        bottomItems: List<ItemPlacement3>,
        space: AbstractContainer3Shape = Container3Shape()
    ): Boolean {
        val directBottomItems = topPlacements(bottomItems) as List<ItemPlacement3>
        val indirectBottomItems = bottomItems.filter { !directBottomItems.contains(it) }

        if (extraStackingOnRule?.invoke(item, directBottomItems, indirectBottomItems) == false) {
            return false
        }

        // If the material is not at the bottom (y coordinate is not 0) and it is required to be at the bottom, there cannot be any non-bottom-required material below it.
        if (item.bottomOnly && bottomItems.any { !it.bottomOnly }) {
            return false
        }

        if (!item.unit.enabledOrientationsAt(space).contains(item.orientation)) {
            return false
        }

        val (layer, height) = layer(item, bottomItems)

        for (bottomItem in directBottomItems) {
            if (!bottomItem.view.topFlat) {
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
