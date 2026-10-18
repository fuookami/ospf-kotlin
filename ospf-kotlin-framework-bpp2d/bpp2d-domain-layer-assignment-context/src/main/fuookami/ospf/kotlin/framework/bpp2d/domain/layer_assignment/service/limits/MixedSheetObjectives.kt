/**
 * 余料与板材类型目标。 / Waste and sheet type objectives.
 *
 * 二维排样以**余料面积最少**为主目标，板数与板材类型成本作为次目标。三者都是 `Σ 系数_j · y_j` 形式的线性项，
 * 通过显式权重相加成一个目标——core 的多目标机制会把同类目（`Minimum`）的系数逐项相加，因此权重必须由调用方
 * 自己给（bpp3d 的两个目标就是无权重相加）。
 * / 2D packing targets **minimum waste area** as the primary objective, with sheet count and per-type cost as
 * secondary terms. All three are linear terms of the form `Σ coefficient_j · y_j` combined through explicit
 * weights: the core multi-objective mechanism sums the coefficients of sub-objectives with the same category
 * (`Minimum`), so weights must be supplied by the caller (bpp3d simply adds its two objectives unweighted).
 *
 * 与 bpp3d 的差异：bpp3d 只有单一 `finalBinAmountCoefficient`，无法按箱型计价；这里按列所属板材类型定价。
 * / Difference from bpp3d: bpp3d has a single `finalBinAmountCoefficient` and cannot price by bin type; here the
 * cost is priced by the sheet type of each column.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.safeLinearPolynomial

/**
 * 计算单列的余料面积。 / Compute the waste area of one column.
 *
 * 余料 = 板材面积 − 该列实际占用面积；列未绑定板材类型时退化为 0，表示无余料可言。
 * / Waste is the sheet area minus the actually occupied area of the column. A column without a sheet type yields
 * zero, meaning no waste can be attributed.
 *
 * @param layer 候选列 / candidate column
 * @return 余料面积 / waste area
*/
fun layerWasteArea(layer: Layer<FltX>): FltX {
    val sheetArea = layer.sheet?.area?.value ?: return FltX.zero
    val occupied = layer.area.value
    return if (sheetArea > occupied) sheetArea - occupied else FltX.zero
}

/**
 * 计算单列的归一化余料率。 / Compute the normalised waste ratio of one column.
 *
 * 余料率 = 余料面积 / 板材面积，落在 `[0, 1]`，与板材尺寸无关，便于跨类型比较。
 * / The waste ratio is the waste area over the sheet area, within `[0, 1]`, independent of sheet size, which makes
 * it comparable across sheet types.
 *
 * @param layer 候选列 / candidate column
 * @return 余料率 / waste ratio
*/
fun layerWasteRatio(layer: Layer<FltX>): FltX {
    val sheetArea = layer.sheet?.area?.value ?: return FltX.zero
    if (sheetArea <= FltX.zero) {
        return FltX.zero
    }
    return layerWasteArea(layer) / sheetArea
}

/**
 * 板材面积消耗最小化目标（主目标，等价于余料最少）。 / Sheet area consumption minimisation (primary, equivalent to
 * minimum waste).
 *
 * `min weight · Σ_j sheetArea_j · y_j`。
 *
 * 为什么这等价于「余料最少」：需求占用的面积由需求本身固定，因此
 * `总余料 = Σ 板材面积 − 需求占用面积 = Σ 板材面积 − 常数`，最小化板材面积就是最小化余料。
 *
 * 不能直接对「`sheetArea_j − layerArea_j`」求和：列代表整板排布，铺满的整板余料记为 0，会让「一块铺满的大板」
 * 显得比「多块小板」更优，与真实裁剪后的余料相反。真实余料由 `layerWasteArea` 在裁剪后统计，用于报告与 KPI。
 *
 * 一列的份数等于它产出的板材数，因此最后一板（尾板）的消耗与剩余量都已包含在该目标内。
 * / `min weight · Σ_j sheetArea_j · y_j`.
 *
 * Why this equals minimum waste: the area occupied by demand is fixed by the demand itself, so
 * `total waste = Σ sheet area − demanded area = Σ sheet area − constant`, hence minimising sheet area minimises
 * waste.
 *
 * Summing `sheetArea_j − layerArea_j` instead would be wrong: a column is a whole-sheet layout and a fully packed
 * sheet would report zero waste, making "one full large sheet" look better than "several small sheets", the
 * opposite of the post-trim reality. The real waste is computed by `layerWasteArea` after trimming, and is used for
 * reporting and KPI.
 *
 * The copies of a column equal the sheets it produces, so the consumption and remainder of the last (tail) sheet are
 * already included in the objective.
 *
 * @property layers 候选列 / candidate columns
 * @property assignment 精确赋值 / precise assignment
 * @property weight 目标权重 / objective weight
 * @property name 目标名称 / objective name
*/
class SheetAreaMinimization(
    private val layers: List<Layer<FltX>>,
    private val assignment: PreciseAssignment,
    private val weight: FltX = FltX.one,
    val name: String = "sheet_area_minimization"
) {

    /**
     * 注册目标。 / Register the objective.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
    */
    fun invoke(model: MetaModel<FltX>): Try {
        val linearModel = model as AbstractLinearMetaModel<FltX>
        val monomials = layers.mapIndexedNotNull { index, layer ->
            val sheetArea = layer.sheet?.area?.value ?: return@mapIndexedNotNull null
            val coefficient = weight * sheetArea
            if (coefficient == FltX.zero) {
                null
            } else {
                LinearMonomial(coefficient, assignment.y[index])
            }
        }
        when (val result = linearModel.minimize(
            polynomial = safeLinearPolynomial(monomials),
            name = "sheet area"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 板材类型成本目标（次目标）。 / Per-sheet-type cost objective (secondary).
 *
 * `min weight · Σ_j typeCost(sheet_j) · y_j`。用于表达「同余料水平下优先 40GP 而非 40HQ」这类偏好；
 * 未配置成本的类型按 0 处理。
 * / `min weight · Σ_j typeCost(sheet_j) · y_j`, expressing preferences such as "prefer 40GP over 40HQ when waste
 * is equal". Types without a configured cost count as zero.
 *
 * @property layers 候选列 / candidate columns
 * @property assignment 精确赋值 / precise assignment
 * @property typeCost 板材类型到成本的映射 / sheet type to cost mapping
 * @property weight 目标权重 / objective weight
 * @property name 目标名称 / objective name
*/
class SheetTypeCostMinimization(
    private val layers: List<Layer<FltX>>,
    private val assignment: PreciseAssignment,
    private val typeCost: Map<SheetTypeId, FltX>,
    private val weight: FltX = FltX.one,
    val name: String = "sheet_type_cost_minimization"
) {

    /**
     * 注册目标。 / Register the objective.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
    */
    fun invoke(model: MetaModel<FltX>): Try {
        if (typeCost.isEmpty() || weight == FltX.zero) {
            return ok
        }
        val linearModel = model as AbstractLinearMetaModel<FltX>
        val monomials = layers.mapIndexedNotNull { index, layer ->
            val typeCode = layer.sheet?.typeCode ?: return@mapIndexedNotNull null
            val cost = typeCost[typeCode] ?: return@mapIndexedNotNull null
            val coefficient = weight * cost
            if (coefficient == FltX.zero) {
                null
            } else {
                LinearMonomial(coefficient, assignment.y[index])
            }
        }
        when (val result = linearModel.minimize(
            polynomial = safeLinearPolynomial(monomials),
            name = "sheet type cost"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 构造按类型计价的单板成本函数。 / Build a per-sheet cost function priced by type.
 *
 * @param typeCost 板材类型到成本的映射 / sheet type to cost mapping
 * @param defaultCost 未配置类型的缺省成本 / default cost for unconfigured types
 * @return 以列为输入的成本函数 / cost function taking a column
*/
fun perTypeCost(
    typeCost: Map<SheetTypeId, FltX>,
    defaultCost: FltX = FltX.zero
): (Layer<FltX>) -> FltX {
    return { layer ->
        layer.sheet?.let { typeCost[it.typeCode] } ?: defaultCost
    }
}

/**
 * 构造按类型的板材数量系数函数。 / Build a per-type sheet amount coefficient function.
 *
 * @param typeCoefficient 板材类型到系数的映射 / sheet type to coefficient mapping
 * @param defaultCoefficient 未配置类型的缺省系数 / default coefficient for unconfigured types
 * @return 以列为输入的系数函数 / coefficient function taking a column
*/
fun perTypeAmountCoefficient(
    typeCoefficient: Map<SheetTypeId, FltX>,
    defaultCoefficient: FltX = FltX.one
): (Layer<FltX>) -> FltX {
    return { layer ->
        layer.sheet?.let { typeCoefficient[it.typeCode] } ?: defaultCoefficient
    }
}

/**
 * 判断板材类型是否等价。 / Check whether two sheet types are equivalent.
 *
 * @param lhs 左板材类型 / left sheet type
 * @param rhs 右板材类型 / right sheet type
 * @return 是否等价 / whether equivalent
*/
internal fun sameSheetType(lhs: SheetType<FltX>?, rhs: SheetType<FltX>?): Boolean {
    return lhs?.typeCode == rhs?.typeCode
}

/**
 * 空多项式常量，便于调用方构造零目标。 / Empty polynomial constant helping callers build a zero objective.
 *
 * @return 零线性多项式 / zero linear polynomial
*/
internal fun zeroLinearPolynomial(): LinearPolynomial<FltX> {
    return LinearPolynomial(emptyList(), FltX.zero)
}