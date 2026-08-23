package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service.shadowPriceKeyOf

/**
 * 客户覆盖约束管线。 / Customer coverage constraint pipeline.
 *
 * 对每个客户 i 添加约束：customerCoverage[i] = 1
 * 其中 customerCoverage[i] = a[i] + sum(x[r] for r visiting i)，
 * a[i] 是人工变量（Phase I），addColumns 后自然包含路线变量。 / For each customer i, adds constraint: customerCoverage[i] = 1.
 * customerCoverage[i] = a[i] + sum(x[r] for r visiting i),
 * where a[i] is the artificial variable (Phase I); after addColumns, route variables are included.
 */
class CustomerCoverageConstraint(
    private val customers: List<Customer<*>>,
    private val compilation: RouteCompilation<*>,
    override val name: String = "customer_coverage"
) : CGPipeline<VrpShadowPriceArguments, AbstractLinearMetaModel<Flt64>, VrpShadowPriceMap> {

    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((index, customer) in customers.withIndex()) {
            when (val result = model.addConstraint(
                compilation.customerCoverage[index] eq 1,
                name = "${name}_${customer.id.value}",
                args = CustomerCoverageShadowPriceKey(customer.id)
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    override fun extractor(): ShadowPriceExtractor<VrpShadowPriceArguments, VrpShadowPriceMap>? {
        return object : ShadowPriceExtractor<VrpShadowPriceArguments, VrpShadowPriceMap> {
            override fun invoke(
                map: AbstractShadowPriceMap<VrpShadowPriceArguments, VrpShadowPriceMap>,
                args: VrpShadowPriceArguments
            ): Flt64 {
                return args.visitedCustomerIds.fold(Flt64.zero) { acc, customerId ->
                    acc + (map.map[CustomerCoverageShadowPriceKey(customerId)]?.price ?: Flt64.zero)
                }
            }
        }
    }

    override fun refresh(
        shadowPriceMap: VrpShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup()) {
            val key = shadowPriceKeyOf<CustomerCoverageShadowPriceKey>(constraint.args) ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(key, price))
            }
        }
        return ok
    }
}
