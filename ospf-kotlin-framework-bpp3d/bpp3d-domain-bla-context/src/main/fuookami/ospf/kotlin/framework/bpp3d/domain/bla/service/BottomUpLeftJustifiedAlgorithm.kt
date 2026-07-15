/**
 * Bottom-up left-justified algorithm.
 * 自底向上左对齐算法。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.bla.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.error.Bpp3dValidationError
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

/**
 * Compare the position priority of two 2D points.
 * 比较两个二维坐标点的位置优先级。
 *
 * @param lhs The left-hand side point.
 * @param rhs The right-hand side point.
 * @param withManhattanDistance Whether to prioritize sorting by Manhattan distance.
 * @return The comparison result.
 * 返回比较结果。
*/
private fun compareWithPosition(
    lhs: QuantityPoint2<FltX>,
    rhs: QuantityPoint2<FltX>,
    withManhattanDistance: Boolean = true
): Order {
    if (withManhattanDistance) {
        val lhsManhattanDistance = lhs.x + lhs.y
        val rhsManhattanDistance = rhs.x + rhs.y
        when (val value = lhsManhattanDistance ord rhsManhattanDistance) {
            Order.Equal -> {}
            else -> {
                return value
            }
        }
    }
    when (val value = lhs.y ord rhs.y) {
        Order.Equal -> {}

        else -> {
            return value
        }
    }
    return lhs.x ord rhs.x
}

/**
 * Compare two projections by shape and weight for placement priority.
 * 按形状和重量比较两个投影的放置优先级。
 *
 * @param lhs The left-hand side projection.
 * @param rhs The right-hand side projection.
 * @return The comparison result.
 * 返回比较结果。
*/
fun <P : ProjectivePlane> compareWithShapeAndWeight(
    lhs: Projection<*, FltX, P>,
    rhs: Projection<*, FltX, P>
): Order {
    if (lhs.bottomOnly && !rhs.bottomOnly) {
        return Order.Less()
    } else if (!lhs.bottomOnly && rhs.bottomOnly) {
        return Order.Greater()
    }
    when (val value = rhs.length ord lhs.length) {
        Order.Equal -> {}
        else -> {
            return value
        }
    }
    when (val value = rhs.width ord lhs.width) {
        Order.Equal -> {}
        else -> {
            return value
        }
    }
    when (val value = rhs.height ord lhs.height) {
        Order.Equal -> {}
        else -> {
            return value
        }
    }
    when (val value = rhs.weight ord lhs.weight) {
        Order.Equal -> {}
        else -> {
            return value
        }
    }
    return Order.Equal
}

/**
 * Bottom-up left-justified algorithm for solving bin packing on a 2D projection plane.
 * 自底向上左对齐算法，在二维投影平面上求解装箱问题。
 *
 * @property space The 2D shape of the container.
 * @property plane The projection plane.
 * @property config The algorithm configuration.
*/
class BottomUpLeftJustifiedAlgorithm<P : ProjectivePlane>(
    private val space: Container2Shape<P>,
    private val plane: P,
    private val config: Config<P> = Config()
) {
    private val logger = logger()

    constructor(length: FltX, width: FltX, plane: P, config: Config<P> = Config()) : this(
        space = Container2Shape(length * Meter, width * Meter, plane),
        plane = plane,
        config = config
    )

    /**
     * Configuration for the bottom-up left-justified algorithm.
     * 自底向上左对齐算法的配置。
     *
     * @property withDisplacementX Whether to enable displacement search along the X axis.
     * @property withDisplacementY Whether to enable displacement search along the Y axis.
     * @property comparator The three-way comparator for projections, determining placement priority.
     * @property positionComparator The three-way comparator for positions, determining candidate point priority.
    */
    data class Config<P : ProjectivePlane>(
        val withDisplacementX: Boolean = true,
        val withDisplacementY: Boolean = true,
        val comparator: ThreeWayComparator<Projection<*, FltX, P>> = ::compareWithShapeAndWeight,
        val positionComparator: ThreeWayComparator<QuantityPoint2<FltX>> = { lhs, rhs -> compareWithPosition(lhs, rhs) }
    ) {
        val withDisplacement = withDisplacementX || withDisplacementY

        companion object {
            fun <P : ProjectivePlane> invoke(
                withDisplacementX: Boolean? = null,
                withDisplacementY: Boolean? = null,
                withDisplacement: Boolean? = null,
                withManhattanDistance: Boolean? = null,
                comparator: ThreeWayComparator<Projection<*, FltX, P>>? = null,
                positionComparator: ThreeWayComparator<QuantityPoint2<FltX>>? = null
            ): Config<P> {
                return Config(
                    withDisplacementX = withDisplacementX ?: withDisplacement ?: true,
                    withDisplacementY = withDisplacementY ?: withDisplacement ?: true,
                    comparator = comparator ?: ::compareWithShapeAndWeight,
                    positionComparator = positionComparator ?: { lhs, rhs -> compareWithPosition(lhs, rhs, withManhattanDistance ?: true) }
                )
            }
        }
    }

    operator fun invoke(
        originProjections: List<Projection<*, FltX, P>>,
        scope: CoroutineScope = bpp3dBlaAsyncScope
    ): ChannelGuard<List<QuantityPlacement2<*, FltX, P>?>> {
        val projections = config.comparator.let { originProjections.sortedWithThreeWayComparator { lhs, rhs -> it(lhs, rhs) } }

        val promise = Channel<List<QuantityPlacement2<*, FltX, P>?>>()
        scope.launch(Dispatchers.Default) {
            val placements = projections.indices.map { null }.toMutableList<QuantityPlacement2<*, FltX, P>?>()
            try {
                bla(
                    promise = promise,
                    placements = placements,
                    projections = projections
                )
            } catch (e: CancellationException) {
                logger.trace { "BLA was stopped by controller." }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.debug { "BLA Error ${e.message}" }
            } finally {
                promise.close()
            }
        }
        return ChannelGuard(promise)
    }

    /**
     * 执行自底向上左对齐算法的核心回溯逻辑。
     * Core backtracking logic of the bottom-up left-justified algorithm.
     *
     * @param promise 用于发送找到的放置方案的通道
     * @param placements 当前放置方案的可变列表
     * @param projections 待放置的投影列表（已排序）
    */
    private suspend fun bla(
        promise: Channel<List<QuantityPlacement2<*, FltX, P>?>>,
        placements: MutableList<QuantityPlacement2<*, FltX, P>?>,
        projections: List<Projection<*, FltX, P>>
    ) {
        if (projections.isEmpty()) {
            return
        }

        val stack = ArrayList<Pair<Int, QuantityPlacement2<*, FltX, P>?>>()
        stack.add(Pair(0, null))
        coroutineScope {
            val promises = ArrayList<Deferred<QuantityPlacement2<*, FltX, P>?>>()
            val thisFeasiblePoints = feasiblePoints(placements = placements, reverse = true)
            for (feasiblePoint in thisFeasiblePoints) {
                promises.add(async(Dispatchers.Default) {
                    val placement = placement2Of(
                        projection = projections[0],
                        position = feasiblePoint
                    )
                    if (feasible(placement, placements).value!!) {
                        placement
                    } else {
                        null
                    }
                })
            }
            stack.addAll(promises.mapNotNull { it.await()?.let { placement -> Pair(0, placement) } })
        }

        var lastImpossible = false
        var lastFeasiblePoints: List<QuantityPoint2<FltX>> = emptyList()
        while (stack.isNotEmpty()) {
            val top = stack.removeAt(stack.lastIndex)
            placements[top.first] = top.second
            val i = top.first + 1
            if (top.first == projections.lastIndex) {
                lastImpossible = false
                lastFeasiblePoints = emptyList()
                if (promise.trySend(placements.toList()).isFailure) {
                    return
                }
            } else {
                stack.add(Pair(i, null))
                if (lastImpossible
                    && projections[i].view == projections[i - 1].view
                ) {
                    continue
                }

                val thisFeasiblePoints = if (projections[i].view == projections[i - 1].view && lastFeasiblePoints.isNotEmpty() && placements[i - 1] != null) {
                    val lastPlacement = placements[i - 1]!!
                    // 要去除的点：
                    // 1. 和上一个放置的物料在相同位置的点
                    // 2. 被上一个放置的物料包住的点
                    val lastAvailablePoints = lastFeasiblePoints.filter {
                        it != lastPlacement.position                                                                // 和上一个放置的物料在相同位置的点
                                && lastPlacement.contains(it, withUpperBound = false).value != true                 // 被上一个放置的物料包住的点（包括三个坐标轴上距离原点较小的平面）
                    }
                    (lastAvailablePoints + feasiblePoints(
                        targetPlacements = listOf(lastPlacement),
                        fixedPlacements = placements,
                        reverse = true
                    )).sortedWithThreeWayComparator { lhs, rhs -> (!config.positionComparator)(lhs, rhs) }
                } else {
                    feasiblePoints(placements = placements, reverse = true)
                }
                coroutineScope {
                    val promises = ArrayList<Deferred<QuantityPlacement2<*, FltX, P>?>>()
                    for (feasiblePoint in thisFeasiblePoints) {
                        promises.add(async(Dispatchers.Default) {
                            val placement = placement2Of(
                                projection = projections[i],
                                position = feasiblePoint
                            )
                            if (feasible(placement, placements).value!!) {
                                placement
                            } else {
                                null
                            }
                        })
                    }
                    val newPlacements = promises.mapNotNull { it.await() }
                    lastImpossible = newPlacements.isEmpty()
                    lastFeasiblePoints = newPlacements.map { it.position }.toSet().toList()
                    stack.addAll(newPlacements.map { Pair(i, it) })
                }
            }
        }
    }

    /**
     * 根据已放置的物料计算所有可行放置点（目标放置与固定放置相同）。
     * Compute all feasible placement points where target and fixed placements are the same.
     *
     * @param placements 已放置的物料列表
     * @param reverse 是否按逆序（从远到近）排列结果
     * @return 可行放置点列表
    */
    private suspend fun feasiblePoints(
        placements: List<QuantityPlacement2<*, FltX, P>?>,
        reverse: Boolean = true
    ): List<QuantityPoint2<FltX>> {
        return feasiblePoints(
            targetPlacements = placements,
            fixedPlacements = placements,
            reverse = reverse
        )
    }

    /**
     * 根据已固定的放置计算所有可行放置点。
     * Compute all feasible placement points based on fixed placements.
     *
     * @param targetPlacements 用于生成候选点的放置列表
     * @param fixedPlacements 用于判断重叠和包含关系的已固定放置列表
     * @param reverse 是否按逆序（从远到近）排列结果
     * @return 可行放置点列表
    */
    private suspend fun feasiblePoints(
        targetPlacements: List<QuantityPlacement2<*, FltX, P>?>,
        fixedPlacements: List<QuantityPlacement2<*, FltX, P>?>,
        reverse: Boolean = true
    ): List<QuantityPoint2<FltX>> {
        val ret = HashSet<QuantityPoint2<FltX>>()
        for (placement in targetPlacements) {
            if (placement != null) {
                val thisPoints = HashSet<QuantityPoint2<FltX>>()

                val upperLeftPoint = point2(x = placement.x, y = placement.maxY)
                val bottomRightPoint = point2(x = placement.maxX, y = placement.y)

                thisPoints.add(upperLeftPoint)
                thisPoints.add(bottomRightPoint)

                if (config.withDisplacement) {
                    thisPoints.addAll(coroutineScope {
                        val promises = thisPoints.map {
                            async(Dispatchers.Default) {
                                return@async actualFeasiblePoints(
                                    point = it,
                                    placement = placement,
                                    fixedPlacements = fixedPlacements
                                )
                            }
                        }
                        promises.awaitAll().flatten()
                    })
                }

                thisPoints.removeAll {
                    fixedPlacements.any { fixedPlacement ->
                        fixedPlacement?.contains(it, withUpperBound = false)?.value ?: false
                    }
                }
                ret.addAll(thisPoints)
            }
        }
        ret.removeAll {
            fixedPlacements.any { fixedPlacement ->
                fixedPlacement?.contains(it, withUpperBound = false)?.value ?: false
            }
        }
        if (ret.isEmpty()) {
            ret.add(point2FltX())
        }
        return if (reverse) {
            ret.sortedWithThreeWayComparator { lhs, rhs -> (!config.positionComparator)(lhs, rhs) }
        } else {
            ret.sortedWithThreeWayComparator { lhs, rhs -> config.positionComparator(lhs, rhs) }
        }
    }

    /**
     * 从候选点出发，沿 X 和 Y 轴方向递归搜索所有实际可行点。
     * Recursively search all actual feasible points from a candidate point along X and Y axes.
     *
     * @param point 起始候选点
     * @param placement 当前放置
     * @param fixedPlacements 已固定的放置列表
     * @return 所有可达的实际可行点列表
    */
    private fun actualFeasiblePoints(
        point: QuantityPoint2<FltX>,
        placement: QuantityPlacement2<*, FltX, *>,
        fixedPlacements: List<QuantityPlacement2<*, FltX, *>?>
    ): List<QuantityPoint2<FltX>> {
        val stack = arrayListOf(point)
        val points = ArrayList<QuantityPoint2<FltX>>()
        while (stack.isNotEmpty()) {
            val thisPoint = stack.removeAt(stack.lastIndex)
            points.add(thisPoint)

            if (config.withDisplacementX) {
                val xPoint = actualFeasiblePointOnX(
                    point = thisPoint,
                    placement = placement,
                    fixedPlacements = fixedPlacements
                )
                if (xPoint != null && xPoint != thisPoint) {
                    stack.add(xPoint)
                }
            }

            if (config.withDisplacementY) {
                val yPoint = actualFeasiblePointOnY(
                    point = thisPoint,
                    placement = placement,
                    fixedPlacements = fixedPlacements
                )
                if (yPoint != null && yPoint != thisPoint) {
                    stack.add(yPoint)
                }
            }
        }
        return points
    }

    /**
     * 沿 X 轴方向寻找实际可行点（向原点方向滑动）。
     * Find the actual feasible point along the X axis (sliding toward the origin).
     *
     * @param point 当前候选点
     * @param placement 当前放置
     * @param fixedPlacements 已固定的放置列表
     * @return X 轴方向的实际可行点，若已在原点则返回 null
    */
    private fun actualFeasiblePointOnX(
        point: QuantityPoint2<FltX>,
        placement: QuantityPlacement2<*, FltX, *>,
        fixedPlacements: List<QuantityPlacement2<*, FltX, *>?>
    ): QuantityPoint2<FltX>? {
        if (point.x eq FltX.zero) {
            return null
        }

        var maxX = FltX.minimum * point.x.unit
        for (fixedPlacement in fixedPlacements.filterNotNull()) {
            if (fixedPlacement == placement) {
                continue
            }
            if (point.y geq fixedPlacement.y
                && point.y leq fixedPlacement.maxY
                && fixedPlacement.maxX leq point.x
            ) {
                if (fixedPlacement.maxX geq maxX) {
                    maxX = fixedPlacement.maxX
                }
            }
        }
        return if (maxX eq FltX.minimum) {
            point2(x = FltX.zero * point.x.unit, y = point.y)
        } else {
            point2(x = maxX, y = point.y)
        }
    }

    /**
     * 沿 Y 轴方向寻找实际可行点（向原点方向滑动）。
     * Find the actual feasible point along the Y axis (sliding toward the origin).
     *
     * @param point 当前候选点
     * @param placement 当前放置
     * @param fixedPlacements 已固定的放置列表
     * @return Y 轴方向的实际可行点，若已在原点则返回 null
    */
    private fun actualFeasiblePointOnY(
        point: QuantityPoint2<FltX>,
        placement: QuantityPlacement2<*, FltX, *>,
        fixedPlacements: List<QuantityPlacement2<*, FltX, *>?>
    ): QuantityPoint2<FltX>? {
        if (point.y eq FltX.zero) {
            return null
        }

        var maxY = FltX.minimum * point.y.unit
        for (fixedPlacement in fixedPlacements.filterNotNull()) {
            if (fixedPlacement == placement) {
                continue
            }
            if (point.x geq fixedPlacement.x
                && point.x leq fixedPlacement.maxX
                && fixedPlacement.maxY leq point.y
            ) {
                if (fixedPlacement.maxY geq maxY) {
                    maxY = fixedPlacement.maxY
                }
            }
        }
        return if (maxY eq FltX.minimum) {
            point2(x = point.x, y = FltX.zero * point.y.unit)
        } else {
            point2(x = point.x, y = maxY)
        }
    }

    /**
     * 检查放置是否可行（不越界、不重叠、满足堆叠约束）。
     * Check whether a placement is feasible (within bounds, no overlap, satisfying stacking constraints).
     *
     * @param placement 待检查的放置
     * @param fixedPlacements 已固定的放置列表
     * @return 可行则返回 Ok(true)，不可行返回 Ok(false)，出错返回 Failed/Fatal
    */
    private suspend fun feasible(
        placement: QuantityPlacement2<*, FltX, P>,
        fixedPlacements: List<QuantityPlacement2<*, FltX, P>?>
    ): Ret<Boolean> {
        if ((placement.maxX gr space.length) || (placement.maxY gr space.width)) {
            return ok(false)
        }

        if (plane != Bottom) {
            when (val unit = placement.unit) {
                is Item -> {
                    if (!when (plane) {
                            is Side, is Front -> placement.enabledItemStackingOnPlane(
                                bottomItems = fixedPlacements,
                                space = space
                            )

                            else -> true
                        }
                    ) {
                        return ok(false)
                    }
                }

                is Block -> {
                    if (!when (plane) {
                            is Side, is Front -> placement.enabledBlockStackingOnPlane(
                                bottomItems = fixedPlacements,
                                space = space
                            )

                            else -> true
                        }
                    ) {
                        return ok(false)
                    }
                }

                else -> {
                    return Failed(Bpp3dValidationError("Invalid unit type: ${unit.javaClass}"))
                }
            }
        }
        for (fixedPlacement in fixedPlacements.asSequence().filterNotNull()) {
            when (val result = fixedPlacement.overlapped(placement)) {
                is Ok -> if (result.value) {
                    return ok(false)
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok(true)
    }
}
