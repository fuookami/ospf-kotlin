@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyCuboid
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyQuantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.LegacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyOne
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyTwo
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.api.legacyZero

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.utils.functional.Order

private typealias MergeScalar = LegacyScalar
private fun scalar(value: Number): MergeScalar = MergeScalar(value.toDouble())
private fun scalar(value: ULong): MergeScalar = MergeScalar(value.toDouble())

data object ItemMerger {
    data class Config(
        val mergeFillerWhenOnlyFiller: Boolean = true,
        val mergeWithRotation: Boolean = true,
        val mergeAsPatternBlock: Boolean = true,
        val mergeAsHollowSquareBlock: Boolean = false,
        val mergeAsPile: Boolean = true,
        val mergeAsBlock: Boolean = true,
        val orientationOrder: List<Orientation> = emptyList(),
        val patternConfig: Pattern.ConfigBuilder = Pattern.ConfigBuilder()
    ) {
        companion object {
            operator fun invoke(builder: ConfigBuilder): Config {
                return Config().new(builder)
            }
        }

        fun new(
            mergeFillerWhenOnlyFiller: Boolean? = null,
            mergeWithRotation: Boolean? = null,
            mergeAsPatternBlock: Boolean? = null,
            mergeAsHollowSquareBlock: Boolean? = null,
            mergeAsPile: Boolean? = null,
            mergeAsBlock: Boolean? = null,
            orientationOrder: List<Orientation>? = null,
            patternConfig: Pattern.ConfigBuilder? = null
        ): Config {
            return Config(
                mergeFillerWhenOnlyFiller = mergeFillerWhenOnlyFiller ?: this.mergeFillerWhenOnlyFiller,
                mergeWithRotation = mergeWithRotation ?: this.mergeWithRotation,
                mergeAsPatternBlock = mergeAsPatternBlock ?: this.mergeAsPatternBlock,
                mergeAsHollowSquareBlock = mergeAsHollowSquareBlock ?: this.mergeAsHollowSquareBlock,
                mergeAsPile = mergeAsPile ?: this.mergeAsPile,
                mergeAsBlock = mergeAsBlock ?: this.mergeAsBlock,
                orientationOrder = orientationOrder ?: this.orientationOrder,
                patternConfig = patternConfig ?: this.patternConfig
            )
        }

        fun new(builder: ConfigBuilder): Config {
            return new(
                mergeFillerWhenOnlyFiller = builder.mergeFillerWhenOnlyFiller,
                mergeWithRotation = builder.mergeWithRotation,
                mergeAsPatternBlock = builder.mergeAsPatternBlock,
                mergeAsHollowSquareBlock = builder.mergeAsHollowSquareBlock,
                mergeAsPile = builder.mergeAsPile,
                mergeAsBlock = builder.mergeAsBlock,
                orientationOrder = builder.orientationOrder,
                patternConfig = builder.patternConfig
            )
        }
    }

    data class ConfigBuilder(
        var mergeFillerWhenOnlyFiller: Boolean? = null,
        var mergeWithRotation: Boolean? = null,
        var mergeAsPatternBlock: Boolean? = null,
        var mergeAsHollowSquareBlock: Boolean? = null,
        var mergeAsPile: Boolean? = null,
        var mergeAsBlock: Boolean? = null,
        var orientationOrder: List<Orientation>? = null,
        var patternConfig: Pattern.ConfigBuilder? = null
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

    fun buildConfig(builder: ConfigBuilder.() -> Unit): ConfigBuilder {
        val config = ConfigBuilder()
        builder(config)
        return config
    }

    suspend fun merge(
        items: List<Item>,
        binType: BinType,
        patterns: List<Pattern>,
        predicate: Predicate<Item>? = null,
        fillerPredicate: Predicate<Item>? = null,
        config: Config = Config()
    ): List<Cuboid<*>> {
        return merge(
            items = items,
            space = binType,
            restWeight = binType.capacity.value,
            patterns = patterns,
            predicate = predicate,
            fillerPredicate = fillerPredicate,
            config = config
        )
    }

    suspend fun merge(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: MergeScalar,
        patterns: List<Pattern>,
        predicate: Predicate<Item>? = null,
        fillerPredicate: Predicate<Item>? = null,
        config: Config = Config()
    ): List<Cuboid<*>> {
        val withFillerMerging = config.mergeFillerWhenOnlyFiller && items.any { item -> fillerPredicate?.invoke(item) == false }

        var restItems = if (!withFillerMerging) {
            items.filter { item -> predicate?.let { it(item) } != false && fillerPredicate?.let { it(item) } != true }
        } else {
            items.filter { item -> predicate?.let { it(item) } != false }
        }
        val mergedItems = ArrayList<Cuboid<*>>()

        if (config.mergeAsPatternBlock) {
            val (thisMergedItems, thisRestItems) = mergePatternBlocks(
                items = restItems,
                space = space,
                patterns = patterns,
                restWeight = restWeight,
                patternConfig = config.patternConfig
            )
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        if (config.mergeAsHollowSquareBlock) {
            val (thisMergedItems, thisRestItems) = mergeHollowSquareBlocks(
                items = restItems,
                space = space,
                restWeight = restWeight
            )
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        if (config.mergeAsBlock) {
            val (thisMergedItems, thisRestItems) = mergeBlocks(
                items = restItems,
                space = space,
                restWeight = restWeight,
                config = config
            )
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        if (config.mergeAsPile) {
            val (thisMergedItems, thisRestItems) = mergePiles(
                items = restItems,
                space = space,
                restWeight = restWeight
            )
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        mergedItems.addAll(restItems)
        if (!withFillerMerging) {
            mergedItems.addAll(items.filter { item -> predicate?.let { it(item) } == false || fillerPredicate?.let { it(item) } == true })
        }
        return mergedItems
    }

    fun mergePiles(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: MergeScalar = MergeScalar.infinity
    ): Pair<List<Pile>, List<Item>> {
        val averagePileBottomArea = items.fold(MergeScalar.zero) { acc, item -> acc + Bottom.shape(item).area.value } / scalar(items.size)
        val averagePileWeight = restWeight / (Bottom.shape(space).area.value / averagePileBottomArea)
        val mergedItems = ArrayList<Pile>()
        val restItems = items
            .sortedWith(compareByDescending<Item> { it.weight.value.toDouble() })
            .map { it.view() }
            .toMutableList()
        while (restItems.isNotEmpty()) {
            var flag = false

            for (i in restItems.indices) {
                val thisBottomItem = restItems[i]
                val enabledItems = restItems
                    .subList(i + 1, restItems.size)
                    .sortedWith(compareByDescending<ItemView> { it.weight.value.toDouble() })
                val visited = enabledItems.map { false }.toMutableList()
                val pileItems = arrayListOf(thisBottomItem)
                for (j in enabledItems.indices) {
                    if (pileItems.sumOf { it.weight } geq averagePileWeight) {
                        break
                    }

                    val (layer, height) = Pile.layer(enabledItems[j], pileItems)
                    if (enabledItems[j].enabledStackingOn(
                            bottomItem = pileItems.last(),
                            layer = layer,
                            height = height,
                            space = space.restSpace(vector3(y = pileItems.sumOf { it.height }))
                        ) && (((enabledItems[j].width - pileItems.last().width).abs() + (enabledItems[j].depth - pileItems.last().depth).abs()) leq scalar(50.0))
                    ) {
                        visited[j] = true
                        pileItems.add(enabledItems[j])
                    }
                }
                for (j in enabledItems.indices.reversed()) {
                    if (visited[j]) {
                        continue
                    }

                    val (layer, height) = Pile.layer(enabledItems[j], pileItems)
                    if (enabledItems[j].enabledStackingOn(
                            bottomItem = pileItems.last(),
                            layer = layer,
                            height = height,
                            space = space.restSpace(vector3(y = pileItems.sumOf { it.height }))
                        )
                        && (((enabledItems[j].width - pileItems.last().width).abs() + (enabledItems[j].depth - pileItems.last().depth).abs()) leq scalar(50.0))
                    ) {
                        visited[j] = true
                        pileItems.add(enabledItems[j])
                    }
                }

                if (pileItems.size > 1) {
                    mergedItems.add(Pile(pileItems))
                    for (item in pileItems) {
                        restItems.remove(item)
                    }
                    flag = true
                    break
                }
            }
            if (!flag) {
                break
            }
        }
        return Pair(mergedItems, restItems.map { it.unit })
    }

    fun mergeBlocks(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: MergeScalar = MergeScalar.infinity,
        config: Config = Config()
    ): Pair<List<SimpleBlock>, List<Item>> {
        val mergedItems = ArrayList<SimpleBlock>()
        val restItems = items.groupBy { it }.map { Pair(it.key, it.value.toMutableList()) }.toMap()
        for ((item, list) in restItems) {
            val orientations = item
                .enabledOrientationsAt(space, config.mergeWithRotation)
                .sortedWithThreeWayComparator { lhs, rhs ->
                    if (config.orientationOrder.isNotEmpty()) {
                        config.orientationOrder.ord(lhs, rhs)
                    } else {
                        val lhsView = item.view(lhs)
                        val lhsItemMaxYAmount = min(lhsView.maxLayer, (lhsView.maxHeight / lhsView.height.value).floor().toUInt64())
                        val lhsItemMaxZAmount = (lhsView.maxDepth / lhsView.depth.value).floor().toUInt64()
                        val lhsMaxAmount = space.maxAmount(
                            unit = lhsView,
                            maxYAmount = lhsItemMaxYAmount,
                            maxZAmount = lhsItemMaxZAmount
                        )

                        val rhsView = item.view(rhs)
                        val rhsItemMaxYAmount = min(rhsView.maxLayer, (rhsView.maxHeight / rhsView.height.value).floor().toUInt64())
                        val rhsItemMaxZAmount = (rhsView.maxDepth / rhsView.depth.value).floor().toUInt64()
                        val rhsMaxAmount = space.maxAmount(
                            unit = rhsView,
                            maxYAmount = rhsItemMaxYAmount,
                            maxZAmount = rhsItemMaxZAmount
                        )

                        when (val result = lhsMaxAmount ord rhsMaxAmount) {
                            Order.Equal -> {}

                            else -> {
                                return@sortedWithThreeWayComparator result
                            }
                        }

                        lhs ord rhs
                    }
                }

            for (orientation in orientations) {
                val view = item.view(orientation)
                val xAmount = (space.width / view.width).floor().toUInt64()
                val yAmount = min(
                    item.maxLayer,
                    (item.maxHeight / view.height.value).floor().toUInt64(),
                    (space.height / view.height).floor().toUInt64()
                )
                val zAmount = if (view.minDepth eq MergeScalar.zero) {
                    UInt64.one
                } else {
                    val minZAmount = (view.minDepth / view.depth.value).ceil().toUInt64()
                    val availableZAmount = UInt64(list.size) / (xAmount * yAmount)
                    if (availableZAmount >= minZAmount) {
                        min(
                            availableZAmount,
                            (view.maxDepth / view.depth.value).floor().toUInt64()
                        )
                    } else {
                        minZAmount
                    }
                }
                val maxAmount = xAmount * yAmount * zAmount
                if (maxAmount != UInt64.zero && maxAmount != UInt64.one && UInt64(list.size) >= maxAmount) {
                    val placements = ArrayList<ItemPlacement3>()
                    for (i in UInt64.zero until xAmount) {
                        val x = scalar(i.toULong()) * view.width
                        for (j in UInt64.zero until yAmount) {
                            val y = scalar(j.toULong()) * view.height
                            for (k in UInt64.zero until zAmount) {
                                val z = scalar(k.toULong()) * view.depth
                                placements.add(QuantityPlacement3(view, point3(x = x, y = y, z = z)))
                            }
                        }
                    }
                    val block = SimpleBlock(placements)
                    for (i in UInt64.zero until (restWeight / block.weight.value).floor().toUInt64()) {
                        if (UInt64(list.size) ls maxAmount) {
                            break
                        }
                        mergedItems.add(block)
                        (UInt64.zero until maxAmount).forEach { _ -> list.removeAt(list.lastIndex) }
                    }
                } else {
                    break
                }
            }
        }
        return Pair(mergedItems, restItems.flatMap { it.value })
    }

    suspend fun mergePatternBlocks(
        items: List<Item>,
        space: AbstractContainer3Shape,
        patterns: List<Pattern>,
        restWeight: MergeScalar = MergeScalar.infinity,
        patternConfig: Pattern.ConfigBuilder = Pattern.ConfigBuilder()
    ): Pair<List<CommonBlock>, List<Item>> {
        val mergedItems = ArrayList<CommonBlock>()
        var restItems = items.toList()
        while (true) {
            val placers = patterns.toList()
            var thisRestItems = restItems.group()

            var patternBlocks: List<CommonBlock> = emptyList()
            for (placer in placers) {
                when (val ret = placer(
                    originItems = thisRestItems,
                    space = space,
                    restWeight = restWeight,
                    predicate = { item -> Pattern.disabledOrientation.all { !item.enabledOrientations.contains(it) } },
                    config = Pattern.Config(withRemainder = true).new(patternConfig)
                )) {
                    is Ok -> {
                        if (ret.value.isNotEmpty()) {
                            patternBlocks = ret.value.map { CommonBlock(it) }
                            break
                        }
                    }

                    is Failed -> {}

                    is Fatal -> {}
                }
            }

            var mergedFlag = false
            if (patternBlocks.isEmpty()) {
                break
            } else {
                for (block in patternBlocks) {
                    var removeFlag = true
                    val tmpRestItems = thisRestItems.toMutableMap()
                    for ((item, amount) in block.amounts) {
                        item as Item
                        if (tmpRestItems[item]?.let { it >= amount } == true) {
                            tmpRestItems[item] = tmpRestItems[item]!! - amount
                        } else {
                            removeFlag = false
                            break
                        }
                    }

                    if (removeFlag) {
                        mergedFlag = true
                        mergedItems.add(block)
                        thisRestItems = tmpRestItems
                    }
                }
                restItems = thisRestItems.toMap().flatten()
            }

            if (!mergedFlag) {
                break
            }
        }
        return Pair(mergedItems, restItems)
    }

    fun mergeHollowSquareBlocks(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: MergeScalar = MergeScalar.infinity,
        config: Config = Config()
    ): Pair<List<HollowSquareBlock>, List<Item>> {
        val restItems = items.groupBy { it }.map { Pair(it.key, UInt64(it.value.size)) }.toMap()
        return mergeHollowSquareBlocks(
            items = restItems,
            space = space,
            restWeight = restWeight,
            config = config
        ).let { Pair(it.first, it.second.flatMap { item -> (UInt64.zero until item.value).map { item.key } }) }
    }

    fun mergeHollowSquareBlocks(
        items: Map<Item, UInt64>,
        space: AbstractContainer3Shape,
        restWeight: MergeScalar = MergeScalar.infinity,
        config: Config = Config()
    ): Pair<List<HollowSquareBlock>, Map<Item, UInt64>> {
        val restItems = items.toMutableMap()
        val mergedItems = ArrayList<HollowSquareBlock>()
        for ((item, restAmount) in restItems) {
            var thisRestAmount = restAmount

            while (true) {
                val orientation = item.enabledOrientationsAt(space, config.mergeWithRotation).find {
                    val view = item.view(it)
                    val depth = it.depth(item)
                    val width = it.width(item)
                    val height = it.height(item)

                    if (depth ls width) {
                        return@find false
                    }

                    val hollowSquareSpace = Container3Shape(
                        width = space.width,
                        height = space.height,
                        depth = depth * MergeScalar.two - (depth % width)
                    )
                    if (hollowSquareSpace.width ls (width + depth)
                        || hollowSquareSpace.depth ls (depth + width)
                    ) {
                        return@find false
                    }

                    val amount = min((hollowSquareSpace.width - depth) / width, depth / width).floor().toUInt64()
                    val rotatedAmount = min((hollowSquareSpace.depth - depth) / width, depth / width).floor().toUInt64()
                    val heightAmount = min(
                        view.maxLayer,
                        (view.maxHeight / view.height.value).floor().toUInt64(),
                        restAmount / ((amount + rotatedAmount) * UInt64.two),
                        (hollowSquareSpace.height / height).floor().toUInt64()
                    )
                    if (heightAmount == UInt64.zero) {
                        return@find false
                    }

                    val hollowSquareAmount = (amount + rotatedAmount) * UInt64.two * heightAmount
                    if (thisRestAmount < hollowSquareAmount) {
                        return@find false
                    }

                    val rotationView = item.view(it.rotation)
                    val rotationMinZAmount = if (rotationView.minDepth eq MergeScalar.zero) {
                        UInt64.one
                    } else {
                        (rotationView.minDepth / rotationView.depth.value).ceil().toUInt64()
                    }
                    if (rotatedAmount < rotationMinZAmount) {
                        return@find false
                    }

                    if ((scalar(hollowSquareAmount.toULong()) * item.weight) gr restWeight) {
                        return@find false
                    }

                    val notHollowAmount = if (config.mergeWithRotation) {
                        max(
                            (hollowSquareSpace.depth / depth).floor().toUInt64() * (hollowSquareSpace.width / width).floor().toUInt64(),
                            (hollowSquareSpace.depth / width).floor().toUInt64() * (hollowSquareSpace.width / depth).floor().toUInt64()
                        )
                    } else {
                        (hollowSquareSpace.depth / depth).floor().toUInt64() * (hollowSquareSpace.width / width).floor().toUInt64()
                    } * heightAmount
                    if (hollowSquareAmount < notHollowAmount) {
                        return@find false
                    }

                    return@find true
                } ?: break

                val view = item.view(orientation)
                val depth = orientation.depth(item)
                val width = orientation.width(item)
                val height = orientation.height(item)
                val hollowSquareSpace = space.restSpace(vector3(z = depth))
                val amount = min((hollowSquareSpace.width - depth) / width, depth / width).floor().toUInt64()
                val rotatedAmount = min((hollowSquareSpace.depth - depth) / width, depth / width).floor().toUInt64()
                val heightAmount = min(
                    view.maxLayer,
                    (view.maxHeight / view.height.value).floor().toUInt64(),
                    restAmount / ((amount + rotatedAmount) * UInt64.two),
                    (hollowSquareSpace.height / height).floor().toUInt64()
                )

                val placements = ArrayList<ItemPlacement3>()
                placements.addAll(
                    (UInt64.zero until amount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j -> QuantityPlacement3(item.view(orientation).copy(), point3(x = scalar(i.toULong()) * width, y = scalar(j.toULong()) * height)) }
                        }
                )
                placements.addAll(
                    (UInt64.zero until rotatedAmount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j ->
                                    QuantityPlacement3(
                                        item.view(orientation.rotation).copy(),
                                        point3(x = scalar(amount.toULong()) * width, y = scalar(j.toULong()) * height, z = scalar(i.toULong()) * width)
                                    )
                                }
                        }
                )
                placements.addAll(
                    (UInt64.zero until rotatedAmount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j -> QuantityPlacement3(item.view(orientation.rotation).copy(), point3(y = scalar(j.toULong()) * height, z = depth + scalar(i.toULong()) * width)) }
                        }
                )
                placements.addAll(
                    (UInt64.zero until amount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j ->
                                    QuantityPlacement3(
                                        item.view(orientation).copy(),
                                        point3(x = depth + scalar(i.toULong()) * width, y = scalar(j.toULong()) * height, z = scalar(rotatedAmount.toULong()) * width)
                                    )
                                }
                        }
                )
                mergedItems.add(HollowSquareBlock(placements))
                if (thisRestAmount == UInt64.maximum) {
                    break
                }
                thisRestAmount -= UInt64(placements.size)
            }

            restItems[item] = thisRestAmount
        }
        return Pair(mergedItems, restItems)
    }

    @JvmName("dumpItems")
    fun dump(cuboids: List<Cuboid<*>>): List<Item> {
        return cuboids.flatMap {
            when (it) {
                is Item -> listOf(it)
                is ItemContainer<*> -> it.items.map { item -> item.unit }
                else -> emptyList()
            }
        }
    }

    @JvmName("dumpPlacements")
    fun dump(
        placements: List<QuantityPlacement3<*>>,
        offset: QuantityVector3 = vector3()
    ): List<ItemPlacement3> {
        return placements.map {
            when (it.unit) {
                is Item -> {
                    it.toItemPlacementOrNull()?.let { itemPlacement ->
                        listOf(
                            QuantityPlacement3(
                                itemPlacement.view,
                                itemPlacement.position + offset
                            )
                        )
                    } ?: emptyList()
                }

                is Block -> {
                    val block = it.unit as Block
                    block.units.dump(it.position + offset)
                }

                is BinLayer -> {
                    val layer = it.unit as BinLayer
                    layer.units.dump(it.position + offset)
                }

                is PalletLayer -> {
                    val layer = it.unit as PalletLayer
                    layer.units.dump(it.position + offset)
                }

                else -> emptyList()
            }
        }.flatten()
    }
}
