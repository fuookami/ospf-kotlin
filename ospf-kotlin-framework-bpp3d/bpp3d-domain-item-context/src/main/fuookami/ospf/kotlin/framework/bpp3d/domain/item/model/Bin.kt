package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.combinatorics.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class BinType(
    // inherited from Container3Shape
    override val width: Flt64,
    override val height: Flt64,
    override val depth: Flt64,
    val capacity: Flt64,
    val longitudinalBalance: Flt64?,
    val lateralBalance: Flt64?,
    val typeCode: String,
    val isMain: Boolean = false,
    val extraCheckRule: ((BinType, List<BinLayerPlacement>) -> Boolean)? = null
) : AbstractContainer3Shape {
    fun new(
        width: Flt64? = null,
        height: Flt64? = null,
        depth: Flt64? = null,
        capacity: Flt64? = null,
        longitudinalBalance: Flt64? = null,
        lateralBalance: Flt64? = null,
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
    override fun enabled(unit: AbstractCuboid, orientation: Orientation): Boolean {
        return super.enabled(unit, orientation) && unit.weight leq capacity
    }

    override fun enabled(unit: Placement3<*>): Boolean {
        return super.enabled(unit) && unit.weight leq capacity
    }

    override fun enabled(units: List<Placement3<*>>): Boolean {
        return super.enabled(units) && units.sumOf { it.weight } leq capacity
    }

    fun estimateAmount(
        totalVolume: Flt64,
        totalWeight: Flt64,
        estimatedLoadingRate: Flt64 = Flt64.one
    ): Flt64 {
        return max((totalVolume / volume) / estimatedLoadingRate, totalWeight / capacity)
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
                                Flt64.zero
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            Flt64.zero
                        }
                    }
                }
                val rhsMaxY = rhs.units.maxOf {
                    it.y + when (val unit = it.unit) {
                        is Item -> {
                            if (unit.bottomOnly) {
                                it.maxY
                            } else {
                                Flt64.zero
                            }
                        }

                        is ItemContainer<*> -> {
                            unit.bottomOnlyHeight
                        }

                        else -> {
                            Flt64.zero
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
                    var z = Flt64.zero
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

@Suppress("UNCHECKED_CAST")
fun List<Bin<*>>.unpack(): Map<Item, UInt64> {
    val items = HashSet<Item>()
    for (bin in this) {
        items.addAll(bin.amounts.keys as Set<Item>)
    }
    return items.associateWith { item -> this.sumOf { bin -> bin.amount(item) } }
}

infix fun Collection<Bin<*>>.ord(rhs: Collection<Bin<*>>): Order {
    return when (val result = this.size ord rhs.size) {
        Order.Equal -> {
            if (this.isEmpty() || rhs.isEmpty()) {
                Order.Equal
            } else {
                this.minOf { it.loadingRate } ord rhs.minOf { it.loadingRate }
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
