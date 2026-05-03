package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

class Redundancy(
    private val aircraftModel: AircraftModel,
    private val flight: Flight,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    private val load: Load,
    private val payload: Payload
) {
    lateinit var redundancy: LinearIntermediateSymbolFlt64
    lateinit var predicateRedundancy: LinearIntermediateSymbolFlt64
    lateinit var redundancySlack: LinearIntermediateSymbolFlt64

    val minRedundancy: LinearPolynomial<Flt64> by lazy {
        TODO("not implemented yet")
    }

    val maxRedundancy: LinearPolynomial<Flt64> by lazy {
        TODO("not implemented yet")
    }

    fun register(
        model: AbstractLinearMetaModelFlt64
    ): Try {
        if (!::redundancy.isInitialized) {
            val poly = MutableLinearPolynomial()
            for ((i, item) in items.withIndex()) {
                if (!item.location.main) {
                    continue
                }

                when (item.status) {
                    ItemStatus.Reserved -> {
                        poly -= Flt64.one
                    }

                    ItemStatus.Optional -> {
                        poly -= LinearPolynomial(stowage.loaded[i])
                    }

                    else -> {}
                }
            }
            redundancy = LinearExpressionSymbol(
                poly,
                name = "redundancy"
            )
        }
        when (val result = model.add(redundancy)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

            is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::predicateRedundancy.isInitialized) {
            TODO("not implemented yet")
        }
        when (val result = model.add(predicateRedundancy)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

            is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::redundancySlack.isInitialized) {
            // TODO: add upper bound slack for maxRedundancy
            redundancySlack = LinearFunctionSymbolAdapter(
                delegate = SlackFunction(
                    x = LinearPolynomial(redundancy),
                    y = minRedundancy,
                    type = UContinuous,
                    withNegative = true,
                    withPositive = true,
                    converter = IntoValue.Flt64,
                    name = "redundancy_slack"
                ),
                converter = IntoValue.Flt64
            )
        }
        when (val result = model.add(redundancySlack)) {
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


