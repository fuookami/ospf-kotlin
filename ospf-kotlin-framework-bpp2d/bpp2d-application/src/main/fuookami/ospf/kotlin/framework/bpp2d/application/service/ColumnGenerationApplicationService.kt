/**
 * 列生成应用服务。 / Column generation application service.
 *
 * 对应 BPP3D 的 `ColumnGenerationApplicationService`：把量纲输入转换为 `FltX` 模型输入，装配列生成算法，执行
 * 求解并输出装箱结果与 KPI。转换集中在 `toModelRequest`，领域层保持泛型。
 * / Mirrors BPP3D `ColumnGenerationApplicationService`: converts quantity inputs into `FltX` model inputs,
 * assembles the column generation algorithm, runs it and emits the packing result with KPI. Conversion is
 * concentrated in `toModelRequest` while the domain layers stay generic.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_generation.LayerGenerationContext
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.PackingContext
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service.Packer
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * 把泛型量值转换为 `FltX` 量值。 / Convert a generic quantity into an `FltX` quantity.
 *
 * @param quantity 量纲值 / quantity value
 * @return FltX 量纲值 / FltX quantity value
*/
fun Quantity<*>.bpp2dToFltXQuantity(): Quantity<FltX> {
    return Quantity(FltX(value.toString().toDouble()), unit)
}

/**
 * 列生成应用请求。 / Column generation application request.
 *
 * @property items 物品件数需求 / item amount demands
 * @property itemCatalog 物品目录 / item catalogue
 * @property sheetTypes 板材类型列表 / sheet type list
 * @property iterationLimit 最大迭代次数 / maximum iteration count
 * @property maxColumnsPerIteration 每次迭代最大候选列数 / maximum candidate columns per iteration
 * @property finalMilpEnabled 是否启用最终 MILP / whether the final MILP is enabled
 * @property executorConfig 执行器配置 / executor config
*/
data class ColumnGenerationApplicationRequest(
    val items: Map<ActualItem<FltX>, UInt64>,
    val itemCatalog: Map<ItemId, ActualItem<FltX>>,
    val sheetTypes: List<SheetType<FltX>>,
    val iterationLimit: Int = 32,
    val maxColumnsPerIteration: Int = 16,
    val finalMilpEnabled: Boolean = true,
    val executorConfig: ColumnGenerationStandardExecutorConfig = ColumnGenerationStandardExecutorConfig()
) {

    /**
     * 构建需求项列表：每个物品同时给出件数需求与可选面积需求。 / Build the demand entry list: each item yields an
     * amount demand and, when requested, an area demand.
     *
     * @param withAreaDemand 是否为每个物品附加面积需求 / whether an area demand is added per item
     * @return 需求项列表 / demand entry list
    */
    fun demandEntries(withAreaDemand: Boolean = false): List<Bpp2dAssignmentDemandEntry> {
        val entries = ArrayList<Bpp2dAssignmentDemandEntry>(items.size * 2)
        for ((item, amount) in items) {
            entries.add(
                Bpp2dAssignmentDemandEntry(
                    mode = Bpp2dAssignmentDemandMode.ItemAmount,
                    itemId = item.id,
                    demand = Bpp2dAssignmentDemandValue.Amount(amount)
                )
            )
            if (withAreaDemand) {
                val perPieceArea = item.shape.boundingArea
                // 总量 = 单件面积 × 件数；件数按数值类型逐次累加，避免引入 Double。
                // Total = per-piece area times amount; the amount is accumulated numerically to avoid Double.
                val totalArea = perPieceArea * amountScaler(perPieceArea.value, amount)
                entries.add(
                    Bpp2dAssignmentDemandEntry(
                        mode = Bpp2dAssignmentDemandMode.ItemArea,
                        itemId = item.id,
                        demand = Bpp2dAssignmentDemandValue.Area(totalArea)
                    )
                )
            }
        }
        return entries
    }

    /**
     * 把无符号整数换算为 `FltX`。 / Convert an unsigned integer to `FltX`.
     *
     * @param sample 样本数值 / sample value
     * @param amount 件数 / amount
     * @return FltX 数值 / FltX value
    */
    private fun amountScaler(sample: FltX, amount: UInt64): FltX {
        var acc = sample.constants.zero
        var remaining = amount
        while (remaining > UInt64.zero) {
            acc += sample.constants.one
            remaining -= UInt64.one
        }
        return acc
    }
}

/**
 * 量纲列生成应用请求。 / Quantity-typed column generation application request.
 *
 * @param V 数值类型 / numeric type
 * @property items 物品件数需求 / item amount demands
 * @property sheetTypes 量纲板材类型列表 / quantity sheet type list
 * @property iterationLimit 最大迭代次数 / maximum iteration count
 * @property maxColumnsPerIteration 每次迭代最大候选列数 / maximum candidate columns per iteration
 * @property finalMilpEnabled 是否启用最终 MILP / whether the final MILP is enabled
*/
data class ColumnGenerationQuantityApplicationRequest<V : FloatingNumber<V>>(
    val items: Map<QuantityItem<V>, UInt64>,
    val sheetTypes: List<QuantitySheetType<V>>,
    val iterationLimit: Int = 32,
    val maxColumnsPerIteration: Int = 16,
    val finalMilpEnabled: Boolean = true
) {

    /**
     * 转换为 `FltX` 模型请求。 / Convert into the `FltX` model request.
     *
     * @return 模型请求或失败 / model request, or failure
    */
    fun toModelRequest(): Ret<ColumnGenerationApplicationRequest> {
        val itemPairs = ArrayList<Pair<ActualItem<FltX>, UInt64>>(items.size)
        val catalog = LinkedHashMap<ItemId, ActualItem<FltX>>()
        for ((item, amount) in items) {
            val model = when (val result = item.toModel()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            itemPairs.add(model to amount)
            catalog[model.id] = model
        }
        return ok(
            ColumnGenerationApplicationRequest(
                items = itemPairs.toMap(),
                itemCatalog = catalog,
                sheetTypes = sheetTypes.map { it.toModel() },
                iterationLimit = iterationLimit,
                maxColumnsPerIteration = maxColumnsPerIteration,
                finalMilpEnabled = finalMilpEnabled
            )
        )
    }
}

/**
 * 列生成应用响应。 / Column generation application response.
 *
 * @property result 列生成结果 / column generation result
 * @property packing 装箱结果（可选） / packing result (optional)
 * @property kpi 关键指标 / key indicators
*/
data class ColumnGenerationApplicationResponse(
    val result: ColumnGenerationResult,
    val packing: fuookami.ospf.kotlin.framework.bpp2d.domain.packing.PackingResult? = null,
    val kpi: ColumnGenerationPackingSnapshot? = null
)

/**
 * 列生成应用服务。 / Column generation application service.
 *
 * @property solver 列生成求解器 / column generation solver
 * @property algorithmFactory 算法工厂（可选） / algorithm factory (optional)
*/
class ColumnGenerationApplicationService(
    private val solver: ColumnGenerationSolver,
    private val algorithmFactory: ((ColumnGenerationApplicationRequest, ColumnGenerationStandardExecutors) -> ColumnGenerationAlgorithm)? = null
) {

    /**
     * 执行列生成求解。 / Execute the column generation solve.
     *
     * @param request 应用请求 / application request
     * @return 应用响应或失败 / application response, or failure
    */
    suspend fun solve(request: ColumnGenerationApplicationRequest): Ret<ColumnGenerationApplicationResponse> {
        val demandEntries = request.demandEntries()
        val executors = ColumnGenerationStandardExecutors(
            solver = solver,
            items = request.items.entries.associate { (item, amount) -> item.id to amount },
            itemCatalogue = request.itemCatalog,
            demandEntries = demandEntries,
            sheetTypes = request.sheetTypes,
            config = request.executorConfig
        )
        val generator = LayerGenerationContext<FltX>()
        val algorithm = algorithmFactory?.invoke(request, executors)
            ?: ColumnGenerationAlgorithm(
                layerGenerator = generator,
                rmpSolver = executors.rmpSolver(),
                finalMilpSolver = executors.finalSolver(),
                layerRequestBuilder = executors.requestBuilder(),
                // 首次 LP 必须可行：先用候选生成器产出一批种子列，否则空变量模型的 RMP 会直接不可行，
                // Gurobi 也不会给出对偶解。
                // The first LP must be feasible: seed columns are produced by the candidate generator first.
                // Otherwise the empty-variable RMP is immediately infeasible and Gurobi reports no dual solution.
                initialColumnsWithResult = {
                    Ok(
                        seedColumns(
                            generator = generator,
                            request = request,
                            executors = executors
                        )
                    )
                }
            )

        val result = when (
            val outcome = algorithm.solve(
                ColumnGenerationConfig(
                    iterationLimit = request.iterationLimit,
                    maxColumnsPerIteration = request.maxColumnsPerIteration,
                    finalMilpEnabled = request.finalMilpEnabled
                )
            )
        ) {
            is Ok -> outcome.value
            is Failed -> return Failed(outcome.error)
            is Fatal -> return Fatal(outcome.errors)
        }

        // 列生成保证「覆盖需求」；最后一板通常会超出需求量，按需求裁剪后再装箱。
        // Column generation guarantees demand coverage; the last sheet usually exceeds the demand, so it is trimmed
        // to the demand before packing.
        val trimmedSheets = trimToDemand(sheets = result.sheets, items = request.items)
        val packedResult = result.copy(sheets = trimmedSheets)
        val packingResult = when (val packed = Packer().invoke(trimmedSheets)) {
            is Ok -> packed.value
            is Failed -> return Failed(packed.error)
            is Fatal -> return Fatal(packed.errors)
        }

        return ok(
            ColumnGenerationApplicationResponse(
                result = packedResult,
                packing = packingResult,
                kpi = ColumnGenerationPackingAnalyzer().analyze(packedResult, packingResult)
            )
        )
    }

    /**
     * 按需求裁剪板材中的放置：超出需求的部分被丢弃。 / Trim placements in the sheets to the demands: anything
     * beyond the demand is dropped.
     *
     * 二维排样中一个候选层是一整块板材的排布，最后一个板材按常理只需产出剩余需求的件数。
     * / In 2D a candidate layer is a whole-sheet layout, and the last sheet normally only produces the remaining
     * demand.
     *
     * @param sheets 候选板材 / candidate sheets
     * @param items 物品与需求件数 / items and demanded amounts
     * @return 裁剪后的板材 / trimmed sheets
    */
    private fun trimToDemand(
        sheets: List<Sheet<FltX>>,
        items: Map<ActualItem<FltX>, UInt64>
    ): List<Sheet<FltX>> {
        val demands = items.entries.associate { it.key.id.value to it.value.toULong() }
        val produced = LinkedHashMap<String, ULong>()

        val trimmed = ArrayList<Sheet<FltX>>(sheets.size)
        for (sheet in sheets) {
            val layers = ArrayList<SheetLayer<FltX>>(sheet.layers.size)
            for (sheetLayer in sheet.layers) {
                val keptLayers = ArrayList<Layer<FltX>>(sheetLayer.layers.size)
                for (layer in sheetLayer.layers) {
                    val units = ArrayList<LayerPlacement2<FltX>>(layer.units.size)
                    for (unit in layer.units) {
                        val itemId = unit.itemId ?: continue
                        val demand = demands[itemId] ?: 0uL
                        val used = produced[itemId] ?: 0uL
                        if (used >= demand) {
                            continue
                        }
                        produced[itemId] = used + 1uL
                        units.add(unit)
                    }
                    if (units.isEmpty()) {
                        continue
                    }
                    keptLayers.add(
                        Layer(
                            iteration = layer.iteration,
                            from = layer.from,
                            sheet = layer.sheet,
                            shape = layer.shape,
                            units = units
                        )
                    )
                }
                if (keptLayers.isEmpty()) {
                    continue
                }
                layers.add(
                    SheetLayer(
                        type = sheetLayer.type,
                        shape = sheetLayer.shape,
                        layers = keptLayers,
                        batchNo = sheetLayer.batchNo
                    )
                )
            }
            if (layers.isEmpty()) {
                continue
            }
            trimmed.add(
                Sheet(
                    type = sheet.type,
                    layers = layers,
                    batchNo = sheet.batchNo
                )
            )
        }
        return trimmed
    }

    /**
     * 生成种子列：用候选生成器为一轮定价产出初始层集合。 / Generate seed columns: produce the initial layer set
     * for one pricing round using the candidate generator.
     *
     * @param generator 层生成器 / layer generator
     * @param request 应用请求 / application request
     * @param executors 标准执行器 / standard executors
     * @return 种子列列表 / seed column list
    */
    private fun seedColumns(
        generator: LayerGenerationContext<FltX>,
        request: ColumnGenerationApplicationRequest,
        executors: ColumnGenerationStandardExecutors
    ): List<Layer<FltX>> {
        val seedRequest = executors.requestBuilder().build(
            ColumnGenerationState(iteration = 0, columns = emptyList()),
            ColumnGenerationConfig(
                iterationLimit = request.iterationLimit,
                maxColumnsPerIteration = request.maxColumnsPerIteration
            )
        )
        return generator.generate(seedRequest)
            .map { it.layer }
            .distinct()
    }
}
