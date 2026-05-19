package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_symbol.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

class Payload(
    val plannedPayload: Quantity<Flt64>,
    val maxPayload: Quantity<Flt64>,
    val computedPayload: Quantity<Flt64>?,
    private val aircraftModel: AircraftModel,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val load: Load
) {
    lateinit var mainEstimatePayload: QuantityLinearIntermediateSymbol
    lateinit var lowEstimatePayload: QuantityLinearIntermediateSymbol
    lateinit var estimatePayload: QuantityLinearIntermediateSymbol

    lateinit var mainActualPayload: QuantityLinearIntermediateSymbol
    lateinit var lowActualPayload: QuantityLinearIntermediateSymbol
    lateinit var actualPayload: QuantityLinearIntermediateSymbol

    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::mainEstimatePayload.isInitialized) {
            mainEstimatePayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // ȫ����ģʽ�£�ҵ�ص���װ��������֮��
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    if (item.location.enabledIn(DeckLocation.Main).ok) {
                                        acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                    } else {
                                        acc
                                    }
                                }
                            ),
                            name = "main_estimate_payload"
                        )
                    }

                    StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
                        val poly = MutableLinearPolynomial()
                        for ((j, position) in positions.withIndex()) {
                            when (position.location.location) {
                                DeckLocation.Main -> {
                                    poly += LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
                                }

                                DeckLocation.LowForward, DeckLocation.LowAft -> {}
                            }
                        }
                        LinearExpressionSymbol(
                            poly,
                            name = "main_estimate_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(mainEstimatePayload)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::lowEstimatePayload.isInitialized) {
            lowEstimatePayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // ȫ����ģʽ�£�ҵ�ص���װ��������֮��
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    if (item.location.enabledIn(DeckLocation.LowAft).ok || item.location.enabledIn(DeckLocation.LowForward).ok) {
                                        acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                    } else {
                                        acc
                                    }
                                }
                            ),
                            name = "main_estimate_payload"
                        )
                    }

                    StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
                        val poly = MutableLinearPolynomial()
                        for ((j, position) in positions.withIndex()) {
                            when (position.location.location) {
                                DeckLocation.Main -> {}

                                DeckLocation.LowForward, DeckLocation.LowAft -> {
                                    poly += LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
                                }
                            }
                        }
                        LinearExpressionSymbol(
                            poly,
                            name = "low_estimate_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(lowEstimatePayload)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::estimatePayload.isInitialized) {
            estimatePayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // ȫ����ģʽ�£�ҵ�ص���װ��������֮��
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                }
                            )
                        )
                    }

                    StowageMode.Predistribution -> {
                        // Ԥ����ģʽ�£�ҵ�ص���Ԥ��ҵ��
                        LinearExpressionSymbol(
                            (computedPayload ?: plannedPayload).to(aircraftModel.weightUnit)!!.value,
                            name = "estimate_payload"
                        )
                    }

                    StowageMode.WeightRecommendation -> {
                        val poly = MutableLinearPolynomial()
                        for ((j, _) in positions.withIndex()) {
                            poly += LinearMonomial(Flt64.one, load.estimateLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
                        }
                        LinearExpressionSymbol(
                            poly,
                            name = "estimate_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(estimatePayload)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::mainActualPayload.isInitialized) {
            mainActualPayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // ȫ����ģʽ�£�ʵ���ص���װ��������֮��
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    if (item.location.enabledIn(DeckLocation.Main).ok) {
                                        acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                    } else {
                                        acc
                                    }
                                }
                            ),
                            name = "main_actual_payload"
                        )
                    }

                    StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
                        val poly = MutableLinearPolynomial()
                        for ((j, position) in positions.withIndex()) {
                            when (position.location.location) {
                                DeckLocation.Main -> {
                                    poly += LinearMonomial(Flt64.one, load.actualLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
                                }

                                DeckLocation.LowForward, DeckLocation.LowAft -> {}
                            }
                        }
                        LinearExpressionSymbol(
                            poly,
                            name = "main_actual_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(mainActualPayload)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::lowActualPayload.isInitialized) {
            lowActualPayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // ȫ����ģʽ�£�ʵ���ص���װ��������֮��
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    if (item.location.enabledIn(DeckLocation.LowAft).ok || item.location.enabledIn(DeckLocation.LowForward).ok) {
                                        acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                    } else {
                                        acc
                                    }
                                }
                            ),
                            name = "main_actual_payload"
                        )
                    }

                    StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
                        val poly = MutableLinearPolynomial()
                        for ((j, position) in positions.withIndex()) {
                            when (position.location.location) {
                                DeckLocation.Main -> {}

                                DeckLocation.LowForward, DeckLocation.LowAft -> {
                                    poly += LinearMonomial(Flt64.one, load.actualLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
                                }
                            }
                        }
                        LinearExpressionSymbol(
                            poly,
                            name = "low_actual_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(lowActualPayload)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        if (!::actualPayload.isInitialized) {
            actualPayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // ȫ����ģʽ�£�ʵ���ص���װ��������֮��
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                }
                            )
                        )
                    }

                    StowageMode.Predistribution, StowageMode.WeightRecommendation -> {
                        val poly = MutableLinearPolynomial()
                        for ((j, _) in positions.withIndex()) {
                            poly += LinearMonomial(Flt64.one, load.actualLoadWeight[j].to(aircraftModel.weightUnit)!!.value)
                        }
                        LinearExpressionSymbol(
                            poly,
                            name = "actual_payload"
                        )
                    }
                },
                aircraftModel.weightUnit
            )
        }
        when (val result = model.add(actualPayload)) {
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












