@file:Suppress("DEPRECATION")

/**
 * 深度优先搜索算法。
 * Depth-first search algorithm.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.model.Space
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.utils.functional.sortedWithThreeWayComparator
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.ord
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger

private fun infraPoint3(
    x: InfraNumber = infraZero(),
    y: InfraNumber = infraZero(),
    z: InfraNumber = infraZero()
): Point<Dim3, InfraNumber> {
    return Point(x, y, z)
}

internal fun fitness(space: Space, block: Block): Quantity<InfraNumber> {
    return when (space.forwardLink?.first ?: Side) {
        is Front -> {
            space.width + space.depth - block.width - block.depth
        }

        is Bottom -> {
            space.width + space.depth + space.height - block.width - block.depth - block.height
        }

        is Side -> {
            space.width - block.width
        }
    }
}

internal fun compareWithFitness(
    space: Space,
    lhs: Block,
    rhs: Block,
    fitness: (Space, Block) -> Quantity<InfraNumber>
): Order {
    val lhsValue = fitness(space, lhs)
    val rhsValue = fitness(space, rhs)
    return lhsValue ord rhsValue
}

private fun compareSpace(lhs: Space, rhs: Space): Order {
    when (val result = lhs.position[2] ord rhs.position[2]) {
        Order.Equal -> {}
        else -> {
            return result
        }
    }
    when (val result = lhs.position[0] ord rhs.position[0]) {
        Order.Equal -> {}
        else -> {
            return result
        }
    }
    return lhs.position[1] ord rhs.position[1]
}

class DepthFirstSearchAlgorithm(
    val config: Config
) {
    private val logger = logger()

    data class Config(
        val blockComparator: (Space, Block, Block) -> Order = { space, lhs, rhs ->
            compareWithFitness(space, lhs, rhs) { spc, block -> fitness(spc, block) }
        },
        val spaceDirectionOrder: List<ProjectivePlane> = listOf(Front, Bottom, Side),
        val spaceComparator: (Space, Space, Block?) -> Order = { lhs, rhs, block ->
            if (block != null) {
                fitness(lhs, block) ord fitness(rhs, block)
            } else {
                compareSpace(lhs, rhs)
            }
        },
        val branch: UInt64 = UInt64.maximum
    )

    suspend operator fun invoke(
        items: Map<Item, UInt64>,
        bins: Map<BinType, UInt64>,
        blockTable: List<Block>
    ): Pair<List<BlockBin>, List<Item>> {
        requireNoCylinderItemsForCuboidSearch(
            items = items
        )
        val restItems = items.toMutableMap()
        val availableBins = bins.toMutableMap()

        val usedBins = ArrayList<BlockBin>()
        try {
            coroutineScope {
                while (!finished(restItems)) {
                    val binType = availableBins.asSequence().find { it.value != UInt64.zero }?.key ?: break
                    val promise = Channel<List<Space>>()
                    launch(Dispatchers.Default) {
                        try {
                            pack(
                                promise = promise,
                                items = restItems,
                                shape = binType,
                                fixedSpaces = emptyList(),
                                blockTable = blockTable,
                                branch = UInt64.one
                            )
                        } catch (e: CancellationException) {
                            logger.trace { "Block Loading DFS was stopped by controller." }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            logger.debug { "Block Loading DFS Error ${e.message}" }
                        } finally {
                            promise.close()
                        }
                    }

                    var spaces: List<Space>? = null
                    try {
                        for (result in promise) {
                            spaces = result
                            break
                        }
                    } catch (e: CancellationException) {
                        logger.trace { "Block Loading DFS was stopped by controller." }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logger.debug { "Block Loading DFS Error ${e.message}" }
                    } finally {
                        promise.close()
                    }
                    if (spaces.isNullOrEmpty()) {
                        break
                    }

                    val bin = blockBinOf(
                        shape = binType,
                        units = spaces.map { space ->
                            blockPlacement3Of(
                                view = space.block!!.view()!!,
                                position = point3(space.position)
                            )
                        }
                    )
                    for ((item, amount) in bin.amounts) {
                        restItems[item as Item] = restItems[item]!! - amount
                    }
                    availableBins[binType] = availableBins[binType]!! - UInt64.one
                    usedBins.add(bin)
                }
                cancel()
            }
        } catch (e: CancellationException) {
            logger.trace { "Block Loading DFS was stopped by controller." }
        }
        return Pair(usedBins, restItems.flatten())
    }

        operator fun invoke(
        items: Map<Item, UInt64>,
        shape: AbstractContainer3Shape,
        blockTable: List<Block>,
        fixedSpaces: List<Space> = emptyList(),
        scope: CoroutineScope = bpp3dBlockLoadingAsyncScope
    ): ChannelGuard<List<Space>> {
        requireNoCylinderItemsForCuboidSearch(
            items = items
        )
        val restItems = items.toMutableMap()
        val promise = Channel<List<Space>>()
        scope.launch(Dispatchers.Default) {
            try {
                pack(
                    promise = promise,
                    items = restItems,
                    shape = shape,
                    fixedSpaces = fixedSpaces,
                    blockTable = blockTable
                )
            } catch (e: CancellationException) {
                logger.trace { "Block Loading DFS was stopped by controller." }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.debug { "Block Loading DFS Error ${e.message}" }
            } finally {
                promise.close()
            }
        }
        return ChannelGuard(promise)
    }

        operator fun invoke(
        items: Map<Item, UInt64>,
        blocks: List<Block>,
        shape: AbstractContainer3Shape,
        fixedSpaces: List<Space> = emptyList(),
        scope: CoroutineScope = bpp3dBlockLoadingAsyncScope
    ): ChannelGuard<List<Space>> {
        requireNoCylinderItemsForCuboidSearch(
            items = items
        )
        val restItems = items.toMutableMap()

        val promise = Channel<List<Space>>()
        scope.launch(Dispatchers.Default) {
            try {
                pack(
                    promise = promise,
                    items = restItems,
                    shape = shape,
                    blocks = blocks,
                    fixedSpaces = fixedSpaces,
                )
            } catch (e: CancellationException) {
                logger.trace { "Block Loading DFS was stopped by controller." }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.debug { "Block Loading DFS Error ${e.message}" }
            } finally {
                promise.close()
            }
        }
        return ChannelGuard(promise)
    }

        operator fun invoke(
        blocks: List<Block>,
        shape: Container3Shape,
        fixedSpaces: List<Space> = emptyList(),
        scope: CoroutineScope = bpp3dBlockLoadingAsyncScope
    ): ChannelGuard<List<Space>> {
        return this(
            items = blocks.flatMap { block -> block.amounts.keys.map { it as Item } }.associateWith { UInt64.maximum },
            blocks = blocks,
            shape = shape,
            fixedSpaces = fixedSpaces,
            scope = scope
        )
    }

    @JvmName("packBlockTable")
        private suspend fun pack(
        promise: Channel<List<Space>>,
        items: Map<Item, UInt64>,
        shape: AbstractContainer3Shape,
        fixedSpaces: List<Space>,
        blockTable: List<Block>,
        branch: UInt64 = config.branch
    ) {
        if (!blockTable.any { shape.enabled(it) && enough(items, it) }) {
            promise.close()
            return
        }

        val stack = arrayListOf(
            if (fixedSpaces.isEmpty()) {
                Pair(mutableListOf(Space(infraPoint3(), shape)), emptyList())
            } else {
                fixedSpaces
                    .flatMap {
                        it.links.mapNotNull { (_, space) ->
                            if (fixedSpaces.all { fixedSpace -> fixedSpace.position != space.position }) {
                                space
                            } else {
                                null
                            }
                        }
                    }
                    .sortedWithThreeWayComparator { lhs, rhs -> config.spaceComparator(lhs, rhs, null) }
                    .reversed()
                    .toMutableList()
                    .let { Pair(it, emptyList<Space>()) }
            }
        )

        val cache = HashMap<Space, List<Block>>()

        while (stack.isNotEmpty()) {
            val (enabledSpaces, thisFixedSpaces) = stack.removeAt(stack.lastIndex)
            val restItems = items.toMutableMap()
            for (space in thisFixedSpaces) {
                for ((item, amount) in space.block!!.amounts) {
                    item as Item
                    restItems[item] = restItems[item]!! - amount
                }
            }

            var flag = false
            while (enabledSpaces.isNotEmpty()) {
                val enabledSpace = enabledSpaces.removeAt(stack.lastIndex)

                val thisBlocks = if (cache.containsKey(enabledSpace)) {
                    cache[enabledSpace]!!
                } else {
                    val enabledBlocks = blockTable
                        .filter { enabledSpace.shape.enabled(it) && enough(restItems, it) }
                        .sortedWithThreeWayComparator { lhs, rhs ->
                            when (val value = lhs.packageType ord rhs.packageType) {
                                Order.Equal -> {}
                                else -> {
                                    return@sortedWithThreeWayComparator value
                                }
                            }
                            when (val value = config.blockComparator(enabledSpace, lhs, rhs)) {
                                Order.Equal -> {}
                                else -> {
                                    return@sortedWithThreeWayComparator value
                                }
                            }
                            when (val value = rhs.units.first().volume ord lhs.units.first().volume) {
                                Order.Equal -> {}
                                else -> {
                                    return@sortedWithThreeWayComparator value
                                }
                            }
                            return@sortedWithThreeWayComparator rhs.volume ord lhs.volume
                        }
                    cache[enabledSpace] = enabledBlocks
                    enabledBlocks
                }

                if (thisBlocks.isEmpty()) {
                    continue
                }

                val newSpaces = coroutineScope {
                    val results = ArrayList<Pair<MutableList<Space>, List<Space>>>()
                    if (branch == UInt64.one) {
                        for (block in thisBlocks) {
                            val usedSpace = enabledSpace.put(block) ?: continue
                            val links = usedSpace.links
                            return@coroutineScope listOf(
                                Pair(
                                    merge(enabledSpaces + config.spaceDirectionOrder.flatMap { direction ->
                                        links.mapNotNull {
                                            if (it.first == direction) {
                                                it.second
                                            } else {
                                                null
                                            }
                                        }
                                    }.reversed()).toMutableList(),
                                    thisFixedSpaces + listOf(usedSpace)
                                ),
                                Pair(
                                    (enabledSpaces + config.spaceDirectionOrder.flatMap { direction ->
                                        links.mapNotNull {
                                            if (it.first == direction) {
                                                it.second
                                            } else {
                                                null
                                            }
                                        }
                                    }.reversed()).toMutableList(),
                                    thisFixedSpaces + listOf(usedSpace)
                                )
                            )
                        }
                    } else {
                        var i = 0
                        while (UInt64(results.size) < branch) {
                            val mj = min(max(branch, UInt64(Runtime.getRuntime().availableProcessors())), UInt64((thisBlocks.size - i))).toInt()
                            val promises = ArrayList<Deferred<Pair<MutableList<Space>, List<Space>>?>>()
                            for (j in 0 until mj) {
                                promises.add(async(Dispatchers.Default) {
                                    val usedSpace = enabledSpace.put(thisBlocks[i + j]) ?: return@async null
                                    val links = usedSpace.links
                                    return@async Pair(
                                        (enabledSpaces + config.spaceDirectionOrder.flatMap { direction ->
                                            links.mapNotNull {
                                                if (it.first == direction) {
                                                    it.second
                                                } else {
                                                    null
                                                }
                                            }
                                        }.reversed()).toMutableList(),
                                        thisFixedSpaces + listOf(usedSpace)
                                    )
                                })
                            }
                            results.addAll(
                                promises
                                .mapNotNull { it.await() }
                                .flatMap { listOf(it, Pair(merge(it.first).toMutableList(), it.second)) }
                            )
                            i += mj

                            if (i == thisBlocks.size) {
                                break
                            }
                        }
                        return@coroutineScope results.reversed().take(branch.toInt())
                    }
                    null
                } ?: continue

                if (newSpaces.isEmpty()) {
                    continue
                }

                flag = true
                stack.addAll(newSpaces)
                break
            }
            if (!flag) {
                if (promise.trySend(fixedSpaces + thisFixedSpaces).isFailure) {
                    break
                }
            }
        }

        promise.close()
        return
    }

        @JvmName("packBlockSequence")
    private suspend fun pack(
        promise: Channel<List<Space>>,
        items: Map<Item, UInt64>,
        shape: AbstractContainer3Shape,
        blocks: List<Block>,
        fixedSpaces: List<Space> = emptyList(),
    ) {
        if (blocks.isEmpty()) {
            promise.close()
            return
        }

        val restItems = items.toMutableMap()
        val enabledSpaces = if (fixedSpaces.isEmpty()) {
            listOf(Space(infraPoint3(), shape))
        } else {
            fixedSpaces
                .flatMap {
                    it.links.mapNotNull { (_, space) ->
                        if (fixedSpaces.all { fixedSpace -> fixedSpace.position != space.position } && blocks.any { block -> space.shape.enabled(block) }) {
                            space
                        } else {
                            null
                        }
                    }
                }
                .sortedWithThreeWayComparator { lhs, rhs -> config.spaceComparator(lhs, rhs, null) }
                .reversed()
        }
        val spaces = blocks.map { null }.toMutableList<Space?>()
        val stack = ArrayList<Triple<Int, Space?, List<Space>>>()

        stack.add(Triple(0, null, enabledSpaces))
        stack.addAll(generateEnabledSpaces(0, blocks[0], enabledSpaces).reversed())

        while (stack.isNotEmpty()) {
            val (top, usedSpace, thisEnabledSpaces) = stack.removeAt(stack.lastIndex)
            val i = top + 1
            if (i >= blocks.size) {
                if (promise.trySend(fixedSpaces + spaces.filterNotNull()).isFailure) {
                    break
                }
            } else {
                if (usedSpace != null && spaces[top] == null) {
                    for ((item, amount) in blocks[top].amounts) {
                        item as Item
                        if (restItems[item] != UInt64.maximum) {
                            restItems[item] = (restItems[item] ?: UInt64.zero) - amount
                        }
                    }
                } else if (usedSpace == null && spaces[top] != null) {
                    for ((item, amount) in blocks[top].amounts) {
                        item as Item
                        if (restItems[item] != UInt64.maximum) {
                            restItems[item] = (restItems[item] ?: UInt64.zero) + amount
                        }
                    }
                }
                spaces[top] = usedSpace

                stack.add(Triple(i, null, thisEnabledSpaces))
                if (enough(restItems, blocks[i])) {
                    stack.addAll(generateEnabledSpaces(i, blocks[i], thisEnabledSpaces).reversed())
                }
            }
        }

        promise.close()
        return
    }

    private suspend fun generateEnabledSpaces(
        index: Int,
        block: Block,
        enabledSpaces: List<Space>
    ): List<Triple<Int, Space?, List<Space>>> {
        return coroutineScope {
            val promises = ArrayList<Deferred<Triple<Int, Space?, List<Space>>?>>()
            for (enabledSpace in enabledSpaces
                .sortedWithThreeWayComparator { lhs, rhs -> config.spaceComparator(lhs, rhs, block) }
                .take(config.branch.toInt())
            ) {
                promises.add(async(Dispatchers.Default) {
                    val thisUsedSpace = enabledSpace.put(block) ?: return@async null
                    val links = thisUsedSpace.links
                    val nextEnabledSpaces = config.spaceDirectionOrder
                        .flatMap { direction ->
                            links.mapNotNull {
                                if (it.first == direction) {
                                    it.second
                                } else {
                                    null
                                }
                            }
                        }
                        .reversed()
                    Triple(index, thisUsedSpace, enabledSpaces.filter { space -> space != enabledSpace } + nextEnabledSpaces)
                })
            }
            promises
                .mapNotNull { it.await() }
                .flatMap { listOf(it, Triple(it.first, it.second, merge(it.third))) }
        }
    }

    private fun finished(restItems: Map<Item, UInt64>): Boolean {
        for ((_, amount) in restItems) {
            if (amount != UInt64.zero) {
                return false
            }
        }
        return true
    }

    private fun enough(
        restItems: Map<Item, UInt64>,
        block: Block
    ): Boolean {
        for ((item, amount) in block.amounts) {
            if (amount > (restItems[item] ?: UInt64.zero)) {
                return false
            }
        }
        return true
    }

    private fun merge(spaces: List<Space>): List<Space> {
        val mergedSpaces = spaces.sortedWith(compareBy { it.z.toDouble() }).withIndex().toMutableList()
        for ((i, space) in mergedSpaces.withIndex()) {
            var space1 = space
            for (j in (i + 1) until mergedSpaces.size) {
                val space2 = mergedSpaces[j]

                if (space2.value.x leq space1.value.x
                    && space2.value.maxX eq space1.value.maxX
                    && space1.value.y eq space2.value.y
                    && space1.value.maxY eq space2.value.maxY
                    && space1.value.maxZ eq space2.value.z
                ) {
                    space1 = IndexedValue(
                        space1.index,
                        Space(
                            position = space1.value.position,
                            shape = Container3Shape(width = space1.value.width, height = space1.value.height, depth = space1.value.depth + space2.value.depth),
                            parentShape = Container3Shape(
                                width = space1.value.parentShape.width,
                                height = space1.value.parentShape.height,
                                depth = space1.value.shape.depth + space2.value.parentShape.depth
                            ),
                            forwardLink = space1.value.forwardLink
                        )
                    )
                    mergedSpaces[j] = IndexedValue(
                        space2.index,
                        Space(
                            position = space2.value.position,
                            shape = Container3Shape(width = (space2.value.width - space1.value.width), height = space2.value.height, depth = space2.value.depth),
                            parentShape = Container3Shape(
                                width = (space2.value.width - space1.value.width),
                                height = space2.value.parentShape.height,
                                depth = space2.value.parentShape.depth
                            ),
                            forwardLink = space2.value.forwardLink
                        )
                    )
                }
            }
            mergedSpaces[i] = space1
        }
        return mergedSpaces
            .filter { it.value.shape.volume neq infraZero() }
            .sortedWith(compareBy { it.index })
            .map { it.value }
    }
}


