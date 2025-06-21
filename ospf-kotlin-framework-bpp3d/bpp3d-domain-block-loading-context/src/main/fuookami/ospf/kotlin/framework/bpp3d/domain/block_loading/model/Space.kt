package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.model

import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

data class Space(
    val position: Point3,
    val shape: AbstractContainer3Shape,
    val parentShape: AbstractContainer3Shape = shape,
    val block: Block? = null,
    val forwardLink: Pair<ProjectivePlane, Space>? = null
) {
    companion object {
        fun from(blocks: Bin<Block>): List<Space>? {
            return from(blocks.units)
        }

        fun from(
            blocks: List<BlockPlacement3>,
            shape: AbstractContainer3Shape = Container3Shape(),
            offset: Point3 = point3()
        ): List<Space>? {
            val absoluteBlocks = blocks.map { BlockPlacement3(it.view.copy(), it.position + offset) }
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

    val x by position::x
    val y by position::y
    val z by position::z

    val maxX get() = x + width
    val maxY get() = y + height
    val maxZ get() = z + depth

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
                                    position = position + vector3(x = block.width),
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
                                    position = position + vector3(y = block.height),
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
                                    position = position + vector3(z = block.depth),
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

    val bottomSpaces: List<Space>
        get() {
            val spaces = ArrayList<Space>()
            var forwardSpace = forwardLink
            while (forwardSpace != null) {
                if (forwardSpace.second.position.y eq Flt64.zero && forwardSpace.first != Bottom) {
                    break
                }
                if (forwardSpace.first == Bottom) {
                    spaces.add(forwardSpace.second)
                }
                forwardSpace = forwardSpace.second.forwardLink
            }
            return spaces
        }

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
            var height = (thisLayer - UInt64.one).toFlt64() * thisItem.height
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

    fun dump(): List<ItemPlacement3> {
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
