@file:Suppress("DEPRECATION")

/**
 * 多层启发式搜索算法。
 * Multi-layer heuristic search algorithm.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.domain.block_loading.model.Space
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.functional.minWithThreeWayComparatorOrNull
import fuookami.ospf.kotlin.utils.functional.sortedWithThreeWayComparator
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.combinatorics.permuteAsync
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.parallel.ChannelGuard
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.logging.log4j.kotlin.logger

class MultiLayerHeuristicSearchAlgorithm(
    val config: Config
) {
    private val logger = logger()

    private val dfs = DepthFirstSearchAlgorithm(
        config = DepthFirstSearchAlgorithm.Config(
            blockComparator = { space, lhs, rhs -> config.blockComparator(space, lhs, rhs) },
            branch = UInt64.one
        )
    )

    data class Config(
        val layer: UInt64 = UInt64.two,
        val depth: UInt64 = UInt64.two,
        val branch: UInt64 = UInt64(128UL),
        val blockComparator: (Space?, Block, Block) -> Order = { space, lhs, rhs ->
            if (space != null) {
                compareWithFitness(space, lhs, rhs) { spc, block -> fitness(spc, block) }
            } else {
                when (val result = rhs.units.first().volume ord lhs.units.first().volume) {
                    Order.Equal -> {
                        rhs.volume ord lhs.volume
                    }

                    else -> {
                        result
                    }
                }
            }
        },
        val binComparator: (AbstractContainer3Shape, List<Block>, List<Block>) -> Order = { _, lhs, rhs ->
            rhs.sumOf { it.volume } ord lhs.sumOf { it.volume }
        }
    )

    suspend operator fun invoke(
        items: Map<Item, UInt64>,
        bins: Map<BinType<FltX>, UInt64>,
        blockTable: List<Block>,
    ): Pair<List<Bin<Block, FltX>>, List<Item>> {
        requireNoCylinderItemsForCuboidSearch(
            items = items
        )
        val restItems = items.toMutableMap()
        val availableBins = bins.toMutableMap()
        val usedBins = ArrayList<Bin<Block, FltX>>()
        try {
            coroutineScope {
                while (!finished(restItems)) {
                    val binType = availableBins.asSequence().find { it.value != UInt64.zero }?.key ?: break
                    val promise = Channel<List<Space>>()
                    pack(
                        promise = promise,
                        items = restItems,
                        shape = binType.asContainer3Shape(),
                        fixedSpaces = emptyList(),
                        blockTable = blockTable
                    )
                    var spaces: List<Space>? = null
                    try {
                        for (result in promise) {
                            spaces = result
                            break
                        }
                    } catch (e: CancellationException) {
                        logger.trace { "Block Loading MLHSA was stopped by controller." }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logger.debug { "Block Loading MLHSA Error ${e.message}" }
                    } finally {
                        promise.close()
                    }

                    if (spaces == null) {
                        break
                    }

                    val bin = blockBinOf(
                        shape = binType,
                        units = spaces.map { space ->
                            blockPlacement3Of(
                                view = space.block!!.view()!!,
                                position = point3FltX(space.position)
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
            logger.trace { "Block Loading MLHSA was stopped by controller." }
        }

        return Pair(usedBins, restItems.flatten())
    }

        operator fun invoke(
        items: Map<Item, UInt64>,
        shape: Container3Shape,
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
                logger.trace { "Block Loading MLHSA was stopped by controller." }
            } catch (e: Exception) {
                e.printStackTrace()
                logger.debug { "Block Loading MLHSA Error ${e.message}" }
            } finally {
                promise.close()
            }
        }
        return ChannelGuard(promise)
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

    private suspend fun pack(
        promise: Channel<List<Space>>,
        items: Map<Item, UInt64>,
        shape: AbstractContainer3Shape,
        fixedSpaces: List<Space>,
        blockTable: List<Block>
    ) {
        var heap: MutableList<List<Space>> = ArrayList()
        for (layer in (UInt64.zero until config.layer)) {
            heap = heap
                .sortedWithThreeWayComparator { lhs, rhs -> config.binComparator(shape, lhs.map { it.block!! }, rhs.map { it.block!! }) }
                .take(config.branch.toInt())
                .toMutableList()

            if (layer == UInt64.zero) {
                try {
                    coroutineScope {
                        val promises = ArrayList<Deferred<List<Space>?>>()
                        val enabledBlocks = blockTable
                            .filter { enough(items, it) }
                            .sortedWithThreeWayComparator { lhs, rhs -> config.blockComparator(null, lhs, rhs) }
                        if (enabledBlocks.isEmpty()) {
                            return@coroutineScope
                        }

                        val blocksPromise = permuteAsync(
                            input = enabledBlocks,
                            scope = this
                        )
                        try {
                            var counter = UInt64.zero
                            for (blocks in blocksPromise) {
                                promises.add(async(Dispatchers.Default) {
                                    val dfsPromise = dfs(
                                        items = items,
                                        blocks = blocks.take(config.depth.toInt()),
                                        shape = shape,
                                        fixedSpaces = fixedSpaces,
                                        scope = this@coroutineScope
                                    )
                                    try {
                                        for (thisSpaces in dfsPromise) {
                                            dfsPromise.close()
                                            return@async thisSpaces
                                        }
                                    } catch (e: CancellationException) {
                                        logger.trace { "Block Loading MLHSA was stopped by controller." }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        logger.debug { "Block Loading MLHSA Error ${e.message}" }
                                    } finally {
                                        dfsPromise.close()
                                    }
                                    return@async null
                                })
                                ++counter
                                if (counter == config.branch) {
                                    blocksPromise.close()
                                    break
                                }
                            }
                        } catch (e: CancellationException) {
                            logger.trace { "Block Loading MLHSA was stopped by controller." }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            logger.debug { "Block Loading MLHSA Error ${e.message}" }
                        } finally {
                            blocksPromise.close()
                        }
                        heap = promises.mapNotNull { it.await() }.toMutableList()
                        cancel()
                    }
                } catch (e: CancellationException) {
                    logger.trace { "Block Loading MLHSA was stopped by controller." }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.debug { "Block Loading MLHSA Error ${e.message}" }
                }
            } else {
                try {
                    coroutineScope {
                        val promises = ArrayList<Deferred<List<Space>?>>()
                        for (spaces in heap) {
                            val thisRestItems = items.toMutableMap()
                            for (space in spaces) {
                                for ((item, amount) in space.block!!.amounts) {
                                    item as Item
                                    thisRestItems[item] = thisRestItems[item]!! - amount
                                }
                            }
                            val enabledBlocks = blockTable
                                .filter { enough(thisRestItems, it) }
                                .sortedWithThreeWayComparator { lhs, rhs -> config.blockComparator(null, lhs, rhs) }
                            if (enabledBlocks.isEmpty()) {
                                continue
                            }

                            val blocksPromise = permuteAsync(
                                input = enabledBlocks,
                                scope = this
                            )
                            try {
                                var counter = UInt64.zero
                                for (blocks in blocksPromise) {
                                    promises.add(async(Dispatchers.Default) {
                                        val dfsPromise = dfs(
                                            items = thisRestItems,
                                            blocks = blocks.take(config.depth.toInt()),
                                            shape = shape,
                                            fixedSpaces = spaces,
                                            scope = this@coroutineScope
                                        )
                                        try {
                                            for (thisSpaces in dfsPromise) {
                                                dfsPromise.close()
                                                return@async thisSpaces
                                            }
                                        } catch (e: CancellationException) {
                                            logger.trace { "Block Loading MLHSA was stopped by controller." }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            logger.debug { "Block Loading MLHSA Error ${e.message}" }
                                        } finally {
                                            dfsPromise.close()
                                        }
                                        return@async null
                                    })
                                    ++counter
                                    if (counter == config.branch) {
                                        blocksPromise.close()
                                        break
                                    }
                                }
                            } catch (e: CancellationException) {
                                logger.trace { "Block Loading MLHSA was stopped by controller." }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                logger.debug { "Block Loading MLHSA Error ${e.message}" }
                            } finally {
                                blocksPromise.close()
                            }
                        }
                        heap = promises.mapNotNull { it.await() }.toMutableList()
                        cancel()
                    }
                } catch (e: CancellationException) {
                    logger.trace { "Block Loading MLHSA was stopped by controller." }
                } catch (e: Exception) {
                    e.printStackTrace()
                    logger.debug { "Block Loading MLHSA Error ${e.message}" }
                }
            }
        }
        val bestSpaces = heap
            .minWithThreeWayComparatorOrNull { lhs, rhs -> config.binComparator(shape, lhs.map { it.block!! }, rhs.map { it.block!! }) }
            ?: emptyList()
        val restItems = items.toMutableMap()
        for (space in bestSpaces) {
            for ((item, amount) in space.block!!.amounts) {
                item as Item
                restItems[item] = restItems[item]!! - amount
            }
        }

        val dfsPromise = dfs(
            items = restItems,
            shape = shape,
            blockTable = blockTable,
            fixedSpaces = bestSpaces
        )
        try {
            for (bin in dfsPromise) {
                if (promise.trySend(bin).isFailure) {
                    break
                }
                break
            }
        } catch (e: CancellationException) {
            logger.trace { "Block Loading MLHSA was stopped by controller." }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.debug { "Block Loading MLHSA Error ${e.message}" }
        } finally {
            dfsPromise.close()
        }

        promise.close()
    }
}
