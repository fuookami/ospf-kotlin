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

class ItemAheadLoadLimit(
    private val items: List<Item>,
    private val stowage: Stowage,
    private val coefficient: (Item) -> Flt64 = { Flt64.one },
    override val name: String = "item_ahead_load_limit"
) : Pipeline<AbstractLinearMetaModelF64> {
    companion object {
        private val predicates = listOf(
            // 已到机下
            { item: Item -> item.order?.hardstand != null },
            // 已出库
            { item: Item -> item.order?.hardstand != null || item.order?.carBoard != null },
            // 已复磅
            { item: Item -> item.order?.hardstand != null || item.order?.carBoard != null || item.order?.reweighed != null },
        )
    }

    override fun invoke(model: AbstractLinearMetaModelF64): Try {
        val predicate = predicates.find { pred -> items.any(pred) }
        if (predicate != null) {
            when (val result = model.minimize(
                sum(items.mapIndexed { i, item ->
                    coefficient(item) * stowage.loaded[i]
                }),
                "item ahead load"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}


















