package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*
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
 * 从零燃油重量点插值的最小低甲板载荷约束。Minimum low-deck payload constraint interpolated from zero-fuel weight points.
 *
 * @property aircraftModel 参数。
 * @property points 参数。
 * @property totalWeight 参数。
 */
class MinLowPayload(
    private val aircraftModel: AircraftModel,
    val points: List<Point>,
    private val totalWeight: TotalWeight
) {
    data class Point(
        val minLowPayload: Quantity<Flt64>,
        val zfw: Quantity<Flt64>
    )

    lateinit var minLowPayload: QuantityLinearIntermediateSymbol<Flt64>

    operator fun invoke(zfw: Quantity<Flt64>): Quantity<Flt64> {
        return Quantity(
            interpolate(zfw.to(aircraftModel.weightUnit)!!.value),
            aircraftModel.weightUnit
        )
    }

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::minLowPayload.isInitialized) {
            val zfw = totalWeight.computedTotalWeight[FlightPhase.ZeroFuel]
            minLowPayload = if (zfw != null) {
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        this(zfw).to(aircraftModel.weightUnit)!!.value,
                        name = "min_low_payload"
                    ),
                    aircraftModel.weightUnit
                )
            } else {
                val zfwEst = totalWeight.estimateTotalWeight[FlightPhase.ZeroFuel]!!.to(aircraftModel.weightUnit)!!.value
                val sorted = points
                    .map {
                        point2(
                            it.zfw.to(aircraftModel.weightUnit)!!.value,
                            it.minLowPayload.to(aircraftModel.weightUnit)!!.value
                        )
                    }
                    .sortedBy { it.x.toDouble() }
                val first = sorted.first()
                val last = sorted.last()
                val slope = (last.y - first.y) / (last.x - first.x)
                val intercept = first.y - slope * first.x
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        LinearPolynomial(
                            monomials = listOf(LinearMonomial(slope, zfwEst)),
                            constant = intercept
                        ),
                        name = "min_low_payload"
                    ),
                    aircraftModel.weightUnit
                )
            }
        }
        when (val result = model.add(minLowPayload)) {
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

    private fun interpolate(zfw: Flt64): Flt64 {
        val sorted = points
            .map {
                point2(
                    it.zfw.to(aircraftModel.weightUnit)!!.value,
                    it.minLowPayload.to(aircraftModel.weightUnit)!!.value
                )
            }
            .sortedBy { it.x.toDouble() }
        if (sorted.size == 1) {
            return sorted.first().y
        }
        if (zfw leq sorted.first().x) {
            return sorted.first().y
        }
        if (zfw geq sorted.last().x) {
            return sorted.last().y
        }
        for (i in 1 until sorted.size) {
            val prev = sorted[i - 1]
            val curr = sorted[i]
            if (zfw leq curr.x) {
                val ratio = (zfw - prev.x) / (curr.x - prev.x)
                return prev.y + (curr.y - prev.y) * ratio
            }
        }
        return sorted.last().y
    }
}
