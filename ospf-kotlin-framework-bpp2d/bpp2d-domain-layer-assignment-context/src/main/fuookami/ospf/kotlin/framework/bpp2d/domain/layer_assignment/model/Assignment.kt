/**
 * 赋值模型。 / Assignment model.
 *
 * 对应 BPP3D 的 `ImpreciseAssignment` / `PreciseAssignment`：
 * - 不精确赋值用于 RMP，每列一个变量；
 * - 精确赋值用于最终 MILP：二维排样中一个候选层就是一块板材的完整排布，因此决策变量是**每列的份数**
 *   `y_j ∈ ℤ⁺`，而不是「板材 × 层」矩阵。这样板材数量等于所有列份数之和，模型与物理含义一致；
 *   若使用矩阵，求解器可以重复选择同一列而超出需求。
 * / Mirrors BPP3D `ImpreciseAssignment` / `PreciseAssignment`: the imprecise form gives one variable per column
 * for the RMP, while the precise form uses **copies per column** `y_j ∈ ℤ⁺` for the final MILP. In 2D a candidate
 * layer already is a complete sheet layout, so the sheet count equals the sum of copies and the model matches the
 * physical meaning; with a sheet × layer matrix the solver could repeat a column and exceed the demands.
 *
 * 本文件是整个 bpp2d 框架的求解器边界，因此这里使用具体的 `FltX`；领域层保持泛型。
 * / This file is the solver boundary of the whole bpp2d framework, so it uses concrete `FltX`; the domain layers
 * stay generic.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.BinaryzationFunction
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*

/**
 * 求解器数值转换器。 / Solver value converter.
*/
private val fltXConverter: IntoValue<FltX> = IntoValue.fromConverter(FltX)

/**
 * 列变量条目类型。 / Column variable item type.
*/
typealias Bpp2dColumnVariable = AbstractVariableItem<UInt64, UInteger>

/**
 * 不精确赋值，用于列生成 RMP 阶段。 / Imprecise assignment used by the column generation RMP phase.
 *
 * @property items 物品件数需求 / item amount demands
 * @property aggregation 层聚合 / layer aggregation
*/
class ImpreciseAssignment(
    private val items: Map<ItemId, UInt64>,
    private val aggregation: LayerAggregation<FltX> = LayerAggregation()
) {
    /** 当前层集合 / current layer set */
    val layers: List<Layer<FltX>> by aggregation::layers

    /** 最近一次迭代的层 / layers of the last iteration */
    val lastIterationLayers: List<Layer<FltX>> by aggregation::lastIterationLayers

    /** 列变量分组，按迭代追加 / column variable groups, appended per iteration */
    private val variableGroups = ArrayList<UIntVariable1>()

    /** 面积目标表达式 / area objective expression */
    lateinit var area: LinearExpressionSymbol<FltX>

    /**
     * 注册赋值符号。 / Register the assignment symbols.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    fun register(model: LinearMetaModel<FltX>): Try {
        if (!::area.isInitialized) {
            area = LinearExpressionSymbol(FltX.zero, name = "area")
        }
        return when (val result = model.add(area)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 获取指定层的列变量。 / Get the column variable of a layer.
     *
     * @param layer 已注册的层 / registered layer
     * @return 列变量 / column variable
    */
    fun variableOf(layer: Layer<FltX>): Bpp2dColumnVariable {
        for (group in variableGroups) {
            if (layer.indexed && layer.index < group.shape.size) {
                return group[layer.index]
            }
        }
        return variableGroups.last()[0]
    }

    /**
     * 添加新列。 / Add new columns.
     *
     * @param iteration 迭代序号 / iteration index
     * @param newLayers 新层列表 / new layer list
     * @param model 线性元模型 / linear meta model
     * @return 去重后新增的层 / deduplicated newly added layers
    */
    fun addColumns(
        iteration: UInt64,
        newLayers: List<Layer<FltX>>,
        model: LinearMetaModel<FltX>
    ): Ret<List<Layer<FltX>>> {
        val unduplicatedLayers = aggregation.addColumns(newLayers)
        if (unduplicatedLayers.isEmpty()) {
            return Ok(emptyList())
        }

        val variables = UIntVariable1("x_${iteration.toULong()}", Shape1(unduplicatedLayers.size))
        for (layer in unduplicatedLayers) {
            // 列变量上界取「满足全部物品需求所需的最多重复次数」的保守估计。
            // The column upper bound is a conservative estimate of the repetitions needed for all demands.
            val maxAmount = items.values.maxOrNull()?.let { demand ->
                val covered = layer.units.size.toULong().coerceAtLeast(1uL)
                UInt64((demand.toULong() + covered - 1uL) / covered)
            }
            maxAmount?.let { variables[layer.index].range.leq(it) }
        }
        variableGroups.add(variables)
        when (val result = model.add(variables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::area.isInitialized) {
            area = LinearExpressionSymbol(FltX.zero, name = "area")
        }
        area.flush()
        area.asMutable() += safeLinearPolynomial(
            unduplicatedLayers.map { layer ->
                LinearMonomial(
                    layer.area.value,
                    variables[layer.index]
                )
            }
        )
        return Ok(unduplicatedLayers)
    }
}

/**
 * 精确赋值，用于最终 MILP 阶段：决策变量是每个候选层的份数。 / Precise assignment used by the final MILP phase:
 * the decision variables are the copies of each candidate layer.
 *
 * @property layers 候选层 / candidate layers
 * @property maxCopies 每列的份数上界 / per-column copy upper bound
*/
class PreciseAssignment(
    private val layers: List<Layer<FltX>>,
    private val maxCopies: List<UInt64>
) {
    /** 每列份数变量 / copies variable per column */
    lateinit var y: UIntVariable1

    /** 列是否被使用的二值化符号 / binaryzation symbols marking whether a column is used */
    lateinit var v: LinearIntermediateSymbols1<FltX>

    /** 尾列标记变量 / tail column marker variables */
    lateinit var tail: BinVariable1

    /**
     * 注册赋值符号。 / Register the assignment symbols.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    fun register(model: LinearMetaModel<FltX>): Try {
        if (!::y.isInitialized) {
            y = UIntVariable1("y", Shape1(layers.size))
            for (index in layers.indices) {
                val upper = maxCopies.getOrNull(index) ?: UInt64.one
                y[index].range.leq(if (upper < UInt64.one) UInt64.one else upper)
            }
        }
        when (val result = model.add(y)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::v.isInitialized) {
            v = LinearIntermediateSymbols1<FltX>(
                name = "v",
                shape = Shape1(layers.size)
            ) { i, _ ->
                LinearFunctionSymbolAdapter(
                    delegate = BinaryzationFunction(
                        polynomial = LinearMonomial(FltX.one, y[i]).toLinearPolynomial(),
                        converter = fltXConverter,
                        name = "v_$i"
                    ),
                    converter = fltXConverter
                )
            }
        }
        when (val result = model.add(v)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::tail.isInitialized) {
            tail = BinVariable1("tail", Shape1(layers.size))
        }
        when (val result = model.add(tail)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }

    /**
     * 展开为「列下标、层」二元组，便于遍历负载与容量。 / Flatten into column index and layer pairs for load and
     * capacity traversal.
     *
     * @return 扁平化的列索引 / flattened column indices
    */
    fun flatVariables(): List<PreciseAssignmentIndex> {
        return layers.mapIndexed { index, layer ->
            PreciseAssignmentIndex(
                layerIndex = index,
                layer = layer
            )
        }
    }
}

/**
 * 精确赋值中的单个列下标。 / A single column index inside the precise assignment.
 *
 * @property layerIndex 列下标 / column index
 * @property layer 层 / layer
*/
data class PreciseAssignmentIndex(
    val layerIndex: Int,
    val layer: Layer<FltX>
)

/**
 * 计算给定需求下每列的份数上界。 / Compute the per-column copy upper bound for the given demands.
 *
 * 上界取「覆盖该列中任一物品的需求所需份数」的最小值：再多放一份必然超出需求，因此可以安全截断，
 * 避免 MILP 重复选择同一列导致超量放置。
 * / The bound is the minimum over the items in the column of the copies needed to cover that item's demand. Any
 * additional copy would necessarily exceed a demand, so truncating here is safe and prevents the MILP from
 * repeating a column and over-placing items.
 *
 * @param layers 候选层 / candidate layers
 * @param demandEntries 需求项列表 / demand entry list
 * @return 每列份数上界 / per-column copy upper bound
*/
fun maxCopiesPerColumn(
    layers: List<Layer<FltX>>,
    demandEntries: List<Bpp2dAssignmentDemandEntry>
): List<UInt64> {
    val amountDemands = demandEntries
        .filter { it.mode == Bpp2dAssignmentDemandMode.ItemAmount }
        .associate { it.itemId to it.demand }

    return layers.map { layer ->
        var bound: ULong? = null
        for ((itemId, coverage) in layer.itemAmounts) {
            if (coverage == UInt64.zero) {
                continue
            }
            val demand = amountDemands[itemId] ?: continue
            val demandValue = when (demand) {
                is Bpp2dAssignmentDemandValue.Amount -> demand.value.toULong()
                is Bpp2dAssignmentDemandValue.Area -> 1uL
            }
            val covered = coverage.toULong().coerceAtLeast(1uL)
            val copies = (demandValue + covered - 1uL) / covered
            bound = if (bound == null) copies else minOf(bound, copies)
        }
        // 未出现在任何需求量中的列没有覆盖价值，限制为一份以避免无意义重复。
        // A column not appearing in any demand has no coverage value; it is limited to one copy.
        UInt64(bound?.coerceAtLeast(1uL) ?: 1uL)
    }
}
