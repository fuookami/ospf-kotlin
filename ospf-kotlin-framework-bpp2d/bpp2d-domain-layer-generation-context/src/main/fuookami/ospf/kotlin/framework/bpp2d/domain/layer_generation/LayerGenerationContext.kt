/**
 * 层生成上下文。 / Layer generation context.
 *
 * 对应 BPP3D 的 `LayerGenerationContext`：组合多个候选层生成策略，为列生成提供候选列。生成器按形状
 * 分派——请求含圆形件时只启用圆网格与混合生成器，纯矩形请求走矩形生成器集合。
 * / Mirrors BPP3D `LayerGenerationContext`: combines several candidate layer strategies, feeding candidate
 * columns to column generation. Generators are dispatched by shape — requests containing circle items only
 * enable the circle-grid and mixed generators, while pure-rectangle requests use the rectangle set.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.partialOrd
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp2d.domain.bla.service.BottomUpLeftJustifiedAlgorithm
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.BlockLoadingContext
import fuookami.ospf.kotlin.framework.bpp2d.domain.block_loading.model.Space2
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.service.CircleGridGenerator
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 构造与目标板材等大的初始空间。 / Build an initial space matching the target sheet.
 *
 * @param V 数值类型 / numeric type
 * @param sheet 板材类型 / sheet type
 * @return 初始空间 / initial space
*/
private fun <V : FloatingNumber<V>> initialSpaceOf(sheet: SheetType<V>): Space2<V> {
    return Space2(
        x = quantityZeroOf(sheet.width),
        y = quantityZeroOf(sheet.height),
        width = sheet.width,
        height = sheet.height
    )
}

/**
 * 由放置列表与板材构造候选层。 / Build a candidate layer from placements and a sheet.
 *
 * @param V 数值类型 / numeric type
 * @param iteration 生成迭代 / generation iteration
 * @param from 生成来源类型 / generation source class
 * @param sheet 板材类型 / sheet type
 * @param placements 放置列表 / placement list
 * @return 候选层 / candidate layer
*/
private fun <V : FloatingNumber<V>> layerOf(
    iteration: Int,
    from: KClass<*>,
    sheet: SheetType<V>,
    placements: List<LayerPlacement2<V>>
): Layer<V> {
    var width = quantityZeroOf(sheet.width)
    var height = quantityZeroOf(sheet.height)
    for (placement in placements) {
        val maxX = (placement.maxX() as? fuookami.ospf.kotlin.utils.functional.Ok)?.value
        val maxY = (placement.maxY() as? fuookami.ospf.kotlin.utils.functional.Ok)?.value
        if (maxX != null) {
            width = quantityMaxOf(width, maxX)
        }
        if (maxY != null) {
            height = quantityMaxOf(height, maxY)
        }
    }
    return Layer(
        iteration = iteration.toLong(),
        from = from,
        sheet = sheet,
        shape = Sheet2Shape(width = width, height = height),
        units = placements
    )
}

/**
 * 历史层生成器：回放请求中已有的层。 / Historical layer generator: replays the layers already in the request.
 *
 * @param V 数值类型 / numeric type
*/
class HistoricalLayerGenerator<V : FloatingNumber<V>> : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        return request.existingLayers.map { layer ->
            Bpp2dLayerGenerationResult(
                layer = layer,
                source = "historical"
            )
        }
    }
}

/**
 * 块装载局部生成器：每个物品单独形成规则块。 / Block-loading local generator: one regular block per item.
 *
 * @param V 数值类型 / numeric type
 * @param context 块装载上下文 / block loading context
*/
class BlLocalLayerGenerator<V : FloatingNumber<V>>(
    private val context: BlockLoadingContext<V> = BlockLoadingContext()
) : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val blocks = when (val result = context.localBlocks(request.items, initialSpaceOf(sheet), kerf = request.kerf)) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> result.value
            else -> return emptyList()
        }
        return blocks.map { block ->
            Bpp2dLayerGenerationResult(
                layer = layerOf(
                    iteration = request.iteration,
                    from = BlLocalLayerGenerator::class,
                    sheet = sheet,
                    placements = block.placements
                ),
                source = "block-local"
            )
        }
    }
}

/**
 * 块装载全局生成器：多物品拼接为异构块。 / Block-loading global generator: heterogeneous blocks from multiple items.
 *
 * @param V 数值类型 / numeric type
 * @param context 块装载上下文 / block loading context
*/
class BlGlobalLayerGenerator<V : FloatingNumber<V>>(
    private val context: BlockLoadingContext<V> = BlockLoadingContext()
) : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val blocks = when (val result = context.globalBlocks(request.items, initialSpaceOf(sheet), kerf = request.kerf)) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> result.value
            else -> return emptyList()
        }
        return blocks.map { block ->
            Bpp2dLayerGenerationResult(
                layer = layerOf(
                    iteration = request.iteration,
                    from = BlGlobalLayerGenerator::class,
                    sheet = sheet,
                    placements = block.placements
                ),
                source = "block-global"
            )
        }
    }
}

/**
 * 空间切分生成器：调用 DFS 与多行启发式搜索。 / Space-splitting generator: runs the DFS and multi-row searches.
 *
 * @param V 数值类型 / numeric type
 * @param context 块装载上下文 / block loading context
*/
class SpaceSplitLayerGenerator<V : FloatingNumber<V>>(
    private val context: BlockLoadingContext<V> = BlockLoadingContext()
) : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val space = initialSpaceOf(sheet)
        val results = ArrayList<Bpp2dLayerGenerationResult<V>>()

        when (val dfs = context.depthFirst(space, request.items, kerf = request.kerf)) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> {
                if (dfs.value.placements.isNotEmpty()) {
                    results.add(
                        Bpp2dLayerGenerationResult(
                            layer = layerOf(
                                iteration = request.iteration,
                                from = SpaceSplitLayerGenerator::class,
                                sheet = sheet,
                                placements = dfs.value.placements
                            ),
                            source = "space-split-dfs"
                        )
                    )
                }
            }

            else -> {}
        }
        when (val multiRow = context.multiRow(space, request.items, kerf = request.kerf)) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> {
                if (multiRow.value.placements.isNotEmpty()) {
                    results.add(
                        Bpp2dLayerGenerationResult(
                            layer = layerOf(
                                iteration = request.iteration,
                                from = SpaceSplitLayerGenerator::class,
                                sheet = sheet,
                                placements = multiRow.value.placements
                            ),
                            source = "space-split-multi-row"
                        )
                    )
                }
            }

            else -> {}
        }
        return results
    }
}

/**
 * BLA 生成器：在板材上执行底部向上左对齐排样。 / BLA generator: runs bottom-up left-justified layout on the sheet.
 *
 * @param V 数值类型 / numeric type
 * @param config BLA 配置 / BLA configuration
*/
class BlaLayerGenerator<V : FloatingNumber<V>>(
    private val config: BottomUpLeftJustifiedAlgorithm.Config<V> = BottomUpLeftJustifiedAlgorithm.Config()
) : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val shape = Sheet2Shape(width = sheet.width, height = sheet.height)
        val layout = BottomUpLeftJustifiedAlgorithm(
            sheet = shape,
            config = config,
            kerf = request.kerf
        ).invoke(request.items)
        val result = when (layout) {
            is fuookami.ospf.kotlin.utils.functional.Ok -> layout.value
            else -> return emptyList()
        }
        if (result.placements.isEmpty()) {
            return emptyList()
        }
        return listOf(
            Bpp2dLayerGenerationResult(
                layer = layerOf(
                    iteration = request.iteration,
                    from = BlaLayerGenerator::class,
                    sheet = sheet,
                    placements = result.placements
                ),
                source = "bla"
            )
        )
    }
}

/**
 * 图样生成器：按「每行件数 × 行数」的图样铺满。 / Pattern generator: fills by a "per-row count × row count" pattern.
 *
 * @param V 数值类型 / numeric type
*/
class PatternLayerGenerator<V : FloatingNumber<V>> : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val results = ArrayList<Bpp2dLayerGenerationResult<V>>()
        for ((item, _) in request.items) {
            if (item.isCircleItem()) {
                continue
            }
            for (candidate in item.shape.candidateShapes(item.allowRotate)) {
                val shape = candidate.shape
                // 刀缝通过节距体现：相邻件按「包围尺寸 + kerf」推进，真实间隙恰为 kerf。
                // The kerf is expressed through the pitch: neighbours advance by bounding size plus kerf, so the
                // real gap is exactly the kerf.
                val pitch = pitchOf(shape, request.kerfSpacing(shape.boundingWidth))
                val cols = countFitting(sheet.width, pitch.x)
                if (cols <= 0) {
                    continue
                }
                val rowWidth = scaledQuantity(pitch.x, cols)
                val rows = countFitting(sheet.height, pitch.y)
                if (rows <= 0) {
                    continue
                }
                val placements = ArrayList<LayerPlacement2<V>>(cols * rows)
                for (row in 0 until rows) {
                    val y = scaledQuantity(pitch.y, row)
                    for (col in 0 until cols) {
                        val x = scaledQuantity(pitch.x, col)
                        placements.add(
                            LayerPlacement2(
                                shape = shape,
                                position = QuantityPoint2(x = x, y = y),
                                rotated = candidate.rotated,
                                itemId = item.id.value
                            )
                        )
                    }
                }
                results.add(
                    Bpp2dLayerGenerationResult(
                        layer = layerOf(
                            iteration = request.iteration,
                            from = PatternLayerGenerator::class,
                            sheet = sheet,
                            placements = placements
                        ),
                        source = if (candidate.rotated) "pattern-rotated" else "pattern"
                    )
                )
                // 行宽小于板材宽度时，给出「整行但减少列数」的替代图样。
                // When the row is narrower than the sheet, also emit an alternative with fewer columns.
                if (quantityLess(rowWidth, sheet.width) && cols > 1) {
                    results.add(
                        Bpp2dLayerGenerationResult(
                            layer = layerOf(
                                iteration = request.iteration,
                                from = PatternLayerGenerator::class,
                                sheet = sheet,
                                placements = placements
                            ),
                            source = "pattern-narrow"
                        )
                    )
                }
            }
        }
        return results
    }
}

/**
 * 圆形网格生成器：直角网格与六角网格。 / Circle grid generator: square and hexagonal grids.
 *
 * @param V 数值类型 / numeric type
 * @param generator 圆形网格生成器 / circle grid generator
*/
class CircleGridLayerGenerator<V : FloatingNumber<V>>(
    private val generator: CircleGridGenerator = CircleGridGenerator()
) : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val results = ArrayList<Bpp2dLayerGenerationResult<V>>()
        for ((item, _) in request.items) {
            if (!item.isCircleItem()) {
                continue
            }
            for (layout in generator.invoke(item, sheet, request.kerf)) {
                if (layout.placements.isEmpty()) {
                    continue
                }
                results.add(
                    Bpp2dLayerGenerationResult(
                        layer = layerOf(
                            iteration = request.iteration,
                            from = CircleGridLayerGenerator::class,
                            sheet = sheet,
                            placements = layout.placements
                        ),
                        source = when (layout.mode) {
                            fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.service.CircleGridMode.SquareGrid ->
                                "circle-rect-grid"

                            fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.service.CircleGridMode.HexGrid ->
                                "circle-hex-grid"
                        }
                    )
                )
            }
        }
        return results
    }
}

/**
 * 混合形状生成器：先生成规则层，再用底层几何逐对复核合法性。 / Mixed shape generator: builds a regular layer then
 * re-validates every pair against the underlying geometry.
 *
 * @param V 数值类型 / numeric type
 * @param generator 圆形网格生成器 / circle grid generator
*/
class MixedShapeLayerGenerator<V : FloatingNumber<V>>(
    private val generator: CircleGridGenerator = CircleGridGenerator()
) : Bpp2dLayerGenerator<V> {
    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        val sheet = request.sheet ?: return emptyList()
        val circles = request.items.filter { (item, _) -> item.isCircleItem() }
        val rectangles = request.items.filter { (item, _) -> !item.isCircleItem() }
        if (circles.isEmpty() || rectangles.isEmpty()) {
            return emptyList()
        }

        val results = ArrayList<Bpp2dLayerGenerationResult<V>>()
        for ((rectangle, _) in rectangles) {
            for (candidate in rectangle.shape.candidateShapes(rectangle.allowRotate)) {
                val shape = candidate.shape
                val cols = countFitting(sheet.width, shape.boundingWidth)
                if (cols <= 0) {
                    continue
                }
                val rowWidth = scaledQuantity(shape.boundingWidth, cols)
                val placements = ArrayList<LayerPlacement2<V>>()
                // 先在底部铺一行矩形件，再用圆形网格填充其上方区域。
                // First lay one row of rectangles, then fill the area above them with a circle grid.
                for (col in 0 until cols) {
                    val x = scaledQuantity(shape.boundingWidth, col)
                    placements.add(
                        LayerPlacement2(
                            shape = shape,
                            position = QuantityPoint2(x = x, y = quantityZeroOf(sheet.height)),
                            rotated = candidate.rotated,
                            itemId = rectangle.id.value
                        )
                    )
                }
                val upperHeight = sheet.height - shape.boundingHeight
                if (!quantityIsPositive(upperHeight)) {
                    continue
                }
                val upperSheet = SheetType(
                    width = rowWidth,
                    height = upperHeight,
                    maxLoad = sheet.maxLoad,
                    typeCode = sheet.typeCode
                )
                for ((circle, _) in circles) {
                    for (layout in generator.invoke(circle, upperSheet, request.kerf)) {
                        val combined = ArrayList<LayerPlacement2<V>>(placements.size + layout.placements.size)
                        combined.addAll(placements)
                        for (placement in layout.placements) {
                            val shifted = placement.translate(
                                QuantityVector2(
                                    x = quantityZeroOf(sheet.width),
                                    y = shape.boundingHeight
                                )
                            )
                            if (shifted.insideSheet(Sheet2Shape(width = sheet.width, height = sheet.height)).let {
                                    when (it) {
                                        is fuookami.ospf.kotlin.utils.functional.Ok -> it.value
                                        else -> false
                                    }
                                }
                            ) {
                                combined.add(shifted)
                            }
                        }
                        val validated = validateNoOverlap(combined)
                        if (validated == null) {
                            continue
                        }
                        results.add(
                            Bpp2dLayerGenerationResult(
                                layer = layerOf(
                                    iteration = request.iteration,
                                    from = MixedShapeLayerGenerator::class,
                                    sheet = sheet,
                                    placements = validated
                                ),
                                source = "mixed-shape"
                            )
                        )
                    }
                }
            }
        }
        return results
    }

    /**
     * 用底层几何逐对复核放置是否互不重叠，有重叠时返回 null。 / Re-validate pairwise non-overlap with the
     * underlying geometry, returning null when any pair overlaps.
     *
     * @param V 数值类型 / numeric type
     * @param placements 放置列表 / placement list
     * @return 合法放置列表，存在重叠时返回 null / validated placement list, or null when overlapping
    */
    private fun <V : FloatingNumber<V>> validateNoOverlap(
        placements: List<LayerPlacement2<V>>
    ): List<LayerPlacement2<V>>? {
        for (i in placements.indices) {
            for (j in i + 1 until placements.size) {
                val overlapped = when (val result = placements[i].overlapped(placements[j])) {
                    is fuookami.ospf.kotlin.utils.functional.Ok -> result.value
                    else -> return null
                }
                if (overlapped) {
                    return null
                }
            }
        }
        return placements
    }
}

/**
 * 组合式层生成上下文。 / Composite layer generation context.
 *
 * @param V 数值类型 / numeric type
 * @property generators 生成器列表 / generator list
*/
class LayerGenerationContext<V : FloatingNumber<V>>(
    private val generators: List<Bpp2dLayerGenerator<V>> = defaultGenerators()
) : Bpp2dLayerGenerator<V> {

    companion object {
        /**
         * 默认生成器集合。 / Default generator set.
         *
         * @param V 数值类型 / numeric type
         * @return 生成器列表 / generator list
        */
        fun <V : FloatingNumber<V>> defaultGenerators(): List<Bpp2dLayerGenerator<V>> {
            return listOf(
                BlLocalLayerGenerator(),
                BlGlobalLayerGenerator(),
                SpaceSplitLayerGenerator(),
                BlaLayerGenerator(),
                PatternLayerGenerator(),
                CircleGridLayerGenerator(),
                MixedShapeLayerGenerator(),
                HistoricalLayerGenerator()
            )
        }
    }

    override fun generate(request: Bpp2dLayerGenerationRequest<V>): List<Bpp2dLayerGenerationResult<V>> {
        if (generators.isEmpty()) {
            return emptyList()
        }
        val selected = if (request.hasCircleItems() && !request.hasRectangleItems()) {
            // 纯圆形件请求只走圆网格与历史生成器。
            // Pure circle requests only use the circle grid and the historical generator.
            generators.filter {
                it is CircleGridLayerGenerator<*> || it is HistoricalLayerGenerator<*>
            }
        } else if (request.hasCircleItems()) {
            // 混合请求需要圆网格、混合、历史、以及可处理矩形件的生成器。
            // Mixed requests need the circle grid, the mixed generator, history and the rectangle-capable ones.
            generators.filter {
                it !is CircleGridLayerGenerator<*> || it is CircleGridLayerGenerator<*>
            }
        } else {
            // 纯矩形请求排除圆形专用生成器。
            // Pure rectangle requests exclude the circle-only generators.
            generators.filter {
                it !is CircleGridLayerGenerator<*> && it !is MixedShapeLayerGenerator<*>
            }
        }

        val all = ArrayList<Bpp2dLayerGenerationResult<V>>()
        for (generator in selected) {
            all += generator.generate(request)
            if (all.size >= request.maxCandidates) {
                break
            }
        }
        return rankByScore(
            request = request,
            results = all
                .distinctBy { it.layer }
                .take(request.maxCandidates)
        )
    }

    /**
     * 按影子价格评分排序候选层。 / Rank candidate layers by the shadow-price score.
     *
     * @param V 数值类型 / numeric type
     * @param request 层生成请求 / layer generation request
     * @param results 候选结果 / candidate results
     * @return 排序后的候选结果 / ranked candidate results
    */
    private fun <V : FloatingNumber<V>> rankByScore(
        request: Bpp2dLayerGenerationRequest<V>,
        results: List<Bpp2dLayerGenerationResult<V>>
    ): List<Bpp2dLayerGenerationResult<V>> {
        val scoreByShadowPrice = request.scoreByShadowPrice
            ?: return results.sortedByDescendingQuantity { it.layer.area }
        return results
            .map { result ->
                result.copy(score = scoreByShadowPrice(result.layer, request))
            }
            .sortedWith { lhs, rhs ->
                val lhsScore = lhs.score ?: lhs.layer.area.value
                val rhsScore = rhs.score ?: rhs.layer.area.value
                when (lhsScore.partialOrd(rhsScore)) {
                    is Order.Greater -> -1
                    is Order.Less -> 1
                    is Order.Equal, null -> 0
                }
            }
    }
}
