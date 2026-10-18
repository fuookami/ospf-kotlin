/**
 * Gurobi 二维列生成集成测试。 / Gurobi 2D column generation integration test.
 *
 * 使用真实 Gurobi 求解器跑通「候选列生成 → RMP(LP) → 对偶 → 加列 → 最终 MILP → 装箱 → KPI」全链路，覆盖
 * 矩形件、圆形件与混合三种场景。测试默认被 surefire 排除，仅在 `gurobi-cg-test` profile 下运行。
 * / Runs the full "candidate generation → RMP (LP) → duals → column addition → final MILP → packing → KPI"
 * chain against the real Gurobi solver, covering rectangle, circle and mixed scenarios. The test is excluded by
 * surefire by default and only runs under the `gurobi-cg-test` profile.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.times
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.PackingShape2Type
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.circleShape2
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.inflatedBy
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.rectangleShape2

/**
 * Gurobi 二维列生成集成测试。 / Gurobi 2D column generation integration test.
*/
@EnabledIfSystemProperty(named = "bpp2d.gurobi.cg.test.enabled", matches = "true")
class GurobiColumnGeneration2DTest {

    /**
     * 构造矩形物品。 / Build a rectangle item.
     *
     * @param id 物品编号 / item identifier
     * @param width 宽度（米） / width in metres
     * @param height 高度（米） / height in metres
     * @param allowRotate 是否允许旋转 / whether rotation is allowed
     * @return 物品 / item
    */
    private fun rectangleItem(
        id: String,
        width: Double,
        height: Double,
        allowRotate: Boolean = true
    ): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = rectangleShape2(
                width = FltX(width) * Meter,
                height = FltX(height) * Meter
            ).valueOrFail(),
            allowRotate = allowRotate,
            weight = FltX(1.0) * Kilogram
        )
    }

    /**
     * 构造圆形物品。 / Build a circle item.
     *
     * @param id 物品编号 / item identifier
     * @param radius 半径（米） / radius in metres
     * @return 物品 / item
    */
    private fun circleItem(id: String, radius: Double): ActualItem<FltX> {
        return ActualItem(
            id = ItemId(id),
            name = id,
            shape = circleShape2(FltX(radius) * Meter).valueOrFail(),
            allowRotate = true,
            weight = FltX(1.0) * Kilogram
        )
    }

    /**
     * 构造板材类型。 / Build a sheet type.
     *
     * @param width 宽度（米） / width in metres
     * @param height 高度（米） / height in metres
     * @return 板材类型 / sheet type
    */
    private fun sheetType(width: Double, height: Double): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(10000.0) * Kilogram,
            typeCode = SheetTypeId("STD"),
            isMain = true
        )
    }

    /**
     * 构造应用请求。 / Build the application request.
     *
     * @param items 物品与件数 / items and amounts
     * @return 应用请求 / application request
    */
    private fun request(items: List<Pair<ActualItem<FltX>, UInt64>>): ColumnGenerationApplicationRequest {
        return ColumnGenerationApplicationRequest(
            items = items.toMap(),
            itemCatalog = items.associate { it.first.id to it.first },
            sheetTypes = listOf(sheetType(20.0, 12.0)),
            iterationLimit = 12,
            maxColumnsPerIteration = 12
        )
    }

    /**
     * 校验装箱结果：板材非空、需求满足、几何合法。 / Validate the packing result: sheets present, demands met and
     * geometry legal.
     *
     * @param response 应用响应 / application response
     * @param items 物品与件数 / items and amounts
    */
    private fun assertValidPacking(
        response: ColumnGenerationApplicationResponse,
        items: List<Pair<ActualItem<FltX>, UInt64>>
    ) {
        val packing = response.packing
        assertTrue(packing != null, "装箱结果不应为空 / packing result must not be null")
        val bins = packing!!.aggregation.bins
        assertTrue(bins.isNotEmpty(), "应至少产生一块板材 / at least one sheet is expected")

        val produced = LinkedHashMap<String, Int>()
        for (bin in bins) {
            for (item in bin.items) {
                val placement = item.placement
                val itemId = placement.itemId ?: continue
                produced[itemId] = (produced[itemId] ?: 0) + 1
                // 每件都必须落在板材内。
                assertTrue(
                    placement.insideSheet(
                        fuookami.ospf.kotlin.framework.bpp2d.infrastructure.Sheet2Shape(
                            width = bin.type.width,
                            height = bin.type.height
                        )
                    ).valueOrFail(),
                    "放置越界 / placement outside the sheet: $itemId"
                )
            }
            // 同一板材内任意两件不得内部重叠。
            for (i in bin.items.indices) {
                for (j in i + 1 until bin.items.size) {
                    assertTrue(
                        !bin.items[i].placement.overlapped(bin.items[j].placement).valueOrFail(),
                        "放置重叠 / overlapping placements in ${bin.name}"
                    )
                }
            }
        }

        for ((item, amount) in items) {
            val count = produced[item.id.value] ?: 0
            assertTrue(
                count > 0,
                "物品 ${item.id} 未被放置 / item ${item.id} was not placed"
            )
            assertTrue(
                count.toULong() <= amount.toULong(),
                "物品 ${item.id} 放置数量超过需求 / item ${item.id} exceeded its demand"
            )
        }
    }

    /**
     * 校验 KPI。 / Validate the KPI snapshot.
     *
     * @param response 应用响应 / application response
    */
    private fun assertValidKpi(response: ColumnGenerationApplicationResponse) {
        val kpi = response.kpi
        assertTrue(kpi != null, "KPI 不应为空 / KPI must not be null")
        assertTrue(kpi!!.sheetCount > 0, "板材数量应为正 / sheet count must be positive")
        assertTrue(kpi.itemCount > 0, "物品件数应为正 / item count must be positive")
        assertTrue(kpi.actualArea > FltX.zero, "实际占用面积应为正 / actual area must be positive")
        assertTrue(
            kpi.boundingArea >= kpi.actualArea,
            "外接面积不应小于实际面积 / bounding area must not be smaller than actual area"
        )
        assertTrue(
            kpi.areaUtilization > FltX.zero && kpi.areaUtilization <= FltX.one,
            "面积利用率应落在 (0, 1] / area utilisation must be within (0, 1], got ${kpi.areaUtilization}"
        )
    }

    /**
     * 从 CSV 数据集加载并求解。 / Load a dataset from CSV and solve it.
     *
     * @param resource CSV 资源名 / CSV resource name
     * @return 应用响应或失败 / application response, or failure
    */
    private fun solveFromCsv(resource: String): Ret<ColumnGenerationApplicationResponse> {
        val text = javaClass.classLoader.getResource(resource)
            ?.readText()
            ?: return Failed(
                fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument,
                "CSV 资源不存在：$resource / CSV resource not found: $resource"
            )
        val dataset = when (val loaded = Bpp2dCsvDatasetLoader().load(text)) {
            is Ok -> loaded.value
            is Failed -> return Failed(loaded.error)
            is Fatal -> return Fatal(loaded.errors)
        }
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        return runBlocking {
            service.solve(
                ColumnGenerationApplicationRequest(
                    items = dataset.items.toMap(),
                    itemCatalog = dataset.items.associate { it.first.id to it.first },
                    sheetTypes = dataset.sheetTypes,
                    iterationLimit = 12,
                    maxColumnsPerIteration = 12
                )
            )
        }
    }

    @Test
    fun gurobiSolvesGroupedLayerCsvDataset() {
        val response = solveFromCsv("gurobi/grouped-layer-2d.csv").valueOrFail()
        assertTrue(response.result.finalSolved, "最终 MILP 应被求解 / the final MILP must be solved")
        assertTrue(response.packing!!.aggregation.bins.isNotEmpty(), "CSV 数据集应产出板材")
        assertTrue(response.kpi!!.itemCount > 0, "CSV 数据集应产出物品")
    }

    @Test
    fun gurobiSolvesMaterialWidthAmountCsvDataset() {
        val response = solveFromCsv("gurobi/material-width-amount-2d.csv").valueOrFail()
        assertTrue(response.result.finalSolved, "最终 MILP 应被求解 / the final MILP must be solved")
        assertTrue(response.kpi!!.rectangleCount > 0, "应出现矩形件 / rectangle items are expected")
        assertTrue(response.kpi!!.circleCount > 0, "应出现圆形件 / circle items are expected")
    }

    @Test
    fun gurobiProducesRemnantReport() {
        val items = listOf(rectangleItem("REM", 4.0, 3.0) to UInt64(2))
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking { service.solve(request(items)) }.valueOrFail()

        val kpi = response.kpi!!
        assertTrue(kpi.remnantCount > 0, "应产生余料矩形 / remnant rectangles are expected")
        assertTrue(kpi.remnantArea > FltX.zero, "余料面积应为正 / remnant area must be positive")
        assertTrue(
            kpi.remnantArea + kpi.actualArea <= kpi.sheetArea + FltX(1e-6),
            "余料与占用面积之和不应超过板材面积 / remnant plus actual area must not exceed the sheet area"
        )
    }

    @Test
    fun gurobiRespectsKerfSpacing() {
        val items = listOf(rectangleItem("K", 2.0, 1.0) to UInt64(8))
        val requestWithKerf = ColumnGenerationApplicationRequest(
            items = items.toMap(),
            itemCatalog = items.associate { it.first.id to it.first },
            sheetTypes = listOf(sheetType(20.0, 12.0)),
            iterationLimit = 8,
            maxColumnsPerIteration = 8,
            kerf = FltX(0.5) * Meter
        )
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking { service.solve(requestWithKerf) }.valueOrFail()

        assertTrue(response.packing!!.aggregation.bins.isNotEmpty(), "带刀缝时应产出板材")
        val kerf = FltX(0.5) * Meter
        for (bin in response.packing!!.aggregation.bins) {
            for (i in bin.items.indices) {
                for (j in i + 1 until bin.items.size) {
                    val lhs = bin.items[i].placement
                    val rhs = bin.items[j].placement
                    val forward = lhs.copy(shape = lhs.shape.inflatedBy(kerf))
                        .overlapped(rhs).valueOrFail()
                    val backward = lhs.overlapped(rhs.copy(shape = rhs.shape.inflatedBy(kerf)))
                        .valueOrFail()
                    assertTrue(!forward && !backward, "刀缝不足 / insufficient kerf in ${bin.name}")
                }
            }
        }
    }

    /**
     * 构造小板材类型。 / Build a small sheet type.
     *
     * @return 小板材类型 / small sheet type
    */
    private fun smallSheetType(): SheetType<FltX> {
        return SheetType(
            width = FltX(10.0) * Meter,
            height = FltX(6.0) * Meter,
            maxLoad = FltX(10000.0) * Kilogram,
            typeCode = SheetTypeId("SMALL")
        )
    }

    /**
     * 构造大板材类型。 / Build a large sheet type.
     *
     * @return 大板材类型 / large sheet type
    */
    private fun bigSheetType(): SheetType<FltX> {
        return SheetType(
            width = FltX(20.0) * Meter,
            height = FltX(12.0) * Meter,
            maxLoad = FltX(10000.0) * Kilogram,
            typeCode = SheetTypeId("BIG"),
            isMain = true
        )
    }

    @Test
    fun gurobiPrefersLessWasteAcrossSheetTypes() {
        // 需求 3 件 4×3（占用 36 m²）。小板 10×6=60 m² 一板即可覆盖（裁剪后余料 60−36=24 m²）；
        // 大板 20×12=240 m² 一板也能覆盖，但裁剪后余料 240−36=204 m²。
        // 余料最少等价于板材面积消耗最小，因此应选小板。
        // Demand is three 4×3 pieces (36 m²). A small 10×6 sheet (60 m²) covers it with 24 m² of post-trim waste; a
        // big 20×12 sheet (240 m²) covers it with 204 m² of waste. Minimum waste is equivalent to minimum sheet area
        // consumption, so the small sheet must win.
        val items = listOf(rectangleItem("W", 4.0, 3.0, allowRotate = false) to UInt64(3))
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking {
            service.solve(
                ColumnGenerationApplicationRequest(
                    items = items.toMap(),
                    itemCatalog = items.associate { it.first.id to it.first },
                    sheetTypes = listOf(smallSheetType(), bigSheetType()),
                    iterationLimit = 10,
                    maxColumnsPerIteration = 10
                )
            )
        }.valueOrFail()

        val bins = response.packing!!.aggregation.bins
        assertTrue(bins.isNotEmpty(), "应产出板材")
        assertTrue(
            bins.all { it.type.typeCode.value == "SMALL" },
            "余料最小时应只用小板，实际用了 ${bins.map { it.type.typeCode.value }}"
        )
        assertTrue(response.kpi!!.remnantArea > FltX.zero, "应有余料")
    }

    @Test
    fun gurobiSheetTypeCostCanOverrideWastePreference() {
        // 给小板加很高的类型成本，迫使模型改用大板，即使余料更多。
        // A high type cost on the small sheet forces the big sheet even though it wastes more.
        val items = listOf(rectangleItem("W", 4.0, 3.0, allowRotate = false) to UInt64(3))
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking {
            service.solve(
                ColumnGenerationApplicationRequest(
                    items = items.toMap(),
                    itemCatalog = items.associate { it.first.id to it.first },
                    sheetTypes = listOf(smallSheetType(), bigSheetType()),
                    iterationLimit = 10,
                    maxColumnsPerIteration = 10,
                    executorConfig = ColumnGenerationStandardExecutorConfig(
                        sheetTypeCosts = mapOf(SheetTypeId("SMALL") to FltX(100000.0)),
                        sheetTypeCostWeight = FltX.one
                    )
                )
            )
        }.valueOrFail()

        val bins = response.packing!!.aggregation.bins
        assertTrue(bins.isNotEmpty(), "应产出板材")
        assertTrue(
            bins.all { it.type.typeCode.value == "BIG" },
            "类型成本应压过余料偏好，实际用了 ${bins.map { it.type.typeCode.value }}"
        )
    }

    @Test
    fun gurobiSolvesRectanglePacking() {
        val items = listOf(
            rectangleItem("R1", 4.0, 3.0) to UInt64(6),
            rectangleItem("R2", 2.0, 2.0) to UInt64(8)
        )
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking { service.solve(request(items)).valueOrFail() }

        assertTrue(response.result.lpSolvedTimes > 0, "应至少求解一次 LP / at least one LP solve is expected")
        assertTrue(response.result.finalSolved, "最终 MILP 应被求解 / the final MILP must be solved")
        assertTrue(response.result.columns.isNotEmpty(), "应产生候选列 / candidate columns are expected")
        assertEquals(
            0,
            response.kpi!!.circleCount,
            "纯矩形场景不应出现圆形件 / no circle item is expected in a rectangle-only scenario"
        )
        assertValidPacking(response, items)
        assertValidKpi(response)
    }

    @Test
    fun gurobiSolvesCirclePacking() {
        val items = listOf(
            circleItem("C1", 1.0) to UInt64(12),
            circleItem("C2", 0.5) to UInt64(16)
        )
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking { service.solve(request(items)).valueOrFail() }

        assertTrue(response.result.finalSolved, "最终 MILP 应被求解 / the final MILP must be solved")
        assertEquals(
            0,
            response.kpi!!.rectangleCount,
            "纯圆形场景不应出现矩形件 / no rectangle item is expected in a circle-only scenario"
        )
        assertTrue(
            response.kpi!!.circleCount > 0,
            "应放置圆形件 / circle items are expected"
        )
        assertValidPacking(response, items)
        assertValidKpi(response)
    }

    @Test
    fun gurobiSolvesMixedShapePacking() {
        val items = listOf(
            rectangleItem("M1", 6.0, 4.0) to UInt64(4),
            circleItem("M2", 1.0) to UInt64(10)
        )
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking { service.solve(request(items)).valueOrFail() }

        assertTrue(response.result.finalSolved, "最终 MILP 应被求解 / the final MILP must be solved")
        assertTrue(response.kpi!!.rectangleCount > 0, "应放置矩形件 / rectangle items are expected")
        assertTrue(response.kpi!!.circleCount > 0, "应放置圆形件 / circle items are expected")
        assertValidPacking(response, items)
        assertValidKpi(response)
    }

    @Test
    fun circleAreaUsesPiRSquaredInKpi() {
        // 半径 1 m 的单个圆：实际面积 π ≈ 3.1416，外接面积 4。
        val items = listOf(circleItem("SINGLE", 1.0) to UInt64.one)
        val service = ColumnGenerationApplicationService(solver = GurobiDelegatingColumnGenerationSolver())
        val response = runBlocking { service.solve(request(items)).valueOrFail() }

        val kpi = response.kpi!!
        assertEquals(1, kpi.circleCount)
        assertTrue(
            kotlin.math.abs(kpi.actualArea.toDouble() - Math.PI) < 1e-6,
            "圆形件实际面积应为 πr² / circle actual area must be πr², got ${kpi.actualArea}"
        )
        assertTrue(
            kotlin.math.abs(kpi.boundingArea.toDouble() - 4.0) < 1e-6,
            "圆形件外接面积应为 4r² / circle bounding area must be 4r², got ${kpi.boundingArea}"
        )
        assertEquals(PackingShape2Type.Circle, response.packing!!.aggregation.bins.first().items.first().placement.shape.shapeType)
    }
}

/**
 * 解包 Ret 结果，失败时抛出断言错误。 / Unwrap a Ret result, throwing an assertion error on failure.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 成功时的值 / value on success
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is Fatal -> kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}
