package fuookami.ospf.kotlin.framework.bpp3d.domain.item.service

import kotlin.random.Random
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class ItemMergerPileOrderTest {
    private fun attribute(maxLayer: Int) = PackageAttribute(
        packageType = PackageType.CartonContainer,
        packageMaxLayer = UInt64(maxLayer),
        maxHeight = FltX(5.0) * Meter,
        maxDepth = FltX(5.0) * Meter,
        weightAttribute = WeightAttribute(),
        deformationAttribute = LinearDeformationAttribute(FltX.zero),
        hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
        stackingOnPolicy = FilterStackingOnPolicy()
    )

    private fun item(id: String, weight: Int, maxLayer: Int): ActualItem {
        return ActualItem(
            id = itemIdOf(id),
            name = id,
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(weight.toDouble()) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            packageAttribute = attribute(maxLayer)
        )
    }

    private fun assertEquivalent(items: List<Item>, space: Container3Shape, weight: FltX) {
        val expected = assertIs<Ok<Pair<List<Pile>, List<Item>>, *, *>>(reference(
            items = items,
            space = space,
            restWeight = weight
        )).value
        val actual = assertIs<Ok<Pair<List<Pile>, List<Item>>, *, *>>(ItemMerger.mergePiles(
            items = items,
            space = space,
            restWeight = weight
        )).value
        assertEquals(expected.second, actual.second)
        assertEquals(expected.first.map { it.itemViews.map { view -> view.unit to view.orientation } },
            actual.first.map { it.itemViews.map { view -> view.unit to view.orientation } })
        for ((before, after) in expected.first.zip(actual.first)) {
            assertEquals(before.height, after.height)
            assertEquals(before.weight, after.weight)
            assertEquals(before.bottomLayer, after.bottomLayer)
            assertEquals(before.topLayer, after.topLayer)
        }
    }

    @Test
    fun stableSuffixAndCompactionMatchOriginalWithRepeatedOccurrences() {
        val random = Random(61824)
        val space = Container3Shape(
            width = FltX(3.0) * Meter,
            height = FltX(4.0) * Meter,
            depth = FltX(3.0) * Meter
        )
        repeat(40) { seed ->
            val distinct = List(random.nextInt(1, 12)) { index ->
                item(
                    id = "$seed-$index",
                    weight = random.nextInt(1, 6),
                    maxLayer = random.nextInt(1, 5)
                )
            }
            val input = (distinct + distinct.take(3)).shuffled(random)
            assertEquivalent(
                items = input,
                space = space,
                weight = FltX(random.nextInt(5, 30).toDouble())
            )
        }
        assertEquivalent(
            items = List(24) {
                item(
                    id = "unmergeable-$it",
                    weight = 2,
                    maxLayer = 1
                )
            },
            space = space,
            weight = FltX(30.0)
        )
        assertEquivalent(
            items = List(128) {
                item(
                    id = "prefix-$it",
                    weight = 2,
                    maxLayer = 4
                )
            },
            space = space,
            weight = FltX(1000.0)
        )
    }

    @Test
    fun customViewWeightsRetainOriginalSuffixOrderingAndReadCounts() {
        var reads = 0
        val input = List(8) { index ->
            object : ActualItem(
                id = itemIdOf("custom-$index"),
                name = "custom-$index",
                width = FltX(1.0) * Meter,
                height = FltX(1.0) * Meter,
                depth = FltX(1.0) * Meter,
                weight = FltX((8 - index).toDouble()) * Kilogram,
                enabledOrientations = listOf(Orientation.Upright),
                packageAttribute = attribute(3)
            ) {
                override fun view(orientation: Orientation): ItemView {
                    return object : ItemView(this, orientation) {
                        override val weight: Quantity<FltX>
                            get() {
                                ++reads
                                return super.weight * FltX((index + 1).toDouble())
                            }
                    }
                }
            }
        }
        val space = Container3Shape(
            width = FltX(3.0) * Meter,
            height = FltX(4.0) * Meter,
            depth = FltX(3.0) * Meter
        )
        val before = reference(
            items = input,
            space = space,
            restWeight = FltX(30.0)
        )
        val beforeReads = reads
        reads = 0
        val after = ItemMerger.mergePiles(
            items = input,
            space = space,
            restWeight = FltX(30.0)
        )
        assertEquals(beforeReads, reads)
        val beforePiles = assertIs<Ok<Pair<List<Pile>, List<Item>>, *, *>>(before).value
        val afterPiles = assertIs<Ok<Pair<List<Pile>, List<Item>>, *, *>>(after).value
        assertEquals(beforePiles.second, afterPiles.second)
        assertEquals(beforePiles.first.map { it.itemViews.map { view -> view.unit } },
            afterPiles.first.map { it.itemViews.map { view -> view.unit } })
    }

    // 固定的优化前差分参照，不用于性能计时。 / Frozen pre-optimization reference, not a performance benchmark.
    private fun reference(
        items: List<Item>,
        space: AbstractContainer3Shape,
        restWeight: FltX = FltX.maximum
    ): Ret<Pair<List<Pile>, List<Item>>> {
        val averagePileBottomArea = items.fold(FltX.zero) { acc, item -> acc + Bottom.shape(item).area.value } / FltX(items.size.toDouble())
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
                        ) && (((enabledItems[j].width - pileItems.last().width).abs() + (enabledItems[j].depth - pileItems.last().depth).abs()) leq FltX(50.0))
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
                        && (((enabledItems[j].width - pileItems.last().width).abs() + (enabledItems[j].depth - pileItems.last().depth).abs()) leq FltX(50.0))
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
}
