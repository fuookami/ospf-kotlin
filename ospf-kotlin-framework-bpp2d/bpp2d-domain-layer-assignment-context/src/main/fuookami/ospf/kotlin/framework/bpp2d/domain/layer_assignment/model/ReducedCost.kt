/**
 * 检验数定价。 / Reduced-cost pricing.
 *
 * 列生成的定价步骤：按当前 RMP 的覆盖约束对偶（影子价格）计算候选列的检验数，只把能改进目标的列加入模型。
 * / The pricing step of column generation: computes the reduced cost of a candidate column from the current RMP
 * coverage duals (shadow prices) and keeps only columns that can improve the objective.
 *
 * 模型为 `min Σ_j area_j · y_j  s.t.  Σ_j a_ij · y_j ≥ d_i`（`a_ij` 为列 j 对需求 i 的覆盖量，`d_i ≥ 0`，
 * 对偶 `π_i ≥ 0`），因此列 j 的检验数为
 * `rc_j = area_j · c_area − Σ_i π_i · a_ij`，`rc_j < 0` 表示该列可改进目标。
 * / For `min Σ_j area_j · y_j  s.t.  Σ_j a_ij · y_j ≥ d_i` (`a_ij` is the coverage of demand i by column j,
 * `d_i ≥ 0`, duals `π_i ≥ 0`), the reduced cost of column j is
 * `rc_j = area_j · c_area − Σ_i π_i · a_ij`; `rc_j < 0` means the column can improve the objective.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer

/**
 * 计算候选列的检验数。 / Compute the reduced cost of a candidate column.
 *
 * @param layer 候选列 / candidate column
 * @param demandEntries 需求项列表 / demand entry list
 * @param shadowPrices 需求影子价格，键为需求种类与物品编号 / demand shadow prices keyed by demand kind and item
 * @param areaCoefficient 面积目标系数，需与 RMP 目标一致 / area objective coefficient, matching the RMP objective
 * @return 检验数；负值表示可改进目标 / reduced cost; a negative value means the column improves the objective
*/
fun layerReducedCost(
    layer: Layer<FltX>,
    demandEntries: List<Bpp2dAssignmentDemandEntry>,
    shadowPrices: Map<Bpp2dDemandShadowPriceKey, FltX>,
    areaCoefficient: FltX = FltX.one
): FltX {
    var dualValue = FltX.zero
    for (entry in demandEntries) {
        val price = shadowPrices[Bpp2dDemandShadowPriceKey(entry.mode, entry.itemId)] ?: continue
        if (price == FltX.zero) {
            continue
        }
        dualValue += price * layerDemandCoefficient(layer, entry)
    }
    return layer.area.value * areaCoefficient - dualValue
}

/**
 * 判断候选列是否为改进列。 / Decide whether a candidate column is an improving column.
 *
 * @param layer 候选列 / candidate column
 * @param demandEntries 需求项列表 / demand entry list
 * @param shadowPrices 需求影子价格 / demand shadow prices
 * @param areaCoefficient 面积目标系数 / area objective coefficient
 * @param tolerance 容差，检验数小于 `-tolerance` 才算改进 / tolerance; the reduced cost must be below
 * `-tolerance` to count as improving
 * @return 是否为改进列 / whether the column is improving
*/
fun isImprovingColumn(
    layer: Layer<FltX>,
    demandEntries: List<Bpp2dAssignmentDemandEntry>,
    shadowPrices: Map<Bpp2dDemandShadowPriceKey, FltX>,
    areaCoefficient: FltX = FltX.one,
    tolerance: FltX = FltX(1e-6)
): Boolean {
    val reducedCost = layerReducedCost(
        layer = layer,
        demandEntries = demandEntries,
        shadowPrices = shadowPrices,
        areaCoefficient = areaCoefficient
    )
    return reducedCost < tolerance * FltX(-1)
}

/**
 * 列为改进列不依赖其覆盖面对偶的比较器，便于按检验数升序挑选候选。
 * / Comparator ordering columns by ascending reduced cost, so the most improving candidates are picked first.
 *
 * @param demandEntries 需求项列表 / demand entry list
 * @param shadowPrices 需求影子价格 / demand shadow prices
 * @param areaCoefficient 面积目标系数 / area objective coefficient
 * @return 按检验数升序的比较器 / comparator ordering by ascending reduced cost
*/
fun reducedCostComparator(
    demandEntries: List<Bpp2dAssignmentDemandEntry>,
    shadowPrices: Map<Bpp2dDemandShadowPriceKey, FltX>,
    areaCoefficient: FltX = FltX.one
): Comparator<Layer<FltX>> {
    return Comparator { lhs, rhs ->
        val lhsCost = layerReducedCost(lhs, demandEntries, shadowPrices, areaCoefficient)
        val rhsCost = layerReducedCost(rhs, demandEntries, shadowPrices, areaCoefficient)
        lhsCost.compareTo(rhsCost)
    }
}