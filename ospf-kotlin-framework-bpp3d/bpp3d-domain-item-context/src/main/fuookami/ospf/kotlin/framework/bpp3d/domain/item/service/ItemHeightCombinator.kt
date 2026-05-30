@file:Suppress("DEPRECATION")

/**
 * 璐х墿楂樺害缁勫悎鍣ㄣ€?
 * Item height combinator.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ItemCuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger






private typealias HeightNumber = InfraNumber

data object ItemHeightCombinator {
    private val logger = logger()
    private val defaultTowSumOffset = HeightNumber(300.0)
    private val defaultThreeSumOffset = HeightNumber(300.0)

    fun twoSum(
        height: HeightNumber,
        heights: List<HeightNumber>,
        offset: HeightNumber = defaultTowSumOffset
    ): List<Pair<HeightNumber, HeightNumber>> {
        // todo: use Two Sum algorithm

        for (scale in 1..2) {
            val heightSoft = offset * HeightNumber(scale.toDouble())
            val map = ArrayList<Pair<HeightNumber, HeightNumber>>()
            for (i in heights.indices) {
                for (j in (i + 1) until heights.size) {
                    val sum = heights[i] + heights[j]
                    if (sum geq (height - heightSoft) && sum leq height) {
                        map.add(Pair(heights[i], heights[j]))
                    }
                }
            }
            if (map.isNotEmpty()) {
                return map.sortedBy { height - (it.first + it.second) }
            }
        }
        return emptyList()
    }

    fun threeSum(
        height: HeightNumber,
        heights: List<HeightNumber>,
        offset: HeightNumber = defaultThreeSumOffset
    ): List<Triple<HeightNumber, HeightNumber, HeightNumber>> {
        // todo: use Three Sum algorithm

        for (scale in 1..2) {
            val heightSoft = offset * HeightNumber(scale.toDouble())
            val map = ArrayList<Triple<HeightNumber, HeightNumber, HeightNumber>>()
            for (i in heights.indices) {
                for (j in i until heights.size) {
                    for (k in j until heights.size) {
                        if (i == j) {
                            continue
                        }

                        val sum = heights[i] + heights[j] + heights[k]
                        if (sum geq (height - heightSoft) && sum leq height) {
                            map.add(Triple(heights[i], heights[j], heights[k]))
                        }
                    }
                }
            }
            if (map.isNotEmpty()) {
                return map.sortedBy { height - (it.first + it.second + it.third) }
            }
        }
        return emptyList()
    }

    suspend fun getItemCombination(
        itemsGroup: Map<HeightNumber, List<Item>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Pair<HeightNumber, HeightNumber>,
        restWeight: HeightNumber = infraInfinity(),
        averageWeight: HeightNumber? = null
    ): List<Item>? {
        return getItemCombination(
            itemsGroup = itemsGroup,
            itemsAmount = itemsAmount,
            heights = heights,
            mapper = { it },
            restWeight = restWeight,
            averageWeight = averageWeight
        )
    }

    suspend fun <T> getItemCombination(
        itemsGroup: Map<HeightNumber, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Pair<HeightNumber, HeightNumber>,
        mapper: Extractor<Item, T>,
        restWeight: HeightNumber = infraInfinity(),
        averageWeight: HeightNumber? = null
    ): List<Item>? {
        var items: List<Item>? = null

        try {
            coroutineScope {
                for (firstItem in getItem(
                    itemsGroup = itemsGroup,
                    itemsAmount = itemsAmount,
                    height = heights.first,
                    mapper = mapper,
                    restWeight = restWeight,
                    averageWeight = averageWeight?.let { it * heights.first / (heights.first + heights.second) },
                    scope = this
                )) {
                    for (secondItem in getItem(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        height = heights.second,
                        mapper = mapper,
                        restWeight = restWeight - mapper(firstItem).weight.value,
                        averageWeight = averageWeight?.let { it - mapper(firstItem).weight.value },
                        scope = this
                    )) {
                        val thisItems = listOf(mapper(firstItem), mapper(secondItem)).sortedByDescending { it.weight.value.toDouble() }
                        var flag = true
                        for (i in thisItems.indices) {
                            if (!thisItems[i + 1].enabledStackingOn(thisItems[i], UInt64((i + 1)))) {
                                flag = false
                                break
                            }
                        }
                        if (flag) {
                            items = thisItems
                            cancel()
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            logger.trace { "Item Combination Stopped" }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.debug { "Item Combination Error ${e.message}" }
        }

        return items
    }

    suspend fun getItemCombination(
        itemsGroup: Map<HeightNumber, List<Item>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Triple<HeightNumber, HeightNumber, HeightNumber>,
        restWeight: HeightNumber = infraInfinity(),
        averageWeight: HeightNumber? = null
    ): List<Item>? {
        return getItemCombination(
            itemsGroup = itemsGroup,
            itemsAmount = itemsAmount,
            heights = heights,
            mapper = { it },
            restWeight = restWeight,
            averageWeight = averageWeight
        )
    }

    suspend fun <T> getItemCombination(
        itemsGroup: Map<HeightNumber, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Triple<HeightNumber, HeightNumber, HeightNumber>,
        mapper: (T) -> Item,
        restWeight: HeightNumber = infraInfinity(),
        averageWeight: HeightNumber? = null,
    ): List<Item>? {
        var items: List<Item>? = null

        try {
            coroutineScope {
                if (heights.first eq heights.second) {
                    for ((firstItem, secondItem) in getItem2(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        height = heights.first,
                        mapper = mapper,
                        restWeight = restWeight,
                        averageWeight = averageWeight?.let { it * heights.first / (heights.first + heights.second + heights.third) },
                        scope = this
                    )) {
                        for (thirdItem in getItem(
                            itemsGroup = itemsGroup,
                            itemsAmount = itemsAmount,
                            height = heights.third,
                            mapper = mapper,
                            restWeight = restWeight - mapper(firstItem).weight.value - mapper(secondItem).weight.value,
                            averageWeight = averageWeight?.let { it - mapper(firstItem).weight.value - mapper(secondItem).weight.value },
                            scope = this
                        )) {
                            val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending { it.weight.value.toDouble() }
                            var flag = true
                            for (i in thisItems.indices) {
                                if (!thisItems[i + 1].enabledStackingOn(thisItems[i], UInt64((i + 1)))) {
                                    flag = false
                                    break
                                }
                            }
                            if (flag) {
                                items = thisItems
                                cancel()
                            }
                        }
                    }
                } else if (heights.second eq heights.third) {
                    for (firstItem in getItem(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        height = heights.first,
                        mapper = mapper,
                        restWeight = restWeight,
                        averageWeight = averageWeight?.let { it * heights.first / (heights.first + heights.second + heights.third) },
                        scope = this
                    )) {
                        for ((secondItem, thirdItem) in getItem2(
                            itemsGroup = itemsGroup,
                            itemsAmount = itemsAmount,
                            height = heights.second,
                            mapper = mapper,
                            restWeight = restWeight - mapper(firstItem).weight.value,
                            averageWeight = averageWeight?.let { (it - mapper(firstItem).weight.value) / HeightNumber.two },
                            scope = this
                        )) {
                            val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending { it.weight.value.toDouble() }
                            var flag = true
                            for (i in thisItems.indices) {
                                if (!thisItems[i + 1].enabledStackingOn(thisItems[i], UInt64((i + 1)))) {
                                    flag = false
                                    break
                                }
                            }
                            if (flag) {
                                items = thisItems
                                cancel()
                            }
                        }
                    }
                } else {
                    for (firstItem in getItem(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        height = heights.first,
                        mapper = mapper,
                        restWeight = restWeight,
                        averageWeight = averageWeight?.let { it * heights.first / (heights.first + heights.second + heights.third) },
                        scope = this
                    )) {
                        for (secondItem in getItem(
                            itemsGroup = itemsGroup,
                            itemsAmount = itemsAmount,
                            height = heights.second,
                            mapper = mapper,
                            restWeight = restWeight - mapper(firstItem).weight.value,
                            averageWeight = averageWeight?.let { (it - mapper(firstItem).weight.value) * heights.second / (heights.second + heights.third) },
                            scope = this
                        )) {
                            for (thirdItem in getItem(
                                itemsGroup = itemsGroup,
                                itemsAmount = itemsAmount,
                                height = heights.third,
                                mapper = mapper,
                                restWeight = restWeight - mapper(firstItem).weight.value - mapper(secondItem).weight.value,
                                averageWeight = averageWeight?.let { it - mapper(firstItem).weight.value - mapper(secondItem).weight.value },
                                scope = this
                            )) {
                                val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending { it.weight.value.toDouble() }
                                var flag = true
                                for (i in thisItems.indices) {
                                    if (!thisItems[i + 1].enabledStackingOn(thisItems[i], UInt64((i + 1)))) {
                                        flag = false
                                        break
                                    }
                                }
                                if (flag) {
                                    items = thisItems
                                    cancel()
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            logger.trace { "Item Combination Stopped" }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.debug { "Item Combination Error ${e.message}" }
        }

        return items
    }

        private fun <T> getItem(
        itemsGroup: Map<HeightNumber, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        height: HeightNumber,
        mapper: (T) -> Item,
        restWeight: HeightNumber = infraInfinity(),
        averageWeight: HeightNumber? = null,
        scope: CoroutineScope = bpp3dItemServiceAsyncScope
    ): ChannelGuard<T> {
        val promise = Channel<T>(Channel.UNLIMITED)
        scope.launch(Dispatchers.Default) {
            try {
                    val items = itemsGroup[height]?.toMutableList()
                    if (items != null) {
                        if (averageWeight != null) {
                            items.sortBy { abs(mapper(it).weight.value - averageWeight).toDouble() }
                        }
                        for (item in items) {
                            if (itemsAmount[mapper(item)]?.let { it >= UInt64.one } == true
                            && mapper(item).weight.value leq restWeight
                        ) {
                            if (promise.trySend(item).isFailure) {
                                break
                            }
                        }
                    }
                }

                promise.close()
            } catch (e: CancellationException) {
                logger.trace { "Item height combination was stopped by controller." }
            } catch (e: Exception) {
                logger.debug { "Item height combination Error ${e.message}" }
            } finally {
                promise.close()
            }
        }
        return ChannelGuard(promise)
    }

        private fun <T> getItem2(
        itemsGroup: Map<HeightNumber, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        height: HeightNumber,
        mapper: (T) -> Item,
        restWeight: HeightNumber = infraInfinity(),
        averageWeight: HeightNumber? = null,
        scope: CoroutineScope = bpp3dItemServiceAsyncScope
    ): ChannelGuard<Pair<T, T>> {
        val promise = Channel<Pair<T, T>>(Channel.UNLIMITED)
        scope.launch(Dispatchers.Default) {
            try {
                val items = itemsGroup[height]
                if (items != null) {
                    val list = items.map { item ->
                        if (itemsAmount[mapper(item)]?.let { it >= UInt64.two } == true) {
                            listOf(item, item)
                        } else if (itemsAmount[mapper(item)]?.let { it >= UInt64.one } == true) {
                            listOf(item)
                        } else {
                            emptyList()
                        }
                    }.flatten().toMutableList()
                    if (averageWeight != null) {
                        list.sortBy { abs(mapper(it).weight.value - averageWeight).toDouble() }
                    }
                    for (i in list.indices) {
                        for (j in (i + 1) until list.size) {
                            if ((mapper(list[i]).weight.value + mapper(list[j]).weight.value) gr restWeight) {
                                continue
                            }
                            if (promise.trySend(Pair(list[i], list[j])).isFailure) {
                                break
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                logger.trace { "Item height combination was stopped by controller." }
            } catch (e: Exception) {
                logger.debug { "Item height combination Error ${e.message}" }
            } finally {
                promise.close()
            }
        }
        return ChannelGuard(promise)
    }
}



