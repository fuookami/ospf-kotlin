package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.eq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.geq
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.gr
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.leq
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item

/**
 * 货物高度组合器，用于在装箱过程中匹配符合高度约束的货物组合。
 * Item height combinator for matching item combinations that satisfy height constraints during bin packing.
*/
data object ItemHeightCombinator {
    private val logger = logger()
    private val defaultTowSumOffset = FltX(300.0)
    private val defaultThreeSumOffset = FltX(300.0)

    /**
     * 二数求和：从高度列表中寻找两个高度，使其和接近但不超过目标高度。
     * Two-sum: find two heights from the list whose sum is close to but does not exceed the target height.
     * @param height 目标高度 / target height
     * @param heights 候选高度列表 / list of candidate heights
     * @param offset 软约束偏移量 / soft constraint offset
     * @return 符合条件的二元组列表，按与目标高度差值的升序排列 / list of qualified pairs sorted by distance to target height
    */
    fun twoSum(
        height: FltX,
        heights: List<FltX>,
        offset: FltX = defaultTowSumOffset
    ): List<Pair<FltX, FltX>> {
        // todo: use Two Sum algorithm

        for (scale in 1..2) {
            val heightSoft = offset * FltX(scale.toDouble())
            val map = ArrayList<Pair<FltX, FltX>>()
            for (i in heights.indices) {
                for (j in (i + 1) until heights.size) {
                    val sum = heights[i] + heights[j]
                    if (sum geq (height - heightSoft) && sum leq height) {
                        map.add(Pair(heights[i], heights[j]))
                    }
                }
            }
            if (map.isNotEmpty()) {
                return map.sortedBy { height - it.first - it.second }
            }
        }
        return emptyList()
    }

    /**
     * 三数求和：从高度列表中寻找三个高度，使其和接近但不超过目标高度。
     * Three-sum: find three heights from the list whose sum is close to but does not exceed the target height.
     * @param height 目标高度 / target height
     * @param heights 候选高度列表 / list of candidate heights
     * @param offset 软约束偏移量 / soft constraint offset
     * @return 符合条件的三元组列表，按与目标高度差值的升序排列 / list of qualified triples sorted by distance to target height
    */
    fun threeSum(
        height: FltX,
        heights: List<FltX>,
        offset: FltX = defaultThreeSumOffset
    ): List<Triple<FltX, FltX, FltX>> {
        // todo: use Three Sum algorithm

        for (scale in 1..2) {
            val heightSoft = offset * FltX(scale.toDouble())
            val map = ArrayList<Triple<FltX, FltX, FltX>>()
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
                return map.sortedBy { height - it.first - it.second - it.third }
            }
        }
        return emptyList()
    }

    /**
     * 获取两个货物的组合，满足高度和重量约束。
     * Get a combination of two items satisfying height and weight constraints.
     * @param itemsGroup 按高度分组的货物映射 / map of items grouped by height
     * @param itemsAmount 货物数量映射 / map of item amounts
     * @param heights 两个货物的目标高度 / target heights for the two items
     * @param restWeight 剩余重量限制 / remaining weight limit
     * @param averageWeight 平均重量参考值 / average weight reference value
     * @return 符合条件的货物组合，未找到则返回 null / qualified item combination, or null if not found
    */
    suspend fun getItemCombination(
        itemsGroup: Map<FltX, List<Item>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Pair<FltX, FltX>,
        restWeight: FltX = FltX.maximum,
        averageWeight: FltX? = null
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
        itemsGroup: Map<FltX, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Pair<FltX, FltX>,
        mapper: Extractor<Item, T>,
        restWeight: FltX = FltX.maximum,
        averageWeight: FltX? = null
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
                        val thisItems = listOf(mapper(firstItem), mapper(secondItem)).sortedByDescending {
                            it.weight.value.toDouble()
                        }
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

    /**
     * 获取三个货物的组合，满足高度和重量约束。
     * Get a combination of three items satisfying height and weight constraints.
     * @param itemsGroup 按高度分组的货物映射 / map of items grouped by height
     * @param itemsAmount 货物数量映射 / map of item amounts
     * @param heights 三个货物的目标高度 / target heights for the three items
     * @param restWeight 剩余重量限制 / remaining weight limit
     * @param averageWeight 平均重量参考值 / average weight reference value
     * @return 符合条件的货物组合，未找到则返回 null / qualified item combination, or null if not found
    */
    suspend fun getItemCombination(
        itemsGroup: Map<FltX, List<Item>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Triple<FltX, FltX, FltX>,
        restWeight: FltX = FltX.maximum,
        averageWeight: FltX? = null
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
        itemsGroup: Map<FltX, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        heights: Triple<FltX, FltX, FltX>,
        mapper: (T) -> Item,
        restWeight: FltX = FltX.maximum,
        averageWeight: FltX? = null,
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
                            val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending {
                                it.weight.value.toDouble()
                            }
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
                            averageWeight = averageWeight?.let { (it - mapper(firstItem).weight.value) / FltX.two },
                            scope = this
                        )) {
                            val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending {
                                it.weight.value.toDouble()
                            }
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
                                val thisItems = listOf(mapper(firstItem), mapper(secondItem), mapper(thirdItem)).sortedByDescending {
                                    it.weight.value.toDouble()
                                }
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
        itemsGroup: Map<FltX, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        height: FltX,
        mapper: (T) -> Item,
        restWeight: FltX = FltX.maximum,
        averageWeight: FltX? = null,
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
        itemsGroup: Map<FltX, List<T>>,
        itemsAmount: Map<Item, UInt64>,
        height: FltX,
        mapper: (T) -> Item,
        restWeight: FltX = FltX.maximum,
        averageWeight: FltX? = null,
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
