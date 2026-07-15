/**
 * Bin model.
 * 箱位模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.AutoIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.combinatorics.permuteAsync
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * Bin type descriptor defining the physical and operational characteristics of a container.
 * 箱型描述符，定义箱位的物理与操作特征。
 *
 * @property width 箱宽 / bin width
 * @property height 箱高 / bin height
 * @property depth 箱深 / bin depth
 * @property capacity 承重容量 / weight capacity
 * @property longitudinalBalance 纵向平衡限制 / longitudinal balance limit
 * @property lateralBalance 横向平衡限制 / lateral balance limit
 * @property typeCode 箱型标识 / bin type identifier
 * @property isMain 是否为主箱型 / whether this is the main bin type
 * @property extraCheckRule 额外校验规则 / extra validation rule
*/
open class BinType<V : FloatingNumber<V>>(
    // inherited from Container3Shape
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val depth: Quantity<V>,
    val capacity: Quantity<V>,
    val longitudinalBalance: V?,
    val lateralBalance: V?,
    open val typeCode: BinTypeId,
    val isMain: Boolean = false,
    val extraCheckRule: ((BinType<V>, List<QuantityPlacement3<BinLayer, FltX>>) -> Boolean)? = null
) : Container3Geometry<V> {

    /**
     * Create a copy of this bin type with selectively overridden properties.
     * 创建此箱型的副本，可选择性地覆盖部分属性。
     *
     * @param width 覆盖宽度 / overridden width
     * @param height 覆盖高度 / overridden height
     * @param depth 覆盖深度 / overridden depth
     * @param capacity 覆盖承重容量 / overridden weight capacity
     * @param longitudinalBalance 覆盖纵向平衡限制 / overridden longitudinal balance limit
     * @param lateralBalance 覆盖横向平衡限制 / overridden lateral balance limit
     * @param typeCode 覆盖箱型标识 / overridden bin type identifier
     * @param isMain 覆盖是否为主箱型 / overridden main bin type flag
     * @param extraCheckRule 覆盖额外校验规则 / overridden extra validation rule
     * @return 新的箱型实例 / new bin type instance
    */
    fun new(
        width: Quantity<V>? = null,
        height: Quantity<V>? = null,
        depth: Quantity<V>? = null,
        capacity: Quantity<V>? = null,
        longitudinalBalance: V? = null,
        lateralBalance: V? = null,
        typeCode: BinTypeId? = null,
        isMain: Boolean? = null,
        extraCheckRule: ((BinType<V>, List<QuantityPlacement3<BinLayer, FltX>>) -> Boolean)? = null
    ): BinType<V> {
        return BinType(
            width = width ?: this.width,
            height = height ?: this.height,
            depth = depth ?: this.depth,
            capacity = capacity ?: this.capacity,
            longitudinalBalance = longitudinalBalance ?: this.longitudinalBalance,
            lateralBalance = lateralBalance ?: this.lateralBalance,
            typeCode = typeCode ?: this.typeCode,
            isMain = isMain ?: this.isMain,
            extraCheckRule = extraCheckRule ?: this.extraCheckRule
        )
    }

    /**
     * Program layers into this bin type by finding a valid placement permutation.
     * 将箱层编排到当前箱型中，寻找有效的放置排列。
     *
     * @param layers 待编排的箱层列表 / bin layers to program
     * @param withCheck 是否执行额外校验 / whether to perform extra validation
     * @param layerComparator 自定义箱层三路比较器 / custom three-way comparator for bin layers
     * @return 有效的放置列表，若无合法排列则返回 null / valid placement list, or null if no legal permutation exists
    */
    suspend fun program(
        layers: List<BinLayer>,
        withCheck: Boolean = true,
        layerComparator: ThreeWayComparator<BinLayer>? = null
    ): List<QuantityPlacement3<BinLayer, FltX>>? {
        val sortedLayers = layers.sortedWithThreeWayComparator { lhs, rhs ->
            if (layerComparator != null) {
                when (val value = layerComparator(lhs, rhs)) {
                    Order.Equal -> {}
                    else -> {
                        return@sortedWithThreeWayComparator value
                    }
                }
            }

            when (val value = lhs.packageType ord rhs.packageType) {
                Order.Equal -> {}
                else -> {
                    return@sortedWithThreeWayComparator value
                }
            }
            if (lhs.bottomOnly && !rhs.bottomOnly) {
                Order.Less()
            } else if (!lhs.bottomOnly && rhs.bottomOnly) {
                Order.Greater()
            } else {
                val lhsMaxY = lhs.units.maxOfQuantity {
                    it.y + when (val unit = it.unit) {
                        is Item -> {
                            if (unit.bottomOnly) {
                                it.maxY
                            } else {
                                it.y * FltX.zero
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            it.y * FltX.zero
                        }
                    }
                }
                val rhsMaxY = rhs.units.maxOfQuantity {
                    it.y + when (val unit = it.unit) {
                        is Item -> {
                            if (unit.bottomOnly) {
                                it.maxY
                            } else {
                                it.y * FltX.zero
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            it.y * FltX.zero
                        }
                    }
                }
                when (val value = rhsMaxY ord lhsMaxY) {
                    Order.Equal -> {}
                    else -> {
                        return@sortedWithThreeWayComparator value
                    }
                }
                rhs.loadingRate ord lhs.loadingRate
            }
        }

        var placements: List<QuantityPlacement3<BinLayer, FltX>>? = null
        return try {
            coroutineScope {
                val layersPromise = permuteAsync(
                    input = sortedLayers,
                    scope = this
                )
                for (thisLayers in layersPromise) {
                    var z = FltX.zero * depth.unit
                    val thisPlacements = thisLayers.map {
                        val ret = binLayerPlacementOf(
                            view = CuboidView<BinLayer, FltX>(it.copy()),
                            position = QuantityPoint3(
                                x = FltX.zero * depth.unit,
                                y = FltX.zero * depth.unit,
                                z = z
                            )
                        )
                        z += it.depth
                        ret
                    }
                    if (withCheck) {
                        if (extraCheckRule?.invoke(this@BinType, thisPlacements) == false) {
                            continue
                        }
                    }
                    placements = thisPlacements
                    break
                }
                layersPromise.close()
                cancel()
            }
            null
        } catch (e: CancellationException) {
            placements
        }
    }

    override fun toString() = "$typeCode-$width*$height*$depth"
}

/**
 * Check whether the given item in the specified orientation fits within this bin type (geometry and capacity).
 * 检查给定货物在指定朝向下是否适配此箱型（几何与承重）。
 *
 * @param unit 货物 / item
 * @param orientation 朝向 / orientation
 * @return 是否启用 / whether enabled
*/
fun BinType<FltX>.enabled(unit: Item, orientation: Orientation): Boolean {
    val geometry: Container3Geometry<FltX> = this
    return geometry.enabled(unit, orientation) && unit.weight leq capacity
}

/**
 * Check whether the given placement fits within this bin type (geometry and capacity).
 * 检查给定放置是否适配此箱型（几何与承重）。
 *
 * @param unit 放置 / placement
 * @return 是否启用 / whether enabled
*/
fun BinType<FltX>.enabled(unit: QuantityPlacement3<*, FltX>): Boolean {
    val geometry: Container3Geometry<FltX> = this
    return geometry.enabled(unit) && unit.weight leq capacity
}

/**
 * Check whether the given list of placements fits within this bin type (geometry and total capacity).
 * 检查给定放置列表是否适配此箱型（几何与总承重）。
 *
 * @param units 放置列表 / placement list
 * @return 是否启用 / whether enabled
*/
fun BinType<FltX>.enabled(units: List<QuantityPlacement3<*, FltX>>): Boolean {
    val geometry: Container3Geometry<FltX> = this
    return geometry.enabled(units) && units.sumOfQuantity { it.weight } leq capacity
}

/**
 * Estimate the number of bins required for the given total volume and weight.
 * 估算给定总体积和总重量所需的箱位数量。
 *
 * @param totalVolume 总体积 / total volume
 * @param totalWeight 总重量 / total weight
 * @param estimatedLoadingRate 预估装载率 / estimated loading rate
 * @return 估算箱位数 / estimated number of bins
*/
fun BinType<FltX>.estimateAmount(
    totalVolume: Quantity<FltX>,
    totalWeight: Quantity<FltX>,
    estimatedLoadingRate: FltX = FltX.one
): FltX {
    return max(
        ((totalVolume / volume).value / estimatedLoadingRate),
        (totalWeight / capacity).value
    )
}

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> Quantity<V>.toFltXQuantity(): Quantity<FltX> {
    return when (value) {
        is FltX -> this as Quantity<FltX>
        else -> Quantity(FltX(value.toString().toDouble()), unit)
    }
}

/**
 * Convert this bin type to a generic [Container3Shape].
 * 将此箱型转换为通用 [Container3Shape]。
 *
 * @return 容器三维形状 / container 3D shape
*/
fun <V : FloatingNumber<V>> BinType<V>.asContainer3Shape(): Container3Shape {
    return Container3Shape(
        width = width.toFltXQuantity(),
        height = height.toFltXQuantity(),
        depth = depth.toFltXQuantity()
    )
}

/** 箱体容器，管理层堆叠与装箱约束 / Bin container, managing layer stacking and packing constraints */
class Bin<T, V> internal constructor(
    // inherited from Container3<Bin<T, V>>
    val type: BinType<V>,
    override val units: List<QuantityPlacement3<T, V>>,
    val batchNo: BatchNo? = null
) : Container3<Bin<T, V>, V>, AutoIndexed(Bin::class) where T : Cuboid<T, V>, V : FloatingNumber<V> {
    override val shape: Container3Geometry<V> = type
    val capacity by type::capacity

    override fun copy() = Bin(type, units.map { it.copy() }, batchNo)

    /**
     * 创建带新批次号的副本。
     * Create a copy with a new batch number.
     *
     * @param newBatchNo 新批次号 / new batch number
     * @return 副本 / a copy
    */
    fun copy(newBatchNo: BatchNo) = Bin(type, units.map { it.copy() }, newBatchNo)
}

/**
 * 创建箱层装箱，隐藏底层 Bin 泛型构造。
 * Create a layer bin while hiding the underlying polymorphic Bin constructor.
 *
 * @param shape 箱型 / bin type
 * @param units 箱层放置列表 / bin-layer placements
 * @param batchNo 批次号 / batch number
 * @return 箱层装箱 / layer bin
*/
fun layerBinOf(
    shape: BinType<FltX>,
    units: List<QuantityPlacement3<BinLayer, FltX>>,
    batchNo: BatchNo? = null
): Bin<BinLayer, FltX> {
    return Bin(
        type = shape,
        units = units,
        batchNo = batchNo
    )
}

/**
 * 创建货物装箱，隐藏底层 Bin 泛型构造。
 * Create an item bin while hiding the underlying polymorphic Bin constructor.
 *
 * @param shape 箱型 / bin type
 * @param units 货物放置列表 / item placements
 * @param batchNo 批次号 / batch number
 * @return 货物装箱 / item bin
*/
fun itemBinOf(
    shape: BinType<FltX>,
    units: List<QuantityPlacement3<Item, FltX>>,
    batchNo: BatchNo? = null
): Bin<Item, FltX> {
    return Bin(
        type = shape,
        units = units,
        batchNo = batchNo
    )
}

/**
 * 创建组合块装箱，隐藏底层 Bin 泛型构造。
 * Create a block bin while hiding the underlying polymorphic Bin constructor.
 *
 * @param shape 箱型 / bin type
 * @param units 组合块放置列表 / block placements
 * @param batchNo 批次号 / batch number
 * @return 组合块装箱 / block bin
*/
fun blockBinOf(
    shape: BinType<FltX>,
    units: List<QuantityPlacement3<Block, FltX>>,
    batchNo: BatchNo? = null
): Bin<Block, FltX> {
    return Bin(
        type = shape,
        units = units,
        batchNo = batchNo
    )
}

/**
 * Group bins by type and count the number of each type.
 * 按箱型对装箱分组并统计各类型数量。
 *
 * @param V 浮点数类型 / floating number type
 * @return 箱型到数量的映射 / bin type to count map
*/
fun <V : FloatingNumber<V>> List<Bin<*, V>>.group(): Map<BinType<V>, UInt64> {
    return this.groupBy { it.type }.mapValues { UInt64(it.value.size) }
}

/**
 * 将装箱列表展开为货物映射。
 * Unpack a list of bins into an item map.
 *
 * @return 货物与数量的映射 / item-to-quantity map
*/
fun List<Bin<*, FltX>>.unpack(): Map<Item, UInt64> {
    val items = HashSet<Item>()
    for (bin in this) {
        items.addAll(bin.amounts.keys.filterIsInstance<Item>())
    }
    return items.associateWith { item ->
        this.fold(UInt64.zero) { acc, bin -> acc + bin.amount(item) }
    }
}

/**
 * 比较两个装箱集合的装载效率。
 * Compare loading efficiency of two bin collections.
 *
 * @param rhs 右侧装箱集合 / the right-hand side bin collection
 * @return 比较结果 / comparison result
*/
infix fun Collection<Bin<*, FltX>>.ord(rhs: Collection<Bin<*, FltX>>): Order {
    return when (val result = this.size ord rhs.size) {
        Order.Equal -> {
            if (this.isEmpty() || rhs.isEmpty()) {
                Order.Equal
            } else {
                var lhsMin = this.first().loadingRate
                for (bin in this.drop(1)) {
                    if (bin.loadingRate ord lhsMin is Order.Less) {
                        lhsMin = bin.loadingRate
                    }
                }
                var rhsMin = rhs.first().loadingRate
                for (bin in rhs.drop(1)) {
                    if (bin.loadingRate ord rhsMin is Order.Less) {
                        rhsMin = bin.loadingRate
                    }
                }
                when (lhsMin ord rhsMin) {
                    is Order.Less -> Order.Less()
                    is Order.Greater -> Order.Greater()
                    Order.Equal -> Order.Equal
                }
            }
        }

        else -> {
            result
        }
    }
}

/**
 * 将箱层装箱展开为货物装箱。
 * Dump a layer bin into an item bin.
 *
 * @return 货物装箱 / item bin
*/
fun Bin<BinLayer, FltX>.dump(): Bin<Item, FltX> {
    return itemBinOf(
        shape = this.type,
        units = this.units.flatMap { it.unit.dumpAbsolutely() }
    )
}
