/**
 * 块模型。
 * Block model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.ItemMerger

/**
 * Block 的基类，表示由多个物品放置单元组成的块。
 * Base class for Block, representing a block composed of multiple item placement units.
 *
 * @property units 物品放置单元列表 / List of item placement units
 */
sealed class Block(
    // inherited from Container3<Block>
    final override val units: List<QuantityPlacement3<Item, FltX>>,
) : ItemContainer<Block> {
    // inherited from Container3<Block>
    final override val shape = Container3Shape(
        width = units.maxOfQuantity { it.maxX } - units.minOfQuantity { it.x },
        height = units.maxOfQuantity { it.maxY } - units.minOfQuantity { it.y },
        depth = units.maxOfQuantity { it.maxZ } - units.minOfQuantity { it.z }
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

/**
 * 通用块，无特殊约束的 Block 实现。
 * Common block, a Block implementation with no special constraints.
 */
class CommonBlock(
    units: List<QuantityPlacement3<Item, FltX>>
) : Block(units) {
    override fun copy() = CommonBlock(units.map { it.copy() })
}

/**
 * 简单块，所有放置单元属于同一个物品。
 * Simple block where all placement units belong to the same item.
 *
 * @property item 物品 / Item
 * @property itemView 物品视图 / Item view
 * @property itemOrientation 物品方向 / Item orientation
 * @property layer 层数 / Number of layers
 */
class SimpleBlock(
    units: List<QuantityPlacement3<Item, FltX>>,
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

/**
 * 空心方框块，由两种方向（原始和旋转）的物品放置单元组成。
 * Hollow square block, composed of item placement units in two orientations (original and rotated).
 *
 * @property item 物品 / Item
 * @property itemView 物品视图 / Item view
 * @property itemOrientation 物品方向 / Item orientation
 * @property itemRotationView 旋转后的物品视图 / Rotated item view
 * @property itemRotatedOrientation 旋转后的物品方向 / Rotated item orientation
 * @property layer 层数 / Number of layers
 */
class HollowSquareBlock(
    units: List<QuantityPlacement3<Item, FltX>>,
) : Block(units) {
    companion object {
        operator fun invoke(
            item: Item,
            space: AbstractContainer3Shape,
            amount: UInt64 = UInt64.maximum
        ): HollowSquareBlock? {
            val units = when (val result = ItemMerger.mergeHollowSquareBlocks(
                items = mapOf(Pair(item, UInt64.maximum)),
                space = space
            )) {
                is Ok -> result.value
                is Failed -> return null
                is Fatal -> return null
            }

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
        val amount = units.count { it.view == itemView && it.position.y eq FltX.zero }
        val rotationAmount = units.filter { it.view == itemRotationView && it.position.y eq FltX.zero }.size
        return "$item (${amount} + ${rotationAmount})*$layer"
    }
}

/**
 * 堆叠块，由多个物品视图沿 Y 轴堆叠而成。
 * Pile block, composed of multiple item views stacked along the Y axis.
 *
 * @property itemViews 物品视图列表 / List of item views
 * @property bottomItem 底部物品 / Bottom item
 * @property bottomItemView 底部物品视图 / Bottom item view
 * @property bottomItemOrientation 底部物品方向 / Bottom item orientation
 * @property topItem 顶部物品 / Top item
 * @property topItemView 顶部物品视图 / Top item view
 * @property topItemOrientation 顶部物品方向 / Top item orientation
 * @property bottomLayer 底部层数 / Bottom layer count
 * @property topLayer 顶部层数 / Top layer count
 */
class Pile(
    val itemViews: List<ItemView>
) : Block(dump(itemViews)) {
    companion object {
        /**
         * 将物品视图列表转储为放置单元列表。
         * Dump the list of item views into a list of placement units.
         *
         * @param items 物品视图列表 / List of item views
         * @return 放置单元列表 / List of placement units
         */
        private fun dump(items: List<ItemView>): List<QuantityPlacement3<Item, FltX>> {
            val units = ArrayList<QuantityPlacement3<Item, FltX>>()
            var y = FltX.zero * items.first().height.unit
            for (item in items) {
                units.add(
                    itemPlacement3Of(
                        view = item,
                        position = QuantityPoint3(
                            x = FltX.zero * item.height.unit,
                            y = y,
                            z = FltX.zero * item.height.unit
                        )
                    )
                )
                y += item.height
            }
            return units
        }

        /**
         * 计算物品在底部物品上的层数信息。
         * Calculate the layer information for an item on bottom items.
         *
         * @param item 物品 / Item
         * @param bottomItems 底部物品列表 / List of bottom items
         * @return 层数和高度 / Layer count and height
         */
        fun layer(
            item: Item,
            bottomItems: List<Item>,
        ): Pair<UInt64, Quantity<FltX>> {
            return layer(item.view(), bottomItems.map { ItemView(it) })
        }

        /**
         * 计算物品视图在底部物品视图上的层数信息。
         * Calculate the layer information for an item view on bottom item views.
         *
         * @param item 物品视图 / Item view
         * @param bottomItems 底部物品视图列表 / List of bottom item views
         * @return 层数和高度 / Layer count and height
         */
        fun layer(
            item: ItemView,
            bottomItems: List<ItemView>,
        ): Pair<UInt64, Quantity<FltX>> {
            return if (bottomItems.isNotEmpty() && bottomItems.last().type == item.type) {
                val notSameIndex = bottomItems.indexOfLast { it.type != item.type }
                val sameItems = if (notSameIndex == -1) {
                    bottomItems
                } else {
                    bottomItems.subList(notSameIndex + 1, bottomItems.size)
                }
                Pair(UInt64(sameItems.size), sameItems.sumOfQuantity { it.height })
            } else {
                Pair(UInt64.zero, item.height * FltX.zero)
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

    override fun view(orientation: Orientation): CuboidView<Block, FltX>? {
        return when (orientation) {
            Orientation.UprightRotated -> rotation?.let { CuboidView<Block, FltX>(it.copy()) }
            else -> CuboidView<Block, FltX>(copy())
        }
    }

    // inherited from ItemContainer<LayeredBlock>
    override val bottomOnly: Boolean = bottomItemView.bottomOnly
    override val topFlat: Boolean = topItemView.topFlat

    override val packageType: PackageType = bottomItem.packageType

    override fun copy() = Pile(itemViews.map { it.copy() })
}

/**
 * 分层块，由多个 SimpleBlock 沿 Y 轴堆叠而成。
 * Layered block, composed of multiple SimpleBlocks stacked along the Y axis.
 *
 * @property blocks 简单块列表 / List of simple blocks
 * @property bottomItem 底部物品 / Bottom item
 * @property bottomItemView 底部物品视图 / Bottom item view
 * @property bottomItemOrientation 底部物品方向 / Bottom item orientation
 * @property topItem 顶部物品 / Top item
 * @property topItemView 顶部物品视图 / Top item view
 * @property topItemOrientation 顶部物品方向 / Top item orientation
 * @property bottomLayer 底部层数 / Bottom layer count
 * @property topLayer 顶部层数 / Top layer count
 */
class LayeredBlock(
    // inherited from Container3<Block>
    val blocks: List<SimpleBlock>
) : Block(dump(blocks)) {
    companion object {
        /**
         * 将 SimpleBlock 列表转储为放置单元列表。
         * Dump the list of SimpleBlocks into a list of placement units.
         *
         * @param blocks 简单块列表 / List of simple blocks
         * @return 放置单元列表 / List of placement units
         */
        private fun dump(blocks: List<SimpleBlock>): List<QuantityPlacement3<Item, FltX>> {
            var y = FltX.zero * blocks.first().height.unit
            val placements = ArrayList<QuantityPlacement3<Item, FltX>>()
            for (block in blocks) {
                placements.addAll(
                    block.units.dump(
                        QuantityPoint3(
                            x = FltX.zero * block.height.unit,
                            y = y,
                            z = FltX.zero * block.height.unit
                        )
                    )
                )
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
        .takeWhile { it.item.type == bottomItem.type }
        .fold(UInt64.zero) { acc, block -> acc + block.layer }
    val topLayer: UInt64 = blocks
        .takeLastWhile { it.item.type == topItem.type }
        .fold(UInt64.zero) { acc, block -> acc + block.layer }

    // inherited from ItemContainer<LayeredBlock>
    override val bottomOnly: Boolean = bottomItemView.bottomOnly
    override val topFlat: Boolean = topItemView.topFlat

    override val packageType: PackageType = bottomItem.packageType

    override fun copy() = LayeredBlock(blocks.map { it.copy() })
}

/**
 * 复杂块，由多个子块通过三维空间放置组合而成。
 * Complex block, composed of multiple sub-blocks placed in 3D space.
 *
 * @property blocks 子块放置列表 / List of sub-block placements
 */
class ComplexBlock(
    // inherited from Container3<Block>
    val blocks: List<QuantityPlacement3<Block, FltX>>
) : Block(dump(blocks)) {
    companion object {
        /**
         * 将 Block 放置列表转储为物品放置单元列表。
         * Dump the list of Block placements into a list of item placement units.
         *
         * @param blocks 块放置列表 / List of block placements
         * @return 物品放置单元列表 / List of item placement units
         */
        private fun dump(blocks: List<QuantityPlacement3<Block, FltX>>): List<QuantityPlacement3<Item, FltX>> {
            val placements = ArrayList<QuantityPlacement3<Item, FltX>>()
            for (block in blocks) {
                placements.addAll(block.unit.units.dump(block.position))
            }
            return placements
        }
    }

    override fun copy() = ComplexBlock(blocks.map { it.copy() })
}
