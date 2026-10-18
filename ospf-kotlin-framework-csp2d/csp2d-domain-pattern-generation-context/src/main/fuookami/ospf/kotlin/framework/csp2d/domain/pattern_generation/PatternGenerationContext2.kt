/**
 * 三级断头台图案生成器。 / Three-stage guillotine pattern generator.
 *
 * @property V 数值类型 / numeric type
 * @property source 生成器名称 / generator name
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.pattern_generation

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dCapabilityError
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutOrientation2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutTree2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Region2

/**
 * 三级断头台图案生成器。 / Three-stage guillotine pattern generator.
 *
 * 与两级生成器的差别在于**允许行内混高**：混高更省料，但每列里还需第 3 刀把件与上方余料分开。
 * 因此刀序是「横切分行 → 竖切分列 → 列内横切成件」，恰好三级，与剪板机「切条、切块、切件」一致。
 * / The difference from the two-stage generator is that **mixed row heights are allowed**: mixed heights waste less, but
 * each column needs a third cut to separate the piece from the waste above it. The cut sequence is therefore
 * "rows horizontally, columns vertically, pieces horizontally inside a column", exactly three stages and matching the
 * shear workflow of strips, blocks and pieces.
 *
 * @property V 数值类型 / numeric type
 * @property source 生成器名称 / generator name
*/
class ThreeStagePatternGenerator2<V : FloatingNumber<V>>(
    source: String = "three-stage"
) : ShelfPatternGenerator2<V>(sameHeightRows = false, source = source) {

    /**
     * 生成三级断头台图案。 / Generate a three-stage guillotine pattern.
     *
     * @param request 生成请求 / generation request
     * @return 候选图案列表 / candidate pattern list
    */
    override fun generate(request: PatternGenerationRequest2<V>): Ret<List<PatternGenerationResult2<V>>> {
        return super.generate(request.copy(process = CuttingProcess2.GuillotineThreeStage))
    }

    /**
     * 构造三级切分树。 / Build the three-stage cut tree.
     *
     * @param region 料域 / region
     * @param rows 行列表 / row list
     * @param kerf 刀缝 / kerf
     * @return 切分树 / cut tree
    */
    internal override fun buildCutTree(
        region: Region2<V>,
        rows: List<Row<V>>,
        kerf: Quantity<V>
    ): Ret<CutTree2<V>> {
        return buildRowsTree3(region = region, rows = rows, index = 0, kerf = kerf)
    }

    /**
     * 递归构造行层切分（第 1 级，横切）。 / Recursively build the row-level splits (stage 1, horizontal).
     *
     * @param region 当前区域 / current region
     * @param rows 全部行 / all rows
     * @param index 当前行下标 / current row index
     * @param kerf 刀缝 / kerf
     * @return 子树 / subtree
    */
    private fun buildRowsTree3(
        region: Region2<V>,
        rows: List<Row<V>>,
        index: Int,
        kerf: Quantity<V>
    ): Ret<CutTree2<V>> {
        if (index >= rows.size) {
            return ok(CutTree2.Leaf2(region = region, stage = 1))
        }
        val row = rows[index]
        val cutPosition = row.y + row.height
        if (quantityLessOrEqual(region.maxY(), cutPosition)) {
            return buildRowColumnsTree(region = region, pieces = row.pieces, stage = 2, kerf = kerf)
        }
        val split = when (val result = region.cut(CutOrientation2.Horizontal, cutPosition, kerf)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (rowRegion, restRegion) = split
        val rowTree = when (
            val result = buildRowColumnsTree(region = rowRegion, pieces = row.pieces, stage = 2, kerf = kerf)
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val restTree = when (
            val result = buildRowsTree3(region = restRegion, rows = rows, index = index + 1, kerf = kerf)
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
     * 构造行内列切分（第 2 级，竖切）。 / Build the column splits inside a row (stage 2, vertical).
     *
     * @param region 行区域 / row region
     * @param pieces 行内放置 / placements inside the row
     * @param stage 当前级数 / current stage
     * @param kerf 刀缝 / kerf
     * @return 子树 / subtree
    */
    private fun buildRowColumnsTree(
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
            // 最后一列占满剩余宽度：直接进入第 3 级。 / The last column fills the width; go straight to stage 3.
            return buildColumnTree(region = region, piece = piece, stage = stage + 1, kerf = kerf)
        }
        val split = when (val result = region.cut(CutOrientation2.Vertical, cutPosition, kerf)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (columnRegion, restRegion) = split
        val columnTree = when (
            val result = buildColumnTree(region = columnRegion, piece = piece, stage = stage + 1, kerf = kerf)
        ) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val restTree = when (
            val result = buildRowColumnsTree(region = restRegion, pieces = pieces.drop(1), stage = stage, kerf = kerf)
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
            first = columnTree,
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
     * 构造列内切分（第 3 级，横切分离件与上方余料）。 / Build the split inside a column (stage 3, horizontal, separating
     * the piece from the waste above it).
     *
     * @param region 列区域 / column region
     * @param piece 该列内的件 / the piece inside the column
     * @param stage 当前级数 / current stage
     * @param kerf 刀缝 / kerf
     * @return 子树 / subtree
    */
    private fun buildColumnTree(
        region: Region2<V>,
        piece: Placement2<V>,
        stage: Int,
        kerf: Quantity<V>
    ): Ret<CutTree2<V>> {
        val cutPosition = piece.y + piece.height
        if (quantityLessOrEqual(region.maxY(), cutPosition)) {
            return ok(CutTree2.Leaf2(region = region, stage = stage, pieceId = piece.itemId))
        }
        val split = when (val result = region.cut(CutOrientation2.Horizontal, cutPosition, kerf)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val (pieceRegion, wasteRegion) = split
        return CutTree2.split2Of(
            region = region,
            orientation = CutOrientation2.Horizontal,
            position = cutPosition,
            stage = stage,
            first = CutTree2.Leaf2(region = pieceRegion, stage = stage, pieceId = piece.itemId),
            second = CutTree2.Leaf2(region = wasteRegion, stage = stage),
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

/**
 * 组合式图案生成上下文：按工艺分派生成器。 / A composite pattern generation context dispatching generators by process.
 *
 * 工艺决定布局族，因此分派不是「随便挑一个生成器」，而是必须挑与工艺相容的那个：
 * 1. `Freeform` → 混高货架，不输出刀序（余料最少）。
 * 2. `GuillotineTwoStage` → 等高行货架，输出两级切分树。
 * 3. `GuillotineThreeStage` → 混高货架，输出三级切分树。
 *
 * 另外总会回放历史图案（若有），使列生成在后续迭代里能复用已生成的列。
 * / The process decides the layout family, so dispatching is not "pick any generator" but must pick the one compatible
 * with the process: `Freeform` uses a mixed-height shelf without a cut sequence (least waste), `GuillotineTwoStage` uses
 * a uniform-height shelf with a two-stage tree, and `GuillotineThreeStage` uses a mixed-height shelf with a three-stage
 * tree. Historical patterns are always replayed so that later column-generation iterations can reuse existing columns.
 *
 * @property V 数值类型 / numeric type
 * @property freeformGenerators 任意排样生成器 / arbitrary nesting generators
 * @property twoStageGenerators 两级断头台生成器 / two-stage guillotine generators
 * @property threeStageGenerators 三级断头台生成器 / three-stage guillotine generators
*/
class PatternGenerationContext2<V : FloatingNumber<V>>(
    private val freeformGenerators: List<PatternGenerator2<V>> = listOf(ShelfPatternGenerator2(sameHeightRows = false)),
    private val twoStageGenerators: List<PatternGenerator2<V>> = listOf(ShelfPatternGenerator2(sameHeightRows = true)),
    private val threeStageGenerators: List<PatternGenerator2<V>> = listOf(ThreeStagePatternGenerator2())
) : PatternGenerator2<V> {

    override fun generate(request: PatternGenerationRequest2<V>): Ret<List<PatternGenerationResult2<V>>> {
        val generators = when (request.process) {
            CuttingProcess2.Freeform -> freeformGenerators
            CuttingProcess2.GuillotineTwoStage -> twoStageGenerators
            CuttingProcess2.GuillotineThreeStage -> threeStageGenerators
        }
        if (generators.isEmpty()) {
            return Failed(
                Csp2dCapabilityError(
                    capability = "工艺 ${request.process.name} 没有配置生成器 / No generator configured for this process"
                )
            )
        }

        val collected = ArrayList<PatternGenerationResult2<V>>()
        for (generator in generators) {
            when (val result = generator.generate(request)) {
                is Ok -> collected.addAll(result.value)
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        // 回放历史图案，避免重复生成已有列。 / Replay historical patterns to avoid regenerating existing columns.
        for (pattern in request.existingPatterns) {
            collected.add(
                PatternGenerationResult2(
                    pattern = pattern,
                    source = "historical"
                )
            )
        }
        return ok(distinctPatterns(results = collected, limit = request.maxCandidates))
    }
}
