package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * Minimizes the advice load weight slack across positions with advisory weight limits.
 * 最小化具有建议重量限制的位置的建议装载重量松弛。
 *
 * @property positions 配载位置列表 / The list of stowage positions.
 * @property loading 提供重量松弛符号的建议装载模型 / The advice loading model providing weight slack symbols.
 * @property coefficient 计算每个位置惩罚系数的函数 / Function computing the penalty coefficient for each position.
*/
class AdviceLoadWeightLimit(
    private val positions: List<Position>,
    private val loading: AdviceLoading,
    private val coefficient: (Position) -> Flt64 = { Flt64.one },
    override val name: String = "advice_load_weight_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val monomials = positions.mapIndexedNotNull { j, position ->
            if (position.alw != null) {
                coefficient(position) * loading.weightSlack[j]
            } else {
                null
            }
        }
        if (monomials.isEmpty()) {
            return ok
        }

        when (val result = model.minimize(
            sum(monomials),
            name = "advice load weight"
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}

