/**
 * 二维套裁问题定义与解。 / 2D cutting stock problem definition and solution.
 *
 * 应用层是唯一允许把求解器数值类型与领域泛型模型结合的地方：问题定义本身仍以 `FltX` 具体化，
 * 因为主问题变量与目标都在这个边界上。
 * / The application layer is the only place where solver numeric types meet the generic domain model: the problem
 * definition itself is concretised on `FltX`, because the master problem variables and objectives live on this boundary.
*/
package fuookami.ospf.kotlin.framework.csp2d.application.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp2d.domain.sheet_assignment.Csp2dSheetAssignmentConfig
import fuookami.ospf.kotlin.framework.csp2d.domain.yield.Csp2dYieldConfig
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.CuttingProcess2

/**
 * 主目标口径。 / The primary objective measure.
*/
enum class Csp2dObjectiveKind {
    /** 消耗段高最小（卷材条带排样）/ minimum consumed segment height (coil strip packing) */
    ConsumedLength,

    /** 板材张数最小（定尺）/ minimum sheet count (fixed sheets) */
    SheetCount,

    /** 余料面积最小 / minimum waste area */
    Waste
}

/**
 * 列生成配置。 / Column generation configuration.
 *
 * @property maxCandidatesPerIteration 每轮每物料候选数上限 / per-round candidate limit per material
 * @property iterationLimit 迭代上限 / iteration limit
 * @property primary 主目标口径，为空时按物料形态自动选择 / primary objective, auto-selected by material kind when null
 * @property primaryWeight 主目标权重 / primary weight
 * @property sheetCountWeight 张数次目标权重 / sheet count secondary weight
 * @property wasteWeight 余料次目标权重 / waste secondary weight
*/
data class Csp2dConfiguration(
    val maxCandidatesPerIteration: Int = 16,
    val iterationLimit: Int = 6,
    val primary: Csp2dObjectiveKind? = null,
    val primaryWeight: FltX = FltX.one,
    val sheetCountWeight: FltX = FltX(1e-6),
    val wasteWeight: FltX = FltX.zero
)

/**
 * 二维套裁问题。 / A 2D cutting stock problem.
 *
 * @property materials 物料列表 / material list
 * @property products 产品列表 / product list
 * @property demands 产品需求 / product demands
 * @property machines 设备列表 / machine list
 * @property costars 副产物列表 / by-product list
 * @property process 裁切工艺 / cutting process
 * @property kerf 刀缝宽度 / kerf width
 * @property config 配置 / configuration
 * @property yieldConfig 良率配置 / yield configuration
 * @property sheetConfig 板长分配配置 / sheet assignment configuration
*/
data class Csp2dProblem(
    val materials: List<Material2<FltX>>,
    val products: List<Product2<FltX>>,
    val demands: List<ProductDemand2<FltX>>,
    val kerf: Quantity<FltX>,
    val machines: List<Machine2<FltX>> = emptyList(),
    val costars: List<Costar2<FltX>> = emptyList(),
    val process: CuttingProcess2 = CuttingProcess2.Freeform,
    val config: Csp2dConfiguration = Csp2dConfiguration(),
    val yieldConfig: Csp2dYieldConfig = Csp2dYieldConfig(),
    val sheetConfig: Csp2dSheetAssignmentConfig = Csp2dSheetAssignmentConfig()
) {

    /**
     * 校验问题定义。 / Validate the problem definition.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        if (materials.isEmpty()) {
            return Failed(
                Csp2dValidationError(detail = "问题没有物料 / The problem has no material")
            )
        }
        if (demands.isEmpty()) {
            return Failed(
                Csp2dValidationError(detail = "问题没有需求 / The problem has no demand")
            )
        }
        for (material in materials) {
            when (val result = material.validate()) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return result
            }
        }
        when (val result = ProductDemands2(demands).validate()) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return result
        }
        for (costar in costars) {
            when (val result = costar.validate()) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return result
            }
        }
        return ok
    }

    /**
     * 产品编号到产品的映射。 / Map from product identifier to product.
     *
     * @return 产品映射 / product map
    */
    fun productsById(): Map<String, Product2<FltX>> {
        return products.associateBy { it.id.value }
    }

    /**
     * 选择主目标口径。 / Select the primary objective measure.
     *
     * 配置显式给出时以配置为准；否则卷材用「消耗段高」、定尺用「板材张数」，这与两种模式的经典目标一致。
     * / An explicit configuration wins; otherwise coils use the consumed segment height and fixed sheets use the sheet
     * count, matching the classic objective of each mode.
     *
     * @return 目标口径 / objective measure
    */
    fun primaryObjective(): Csp2dObjectiveKind {
        config.primary?.let { return it }
        return if (materials.all { it.isCoil }) {
            Csp2dObjectiveKind.ConsumedLength
        } else {
            Csp2dObjectiveKind.SheetCount
        }
    }

    /**
     * 需求件数总和，作为使用次数变量的上界。 / Total demanded piece count, used as the usage variable bound.
     *
     * @return 件数总和 / total piece count
    */
    fun totalPieceDemand(): UInt64 {
        var total = UInt64.zero
        for (demand in demands) {
            val pieces = demand.piecesOrNull() ?: continue
            total = UInt64(total.toInt() + pieces.toInt())
        }
        return if (total == UInt64.zero) UInt64.one else total
    }
}

/**
 * 带使用次数的方案。 / A plan together with its usage count.
 *
 * @property plan 裁切方案 / cutting plan
 * @property usage 使用次数 / usage count
*/
data class Csp2dPlanUsage(
    val plan: CuttingPlan2<FltX>,
    val usage: Int
)

/**
 * 二维套裁解。 / A 2D cutting stock solution.
 *
 * @property usages 带使用次数的方案，仅保留使用次数为正的 / plan usages, positive usages only
 * @property consumedLength 消耗段高之和 / total consumed segment height
 * @property sheetCount 板材张数 / sheet count
 * @property usedArea 成品占用面积 / area occupied by finished parts
 * @property wasteArea 余料面积 / remnant area
 * @property utilization 利用率 / utilization
 * @property coverage 产品编号到产出件数的映射 / product identifier to produced piece count
 * @property iterations 列生成迭代次数，MILP 路径为 1 / column generation iteration count, 1 for the MILP path
 * @property kpi 关键指标 / key indicators
*/
data class Csp2dSolution(
    val usages: List<Csp2dPlanUsage>,
    val consumedLength: FltX,
    val sheetCount: Int,
    val usedArea: FltX,
    val wasteArea: FltX,
    val utilization: FltX,
    val coverage: Map<String, Int>,
    val iterations: Int,
    val kpi: Map<String, String> = emptyMap()
) {

    /**
     * 是否覆盖全部需求。 / Whether the solution covers every demand.
     *
     * @param problem 问题 / problem
     * @return 是否覆盖 / whether covered
    */
    fun covers(problem: Csp2dProblem): Boolean {
        for (demand in problem.demands) {
            val required = demand.piecesOrNull()?.toInt() ?: continue
            val produced = coverage[demand.product.id.value] ?: 0
            if (produced < required) {
                return false
            }
        }
        return true
    }
}
