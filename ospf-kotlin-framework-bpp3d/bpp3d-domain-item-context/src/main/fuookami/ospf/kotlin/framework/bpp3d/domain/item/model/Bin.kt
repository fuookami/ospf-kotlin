@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.combinatorics.permuteAsync
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.concept.AutoIndexed
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.ThreeWayComparator
import fuookami.ospf.kotlin.utils.functional.ord
import fuookami.ospf.kotlin.utils.functional.sortedWithThreeWayComparator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
class BinType<V : FloatingNumber<V>>(
    // inherited from Container3Shape
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val depth: Quantity<V>,
    val capacity: Quantity<V>,
    val longitudinalBalance: V?,
    val lateralBalance: V?,
    val typeCode: String,
    val isMain: Boolean = false,
    val extraCheckRule: ((BinType<V>, List<BinLayerPlacement>) -> Boolean)? = null
) : Container3Geometry<V> {
    fun new(
        width: Quantity<V>? = null,
        height: Quantity<V>? = null,
        depth: Quantity<V>? = null,
        capacity: Quantity<V>? = null,
        longitudinalBalance: V? = null,
        lateralBalance: V? = null,
        typeCode: String? = null,
        isMain: Boolean? = null,
        extraCheckRule: ((BinType<V>, List<BinLayerPlacement>) -> Boolean)? = null
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

    suspend fun program(
        layers: List<BinLayer>,
        withCheck: Boolean = true,
        layerComparator: ThreeWayComparator<BinLayer>? = null
    ): List<BinLayerPlacement>? {
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
                val lhsMaxY = lhs.units.maxOf {
                    it.y + when (val unit = it.unit) {
                        is Item -> {
                            if (unit.bottomOnly) {
                                it.maxY
                            } else {
                                it.y * fltXZero()
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            it.y * fltXZero()
                        }
                    }
                }
                val rhsMaxY = rhs.units.maxOf {
                    it.y + when (val unit = it.unit) {
                        is Item -> {
                            if (unit.bottomOnly) {
                                it.maxY
                            } else {
                                it.y * fltXZero()
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            it.y * fltXZero()
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

        var placements: List<BinLayerPlacement>? = null
        return try {
            coroutineScope {
                val layersPromise = permuteAsync(
                    input = sortedLayers,
                    scope = this
                )
                for (thisLayers in layersPromise) {
                    var z = fltXZero() * depth.unit
                    val thisPlacements = thisLayers.map {
                        val ret = binLayerPlacementOf(
                            view = BinLayerView(it.copy()),
                            position = QuantityPoint3(
                                x = fltXZero() * depth.unit,
                                y = fltXZero() * depth.unit,
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

fun BinType<FltX>.enabled(unit: Item, orientation: Orientation): Boolean {
    val geometry: Container3Geometry<FltX> = this
    return geometry.enabled(unit, orientation) && unit.weight leq capacity
}

fun BinType<FltX>.enabled(unit: AnyPlacement3): Boolean {
    val geometry: Container3Geometry<FltX> = this
    return geometry.enabled(unit) && unit.weight leq capacity
}

fun BinType<FltX>.enabled(units: List<AnyPlacement3>): Boolean {
    val geometry: Container3Geometry<FltX> = this
    return geometry.enabled(units) && units.sumOf { it.weight } leq capacity
}

fun BinType<FltX>.estimateAmount(
    totalVolume: Quantity<FltX>,
    totalWeight: Quantity<FltX>,
    estimatedLoadingRate: FltX = fltXOne()
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
        else -> Quantity(fltX(value.toString().toDouble()), unit)
    }
}

fun <V : FloatingNumber<V>> BinType<V>.asContainer3Shape(): Container3Shape {
    return Container3Shape(
        width = width.toFltXQuantity(),
        height = height.toFltXQuantity(),
        depth = depth.toFltXQuantity()
    )
}

class Bin<T, V> internal constructor(
    // inherited from Container3<Bin<T, V>>
    val type: BinType<V>,
    override val units: List<Placement3<T, V>>,
    val batchNo: BatchNo? = null
) : Container3<Bin<T, V>, V>, AutoIndexed(Bin::class) where T : Cuboid<T, V>, V : FloatingNumber<V> {
    override val shape: Container3Geometry<V> = type
    val capacity by type::capacity

    override fun copy() = Bin(type, units.map { it.copy() }, batchNo)
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
    units: List<BinLayerPlacement>,
    batchNo: BatchNo? = null
): LayerBin {
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
    units: List<ItemPlacement3>,
    batchNo: BatchNo? = null
): ItemBin {
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
    units: List<BlockPlacement3>,
    batchNo: BatchNo? = null
): BlockBin {
    return Bin(
        type = shape,
        units = units,
        batchNo = batchNo
    )
}

fun <V : FloatingNumber<V>> List<Bin<*, V>>.group(): Map<BinType<V>, UInt64> {
    return this.groupBy { it.type }.mapValues { UInt64(it.value.size) }
}

fun List<Bin<*, FltX>>.unpack(): Map<Item, UInt64> {
    val items = HashSet<Item>()
    for (bin in this) {
        items.addAll(bin.amounts.keys.filterIsInstance<Item>())
    }
    return items.associateWith { item ->
        this.fold(UInt64.zero) { acc, bin -> acc + bin.amount(item) }
    }
}

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

/** 箱层装箱别名。Bin alias for bin-layer units. */
typealias LayerBin = Bin<BinLayer, FltX>
/** 货物装箱别名。Bin alias for item units. */
typealias ItemBin = Bin<Item, FltX>
/** 组合块装箱别名。Bin alias for block units. */
typealias BlockBin = Bin<Block, FltX>

fun LayerBin.dump(): ItemBin {
    return itemBinOf(
        shape = this.type,
        units = this.units.flatMap { it.unit.dumpAbsolutely() }
    )
}
