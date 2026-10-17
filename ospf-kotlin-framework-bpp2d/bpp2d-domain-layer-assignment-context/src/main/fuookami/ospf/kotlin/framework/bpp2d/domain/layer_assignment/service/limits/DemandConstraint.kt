/**
 * 需求约束与影子价格提取。 / Demand constraint and shadow price extraction.
 *
 * 对应 BPP3D 的 `DemandConstraint`：为每个需求项注册上界与可选下界约束，并从中提取影子价格，供候选列的
 * 检验数计算使用。
 * / Mirrors BPP3D `DemandConstraint`: registers an upper bound and an optional lower bound per demand entry and
 * extracts shadow prices used by the reduced-cost computation of candidate columns.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.CGPipeline
import fuookami.ospf.kotlin.framework.model.ShadowPriceExtractor
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.*

/**
 * 影子价格转换器。 / Shadow price converter.
*/
private val shadowPriceConverter: IntoValue<FltX> = IntoValue.fromConverter(FltX)

/**
 * 构造以符号为唯一单项的线性多项式。 / Build a linear polynomial whose only monomial is the given symbol.
 *
 * @param symbol 线性符号 / linear symbol
 * @return 线性多项式 / linear polynomial
*/
private fun asLinearPolynomial(symbol: Symbol): LinearPolynomial<FltX> {
    return LinearPolynomial(
        monomials = listOf(LinearMonomial(FltX.one, symbol)),
        constant = FltX.zero
    )
}

/**
 * 构造常量线性多项式。 / Build a constant linear polynomial.
 *
 * @param value 常量 / constant value
 * @return 仅含常量的线性多项式 / constant-only linear polynomial
*/
private fun constantPolynomial(value: FltX): LinearPolynomial<FltX> {
    return LinearPolynomial(emptyList(), value)
}

/**
 * 需求模式短标签，用于约束命名。 / Short demand-mode tag used in constraint names.
 *
 * @return 模式标签 / mode tag
*/
private fun modeTag(mode: Bpp2dAssignmentDemandMode): String {
    return when (mode) {
        Bpp2dAssignmentDemandMode.ItemAmount -> "item_amount"
        Bpp2dAssignmentDemandMode.ItemArea -> "item_area"
    }
}

/**
 * 需求约束。 / Demand constraint.
 *
 * @property load 负载符号 / load symbols
 * @property demandEntries 需求项列表 / demand entry list
 * @property name 约束名称前缀 / constraint name prefix
*/
class DemandConstraint(
    private val load: Load,
    private val demandEntries: List<Bpp2dAssignmentDemandEntry> = load.demandEntries,
    override val name: String = "demand"
) : CGPipeline<Bpp2dShadowPriceArguments, AbstractLinearMetaModel<FltX>, Bpp2dShadowPriceMap> {

    /**
     * 按索引安全读取负载符号。 / Safely read the load symbol at an index.
     *
     * @param index 符号索引 / symbol index
     * @return 负载符号或失败 / load symbol, or failure
    */
    private fun symbolAt(index: Int): Ret<Symbol> {
        return runCatching { load.load[index] as Symbol }.getOrNull()
            ?.let { ok(it) }
            ?: Failed(ErrorCode.IllegalArgument, "Missing load symbol at index $index")
    }

    /**
     * 注册需求约束。 / Register the demand constraints.
     *
     * @param model 线性元模型 / linear meta model
     * @return 操作结果 / operation result
    */
    override fun invoke(model: AbstractLinearMetaModel<FltX>): Try {
        for ((i, demand) in demandEntries.withIndex()) {
            val priceKey = Bpp2dDemandShadowPriceKey(
                mode = demand.mode,
                itemId = demand.itemId
            )
            val tag = modeTag(demand.mode)
            val demandValue = demand.demand.toSolver()

            val loadSymbol = when (val result = symbolAt(i)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // 需求必须被覆盖：注册下界约束 load ≥ demand。装箱问题若只注册上界，最小化目标会退化为不装任何货。
            // The demand must be covered: register the lower bound load ≥ demand. Registering only an upper bound
            // would let the minimisation degenerate to shipping nothing.
            when (val result = model.addConstraint(
                relation = LinearInequality(
                    asLinearPolynomial(loadSymbol),
                    constantPolynomial(demandValue),
                    Comparison.GE
                ),
                name = "${name}_${tag}_lb_$i",
                args = priceKey
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // 可选业务上限：注册上界约束 load ≤ upperBound。
            // Optional business cap: register the upper bound load ≤ upperBound.
            val upperBound = demand.upperBound
            if (upperBound != null) {
                when (val result = model.addConstraint(
                    relation = LinearInequality(
                        asLinearPolynomial(loadSymbol),
                        constantPolynomial(upperBound.toSolver()),
                        Comparison.LE
                    ),
                    name = "${name}_${tag}_ub_$i",
                    args = priceKey
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    /**
     * 构造影子价格提取器。 / Build the shadow price extractor.
     *
     * @return 影子价格提取器，无需求项时返回 null / shadow price extractor, or null when there are no demands
    */
    override fun extractor(): ShadowPriceExtractor<Bpp2dShadowPriceArguments, Bpp2dShadowPriceMap>? {
        if (demandEntries.isEmpty()) {
            return null
        }
        return ShadowPriceExtractor { map, args ->
            var price = FltX.zero
            for (demand in demandEntries) {
                val key = Bpp2dDemandShadowPriceKey(
                    mode = demand.mode,
                    itemId = demand.itemId
                )
                val shadow = (map as AbstractShadowPriceMap<Bpp2dShadowPriceArguments, Bpp2dShadowPriceMap>)[key]
                    ?.price
                    ?: continue
                val coefficient = layerDemandCoefficient(args.layer, demand)
                if (shadow neq fuookami.ospf.kotlin.math.algebra.number.Flt64.zero) {
                    price += FltX(shadow.toDouble()) * coefficient
                }
            }
            shadowPriceConverter.fromValue(price)
        }
    }

}

