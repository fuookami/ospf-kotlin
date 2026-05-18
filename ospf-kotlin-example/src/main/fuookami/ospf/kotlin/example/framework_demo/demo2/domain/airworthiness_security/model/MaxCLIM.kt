package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

class MaxCLIM(
    private val aircraftModel: AircraftModel,
    val points: List<Point>,
    private val totalWeight: TotalWeight
) {
    data class Point(
        val tow: Quantity<Flt64>,
        val maxCLIM: Quantity<Flt64>
    )

    operator fun invoke(tow: Quantity<Flt64>): Quantity<Flt64> {
        val x = tow.to(aircraftModel.weightUnit)!!.value
        val sorted = points.sortedBy { it.tow.to(aircraftModel.weightUnit)!!.value.toDouble() }
        val x0 = sorted.first().tow.to(aircraftModel.weightUnit)!!.value
        val y0 = sorted.first().maxCLIM.to(aircraftModel.torqueUnit)!!.value
        if (x ls x0) {
            return Quantity(y0, aircraftModel.torqueUnit)
        }
        for (i in 0 until sorted.size - 1) {
            val x1 = sorted[i].tow.to(aircraftModel.weightUnit)!!.value
            val y1 = sorted[i].maxCLIM.to(aircraftModel.torqueUnit)!!.value
            val x2 = sorted[i + 1].tow.to(aircraftModel.weightUnit)!!.value
            val y2 = sorted[i + 1].maxCLIM.to(aircraftModel.torqueUnit)!!.value
            if (x leq x2) {
                val slope = (y2 - y1) / (x2 - x1)
                return Quantity(y1 + (x - x1) * slope, aircraftModel.torqueUnit)
            }
        }
        val yn = sorted.last().maxCLIM.to(aircraftModel.torqueUnit)!!.value
        return Quantity(yn, aircraftModel.torqueUnit)
    }

    lateinit var maxCLIM: QuantityLinearIntermediateSymbol

    fun register(
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::maxCLIM.isInitialized) {
            val tow = totalWeight.computedTotalWeight[FlightPhase.TakeOff]
            maxCLIM = if (tow != null) {
                Quantity(
                    LinearExpressionSymbol(
                        this(tow).to(aircraftModel.torqueUnit)!!.value,
                        name = "max_clim"
                    ),
                    aircraftModel.torqueUnit
                )
            } else {
                val xSymbol = totalWeight.estimateTotalWeight[FlightPhase.TakeOff]!!.value
                val sorted = points.sortedBy { it.tow.to(aircraftModel.weightUnit)!!.value.toDouble() }
                val x1 = sorted.first().tow.to(aircraftModel.weightUnit)!!.value
                val y1 = sorted.first().maxCLIM.to(aircraftModel.torqueUnit)!!.value
                val x2 = sorted.last().tow.to(aircraftModel.weightUnit)!!.value
                val y2 = sorted.last().maxCLIM.to(aircraftModel.torqueUnit)!!.value
                val slope = (y2 - y1) / (x2 - x1)
                val intercept = y1 - slope * x1
                val poly = MutableLinearPolynomial()
                poly += LinearMonomial(slope, xSymbol)
                poly += intercept
                Quantity(
                    LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant), name = "max_clim"),
                    aircraftModel.torqueUnit
                )
            }
        }
        when (val result = model.add(maxCLIM)) {
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
