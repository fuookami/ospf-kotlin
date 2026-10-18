/**
 * 层分配上下文。 / Layer assignment context.
 *
 * 对应 BPP3D 的 `LayerAssignmentContext`：作为列生成建模的领域入口，覆盖注册、加列、移除列、影子价格刷新
 * 与结果提取五个生命周期阶段（见 `.rules/framework-architecture.md` 第 4 节）。
 * / Mirrors BPP3D `LayerAssignmentContext`: the domain entry point for column-generation modelling, covering the
 * five lifecycle stages of register, add columns, remove columns, shadow price refresh and solution extraction
 * (see `.rules/framework-architecture.md` section 4).
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.AreaMinimization
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.DemandConstraint
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.SheetAmountMinimization
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.SheetCapacityConstraint
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.SheetTypeCostMinimization
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits.SheetAreaMinimization

/**
 * 层分配上下文。 / Layer assignment context.
 *
 * @property items 物品件数需求 / item amount demands
 * @property aggregation 层聚合 / layer aggregation
*/
class LayerAssignmentContext(
    private val items: Map<ItemId, UInt64> = emptyMap(),
    val aggregation: LayerAggregation<FltX> = LayerAggregation()
) {

    /**
     * 注册 RMP 阶段的赋值、负载与需求约束。 / Register assignment, load and demand constraint for the RMP phase.
     *
     * @param model 线性元模型 / linear meta model
     * @param demandEntries 需求项列表 / demand entry list
     * @param overEnabled 是否启用超载 / whether over-load is enabled
     * @return 赋值与负载构件或失败 / assignment and load artifacts, or failure
    */
    fun registerRmp(
        model: LinearMetaModel<FltX>,
        demandEntries: List<Bpp2dAssignmentDemandEntry>,
        overEnabled: Boolean = false
    ): Ret<RmpAssignmentArtifacts> {
        val assignment = ImpreciseAssignment(
            items = items,
            aggregation = aggregation
        )
        when (val result = assignment.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val load = ImpreciseLoad(
            demandEntries = demandEntries,
            assignment = assignment,
            overEnabled = overEnabled,
            lessEnabled = true
        )
        when (val result = load.registerLoad(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = load.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val demandConstraint = DemandConstraint(load = load, demandEntries = demandEntries)
        demandConstraint.register(model)
        when (val result = demandConstraint.invoke(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok(
            RmpAssignmentArtifacts(
                assignment = assignment,
                load = load,
                demandConstraint = demandConstraint
            )
        )
    }

    /**
     * 向 RMP 追加列。 / Add columns to the RMP.
     *
     * @param assignment 不精确赋值 / imprecise assignment
     * @param load 不精确负载 / imprecise load
     * @param iteration 迭代序号 / iteration index
     * @param newLayers 新层列表 / new layer list
     * @param model 线性元模型 / linear meta model
     * @return 去重后新增的层或失败 / deduplicated newly added layers, or failure
    */
    fun addColumns(
        assignment: ImpreciseAssignment,
        load: ImpreciseLoad,
        iteration: UInt64,
        newLayers: List<Layer<FltX>>,
        model: LinearMetaModel<FltX>
    ): Ret<List<Layer<FltX>>> {
        val added = when (val result = assignment.addColumns(iteration, newLayers, model)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (added.isNotEmpty()) {
            load.appendColumnTerms()
        }
        return ok(added)
    }

    /**
     * 移除列。 / Remove columns.
     *
     * @param layers 要移除的层 / layers to remove
    */
    fun removeColumns(layers: List<Layer<FltX>>) {
        aggregation.removeColumns(layers)
    }

    /**
     * 注册最终 MILP 阶段的赋值、负载、需求约束与容量约束。 / Register assignment, load, demand constraint and
     * capacity constraint for the final MILP phase.
     *
     * @param model 线性元模型 / linear meta model
     * @param bins 候选板材 / candidate sheets
     * @param layers 候选层 / candidate layers
     * @param demandEntries 需求项列表 / demand entry list
     * @param enableCapacityConstraint 是否启用板材容量约束 / whether the sheet capacity constraint is enabled
     * @return 精确赋值与容量构件或失败 / precise assignment and capacity artifacts, or failure
    */
    fun registerFinal(
        model: LinearMetaModel<FltX>,
        layers: List<Layer<FltX>>,
        demandEntries: List<Bpp2dAssignmentDemandEntry>,
        enableCapacityConstraint: Boolean = true
    ): Ret<FinalAssignmentArtifacts> {
        val assignment = PreciseAssignment(
            layers = layers,
            maxCopies = maxCopiesPerColumn(layers = layers, demandEntries = demandEntries)
        )
        when (val result = assignment.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val load = PreciseLoad(
            demandEntries = demandEntries,
            assignment = assignment,
            overEnabled = false,
            lessEnabled = true
        )
        when (val result = load.registerLoad(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = load.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val demandConstraint = DemandConstraint(load = load, demandEntries = demandEntries)
        demandConstraint.register(model)
        when (val result = demandConstraint.invoke(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val capacity = PreciseLoadCapacity(
            layers = layers,
            assignment = assignment
        )
        when (val result = capacity.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        if (enableCapacityConstraint) {
            when (val result = SheetCapacityConstraint(capacity = capacity).invoke(model)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok(
            FinalAssignmentArtifacts(
                assignment = assignment,
                load = load,
                demandConstraint = demandConstraint,
                capacity = capacity
            )
        )
    }

    /**
     * 注册板材面积消耗最小化目标（主目标，等价于余料最少）。 / Register the sheet area consumption minimisation
     * objective (primary, equivalent to minimum waste).
     *
     * @param assignment 精确赋值 / precise assignment
     * @param layers 候选列 / candidate columns
     * @param model 线性元模型 / linear meta model
     * @param weight 目标权重 / objective weight
     * @return 操作结果 / operation result
    */
    fun registerSheetAreaMinimization(
        assignment: PreciseAssignment,
        layers: List<Layer<FltX>>,
        model: LinearMetaModel<FltX>,
        weight: FltX = FltX.one
    ): Try {
        return SheetAreaMinimization(
            layers = layers,
            assignment = assignment,
            weight = weight
        ).invoke(model)
    }

    /**
     * 注册板材类型成本目标（次目标）。 / Register the per-sheet-type cost objective (secondary).
     *
     * @param assignment 精确赋值 / precise assignment
     * @param layers 候选列 / candidate columns
     * @param model 线性元模型 / linear meta model
     * @param typeCost 板材类型到成本的映射 / sheet type to cost mapping
     * @param weight 目标权重 / objective weight
     * @return 操作结果 / operation result
    */
    fun registerSheetTypeCostMinimization(
        assignment: PreciseAssignment,
        layers: List<Layer<FltX>>,
        model: LinearMetaModel<FltX>,
        typeCost: Map<SheetTypeId, FltX>,
        weight: FltX = FltX.one
    ): Try {
        return SheetTypeCostMinimization(
            layers = layers,
            assignment = assignment,
            typeCost = typeCost,
            weight = weight
        ).invoke(model)
    }

    /**
     * 注册 RMP 的层面积最小化目标。 / Register the RMP layer area minimisation objective.
     *
     * @param assignment 不精确赋值 / imprecise assignment
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    fun registerAreaMinimization(
        assignment: ImpreciseAssignment,
        model: LinearMetaModel<FltX>
    ): Try {
        return AreaMinimization(assignment = assignment).invoke(model)
    }

    /**
     * 注册最终 MILP 的板材数量最小化目标。 / Register the final MILP sheet amount minimisation objective.
     *
     * @param assignment 精确赋值 / precise assignment
     * @param bins 候选板材 / candidate sheets
     * @param model 线性元模型 / linear meta model
     * @param coefficient 单个板材的系数 / per-sheet coefficient
     * @return 操作结果 / operation result
    */
    fun registerSheetAmountMinimization(
        assignment: PreciseAssignment,
        layers: List<Layer<FltX>>,
        model: LinearMetaModel<FltX>,
        coefficient: FltX = FltX.one
    ): Try {
        return SheetAmountMinimization(
            layers = layers,
            assignment = assignment,
            coefficient = { coefficient }
        ).invoke(model)
    }
}

/**
 * RMP 阶段的赋值构件。 / Assignment artifacts of the RMP phase.
 *
 * @property assignment 不精确赋值 / imprecise assignment
 * @property load 不精确负载 / imprecise load
 * @property demandConstraint 需求约束 / demand constraint
*/
data class RmpAssignmentArtifacts(
    val assignment: ImpreciseAssignment,
    val load: ImpreciseLoad,
    val demandConstraint: DemandConstraint
)

/**
 * 最终 MILP 阶段的赋值构件。 / Assignment artifacts of the final MILP phase.
 *
 * @property assignment 精确赋值 / precise assignment
 * @property load 精确负载 / precise load
 * @property demandConstraint 需求约束 / demand constraint
 * @property capacity 装载容量 / load capacity
*/
data class FinalAssignmentArtifacts(
    val assignment: PreciseAssignment,
    val load: PreciseLoad,
    val demandConstraint: DemandConstraint,
    val capacity: PreciseLoadCapacity
)