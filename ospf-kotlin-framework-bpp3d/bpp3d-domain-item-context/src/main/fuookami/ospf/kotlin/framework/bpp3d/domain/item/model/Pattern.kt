package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.parallel.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.*

private data class PatternItemInfo(
    val item: Item,
    var amount: UInt64,
) : Copyable<PatternItemInfo> {
    override fun copy() = PatternItemInfo(item, amount)
}

private typealias NextPointExtractor = (projection: ItemProjection<Bottom>, placements: List<ItemPlacement2<Bottom>>) -> Point2

sealed class Pattern {
    data class Step(
        val lengthOrientation: ProjectivePlane,
        val nextPointExtractor: NextPointExtractor?
    ) {
        fun generatePlacement(
            projection: ItemProjection<Bottom>,
            placements: List<ItemPlacement2<Bottom>>,
            i: Int
        ): ItemPlacement2<Bottom> {
            val point = if (i == 0) {
                originPoint2
            } else {
                nextPointExtractor!!(projection, placements)
            }
            return Placement2(projection, point)
        }

        fun generateMultiPileProjection(items: List<Item>): Result<MultipleItemProjection<Bottom>?, Error> {
            var projection: MultipleItemProjection<Bottom>? = null
            val views = ArrayList<ItemView>()
            for (item in items) {
                when (val ret = getPatternView(item = item)) {
                    is Ok -> {
                        val view = ret.value
                        if (view != null) {
                            views.add(view)
                        }
                    }

                    is Failed -> {
                        return Failed(ret.error)
                    }
                }
            }
            if (views.size == items.size) {
                projection = MultiPileProjection(views, Bottom)
            }
            return Ok(projection)
        }

        fun getPatternView(item: Item): Result<ItemView?, Error> {
            for (orientation in item.enabledOrientations) {
                val view = item.view(orientation)
                when (lengthOrientation) {
                    is Front -> {
                        if (Bottom.length(view) geq Bottom.width(view)) {
                            return Ok(view)
                        }
                    }

                    is Side -> {
                        if (Bottom.length(view) leq Bottom.width(view)) {
                            return Ok(view)
                        }
                    }

                    else -> {
                        return Failed(Err(ErrorCode.ApplicationError, "Logic error!"))
                    }
                }
            }
            return Ok(null)
        }
    }

    data class Config(
        val withPiling: UInt64 = UInt64.maximum,
        val withRemainder: Boolean = false,
    ) {
        companion object {
            operator fun invoke(builder: ConfigBuilder): Config {
                return Config().new(builder)
            }
        }

        fun new(
            withPiling: UInt64? = null,
            withRemainder: Boolean? = null
        ): Config {
            return Config(
                withPiling = withPiling ?: this.withPiling,
                withRemainder = withRemainder ?: this.withRemainder
            )
        }

        fun new(builder: ConfigBuilder): Config {
            return new(
                withPiling = builder.withPiling,
                withRemainder = builder.withRemainder
            )
        }
    }

    data class ConfigBuilder(
        var withPiling: UInt64? = null,
        var withRemainder: Boolean? = null
    ) {
        companion object {
            operator fun invoke(func: ConfigBuilder.() -> Unit): ConfigBuilder {
                val builder = ConfigBuilder()
                func(builder)
                return builder
            }
        }

        operator fun invoke(): Config {
            return Config(this)
        }
    }

    companion object {
        fun buildConfig(builder: ConfigBuilder.() -> Unit): ConfigBuilder {
            val config = ConfigBuilder()
            builder(config)
            return config
        }

        val disabledOrientation = setOf(Orientation.Side, Orientation.SideRotated, Orientation.Lie, Orientation.LieRotated)

        val rightBottom: NextPointExtractor = { _, placements ->
            point2(x = placements.filter { it.y eq Flt64.zero }.maxOf { it.maxX })
        }

        val leftUpper: NextPointExtractor = { projection, placements ->
            val y = placements.filter { it.y eq Flt64.zero }.maxOf { it.maxY }
            val maxY = max(placements.maxOf { it.maxY }, y + projection.height)
            val yRange = ValueRange(y, maxY).value!!
            var x = Flt64.zero
            for (placement in placements) {
                val range = ValueRange(placement.y, placement.maxY, Interval.Closed, Interval.Open, Flt64).value!!
                if (yRange.intersect(range) != null) {
                    x = max(x, placement.maxX)
                }
            }
            point2(x, y)
        }
    }

    private val logger = logger()

    abstract val bottomLengthRange: ValueRange<Flt64>
    abstract val bottomWidthRange: ValueRange<Flt64>
    abstract val patterns: List<List<Step>>

    @JvmName("patternPlaceBinWithOnePatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        binType: BinType,
        pattern: List<Step>,
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<ItemPlacement3>>, Error> {
        return this(
            originItems = originItems,
            space = binType,
            restWeight = binType.capacity,
            patterns = listOf(pattern),
            predicate = predicate,
            config = config
        )
    }

    @JvmName("patternPlaceBinWithMultiPatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        binType: BinType,
        patterns: List<List<Step>> = emptyList(),
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<ItemPlacement3>>, Error> {
        return this(
            originItems = originItems,
            space = binType,
            restWeight = binType.capacity,
            patterns = patterns.ifEmpty { this.patterns },
            predicate = predicate,
            config = config
        )
    }

    @JvmName("patternPlaceSpaceWithOnePatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        space: Container3Shape,
        restWeight: Flt64,
        pattern: List<Step>,
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<ItemPlacement3>>, Error> {
        return this(
            originItems = originItems,
            space = space,
            restWeight = restWeight,
            patterns = listOf(pattern),
            predicate = predicate,
            config = config
        )
    }

    @JvmName("patternPlaceSpaceWithMultiPatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        space: Container3Shape,
        restWeight: Flt64,
        patterns: List<List<Step>> = emptyList(),
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<ItemPlacement3>>, Error> {
        val items = originItems
            .asSequence()
            .filter {
                if (predicate?.let { pred -> pred(it.key) } == false) {
                    return@filter false
                }
                val length = max(Bottom.length(it.key), Bottom.width(it.key))
                val width = min(Bottom.length(it.key), Bottom.width(it.key))
                bottomLengthRange.contains(length) && bottomWidthRange.contains(width)
            }
            .sortedBy {
                space.height - min(
                    (space.height / it.key.height).floor(),
                    it.key.maxLayer.toFlt64(),
                    it.key.maxHeight / it.key.height,
                    it.value.toFlt64()
                ) * it.key.height
            }
            .map { PatternItemInfo(it.key, it.value) }
            .toList()

        val (twoSumHeight, threeSumHeight) = when (config.withPiling) {
            UInt64.zero, UInt64.one -> {
                Pair(emptyList(), emptyList())
            }

            UInt64.two -> {
                val itemHeights = items.map { it.item.height }.toSet().toList().sorted()
                Pair(ItemHeightCombinator.twoSum(space.height, itemHeights), emptyList())
            }

            else -> {
                val itemHeights = items.map { it.item.height }.toSet().toList().sorted()
                Pair(ItemHeightCombinator.twoSum(space.height, itemHeights), ItemHeightCombinator.threeSum(space.height, itemHeights))
            }
        }

        return coroutineScope {
            val placementsList = ArrayList<List<ItemPlacement3>>()
            val promise = generatePlanePlacements(
                originItems = items,
                itemsGroup = items.groupBy { it.item.height }.toSortedMap(),
                twoSumHeight = twoSumHeight,
                threeSumHeight = threeSumHeight,
                space = space,
                restWeight = restWeight,
                patterns = patterns.ifEmpty { this@Pattern.patterns },
                config = config,
                scope = this
            )
            for (planePlacement in promise) {
                when (planePlacement) {
                    is Ok -> {
                        val placements = planePlacement.value.flatMap { it.toPlacement3() }
                        if (!placementsList.any { it.toTypedArray() contentEquals placements.toTypedArray() }
                            && Flt64(placements.maxOf { it.maxZ.toDouble() }) leq space.depth
                        ) {
                            placementsList.add(placements)
                        }
                    }

                    is Failed -> {
                        promise.close()
                        return@coroutineScope Failed(planePlacement.error)
                    }
                }
            }
            promise.close()
            return@coroutineScope Ok(placementsList)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun generatePlanePlacements(
        originItems: List<PatternItemInfo>,
        itemsGroup: Map<Flt64, List<PatternItemInfo>>,
        twoSumHeight: List<Pair<Flt64, Flt64>>,
        threeSumHeight: List<Triple<Flt64, Flt64, Flt64>>,
        space: Container3Shape,
        restWeight: Flt64,
        patterns: List<List<Step>>,
        config: Config,
        scope: CoroutineScope = GlobalScope
    ): ChannelGuard<Result<List<ItemPlacement2<Bottom>>, Error>> {
        val promise = Channel<Result<List<ItemPlacement2<Bottom>>, Error>>(Channel.UNLIMITED)
        for (pattern in patterns) {
            scope.launch(Dispatchers.Default) {
                try {
                    generatePlanePlacements(
                        originItems = originItems,
                        itemsGroup = itemsGroup,
                        twoSumHeight = twoSumHeight,
                        threeSumHeight = threeSumHeight,
                        space = space,
                        restWeight = restWeight,
                        pattern = pattern,
                        config = config,
                        promise = promise
                    )
                } catch (e: ClosedSendChannelException) {
                    logger.debug { "Pattern generation was stopped by controller." }
                } catch (e: Exception) {
                    logger.debug { "Pattern generation Error ${e.message}" }
                } finally {
                    promise.close()
                }
            }
        }
        return ChannelGuard(promise)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun generatePlanePlacements(
        originItems: List<PatternItemInfo>,
        itemsGroup: Map<Flt64, List<PatternItemInfo>>,
        twoSumHeight: List<Pair<Flt64, Flt64>>,
        threeSumHeight: List<Triple<Flt64, Flt64, Flt64>>,
        space: Container3Shape,
        restWeight: Flt64,
        pattern: List<Step>,
        config: Config,
        promise: Channel<Result<List<ItemPlacement2<Bottom>>, Error>>,
    ) {
        val items = originItems.map { it.copy() }
        while (true) {
            var i = 0
            val placements = ArrayList<ItemPlacement2<Bottom>>()
            val itemsAmount = items.associate { Pair(it.item, it.amount) }.toMutableMap()
            // generate mixed stacks through the combination of heights stacked in threes, and place them at the specified position
            for (heights in threeSumHeight) {
                val loadedWeight = placements.sumOf { it.weight }
                val thisRestWeight = restWeight - loadedWeight
                val thisRestPile = UInt64(pattern.size - i)
                val thisPileAverageWeight = if (i == 0) {
                    Flt64.zero
                } else {
                    loadedWeight / Flt64(i.toDouble())
                }
                val thisRestPileAverageWeight = thisRestWeight / thisRestPile.toFlt64()

                val itemCombination =
                    ItemHeightCombinator.getItemCombination(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        heights = heights,
                        mapper = { it.item },
                        restWeight = thisRestWeight,
                        averageWeight = if (thisPileAverageWeight geq (thisRestPileAverageWeight * Flt64.two)) {
                            thisRestPileAverageWeight
                        } else {
                            null
                        }
                    ) ?: continue
                val projection = when (val ret = pattern[i].generateMultiPileProjection(itemCombination)) {
                    is Ok -> {
                        ret.value
                    }

                    is Failed -> {
                        if (!promise.isClosedForSend) {
                            promise.send(Failed(ret.error))
                        }
                        return
                    }
                }

                if (projection != null) {
                    placements.add(pattern[i].generatePlacement(projection, placements, i))
                    ++i
                    for (item in itemCombination) {
                        itemsAmount[item] = itemsAmount[item]!! - UInt64.one
                    }
                }

                if (i == pattern.size) {
                    break
                }
            }

            // generate mixed stacks through the combination of heights stacked in pairs, and place them at the specified position
            if (i != pattern.size) {
                for (heights in twoSumHeight) {
                    val loadedWeight = placements.sumOf { it.weight }
                    val thisRestWeight = restWeight - loadedWeight
                    val thisRestPile = UInt64(pattern.size - i)
                    val thisPileAverageWeight = if (i == 0) {
                        Flt64.zero
                    } else {
                        loadedWeight / Flt64(i.toDouble())
                    }
                    val thisRestPileAverageWeight = thisRestWeight / thisRestPile.toFlt64()

                    val itemCombination = ItemHeightCombinator.getItemCombination(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        heights = heights,
                        mapper = { it.item },
                        restWeight = thisRestWeight,
                        averageWeight = if (thisPileAverageWeight geq (thisRestPileAverageWeight * Flt64.two)) {
                            thisRestPileAverageWeight
                        } else {
                            null
                        }
                    ) ?: continue
                    val projection = when (val ret = pattern[i].generateMultiPileProjection(itemCombination)) {
                        is Ok -> {
                            ret.value
                        }

                        is Failed -> {
                            if (!promise.isClosedForSend) {
                                promise.send(Failed(ret.error))
                            }
                            return
                        }
                    }

                    if (projection != null) {
                        placements.add(pattern[i].generatePlacement(projection, placements, i))
                        ++i
                        for (item in itemCombination) {
                            itemsAmount[item] = itemsAmount[item]!! - UInt64.one
                        }
                    }

                    if (i == pattern.size) {
                        break
                    }
                }
            }

            // generate stacks using a single material, and place them at the specified position
            if (i != pattern.size) {
                while (true) {
                    val loadedWeight = placements.sumOf { it.weight }
                    val thisRestWeight = restWeight - loadedWeight
                    val thisRestPile = UInt64(pattern.size - i)
                    val thisPileAverageWeight = if (i == 0) {
                        Flt64.zero
                    } else {
                        loadedWeight / Flt64(i.toDouble())
                    }
                    val thisRestPileAverageWeight = thisRestWeight / thisRestPile.toFlt64()

                    val thisSortedItems = if (thisPileAverageWeight geq (thisRestPileAverageWeight * Flt64.two)) {
                        items.filter { (_, amount) -> amount != UInt64.zero }.sortedBy { (item, _) ->
                            val heightAmount =
                                min(
                                    item.maxLayer,
                                    (thisRestWeight / item.weight).floor().toUInt64(),
                                    (item.maxHeight / Bottom.height(item)).floor().toUInt64(),
                                    (space.height / Bottom.height(item)).floor().toUInt64()
                                )
                            return@sortedBy if (heightAmount == UInt64.zero) {
                                Flt64.infinity
                            } else {
                                abs(heightAmount.toFlt64() * item.weight - thisRestPileAverageWeight)
                            }
                        }
                    } else {
                        items.filter { (_, amount) -> amount != UInt64.zero }
                    }

                    var flag = false
                    for ((item, _) in thisSortedItems) {
                        var amount = itemsAmount[item]!!
                        val heightAmount =
                            min(
                                config.withPiling,
                                min(
                                    item.maxLayer,
                                    (thisRestWeight / item.weight).floor().toUInt64(),
                                    (item.maxHeight / Bottom.height(item)).floor().toUInt64(),
                                    (space.height / Bottom.height(item)).floor().toUInt64()
                                )
                            )
                        if (heightAmount eq UInt64.zero || heightAmount gr amount) {
                            continue
                        }
                        flag = true
                        val pileAmount = (thisRestWeight / item.weight).floor().toUInt64() / heightAmount

                        for (k in UInt64.zero until pileAmount) {
                            if (heightAmount gr amount || i == pattern.size) {
                                break
                            }
                            val projection = when (val ret = pattern[i].getPatternView(item)) {
                                is Ok -> {
                                    ret.value?.let { PileProjection(it, Bottom, heightAmount) }
                                }

                                is Failed -> {
                                    if (!promise.isClosedForSend) {
                                        promise.send(Failed(ret.error))
                                    }
                                    return
                                }
                            }

                            if (projection == null) {
                                break
                            } else {
                                placements.add(pattern[i].generatePlacement(projection, placements, i))
                                ++i
                                amount -= heightAmount
                                itemsAmount[item] = itemsAmount[item]!! - heightAmount
                            }

                            if (i == pattern.size) {
                                break
                            }
                        }
                        break
                    }

                    if (!flag) {
                        break
                    }
                    if (i == pattern.size) {
                        break
                    }
                }
            }

            if ((!config.withRemainder && placements.size != pattern.size) || placements.isEmpty()) {
                break
            }
            for (item in items) {
                val thisAmount = placements.sumOf { it.projection.amount(item.item) }
                item.amount -= thisAmount
            }
            if (promise.isClosedForSend) {
                return
            }
            promise.send(Ok(placements))

            if (config.withRemainder && placements.size != pattern.size) {
                break
            }
        }
    }
}
