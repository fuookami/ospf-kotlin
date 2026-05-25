@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyCuboid
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyQuantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyTwo
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyZero

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.concept.AutoIndexed
import fuookami.ospf.kotlin.utils.functional.ThreeWayComparator
import fuookami.ospf.kotlin.utils.functional.sortedWithThreeWayComparator
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.combinatorics.permuteAsync
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.ord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope

class BinType(
    // inherited from Container3Shape
    override val width: LegacyQuantity,
    override val height: LegacyQuantity,
    override val depth: LegacyQuantity,
    val capacity: LegacyQuantity,
    val longitudinalBalance: LegacyScalar?,
    val lateralBalance: LegacyScalar?,
    val typeCode: String,
    val isMain: Boolean = false,
    val extraCheckRule: ((BinType, List<BinLayerPlacement>) -> Boolean)? = null
) : AbstractContainer3Shape {
    fun new(
        width: LegacyQuantity? = null,
        height: LegacyQuantity? = null,
        depth: LegacyQuantity? = null,
        capacity: LegacyQuantity? = null,
        longitudinalBalance: LegacyScalar? = null,
        lateralBalance: LegacyScalar? = null,
        typeCode: String? = null,
        isMain: Boolean? = null,
        extraCheckRule: ((BinType, List<BinLayerPlacement>) -> Boolean)? = null
    ): BinType {
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

    // inherited from Container3Shape
    override fun enabled(unit: LegacyCuboid, orientation: Orientation): Boolean {
        return super.enabled(unit, orientation) && unit.weight leq capacity
    }

    override fun enabled(unit: Placement3<*>): Boolean {
        return super.enabled(unit) && unit.weight leq capacity
    }

    override fun enabled(units: List<Placement3<*>>): Boolean {
        return super.enabled(units) && units.sumOf { it.weight } leq capacity
    }

    fun estimateAmount(
        totalVolume: LegacyQuantity,
        totalWeight: LegacyQuantity,
        estimatedLoadingRate: LegacyScalar = legacyOne()
    ): LegacyScalar {
        return max(
            ((totalVolume / volume).value / estimatedLoadingRate),
            (totalWeight / capacity).value
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
                                it.y * legacyZero()
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            it.y * legacyZero()
                        }
                    }
                }
                val rhsMaxY = rhs.units.maxOf {
                    it.y + when (val unit = it.unit) {
                        is Item -> {
                            if (unit.bottomOnly) {
                                it.maxY
                            } else {
                                it.y * legacyZero()
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            it.y * legacyZero()
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
                    var z = legacyZero() * depth.unit
                    val thisPlacements = thisLayers.map {
                        val ret = Placement3(BinLayerView(it.copy()), point3(z = z))
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

class Bin<T : Cuboid<T>>(
    // inherited from Container3<Bin<T>>
    override val shape: BinType,
    override val units: List<Placement3<T>>,
    val batchNo: BatchNo? = null
) : Container3<Bin<T>>, AutoIndexed(Bin::class) {
    val capacity by shape::capacity

    override fun copy() = Bin(shape, units.map { it.copy() }, batchNo)
    fun copy(newBatchNo: BatchNo) = Bin(shape, units.map { it.copy() }, newBatchNo)
}

fun List<Bin<*>>.group(): Map<BinType, UInt64> {
    return this.groupBy { it.shape }.mapValues { UInt64(it.value.size) }
}
fun List<Bin<*>>.unpack(): Map<Item, UInt64> {
    val items = HashSet<Item>()
    for (bin in this) {
        items.addAll(bin.amounts.keys.filterIsInstance<Item>())
    }
    return items.associateWith { item ->
        this.fold(UInt64.zero) { acc, bin -> acc + bin.amount(item) }
    }
}

infix fun Collection<Bin<*>>.ord(rhs: Collection<Bin<*>>): Order {
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

typealias LayerBin = Bin<BinLayer>
typealias ItemBin = Bin<Item>

fun LayerBin.dump(): ItemBin {
    return Bin(this.shape, this.units.flatMap { it.unit.dumpAbsolutely() })
}
