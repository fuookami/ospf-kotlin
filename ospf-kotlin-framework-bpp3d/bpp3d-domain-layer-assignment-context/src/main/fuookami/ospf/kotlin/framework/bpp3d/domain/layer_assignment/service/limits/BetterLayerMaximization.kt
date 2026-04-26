package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class BetterLayerMaximization(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    private val coefficient: (BinLayer, Bin<BinLayer>) -> Flt64,
    override val name: String = "better_layer_maximization"
) : Pipeline<AbstractLinearMetaModel<*>> {
    override fun invoke(model: AbstractLinearMetaModel<*>): Try {
        when (val result = model.maximize(
            polynomial = sum(bins.flatMapIndexed { i, bin ->
                layers.mapIndexed { j, layer ->
                    LinearMonomial(coefficient(layer, bin), assignment.x[i, j])
                }
            }),
            name = "better layer"
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



