package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

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
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

class Payload(
    val plannedPayload: Quantity<Flt64>,
    val maxPayload: Quantity<Flt64>,
    val computedPayload: Quantity<Flt64>?,
    private val aircraftModel: AircraftModel,
    private val items: List<Item>,
    private val positions: List<Position>,
    private val load: Load
) {
    lateinit var mainEstimatePayload: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var lowEstimatePayload: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var estimatePayload: QuantityLinearIntermediateSymbol<Flt64>

    lateinit var mainActualPayload: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var lowActualPayload: QuantityLinearIntermediateSymbol<Flt64>
    lateinit var actualPayload: QuantityLinearIntermediateSymbol<Flt64>

    /**
     * 向模型注册业载相关中间符号。
     * Register payload-related intermediate symbols into the model.
     *
     * @param stowageMode 装载模式 / stowage mode
     * @param model 线性元模型 / linear meta model
     * @return 成功或失败 / success or failure
     */
    fun register(
        stowageMode: StowageMode,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        if (!::mainEstimatePayload.isInitialized) {
            mainEstimatePayload = Quantity(
                when (stowageMode) {
                    StowageMode.FullLoad -> {
                        // 全装载模式下，主舱的装载重量之和
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
                        // 全装载模式下，下舱的装载重量之和
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
                        // 全装载模式下，总舱的装载重量之和
                        LinearExpressionSymbol(
                            LinearPolynomial(
                                items.fold(Flt64.zero) { acc, item ->
                                    acc + item.weight.to(aircraftModel.weightUnit)!!.value
                                }
                            )
                        )
                    }

                    StowageMode.Predistribution -> {
                        // 预分配模式下，总舱为预估业载
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
                        // 全装载模式下，主舱的实际装载重量之和
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
                        // 全装载模式下，下舱的实际装载重量之和
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
                        // 全装载模式下，总舱的实际装载重量之和
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
