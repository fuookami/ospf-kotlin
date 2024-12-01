package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class LoadingOrderCalculator(
    private val maxBlockDepth: Flt64?,
    private val sameTypeJudger: (Item, Item) -> Boolean
) {
    operator fun invoke(placements: List<Placement3<*>>): List<Pair<ItemPlacement3, UInt64>> {
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
        val ret = ArrayList<Pair<Placement3<*>, UInt64>>()
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

    private fun merge(oldPlacements: List<Placement3<*>>): List<Placement3<*>> {
        return merge(merge(merge(oldPlacements)
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

    private fun isSameType(lhs: AbstractCuboid, rhs: AbstractCuboid): Boolean {
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

    private fun merge(
        oldPlacements: List<Placement3<*>>,
        checkDepth: Boolean = false,
        predicate: (Placement3<*>, Placement3<*>, List<Placement3<*>>) -> Boolean
    ): List<Placement3<*>> {
        val newPlacements = ArrayList<Placement3<*>>()
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
                    val thisPlacements = ArrayList<Placement3<*>>()
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
                val thisPlacements = ArrayList<Placement3<*>>()
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
                newPlacements.add(Placement3(block.view()!!, oldPlacements[i].absolutePosition))
            }

            visited[i] = true
        }
        return newPlacements
    }

    private fun mergeToBlock(
        thisPlacement: Placement3<*>,
        thisPlacements: MutableList<Placement3<*>>
    ): CommonBlock {
        val items = ArrayList<ItemPlacement3>()
        val origin = thisPlacement.absolutePosition
        thisPlacements.add(0, thisPlacement)
        for (placement in thisPlacements) {
            when (val unit = placement.unit) {
                is Item -> {
                    items.add(Placement3(placement.view as ItemView, placement.absolutePosition - origin))
                }

                is ItemContainer<*> -> {
                    for (item in unit.items) {
                        items.add(Placement3(item.view, item.absolutePosition - origin))
                    }
                }

                else -> {}
            }
        }
        return CommonBlock(items)
    }

    private fun tidyForwardMatrix(placements: List<Placement3<*>>): List<List<Int>> {
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

    private fun forward(lhs: Placement3<*>, rhs: Placement3<*>): Boolean {
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

    private fun <P : ProjectivePlane> forwardAt(lhs: Placement3<*>, rhs: Placement3<*>, plane: P): Boolean {
        val lhsProjection = Placement2(lhs, plane)
        val rhsProjection = Placement2(rhs, plane)
        return if (!lhsProjection.overlapped(rhsProjection)) {
            false
        } else {
            plane.distance(lhs.position) leq plane.distance(rhs.position)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dump(placements: List<Pair<Placement3<*>, UInt64>>): List<Pair<ItemPlacement3, UInt64>> {
        val ret = ArrayList<Pair<ItemPlacement3, UInt64>>()
        for ((placement, sequence) in placements) {
            when (val unit = placement.unit) {
                is Item -> {
                    ret.add(Pair(placement as ItemPlacement3, sequence))
                }

                is Block -> {
                    for (item in unit.units) {
                        ret.add(Pair(Placement3(item.view, item.absolutePosition), sequence))
                    }
                }

                else -> {}
            }
        }
        return ret
    }
}
