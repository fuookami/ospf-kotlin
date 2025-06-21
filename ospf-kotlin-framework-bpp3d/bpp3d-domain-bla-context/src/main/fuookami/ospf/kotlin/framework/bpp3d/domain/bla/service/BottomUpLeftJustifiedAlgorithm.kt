package fuookami.ospf.kotlin.framework.bpp3d.domain.bla.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

private fun compareWithPosition(
    lhs: Point2,
    rhs: Point2,
    withManhattanDistance: Boolean = true
): Order {
    if (withManhattanDistance) {
        val lhsManhattanDistance = lhs.distanceBetween(point2(), Distance.Manhattan)
        val rhsManhattanDistance = rhs.distanceBetween(point2(), Distance.Manhattan)
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

fun <P : ProjectivePlane> compareWithShapeAndWeight(
    lhs: Projection<*, P>,
    rhs: Projection<*, P>
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

class BottomUpLeftJustifiedAlgorithm<P : ProjectivePlane>(
    private val space: Container2Shape<P>,
    private val plane: P,
    private val config: Config<P> = Config()
) {
    private val logger = logger()

    constructor(length: Flt64, width: Flt64, plane: P, config: Config<P> = Config()) : this(
        space = Container2Shape(length, width, plane),
        plane = plane,
        config = config
    )

    data class Config<P : ProjectivePlane>(
        val withDisplacementX: Boolean = true,
        val withDisplacementY: Boolean = true,
        val comparator: ThreeWayComparator<Projection<*, P>> = ::compareWithShapeAndWeight,
        val positionComparator: ThreeWayComparator<Point2> = { lhs, rhs -> compareWithPosition(lhs, rhs) }
    ) {
        val withDisplacement = withDisplacementX || withDisplacementY

        companion object {
            fun <P : ProjectivePlane> invoke(
                withDisplacementX: Boolean? = null,
                withDisplacementY: Boolean? = null,
                withDisplacement: Boolean? = null,
                withManhattanDistance: Boolean? = null,
                comparator: ThreeWayComparator<Projection<*, P>>? = null,
                positionComparator: ThreeWayComparator<Point2>? = null
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
    
    @OptIn(DelicateCoroutinesApi::class)
    operator fun invoke(
        originProjections: List<Projection<*, P>>,
        scope: CoroutineScope = GlobalScope
    ): ChannelGuard<List<Placement2<*, P>?>> {
        val projections = config.comparator.let { originProjections.sortedWithThreeWayComparator { lhs, rhs -> it(lhs, rhs) } }

        val promise = Channel<List<Placement2<*, P>?>>()
        scope.launch(Dispatchers.Default) {
            val placements = projections.indices.map { null }.toMutableList<Placement2<*, P>?>()
            try {
                bla(
                    promise = promise,
                    placements = placements,
                    projections = projections
                )
            } catch (e: ClosedSendChannelException) {
                logger.trace { "BLA was stopped by controller." }
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
    
    @JvmName("invokeT")
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    operator fun <T : Cuboid<T>> invoke(
        projections: List<Projection<T, P>>,
        scope: CoroutineScope = GlobalScope
    ): ChannelGuard<List<Placement2<T, P>?>> {
        val promiseWrapper = Channel<List<Placement2<T, P>?>>()
        scope.launch(Dispatchers.Default) {
            val promise = invoke(
                originProjections = projections,
                scope = scope
            )
            try {
                for (result in promise) {
                    if (promiseWrapper.isClosedForSend) {
                        break
                    }
                    promiseWrapper.send(result as List<Placement2<T, P>?>)
                }
            } catch (e: ClosedSendChannelException) {
                logger.trace { "BLA was stopped by controller." }
            } catch (e: CancellationException) {
                logger.trace { "BLA was stopped by controller." }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.debug { "BLA Error ${e.message}" }
            } finally {
                promise.close()
                promiseWrapper.close()
            }
        }
        return ChannelGuard(promiseWrapper)
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun bla(
        promise: Channel<List<Placement2<*, P>?>>,
        placements: MutableList<Placement2<*, P>?>,
        projections: List<Projection<*, P>>
    ) {
        if (projections.isEmpty()) {
            return
        }

        val stack = ArrayList<Pair<Int, Placement2<*, P>?>>()
        stack.add(Pair(0, null))
        coroutineScope {
            val promises = ArrayList<Deferred<Placement2<*, P>?>>()
            val thisFeasiblePoints = feasiblePoints(placements = placements, reverse = true)
            for (feasiblePoint in thisFeasiblePoints) {
                promises.add(async(Dispatchers.Default) {
                    val placement = Placement2(projections[0], feasiblePoint)
                    if (feasible(placement, placements)) {
                        placement
                    } else {
                        null
                    }
                })
            }
            stack.addAll(promises.mapNotNull { it.await()?.let { placement -> Pair(0, placement) } })
        }

        var lastImpossible = false
        var lastFeasiblePoints: List<Point2> = emptyList()
        while (stack.isNotEmpty()) {
            if (promise.isClosedForSend) {
                return
            }

            val top = stack.removeAt(stack.lastIndex)
            placements[top.first] = top.second
            val i = top.first + 1
            if (top.first == projections.lastIndex) {
                if (promise.isClosedForSend) {
                    return
                }
                lastImpossible = false
                lastFeasiblePoints = emptyList()
                promise.send(placements.toList())
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
                                && !lastPlacement.contains(it, withUpperBound = false)                              // 被上一个放置的物料包住的点（包括三个坐标轴上距离原点较小的平面）
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
                    val promises = ArrayList<Deferred<Placement2<*, P>?>>()
                    for (feasiblePoint in thisFeasiblePoints) {
                        promises.add(async(Dispatchers.Default) {
                            val placement = Placement2(projections[i], feasiblePoint)
                            if (feasible(placement, placements)) {
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
    
    private suspend fun feasiblePoints(
        placements: List<Placement2<*, P>?>,
        reverse: Boolean = true
    ): List<Point2> {
        return feasiblePoints(
            targetPlacements = placements,
            fixedPlacements = placements,
            reverse = reverse
        )
    }
    
    private suspend fun feasiblePoints(
        targetPlacements: List<Placement2<*, P>?>,
        fixedPlacements: List<Placement2<*, P>?>,
        reverse: Boolean = true
    ): List<Point2> {
        val ret = HashSet<Point2>()
        for (placement in targetPlacements) {
            if (placement != null) {
                val thisPoints = HashSet<Point2>()

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
                        promises.map { it.await() }.flatten()
                    })
                }

                thisPoints.removeAll {
                    fixedPlacements.any { fixedPlacement ->
                        fixedPlacement?.contains(it, withUpperBound = false) ?: false
                    }
                }
                ret.addAll(thisPoints)
            }
        }
        ret.removeAll {
            fixedPlacements.any { fixedPlacement ->
                fixedPlacement?.contains(it, withUpperBound = false) ?: false
            }
        }
        if (ret.isEmpty()) {
            ret.add(point2())
        }
        return if (reverse) {
            ret.sortedWithThreeWayComparator { lhs, rhs -> (!config.positionComparator)(lhs, rhs) }
        } else {
            ret.sortedWithThreeWayComparator { lhs, rhs -> config.positionComparator(lhs, rhs) }
        }
    }
    
    private fun actualFeasiblePoints(
        point: Point2,
        placement: Placement2<*, *>,
        fixedPlacements: List<Placement2<*, *>?>
    ): List<Point2> {
        val stack = arrayListOf(point)
        val points = ArrayList<Point2>()
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
    
    private fun actualFeasiblePointOnX(
        point: Point2,
        placement: Placement2<*, *>,
        fixedPlacements: List<Placement2<*, *>?>
    ): Point2? {
        if (point.x eq Flt64.zero) {
            return null
        }

        var maxX = Flt64.negativeInfinity
        for (fixedPlacement in fixedPlacements.filterNotNull()) {
            if (fixedPlacement == placement) {
                continue
            }
            if (ValueRange(fixedPlacement.y, fixedPlacement.maxY).value!!.contains(point.y)
                && fixedPlacement.maxX leq point.x
            ) {
                if (fixedPlacement.maxX geq maxX) {
                    maxX = fixedPlacement.maxX
                }
            }
        }
        return if (maxX.isNegativeInfinity()) {
            point2(x = Flt64.zero, y = point.y)
        } else {
            point2(x = maxX, y = point.y)
        }
    }
    
    private fun actualFeasiblePointOnY(
        point: Point2,
        placement: Placement2<*, *>,
        fixedPlacements: List<Placement2<*, *>?>
    ): Point2? {
        if (point.y eq Flt64.zero) {
            return null
        }

        var maxY = Flt64.negativeInfinity
        for (fixedPlacement in fixedPlacements.filterNotNull()) {
            if (fixedPlacement == placement) {
                continue
            }
            if (ValueRange(fixedPlacement.x, fixedPlacement.maxX).value!!.contains(point.x)
                && fixedPlacement.maxY leq point.y
            ) {
                if (fixedPlacement.maxY geq maxY) {
                    maxY = fixedPlacement.maxY
                }
            }
        }
        return if (maxY.isNegativeInfinity()) {
            point2(x = point.x, y = Flt64.zero)
        } else {
            point2(x = point.x, y = maxY)
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    private suspend fun feasible(
        placement: Placement2<*, P>,
        fixedPlacements: List<Placement2<*, P>?>
    ): Boolean {
        if ((placement.maxX gr space.length) || (placement.maxY gr space.width)) {
            return false
        }

        if (plane != Bottom) {
            when (val unit = placement.unit) {
                is Item -> {
                    if (!when (plane) {
                            is Side -> (placement as ItemPlacement2<Side>).enabledStackingOn(
                                bottomItems = fixedPlacements.filterNotNull().map { it as Placement2<*, Side> },
                                space = space.restSpace(placement.position) as Container2Shape<Side>
                            )

                            is Front -> (placement as ItemPlacement2<Front>).enabledStackingOn(
                                fixedPlacements.filterNotNull().map { it as Placement2<*, Front> },
                                space = space.restSpace(placement.position) as Container2Shape<Front>
                            )

                            else -> true
                        }
                    ) {
                        return false
                    }
                }

                is Block -> {
                    if (!when (plane) {
                            is Side -> (placement as BlockPlacement2<Side>).enabledStackingOn(
                                bottomItems = fixedPlacements.filterNotNull().map { it as Placement2<*, Side> },
                                space = space.restSpace(placement.position) as Container2Shape<Side>
                            )

                            is Front -> (placement as BlockPlacement2<Front>).enabledStackingOn(
                                fixedPlacements.filterNotNull().map { it as Placement2<*, Front> },
                                space = space.restSpace(placement.position) as Container2Shape<Front>
                            )

                            else -> true
                        }
                    ) {
                        return false
                    }
                }

                else -> {
                    throw IllegalArgumentException("Invalid unit type: ${unit.javaClass}")
                }
            }
        }
        return fixedPlacements.asSequence().filterNotNull().all { !it.overlapped(placement) }
    }
}
