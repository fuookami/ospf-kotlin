/**
 * 余料上下文：余料最小化与余料可用性汇总。 / Waste context: waste minimisation and remnant usability summary.
 *
 * 二维套裁的余料不是一个数字（一维的「剩余幅宽」），而是一组自由矩形，其是否可用取决于物料的最小可用余料阈值。
 * 因此这里分成两件事：
 * 1. **目标**：`min Σ_j 余料面积_j · y_j`，直接进主问题；
 * 2. **汇总**：按图案统计余料面积与「是否可复用」，用于报表与后续余料库存。
 * / In 2D cutting stock the remnant is not a single number (the 1D "rest width") but a set of free rectangles whose
 * usability depends on the material's minimum usable remnant threshold. This context therefore separates two things:
 * the objective `min Σ_j waste area_j · y_j` that goes into the master problem, and a per-pattern summary of the waste
 * area and its reusability, used for reporting and for a future remnant inventory.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.wasting_minimization

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.geometry2d.plus
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.CuttingPlan2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Material2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Pattern2
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.Csp2dPatternAssignment
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.Csp2dUsageObjective

/**
 * 余料最小化目标：`min weight · Σ_j 余料面积_j · y_j`。 / Waste minimisation objective
 * `min weight · Σ_j waste area_j · y_j`.
 *
 * 为什么会同时有「消耗面积」和「余料面积」两种目标：消耗面积等价于余料最少（需求固定时两者只差常数），
 * 但当料域尺寸可变（卷材段高待定）时，余料口径会鼓励把段高收紧，而消耗面积口径会鼓励少开料，两者并不等价。
 * 因此两个目标都提供，由调用方按场景选用或加权。
 * / Why both a consumed-area and a waste-area objective: consumed area is equivalent to minimum waste only when the
 * region size is fixed (they differ by a constant given a fixed demand), but with a variable region size (an
 * undetermined coil segment height) the waste measure rewards tightening the segment while the consumed measure rewards
 * opening less material, so they are not equivalent and both are provided for the caller to choose or weight.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property weight 目标权重 / objective weight
 * @property name 目标名称 / objective name
*/
class Csp2dWasteMinimization(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val weight: FltX = FltX.one,
    val name: String = "csp2d_waste_minimization"
) {

    /**
     * 注册余料最小化目标。 / Register the waste minimisation objective.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        return Csp2dUsageObjective(
            plans = plans,
            assignment = assignment,
            coefficient = { plan -> weight * plan.wasteArea.value },
            name = name
        ).invoke(model)
    }
}

/**
 * 余料汇总条目。 / A remnant summary entry.
 *
 * @property patternId 图案编号 / pattern identifier
 * @property materialId 物料编号 / material identifier
 * @property wasteArea 余料面积 / waste area
 * @property usable 余料是否达到可复用阈值 / whether the remnant meets the reusable threshold
 * @property utilization 利用率 / utilization
*/
data class Csp2dRemnantSummary(
    val patternId: String,
    val materialId: String,
    val wasteArea: Quantity<FltX>,
    val usable: Boolean,
    val utilization: FltX
)

/**
 * 余料可用性汇总器。 / Remnant usability summariser.
 *
 * @property materials 物料目录，用于读取余料阈值 / material catalogue providing the remnant threshold
*/
class Csp2dRemnantSummarizer(
    private val materials: List<Material2<FltX>>
) {

    /**
     * 汇总图案的余料。 / Summarise the remnants of patterns.
     *
     * @param patterns 图案列表 / pattern list
     * @return 余料汇总 / remnant summary
    */
    fun summarize(patterns: List<Pattern2<FltX>>): List<Csp2dRemnantSummary> {
        val byId = materials.associateBy { it.id.value }
        return patterns.map { pattern ->
            val material = byId[pattern.material.id.value] ?: pattern.material
            Csp2dRemnantSummary(
                patternId = pattern.id.value,
                materialId = pattern.material.id.value,
                wasteArea = pattern.wasteArea,
                usable = material.isUsableRemnant(pattern.wasteArea),
                utilization = pattern.utilization()
            )
        }
    }

    /**
     * 统计可复用余料的总面积。 / Total area of the reusable remnants.
     *
     * @param summaries 汇总列表 / summary list
     * @return 可复用余料面积 / reusable remnant area
    */
    fun reusableArea(summaries: List<Csp2dRemnantSummary>, sample: Quantity<FltX>): Quantity<FltX> {
        var total = Quantity(FltX.zero, sample.unit)
        for (summary in summaries) {
            if (summary.usable) {
                total = total + summary.wasteArea
            }
        }
        return total
    }
}
