package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

data object ItemHeightCombinator {
    private val logger = logger()
    private val defaultTowSumOffset = Flt64(300.0)
    private val defaultThreeSumOffset = Flt64(300.0)

    fun twoSum(
        height: Flt64,
        heights: List<Flt64>,
        offset: Flt64 = defaultTowSumOffset
    ): List<Pair<Flt64, Flt64>> {
        // todo: use Two Sum algorithm

        for (scale in 1..2) {
            val heightSoft = offset * Flt64(scale.toDouble())
            val map = ArrayList<Pair<Flt64, Flt64>>()
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
        height: Flt64,
        heights: List<Flt64>,
        offset: Flt64 = defaultThreeSumOffset
    ): List<Triple<Flt64, Flt64, Flt64>> {
        // todo: use Three Sum algorithm

        for (scale in 1..2) {
            val heightSoft = offset * Flt64(scale.toDouble())
            val map = ArrayList<Triple<Flt64, Flt64, Flt64>>()
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
        itemsGroup: Map<Flt64, List<Item>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Pair<Flt64, Flt64>,
        restWeight: Flt64 = Flt64.infinity,
        averageWeight: Flt64? = null
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
        itemsGroup: Map<Flt64, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Pair<Flt64, Flt64>,
        mapper: Extractor<Item, T>,
        restWeight: Flt64 = Flt64.infinity,
        averageWeight: Flt64? = null
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
                        restWeight = restWeight - mapper(firstItem).weight,
                        averageWeight = averageWeight?.let { it - mapper(firstItem).weight },
                        scope = this
                    )) {
                        val thisItems = listOf(mapper(firstItem), mapper(secondItem)).sortedByDescending { it.weight }
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
        itemsGroup: Map<Flt64, List<Item>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Triple<Flt64, Flt64, Flt64>,
        restWeight: Flt64 = Flt64.infinity,
        averageWeight: Flt64? = null
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
        itemsGroup: Map<Flt64, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Triple<Flt64, Flt64, Flt64>,
        mapper: (T) -> Item,
        restWeight: Flt64 = Flt64.infinity,
        averageWeight: Flt64? = null,
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
                            restWeight = restWeight - mapper(firstItem).weight - mapper(secondItem).weight,
                            averageWeight = averageWeight?.let { it - mapper(firstItem).weight - mapper(secondItem).weight },
                            scope = this
                        )) {
                            val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending { it.weight }
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
                            restWeight = restWeight - mapper(firstItem).weight,
                            averageWeight = averageWeight?.let { (it - mapper(firstItem).weight) / Flt64.two },
                            scope = this
                        )) {
                            val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending { it.weight }
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
                            restWeight = restWeight - mapper(firstItem).weight,
                            averageWeight = averageWeight?.let { (it - mapper(firstItem).weight) * heights.second / (heights.second + heights.third) },
                            scope = this
                        )) {
                            for (thirdItem in getItem(
                                itemsGroup = itemsGroup,
                                itemsAmount = itemsAmount,
                                height = heights.third,
                                mapper = mapper,
                                restWeight = restWeight - mapper(firstItem).weight - mapper(secondItem).weight,
                                averageWeight = averageWeight?.let { it - mapper(firstItem).weight - mapper(secondItem).weight },
                                scope = this
                            )) {
                                val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending { it.weight }
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> getItem(
        itemsGroup: Map<Flt64, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        height: Flt64,
        mapper: (T) -> Item,
        restWeight: Flt64 = Flt64.infinity,
        averageWeight: Flt64? = null,
        scope: CoroutineScope = GlobalScope
    ): ChannelGuard<T> {
        val promise = Channel<T>(Channel.UNLIMITED)
        scope.launch(Dispatchers.Default) {
            try {
                val items = itemsGroup[height]?.toMutableList()
                if (items != null) {
                    if (averageWeight != null) {
                        items.sortBy { abs(mapper(it).weight - averageWeight) }
                    }
                    for (item in items) {
                        if (itemsAmount[mapper(item)]?.let { it >= UInt64.one } == true
                            && mapper(item).weight leq restWeight
                        ) {
                            if (promise.isClosedForSend) {
                                break
                            }
                            promise.send(item)
                        }
                    }
                }

                promise.close()
            } catch (e: ClosedSendChannelException) {
                logger.trace { "Item height combination was stopped by controller." }
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> getItem2(
        itemsGroup: Map<Flt64, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        height: Flt64,
        mapper: (T) -> Item,
        restWeight: Flt64 = Flt64.infinity,
        averageWeight: Flt64? = null,
        scope: CoroutineScope = GlobalScope
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
                        list.sortBy { abs(mapper(it).weight - averageWeight) }
                    }
                    for (i in list.indices) {
                        for (j in (i + 1) until list.size) {
                            if ((mapper(list[i]).weight + mapper(list[j]).weight) gr restWeight) {
                                continue
                            }
                            if (promise.isClosedForSend) {
                                break
                            }
                            promise.send(Pair(list[i], list[j]))
                        }
                    }
                }
            } catch (e: ClosedSendChannelException) {
                logger.trace { "Item height combination was stopped by controller." }
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
