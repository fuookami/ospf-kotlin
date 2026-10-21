/**
 * 图案生成契约测试。 / Pattern generation contract test.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.pattern_generation

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CutOrientation2
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2

class PatternGenerationContractTest {

    /**
     * 构造米制量值。 / Build a metric quantity.
     *
     * @param value 数值 / value
     * @return 量值 / quantity
    */
    private fun meter(value: Double): Quantity<Flt64> = Flt64(value) * Meter

    /**
     * 构造定尺板材。 / Build a fixed-size sheet material.
     *
     * @param width 板宽 / sheet width
     * @param height 板高 / sheet height
     * @return 物料 / material
    */
    private fun sheet(width: Double = 100.0, height: Double = 60.0): Material2<Flt64> {
        return Material2(
            id = materialId2Of("SHEET"),
            name = "SHEET",
            width = meter(width),
            height = meter(height)
        )
    }

    /**
     * 构造卷材。 / Build a coil material.
     *
     * @param width 幅宽 / coil width
     * @return 物料 / material
    */
    private fun coil(width: Double = 100.0): Material2<Flt64> {
        return Material2(
            id = materialId2Of("COIL"),
            name = "COIL",
            width = meter(width)
        )
    }

    /**
     * 构造产品。 / Build a product.
     *
     * @param id 产品编号 / product identifier
     * @param width 宽度 / width
     * @param height 高度 / height
     * @return 产品 / product
    */
    private fun product(id: String, width: Double, height: Double): Product2<Flt64> {
        return Product2(
            id = productId2Of(id),
            name = id,
            width = meter(width),
            height = meter(height)
        )
    }

    /**
     * 构造请求。 / Build a request.
     *
     * @param material 物料 / material
     * @param products 产品与件数 / products and counts
     * @param process 工艺 / process
     * @param kerf 刀缝 / kerf
     * @return 请求 / request
    */
    private fun request(
        material: Material2<Flt64>,
        products: List<Pair<Product2<Flt64>, UInt64>>,
        process: CuttingProcess2 = CuttingProcess2.Freeform,
        kerf: Double = 0.0
    ): PatternGenerationRequest2<Flt64> {
        return PatternGenerationRequest2(
            material = material,
            products = products,
            kerf = meter(kerf),
            process = process
        )
    }

    @Test
    fun freeformShelfProducesPatternWithoutCutTree() {
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = false)
            .generate(
                request(
                    material = sheet(),
                    products = listOf(product("P1", 60.0, 60.0) to UInt64.one)
                )
            )
            .valueOrFail()
        assertEquals(1, results.size)
        val pattern = results.first().pattern
        assertEquals(null, pattern.cutTree)
        assertEquals(1, pattern.placements.size)
        assertTrue(pattern.validate(meter(0.0)) is Ok)
    }

    @Test
    fun rowPitchIncludesKerfSoTheLastRowNeverOverflows() {
        // 板材 10×8、件 2×2、刀缝 1：行距 3，只装得下 2 行（6 件）。
        // 若按裸行高判定会挤进第 3 行（9 件），超出板高。
        // Sheet 10×8, pieces 2×2, kerf 1: the row pitch is 3, so only two rows (6 pieces) fit. Checking the bare
        // height instead would force a third row (9 pieces) past the sheet height.
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = true)
            .generate(
                request(
                    material = sheet(width = 10.0, height = 8.0),
                    products = listOf(product("P1", 2.0, 2.0) to UInt64(20uL)),
                    kerf = 1.0
                )
            )
            .valueOrFail()

        val pattern = results.last().pattern
        assertEquals(6, pattern.placements.size, "行距含刀缝后只应放 6 件")
        val sheetShape = Sheet2Shape(width = meter(10.0), height = meter(8.0))
        for (placement in pattern.placements) {
            assertTrue(
                placement.insideSheet(sheetShape).valueOrFail(),
                "放置不得越出板材：y=${placement.y.value}"
            )
        }
    }

    @Test
    fun mixedHeightShelfIsNotATwoStageGuillotine() {
        // 混高行需要第三刀才能把件与上方余料分开，因此两级断头台必须拒绝这种布局。
        // A mixed-height row needs a third cut to separate the piece from the waste above it, so a two-stage guillotine
        // must reject this layout.
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = false)
            .generate(
                request(
                    material = sheet(),
                    products = listOf(
                        product("P1", 60.0, 60.0) to UInt64.one,
                        product("P2", 40.0, 30.0) to UInt64.one
                    ),
                    process = CuttingProcess2.GuillotineTwoStage
                )
            )
        assertTrue(results is Failed)
    }

    @Test
    fun twoStageShelfEmitsValidTwoStageTree() {
        // 等高行：40×30 与 30×30 同高，可先横切分行、再竖切分件。
        // Uniform row height: 40 by 30 and 30 by 30 share the height, so rows can be cut then pieces.
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = true)
            .generate(
                request(
                    material = sheet(),
                    products = listOf(
                        product("P1", 40.0, 30.0) to UInt64.one,
                        product("P2", 30.0, 30.0) to UInt64.one
                    ),
                    process = CuttingProcess2.GuillotineTwoStage
                )
            )
            .valueOrFail()
        val pattern = results.first().pattern
        val tree = pattern.cutTree ?: kotlin.test.fail("两级断头台必须输出切分树 / a cut tree is required")
        assertEquals(2, pattern.placements.size)
        assertTrue(pattern.validate(meter(0.0)) is Ok)
        assertTrue(tree.maxStage() <= 2)
        // 刀序：先横切分行，再竖切分件。 / Cut order: rows horizontally first, then pieces vertically.
        assertEquals(CutOrientation2.Horizontal, tree.cutLines().first().orientation)
    }

    @Test
    fun threeStageShelfEmitsThreeStageTreeForMixedHeights() {
        val results = ThreeStagePatternGenerator2<Flt64>()
            .generate(
                request(
                    material = sheet(),
                    products = listOf(
                        product("P1", 60.0, 60.0) to UInt64.one,
                        product("P2", 40.0, 30.0) to UInt64.one
                    )
                )
            )
            .valueOrFail()
        val pattern = results.first().pattern
        assertEquals(CuttingProcess2.GuillotineThreeStage, pattern.process)
        assertTrue(pattern.validate(meter(0.0)) is Ok)
        val tree = pattern.cutTree ?: kotlin.test.fail("三级断头台必须输出切分树 / a cut tree is required")
        assertEquals(3, tree.maxStage())
    }

    @Test
    fun threeStageTreeIsRejectedByTwoStageProcess() {
        // 同一棵三级树交给两级工艺必须被拒绝：级数超出工艺上限。
        // Handing the same three-stage tree to a two-stage process must be rejected: the stage exceeds the process limit.
        val results = ThreeStagePatternGenerator2<Flt64>()
            .generate(
                request(
                    material = sheet(),
                    products = listOf(
                        product("P1", 60.0, 60.0) to UInt64.one,
                        product("P2", 40.0, 30.0) to UInt64.one
                    )
                )
            )
            .valueOrFail()
        val pattern = results.first().pattern
        val twoStagePattern = Pattern2(
            id = pattern.id,
            material = pattern.material,
            region = pattern.region,
            placements = pattern.placements,
            cutTree = pattern.cutTree,
            process = CuttingProcess2.GuillotineTwoStage
        )
        assertTrue(twoStagePattern.validate(meter(0.0)) is Failed)
    }

    @Test
    fun coilModeShrinksRegionToConsumedLength() {
        // 卷材 100 宽、件 40×30 共 3 件：行 1 放 2 件、行 2 放 1 件，消耗段高 60。
        // Coil of width 100 with three 40 by 30 pieces: two in row 1 and one in row 2, consuming a segment height of 60.
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = false)
            .generate(
                request(
                    material = coil(),
                    products = listOf(product("P1", 40.0, 30.0) to UInt64(3))
                )
            )
            .valueOrFail()
        // 生成器按前缀行数产出多个候选，覆盖最全的是最后一个。
        // The generator emits one candidate per prefix row count, and the last one covers the most.
        val pattern = results.last().pattern
        assertEquals(3, pattern.placements.size)
        assertEquals(60.0, pattern.region.height.value.toDouble(), 1e-9)
        assertEquals(100.0, pattern.region.width.value.toDouble(), 1e-9)
        // 卷材段高即为消耗长度，不需要额外料域。 / The coil segment height is the consumed length, so no extra region.
        assertTrue(pattern.validate(meter(0.0)) is Ok)
    }

    @Test
    fun maxCandidatesBoundsEmittedPatternsNotRows() {
        // `maxCandidates` 限制的是产出图案数：板材 10×8、件 2×2 共 20 件应铺满 4 行，
        // 上限 2 时只输出末尾（覆盖最大）的 2 个前缀，而不是把行数截成 2。
        // `maxCandidates` bounds the number of emitted patterns: a 10×8 sheet with twenty 2×2 pieces fills four
        // rows, and a limit of 2 emits only the two largest trailing prefixes instead of truncating the rows to
        // two.
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = true)
            .generate(
                request(
                    material = sheet(width = 10.0, height = 8.0),
                    products = listOf(product("P1", 2.0, 2.0) to UInt64(20uL))
                ).copy(maxCandidates = 2)
            )
            .valueOrFail()

        assertEquals(2, results.size)
        // 前缀件数应为 15 与 20（每行 5 件）。
        // The prefixes hold 15 and 20 pieces (five per row).
        assertEquals(15, results[0].pattern.placements.size)
        assertEquals(20, results[1].pattern.placements.size)
        // 覆盖最全的候选必须铺满 4 行。
        // The widest candidate must fill all four rows.
        assertEquals(8.0, results[1].pattern.region.height.value.toDouble(), 1e-9)
    }

    @Test
    fun kerfIsRespectedInPlacementAndTree() {
        val kerf = 2.0
        val results = ShelfPatternGenerator2<Flt64>(sameHeightRows = true)
            .generate(
                request(
                    material = sheet(),
                    products = listOf(
                        product("P1", 40.0, 30.0) to UInt64(2)
                    ),
                    process = CuttingProcess2.GuillotineTwoStage,
                    kerf = kerf
                )
            )
            .valueOrFail()
        val pattern = results.first().pattern
        assertTrue(pattern.validate(meter(kerf)) is Ok)
        val xs = pattern.placements.map { it.x.value.toDouble() }.sorted()
        assertEquals(0.0, xs[0], 1e-9)
        // 第二件的 x = 40 + kerf。 / The second piece starts at 40 plus the kerf.
        assertEquals(42.0, xs[1], 1e-9)
    }

    @Test
    fun contextDispatchesByProcess() {
        val context = PatternGenerationContext2<Flt64>()
        val products = listOf(product("P1", 40.0, 30.0) to UInt64(2))

        val freeform = context.generate(request(sheet(), products, CuttingProcess2.Freeform)).valueOrFail()
        assertEquals(null, freeform.first().pattern.cutTree)

        val twoStage = context
            .generate(request(sheet(), products, CuttingProcess2.GuillotineTwoStage))
            .valueOrFail()
        assertTrue(twoStage.first().pattern.cutTree != null)

        val threeStage = context
            .generate(request(sheet(), products, CuttingProcess2.GuillotineThreeStage))
            .valueOrFail()
        assertEquals(3, threeStage.first().pattern.cutTree?.maxStage())
    }

    @Test
    fun contextReplaysHistoricalPatternsAndDeduplicates() {
        val material = sheet()
        val products = listOf(product("P1", 40.0, 30.0) to UInt64(2))
        val first = PatternGenerationContext2<Flt64>()
            .generate(request(material, products))
            .valueOrFail()
        val historical = first.map { it.pattern }

        val replayed = PatternGenerationContext2<Flt64>()
            .generate(
                PatternGenerationRequest2(
                    material = material,
                    products = products,
                    kerf = meter(0.0),
                    existingPatterns = historical
                )
            )
            .valueOrFail()
        // 历史列与新生列内容相同，应被去重为同一个键。 / The historical column duplicates the generated one, so it must
        // deduplicate to a single key.
        assertEquals(historical.map { patternKey(it) }.distinct(), replayed.map { patternKey(it.pattern) }.distinct())
    }

    @Test
    fun invalidRequestIsRejected() {
        // 剩余件数为零不合法。 / A zero remaining amount is invalid.
        val invalid = ShelfPatternGenerator2<Flt64>()
            .generate(request(sheet(), listOf(product("P1", 40.0, 30.0) to UInt64.zero)))
        assertTrue(invalid is Failed)
        // 负刀缝不合法。 / A negative kerf is invalid.
        val negativeKerf = PatternGenerationRequest2(
            material = sheet(),
            products = listOf(product("P1", 40.0, 30.0) to UInt64.one),
            kerf = meter(-1.0)
        )
        assertTrue(ShelfPatternGenerator2<Flt64>().generate(negativeKerf) is Failed)
    }

    @Test
    fun reducedCostUsesShadowPrices() {
        val material = sheet()
        val products = listOf(product("P1", 40.0, 30.0) to UInt64.one)
        val pattern = ShelfPatternGenerator2<Flt64>()
            .generate(request(material, products))
            .valueOrFail()
            .first()
            .pattern

        // 无对偶时检验数等于余料面积。 / Without duals the reduced cost equals the waste area.
        val noDual = patternReducedCost(pattern, emptyMap(), Flt64.one)
        assertEquals(pattern.wasteArea.value.toDouble(), noDual.value.toDouble(), 1e-9)

        // 对偶足够大时检验数为负，表示该图案能改进目标。
        // A large enough dual makes the reduced cost negative, meaning the pattern can improve the objective.
        val withDual = patternReducedCost(pattern, mapOf("P1" to Flt64(1000.0)), Flt64.one)
        assertTrue(withDual.value.toDouble() < 0.0)
    }

    @Test
    fun improvingPatternsDropsNonImprovingCandidates() {
        val material = sheet()
        val pattern = ShelfPatternGenerator2<Flt64>()
            .generate(request(material, listOf(product("P1", 40.0, 30.0) to UInt64.one)))
            .valueOrFail()
            .first()
            .pattern
        val improving = PatternGenerationResult2(
            pattern = pattern,
            source = "test",
            reducedCost = meter(-1.0) * meter(1.0)
        )
        val nonImproving = PatternGenerationResult2(
            pattern = pattern,
            source = "test",
            reducedCost = meter(1.0) * meter(1.0)
        )
        val kept = improvingPatterns(listOf(nonImproving, improving))
        assertEquals(1, kept.size)
        assertEquals("test", kept.first().source)
    }

    @Test
    fun requestHeightBoundForCoilUsesSumOfHeights() {
        val request = PatternGenerationRequest2(
            material = coil(),
            products = listOf(
                product("P1", 40.0, 30.0) to UInt64.one,
                product("P2", 40.0, 20.0) to UInt64.one
            ),
            kerf = meter(2.0)
        )
        // 上界 = 30 + 2 + 20 = 52。 / The bound is 30 plus 2 plus 20.
        assertEquals(52.0, request.regionHeightBound().value.toDouble(), 1e-9)
    }
}

/**
 * 解包 Ret 结果。 / Unwrap a Ret result.
 *
 * @param T 结果类型 / result type
 * @param message 失败信息 / failure message
 * @return 成功值 / the successful value
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is Fatal -> kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}
