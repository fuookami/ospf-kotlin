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
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.core.intermediate_symbol.function.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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

    lateinit var predicateLoadWeightSlack: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var loadAmount: LinearIntermediateSymbols1<Flt64>
    lateinit var full: LinearIntermediateSymbols1<Flt64>

    lateinit var estimateLoadWeight: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var actualLoadWeight: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var estimateLoaded: LinearIntermediateSymbols1<Flt64>
    lateinit var actualLoaded: LinearIntermediateSymbols1<Flt64>

    lateinit var loadEstimateLongitudinalTorque: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var loadActualLongitudinalTorque: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var loadLateralTorque: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var loadCLIM: QuantityLinearIntermediateSymbols1<Flt64>
    lateinit var loadIndex: QuantityLinearIntermediateSymbols1<Flt64>

    fun loadAmountOf(position: Position, predicate: (Item) -> Boolean): LinearIntermediateSymbol<Flt64> {
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
        model: AbstractLinearMetaModel<Flt64>
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

        if (!::predicateLoadWeightSlack.isInitialized) {
            predicateLoadWeightSlack = QuantityLinearIntermediateSymbols1<Flt64>("predicate_load_weight_slack", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                Quantity(
                    if (position.status.predicateWeightNeeded) {
                        // ��ҪԤ�������Ĳ�λ���������һ���� 1����������װ����һ���Ƕ�Ԫֵ��0 �� 1��
                        assert(position.mla eq UInt64.one)
                        if (position.status.stowageNeeded || position.status.adjustmentNeeded) {
                            // TODO: upper bound constraint (x <= ub) not yet enforced with new SlackFunction API
                            LinearFunctionSymbolAdapter(
                                delegate = SlackFunction(
                                    x = LinearPolynomial(y[j].to(aircraftModel.weightUnit)!!.value),
                                    y = LinearPolynomial(loadAmount[j]) * position.plw!!.min.to(aircraftModel.weightUnit)!!.value,
                                    type = UContinuous,
                                    withNegative = true,
                                    withPositive = true,
                                    converter = flt64Converter,
                                    name = "predicate_load_weight_slack_${position}"
                                ),
                                converter = flt64Converter
                            )
                        } else {
                            // TODO: upper bound constraint (x <= ub) not yet enforced with new SlackFunction API
                            LinearFunctionSymbolAdapter(
                                delegate = SlackFunction(
                                    x = LinearPolynomial(y[j].to(aircraftModel.weightUnit)!!.value),
                                    y = LinearPolynomial(position.plw!!.min.to(aircraftModel.weightUnit)!!.value),
                                    type = UContinuous,
                                    withNegative = true,
                                    withPositive = true,
                                    converter = flt64Converter,
                                    name = "predicate_load_weight_slack_${position}"
                                ),
                                converter = flt64Converter
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loadAmount.isInitialized) {
            loadAmount = LinearIntermediateSymbols1<Flt64>("load_amount", Shape1(positions.size)) { j, _ ->
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::full.isInitialized) {
            full = LinearIntermediateSymbols1<Flt64>("full", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.status.available) {
                    if (position.mla eq UInt64.one) {
                        LinearExpressionSymbol(
                            loadAmount[j],
                            name = "full_${position}"
                        )
                    } else {
                        LinearFunctionSymbolAdapter(
                            delegate = SameAsFunction(
                                inequalities = listOf(loadAmount[j].toLinearPolynomial() eq position.mla.toFlt64()),
                                constraint = true,
                                epsilon = Flt64(1e-6),
                                m = Flt64(1e6),
                                converter = flt64Converter,
                                name = "full_${position}"
                            ),
                            converter = flt64Converter
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (withMultiLoadingSchema) {
            if (!::estimateLoadWeight.isInitialized) {
                estimateLoadWeight = QuantityLinearIntermediateSymbols1<Flt64>("load_weight", Shape1(positions.size)) { j, _ ->
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
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::actualLoadWeight.isInitialized) {
            actualLoadWeight = QuantityLinearIntermediateSymbols1<Flt64>("actual_load_weight", Shape1(positions.size)) { j, _ ->
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::estimateLoaded.isInitialized) {
            estimateLoaded = LinearIntermediateSymbols1<Flt64>("estimate_loaded", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.status.available) {
                    val loadedItem: LinearIntermediateSymbol<Flt64> = if (position.mla eq UInt64.one) {
                        LinearExpressionSymbol(
                            loadAmount[j],
                            name = "estimate_loaded_item_${position}"
                        )
                    } else {
                        LinearFunctionSymbolAdapter(
                            delegate = BinaryzationFunction(
                                LinearPolynomial(loadAmount[j]),
                                converter = flt64Converter,
                                name = "estimate_loaded_item_${position}"
                            ),
                            converter = flt64Converter
                        )
                    }
                    val withPredicateLoadWeight = if (position.status.predicateWeightNeeded) {
                        LinearFunctionSymbolAdapter(
                            delegate = IfFunction(
                                condition = LinearPolynomial(y[j].to(aircraftModel.weightUnit)!!.value) - LinearPolynomial(Flt64.one),
                                converter = flt64Converter,
                                name = "estimate_loaded_predicate_load_weight_${position}"
                            ),
                            converter = flt64Converter
                        )
                    } else {
                        null
                    }
                    val withRecommendLoadWeight = if (position.status.recommendedWeightNeeded) {
                        LinearFunctionSymbolAdapter(
                            delegate = BinaryzationFunction(
                                LinearPolynomial(z[j].to(aircraftModel.weightUnit)!!.value),
                                converter = flt64Converter,
                                name = "estimate_loaded_recommended_load_weight_${position}"
                            ),
                            converter = flt64Converter
                        )
                    } else {
                        null
                    }
                    val polys: List<LinearPolynomial<Flt64>> = listOfNotNull(loadedItem, withPredicateLoadWeight, withRecommendLoadWeight).map { sym ->
                        if (sym is LinearFunctionSymbolAdapter<*>) {
                            val adapter = sym as LinearFunctionSymbolAdapter<Flt64>
                            when (val d = adapter.delegate) {
                                is IfFunction<Flt64> -> d.result
                                is BinaryzationFunction<Flt64> -> LinearPolynomial(listOf(LinearMonomial(Flt64.one, d.resultVar)), Flt64.zero)
                                is SameAsFunction<Flt64> -> LinearPolynomial(listOf(LinearMonomial(Flt64.one, d.resultVar)), Flt64.zero)
                                else -> sym.toLinearPolynomial()
                            }
                        } else {
                            sym.toLinearPolynomial()
                        }
                    }
                    LinearFunctionSymbolAdapter(
                        delegate = OrFunction(
                            polynomials = polys,
                            converter = flt64Converter,
                            name = "estimate_load_${position}"
                        ),
                        converter = flt64Converter
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::actualLoaded.isInitialized) {
            actualLoaded = LinearIntermediateSymbols1<Flt64>("actual_loaded", Shape1(positions.size)) { j, _ ->
                val position = positions[j]
                if (position.status.available) {
                    if (position.mla eq UInt64.one) {
                        LinearExpressionSymbol(
                            loadAmount[j],
                            name = "actual_loaded_${position}"
                        )
                    } else {
                        LinearFunctionSymbolAdapter(
                            delegate = BinaryzationFunction(
                                LinearPolynomial(loadAmount[j]),
                                converter = flt64Converter,
                                name = "actual_loaded_${position}"
                            ),
                            converter = flt64Converter
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loadEstimateLongitudinalTorque.isInitialized) {
            loadEstimateLongitudinalTorque = QuantityLinearIntermediateSymbols1<Flt64>("load_longitudinal_torque", Shape1(positions.size)) { j, _ ->
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::loadActualLongitudinalTorque.isInitialized) {
            loadActualLongitudinalTorque = QuantityLinearIntermediateSymbols1<Flt64>("load_actual_longitudinal_torque", Shape1(positions.size)) { j, _ ->
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
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (aircraftModel.wideBody) {
            if (!::loadLateralTorque.isInitialized) {
                loadLateralTorque = QuantityLinearIntermediateSymbols1<Flt64>("load_lateral_torque", Shape1(positions.size)) { j, _ ->
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
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }

            if (!::loadCLIM.isInitialized) {
                loadCLIM = QuantityLinearIntermediateSymbols1<Flt64>("load_clim", Shape1(positions.size)) { j, _ ->
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
                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
            }
        }

        if (!::loadIndex.isInitialized) {
            loadIndex = QuantityLinearIntermediateSymbols1<Flt64>("load_index", Shape1(positions.size)) { j, _ ->
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













