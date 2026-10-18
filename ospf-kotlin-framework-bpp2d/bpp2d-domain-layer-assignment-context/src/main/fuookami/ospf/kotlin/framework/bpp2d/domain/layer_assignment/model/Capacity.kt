/**
 * 容量模型。 / Capacity model.
 *
 * 对应 BPP3D 的 `Capacity` / `PreciseLoadCapacity`：给出每块候选板材的装载面积与装载率符号，供板材容量
 * 约束与利用率目标使用。
 * / Mirrors BPP3D `Capacity` / `PreciseLoadCapacity`: provides per-candidate-sheet loaded area and loading rate
 * symbols for the sheet capacity constraint and the utilisation objective.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer

/**
 * 容量接口：每块板材的装载面积与装载率。 / Capacity interface: loaded area and loading rate per sheet.
*/
interface Capacity {

    /** 装载面积符号 / loaded area symbols */
    val loadArea: LinearIntermediateSymbols1<FltX>

    /** 装载率符号 / loading rate symbols */
    val loadingRate: LinearIntermediateSymbols1<FltX>

    /**
     * 读取第 i 列对应板材的面积。 / Read the sheet area of column i.
     *
     * @param index 列下标 / column index
     * @return 板材面积 / sheet area
    */
    fun sheetAreaAt(index: Int): FltX

    /**
     * 候选列数量。 / Number of candidate columns.
     *
     * @return 列数量 / column count
    */
    fun columnCount(): Int
}

/**
 * 精确装载容量，用于最终 MILP 阶段。 / Precise load capacity used by the final MILP phase.
 *
 * @property layers 候选层 / candidate layers
 * @property assignment 精确赋值 / precise assignment
*/
class PreciseLoadCapacity(
    private val layers: List<Layer<FltX>>,
    private val assignment: PreciseAssignment
) : Capacity {
    override lateinit var loadArea: LinearIntermediateSymbols1<FltX>
    override lateinit var loadingRate: LinearIntermediateSymbols1<FltX>

    /**
     * 注册容量符号。 / Register the capacity symbols.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    override fun columnCount(): Int = layers.size

    override fun sheetAreaAt(index: Int): FltX {
        return layers.getOrNull(index)?.sheet?.area?.value ?: FltX.zero
    }

    fun register(model: LinearMetaModel<FltX>): Try {
        if (!::loadArea.isInitialized) {
            loadArea = LinearIntermediateSymbols1<FltX>(
                name = "load_area",
                shape = Shape1(layers.size)
            ) { i, _ ->
                LinearExpressionSymbol(
                    polynomial = safeLinearPolynomial(
                        listOf(
                            LinearMonomial(layers[i].area.value, assignment.y[i])
                        )
                    ),
                    name = "load_area_$i"
                )
            }
        }
        when (val result = model.add(loadArea)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        if (!::loadingRate.isInitialized) {
            loadingRate = LinearIntermediateSymbols1<FltX>(
                name = "loading_rate",
                shape = Shape1(layers.size)
            ) { i, _ ->
                val denominator = layers[i].sheet?.area?.value ?: FltX.zero
                val coefficient = if (denominator <= FltX.zero) {
                    FltX.zero
                } else {
                    layers[i].area.value / denominator
                }
                LinearExpressionSymbol(
                    polynomial = safeLinearPolynomial(
                        listOf(LinearMonomial(coefficient, assignment.y[i]))
                    ),
                    name = "loading_rate_$i"
                )
            }
        }
        when (val result = model.add(loadingRate)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        return ok
    }
}