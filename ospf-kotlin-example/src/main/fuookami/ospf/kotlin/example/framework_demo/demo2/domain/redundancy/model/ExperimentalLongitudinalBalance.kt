package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

/**
 * 用于分析主甲板扭矩分布的实验纵向平衡模型。Experimental longitudinal balance model for analyzing main-deck torque distribution.
 *
 * @property aircraftModel 参数。
 * @property positions 参数。
 * @property load 参数。
 * @property payload 参数。
 * @property redundancy 参数。
 */
class ExperimentalLongitudinalBalance(
    private val aircraftModel: AircraftModel,
    private val positions: List<Position>,
    private val load: Load,
    private val payload: Payload,
    private val redundancy: Redundancy
) {
    lateinit var mainActualLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var predicateLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var longitudinalTorqueSlack: QuantityLinearIntermediateSymbol<Flt64>

    val minLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64> by lazy {
        TODO("not implemented yet")
    }

    val maxLongitudinalTorque: QuantityLinearIntermediateSymbol<Flt64> by lazy {
        TODO("not implemented yet")
    }

    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::mainActualLongitudinalTorque.isInitialized) {
            val poly = MutableLinearPolynomial()
            for ((j, position) in positions.withIndex()) {
                if (position.location.main) {
                    poly += LinearMonomial(
                        Flt64.one,
                        load.loadActualLongitudinalTorque[j].to(aircraftModel.torqueUnit)!!.value
                    )
                }
            }
            mainActualLongitudinalTorque = Quantity(
                LinearExpressionSymbol(
                    poly,
                    name = "main_actual_longitudinal_torque"
                ),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(mainActualLongitudinalTorque)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        if (!::predicateLongitudinalTorque.isInitialized) {
            TODO("not implemented yet")
        }
        when (val result = model.add(predicateLongitudinalTorque)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Failed(result.error)
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        if (!::longitudinalTorqueSlack.isInitialized) {
            longitudinalTorqueSlack = Quantity(
                LinearExpressionSymbol(Flt64, name = "longitudinal_torque_slack"),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(longitudinalTorqueSlack)) {
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
