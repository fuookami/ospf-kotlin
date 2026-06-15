/**
 * 模式模型。
 * Pattern model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.ItemHeightCombinator

private fun patternScalar(value: Number): FltX = FltX(value.toDouble())
private fun patternScalar(value: ULong): FltX = FltX(value.toDouble())

private data class PatternItemInfo(
    val item: Item,
    var amount: UInt64,
) : Copyable<PatternItemInfo> {
    override fun copy() = PatternItemInfo(item, amount)
}

abstract class Pattern {
    data class Step(
        val lengthOrientation: ProjectivePlane,
        val nextPointExtractor: ((projection: Projection<Item, FltX, Bottom>, placements: List<QuantityPlacement2<Item, FltX, Bottom>>) -> QuantityPoint2<FltX>)?
    ) {
        fun generatePlacement(
            projection: Projection<Item, FltX, Bottom>,
            placements: List<QuantityPlacement2<Item, FltX, Bottom>>,
            i: Int
        ): QuantityPlacement2<Item, FltX, Bottom> {
            val point = if (i == 0) {
                point2FltX()
            } else {
                nextPointExtractor!!(projection, placements)
            }
            return itemPlacement2Of(
                projection = projection,
                position = point
            )
        }

        fun generateMultiPileProjection(items: List<Item>): Result<MultiPileProjection<Item, FltX, Bottom>?, ErrorCode, Error<ErrorCode>> {
            var projection: MultiPileProjection<Item, FltX, Bottom>? = null
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

                    is Fatal -> {
                        return Fatal(ret.errors)
                    }
                }
            }
            if (views.size == items.size) {
                projection = MultiPileProjection(views, Bottom)
            }
            return Ok(projection)
        }

        fun getPatternView(item: Item): Result<ItemView?, ErrorCode, Error<ErrorCode>> {
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

        val rightBottom: (Projection<Item, FltX, Bottom>, List<QuantityPlacement2<Item, FltX, Bottom>>) -> QuantityPoint2<FltX> = { _, placements ->
            QuantityPoint2(
                x = placements.filter { it.y eq FltX.zero }.maxOfQuantity { it.maxX },
                y = FltX.zero * placements.first().y.unit
            )
        }

        val leftUpper: (Projection<Item, FltX, Bottom>, List<QuantityPlacement2<Item, FltX, Bottom>>) -> QuantityPoint2<FltX> = { projection, placements ->
            val y = placements.filter { it.y eq FltX.zero }.maxOfQuantity { it.maxY }
            val maxY = max(placements.maxOfQuantity { it.maxY }, y + projection.height)
            var x = y * FltX.zero
            for (placement in placements) {
                if (!((placement.maxY leq y) || (placement.y geq maxY))) {
                    x = max(x, placement.maxX)
                }
            }
            point2(x, y)
        }
    }

    private val logger = logger()

    abstract val bottomLengthRange: ValueRange<FltX>
    abstract val bottomWidthRange: ValueRange<FltX>
    abstract val patterns: List<List<Step>>

    @JvmName("patternPlaceBinWithOnePatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        binType: BinType<FltX>,
        pattern: List<Step>,
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<QuantityPlacement3<Item, FltX>>>, ErrorCode, Error<ErrorCode>> {
        return this(
            originItems = originItems,
            space = binType.asContainer3Shape(),
            restWeight = binType.capacity.value,
            patterns = listOf(pattern),
            predicate = predicate,
            config = config
        )
    }

    @JvmName("patternPlaceBinWithMultiPatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        binType: BinType<FltX>,
        patterns: List<List<Step>> = emptyList(),
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<QuantityPlacement3<Item, FltX>>>, ErrorCode, Error<ErrorCode>> {
        return this(
            originItems = originItems,
            space = binType.asContainer3Shape(),
            restWeight = binType.capacity.value,
            patterns = patterns.ifEmpty { this.patterns },
            predicate = predicate,
            config = config
        )
    }

    @JvmName("patternPlaceSpaceWithOnePatternImpl")
    suspend operator fun invoke(
        originItems: Map<Item, UInt64>,
        space: AbstractContainer3Shape,
        restWeight: FltX,
        pattern: List<Step>,
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<QuantityPlacement3<Item, FltX>>>, ErrorCode, Error<ErrorCode>> {
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
        space: AbstractContainer3Shape,
        restWeight: FltX,
        patterns: List<List<Step>> = emptyList(),
        predicate: ((Item) -> Boolean)? = null,
        config: Config = Config(),
    ): Result<List<List<QuantityPlacement3<Item, FltX>>>, ErrorCode, Error<ErrorCode>> {
        if (hasCylinderItem(originItems.keys)) {
            return Failed(
                Err(
                    ErrorCode.ApplicationError,
                    unsupportedCylinderCuboidOnlyPathMessage(
                        source = CylinderCapabilityPath.PatternPlacement.source,
                        pathPredicate = CylinderCapabilityPath.PatternPlacement.pathPredicate!!
                    )
                )
            )
        }

        val items = originItems
            .asSequence()
            .filter {
                if (predicate?.let { pred -> pred(it.key) } == false) {
                    return@filter false
                }
                val length = max(Bottom.length(it.key), Bottom.width(it.key)).value
                val width = min(Bottom.length(it.key), Bottom.width(it.key)).value
                bottomLengthRange.contains(length) && bottomWidthRange.contains(width)
            }
            .sortedBy {
                (
                    space.height.value - min(
                    (space.height / it.key.height).floor(),
                    FltX(it.key.maxLayer.toULong().toDouble()),
                    (it.key.maxHeight / it.key.height).floor(),
                    FltX(it.value.toULong().toDouble())
                ) * it.key.height.value
                ).toDouble()
            }
            .map { PatternItemInfo(it.key, it.value) }
            .toList()

        val (twoSumHeight, threeSumHeight) = when (config.withPiling) {
            UInt64.zero, UInt64.one -> {
                Pair(emptyList(), emptyList())
            }

            UInt64.two -> {
                val itemHeights = items.map { it.item.height.value }.toSet().toList().sorted()
                Pair(ItemHeightCombinator.twoSum(space.height.value, itemHeights), emptyList())
            }

            else -> {
                val itemHeights = items.map { it.item.height.value }.toSet().toList().sorted()
                Pair(ItemHeightCombinator.twoSum(space.height.value, itemHeights), ItemHeightCombinator.threeSum(space.height.value, itemHeights))
            }
        }

        return coroutineScope {
            val placementsList = ArrayList<List<QuantityPlacement3<Item, FltX>>>()
            val promise = generatePlanePlacements(
                originItems = items,
                itemsGroup = items.groupBy { it.item.height.value }.toSortedMap(),
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
                        val maxZ = placements.fold(FltX.minimum) { acc, placement ->
                            val current = FltX(placement.maxZ.toDouble())
                            if (current gr acc) current else acc
                        }
                        if (!placementsList.any { it.toTypedArray() contentEquals placements.toTypedArray() }
                            && maxZ leq space.depth.value
                        ) {
                            placementsList.add(placements)
                        }
                    }

                    is Failed -> {
                        promise.close()
                        return@coroutineScope Failed(planePlacement.error)
                    }

                    is Fatal -> {
                        promise.close()
                        return@coroutineScope Fatal(planePlacement.errors)
                    }
                }
            }
            promise.close()
            return@coroutineScope Ok(placementsList)
        }
    }

        private fun generatePlanePlacements(
        originItems: List<PatternItemInfo>,
        itemsGroup: Map<FltX, List<PatternItemInfo>>,
        twoSumHeight: List<Pair<FltX, FltX>>,
        threeSumHeight: List<Triple<FltX, FltX, FltX>>,
        space: AbstractContainer3Shape,
        restWeight: FltX,
        patterns: List<List<Step>>,
        config: Config,
        scope: CoroutineScope = bpp3dItemModelAsyncScope
    ): ChannelGuard<Result<List<QuantityPlacement2<Item, FltX, Bottom>>, ErrorCode, Error<ErrorCode>>> {
        val promise = Channel<Result<List<QuantityPlacement2<Item, FltX, Bottom>>, ErrorCode, Error<ErrorCode>>>(Channel.UNLIMITED)
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
                } catch (e: Exception) {
                    logger.debug { "Pattern generation Error ${e.message}" }
                } finally {
                    promise.close()
                }
            }
        }
        return ChannelGuard(promise)
    }

        private suspend fun generatePlanePlacements(
        originItems: List<PatternItemInfo>,
        itemsGroup: Map<FltX, List<PatternItemInfo>>,
        twoSumHeight: List<Pair<FltX, FltX>>,
        threeSumHeight: List<Triple<FltX, FltX, FltX>>,
        space: AbstractContainer3Shape,
        restWeight: FltX,
        pattern: List<Step>,
        config: Config,
        promise: Channel<Result<List<QuantityPlacement2<Item, FltX, Bottom>>, ErrorCode, Error<ErrorCode>>>,
    ) {
        val items = originItems.map { it.copy() }
        while (true) {
            var i = 0
            val placements = ArrayList<QuantityPlacement2<Item, FltX, Bottom>>()
            val itemsAmount = items.associate { Pair(it.item, it.amount) }.toMutableMap()
            // generate mixed stacks through the combination of heights stacked in threes, and place them at the specified position
            // 閫氳繃涓夊眰楂樺害缁勫悎鐢熸垚娣峰悎鍫嗗灈锛屽苟鏀剧疆鍒版寚瀹氫綅缃?
            for (heights in threeSumHeight) {
                val loadedWeight = placements.fold(FltX.zero) { acc, placement -> acc + placement.weight.value }
                val thisRestWeight = restWeight - loadedWeight
                val thisRestPile = UInt64(pattern.size - i)
                val thisPileAverageWeight = if (i == 0) {
                    FltX.zero
                } else {
                    loadedWeight / FltX(i.toDouble())
                }
                val thisRestPileAverageWeight = thisRestWeight / FltX(thisRestPile.toULong().toDouble())

                val itemCombination =
                    ItemHeightCombinator.getItemCombination(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        heights = heights,
                        mapper = { it.item },
                        restWeight = thisRestWeight,
                        averageWeight = if (thisPileAverageWeight geq (thisRestPileAverageWeight * FltX.two)) {
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
                        promise.trySend(Failed(ret.error))
                        return
                    }

                    is Fatal -> {
                        promise.trySend(Fatal(ret.errors))
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
            // 閫氳繃鍙屽眰楂樺害缁勫悎鐢熸垚娣峰悎鍫嗗灈锛屽苟鏀剧疆鍒版寚瀹氫綅缃?
            if (i != pattern.size) {
                for (heights in twoSumHeight) {
                    val loadedWeight = placements.fold(FltX.zero) { acc, placement -> acc + placement.weight.value }
                    val thisRestWeight = restWeight - loadedWeight
                    val thisRestPile = UInt64(pattern.size - i)
                    val thisPileAverageWeight = if (i == 0) {
                        FltX.zero
                    } else {
                        loadedWeight / FltX(i.toDouble())
                    }
                    val thisRestPileAverageWeight = thisRestWeight / FltX(thisRestPile.toULong().toDouble())

                    val itemCombination = ItemHeightCombinator.getItemCombination(
                        itemsGroup = itemsGroup,
                        itemsAmount = itemsAmount,
                        heights = heights,
                        mapper = { it.item },
                        restWeight = thisRestWeight,
                        averageWeight = if (thisPileAverageWeight geq (thisRestPileAverageWeight * FltX.two)) {
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
                            promise.trySend(Failed(ret.error))
                            return
                        }

                        is Fatal -> {
                            promise.trySend(Fatal(ret.errors))
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
            // 浣跨敤鍗曚竴鏉愭枡鐢熸垚鍫嗗灈锛屽苟鏀剧疆鍒版寚瀹氫綅缃?
            if (i != pattern.size) {
                while (true) {
                    val loadedWeight = placements.fold(FltX.zero) { acc, placement -> acc + placement.weight.value }
                    val thisRestWeight = restWeight - loadedWeight
                    val thisRestPile = UInt64(pattern.size - i)
                    val thisPileAverageWeight = if (i == 0) {
                        FltX.zero
                    } else {
                        loadedWeight / FltX(i.toDouble())
                    }
                    val thisRestPileAverageWeight = thisRestWeight / FltX(thisRestPile.toULong().toDouble())

                    val thisSortedItems = if (thisPileAverageWeight geq (thisRestPileAverageWeight * FltX.two)) {
                        items.filter { (_, amount) -> amount != UInt64.zero }
                            .sortedWith(compareBy<PatternItemInfo> { info ->
                                val item = info.item
                                val heightAmount =
                                    min(
                                        item.maxLayer,
                                        (thisRestWeight / item.weight.value).floor().toUInt64(),
                                        (item.maxHeight / Bottom.height(item)).floor().toUInt64(),
                                        (space.height / Bottom.height(item)).floor().toUInt64()
                                    )
                                if (heightAmount == UInt64.zero) {
                                    FltX.maximum
                                } else {
                                    abs(FltX(heightAmount.toULong().toDouble()) * item.weight.value - thisRestPileAverageWeight)
                                }
                            })
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
                                    (thisRestWeight / item.weight.value).floor().toUInt64(),
                                    (item.maxHeight / Bottom.height(item)).floor().toUInt64(),
                                    (space.height / Bottom.height(item)).floor().toUInt64()
                                )
                            )
                        if (heightAmount eq UInt64.zero || heightAmount gr amount) {
                            continue
                        }
                        flag = true
                        val pileAmount = (thisRestWeight / item.weight.value).floor().toUInt64() / heightAmount

                        for (k in UInt64.zero until pileAmount) {
                            if (heightAmount gr amount || i == pattern.size) {
                                break
                            }
                            val projection = when (val ret = pattern[i].getPatternView(item)) {
                                is Ok -> {
                                    ret.value?.let {
                                        PileProjection(
                                            view = it,
                                            plane = Bottom,
                                            layer = heightAmount
                                        )
                                    }
                                }

                                is Failed -> {
                                    promise.trySend(Failed(ret.error))
                                    return
                                }

                                is Fatal -> {
                                    promise.trySend(Fatal(ret.errors))
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
                val thisAmount = placements.fold(UInt64.zero) { acc, placement -> acc + placement.projection.amount(item.item) }
                item.amount -= thisAmount
            }
            if (promise.trySend(Ok(placements)).isFailure) {
                return
            }

            if (config.withRemainder && placements.size != pattern.size) {
                break
            }
        }
    }
}
