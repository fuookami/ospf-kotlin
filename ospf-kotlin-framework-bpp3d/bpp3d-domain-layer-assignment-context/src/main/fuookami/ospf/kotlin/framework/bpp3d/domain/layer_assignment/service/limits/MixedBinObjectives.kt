/**
 * 箱型混合目标。 / Mixed bin type objectives.
 *
 * 为 bpp3d 补齐「按箱型差异化计价 + 体积消耗为主目标」的能力，与 bpp2d 的
 * `SheetAreaMinimization` / `SheetAmountMinimization` / `SheetTypeCostMinimization` 对应。
 * / Adds per-bin-type pricing and a volume-consumption primary objective to bpp3d, mirroring bpp2d's
 * `SheetAreaMinimization` / `SheetAmountMinimization` / `SheetTypeCostMinimization`.
 *
 * 主目标为什么是「箱型体积消耗最小」而不是「逐箱余料最小」：需求占用的体积由需求本身固定，因此
 * `总体积浪费 = Σ 箱型体积 − 需求占用体积 = Σ 箱型体积 − 常数`，最小化箱型体积消耗就是最小化体积浪费。
 * 若改为对 `箱型体积 − 已装层体积` 求和，铺满的整箱会记为 0 余料，反而让「一个铺满的大箱」优于「多个小箱」，
 * 与真实装载结果相反（bpp2d 实现时由真实求解用例暴露过该错误）。
 * / Why the primary objective is minimum bin-type volume consumption rather than per-bin waste: the volume occupied
 * by demand is fixed, so `total waste = Σ bin volume − demanded volume = Σ bin volume − constant`, hence
 * minimising consumed bin volume minimises waste. Summing `bin volume − loaded layer volume` instead would report
 * zero waste for a fully packed bin, making "one full large bin" look better than "several small bins", the
 * opposite of reality (an error exposed by a real solve case while implementing bpp2d).
 *
 * 多个 `minimize` 目标按权重线性相加：core 会把同类别（`Minimum`）子目标的系数逐项相加，因此权重必须显式给出。
 * / Multiple `minimize` objectives are combined as a weighted sum: the core adds the coefficients of sub-objectives
 * with the same category (`Minimum`) term by term, so weights must be explicit.
 *
 * 默认值与 bpp2d 对齐：体积消耗为主目标（权重 1），箱数系数降为 1e-6 仅用于在体积消耗相同的解之间打破平局，
 * 箱型成本权重保持 0 直到调用方配置 `finalBinTypeCosts`。
 * / Defaults are aligned with bpp2d: volume consumption is the primary objective (weight 1), the bin amount coefficient
 * drops to 1e-6 as a pure tie-breaker between equal-volume solutions, and the per-bin-type cost weight stays zero until
 * a caller configures `finalBinTypeCosts`.
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinTypeId
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.Bpp3dSolverValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.DefaultBpp3dSolverValueAdapter
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment

/**
 * 计算单箱的余料体积。 / Compute the waste volume of one bin.
 *
 * 余料体积 = 箱型体积 − 已装层体积之和，下限为 0。用于装箱后的统计与 KPI，不作为优化目标（原因见文件头注释）。
 * / Waste volume is the bin volume minus the sum of loaded layer volumes, floored at zero. It is used for
 * post-packing statistics and KPI, not as the optimisation objective (see the file header).
 *
 * @param type 箱型 / bin type
 * @param loadedLayerVolumes 已装层体积 / loaded layer volumes
 * @return 余料体积 / waste volume
*/
fun binWasteVolume(type: BinType<FltX>, loadedLayerVolumes: List<Quantity<FltX>>): FltX {
    var occupied = FltX.zero
    for (volume in loadedLayerVolumes) {
        occupied += volume.value
    }
    val container = type.volume.value
    return if (container > occupied) container - occupied else FltX.zero
}

/**
 * 计算单箱的余料率。 / Compute the waste ratio of one bin.
 *
 * 余料率 = 余料体积 / 箱型体积，落在 `[0, 1]`，便于跨箱型比较。
 * / The waste ratio is the waste volume over the bin volume, within `[0, 1]`, comparable across bin types.
 *
 * @param type 箱型 / bin type
 * @param loadedLayerVolumes 已装层体积 / loaded layer volumes
 * @return 余料率 / waste ratio
*/
fun binWasteRatio(type: BinType<FltX>, loadedLayerVolumes: List<Quantity<FltX>>): FltX {
    val container = type.volume.value
    if (container <= FltX.zero) {
        return FltX.zero
    }
    return binWasteVolume(type, loadedLayerVolumes) / container
}

/**
 * 箱型体积消耗最小化目标（主目标，等价于体积浪费最少）。 / Bin volume consumption minimisation (primary,
 * equivalent to minimum volume waste).
 *
 * `min weight · Σ_i volume_i · v_i`，其中 `v_i` 是箱子 i 是否被使用。因为需求体积固定，该目标等价于体积浪费最少。
 * / `min weight · Σ_i volume_i · v_i` where `v_i` marks whether bin i is used. Since the demanded volume is fixed,
 * this is equivalent to minimum volume waste.
 *
 * @property bins 候选箱子 / candidate bins
 * @property assignment 精确赋值 / precise assignment
 * @property weight 目标权重 / objective weight
 * @property solverValueAdapter 求解器数值转换器 / solver value adapter
 * @property name 目标名称 / objective name
*/
class BinVolumeMinimization(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val assignment: PreciseAssignment,
    private val weight: FltX = FltX.one,
    private val solverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dSolverValueAdapter,
    val name: String = "bin_volume_minimization"
) {

    /**
     * 注册目标。 / Register the objective.
     *
     * @param model 元模型 / meta model
     * @return 操作结果 / operation result
    */
    fun invoke(model: MetaModel<FltX>): Try {
        if (weight == FltX.zero) {
            return ok
        }
        val linearModel = model as AbstractLinearMetaModel<FltX>
        when (val result = linearModel.minimize(
            polynomial = sum(bins.mapIndexed { i, bin ->
                LinearMonomial(
                    weight * solverValueAdapter.volumeToSolver(bin.type.volume),
                    assignment.v[i]
                )
            }),
            name = "bin volume"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 箱型成本最小化目标（次目标）。 / Per-bin-type cost minimisation (secondary).
 *
 * `min weight · Σ_i typeCost(type_i) · v_i`，用于表达「同体积消耗下优先 40GP 而非 40HQ」这类偏好；
 * 未配置成本的箱型按 0 处理。与 bpp3d 原有单一 `finalBinAmountCoefficient` 的区别是这里**按箱型**计价。
 * / `min weight · Σ_i typeCost(type_i) · v_i`, expressing preferences such as "prefer 40GP over 40HQ at equal
 * volume consumption". Types without a configured cost count as zero. Unlike bpp3d's single
 * `finalBinAmountCoefficient`, the pricing here is **per bin type**.
 *
 * @property bins 候选箱子 / candidate bins
 * @property assignment 精确赋值 / precise assignment
 * @property typeCost 箱型到成本的映射 / bin type to cost mapping
 * @property weight 目标权重 / objective weight
 * @property name 目标名称 / objective name
*/
class BinTypeCostMinimization(
    private val bins: List<Bin<BinLayer, FltX>>,
    private val assignment: PreciseAssignment,
    private val typeCost: Map<BinTypeId, FltX>,
    private val weight: FltX = FltX.one,
    val name: String = "bin_type_cost_minimization"
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
        val monomials = bins.mapIndexedNotNull { i, bin ->
            val cost = typeCost[bin.type.typeCode] ?: return@mapIndexedNotNull null
            val coefficient = weight * cost
            if (coefficient == FltX.zero) {
                null
            } else {
                LinearMonomial(coefficient, assignment.v[i])
            }
        }
        if (monomials.isEmpty()) {
            return ok
        }
        when (val result = linearModel.minimize(
            polynomial = sum(monomials),
            name = "bin type cost"
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 构造按箱型计价的单箱成本函数。 / Build a per-bin cost function priced by type.
 *
 * @param typeCost 箱型到成本的映射 / bin type to cost mapping
 * @param defaultCost 未配置箱型的缺省成本 / default cost for unconfigured types
 * @return 以箱子为输入的成本函数 / cost function taking a bin
*/
fun perTypeBinCost(
    typeCost: Map<BinTypeId, FltX>,
    defaultCost: FltX = FltX.zero
): (BinType<FltX>) -> FltX {
    return { type -> typeCost[type.typeCode] ?: defaultCost }
}
