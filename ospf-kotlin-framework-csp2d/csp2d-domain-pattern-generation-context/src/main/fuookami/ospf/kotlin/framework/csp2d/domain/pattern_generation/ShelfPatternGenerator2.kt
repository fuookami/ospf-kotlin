/**
 * 货架式图案生成器。 / Shelf pattern generator.
 *
 * 逐行铺满幅宽：取一个件确定行高，再在同一行内继续放件。是否允许行内混高决定了工艺：
 * 1. **行内等高**：每件都占满行高，因此「横切分行 + 竖切分件」恰好是**两级断头台**，可直接输出切分树。
 * 2. **行内混高**：混高更省料，但每列内还需一刀把件与上方余料分开，因此只能用于**任意排样**（不输出刀序）
 *    或交给三级断头台生成器（见 `ThreeStagePatternGenerator2`）。
 *
 * 这是本框架与「随手排个样」的关键区别：工艺约束直接改变了可用的布局族，而不只是一个开关。
 * / Fill the width row by row: one piece fixes the row height, then more pieces are added to that row. Whether mixed
 * heights are allowed decides the process:
 * 1. **Uniform row height**: every piece spans the row height, so "cut rows horizontally, then pieces vertically" is
 *    exactly a **two-stage guillotine** and a cut tree can be emitted.
 * 2. **Mixed row height**: mixed heights waste less material, but each column needs one more cut to separate the piece
 *    from the waste above it, so it is only usable for **arbitrary nesting** (without a cut sequence) or for the
 *    three-stage guillotine generator (`ThreeStagePatternGenerator2`).
 *
 * This is the essential difference from "just nest something": the process constraint changes the family of legal
 * layouts, not merely a switch.
 *
 * @property V 数值类型 / numeric type
 * @property sameHeightRows 是否要求行内等高 / whether rows must have a uniform height
 * @property source 生成器名称 / generator name
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.pattern_generation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Pattern2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Product2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.patternId2Of
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dCapabilityError
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutOrientation2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutTree2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Region2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.region2Of

open class ShelfPatternGenerator2<V : FloatingNumber<V>>(
    private val sameHeightRows: Boolean = false,
    private val source: String = "shelf"
) : PatternGenerator2<V> {

    /**
     * 一行排样。 / One layout row.
     *
     * @property V2 数值类型 / numeric type
     * @property y 行底部 y 坐标 / row bottom y coordinate
     * @property height 行高 / row height
     * @property pieces 行内放置 / placements inside the row
    */
    internal data class Row<V2 : FloatingNumber<V2>>(
        val y: Quantity<V2>,
        val height: Quantity<V2>,
        val pieces: List<Placement2<V2>>
    )

    override fun generate(request: PatternGenerationRequest2<V>): Ret<List<PatternGenerationResult2<V>>> {
        when (val checked = request.validate()) {
            is Ok -> {}
            is Failed -> return Failed(checked.error)
            is Fatal -> return Fatal(checked.errors)
        }
        if (request.process == CuttingProcess2.GuillotineTwoStage && !sameHeightRows) {
            return Failed(
                Csp2dCapabilityError(
                    capability = "行内混高的货架排样不是两级断头台；请使用三级断头台生成器或改为行内等高 / " +
                            "A mixed-height shelf layout is not a two-stage guillotine"
                )
            )
        }

        val material = request.material
        val kerf = request.kerf
        val zero = quantityZeroOf(material.width)
        val regionHeight = request.targetRegionHeight()
        if (!quantityIsPositive(regionHeight)) {
            return ok(emptyList())
        }

        val remaining = LinkedHashMap<String, UInt64>()
        val productById = LinkedHashMap<String, Product2<V>>()
        for ((product, amount) in request.products) {
            productById[product.id.value] = product
            remaining[product.id.value] = amount
        }
        val ordered = productById.values.sortedWith(
            compareByDescending<Product2<V>> { it.height.value }
                .thenByDescending { it.width.value }
        )

        val rows = ArrayList<Row<V>>()
        var cursorY: Quantity<V> = zero
        var usedHeight: Quantity<V> = zero
        while (true) {
            if (!quantityLess(cursorY, regionHeight)) {
                break
            }
            val row = buildRow(
                ordered = ordered,
                remaining = remaining,
                kerf = kerf,
                y = cursorY,
                regionWidth = material.width,
                regionHeight = regionHeight
            ) ?: break
            rows.add(row)
            usedHeight = row.y + row.height
            cursorY = row.y + row.height + kerf
            if (rows.size >= request.maxCandidates) {
                break
            }
        }
        if (rows.isEmpty()) {
            return ok(emptyList())
        }

        // 卷材模式把料域收缩到实际消耗的段高；定尺模式保留整块板材，下方与右侧为余料。
        // Coil mode shrinks the region to the consumed segment height; sheet mode keeps the whole sheet, with remnants
        // below and to the right.
        val actualHeight = material.height ?: usedHeight
        val region = region2Of(
            x = zero,
            y = zero,
            width = material.width,
            height = actualHeight
        )
        val placements = rows.flatMap { it.pieces }
        if (placements.isEmpty()) {
            return ok(emptyList())
        }

        val cutTree = if (request.process.requiresCutTree) {
            when (val tree = buildCutTree(region = region, rows = rows, kerf = kerf)) {
                is Ok -> tree.value
                is Failed -> return Failed(tree.error)
                is Fatal -> return Fatal(tree.errors)
            }
        } else {
            null
        }

        val pattern = Pattern2(
            id = patternId2Of("$source-${request.iteration}-${rows.size}"),
            material = material,
            region = region,
            placements = placements,
            cutTree = cutTree,
            process = request.process
        )
        when (val checked = pattern.validate(kerf)) {
            is Ok -> {}
            is Failed -> return Failed(checked.error)
            is Fatal -> return Fatal(checked.errors)
        }
        return ok(listOf(PatternGenerationResult2(pattern = pattern, source = source)))
    }

    /**
     * 构造一行。 / Build one row.
     *
     * `sameHeightRows` 为真时只接受与行高等高的件；否则接受不高于行高的件。
     * / When `sameHeightRows` is true only pieces as tall as the row are accepted; otherwise pieces no taller than the
     * row are accepted.
     *
     * @param ordered 按高度降序的产品 / products ordered by descending height
     * @param remaining 剩余件数，就地扣减 / remaining counts, decremented in place
     * @param kerf 刀缝 / kerf
     * @param y 行底部坐标 / row bottom coordinate
     * @param regionWidth 料域宽度 / region width
     * @param regionHeight 料域高度 / region height
     * @return 行，无法放置时返回 null / the row, or null when nothing fits
    */
    internal fun buildRow(
        ordered: List<Product2<V>>,
        remaining: MutableMap<String, UInt64>,
        kerf: Quantity<V>,
        y: Quantity<V>,
        regionWidth: Quantity<V>,
        regionHeight: Quantity<V>
    ): Row<V>? {
        val seed = ordered.firstOrNull { left(it, remaining) > 0 } ?: return null
        val rowHeight = seed.height
        if (quantityGreater(y + rowHeight, regionHeight)) {
            return null
        }

        val pieces = ArrayList<Placement2<V>>()
        var cursorX: Quantity<V> = quantityZeroOf(regionWidth)
        while (true) {
            val candidate = ordered.firstOrNull { product ->
                val left = left(product, remaining)
                val heightFits = if (sameHeightRows) {
                    quantityEqual(product.height, rowHeight)
                } else {
                    quantityLessOrEqual(product.height, rowHeight)
                }
                left > 0 && heightFits && quantityLessOrEqual(cursorX + product.width, regionWidth)
            } ?: break

            val shape = when (val result = rectangleShape2(width = candidate.width, height = candidate.height)) {
                is Ok -> result.value
                is Failed -> return null
                is Fatal -> return null
            }
            pieces.add(
                Placement2(
                    shape = shape,
                    position = QuantityPoint2(x = cursorX, y = y),
                    itemId = candidate.id.value
                )
            )
            remaining[candidate.id.value] = UInt64((left(candidate, remaining) - 1).coerceAtLeast(0))
            cursorX = cursorX + candidate.width + kerf
        }
        if (pieces.isEmpty()) {
            return null
        }
        return Row(y = y, height = rowHeight, pieces = pieces)
    }

    /**
     * 读取剩余件数。 / Read the remaining piece count.
     *
     * @param product 产品 / product
     * @param remaining 剩余件数映射 / remaining count map
     * @return 件数 / count
    */
    internal fun left(product: Product2<V>, remaining: Map<String, UInt64>): Int {
        return remaining[product.id.value]?.toInt() ?: 0
    }

    /**
     * 由行构造两级断头台切分树。 / Build a two-stage guillotine cut tree from the rows.
     *
     * 第 1 级横切分行（最后一刀分出下方余料），第 2 级竖切分件（最后一刀分出右侧余料）。
     * / Stage 1 cuts rows horizontally (the last cut separates the remnant below), stage 2 cuts pieces vertically (the
     * last cut separates the remnant to the right).
     *
     * @param region 料域 / region
     * @param rows 行列表 / row list
     * @param kerf 刀缝 / kerf
     * @param materialWidth 料域宽度 / material width
     * @return 切分树 / cut tree
    */
    internal open fun buildCutTree(
        region: Region2<V>,
        rows: List<Row<V>>,
        kerf: Quantity<V>
    ): Ret<CutTree2<V>> {
        return buildRowsTree(region = region, rows = rows, index = 0, kerf = kerf)
    }

    /**
     * 递归构造行层切分。 / Recursively build the row-level splits.
     *
     * @param region 当前区域 / current region
     * @param rows 全部行 / all rows
     * @param index 当前行下标 / current row index
     * @param kerf 刀缝 / kerf
     * @param materialWidth 料域宽度 / material width
     * @return 子树 / subtree
    */
    private fun buildRowsTree(
        region: Region2<V>,
        rows: List<Row<V>>,
        index: Int,
        kerf: Quantity<V>
    ): Ret<CutTree2<V>> {
        if (index >= rows.size) {
            // 行下方的余料。 / The remnant below the rows.
            return ok(CutTree2.Leaf2(region = region, stage = 1))
        }
        val row = rows[index]
        val cutPosition = row.y + row.height
        if (quantityLessOrEqual(region.maxY(), cutPosition)) {
            return buildRowPiecesTree(
                region = region,
                pieces = row.pieces,
                stage = 2,
                kerf = kerf
            )
        }
        val split = when (val result = region.cut(CutOrientation2.Horizontal, cutPosition, kerf)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (rowRegion, restRegion) = split
        val rowTree = when (
            val result = buildRowPiecesTree(
                region = rowRegion,
                pieces = row.pieces,
                stage = 2,
                kerf = kerf
            )
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val restTree = when (
            val result = buildRowsTree(
                region = restRegion,
                rows = rows,
                index = index + 1,
                kerf = kerf
            )
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return CutTree2.split2Of(
            region = region,
            orientation = CutOrientation2.Horizontal,
            position = cutPosition,
            stage = 1,
            first = rowTree,
            second = restTree,
            kerf = kerf
        ).let { result ->
            when (result) {
                is Ok -> ok<CutTree2<V>>(result.value)
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }

    /**
     * 构造行内切分：竖切成件，最后一刀分出右侧余料。 / Build the splits inside a row: vertical cuts into pieces with the
     * last cut separating the remnant to the right.
     *
     * @param region 行区域 / row region
     * @param pieces 行内放置 / placements inside the row
     * @param stage 当前级数 / current stage
     * @param kerf 刀缝 / kerf
     * @param materialWidth 料域宽度 / material width
     * @return 子树 / subtree
    */
    private fun buildRowPiecesTree(
        region: Region2<V>,
        pieces: List<Placement2<V>>,
        stage: Int,
        kerf: Quantity<V>
    ): Ret<CutTree2<V>> {
        if (pieces.isEmpty()) {
            return ok(CutTree2.Leaf2(region = region, stage = stage))
        }
        val piece = pieces.first()
        val cutPosition = piece.x + piece.width
        if (quantityLessOrEqual(region.maxX(), cutPosition)) {
            // 该件占满剩余宽度。 / The piece fills the remaining width.
            return ok(CutTree2.Leaf2(region = region, stage = stage, pieceId = piece.itemId))
        }
        val split = when (val result = region.cut(CutOrientation2.Vertical, cutPosition, kerf)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (pieceRegion, restRegion) = split
        val restTree = when (
            val result = buildRowPiecesTree(
                region = restRegion,
                pieces = pieces.drop(1),
                stage = stage,
                kerf = kerf
            )
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return CutTree2.split2Of(
            region = region,
            orientation = CutOrientation2.Vertical,
            position = cutPosition,
            stage = stage,
            first = CutTree2.Leaf2(region = pieceRegion, stage = stage, pieceId = piece.itemId),
            second = restTree,
            kerf = kerf
        ).let { result ->
            when (result) {
                is Ok -> ok<CutTree2<V>>(result.value)
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
    }
}
