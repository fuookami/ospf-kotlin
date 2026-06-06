@file:Suppress("DEPRECATION")

/**
 * 圆柱形状契约。
 * Cylinder shape contract.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.OrientationCategory
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShape3

/**
 * 圆柱能力路径状态。
 * Cylinder capability path status.
 */
enum class CylinderCapabilityStatus {
    /** 仅长方体路径。 / Cuboid-only path. */
    CuboidOnly,

    /** 默认候选生成路径，仅支持竖直圆柱。 / Default candidate path, vertical-cylinder-only. */
    VerticalCandidateOnly,

    /** 支撑语义路径，仅支持直立竖直圆柱。 / Support semantics path, upright vertical-cylinder-only. */
    UprightVerticalSupportOnly,

    /** 已知坐标终态校验路径。 / Known-coordinate final validation path. */
    KnownCoordinateFinalValidation,

    /** 后验深度边界校验路径。 / Post-solve depth-boundary validation path. */
    DepthBoundaryFinalValidation
}

/**
 * 圆柱能力路径。
 * Cylinder capability path.
 *
 * @property source 调用来源 / call source
 * @property status 能力状态 / capability status
 * @property pathPredicate 仅长方体路径描述 / cuboid-only path predicate
 */
enum class CylinderCapabilityPath(
    val source: String,
    val status: CylinderCapabilityStatus,
    val pathPredicate: String? = null
) {
    DefaultLayerCandidate(
        source = "LayerGeneration.defaultCandidate",
        status = CylinderCapabilityStatus.VerticalCandidateOnly
    ),
    CirclePackingCandidate(
        source = "CirclePackingLayerGenerator",
        status = CylinderCapabilityStatus.VerticalCandidateOnly
    ),
    ApplicationLayerPlacementCandidate(
        source = "LayerPlacementAdapter.toLayerPlacement",
        status = CylinderCapabilityStatus.VerticalCandidateOnly
    ),
    PileSupportCandidate(
        source = "PileLayerGenerator",
        status = CylinderCapabilityStatus.UprightVerticalSupportOnly
    ),
    PackageAttributeSupport(
        source = "PackageAttribute.supportPackingShape",
        status = CylinderCapabilityStatus.UprightVerticalSupportOnly
    ),
    SimpleBlockCandidate(
        source = "SimpleBlockGenerator",
        status = CylinderCapabilityStatus.VerticalCandidateOnly
    ),
    DfsMlhsCuboidSearch(
        source = "DFS/MLHS",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "DFS/MLHS space-splitting path is"
    ),
    ItemMerge(
        source = "ItemMerger.merge",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergePiles(
        source = "ItemMerger.mergePiles",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergeBlocks(
        source = "ItemMerger.mergeBlocks",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergePatternBlocks(
        source = "ItemMerger.mergePatternBlocks",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    ItemMergeHollowSquareBlocks(
        source = "ItemMerger.mergeHollowSquareBlocks",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "item merge paths are"
    ),
    PatternPlacement(
        source = "Pattern",
        status = CylinderCapabilityStatus.CuboidOnly,
        pathPredicate = "pattern placement paths are"
    ),
    KnownCoordinateFinalPacking(
        source = "known-coordinate final packing",
        status = CylinderCapabilityStatus.KnownCoordinateFinalValidation
    ),
    RendererFinalPacking(
        source = "renderer final packing",
        status = CylinderCapabilityStatus.KnownCoordinateFinalValidation
    ),
    DepthBoundaryFinalValidation(
        source = "DepthBoundaryLayerOrientationPolicy",
        status = CylinderCapabilityStatus.DepthBoundaryFinalValidation
    )
}

/**
 * 横向圆柱轴向未开放错误信息。
 * Unsupported horizontal cylinder axis message.
 *
 * @param source 调用来源 / call source
 * @param axis 实际轴向 / actual axis
 * @return 错误信息 / error message
 */
fun unsupportedCylinderAxisMessage(source: String, axis: Axis3): String {
    return "Unsupported cylinder axis in $source: only Axis3.Y is allowed, but got $axis."
}

/**
 * 圆柱非直立朝向未开放错误信息。
 * Unsupported non-upright cylinder orientation message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedCylinderOrientationMessage(source: String): String {
    return "Unsupported cylinder orientation in $source: only upright orientations are allowed."
}

/**
 * 圆柱侧放/卧放堆叠层策略未开放错误信息。
 * Unsupported side/lie cylinder top-layer policy message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedCylinderTopLayerPolicyMessage(source: String): String {
    return "Unsupported cylinder top-layer policy in $source: side/lie stacking is not allowed."
}

/**
 * 圆柱堆叠/悬挂支撑未开放错误信息。
 * Unsupported cylinder stacking/hanging support message.
 *
 * @param source 调用来源 / call source
 * @return 错误信息 / error message
 */
fun unsupportedCylinderStackingSupportMessage(source: String): String {
    return "Unsupported cylinder stacking and hanging support in $source: only upright Axis3.Y items are allowed."
}

/**
 * 圆柱进入仅长方体路径未开放错误信息。
 * Unsupported cylinder message for cuboid-only paths.
 *
 * @param source 调用来源 / call source
 * @param pathPredicate 路径谓词描述 / path predicate description
 * @return 错误信息 / error message
 */
fun unsupportedCylinderCuboidOnlyPathMessage(source: String, pathPredicate: String): String {
    return "Unsupported cylinder in $source: $pathPredicate cuboid-only and does not provide verified cylinder geometry yet."
}

/**
 * 判断物品集合是否包含圆柱。
 * Return whether the item collection contains a cylinder.
 *
 * @param items 待检查物品 / items to check
 * @return 是否包含圆柱 / whether a cylinder exists
 */
fun hasCylinderItem(items: Iterable<Item>): Boolean {
    return items.any { item ->
        item.packingShape is CylinderPackingShape3
    }
}

/**
 * 要求圆柱为当前默认候选路径支持的竖直轴向。
 * Require the cylinder axis supported by current default candidate paths.
 *
 * @param shape 装箱形状 / packing shape
 * @param source 调用来源 / call source
 */
fun requireVerticalCylinderAxis(
    shape: PackingShape3<InfraNumber>,
    source: String
) {
    if (shape is CylinderPackingShape3 && shape.axis != Axis3.Y) {
        throw IllegalArgumentException(unsupportedCylinderAxisMessage(source = source, axis = shape.axis))
    }
}

/**
 * 要求圆柱为当前默认候选路径支持的竖直轴向。
 * Require the cylinder axis supported by current default candidate paths.
 *
 * @param shape 装箱形状 / packing shape
 * @param path 能力路径 / capability path
 */
fun requireVerticalCylinderAxis(
    shape: PackingShape3<InfraNumber>,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.VerticalCandidateOnly) {
        "Cylinder capability path ${path.name} is not a vertical candidate path."
    }
    requireVerticalCylinderAxis(
        shape = shape,
        source = path.source
    )
}

/**
 * 要求圆柱支撑语义只使用直立 Y 轴圆柱。
 * Require cylinder support semantics to use upright Y-axis cylinders only.
 *
 * @param shape 装箱形状 / packing shape
 * @param orientation 物品朝向 / item orientation
 * @param source 调用来源 / call source
 */
fun requireUprightVerticalCylinderSupport(
    shape: PackingShape3<InfraNumber>,
    orientation: Orientation,
    source: String
) {
    if (shape is CylinderPackingShape3 && (shape.axis != Axis3.Y || orientation != Orientation.Upright)) {
        throw IllegalArgumentException(unsupportedCylinderStackingSupportMessage(source))
    }
}

/**
 * 要求圆柱支撑语义只使用直立 Y 轴圆柱。
 * Require cylinder support semantics to use upright Y-axis cylinders only.
 *
 * @param shape 装箱形状 / packing shape
 * @param orientation 物品朝向 / item orientation
 * @param path 能力路径 / capability path
 */
fun requireUprightVerticalCylinderSupport(
    shape: PackingShape3<InfraNumber>,
    orientation: Orientation,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.UprightVerticalSupportOnly) {
        "Cylinder capability path ${path.name} is not an upright vertical support path."
    }
    requireUprightVerticalCylinderSupport(
        shape = shape,
        orientation = orientation,
        source = path.source
    )
}

/**
 * 要求简单块生成只接收当前已验证的圆柱能力。
 * Require simple block generation to accept only currently verified cylinder capability.
 *
 * @param item 物品 / item
 * @param source 调用来源 / call source
 */
fun requireSupportedCylinderItemForSimpleBlock(item: Item, source: String) {
    val shape = item.packingShape
    if (shape !is CylinderPackingShape3) {
        return
    }
    requireVerticalCylinderAxis(shape = shape, source = source)
    val unsupportedOrientations = item.enabledOrientations.filter { it.category != OrientationCategory.Upright }
    if (unsupportedOrientations.isNotEmpty()) {
        throw IllegalArgumentException(unsupportedCylinderOrientationMessage(source))
    }
    if (item.enabledSideOnTop || item.enabledLieOnTop) {
        throw IllegalArgumentException(unsupportedCylinderTopLayerPolicyMessage(source))
    }
}

/**
 * 要求简单块生成只接收当前已验证的圆柱能力。
 * Require simple block generation to accept only currently verified cylinder capability.
 *
 * @param item 物品 / item
 * @param path 能力路径 / capability path
 */
fun requireSupportedCylinderItemForSimpleBlock(item: Item, path: CylinderCapabilityPath) {
    require(path == CylinderCapabilityPath.SimpleBlockCandidate) {
        "Cylinder capability path ${path.name} is not the simple block candidate path."
    }
    requireSupportedCylinderItemForSimpleBlock(
        item = item,
        source = path.source
    )
}

/**
 * 要求仅长方体路径不接收圆柱物品。
 * Require cuboid-only paths to reject cylinder items.
 *
 * @param items 待检查物品 / items to check
 * @param source 调用来源 / call source
 * @param pathPredicate 路径谓词描述 / path predicate description
 */
fun requireNoCylinderItemsForCuboidOnlyPath(
    items: Iterable<Item>,
    source: String,
    pathPredicate: String
) {
    if (hasCylinderItem(items)) {
        throw IllegalArgumentException(
            unsupportedCylinderCuboidOnlyPathMessage(
                source = source,
                pathPredicate = pathPredicate
            )
        )
    }
}

/**
 * 要求仅长方体路径不接收圆柱物品。
 * Require cuboid-only paths to reject cylinder items.
 *
 * @param items 待检查物品 / items to check
 * @param path 能力路径 / capability path
 */
fun requireNoCylinderItemsForCuboidOnlyPath(
    items: Iterable<Item>,
    path: CylinderCapabilityPath
) {
    require(path.status == CylinderCapabilityStatus.CuboidOnly && path.pathPredicate != null) {
        "Cylinder capability path ${path.name} is not a cuboid-only path."
    }
    requireNoCylinderItemsForCuboidOnlyPath(
        items = items,
        source = path.source,
        pathPredicate = path.pathPredicate
    )
}
