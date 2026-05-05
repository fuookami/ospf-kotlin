package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class StowageLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    override val name: String = "stowage_limit"
): Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, item) in items.withIndex()) {
            for ((j, position) in positions.withIndex()) {
                if (item.status.available && position.status.available && !position.enabled(item).ok) {
                    when (val result = model.addConstraint(
                        stowage.stowage[i, j] eq false,
                        name = "${name}_${item}_${position}"
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
            }
        }

        return ok
    }
}

















