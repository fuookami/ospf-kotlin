/**
 * 目标函数族。 / Objective functions.
 *
 * 对应 BPP3D 的 `BinAmountMinimization` / `VolumeMinimization` 等目标，二维侧提供：
 * - 板材数量最小化（最终 MILP）；
 * - 层面积最小化（RMP，等价于用最少的板材面积满足需求）；
 * - 装载率最大化（最终 MILP 的补充目标）。
 * / Mirrors BPP3D `BinAmountMinimization` / `VolumeMinimization` and provides: sheet amount minimisation (final
 * MILP), layer area minimisation (RMP, i.e. satisfying demand with the least sheet area) and loading rate
 * maximisation (a supplementary final MILP objective).
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.safeLinearPolynomial
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.ImpreciseAssignment
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.PreciseAssignment

/**
 * 板材数量最小化目标。 / Sheet amount minimisation objective.
 *
 * @property bins 候选板材 / candidate sheets
 * @property assignment 精确赋值 / precise assignment
 * @property coefficient 单个板材的系数 / per-sheet coefficient
 * @property name 目标名称 / objective name
*/
class SheetAmountMinimization(
    private val layers: List<Layer<FltX>>,
    private val assignment: PreciseAssignment,
    private val coefficient: (Layer<FltX>) -> FltX,
    val name: String = "sheet_amount_minimization"
) {

    /**
     * 注册目标。 / Register the objective.
     *
     * 每列的份数即该列对应的板材数量，因此目标为「列份数 × 该列板材系数」之和。
     * / The copies of a column are the sheets it produces, so the objective sums copies times the per-sheet
     * coefficient of that column.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
    */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        when (val result = linearModel.minimize(
            polynomial = safeLinearPolynomial(
                layers.mapIndexed { i, layer ->
                    LinearMonomial(coefficient(layer), assignment.y[i])
                }
            ),
            name = "sheet amount"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 层面积最小化目标，用于 RMP。 / Layer area minimisation objective used by the RMP.
 *
 * @property assignment 不精确赋值 / imprecise assignment
 * @property coefficient 面积系数 / area coefficient
 * @property name 目标名称 / objective name
*/
class AreaMinimization(
    private val assignment: ImpreciseAssignment,
    private val coefficient: FltX = FltX.one,
    val name: String = "area_minimization"
) {

    /**
     * 注册目标。 / Register the objective.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
    */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        when (val result = linearModel.minimize(
            polynomial = LinearPolynomial(
                monomials = assignment.area.toLinearPolynomial().monomials.map {
                    LinearMonomial(it.coefficient * coefficient, it.symbol)
                },
                constant = assignment.area.toLinearPolynomial().constant * coefficient
            ),
            name = "area"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 板材容量约束：每块板材的装载面积不得超过板材面积。 / Sheet capacity constraint: the loaded area of each
 * sheet must not exceed the sheet area.
 *
 * @property bins 候选板材 / candidate sheets
 * @property capacity 容量符号 / capacity symbols
 * @property name 约束名称 / constraint name
*/
class SheetCapacityConstraint(
    private val capacity: fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.Capacity,
    val name: String = "sheet_capacity"
) {

    /**
     * 注册约束。 / Register the constraints.
     *
     * 每个候选层的装载面积不得超过其板材面积：`area_j · y_j ≤ sheetArea_j`。份数上界已在变量定义处收紧，
     * 这里补充面积口径的硬约束。
     * / Each candidate layer's loaded area must not exceed its sheet area: `area_j · y_j ≤ sheetArea_j`. The copy
     * bound is already tightened at variable definition; this adds the hard area-side constraint.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
    */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        val areaSigns = capacity.loadArea
        for (i in 0 until capacity.columnCount()) {
            val sheetArea = capacity.sheetAreaAt(i)
            if (sheetArea <= FltX.zero) {
                continue
            }
            when (val result = linearModel.addConstraint(
                relation = LinearInequality(
                    LinearPolynomial(
                        monomials = listOf(LinearMonomial(FltX.one, areaSigns[i])),
                        constant = FltX.zero
                    ),
                    LinearPolynomial(
                        monomials = emptyList(),
                        constant = sheetArea
                    ),
                    Comparison.LE
                ),
                name = "${name}_$i"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }
}