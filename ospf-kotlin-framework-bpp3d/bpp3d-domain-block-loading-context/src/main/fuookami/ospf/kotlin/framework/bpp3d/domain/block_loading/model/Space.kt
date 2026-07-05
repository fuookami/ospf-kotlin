/**
 * 空间模型。
 * Space model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.model

import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Vector
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

private fun infraPoint3(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    z: FltX = FltX.zero
): Point<Dim3, FltX> {
    return Point(x, y, z)
}

private fun infraVector3(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    z: FltX = FltX.zero
): Vector<Dim3, FltX> {
    return Vector(x, y, z)
}

/**
 * 空间模型，表示容器中的一个可放置区域。
 * Space model, representing a placeable region within a container.
 * @property parentShape 父空间形状，默认为当前形状
 * @property parentShape the parent space shape, defaults to the current shape
 * @property block 放置在该空间中的箱子，为空表示空闲空间
 * @property block the block placed in this space, null indicates free space
 * @property forwardLink 前向关联，指向分割后的下一个空间
 * @property forwardLink the forward link to the next space after splitting
 */
data class Space(
    val position: Point<Dim3, FltX>,
    val shape: AbstractContainer3Shape,
    val parentShape: AbstractContainer3Shape = shape,
    val block: Block? = null,
    val forwardLink: Pair<ProjectivePlane, Space>? = null
) {
    companion object {
        fun from(blocks: Bin<Block, FltX>): List<Space>? {
            return from(blocks.units)
        }

        fun from(
            blocks: List<QuantityPlacement3<Block, FltX>>,
            shape: AbstractContainer3Shape = Container3Shape(),
            offset: Point<Dim3, FltX> = infraPoint3()
        ): List<Space>? {
            val absoluteBlocks = blocks.map { QuantityPlacement3<Block, FltX>(it.view.copy(), it.position + offset) }
            val spaces = ArrayList<Space>()
            val stack = arrayListOf(Space(position = offset, shape = shape))
            while (stack.isNotEmpty()) {
                if (memoryUseOver()) {
                    System.gc()
                }
                val top = stack.removeAt(stack.lastIndex)
                val block = absoluteBlocks.find { it.position == top.position } ?: continue
                val space = top.put(block.unit) ?: continue
                spaces.add(space)
                stack.addAll(space.links.map { it.second })
            }
            System.gc()
            return if (spaces.size == absoluteBlocks.size) {
                spaces
            } else {
                null
            }
        }
    }

    val complexity: UInt64 by lazy {
        forwardLink?.let { it.second.complexity + UInt64.one } ?: UInt64.zero
    }

    val width get() = block?.width ?: shape.width
    val height get() = block?.height ?: shape.height
    val depth get() = block?.depth ?: shape.depth

    val x get() = position[0]
    val y get() = position[1]
    val z get() = position[2]

    val maxX get() = x + width
    val maxY get() = y + height
    val maxZ get() = z + depth

    /**
     * 当前空间放置箱子后产生的关联空间列表。
     * The list of linked spaces generated after placing a block in the current space.
     */
    val links: List<Pair<ProjectivePlane, Space>>
        get() {
            return if (block != null) {
                when (block) {
                    is ComplexBlock -> {
                        val spaces = from(
                            blocks = block.blocks,
                            shape = parentShape,
                            offset = position
                        ) ?: emptyList()
                        spaces
                            .flatMap { it.links }
                            .filter { link -> block.blocks.all { block -> (block.position + position) != link.second.position } }
                    }

                    else -> {
                        val links = ArrayList<Space>()
                        if (parentShape.width gr block.width) {
                            links.add(
                                Space(
                                    position = position + infraVector3(x = block.width.value),
                                    shape = Container3Shape(
                                        width = parentShape.width - block.width,
                                        height = parentShape.height,
                                        depth = block.depth
                                    ),
                                    forwardLink = Pair(Front, this)
                                )
                            )
                        }
                        if (block.topFlat && parentShape.height gr block.height) {
                            links.add(
                                Space(
                                    position = position + infraVector3(y = block.height.value),
                                    shape = Container3Shape(
                                        width = block.width,
                                        height = parentShape.height - block.height,
                                        depth = block.depth
                                    ),
                                    forwardLink = Pair(Bottom, this)
                                )
                            )
                        }
                        if (parentShape.depth gr block.depth) {
                            links.add(
                                Space(
                                    position = position + infraVector3(z = block.depth.value),
                                    shape = Container3Shape(
                                        width = parentShape.width,
                                        height = parentShape.height,
                                        depth = parentShape.depth - block.depth,
                                    ),
                                    forwardLink = Pair(Side, this)
                                )
                            )
                        }
                        links.map { Pair(it.forwardLink!!.first, it) }
                    }
                }
            } else {
                emptyList()
            }
        }

    /**
     * 获取底部方向关联的空间列表。
     * Get the list of spaces linked in the bottom direction.
     */
    val bottomSpaces: List<Space>
        get() {
            val spaces = ArrayList<Space>()
            var forwardSpace = forwardLink
            while (forwardSpace != null) {
                if (forwardSpace.second.position[1] eq FltX.zero && forwardSpace.first != Bottom) {
                    break
                }
                if (forwardSpace.first == Bottom) {
                    spaces.add(forwardSpace.second)
                }
                forwardSpace = forwardSpace.second.forwardLink
            }
            return spaces
        }

    /**
     * 将箱子放入该空间。
     * Put a block into this space.
     * @param block 要放入的箱子
     * @param block the block to put
     * @return 放置后的新空间，如果无法放置则返回 null
     * @return the new space after placement, or null if placement is not possible
     */
    fun put(block: Block): Space? {
        val thisBottomSpaces = bottomSpaces

        if (!shape.enabled(block)) {
            return null
        }

        for (unit in block.units) {
            if (!unit.unit.enabledOrientationsAt(shape.restSpace(unit.position)).contains(unit.orientation)) {
                return null
            }
        }

        if (thisBottomSpaces.isNotEmpty()) {
            val (thisItem, thisLayer) = when (block) {
                is SimpleBlock -> {
                    Pair(block.itemView, block.layer)
                }

                is HollowSquareBlock -> {
                    Pair(block.itemView, block.layer)
                }

                is LayeredBlock -> {
                    Pair(block.bottomItemView, block.bottomLayer)
                }

                else -> return null
            }
            val bottomItem = when (val bottomBlock = thisBottomSpaces.last().block!!) {
                is SimpleBlock -> {
                    bottomBlock.itemView
                }

                is HollowSquareBlock -> {
                    bottomBlock.itemView
                }

                is LayeredBlock -> {
                    bottomBlock.topItemView
                }

                else -> {
                    return null
                }
            }
            var layer = thisLayer - UInt64.one
            var height = FltX((thisLayer - UInt64.one).toULong().toDouble()) * thisItem.height
            for (space in thisBottomSpaces) {
                when (val bottomBlock = space.block!!) {
                    is SimpleBlock -> {
                        if (thisItem.packageType == bottomBlock.packageType
                            && thisItem.orientation.category == bottomBlock.itemOrientation.category
                        ) {
                            layer += bottomBlock.layer
                            height += bottomBlock.height
                        }
                    }

                    is HollowSquareBlock -> {
                        if (thisItem.packageType == bottomBlock.packageType
                            && thisItem.orientation.category == bottomBlock.itemOrientation.category
                        ) {
                            layer += bottomBlock.layer
                            height += bottomBlock.height
                        }
                    }

                    is LayeredBlock -> {
                        for (thisBottomBlock in bottomBlock.blocks.reversed()) {
                            if (thisItem.packageType == thisBottomBlock.packageType
                                && thisItem.orientation.category == thisBottomBlock.itemOrientation.category
                            ) {
                                layer += thisBottomBlock.layer
                                height += thisBottomBlock.height
                            } else {
                                break
                            }
                        }
                    }

                    else -> {
                        return null
                    }
                }
            }
            if (!thisItem.enabledStackingOn(
                    bottomItem = bottomItem,
                    layer = layer,
                    height = height,
                    space = this.shape
                )
            ) {
                return null
            }
        }

        return Space(
            position = position,
            shape = block.shape,
            parentShape = this.shape,
            block = block,
            forwardLink = this.forwardLink
        )
    }

    /**
     * 转储空间中的物品列表。
     * Dump the items in the space.
     * @return 物品放置列表，如果空间为空则返回空列表
     * @return the list of item placements, or an empty list if the space is empty
     */
    fun dump(): List<QuantityPlacement3<Item, FltX>> {
        return block?.dump() ?: emptyList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Space) return false

        if (position != other.position) return false
        if (shape != other.shape) return false
        if (parentShape != other.parentShape) return false
        if (block != other.block) return false
        if (forwardLink != other.forwardLink) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + parentShape.hashCode()
        result = 31 * result + (block?.hashCode() ?: 0)
        result = 31 * result + (forwardLink?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return if (block != null) {
            "$position -> $shape: $block"
        } else {
            "$position -> $shape"
        }
    }
}
