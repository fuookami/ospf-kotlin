package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
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
 * Computes longitudinal torque, lateral torque, CLIM, and index for each flight phase.
 * 计算每个飞行阶段的纵向扭矩、横向扭矩、CLIM 和指数。
 *
 * @property longitudinalTorque 每个飞行阶段的纵向扭矩 / Longitudinal torque per flight phase
 * @property lateralTorque 宽体飞机的横向扭矩 / Lateral torque for wide-body aircraft
 * @property clim 宽体飞机的 CLIM 值 / CLIM value for wide-body aircraft
 * @property index 每个飞行阶段的指数 / Index per flight phase
*/
class Torque(
    private val aircraftModel: AircraftModel,
    private val fuselage: Fuselage,
    private val fuel: Map<FlightPhase, FuelConstant>,
    private val formula: Formula,
    private val positions: List<Position>,
    private val load: Load
) {
    lateinit var longitudinalTorque: Map<FlightPhase, QuantityLinearIntermediateSymbol<Flt64>>
    lateinit var lateralTorque: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var clim: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var index: Map<FlightPhase, QuantityLinearIntermediateSymbol<Flt64>>

    /**
     * Registers longitudinal torque, lateral torque, CLIM, and index symbols into the optimization model.
     * 将纵向扭矩、横向扭矩、CLIM 和指数符号注册到优化模型中。
     *
     * @param model 要注册符号的线性元模型 / The linear meta-model to register symbols into
     * @return 表示成功或失败 / [Try] indicating success or failure
    */
    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::longitudinalTorque.isInitialized) {
            longitudinalTorque = FlightPhase.entries.associateWith { phase ->
                var poly = LinearPolynomial()
                for ((j, _) in positions.withIndex()) {
                    poly += load.loadEstimateLongitudinalTorque[j].to(aircraftModel.torqueUnit)!!.value
                }
                when (phase) {
                    FlightPhase.TakeOff, FlightPhase.Landing -> {
                        poly += fuel[phase]!!.index.to(aircraftModel.torqueUnit)!!.value
                    }

                    FlightPhase.ZeroFuel -> {}
                }
                poly += (aircraftModel.gravity(fuselage.dow) * fuselage.balancedArm)!!
                    .to(aircraftModel.torqueUnit)!!.value
                poly += fuselage.liferaft?.let {
                    val arm = formula.arm(it.index, it.weight)
                    (it.weight * arm)!!.to(aircraftModel.torqueUnit)!!.value
                } ?: Flt64.zero
                Quantity(
                    LinearExpressionSymbol(
                        poly,
                        name = "longitudinal_torque_${phase.name.lowercase()}"
                    ),
                    aircraftModel.torqueUnit
                )
            }
        }
        longitudinalTorque.values.forEach {
            when (val result = model.add(it)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        run {
            if (!::lateralTorque.isInitialized) {
                var poly = LinearPolynomial()
                for ((j, _) in positions.withIndex()) {
                    poly += load.loadLateralTorque[j].to(aircraftModel.torqueUnit)!!.value
                }
                lateralTorque = Quantity(
                    LinearExpressionSymbol(
                        poly,
                        name = "lateral_torque"
                    ),
                    aircraftModel.torqueUnit
                )
            }
            when (val result = model.add(lateralTorque)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }

            if (aircraftModel.wideBody && !::clim.isInitialized) {
                var poly = LinearPolynomial()
                for ((j, _) in positions.withIndex()) {
                    poly += load.loadCLIM[j].to(aircraftModel.torqueUnit)!!.value
                }
                clim = Quantity(
                    LinearExpressionSymbol(
                        poly,
                        name = "clim"
                    ),
                    aircraftModel.torqueUnit
                )
            }
            if (aircraftModel.wideBody) {
                when (val result = model.add(clim)) {
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

        if (!::index.isInitialized) {
            index = FlightPhase.entries.associateWith { phase ->
                var poly = LinearPolynomial()
                for ((j, _) in positions.withIndex()) {
                    poly += load.loadIndex[j].to(aircraftModel.torqueUnit)!!.value
                }
                when (phase) {
                    FlightPhase.TakeOff, FlightPhase.Landing -> {
                        poly += fuel[phase]!!.index.to(aircraftModel.torqueUnit)!!.value
                    }

                    FlightPhase.ZeroFuel -> {}
                }
                poly += fuselage.doi.to(aircraftModel.torqueUnit)!!.value
                poly += fuselage.liferaft?.index?.let {
                    it.to(aircraftModel.torqueUnit)!!.value
                } ?: Flt64.zero
                Quantity(
                    LinearExpressionSymbol(
                        poly,
                        name = "index_${phase.name.lowercase()}"
                    ),
                    aircraftModel.torqueUnit
                )
            }
        }
        index.values.forEach {
            when (val result = model.add(it)) {
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
