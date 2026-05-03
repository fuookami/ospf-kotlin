package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.MAC
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

typealias MACDecision = fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.model.MAC

class HorizontalStabilizer(
    private val aircraftModel: AircraftModel,
    val key: Key,
    val points: List<Point>,
    val limit: Limit,
    private val totalWeight: TotalWeight,
    private val mac: MACDecision
) {
    data class Key(
        val angle: HorizontalStabilizerAngle,
        val thrustDrate: HorizontalStabilizerThrustDrate?
    ) {
        override fun toString(): String {
            return angle.toString()
        }
    }

    data class Point(
        val tow: Quantity<Flt64>,
        val mac: MAC,
        val trim: Flt64
    )

    data class Limit(
        val minTrim: Flt64?,
        val maxTrim: Flt64?,
        val warnMinTrim: Flt64?,
        val warnMaxTrim: Flt64?
    )

    lateinit var trim: LinearIntermediateSymbolFlt64
    lateinit var warnSlack: LinearIntermediateSymbolFlt64

    operator fun invoke(tow: Quantity<Flt64>, mac: MAC): Flt64 {
        return points
            .minByOrNull { (it.tow.to(aircraftModel.weightUnit)!!.value - tow.to(aircraftModel.weightUnit)!!.value).abs() }
            ?.trim ?: Flt64.zero
    }

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModelFlt64
    ): Try {
        if (!::trim.isInitialized) {
            trim = LinearExpressionSymbol(
                Flt64.zero,
                name = "${key}_trim"
            )
        }
        when (val result = model.add(trim)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (stowageMode.withMacOptimization) {
            if (!::warnSlack.isInitialized) {
                warnSlack = if (limit.warnMinTrim != null && limit.warnMaxTrim != null) {
                    // TODO: add upper bound slack for limit.warnMaxTrim
                    LinearFunctionSymbolAdapter(
                        delegate = SlackFunction(
                            x = LinearPolynomial(trim),
                            y = LinearPolynomial(limit.warnMinTrim),
                            type = UContinuous,
                            withNegative = true,
                            withPositive = true,
                            converter = IntoValue.Flt64,
                            name = "${key}_trim_warn_slack"
                        ),
                        converter = IntoValue.Flt64
                    )
                } else if (limit.warnMinTrim != null) {
                    SlackFunction(
                        x = trim,
                        threshold = limit.warnMinTrim,
                        withPositive = false,
                        name = "${key}_trim_warn_slack"
                    )
                } else if (limit.warnMaxTrim != null) {
                    SlackFunction(
                        x = trim,
                        threshold = limit.warnMaxTrim,
                        withPositive = true,
                        name = "${key}_trim_warn_slack"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.zero,
                        name = "${key}_trim_warn_slack"
                    )
                }
            }
            when (val result = model.add(warnSlack)) {
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

    private fun pointsOf(tow: Quantity<Flt64>): List<Pair<MAC, Flt64>> {
        val eps = Equal<Flt64, Flt64>(Flt64(1e-5))
        val towV = tow.to(aircraftModel.weightUnit)!!.value
        val sameTowPoints = points.filter { eps(it.tow.to(aircraftModel.weightUnit)!!.value, towV) }
        val source = if (sameTowPoints.isNotEmpty()) sameTowPoints else points
        return source.map { it.mac to it.trim }.sortedBy { it.first.mac }
    }
}














