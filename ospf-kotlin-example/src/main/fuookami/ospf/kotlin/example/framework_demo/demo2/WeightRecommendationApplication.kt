@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2

import kotlinx.datetime.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearBendersDecompositionSolver
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.payload_maximization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.recommended_weight_equalization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.solveLinearMetaModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * 重量推荐应用程序入口
 * Entry point for the weight recommendation application
 */
class WeightRecommendationApplication {
    /**
     * Benders 分解的模型容器
     * Container for Benders decomposition models
     * @property masterModel Benders 主问题模型 / Master problem model
     * @property subModel Benders 子问题模型 / Sub problem model
     * @property objectVariable 目标变量 / Object variable
     * @property fixedVariables 固定变量映射 / Fixed variables map
     */
    private data class BendersModels(
        val masterModel: LinearMetaModel<Flt64>,
        val subModel: LinearMetaModel<Flt64>,
        val objectVariable: fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>,
        val fixedVariables: Map<fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>, Flt64>
    )
}

/**
 * 重量推荐算法实现
 * Implementation of the weight recommendation algorithm
 */
private class WeightRecommendationAlgorithmImpl {
    lateinit var aircraftContext: AircraftContext
    lateinit var stowageContext: StowageContext
    lateinit var macContext: MacContext
    lateinit var airworthinessSecurityContext: AirworthinessSecurityContext
    lateinit var recommendedWeightEqualizationContext: RecommendedWeightEqualizationContext
    lateinit var payloadMaximizationContext: PayloadMaximizationContext

    suspend operator fun invoke(
        request: RequestDTO,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null,
        finnishHeartBeatCallBack: ((FinnishHeartBeatDTO) -> Unit)? = null,
        withRender: Boolean = false
    ): Pair<ResponseDTO, RenderDTO?> {
        val startTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val parameter = request.parameter
        val notes = mutableListOf<String>()

        when (val result = init(request)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return ResponseDTO(request, result.error) to null
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
            }
        }

        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        if (notes.isNotEmpty()) {
            return ResponseDTO.noSolution("NoSolution", notes) to null
        }

        if (!BendersStrategy.supportedAircraft(request.aircraftType)) {
            notes.add("unsupported aircraft type for weight-recommendation path: ${request.aircraftType}")
            return ResponseDTO.noSolution("UnsupportedAircraft", notes) to null
        }

        val solveMode = BendersStrategy.resolveSolveMode(request, notes)

        val solution = when (solveMode) {
            is SolveMode.Benders -> {
                when (val result = solveWithBendersAlgorithm(
                    request = request,
                    notes = notes
                )) {
                    is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> result.value!!
                    is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        if (request.solvePolicy.bendersFallbackToMilp) {
                            notes.add("Benders failed, falling back to MILP")
                            Diagnostics.pushGroupedNote(
                                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                                Diagnostics.CODE_BENDERS_FAILED, "benders failed, fallback to milp"
                            )
                            when (val milpResult = solveWithMILP(
                                id = request.id,
                                parameter = parameter,
                                startTime = startTime,
                                runningHeartBeatCallBack = runningHeartBeatCallBack
                            )) {
                                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> milpResult.value!!
                                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return ResponseDTO(request, milpResult.error) to null
                                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return ResponseDTO(request, milpResult.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                            }
                        } else {
                            return ResponseDTO.noSolution("BendersFailed", notes) to null
                        }
                    }
                    is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                        if (request.solvePolicy.bendersFallbackToMilp) {
                            notes.add("Benders fatal, falling back to MILP")
                            when (val milpResult = solveWithMILP(
                                id = request.id,
                                parameter = parameter,
                                startTime = startTime,
                                runningHeartBeatCallBack = runningHeartBeatCallBack
                            )) {
                                is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> milpResult.value!!
                                is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return ResponseDTO(request, milpResult.error) to null
                                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return ResponseDTO(request, milpResult.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                            }
                        } else {
                            return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                        }
                    }
                }
            }
            is SolveMode.Milp -> {
                notes.add("solver_path=milp_direct")
                when (aircraftContext.aggregation.aircraftModel.type) {
                    AircraftType.B737, AircraftType.B757 -> {
                        when (val result = solveWithMILP(
                            id = request.id,
                            parameter = parameter,
                            startTime = startTime,
                            runningHeartBeatCallBack = runningHeartBeatCallBack
                        )) {
                            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> result.value!!
                            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return ResponseDTO(request, result.error) to null
                            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                        }
                    }
                    AircraftType.B767 -> TODO("not implemented yet")
                    AircraftType.B747 -> TODO("not implemented yet")
                    null -> TODO("not implemented yet")
                }
            }
        }

        val output = when (val result = stowageContext.analyze(solution, request)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                result.value!! to if (withRender) {
                    solution.render()
                } else {
                    null
                }
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return ResponseDTO(request, result.error) to null
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
            }
        }

        runningHeartBeatCallBack?.let {
            val runTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
            RunningHeartBeatDTO(
                id = request.id,
                runTime = runTime,
                estimatedTime = runTime,
                optimizedRate = Flt64(1.0)
            )
        }

        return output
    }

    /**
     * 初始化所有领域上下文
     * Initialize all domain contexts
     * @param request 请求 DTO / Request DTO
     * @return 初始化结果 / Initialization result
     */
    private fun init(request: RequestDTO): Try {
        when (val result = aircraftContext.init(
            input = request
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = stowageContext.init(
            aircraftContext = aircraftContext,
            input = request
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = macContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = airworthinessSecurityContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            macContext = macContext,
            input = request
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = recommendedWeightEqualizationContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = payloadMaximizationContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
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
    
    /**
     * 使用 MILP 直接求解
     * Solve directly using MILP
     * @param id 请求 ID / Request ID
     * @param parameter 求解参数 / Solving parameter
     * @param startTime 开始时间 / Start time
     * @param runningHeartBeatCallBack 运行心跳回调 / Running heartbeat callback
     * @return 求解结果，包含配载方案 / Result containing the stowage solution
     */
    private suspend fun solveWithMILP(
        id: String,
        parameter: Parameter,
        startTime: kotlin.time.Instant,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null
    ): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution> {
        val model = LinearMetaModel<Flt64>(converter = flt64Converter)
        when (val result = register(parameter, model)) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        val timeLimit = 1.minutes
        runningHeartBeatCallBack?.let {
            val runTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
            RunningHeartBeatDTO(
                id = id,
                runTime = runTime,
                estimatedTime = runTime + timeLimit + 10.seconds,
                optimizedRate = Flt64(0.1)
            )
        }
        val solver = LinearSolverBuilder(
            config = SolverConfig(
                time = timeLimit,
                notImprovementTime = 30.seconds
            )
        )
        var gap: Flt64? = null
        val modelSolution = when (val result = solveLinearMetaModel(
            solver = solver,
            metaModel = model,
            registrationStatusCallBack = { status ->
                runningHeartBeatCallBack?.let {
                    val runTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
                    it(
                        RunningHeartBeatDTO(
                            id = id,
                            runTime = runTime,
                            estimatedTime = runTime + timeLimit + 30.seconds,
                            optimizedRate = Flt64(0.1) + Flt64(0.3) * status.notEmptyProgress.cub()
                        )
                    )
                }
                ok
            },
            solvingStatusCallBack = { status ->
                runningHeartBeatCallBack?.let {
                    val runTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
                    if (gap == null) {
                        gap = status.gap
                        it(
                            RunningHeartBeatDTO(
                                id = id,
                                runTime = runTime,
                                estimatedTime = maxOf(
                                    timeLimit + 10.seconds,
                                    runTime + maxOf(30.seconds, (timeLimit - status.time) + 10.seconds)
                                ),
                                optimizedRate = Flt64(0.4) + Flt64(0.59) * (Flt64.one - min(Flt64.one, status.gap.abs())).sqr()
                            )
                        )
                    } else if (gap!! neq status.gap) {
                        gap = status.gap
                        it(
                            RunningHeartBeatDTO(
                                id = id,
                                runTime = runTime,
                                estimatedTime = maxOf(
                                    timeLimit + 10.seconds,
                                    runTime + maxOf(30.seconds, (timeLimit - status.time) + 10.seconds)
                                ),
                                optimizedRate = Flt64(0.4) + Flt64(0.59) * (Flt64.one - min(Flt64.one, status.gap.abs())).sqr()
                            )
                        )
                    } else {
                        // nothing to do
                    }
                }
                ok
            }
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                result.value!!
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                    TODO("not implemented yet")
                } else {
                    return Failed(result.error)
                }
            }

            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                return Fatal(result.errors)
            }
        }

        val solution = when (val result = stowageContext.analyze(
            solution = modelSolution.solution,
            model = model
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                result.value!!
            }

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        return Ok<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution, ErrorCode, Error<ErrorCode>>(solution)
    }

    /**
     * 注册所有领域上下文的约束与变量到模型中
     * Register constraints and variables from all domain contexts into the model
     * @param parameter 求解参数 / Solving parameter
     * @param model 线性元模型 / Linear meta model
     * @return 注册结果 / Registration result
     */
    private fun register(
        parameter: Parameter,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = stowageContext.register(
            stowageMode = StowageMode.WeightRecommendation,
            model = model
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = macContext.register(
            stowageMode = StowageMode.WeightRecommendation,
            model = model
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = airworthinessSecurityContext.register(
            stowageMode = StowageMode.WeightRecommendation,
            model = model
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = recommendedWeightEqualizationContext.register(
            stowageMode = StowageMode.WeightRecommendation,
            model = model
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {}

            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Failed(result.error)
                }

                is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = payloadMaximizationContext.register(
            stowageMode = StowageMode.WeightRecommendation,
            parameter = parameter,
            model = model
        )) {
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
    
    /**
     * 使用 Benders 分解算法求解
     * Solve using Benders decomposition algorithm
     * @param request 请求 DTO / Request DTO
     * @param notes 求解备注列表 / Solving notes list
     * @return 求解结果，包含配载方案 / Result containing the stowage solution
     */
    private suspend fun solveWithBendersAlgorithm(
        request: RequestDTO,
        notes: MutableList<String>
    ): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution> {
        val bendersModels = buildBendersModels()
        val bendersConfig = BendersStrategy.tuneAdaptiveConfig(
            request.bendersAdaptive,
            request.cargos.size * request.positions.size
        )

        notes.add("benders_adaptive=max_iterations=${bendersConfig.maxIterations},tolerance=${bendersConfig.tolerance}")
        Diagnostics.pushGroupedNote(
            notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
            Diagnostics.CODE_BENDERS_ADAPTIVE_EFFECTIVE,
            "max_iterations=${bendersConfig.maxIterations},tolerance=${String.format("%.6f", bendersConfig.tolerance)}"
        )

        val solver = GurobiLinearBendersDecompositionSolver()
        val bendersResult = when (val result = BendersSolver.solve(
            solver = solver,
            masterModel = bendersModels.masterModel,
            subModel = bendersModels.subModel,
            fixedVariables = bendersModels.fixedVariables,
            objectVariable = bendersModels.objectVariable,
            config = bendersConfig,
            notes = notes
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> result.value!!
            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Failed(result.error)
            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> return Fatal(result.errors)
        }

        // Quality guard check
        val qualityGuard = BendersStrategy.resolveQualityGuardConfig(request.bendersQualityOverrides)
        val qualityReason = BendersStrategy.resolveQualityReason(
            adaptive = bendersConfig,
            qualityGuard = qualityGuard,
            bendersIterations = bendersResult.bendersIterations,
            bendersGap = bendersResult.gap,
            bendersTimeMs = bendersResult.timeMs,
            executedIterations = bendersResult.runtimeMetrics?.executedIterations,
            totalCuts = bendersResult.runtimeMetrics?.totalCuts,
            iterationSnapshots = bendersResult.runtimeMetrics?.iterationSnapshots?.map { it.masterObj }
        )

        if (qualityReason != null) {
            val qualityCode = when (qualityReason) {
                "gap_guard_exceeded" -> Diagnostics.CODE_BENDERS_GAP_GUARD_EXCEEDED
                "time_guard_exceeded" -> Diagnostics.CODE_BENDERS_TIME_GUARD_EXCEEDED
                "progress_guard_triggered" -> Diagnostics.CODE_BENDERS_PROGRESS_GUARD_TRIGGERED
                "cut_efficiency_low" -> Diagnostics.CODE_BENDERS_CUT_EFFICIENCY_LOW
                "trajectory_weak" -> Diagnostics.CODE_BENDERS_TRAJECTORY_WEAK
                else -> Diagnostics.CODE_BENDERS_FAILED
            }
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                qualityCode, "benders quality reason: $qualityReason"
            )

            val qualityScore = BendersStrategy.resolveQualityScore(
                adaptive = bendersConfig,
                qualityGuard = qualityGuard,
                bendersIterations = bendersResult.bendersIterations,
                bendersGap = bendersResult.gap,
                bendersTimeMs = bendersResult.timeMs,
                executedIterations = bendersResult.runtimeMetrics?.executedIterations,
                totalCuts = bendersResult.runtimeMetrics?.totalCuts,
                iterationSnapshots = bendersResult.runtimeMetrics?.iterationSnapshots?.map { it.masterObj }
            )
            Diagnostics.pushGroupedNote(
                notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                Diagnostics.CODE_BENDERS_QUALITY_SCORE,
                "quality_score=${String.format("%.2f", qualityScore)}"
            )

            if (request.solvePolicy.bendersFallbackToMilp) {
                Diagnostics.pushGroupedNote(
                    notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                    Diagnostics.CODE_BENDERS_QUALITY_ACTION,
                    "action=fallback_to_milp reason=$qualityReason"
                )
                return Failed(fuookami.ospf.kotlin.utils.error.Err(
                    fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationError,
                    "Benders quality insufficient: $qualityReason"
                ))
            } else {
                Diagnostics.pushGroupedNote(
                    notes, Diagnostics.LEVEL_DIAGNOSTIC, Diagnostics.GROUP_SOLVER,
                    Diagnostics.CODE_BENDERS_QUALITY_ACTION,
                    "action=accept_benders reason=$qualityReason"
                )
            }
        }

        val solutionList = bendersResult.solution.map { Flt64(it) }
        return when (val result = stowageContext.analyze(
            solution = solutionList,
            model = bendersModels.masterModel
        )) {
            is Ok<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> Ok(result.value!!)
            is Failed<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> Failed(result.error)
            is Fatal<*, fuookami.ospf.kotlin.utils.error.ErrorCode, fuookami.ospf.kotlin.utils.error.Error<fuookami.ospf.kotlin.utils.error.ErrorCode>> -> Fatal(result.errors)
        }
    }

    /**
     * 构建 Benders 分解的主问题与子问题模型
     * Build master and sub problem models for Benders decomposition
     * @return Benders 模型容器 / Benders models container
     */
    private fun buildBendersModels(): BendersModels {
        val masterModel = LinearMetaModel<Flt64>(
            name = "demo2_weight_recommendation_master",
            converter = flt64Converter
        )
        val subModel = LinearMetaModel<Flt64>(
            name = "demo2_weight_recommendation_sub",
            converter = flt64Converter
        )

        // Master: stowage + mac + recommendedWeightEqualization + payloadMaximization
        stowageContext.registerForBendersMP(masterModel)
        macContext.registerForBendersMP(masterModel)
        recommendedWeightEqualizationContext.registerForBendersMP(masterModel)
        payloadMaximizationContext.registerForBendersMP(masterModel)

        // Sub: stowage (shared variables) + airworthiness
        stowageContext.registerForBendersSP(subModel, emptyList())
        airworthinessSecurityContext.registerForBendersSP(subModel)

        // Create deviation variable z for weight recommendation objective
        val zVar = URealVar("wr_max_deviation")
        masterModel.add(zVar)

        val stowageAgg = stowageContext.aggregation
        val fixedVariables = mutableMapOf<fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>, Flt64>()
        for (i in stowageAgg.items.indices) {
            for (j in stowageAgg.positions.indices) {
                fixedVariables[stowageAgg.stowage.x[i, j]] = Flt64.zero
            }
        }

        return BendersModels(
            masterModel = masterModel,
            subModel = subModel,
            objectVariable = zVar,
            fixedVariables = fixedVariables
        )
    }
    /**
     * Benders 分解的模型容器
     * Container for Benders decomposition models
     * @property masterModel Benders 主问题模型 / Master problem model
     * @property subModel Benders 子问题模型 / Sub problem model
     * @property objectVariable 目标变量 / Object variable
     * @property fixedVariables 固定变量映射 / Fixed variables map
     */
    private data class BendersModels(
        val masterModel: LinearMetaModel<Flt64>,
        val subModel: LinearMetaModel<Flt64>,
        val objectVariable: fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>,
        val fixedVariables: Map<fuookami.ospf.kotlin.core.variable.AbstractVariableItem<*, *>, Flt64>
    )
}


