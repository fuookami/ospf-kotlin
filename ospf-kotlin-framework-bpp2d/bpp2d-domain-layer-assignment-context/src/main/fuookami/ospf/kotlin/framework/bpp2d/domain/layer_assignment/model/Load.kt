/**
 * 需求负载模型。 / Demand load model.
 *
 * 对应 BPP3D 的 `Load`：把「候选层对需求的贡献」表达为线性符号，供需求约束与影子价格提取使用。
 * 二维侧支持两类需求：物品件数与物品面积。本文件位于求解器边界，使用具体的 `FltX`。
 * / Mirrors BPP3D `Load`: expresses the contribution of candidate layers to demands as linear symbols used by
 * the demand constraint and shadow price extraction. 2D supports item amount and item area demands. This file
 * sits on the solver boundary and uses concrete `FltX`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plusAssign
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.geometry2d.quantityZeroOf

/**
 * 层分配需求种类。 / Layer assignment demand kind.
*/
enum class Bpp2dAssignmentDemandMode {
    /** 物品件数需求 / item amount demand */
    ItemAmount,

    /** 物品面积需求 / item area demand */
    ItemArea
}

/**
 * 层分配需求值。 / Layer assignment demand value.
*/
sealed interface Bpp2dAssignmentDemandValue {

    /**
     * 件数需求。 / Amount demand.
     *
     * @property value 件数 / amount
    */
    data class Amount(val value: UInt64) : Bpp2dAssignmentDemandValue

    /**
     * 面积需求。 / Area demand.
     *
     * @property value 面积量 / area quantity
    */
    data class Area(val value: fuookami.ospf.kotlin.quantities.quantity.Quantity<FltX>) :
        Bpp2dAssignmentDemandValue
}

/**
 * 层分配需求项。 / Layer assignment demand entry.
 *
 * `demand` 是必须被**覆盖**的下界（装箱语义：需求必须装完），`upperBound` 为可选的额外上界（业务上限）。
 * 若把需求只当作上界，最小化目标会退化为「一块板材都不用」。
 * / `demand` is a lower bound that must be **covered** (packing semantics: the demand has to be shipped) and
 * `upperBound` is an optional extra upper bound (a business cap). Treating the demand as an upper bound only
 * would let the minimisation degenerate to "use no sheet at all".
 *
 * @property mode 需求种类 / demand kind
 * @property itemId 物品编号 / item identifier
 * @property demand 必须覆盖的需求下界 / demand lower bound to cover
 * @property upperBound 可选的需求上界 / optional demand upper bound
*/
data class Bpp2dAssignmentDemandEntry(
    val mode: Bpp2dAssignmentDemandMode,
    val itemId: ItemId,
    val demand: Bpp2dAssignmentDemandValue,
    val upperBound: Bpp2dAssignmentDemandValue? = null
)

/**
 * 计算需求值对应的求解器数值。 / Compute the solver number of a demand value.
 *
 * @return 求解器数值 / solver number
*/
fun Bpp2dAssignmentDemandValue.toSolver(): FltX {
    return when (this) {
        is Bpp2dAssignmentDemandValue.Amount -> bpp2dSolverAmount(value)
        is Bpp2dAssignmentDemandValue.Area -> value.value
    }
}

/**
 * 计算候选层对某个需求的贡献系数。 / Compute the contribution coefficient of a candidate layer for a demand.
 *
 * @param layer 候选层 / candidate layer
 * @param entry 需求项 / demand entry
 * @return 贡献系数 / contribution coefficient
*/
fun layerDemandCoefficient(
    layer: Layer<FltX>,
    entry: Bpp2dAssignmentDemandEntry
): FltX {
    val amount = layer.itemAmounts[entry.itemId] ?: return FltX.zero
    return when (entry.mode) {
        Bpp2dAssignmentDemandMode.ItemAmount -> bpp2dSolverAmount(amount)
        Bpp2dAssignmentDemandMode.ItemArea -> {
            val unitArea = layer.units
                .firstOrNull { it.itemId == entry.itemId.value }
                ?.shape
                ?.area
                ?.value
                ?: return FltX.zero
            unitArea * bpp2dSolverAmount(amount)
        }
    }
}

/**
 * 负载接口。 / Load interface.
 *
 * @property demandEntries 需求项列表 / demand entry list
 * @property load 负载符号 / load symbols
 * @property overLoad 超载符号 / over-load symbols
 * @property lessLoad 欠载符号 / less-load symbols
 * @property overEnabled 是否启用超载 / whether over-load is enabled
 * @property lessEnabled 是否启用欠载 / whether less-load is enabled
*/
interface Load {
    val demandEntries: List<Bpp2dAssignmentDemandEntry>
    val load: LinearExpressionSymbols1<FltX>
    val overLoad: LinearIntermediateSymbols1<FltX>
    val lessLoad: LinearIntermediateSymbols1<FltX>
    val overEnabled: Boolean
    val lessEnabled: Boolean
}

/**
 * 抽象负载基类，提供超载与欠载符号注册。 / Abstract load base class registering over-load and less-load symbols.
*/
abstract class AbstractLoad : Load {
    override lateinit var overLoad: LinearIntermediateSymbols1<FltX>
    override lateinit var lessLoad: LinearIntermediateSymbols1<FltX>

    /**
     * 向模型注册超载与欠载符号。 / Register over-load and less-load symbols into the model.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    open fun register(model: LinearMetaModel<FltX>): Try {
        if (overEnabled && !::overLoad.isInitialized) {
            overLoad = LinearIntermediateSymbols1<FltX>(
                name = "over_load",
                shape = Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = load[i].toLinearPolynomial(),
                    name = "over_load_$i"
                )
            }
        }
        if (lessEnabled && !::lessLoad.isInitialized) {
            lessLoad = LinearIntermediateSymbols1<FltX>(
                name = "less_load",
                shape = Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = load[i].toLinearPolynomial(),
                    name = "less_load_$i"
                )
            }
        }
        if (overEnabled) {
            when (val result = model.add(overLoad)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        if (lessEnabled) {
            when (val result = model.add(lessLoad)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }
}

/**
 * 不精确负载，用于 RMP 阶段：需求由列变量线性组合表达。 / Imprecise load for the RMP phase: demands are linear
 * combinations of the column variables.
 *
 * @property demandEntries 需求项列表 / demand entry list
 * @property assignment 不精确赋值 / imprecise assignment
 * @property overEnabled 是否启用超载 / whether over-load is enabled
 * @property lessEnabled 是否启用欠载 / whether less-load is enabled
*/
class ImpreciseLoad(
    override val demandEntries: List<Bpp2dAssignmentDemandEntry>,
    private val assignment: ImpreciseAssignment,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = true
) : AbstractLoad() {
    override lateinit var load: LinearExpressionSymbols1<FltX>

    /**
     * 注册负载符号。 / Register the load symbols.
     *
     * 采用可变的表达式符号组：符号只注册一次，后续加列时就地追加单项式。若每次加列都重新 `model.add`，
     * 模型会因同名符号重复注册而拒绝。
     * / A mutable expression symbol group is used: symbols are registered once and later columns are appended in
     * place. Re-adding through `model.add` on every column would be rejected as a duplicate symbol.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    fun registerLoad(model: LinearMetaModel<FltX>): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1<FltX>(
                name = "load",
                shape = Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(FltX.zero, name = "load_$i")
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        appendColumnTerms()
        return ok
    }

    /**
     * 为已注册的负载符号就地追加当前全部列项。 / Append every current column term to the registered load symbols
     * in place.
    */
    fun appendColumnTerms() {
        if (!::load.isInitialized) {
            return
        }
        for (i in demandEntries.indices) {
            val terms = safeLinearMonomials(
                assignment.layers.map { layer ->
                    LinearMonomial(
                        layerDemandCoefficient(layer, demandEntries[i]),
                        assignment.variableOf(layer)
                    )
                }
            )
            if (terms.isEmpty()) {
                continue
            }
            val thisLoad = load[i]
            thisLoad.flush()
            thisLoad.asMutable() += safeLinearPolynomial(terms)
        }
    }
}

/**
 * 精确负载，用于最终 MILP 阶段：需求由「板材 × 层」变量矩阵表达。 / Precise load for the final MILP phase:
 * demands are expressed over the sheet-by-layer variable matrix.
 *
 * @property demandEntries 需求项列表 / demand entry list
 * @property assignment 精确赋值 / precise assignment
 * @property overEnabled 是否启用超载 / whether over-load is enabled
 * @property lessEnabled 是否启用欠载 / whether less-load is enabled
*/
class PreciseLoad(
    override val demandEntries: List<Bpp2dAssignmentDemandEntry>,
    private val assignment: PreciseAssignment,
    override val overEnabled: Boolean = false,
    override val lessEnabled: Boolean = true
) : AbstractLoad() {
    override lateinit var load: LinearExpressionSymbols1<FltX>

    /**
     * 注册负载符号。 / Register the load symbols.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    fun registerLoad(model: LinearMetaModel<FltX>): Try {
        if (!::load.isInitialized) {
            load = LinearExpressionSymbols1<FltX>(
                name = "load",
                shape = Shape1(demandEntries.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = safeLinearPolynomial(
                        assignment.flatVariables().map { index ->
                            LinearMonomial(
                                layerDemandCoefficient(index.layer, demandEntries[i]),
                                assignment.y[index.layerIndex]
                            )
                        }
                    ),
                    name = "load_$i"
                )
            }
        }
        when (val result = model.add(load)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        return ok
    }
}

/**
 * 构造与给定层同单位的零面积。 / Build a zero area sharing the unit of the given layer.
 *
 * @param layer 参考层 / reference layer
 * @return 零面积量 / zero area quantity
*/
internal fun zeroAreaOf(layer: Layer<FltX>): FltX {
    return quantityZeroOf(layer.shape.width).value
}