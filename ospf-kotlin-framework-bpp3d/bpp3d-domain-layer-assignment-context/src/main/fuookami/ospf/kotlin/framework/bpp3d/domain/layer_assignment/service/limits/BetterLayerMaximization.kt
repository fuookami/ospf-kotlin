package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.service.limits

import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.sum
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.PreciseAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.LayerAssignmentScalar
import fuookami.ospf.kotlin.utils.functional.*

class BetterLayerMaximization(
    private val bins: List<Bin<BinLayer>>,
    private val layers: List<BinLayer>,
    private val assignment: PreciseAssignment,
    private val coefficient: (BinLayer, Bin<BinLayer>) -> LayerAssignmentScalar,
    val name: String = "better_layer_maximization"
) {
    fun invoke(model: MetaModel<LayerAssignmentScalar>): Try {
        val linearModel = model as AbstractLinearMetaModel<LayerAssignmentScalar>
        when (val result = linearModel.maximize(
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


