/**
 * Item merger service.
 * 货物合并服务。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
/**
 * scalar.
 * scalar。
 *
 * @param value 数值 / Number value
 * @return 浮点扩展值 / Floating-point extended value
*/
private fun scalar(value: Number): FltX = FltX(value.toDouble())

/**
 * scalar.
 * scalar。
 *
 * @param value 无符号长整型值 / Unsigned long value
 * @return 浮点扩展值 / Floating-point extended value
*/
private fun scalar(value: ULong): FltX = FltX(value.toDouble())

/**
 * 将 [Try] 的失败分支转换为 [Ret] 类型，成功分支返回 null。
 * Converts the failure branch of [Try] to [Ret], returning null for the success branch.
 *
 * @return 成功时返回 null（由调用方继续处理），失败时返回对应的 [Failed] 或 [Fatal] /
 *         null on success (caller continues processing), [Failed] or [Fatal] on failure
*/
private fun <T> Try.failureAsRet(): Ret<T>? {
    return when (this) {
        is Ok -> null
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

/**
 * ItemMerger object.
 * ItemMerger对象。
*/
data object ItemMerger {

/**
 * Config data class.
 * Config数据类。
 *
 * @param mergeFillerWhenOnlyFiller 是否仅合并填充物 / Whether to merge filler only
 * @param mergeWithRotation 是否允许旋转合并 / Whether to merge with rotation
 * @param mergeAsPatternBlock 是否合并为图案方块 / Whether to merge as pattern block
 * @param mergeAsHollowSquareBlock 是否合并为空心方块 / Whether to merge as hollow square block
 * @param mergeAsPile 是否合并为堆叠 / Whether to merge as pile
 * @param mergeAsBlock 是否合并为方块 / Whether to merge as block
 * @param orientationOrder 朝向顺序 / Orientation order
 * @param patternConfig 图案配置构建器 / Pattern config builder
*/
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

/**
 * new.
 * new。
 *
 * @param mergeFillerWhenOnlyFiller 是否仅合并填充物 / Whether to merge filler only
 * @param mergeWithRotation 是否允许旋转合并 / Whether to merge with rotation
 * @param mergeAsPatternBlock 是否合并为图案方块 / Whether to merge as pattern block
 * @param mergeAsHollowSquareBlock 是否合并为空心方块 / Whether to merge as hollow square block
 * @param mergeAsPile 是否合并为堆叠 / Whether to merge as pile
 * @param mergeAsBlock 是否合并为方块 / Whether to merge as block
 * @param orientationOrder 朝向顺序 / Orientation order
 * @param patternConfig 图案配置构建器 / Pattern config builder
 * @return 新配置 / New configuration
*/
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

/**
 * new.
 * new。
 *
 * @param builder 配置构建器 / Config builder
 * @return 新配置 / New configuration
*/
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

/**
 * ConfigBuilder data class.
 * ConfigBuilder数据类。
 *
 * @param mergeFillerWhenOnlyFiller 是否仅合并填充物 / Whether to merge filler only
 * @param mergeWithRotation 是否允许旋转合并 / Whether to merge with rotation
 * @param mergeAsPatternBlock 是否合并为图案方块 / Whether to merge as pattern block
 * @param mergeAsHollowSquareBlock 是否合并为空心方块 / Whether to merge as hollow square block
 * @param mergeAsPile 是否合并为堆叠 / Whether to merge as pile
 * @param mergeAsBlock 是否合并为方块 / Whether to merge as block
 * @param orientationOrder 朝向顺序 / Orientation order
 * @param patternConfig 图案配置构建器 / Pattern config builder
*/
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

/**
 * Builds config.
 * 构建Config。
 *
 * @param builder 配置构建器 DSL / Config builder DSL
 * @return 配置构建器 / Config builder
*/
    fun buildConfig(builder: ConfigBuilder.() -> Unit): ConfigBuilder {
        val config = ConfigBuilder()
        builder(config)
        return config
    }

    /**
     * Merges items into merge units based on bin type and patterns.
     * 中文根据箱型和模式将物品合并为合并单元。
     *
     * @param items the list of items to merge / 待合并的物品列表
     * @param binType the bin type for merging / 合并使用的箱型
     * @param patterns the packing patterns / 装箱模式
     * @param predicate optional filter predicate for items / 可选的物品过滤谓词
     * @param fillerPredicate optional filter predicate for filler items / 可选的填充物品过滤谓词
     * @param config the merge configuration / 合并配置
     * @return the list of item merge units / 物品合并单元列表
    */
    suspend fun merge(
        items: List<Item>,
        binType: BinType<FltX>,
        patterns: List<Pattern>,
        predicate: Predicate<Item>? = null,
        fillerPredicate: Predicate<Item>? = null,
        config: Config = Config()
    ): Ret<List<ItemMergeUnit>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items,
            path = CylinderCapabilityPath.ItemMerge
        ).failureAsRet<List<ItemMergeUnit>>()?.let { return it }
        return merge(
            items = items,
            space = binType.asContainer3Shape(),
            restWeight = binType.capacity.value,
            patterns = patterns,
            predicate = predicate,
            fillerPredicate = fillerPredicate,
            config = config
        )
    }

    /**
     * 合并物品，处理圆柱约束。
     * Merge items, handling cylinder constraints.
     *
     * @param items 待合并的物品列表 / Items to merge
     * @param space 容器空间 / Container space
     * @param restWeight 剩余重量约束 / Remaining weight constraint
     * @param patterns 可用图案列表 / Available patterns
     * @param predicate 物品过滤谓词 / Item filter predicate
     * @param fillerPredicate 填充物过滤谓词 / Filler filter predicate
     * @param config 合并配置 / Merge configuration
     * @return 合并结果 / Merge result
    */
    suspend fun merge(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: FltX,
        patterns: List<Pattern>,
        predicate: Predicate<Item>? = null,
        fillerPredicate: Predicate<Item>? = null,
        config: Config = Config()
    ): Ret<List<ItemMergeUnit>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items,
            path = CylinderCapabilityPath.ItemMerge
        ).failureAsRet<List<ItemMergeUnit>>()?.let { return it }
        val withFillerMerging = config.mergeFillerWhenOnlyFiller && items.any { item -> fillerPredicate?.invoke(item) == false }

        var restItems = if (!withFillerMerging) {
            items.filter { item -> predicate?.let { it(item) } != false && fillerPredicate?.let { it(item) } != true }
        } else {
            items.filter { item -> predicate?.let { it(item) } != false }
        }
        val mergedItems = ArrayList<ItemMergeUnit>()

        if (config.mergeAsPatternBlock) {
            val (thisMergedItems, thisRestItems) = when (val result = mergePatternBlocks(
                items = restItems,
                space = space,
                patterns = patterns,
                restWeight = restWeight,
                patternConfig = config.patternConfig
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        if (config.mergeAsHollowSquareBlock) {
            val (thisMergedItems, thisRestItems) = when (val result = mergeHollowSquareBlocks(
                items = restItems,
                space = space,
                restWeight = restWeight
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        if (config.mergeAsBlock) {
            val (thisMergedItems, thisRestItems) = when (val result = mergeBlocks(
                items = restItems,
                space = space,
                restWeight = restWeight,
                config = config
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        if (config.mergeAsPile) {
            val (thisMergedItems, thisRestItems) = when (val result = mergePiles(
                items = restItems,
                space = space,
                restWeight = restWeight
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            mergedItems.addAll(thisMergedItems)
            restItems = thisRestItems
        }

        mergedItems.addAll(restItems)
        if (!withFillerMerging) {
            mergedItems.addAll(items.filter { item -> predicate?.let { it(item) } == false || fillerPredicate?.let { it(item) } == true })
        }
        return Ok(mergedItems.toList())
    }

    /**
     * 合并堆叠物品。
     * Merge piled items.
     *
     * @param items 待合并的物品列表 / Items to merge
     * @param space 容器空间 / Container space
     * @param restWeight 剩余重量约束 / Remaining weight constraint
     * @return 合并结果 / Merge result
    */
    fun mergePiles(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: FltX = FltX.maximum
    ): Ret<Pair<List<Pile>, List<Item>>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items,
            path = CylinderCapabilityPath.ItemMergePiles
        ).failureAsRet<Pair<List<Pile>, List<Item>>>()?.let { return it }
        val averagePileBottomArea = items.fold(FltX.zero) { acc, item -> acc + Bottom.shape(item).area.value } / scalar(items.size)
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
                    if (pileItems.sumOfQuantity { it.weight } geq averagePileWeight) {
                        break
                    }

                    val (layer, height) = Pile.layer(enabledItems[j], pileItems)
                    if (enabledItems[j].enabledStackingOn(
                            bottomItem = pileItems.last(),
                            layer = layer,
                            height = height,
                            space = space.restSpace(QuantityVector3(
                                x = FltX.zero * space.width.unit,
                                y = pileItems.sumOfQuantity { it.height },
                                z = FltX.zero * space.depth.unit
                            ))
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
                            space = space.restSpace(QuantityVector3(
                                x = FltX.zero * space.width.unit,
                                y = pileItems.sumOfQuantity { it.height },
                                z = FltX.zero * space.depth.unit
                            ))
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
        return Ok(Pair(mergedItems, restItems.map { it.unit }))
    }

/**
 * mergeBlocks.
 * mergeBlocks。
 *
 * @param items 待合并的物品列表 / Items to merge
 * @param space 容器空间 / Container space
 * @param restWeight 剩余重量约束 / Remaining weight constraint
 * @param config 合并配置 / Merge configuration
 * @return 合并后的方块列表与剩余物品 / Merged blocks and remaining items
*/
    fun mergeBlocks(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: FltX = FltX.maximum,
        config: Config = Config()
    ): Ret<Pair<List<SimpleBlock>, List<Item>>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items,
            path = CylinderCapabilityPath.ItemMergeBlocks
        ).failureAsRet<Pair<List<SimpleBlock>, List<Item>>>()?.let { return it }
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
                val zAmount = if (view.minDepth eq FltX.zero) {
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
                    val placements = ArrayList<QuantityPlacement3<Item, FltX>>()
                    for (i in UInt64.zero until xAmount) {
                        val x = scalar(i.toULong()) * view.width
                        for (j in UInt64.zero until yAmount) {
                            val y = scalar(j.toULong()) * view.height
                            for (k in UInt64.zero until zAmount) {
                                val z = scalar(k.toULong()) * view.depth
                                placements.add(
                                    itemPlacement3Of(
                                        view = view,
                                        position = point3(
                                            x = x,
                                            y = y,
                                            z = z
                                        )
                                    )
                                )
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
        return Ok(Pair(mergedItems, restItems.flatMap { it.value }))
    }

    /**
     * 合并图案方块。
     * Merge pattern blocks.
     *
     * @param items 待合并的物品列表 / Items to merge
     * @param space 容器空间 / Container space
     * @param patterns 可用图案列表 / Available patterns
     * @param restWeight 剩余重量约束 / Remaining weight constraint
     * @param patternConfig 图案配置 / Pattern configuration
     * @return 合并后的通用方块列表与剩余物品列表 / Merged common blocks and remaining items
    */
    suspend fun mergePatternBlocks(
        items: List<Item>,
        space: AbstractContainer3Shape,
        patterns: List<Pattern>,
        restWeight: FltX = FltX.maximum,
        patternConfig: Pattern.ConfigBuilder = Pattern.ConfigBuilder()
    ): Ret<Pair<List<CommonBlock>, List<Item>>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items,
            path = CylinderCapabilityPath.ItemMergePatternBlocks
        ).failureAsRet<Pair<List<CommonBlock>, List<Item>>>()?.let { return it }
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
        return Ok(Pair(mergedItems, restItems))
    }

    /**
     * 合并空心方块。
     * Merge hollow square blocks.
     *
     * @param items 待合并的物品列表 / Items to merge
     * @param space 容器空间 / Container space
     * @param restWeight 剩余重量约束 / Remaining weight constraint
     * @param config 合并配置 / Merge configuration
     * @return 合并后的空心方块列表与剩余物品列表 / Merged hollow square blocks and remaining items
    */
    fun mergeHollowSquareBlocks(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: FltX = FltX.maximum,
        config: Config = Config()
    ): Ret<Pair<List<HollowSquareBlock>, List<Item>>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items,
            path = CylinderCapabilityPath.ItemMergeHollowSquareBlocks
        ).failureAsRet<Pair<List<HollowSquareBlock>, List<Item>>>()?.let { return it }
        val restItems = items.groupBy { it }.map { Pair(it.key, UInt64(it.value.size)) }.toMap()
        return when (val result = mergeHollowSquareBlocks(
            items = restItems,
            space = space,
            restWeight = restWeight,
            config = config
        )) {
            is Ok -> Ok(Pair(result.value.first, result.value.second.flatMap { item -> (UInt64.zero until item.value).map { item.key } }))
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 合并空心方块的 Map 重载。
     * Map overload for merging hollow square blocks.
     *
     * @param items 物品数量映射 / Item amount map
     * @param space 容器空间 / Container space
     * @param restWeight 剩余重量约束 / Remaining weight constraint
     * @param config 合并配置 / Merge configuration
     * @return 合并后的空心方块列表与剩余物品映射 / Merged hollow square blocks and remaining item map
    */
    fun mergeHollowSquareBlocks(
        items: Map<Item, UInt64>,
        space: AbstractContainer3Shape,
        restWeight: FltX = FltX.maximum,
        config: Config = Config()
    ): Ret<Pair<List<HollowSquareBlock>, Map<Item, UInt64>>> {
        requireNoCylinderItemsForCuboidOnlyPath(
            items = items.keys,
            path = CylinderCapabilityPath.ItemMergeHollowSquareBlocks
        ).failureAsRet<Pair<List<HollowSquareBlock>, Map<Item, UInt64>>>()?.let { return it }
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
                        depth = depth * FltX.two - (depth % width)
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
                    val rotationMinZAmount = if (rotationView.minDepth eq FltX.zero) {
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
                val hollowSquareSpace = space.restSpace(QuantityVector3(
                    x = FltX.zero * space.width.unit,
                    y = FltX.zero * space.height.unit,
                    z = depth
                ))
                val amount = min((hollowSquareSpace.width - depth) / width, depth / width).floor().toUInt64()
                val rotatedAmount = min((hollowSquareSpace.depth - depth) / width, depth / width).floor().toUInt64()
                val heightAmount = min(
                    view.maxLayer,
                    (view.maxHeight / view.height.value).floor().toUInt64(),
                    restAmount / ((amount + rotatedAmount) * UInt64.two),
                    (hollowSquareSpace.height / height).floor().toUInt64()
                )

                val placements = ArrayList<QuantityPlacement3<Item, FltX>>()
                placements.addAll(
                    (UInt64.zero until amount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j ->
                                    itemPlacement3Of(
                                        view = item.view(orientation).copy(),
                                        position = QuantityPoint3(
                                            x = scalar(i.toULong()) * width,
                                            y = scalar(j.toULong()) * height,
                                            z = FltX.zero * depth.unit
                                        )
                                    )
                                }
                        }
                )
                placements.addAll(
                    (UInt64.zero until rotatedAmount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j ->
                                    itemPlacement3Of(
                                        view = item.view(orientation.rotation).copy(),
                                        position = QuantityPoint3(
                                            x = scalar(amount.toULong()) * width,
                                            y = scalar(j.toULong()) * height,
                                            z = scalar(i.toULong()) * width
                                        )
                                    )
                                }
                        }
                )
                placements.addAll(
                    (UInt64.zero until rotatedAmount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j ->
                                    itemPlacement3Of(
                                        view = item.view(orientation.rotation).copy(),
                                        position = QuantityPoint3(
                                            x = FltX.zero * width.unit,
                                            y = scalar(j.toULong()) * height,
                                            z = depth + scalar(i.toULong()) * width
                                        )
                                    )
                                }
                        }
                )
                placements.addAll(
                    (UInt64.zero until amount)
                        .flatMap { i ->
                            (UInt64.zero until heightAmount)
                                .map { j ->
                                    itemPlacement3Of(
                                        view = item.view(orientation).copy(),
                                        position = QuantityPoint3(
                                            x = depth + scalar(i.toULong()) * width,
                                            y = scalar(j.toULong()) * height,
                                            z = scalar(rotatedAmount.toULong()) * width
                                        )
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
        return Ok(Pair(mergedItems, restItems))
    }

    /**
     * 将合并结果展开为 Item 列表。
     * 仅支持长方体（Cuboid-only）：圆柱在此路径被显式拒绝。
     * Flatten merged results to Item list.
     * Cuboid-only: cylinders are explicitly rejected in this path.
     *
     * @param units 合并单元列表 / The merge units to flatten
     * @return 展开后的物品列表 / The flattened item list
    */
    @JvmName("dumpItems")
    fun dump(units: List<ItemMergeUnit>): List<Item> {
        return units.flatMap { unit ->
            when (unit) {
                is Item -> listOf(unit)
                is ItemContainer<*> -> unit.items.map { item -> item.unit }
            }
        }
    }

    /**
     * 将放置列表展开为货物放置列表，处理嵌套容器。
     * Flatten placement list to item placements, handling nested containers.
     *
     * @param placements 放置列表 / placement list
     * @param offset 位置偏移量 / position offset
     * @return 货物放置列表 / item placement list
    */
    @JvmName("dumpPlacements")
    fun dump(
        placements: List<QuantityPlacement3<*, FltX>>,
        offset: QuantityVector3<FltX> = vector3FltX()
    ): List<QuantityPlacement3<Item, FltX>> {
        return placements.map {
            when (it.unit) {
                is Item -> {
                    it.toItemPlacementOrNull()?.let { itemPlacement ->
                        listOf(
                            itemPlacement3Of(
                                view = itemPlacement.view as ItemView,
                                position = itemPlacement.position + offset
                            )
                        )
                    } ?: emptyList<QuantityPlacement3<Item, FltX>>()
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

                else -> emptyList<QuantityPlacement3<Item, FltX>>()
            }
        }.flatten()
    }
}
