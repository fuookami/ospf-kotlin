package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/**
 * Experimental longitudinal balance model for analyzing main-deck torque distribution.
 * 用于分析主甲板扭矩分布的实验纵向平衡模型。
 *
 * @property aircraftModel 飞机模型引用 / The aircraft model reference
 * @property positions 装载位置列表 / The list of stowage positions
 * @property load 载荷分布数据 / The load distribution data
 * @property payload 载荷数据 / The payload data
 * @property redundancy 冗余模型引用 / The redundancy model reference
 * @property mainActualLongitudinalTorque 主甲板实际纵向扭矩符号 / The main deck actual longitudinal torque symbol
 * @property predicateLongitudinalTorque 预测纵向扭矩符号 / The predicate longitudinal torque symbol
 * @property longitudinalTorqueSlack 纵向扭矩松弛变量 / The longitudinal torque slack variable
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

    /**
     * Registers longitudinal balance intermediate symbols into the optimization model.
     * 将纵向平衡中间符号注册到优化模型中。
     *
     * @param model 要注册到的线性元模型 / The linear meta model to register into
     * @return 成功或失败结果 / Success or failure result
    */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (!::mainActualLongitudinalTorque.isInitialized) {
            var poly = LinearPolynomial()
            for ((j, position) in positions.withIndex()) {
                if (position.location.main) {
                    poly += load.loadActualLongitudinalTorque[j].to(aircraftModel.torqueUnit)!!.value
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
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::predicateLongitudinalTorque.isInitialized) {
            var poly = LinearPolynomial()
            for ((j, position) in positions.withIndex()) {
                if (position.location.main) {
                    poly += load.loadEstimateLongitudinalTorque[j].to(aircraftModel.torqueUnit)!!.value
                }
            }
            predicateLongitudinalTorque = Quantity(
                LinearExpressionSymbol(
                    poly,
                    name = "predicate_longitudinal_torque"
                ),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(predicateLongitudinalTorque)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        if (!::longitudinalTorqueSlack.isInitialized) {
            longitudinalTorqueSlack = Quantity(
                LinearFunctionSymbolAdapter(
                delegate = SlackFunction(
                    x = LinearPolynomial(mainActualLongitudinalTorque.value),
                    y = LinearPolynomial(predicateLongitudinalTorque.value),
                    type = UContinuous,
                    withNegative = true,
                    withPositive = true,
                    converter = flt64Converter,
                    name = "longitudinal_torque_slack"
                ),
                    converter = flt64Converter
                ),
                aircraftModel.torqueUnit
            )
        }
        when (val result = model.add(longitudinalTorqueSlack)) {
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
