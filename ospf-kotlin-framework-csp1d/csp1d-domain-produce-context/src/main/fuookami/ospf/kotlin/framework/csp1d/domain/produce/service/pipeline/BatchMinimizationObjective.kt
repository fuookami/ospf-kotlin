package fuookami.ospf.kotlin.framework.csp1d.domain.produce.service.pipeline

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.ProduceAggregation
import fuookami.ospf.kotlin.framework.model.Pipeline

/**
 * 批次最小化目标管线 / Batch minimization objective pipeline
 *
 * 添加基础目标函数：minimize sum(x_j)
 * 可选地通过 batchCoefficient 施加额外权重。
 *
 * Add base objective function: minimize sum(x_j)
 * Optionally apply extra weight via batchCoefficient.
 *
 * @property produce 产出聚合 / Produce aggregation
 * @property batchCoefficient 批次目标系数（默认 1.0）/ Batch objective coefficient (default 1.0)
*/
class BatchMinimizationObjective(
    private val produce: ProduceAggregation<*>,
    private val batchCoefficient: Flt64 = Flt64.one
) : Pipeline<LinearMetaModel<Flt64>> {

    override val name: String = "batch_minimization"

    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        val objective = LinearPolynomial(
            monomials = (0 until produce.planCount).map { index ->
                LinearMonomial(batchCoefficient, produce[index]!!)
            },
            constant = Flt64.zero
        )
        return when (val result = model.minimize(polynomial = objective, name = "batch_minimization")) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}
