package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class ItemReweighNeededLimit(
    private val items: List<Item>,
    private val stowage: Stowage,
    private val coefficient: (Item) -> Flt64 = { Flt64.one },
    override val name: String = "item_reweigh_needed_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.minimize(
            sum(items.mapIndexedNotNull { i, item ->
                if (item.order?.reweighed == null) {
                    coefficient(item) * stowage.loaded[i]
                } else {
                    null
                }
            }),
            "item reweigh needed"
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


















