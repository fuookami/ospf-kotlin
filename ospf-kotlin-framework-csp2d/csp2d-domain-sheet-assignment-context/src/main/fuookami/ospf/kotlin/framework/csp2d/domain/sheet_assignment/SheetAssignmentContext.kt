/**
 * 板长分配上下文：卷材消耗长度与定尺板材张数。 / Sheet assignment context: consumed coil length and sheet count.
 *
 * 这是 csp1d `length-assignment`（动态卷长推导）在二维的对应物，也是 csp2d 相对 bpp2d 的差异化能力：
 * 卷材模式下料域高度本身就是决策结果，因此主问题要显式约束「消耗段高之和」并检测超长；定尺模式下则是张数。
 * / This is the 2D counterpart of csp1d `length-assignment` (dynamic coil length derivation) and the capability that
 * distinguishes csp2d from bpp2d: in coil mode the region height is itself a decision outcome, so the master problem
 * must constrain the total consumed segment height and detect over-length, while in sheet mode it is the sheet count.
 *
 * 两种模式的切换不需要两套代码：同一组 `Σ 高度_j · y_j` 表达式，定尺模式下只做报表，卷材模式下进入约束与目标。
 * / Switching between the modes needs no second code path: the same `Σ height_j · y_j` expression is only reported in
 * sheet mode, while in coil mode it enters constraints and objectives.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.sheet_assignment

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.CuttingPlan2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Material2
import fuookami.ospf.kotlin.framework.csp2d.domain.material.model.Material2Kind
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.Csp2dPatternAssignment
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.Csp2dUsageObjective
import fuookami.ospf.kotlin.framework.csp2d.domain.produce.safeCsp2dPolynomial
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError

/**
 * 板长分配配置。 / Sheet assignment configuration.
 *
 * @property maxConsumedLength 卷材允许的最大消耗长度，为空表示不限 / maximum allowed consumed coil length, null meaning
 * unlimited
 * @property overLengthWeight 超长惩罚权重，为空表示超长作为硬约束 / over-length penalty weight, null meaning over-length is a
 * hard constraint
*/
data class Csp2dSheetAssignmentConfig(
    val maxConsumedLength: FltX? = null,
    val overLengthWeight: FltX? = null
)

/**
 * 消耗段高约束：`Σ_j 段高_j · y_j ≤ 最大消耗长度`。 / Consumed segment height constraint
 * `Σ_j height_j · y_j ≤ max consumed length`.
 *
 * 只对卷材注册；定尺板材的「长度」就是板高，不参与约束。
 * / Registered for coils only; a fixed sheet's "length" is its height and does not take part.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property config 配置 / configuration
 * @property name 约束名称 / constraint name
*/
class Csp2dConsumedLengthConstraint(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    private val config: Csp2dSheetAssignmentConfig,
    val name: String = "csp2d_consumed_length"
) {

    /**
     * 注册消耗段高约束。 / Register the consumed segment height constraint.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        val limit = config.maxConsumedLength ?: return ok
        val monomials = ArrayList<LinearMonomial<FltX>>()
        for ((index, plan) in plans.withIndex()) {
            if (plan.material.kind != Material2Kind.Coil) {
                continue
            }
            monomials.add(LinearMonomial(plan.pattern.region.height.value, assignment.usage[index]))
        }
        if (monomials.isEmpty()) {
            return ok
        }
        val relation = LinearInequality(
            safeCsp2dPolynomial(monomials),
            LinearPolynomial(emptyList(), limit),
            Comparison.LE
        )
        when (val result = model.addConstraint(relation = relation, name = name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 消耗长度最小化目标：`min Σ_j 段高_j · y_j`（卷材条带排样的经典目标）。 / Consumed length minimisation objective
 * `min Σ_j height_j · y_j`, the classic strip packing objective.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property name 目标名称 / objective name
*/
class Csp2dConsumedLengthObjective(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    val name: String = "csp2d_consumed_length"
) {

    /**
     * 注册消耗长度目标。 / Register the consumed length objective.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        return Csp2dUsageObjective(
            plans = plans,
            assignment = assignment,
            coefficient = { plan ->
                if (plan.material.kind == Material2Kind.Coil) {
                    plan.pattern.region.height.value
                } else {
                    FltX.zero
                }
            },
            name = name
        ).invoke(model)
    }
}

/**
 * 板材张数最小化目标：`min Σ_j y_j`（定尺模式的经典目标）。 / Sheet count minimisation objective `min Σ_j y_j`, the
 * classic fixed-sheet objective.
 *
 * @property plans 候选方案 / candidate plans
 * @property assignment 图案使用量变量 / pattern usage variables
 * @property name 目标名称 / objective name
*/
class Csp2dSheetCountObjective(
    private val plans: List<CuttingPlan2<FltX>>,
    private val assignment: Csp2dPatternAssignment,
    val name: String = "csp2d_sheet_count"
) {

    /**
     * 注册张数目标。 / Register the sheet count objective.
     *
     * @param model 线性元模型 / linear meta model
     * @return 注册结果 / registration result
    */
    fun invoke(model: LinearMetaModel<FltX>): Try {
        return Csp2dUsageObjective(
            plans = plans,
            assignment = assignment,
            coefficient = { FltX.one },
            name = name
        ).invoke(model)
    }
}

/**
 * 板长分配结果。 / A sheet assignment result.
 *
 * @property consumedLength 消耗段高之和（卷材）/ total consumed segment height for coils
 * @property sheetCount 板材张数（定尺）/ sheet count for fixed sheets
 * @property overLength 超出最大消耗长度的部分 / the amount exceeding the maximum consumed length
 * @property overLengthDetected 是否检测到超长 / whether over-length was detected
*/
data class Csp2dSheetAssignmentResult(
    val consumedLength: FltX,
    val sheetCount: Int,
    val overLength: FltX,
    val overLengthDetected: Boolean
)

/**
 * 由使用次数计算板长分配结果。 / Compute the sheet assignment result from usage counts.
 *
 * 纯函数，便于在列生成或恢复阶段复用，不需要访问模型。
 * / A pure function so that column generation and recovery can reuse it without touching the model.
 *
 * @param plans 候选方案 / candidate plans
 * @param usages 与方案一一对应的使用次数 / usage counts aligned with the plans
 * @param config 配置 / configuration
 * @return 板长分配结果 / sheet assignment result
*/
fun csp2dSheetAssignmentOf(
    plans: List<CuttingPlan2<FltX>>,
    usages: List<Int>,
    config: Csp2dSheetAssignmentConfig
): Ret<Csp2dSheetAssignmentResult> {
    if (plans.size != usages.size) {
        return Failed(
            Csp2dValidationError(
                detail = "方案数与使用次数不一致：${plans.size} vs ${usages.size} / " +
                        "The plan count and the usage count do not match"
            )
        )
    }
    var consumed = FltX.zero
    var sheetCount = 0
    for ((index, plan) in plans.withIndex()) {
        val usage = usages[index]
        if (usage <= 0) {
            continue
        }
        sheetCount += usage
        if (plan.material.kind == Material2Kind.Coil) {
            consumed += plan.pattern.region.height.value * FltX(usage.toDouble())
        }
    }
    val limit = config.maxConsumedLength
    val over = if (limit != null && consumed > limit) consumed - limit else FltX.zero
    return Ok(
        Csp2dSheetAssignmentResult(
            consumedLength = consumed,
            sheetCount = sheetCount,
            overLength = over,
            overLengthDetected = over > FltX.zero
        )
    )
}

/**
 * 判断物料属于哪种供给模式。 / Decide which supply mode a material belongs to.
 *
 * @param material 物料 / material
 * @return 是否为卷材 / whether it is a coil
*/
fun isCoilMode(material: Material2<FltX>): Boolean {
    return material.kind == Material2Kind.Coil
}
