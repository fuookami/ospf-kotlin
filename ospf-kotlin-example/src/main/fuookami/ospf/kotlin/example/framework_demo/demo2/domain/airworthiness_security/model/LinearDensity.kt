package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 计算每个货物位置的线性密度（单位长度重量）并将其注册到模型。Computes linear density (weight per unit length) for each cargo position and registers it with the model.
 *
 * @property private val aircraftModel 参数。
 * @property limitsZones 参数。
 * @property limitLines 参数。
 * @property private val positions 参数。
 * @property private val load 参数。
 */
class LinearDensity(
    private val aircraftModel: AircraftModel,
    val limitsZones: List<LimitZone>,
    val limitLines: List<LimitLine>,
    private val positions: List<Position>,
    private val load: Load
) {
    data class LimitZone(
        val name: String,
        val locations: Set<DeckLocation>,
        val frontArm: Quantity<Flt64>,
        val backArm: Quantity<Flt64>,
        val maxLinearDensity: Quantity<Flt64>
    )

    data class LimitLine(
        val zone: LimitZone,
        val arm: Quantity<Flt64>,
        val positions: List<Position>
    )

    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            limitZones: List<LimitZone>,
            positions: List<Position>,
            load: Load
        ): LinearDensity {
            TODO("not implemented yet")
        }
    }

    lateinit var linearDensity: QuantityLinearIntermediateSymbols1<Flt64>

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::linearDensity.isInitialized) {
            linearDensity = QuantityLinearIntermediateSymbols1<Flt64>("linear_density", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val coefficient = (Flt64.one / position.shape.length.to(aircraftModel.lengthUnit)!!.value)!!
                Quantity(
                    LinearExpressionSymbol(
                        LinearMonomial(coefficient, load.estimateLoadWeight[j].value),
                        name = "linear_density_${position}",
                    ),
                    aircraftModel.linearDensityUnit
                )
            }
        }
        when (val result = model.add(linearDensity)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return ok
    }
}
