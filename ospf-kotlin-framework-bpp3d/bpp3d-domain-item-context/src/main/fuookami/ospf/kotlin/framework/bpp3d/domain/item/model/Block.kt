package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.*

sealed class Block(
    // inherited from Container3<Block>
    final override val units: List<ItemPlacement3>,
) : ItemContainer<Block> {
    // inherited from Container3<Block>
    final override val shape = Container3Shape(
        width = units.maxOf { it.maxX } - units.minOf { it.x },
        height = units.maxOf { it.maxY } - units.minOf { it.y },
        depth = units.maxOf { it.maxZ } - units.minOf { it.z }
    )

    // inherited from ItemContainer<Block>
    final override val items = units

    override fun hashCode() = units.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Block) return false

        if (!(units.toTypedArray() contentEquals other.units.toTypedArray())) return false

        return true
    }
}

class CommonBlock(
    units: List<ItemPlacement3>
) : Block(units) {
    override fun copy() = CommonBlock(units.map { it.copy() })
}

class SimpleBlock(
    units: List<ItemPlacement3>,
) : Block(units) {
    init {
        assert(units.all { it.unit == units.first().unit })
    }

    val item: Item = units.first().unit
    val itemView: ItemView = units.first().view as ItemView
    val itemOrientation: Orientation = units.first().orientation
    val layer: UInt64 = (height / itemView.height).round().toUInt64()

    // inherited from ItemContainer<HollowSquareBlock>
    override val bottomOnly: Boolean = itemView.bottomOnly
    override val topFlat: Boolean = itemView.topFlat

    override val packageType: PackageType = item.packageType

    override fun copy() = SimpleBlock(units.map { it.copy() })

    override fun toString(): String {
        return "$itemView ${(width / itemView.width).round().toUInt64()} * $layer * ${(depth / itemView.depth).round().toUInt64()}"
    }
}

class HollowSquareBlock(
    units: List<ItemPlacement3>,
) : Block(units) {
    companion object {
        operator fun invoke(
            item: Item,
            space: AbstractContainer3Shape,
            amount: UInt64 = UInt64.maximum
        ): HollowSquareBlock? {
            val units = ItemMerger.mergeHollowSquareBlocks(
                items = mapOf(Pair(item, UInt64.maximum)),
                space = space
            )

            return if (units.first.isNotEmpty() && units.first.first().amounts[item]!! leq amount) {
                units.first.first()
            } else {
                null
            }
        }
    }

    init {
        assert(units.all { it.unit == units.first().unit })
        val orientations = units.groupBy { it.orientation }.keys.toList()
        assert(orientations.size == 2)
        assert(orientations[0].rotation == orientations[1])
    }

    val item: Item = units.first().unit
    val itemView: ItemView = units.first().view as ItemView
    val itemOrientation: Orientation = units.first().orientation
    val itemRotationView: ItemView = units.first().view.rotation!! as ItemView
    val itemRotatedOrientation: Orientation = units.find { it.orientation != itemOrientation }!!.orientation
    val layer: UInt64 = (height / itemView.height).round().toUInt64()

    // inherited from ItemContainer<HollowSquareBlock>
    override val bottomOnly: Boolean = itemView.bottomOnly
    override val topFlat: Boolean = itemView.topFlat

    override val packageType: PackageType = item.packageType

    override fun copy() = HollowSquareBlock(units.map { it.copy() })

    override fun toString(): String {
        val amount = units.count { it.view == itemView && it.position.y eq Flt64.zero }
        val rotationAmount = units.filter { it.view == itemRotationView && it.position.y eq Flt64.zero }.size
        return "$item (${amount} + ${rotationAmount})*$layer"
    }
}

class Pile(
    val itemViews: List<ItemView>
) : Block(dump(itemViews)) {
    companion object {
        private fun dump(items: List<ItemView>): List<ItemPlacement3> {
            val units = ArrayList<ItemPlacement3>()
            var y = Flt64.zero
            for (item in items) {
                units.add(Placement3(item, point3(y = y)))
                y += item.height
            }
            return units
        }

        fun layer(
            item: Item,
            bottomItems: List<Item>,
        ): Pair<UInt64, Flt64> {
            return layer(item.view(), bottomItems.map { ItemView(it) })
        }

        fun layer(
            item: ItemView,
            bottomItems: List<ItemView>,
        ): Pair<UInt64, Flt64> {
            return if (bottomItems.isNotEmpty() && bottomItems.last().type == item.type) {
                val notSameIndex = bottomItems.indexOfLast { it.type != item.type }
                val sameItems = if (notSameIndex == -1) {
                    bottomItems
                } else {
                    bottomItems.subList(notSameIndex + 1, bottomItems.size)
                }
                Pair(UInt64(sameItems.size), sameItems.sumOf { it.height })
            } else {
                Pair(UInt64.zero, Flt64.zero)
            }
        }
    }

    val bottomItem: Item = itemViews.first().unit
    val bottomItemView: ItemView = itemViews.first()
    val bottomItemOrientation: Orientation = items.first().orientation
    val topItem: Item = itemViews.last().unit
    val topItemView: ItemView = itemViews.last()
    val topItemOrientation: Orientation = itemViews.last().orientation
    val bottomLayer: UInt64 = UInt64(itemViews.indexOfFirst { it.type != bottomItem.type })
    val topLayer: UInt64 = UInt64(itemViews.asReversed().indexOfFirst { it.type != topItem.type })

    val rotation: Pile?
        get() {
            return try {
                Pile(itemViews.map { it.rotation!! })
            } catch (e: Exception) {
                null
            }
        }

    // inherited from CuboidUnit<Pile>
    override val enabledOrientations: List<Orientation> = listOf(Orientation.Upright, Orientation.UprightRotated)

    override fun view(orientation: Orientation): BlockView? {
        return when (orientation) {
            Orientation.UprightRotated -> rotation?.let { BlockView(it.copy()) }
            else -> BlockView(copy())
        }
    }

    // inherited from ItemContainer<LayeredBlock>
    override val bottomOnly: Boolean = bottomItemView.bottomOnly
    override val topFlat: Boolean = topItemView.topFlat

    override val packageType: PackageType = bottomItem.packageType

    override fun copy() = Pile(itemViews.map { it.copy() })
}

class LayeredBlock(
    // inherited from Container3<Block>
    val blocks: List<SimpleBlock>
) : Block(dump(blocks)) {
    companion object {
        private fun dump(blocks: List<SimpleBlock>): List<ItemPlacement3> {
            var y = Flt64.zero
            val placements = ArrayList<ItemPlacement3>()
            for (block in blocks) {
                placements.addAll(block.units.dump(point3(y = y)))
                y += block.height
            }
            return placements
        }
    }

    val bottomItem: Item = blocks.first().item
    val bottomItemView: ItemView = blocks.first().itemView
    val bottomItemOrientation: Orientation = blocks.first().itemOrientation
    val topItem: Item = blocks.last().item
    val topItemView: ItemView = blocks.last().itemView
    val topItemOrientation: Orientation = blocks.last().itemOrientation
    val bottomLayer: UInt64 = blocks
        .subList(0, blocks.indexOfFirst { it.item.type != bottomItem.type })
        .sumOf { it.layer }
    val topLayer: UInt64 = blocks
        .subList(blocks.indexOfLast { it.item.type != topItem.type } + 1, blocks.size)
        .sumOf { it.layer }

    // inherited from ItemContainer<LayeredBlock>
    override val bottomOnly: Boolean = bottomItemView.bottomOnly
    override val topFlat: Boolean = topItemView.topFlat

    override val packageType: PackageType = bottomItem.packageType

    override fun copy() = LayeredBlock(blocks.map { it.copy() })
}

class ComplexBlock(
    // inherited from Container3<Block>
    val blocks: List<BlockPlacement3>
) : Block(dump(blocks)) {
    companion object {
        private fun dump(blocks: List<BlockPlacement3>): List<ItemPlacement3> {
            val placements = ArrayList<ItemPlacement3>()
            for (block in blocks) {
                placements.addAll(block.unit.units.dump(block.position))
            }
            return placements
        }
    }

    override fun copy() = ComplexBlock(blocks.map { it.copy() })
}

typealias BlockView = CuboidView<Block>
typealias BlockPlacement2<P> = Placement2<Block, P>
typealias BlockPlacement3 = Placement3<Block>
