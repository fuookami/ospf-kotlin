package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class SimpleBlockGenerator(
    val config: Config
) {
    data class Config(
        val mergeAsPatternBlock: Boolean = true,
        val withRotation: Boolean = true,
        val withRemainder: Boolean = false
    ) {
        companion object {
            operator fun invoke(builder: ConfigBuilder): Config {
                return Config().new(builder)
            }
        }

        fun new(
            mergeAsPatternBlock: Boolean? = null,
            withRotation: Boolean? = null,
            withRemainder: Boolean? = null
        ): Config {
            return Config(
                mergeAsPatternBlock = mergeAsPatternBlock ?: this.mergeAsPatternBlock,
                withRotation = withRotation ?: this.withRotation,
                withRemainder = withRemainder ?: this.withRemainder
            )
        }

        fun new(
            builder: ConfigBuilder
        ): Config {
            return new(
                mergeAsPatternBlock = builder.mergeAsPatternBlock,
                withRotation = builder.withRotation,
                withRemainder = builder.withRemainder
            )
        }
    }

    data class ConfigBuilder(
        var mergeAsPatternBlock: Boolean? = null,
        var withRotation: Boolean? = null,
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
    }

    suspend operator fun invoke(
        items: Map<Item, UInt64>,
        space: Container3Shape,
        patterns: List<Pattern>,
        restWeight: Flt64 = Flt64.infinity
    ): List<Block> {
        val blocks = ArrayList<Block>()
        for ((item, amount) in items) {
            if (config.mergeAsPatternBlock) {
                for (pattern in patterns) {
                    for (patternImpl in pattern.patterns) {
                        val patternAmount = UInt64(patternImpl.size)
                        if (patternAmount > amount) {
                            continue
                        }

                        for (i in UInt64.one..(amount / patternAmount)) {
                            when (val placementsList = pattern(
                                originItems = mapOf(Pair(item, i * patternAmount)),
                                space = space,
                                restWeight = restWeight,
                                pattern = patternImpl,
                                config = Pattern.buildConfig {
                                    withPiling = i
                                    withRemainder = false
                                }()
                            )) {
                                is Ok -> {
                                    for (placements in placementsList.value) {
                                        val block = SimpleBlock(placements)
                                        if (!blocks.any { it == block }) {
                                            blocks.add(block)
                                        }
                                    }
                                }

                                is Failed -> {
                                    // nothing to do
                                }
                            }
                        }
                    }
                }
            }

            for (orientation in item.enabledOrientations.filter { config.withRotation || !it.rotated }) {
                val view = item.view(orientation)
                val spaceMaxXAmount = (space.width / view.width).floor().toUInt64()
                val spaceMaxYAmount = (space.height / view.height).floor().toUInt64()
                val itemMaxYAmount = min(view.maxLayer, (view.maxHeight / view.height).floor().toUInt64())
                val spaceMaxZAmount = (space.depth / view.depth).floor().toUInt64()
                val itemMinZAmount = if (view.minDepth eq Flt64.zero) {
                    UInt64.one
                } else {
                    (view.minDepth / view.depth).ceil().toUInt64()
                }
                val itemMaxZAmount = (view.maxDepth / view.depth).floor().toUInt64()

                blocks.addAll(
                    simpleBlocks(
                        item = item,
                        amount = amount,
                        orientation = orientation,
                        maxXAmount = min(spaceMaxXAmount, amount),
                        maxYAmount = min(itemMaxYAmount, spaceMaxYAmount, amount),
                        minZAmount = itemMinZAmount,
                        maxZAmount = min(itemMaxZAmount, spaceMaxZAmount, amount)
                    )
                )
            }

            if (!item.enabledOrientations.any { it.category == OrientationCategory.Side } && item.enabledSideOnTop) {
                for (orientation in Orientation.entries.filter { it.category == OrientationCategory.Side }) {
                    val view = item.view(orientation)
                    val spaceMaxXAmount = (space.width / view.width).floor().toUInt64()
                    val spaceMaxYAmount = (space.height / view.height).floor().toUInt64()
                    val itemMaxYAmount = item.sideOnTopLayer
                    val spaceMaxZAmount = (space.depth / view.depth).floor().toUInt64()
                    val itemMinZAmount = if (view.minDepth eq Flt64.zero) {
                        UInt64.one
                    } else {
                        (view.minDepth / view.depth).ceil().toUInt64()
                    }
                    val itemMaxZAmount = (view.maxDepth / view.depth).floor().toUInt64()

                    blocks.addAll(
                        simpleBlocks(
                            item = item,
                            amount = amount,
                            orientation = orientation,
                            maxXAmount = min(spaceMaxXAmount, amount),
                            maxYAmount = min(spaceMaxYAmount, itemMaxYAmount, amount),
                            minZAmount = itemMinZAmount,
                            maxZAmount = min(spaceMaxZAmount, itemMaxZAmount, amount)
                        )
                    )
                }
            }

            if (!item.enabledOrientations.any { it.category == OrientationCategory.Lie } && item.enabledLieOnTop) {
                for (orientation in Orientation.entries.filter { it.category == OrientationCategory.Lie }) {
                    val view = item.view(orientation)
                    val spaceMaxXAmount = (space.width / view.width).floor().toUInt64()
                    val spaceMaxYAmount = (space.height / view.height).floor().toUInt64()
                    val itemMaxYAmount = item.lieOnTopLayer
                    val spaceMaxZAmount = (space.depth / view.depth).floor().toUInt64()
                    val itemMinZAmount = if (view.minDepth eq Flt64.zero) {
                        UInt64.one
                    } else {
                        (view.minDepth / view.depth).ceil().toUInt64()
                    }
                    val itemMaxZAmount = (view.maxDepth / view.depth).floor().toUInt64()

                    blocks.addAll(
                        simpleBlocks(
                            item = item,
                            amount = amount,
                            orientation = orientation,
                            maxXAmount = min(spaceMaxXAmount, amount),
                            maxYAmount = min(spaceMaxYAmount, itemMaxYAmount, amount),
                            minZAmount = itemMinZAmount,
                            maxZAmount = min(spaceMaxZAmount, itemMaxZAmount, amount)
                        )
                    )
                }
            }
        }
        return blocks
    }

    private fun simpleBlocks(
        item: Item,
        amount: UInt64,
        orientation: Orientation,
        maxXAmount: UInt64,
        maxYAmount: UInt64,
        minZAmount: UInt64,
        maxZAmount: UInt64
    ): List<Block> {
        if (maxXAmount == UInt64.zero || maxYAmount == UInt64.zero || maxZAmount == UInt64.zero) {
            return emptyList()
        }

        val blocks = ArrayList<Block>()
        for (i in UInt64.one..maxXAmount) {
            for (j in UInt64.one..maxYAmount) {
                for (k in minZAmount..maxZAmount) {
                    val placements = ArrayList<Placement3<Item>>()
                    for (p in UInt64.zero until i) {
                        val x = orientation.width(item) * p.toFlt64()
                        for (q in UInt64.zero until j) {
                            val y = orientation.height(item) * q.toFlt64()
                            for (m in UInt64.zero until k) {
                                val z = orientation.depth(item) * m.toFlt64()
                                placements.add(Placement3(item.view(orientation), point3(x, y, z)))
                            }
                        }
                    }
                    blocks.add(SimpleBlock(placements))

                    if (i * j * k > amount) {
                        break
                    }
                }
            }
        }
        if (config.withRemainder) {
            val remainder = (minZAmount..maxZAmount).minOf { amount % (maxXAmount * maxYAmount * it) }

            if (remainder != UInt64.zero && remainder ls (maxXAmount * maxYAmount) && minZAmount == UInt64.one) {
                val remainderMaxYAmount = remainder / maxXAmount
                val placements = ArrayList<Placement3<Item>>()
                if (remainderMaxYAmount != UInt64.zero) {
                    for (i in UInt64.zero until maxXAmount) {
                        val x = orientation.width(item) * i.toFlt64()
                        for (j in UInt64.zero until remainderMaxYAmount) {
                            val y = orientation.height(item) * j.toFlt64()
                            placements.add(Placement3(item.view(orientation), point3(x = x, y = y)))
                        }
                    }
                }
                val remainderPlacements = ArrayList<Placement3<Item>>()
                if ((remainder % maxXAmount) != UInt64.zero) {
                    for (i in UInt64.zero until (remainder % maxXAmount)) {
                        val x = orientation.width(item) * i.toFlt64()
                        remainderPlacements.add(Placement3(item.view(orientation), point3(x = x)))
                    }
                }
                if (placements.isEmpty()) {
                    blocks.add(SimpleBlock(remainderPlacements))
                } else if (remainderPlacements.isEmpty()) {
                    blocks.add(SimpleBlock(placements))
                } else {
                    blocks.add(ComplexBlock(
                        listOf(
                            Placement3(SimpleBlock(placements).view()!!, point3()),
                            Placement3(SimpleBlock(remainderPlacements).view()!!, point3(y = orientation.height(item) * remainderMaxYAmount.toFlt64()))
                        )
                    ))
                }
            } else if (remainder != UInt64.zero && (remainder / (maxXAmount * maxYAmount)) geq (minZAmount - UInt64.zero)) {
                val remainderMaxZAmount = remainder / (maxXAmount * maxYAmount)
                val remainderMaxYAmount = (remainder % (maxXAmount * maxYAmount)) / maxXAmount
                val placements = ArrayList<Placement3<Item>>()
                for (i in UInt64.zero until maxXAmount) {
                    val x = orientation.width(item) * i.toFlt64()
                    for (j in UInt64.zero until maxYAmount) {
                        val y = orientation.height(item) * j.toFlt64()
                        for (k in UInt64.zero until remainderMaxZAmount) {
                            val z = orientation.depth(item) * k.toFlt64()
                            placements.add(Placement3(item.view(orientation), point3(x = x, y = y, z = z)))
                        }
                    }
                }
                val remainderPlacements = ArrayList<Placement3<Item>>()
                if (remainderMaxYAmount != UInt64.zero) {
                    for (i in UInt64.zero until maxXAmount) {
                        val x = orientation.width(item) * i.toFlt64()
                        for (j in UInt64.zero until remainderMaxYAmount) {
                            val y = orientation.height(item) * j.toFlt64()
                            remainderPlacements.add(Placement3(item.view(orientation), point3(x = x, y = y)))
                        }
                    }
                }
                val remainderRemainderPlacements = ArrayList<Placement3<Item>>()
                if (((remainder % (maxXAmount * maxYAmount)) % maxXAmount) != UInt64.zero) {
                    for (i in UInt64.zero until ((remainder % (maxXAmount * maxYAmount)) % maxXAmount)) {
                        val x = orientation.width(item) * i.toFlt64()
                        remainderRemainderPlacements.add(Placement3(item.view(orientation), point3(x = x)))
                    }
                }
                val remainderBlocks = ArrayList<BlockPlacement3>()
                remainderBlocks.add(Placement3(SimpleBlock(placements).view()!!, point3()))
                if (remainderPlacements.isEmpty()) {
                    remainderBlocks.add(Placement3(
                        SimpleBlock(remainderPlacements).view()!!,
                        point3(z = orientation.depth(item) * remainderMaxZAmount.toFlt64())
                    ))
                }
                if (remainderRemainderPlacements.isEmpty()) {
                    remainderBlocks.add(Placement3(
                        SimpleBlock(remainderRemainderPlacements).view()!!,
                        point3(y = orientation.height(item) * remainderMaxYAmount.toFlt64(), z = orientation.depth(item) * remainderMaxZAmount.toFlt64())
                    ))
                }
                blocks.add(ComplexBlock(remainderBlocks))
            }
        }
        return blocks
    }
}
