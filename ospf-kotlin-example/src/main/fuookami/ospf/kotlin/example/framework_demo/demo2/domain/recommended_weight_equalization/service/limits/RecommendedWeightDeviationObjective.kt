package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

/**
 * 将推荐重量偏差作为最大化目标中的负项。Adds recommended-weight deviation as a negative maximization objective term.
 *
 * @property load 含逐舱偏差变量的载荷模型 / The load model containing per-position deviation variables
 * @property coefficient 偏差惩罚系数 / The deviation penalty coefficient
 */
class RecommendedWeightDeviationObjective(
    private val load: Load,
    private val coefficient: () -> Flt64,
    override val name: String = "recommended_weight_deviation_objective"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        var poly = LinearPolynomial()
        for (deviation in load.z) {
            poly += -coefficient() * deviation.value
        }
        when (val result = model.maximize(
            poly,
            name = name
        )) {
            is Ok<*, ErrorCode, Error<ErrorCode>> -> {}
            is Failed<*, ErrorCode, Error<ErrorCode>> -> return Failed(result.error)
            is Fatal<*, ErrorCode, Error<ErrorCode>> -> return Fatal(result.errors)
        }

        return ok
    }
}
