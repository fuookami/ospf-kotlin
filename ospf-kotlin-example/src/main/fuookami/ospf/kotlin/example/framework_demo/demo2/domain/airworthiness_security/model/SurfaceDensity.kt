package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 计算每个货物位置的表面密度（单位面积重量）并将其注册到模型。Computes surface density (weight per unit area) for each cargo position and registers it with the model.
 *
 * @property aircraftModel 参数。
 * @property limitsZones 参数。
 * @property positions 参数。
 * @property load 参数。
 */
class SurfaceDensity(
    private val aircraftModel: AircraftModel,
    val limitsZones: List<LimitZone>,
    private val positions: List<Position>,
    private val load: Load
) {
    data class LimitZone(
        val name: String,
        val locations: Set<DeckLocation>,
        val frontArm: Quantity<Flt64>,
        val backArm: Quantity<Flt64>,
        val maxSurfaceDensity: Quantity<Flt64>
    )

    lateinit var surfaceDensity: QuantityLinearIntermediateSymbols1<Flt64>

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::surfaceDensity.isInitialized) {
            surfaceDensity = QuantityLinearIntermediateSymbols1<Flt64>("surface_density", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val coefficient = (Flt64.one / position.shape.area.to(aircraftModel.areaUnit)!!.value)!!
                Quantity(
                    LinearExpressionSymbol(
                        LinearMonomial(coefficient, load.estimateLoadWeight[j].value),
                        name = "surface_density_${position}",
                    ),
                    aircraftModel.surfaceDensityUnit
                )
            }
        }
        for ((j, position) in positions.withIndex()) {
            if (limitsZones.any { position.coordinate.withIntersectionWith(it.frontArm, it.backArm) }) {
                when (val result = model.add(surfaceDensity[j])) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
                }
            }
        }

        return ok
    }
}
