/**
 * Loading order calculator.
 * 装载顺序计算器。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class LoadingOrderCalculator(
    private val maxBlockDepth: Quantity<FltX>?,
    private val sameTypeJudger: (Item, Item) -> Boolean
) {
    /**
     * 解析放置的装箱形状。
     * Resolve the packing shape of a placement.
     * @param placement the placement to resolve
     * @return the resolved packing shape
     */
    private fun resolvePackingShape(placement: QuantityPlacement3<*, FltX>): PackingShape3<FltX> {
        return placement.resolvedPackingShape()
    }

    /**
     * 判断两个放置的底部投影（footprint）是否有重叠区域。
     * Determine whether the bottom footprints of two placements overlap.
     * @param lhs the left-hand side placement
     * @param rhs the right-hand side placement
     * @return true if the footprints overlap
     */
    private fun bottomFootprintOverlapped(lhs: QuantityPlacement3<*, FltX>, rhs: QuantityPlacement3<*, FltX>): Boolean {
        val lhsShapePlacement = lhs.asShapePlacement3(::resolvePackingShape)
        val rhsShapePlacement = rhs.asShapePlacement3(::resolvePackingShape)
        val overlapArea = lhsShapePlacement.footprintOverlapArea(rhsShapePlacement)
        return (overlapArea gr (FltX.zero * overlapArea.unit)) == true
    }

    /**
     * 判断两个放置在一维轴向上的投影是否有重叠区间。
     * Determine whether two placements have overlapping intervals along a one-dimensional axis.
     * @param lhsStart the start coordinate of the left-hand side
     * @param lhsEnd the end coordinate of the left-hand side
     * @param rhsStart the start coordinate of the right-hand side
     * @param rhsEnd the end coordinate of the right-hand side
     * @return true if the intervals overlap
     */
    private fun axisOverlapped(
        lhsStart: Quantity<FltX>,
        lhsEnd: Quantity<FltX>,
        rhsStart: Quantity<FltX>,
        rhsEnd: Quantity<FltX>
    ): Boolean {
        val overlapStart = if ((lhsStart gr rhsStart) == true) {
            lhsStart
        } else {
            rhsStart
        }
        val overlapEnd = if ((lhsEnd ls rhsEnd) == true) {
            lhsEnd
        } else {
            rhsEnd
        }
        val overlap = overlapEnd - overlapStart
        return (overlap gr (FltX.zero * overlap.unit)) == true
    }

    /**
     * 判断两个放置是否在指定投影平面上重叠。
     * Determine whether two placements overlap on the specified projective plane.
     * @param lhs the left-hand side placement
     * @param rhs the right-hand side placement
     * @param plane the projective plane to check overlap on
     * @return true if the placements overlap on the given plane
     */
    private fun overlappedAt(
        lhs: QuantityPlacement3<*, FltX>,
        rhs: QuantityPlacement3<*, FltX>,
        plane: ProjectivePlane
    ): Boolean {
        if (plane == Bottom) {
            return bottomFootprintOverlapped(lhs, rhs)
        }

        val lhsShapePlacement = lhs.asShapePlacement3(::resolvePackingShape)
        val rhsShapePlacement = rhs.asShapePlacement3(::resolvePackingShape)
        return when (plane) {
            Side -> {
                axisOverlapped(lhsShapePlacement.x, lhsShapePlacement.maxX, rhsShapePlacement.x, rhsShapePlacement.maxX)
                        && axisOverlapped(lhsShapePlacement.y, lhsShapePlacement.maxY, rhsShapePlacement.y, rhsShapePlacement.maxY)
            }

            Front -> {
                axisOverlapped(lhsShapePlacement.z, lhsShapePlacement.maxZ, rhsShapePlacement.z, rhsShapePlacement.maxZ)
                        && axisOverlapped(lhsShapePlacement.y, lhsShapePlacement.maxY, rhsShapePlacement.y, rhsShapePlacement.maxY)
            }

            else -> {
                false
            }
        }
    }

    operator fun invoke(placements: List<QuantityPlacement3<*, FltX>>): List<Pair<QuantityPlacement3<Item, FltX>, UInt64>> {
        val thisPlacements = merge(placements)
            .sortedWithThreeWayComparator { lhs, rhs ->
                when (val ret = lhs.z ord rhs.z) {
                    Order.Equal -> {}

                    else -> {
                        return@sortedWithThreeWayComparator ret
                    }
                }
                when (val ret = lhs.y ord rhs.y) {
                    Order.Equal -> {}

                    else -> {
                        return@sortedWithThreeWayComparator ret
                    }
                }
                lhs.x ord rhs.x
            }
        val ret = ArrayList<Pair<QuantityPlacement3<*, FltX>, UInt64>>()
        val forwardMatrix = tidyForwardMatrix(thisPlacements)
        var visited = thisPlacements.map { _ -> false }
        var sequence = UInt64.zero
        while (!visited.all { it }) {
            val thisVisited = visited.toMutableList()
            var flag = false

            for (i in thisPlacements.indices) {
                if (visited[i]) {
                    continue
                }

                if (forwardMatrix[i].all { visited[it] }) {
                    flag = true
                    thisVisited[i] = true
                    ret.add(Pair(thisPlacements[i], sequence))
                    sequence += UInt64.one
                    break
                }
            }

            if (!flag) {
                for (i in thisPlacements.indices) {
                    if (visited[i]) {
                        continue
                    }

                    thisVisited[i] = true
                    ret.add(Pair(thisPlacements[i], sequence))
                    sequence += UInt64.one
                }
            }
            visited = thisVisited
        }
        return dump(ret)
    }

    /**
     * 将旧放置列表合并为块序列（按宽度-深度、高度-深度、宽度-高度三个方向依次尝试合并）。
     * Merge the old placement list into a block sequence by attempting merge along width-depth, height-depth, and width-height directions.
     * @param oldPlacements the original placements to merge
     * @return the merged placements
     */
    private fun merge(oldPlacements: List<QuantityPlacement3<*, FltX>>): List<QuantityPlacement3<*, FltX>> {
        return merge(
            merge(
            merge(oldPlacements)
            { lhs, rhs, placements ->
                lhs.width == rhs.width && lhs.depth == rhs.depth
                        && lhs.absoluteX eq rhs.absoluteX && lhs.absoluteZ eq rhs.absoluteZ
                        && (lhs.maxAbsoluteY eq rhs.absoluteY || placements.any { it.maxAbsoluteY eq rhs.absoluteY })
                        && isSameType(lhs.unit, rhs.unit)
            }) { lhs, rhs, placements ->
            lhs.height == rhs.height && lhs.depth == rhs.depth
                    && lhs.absoluteY eq rhs.absoluteY && lhs.absoluteZ eq rhs.absoluteZ
                    && (lhs.maxAbsoluteX eq rhs.absoluteX || placements.any { it.maxAbsoluteX eq rhs.absoluteX })
                    && isSameType(lhs.unit, rhs.unit)
        }, maxBlockDepth != null
        ) { lhs, rhs, placements ->
            lhs.width == rhs.width && lhs.height == rhs.height
                    && lhs.absoluteX eq rhs.absoluteX && lhs.absoluteY eq rhs.absoluteY
                    && (lhs.maxAbsoluteZ eq rhs.absoluteZ || placements.any { it.maxAbsoluteZ eq rhs.absoluteZ })
                    && isSameType(lhs.unit, rhs.unit)
        }
    }

    /**
     * 判断两个单元是否同类型。
     * Determine whether two units are the same type.
     *
     * 使用 Any 参数代替基础设施层通配 cuboid 类型：when-dispatch 本身即为运行时类型检查，
     * Any 等价且更通用，减少 domain 层对基础设施层几何兼容类型体系的绑定。
     * Uses Any parameter instead of the infrastructure wildcard cuboid type: when-dispatch is runtime type checking,
     * Any is equivalent and more general, reducing domain-layer binding to infrastructure geometry compatibility type hierarchy.
     * @param lhs the left-hand side unit
     * @param rhs the right-hand side unit
     * @return true if the two units are the same type
     */
    private fun isSameType(lhs: Any, rhs: Any): Boolean {
        return if (lhs is Item && rhs is Item) {
            return sameTypeJudger(lhs, rhs)
        } else if (lhs is ItemContainer<*> && rhs is ItemContainer<*>) {
            lhs.amounts.keys.all { lhsItem -> rhs.amounts.keys.all { rhsItem -> isSameType(lhsItem, rhsItem) } }
        } else if (lhs is ItemContainer<*>) {
            lhs.amounts.keys.all { isSameType(it, rhs) }
        } else if (rhs is ItemContainer<*>) {
            rhs.amounts.keys.all { isSameType(lhs, it) }
        } else {
            false
        }
    }

    /**
     * 合并可合并的放置，支持深度检查以限制块深度。
     * Merge mergeable placements, with optional depth checking to constrain block depth.
     * @param oldPlacements the original placements to merge
     * @param checkDepth whether to check the depth constraint
     * @param predicate the merge predicate
     * @return the merged placements
     */
    private fun merge(
        oldPlacements: List<QuantityPlacement3<*, FltX>>,
        checkDepth: Boolean = false,
        predicate: (QuantityPlacement3<*, FltX>, QuantityPlacement3<*, FltX>, List<QuantityPlacement3<*, FltX>>) -> Boolean
    ): List<QuantityPlacement3<*, FltX>> {
        val newPlacements = ArrayList<QuantityPlacement3<*, FltX>>()
        val visited = oldPlacements.map { false }.toMutableList()
        for (i in oldPlacements.indices) {
            if (visited[i]) {
                continue
            }

            val thisPlacements = if (checkDepth && maxBlockDepth != null) {
                var currentDepth = oldPlacements[i].depth
                if (currentDepth gr maxBlockDepth) {
                    ArrayList()
                } else {
                    val thisPlacements = ArrayList<QuantityPlacement3<*, FltX>>()
                    for (j in (i + 1) until oldPlacements.size) {
                        if (predicate(oldPlacements[i], oldPlacements[j], thisPlacements)) {
                            thisPlacements.add(oldPlacements[j])
                            visited[j] = true

                            currentDepth += oldPlacements[j].depth
                            if (currentDepth gr maxBlockDepth) {
                                break
                            }
                        }
                    }
                    thisPlacements
                }
            } else {
                val thisPlacements = ArrayList<QuantityPlacement3<*, FltX>>()
                for (j in (i + 1) until oldPlacements.size) {
                    if (predicate(oldPlacements[i], oldPlacements[j], thisPlacements)) {
                        thisPlacements.add(oldPlacements[j])
                        visited[j] = true
                    }
                }
                thisPlacements
            }

            if (thisPlacements.isEmpty()) {
                newPlacements.add(oldPlacements[i])
            } else {
                val block = mergeToBlock(oldPlacements[i], thisPlacements)
                newPlacements.add(
                    blockPlacement3Of(
                        view = block.view()!!,
                        position = oldPlacements[i].absolutePosition
                    )
                )
            }

            visited[i] = true
        }
        return newPlacements
    }

    /**
     * 将一组可合并的放置合并为一个公共块。
     * Merge a group of mergeable placements into a common block.
     * @param thisPlacement the reference placement
     * @param thisPlacements the other placements to merge into the block
     * @return the merged common block
     */
    private fun mergeToBlock(
        thisPlacement: QuantityPlacement3<*, FltX>,
        thisPlacements: MutableList<QuantityPlacement3<*, FltX>>
    ): CommonBlock {
        val items = ArrayList<QuantityPlacement3<Item, FltX>>()
        val origin = thisPlacement.absolutePosition
        thisPlacements.add(0, thisPlacement)
        for (placement in thisPlacements) {
            when (val unit = placement.unit) {
                is Item -> {
                    val offset = placement.absolutePosition - origin
                    items.add(
                        itemPlacement3Of(
                            view = placement.view as ItemView,
                            position = point3(offset.x, offset.y, offset.z)
                        )
                    )
                }

                is ItemContainer<*> -> {
                    for (item in unit.items) {
                        val offset = item.absolutePosition - origin
                        items.add(
                            itemPlacement3Of(
                                view = item.view as ItemView,
                                position = point3(offset.x, offset.y, offset.z)
                            )
                        )
                    }
                }

                else -> {}
            }
        }
        return CommonBlock(items)
    }

    /**
     * 构建向前依赖矩阵，记录每个放置之前必须装载的其它放置索引。
     * Build a forward dependency matrix recording the indices of other placements that must be loaded before each placement.
     * @param placements the list of placements
     * @return a matrix where matrix[i] contains indices of placements that precede placement i
     */
    private fun tidyForwardMatrix(placements: List<QuantityPlacement3<*, FltX>>): List<List<Int>> {
        val forwardMatrix = placements.map { _ -> ArrayList<Int>() }
        for (i in placements.indices) {
            for (j in placements.indices) {
                if (i != j && forward(placements[i], placements[j])) {
                    forwardMatrix[j].add(i)
                }
            }
        }
        return forwardMatrix
    }

    /**
     * 判断 lhs 是否应该在 rhs 之前装载（基于底面、侧面和正面三个投影平面的叠放关系）。
     * Determine whether lhs should be loaded before rhs based on overlapping in bottom, side, and front projective planes.
     * @param lhs the left-hand side placement
     * @param rhs the right-hand side placement
     * @return true if lhs should precede rhs
     */
    private fun forward(lhs: QuantityPlacement3<*, FltX>, rhs: QuantityPlacement3<*, FltX>): Boolean {
        val planes = listOf(Bottom, Side, Front)
        for (plane in planes) {
            if (forwardAt(lhs, rhs, plane)) {
                return true
            }
            if (forwardAt(rhs, lhs, plane)) {
                return false
            }
        }
        return false
    }

    private fun <P : ProjectivePlane> forwardAt(lhs: QuantityPlacement3<*, FltX>, rhs: QuantityPlacement3<*, FltX>, plane: P): Boolean {
        val overlapped = overlappedAt(lhs, rhs, plane)
        return if (!overlapped) {
            false
        } else {
            plane.distance(lhs.position) leq plane.distance(rhs.position)
        }
    }
    /**
     * 将合并后的放置序列展平为单个物品级别的放置列表。
     * Flatten the merged placement sequence into a list of individual item-level placements.
     * @param placements the merged placements with sequence numbers
     * @return the flattened item-level placements with sequence numbers
     */
    private fun dump(placements: List<Pair<QuantityPlacement3<*, FltX>, UInt64>>): List<Pair<QuantityPlacement3<Item, FltX>, UInt64>> {
        val ret = ArrayList<Pair<QuantityPlacement3<Item, FltX>, UInt64>>()
        for ((placement, sequence) in placements) {
            when (val unit = placement.unit) {
                is Item -> {
                    val itemPlacement = placement.toItemPlacementOrNull()
                    if (itemPlacement != null) {
                        ret.add(Pair(itemPlacement, sequence))
                    }
                }

                is Block -> {
                    for (item in unit.units) {
                        ret.add(
                            Pair(
                                itemPlacement3Of(
                                    view = item.view as ItemView,
                                    position = item.absolutePosition
                                ),
                                sequence
                            )
                        )
                    }
                }

                else -> {}
            }
        }
        return ret
    }
}
