package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.math.struct.*

/**
 * 表示三维装箱问题中物品的放置方案 / Represents the placement of an item in a 3D bin packing problem
 * @property item 被放置的物品 / The item to be placed
 * @property position 物品在三维空间中的位置 / The position of the item in 3D space
 * @property rotation 物品的旋转方向，默认为无旋转 / The rotation of the item, defaults to no rotation
 */
data class Placement(
    /** 被放置的物品 / The item to be placed */
    val item: Item,
    /** 物品在三维空间中的位置 / The position of the item in 3D space */
    val position: Point3d,
    /** 物品的旋转方向 / The rotation of the item */
    val rotation: Rotation3d = Rotation3d(),
)
