package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

class Load(
    private val aircraftModel: AircraftModel,
    private val formula: Formula,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val withMultiLoadingSchema: Boolean,
    private val stowage: Stowage
) {
    lateinit var y: QuantityURealVariable1
    lateinit var z: QuantityUIntVariable1

    lateinit var predicateLoadWeightSlack: QuantityLinearIntermediateSymbols1
    lateinit var loadAmount: LinearIntermediateSymbols1
    lateinit var full: LinearIntermediateSymbols1

    lateinit var estimateLoadWeight: QuantityLinearIntermediateSymbols1
    lateinit var actualLoadWeight: QuantityLinearIntermediateSymbols1
    lateinit var estimateLoaded: LinearIntermediateSymbols1
    lateinit var actualLoaded: LinearIntermediateSymbols1

    lateinit var loadEstimateLongitudinalTorque: QuantityLinearIntermediateSymbols1
    lateinit var loadActualLongitudinalTorque: QuantityLinearIntermediateSymbols1
    lateinit var loadLateralTorque: QuantityLinearIntermediateSymbols1
    lateinit var loadCLIM: QuantityLinearIntermediateSymbols1
    lateinit var loadIndex: QuantityLinearIntermediateSymbols1

    fun loadAmountOf(position: Position, predicate: (Item) -> Boolean): LinearIntermediateSymbol<*> {
        val j = positions.indexOf(position)
        val poly = sum(items.mapIndexedNotNull { i, item ->
            if (predicate(item)) {
                stowage.stowage[i, j]
            } else {
                null
            }
        })
        return LinearExpressionSymbol(
            poly,
            name = "load_amount_of_${position}"
        )
    }

    fun register(
        model: AbstractLinearMetaModelF64
    ): Try {
        if (!::y.isInitialized) {
            y = QuantityURealVariable1("y", Shape1(positions.size), aircraftModel.weightUnit)
            for ((j, position) in positions.withIndex()) {
                y[j].value.name = "y_${position}"
                if (!position.status.predicateWeightNeeded) {
                    y[j].value.range.eq(Flt64.zero)
                } else {
                    y[j].value.range.leq(position.mlw.mlw.to(aircraftModel.weightUnit)!!.value)
                }
            }
        }
        for ((j, position) in positions.withIndex()) {
            if (position.status.predicateWeightNeeded) {
                when (val result = model.add(y[j])) {
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

        if (!::z.isInitialized) {
            z = QuantityUIntVariable1("z", Shape1(items.size), aircraftModel.weightUnit)
            for ((j, position) in positions.withIndex()) {
                z[j].value.name = "z_${position}"
                if (!position.status.recommendedWeightNeeded) {
                    z[j].value.range.eq(UInt64.zero)
                } else {
                    z[j].value.range.leq(position.mlw.mlw.to(aircraftModel.weightUnit)!!.value.floor().toUInt64())
                }
            }
        }
        for ((j, position) in positions.withIndex()) {
            if (position.status.recommendedWeightNeeded) {
                when (val result = model.add(z[j])) {
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

        if (!::predicateLoadWeightSlack.isInitialized) {
            predicateLoadWeightSlack = QuantityLinearIntermediateSymbols1("predicate_load_weight_slack", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                Quantity(
                    if (position.status.predicateWeightNeeded) {
                        // 需要预计重量的舱位，最大载量一定是 1，所以它的装载量一定是二元值（0 或 1）
                        assert(position.mla eq UInt64.one)
                        if (position.status.stowageNeeded || position.status.adjustmentNeeded) {
                            SlackRangeFunction(
                                UContinuous,
                                x = LinearPolynomial(y[j].to(aircraftModel.weightUnit)!!.value),
                                lb = LinearPolynomial(loadAmount[j]) * position.plw!!.min.to(aircraftModel.weightUnit)!!.value,
                                ub = LinearPolynomial(loadAmount[j]) * position.plw.max.to(aircraftModel.weightUnit)!!.value,
                                name = "predicate_load_weight_slack_${position}"
                            )
                        } else {
                            SlackRangeFunction(
                                UContinuous,
                                x = LinearPolynomial(y[j].to(aircraftModel.weightUnit)!!.value),
                                lb = LinearPolynomial(position.plw!!.min.to(aircraftModel.weightUnit)!!.value),
                                ub = LinearPolynomial(position.plw.max.to(aircraftModel.weightUnit)!!.value),
                                name = "predicate_load_weight_slack_${position}"
                            )
                        }
                    } else {
                        LinearExpressionSymbol(
                            Flt64.zero,
                            name = "predicate_load_weight_slack_${position}"
                        )
                    },
                    aircraftModel.weightUnit
                )
            }
        }
        when (val result = model.add(predicateLoadWeightSlack)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loadAmount.isInitialized) {
            loadAmount = LinearIntermediateSymbols1("load_amount", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                LinearExpressionSymbol(
                    sum(stowage.stowage[_a, j]),
                    name = "load_amount_${position}"
                )
            }
            for ((j, position) in positions.withIndex()) {
                loadAmount[j].range.leq(position.mla.toFlt64())
            }
        }
        when (val result = model.add(loadAmount)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::full.isInitialized) {
            full = LinearIntermediateSymbols1("full", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.status.available) {
                    if (position.mla eq UInt64.one) {
                        LinearExpressionSymbol(
                            loadAmount[j],
                            name = "full_${position}"
                        )
                    } else {
                        IfFunction(
                            inequality = loadAmount[j] eq position.mla,
                            name = "full_${position}"
                        )
                    }
                } else {
                    LinearExpressionSymbol(
                        Flt64.one,
                        name = "full_${position}"
                    )
                }
            }
        }
        when (val result = model.add(full)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (withMultiLoadingSchema) {
            if (!::estimateLoadWeight.isInitialized) {
                estimateLoadWeight = QuantityLinearIntermediateSymbols1("load_weight", Shape1(positions.size)) { j, _ ->
                    val position = positions[j]
                    val poly = MutableLinearPolynomial()
                    for ((i, item) in items.withIndex()) {
                        poly += item.weight.to(aircraftModel.weightUnit)!!.value * stowage.stowage[i, j]
                    }
                    if (position.status.predicateWeightNeeded) {
                        poly += LinearPolynomial(y[j].to(aircraftModel.weightUnit)!!.value)
                    }
                    if (position.status.recommendedWeightNeeded) {
                        poly += LinearPolynomial(z[j].to(aircraftModel.weightUnit)!!.value)
                    }
                    QuantityLinearIntermediateSymbol(
                        LinearExpressionSymbol(
                            poly,
                            name = "load_weight_${position}"
                        ),
                        aircraftModel.weightUnit
                    )
                }
            }
            when (val result = model.add(estimateLoadWeight)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::actualLoadWeight.isInitialized) {
            actualLoadWeight = QuantityLinearIntermediateSymbols1("actual_load_weight", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val poly = MutableLinearPolynomial()
                for ((i, item) in items.withIndex()) {
                    poly += item.weight.to(aircraftModel.weightUnit)!!.value * stowage.stowage[i, j]
                }
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        poly,
                        name = "actual_load_weight_${position}"
                    ),
                    aircraftModel.weightUnit
                )
            }
        }
        when (val result = model.add(actualLoadWeight)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::estimateLoaded.isInitialized) {
            estimateLoaded = LinearIntermediateSymbols1("estimate_loaded", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.status.available) {
                    val loadedItem: LinearIntermediateSymbol<*> = if (position.mla eq UInt64.one) {
                        LinearExpressionSymbol(
                            loadAmount[j],
                            name = "estimate_loaded_item_${position}"
                        )
                    } else {
                        BinaryzationFunction(
                            LinearPolynomial(loadAmount[j]),
                            name = "estimate_loaded_item_${position}"
                        )
                    }
                    val withPredicateLoadWeight = if (position.status.predicateWeightNeeded) {
                        IfFunction(
                            y[j].to(aircraftModel.weightUnit)!!.value geq 1,
                            name = "estimate_loaded_predicate_load_weight_${position}"
                        )
                    } else {
                        null
                    }
                    val withRecommendLoadWeight = if (position.status.recommendedWeightNeeded) {
                        BinaryzationFunction(
                            LinearPolynomial(z[j].to(aircraftModel.weightUnit)!!.value),
                            name = "estimate_loaded_recommended_load_weight_${position}"
                        )
                    } else {
                        null
                    }
                    val symbols: List<LinearIntermediateSymbol<*>> = listOfNotNull(
                        loadedItem,
                        withPredicateLoadWeight,
                        withRecommendLoadWeight
                    )
                    OrFunction(
                        polynomials = symbols,
                        name = "estimate_load_${position}"
                    )
                } else {
                    LinearExpressionSymbol(
                        Flt64.one,
                        name = "estimate_loaded_${position}"
                    )
                }
            }
        }
        when (val result = model.add(estimateLoaded)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::actualLoaded.isInitialized) {
            actualLoaded = LinearIntermediateSymbols1("actual_loaded", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.status.available) {
                    if (position.mla eq UInt64.one) {
                        LinearExpressionSymbol(
                            loadAmount[j],
                            name = "actual_loaded_${position}"
                        )
                    } else {
                        BinaryzationFunction(
                            LinearPolynomial(loadAmount[j]),
                            name = "actual_loaded_${position}"
                        )
                    }
                } else {
                    LinearExpressionSymbol(
                        Flt64.one,
                        name = "actual_loaded_${position}"
                    )
                }
            }
        }
        when (val result = model.add(actualLoaded)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loadEstimateLongitudinalTorque.isInitialized) {
            loadEstimateLongitudinalTorque = QuantityLinearIntermediateSymbols1("load_longitudinal_torque", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val torquePerWeight = aircraftModel.gravity(Quantity(Flt64.one, aircraftModel.weightUnit)) * position.coordinate.longitudinalArm
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        torquePerWeight.to(aircraftModel.torqueUnit)!!.value * estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value,
                        name = "load_longitudinal_torque_${position}"
                    ),
                    aircraftModel.torqueUnit
                )
            }
        }
        when (val result = model.add(loadEstimateLongitudinalTorque)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loadActualLongitudinalTorque.isInitialized) {
            loadActualLongitudinalTorque = QuantityLinearIntermediateSymbols1("load_actual_longitudinal_torque", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val torquePerWeight = aircraftModel.gravity(Quantity(Flt64.one, aircraftModel.weightUnit)) * position.coordinate.longitudinalArm
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        torquePerWeight.to(aircraftModel.torqueUnit)!!.value * actualLoadWeight[j].to(aircraftModel.weightUnit)!!.value,
                        name = "load_longitudinal_torque_${position}"
                    ),
                    aircraftModel.torqueUnit
                )
            }
        }
        when (val result = model.add(loadEstimateLongitudinalTorque)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        if (aircraftModel.wideBody) {
            if (!::loadLateralTorque.isInitialized) {
                loadLateralTorque = QuantityLinearIntermediateSymbols1("load_lateral_torque", Shape1(positions.size)) { j, _ ->
                    val position = positions[j]
                    val torquePerWeight = aircraftModel.gravity(Quantity(Flt64.one, aircraftModel.weightUnit)) * position.coordinate.lateralArm
                    QuantityLinearIntermediateSymbol(
                        LinearExpressionSymbol(
                            torquePerWeight.to(aircraftModel.torqueUnit)!!.value * estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value,
                            name = "load_lateral_torque_${position}"
                        ),
                        aircraftModel.torqueUnit
                    )
                }
            }
            when (val result = model.add(loadLateralTorque)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }

            if (!::loadCLIM.isInitialized) {
                loadCLIM = QuantityLinearIntermediateSymbols1("load_clim", Shape1(positions.size)) { j, _ ->
                    val position = positions[j]
                    QuantityLinearIntermediateSymbol(
                        LinearExpressionSymbol(
                            Flt64.zero,
                            name = "load_clim_${position}"
                        ),
                        aircraftModel.torqueUnit
                    )
                }
            }
            when (val result = model.add(loadCLIM)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::loadIndex.isInitialized) {
            loadIndex = QuantityLinearIntermediateSymbols1("load_index", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                val poly = MutableLinearPolynomial()
                for ((i, item) in items.withIndex()) {
                    val index = formula.index(item.weight, position.coordinate.longitudinalArm)
                    poly += index.to(aircraftModel.torqueUnit)!!.value * stowage.stowage[i, j]
                }
                if (position.status.predicateWeightNeeded || position.status.recommendedWeightNeeded) {
                    val indexPerWeight = formula.index(Quantity(Flt64.one, aircraftModel.weightUnit), position.coordinate.longitudinalArm).to(aircraftModel.torqueUnit)!!.value
                    if (position.status.predicateWeightNeeded) {
                        poly += indexPerWeight * y[j].to(aircraftModel.weightUnit)!!.value
                    }
                    if (position.status.recommendedWeightNeeded) {
                        poly += indexPerWeight * z[j].to(aircraftModel.weightUnit)!!.value
                    }
                }
                QuantityLinearIntermediateSymbol(
                    LinearExpressionSymbol(
                        poly,
                        name = "load_index_${position}"
                    ),
                    aircraftModel.torqueUnit
                )
            }
        }
        when (val result = model.add(loadIndex)) {
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














