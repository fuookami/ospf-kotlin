@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2


import fuookami.ospf.kotlin.math.algebra.number.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.BendersStrategy
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.Diagnostics
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.FeasibilityDiagnostics
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.SolveMode
import fuookami.ospf.kotlin.math.algebra.number.Flt64

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class PredistributionApplication {
    suspend operator fun invoke(
        request: RequestDTO,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null,
        finnishHeartBeatCallBack: ((FinnishHeartBeatDTO) -> Unit)? = null,
        withRender: Boolean = false
    ): Pair<ResponseDTO, RenderDTO?> {
        val algo = PredistributionAlgorithmImpl()
        return algo(request, runningHeartBeatCallBack, finnishHeartBeatCallBack, withRender)
    }
}

private class PredistributionAlgorithmImpl {
    lateinit var aircraftContext: AircraftContext
    lateinit var stowageContext: StowageContext
    lateinit var macContext: MacContext
    lateinit var airworthinessSecurityContext: AirworthinessSecurityContext
    lateinit var softSecurityContext: SoftSecurityContext
    lateinit var macOptimizationContext: MacOptimizationContext
    lateinit var expressEffectivenessContext: ExpressEffectivenessContext
    lateinit var loadingEffectivenessContext: LoadingEffectivenessContext
    lateinit var redundancyContext: RedundancyContext

    suspend operator fun invoke(
        request: RequestDTO,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null,
        finnishHeartBeatCallBack: ((FinnishHeartBeatDTO) -> Unit)? = null,
        withRender: Boolean = false
    ): Pair<ResponseDTO, RenderDTO?> {
        val startTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val parameter = request.parameter
        val notes = mutableListOf<String>()

        when (val result = init(request)) {
            is Ok -> {}

            is Failed -> {
                return ResponseDTO(request, result.error) to null
            }

            is Fatal -> {
                return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
            }
        }

        FeasibilityDiagnostics.appendCoreFeasibilityDiagnostics(request, notes)
        if (notes.isNotEmpty()) {
            return ResponseDTO.noSolution("NoSolution", notes) to null
        }

        if (!BendersStrategy.supportedAircraft(request.aircraftType)) {
            notes.add("unsupported aircraft type for predistribution path: ${request.aircraftType}")
            return ResponseDTO.noSolution("UnsupportedAircraft", notes) to null
        }

        val solveMode = BendersStrategy.resolveSolveMode(request, notes)

        val solution = when (solveMode) {
            is SolveMode.Benders -> {
                notes.add("solver_path=benders")
                when (val result = solveWithBendersAlgorithm(
                    parameter = parameter,
                    runningHeartBeatCallBack = runningHeartBeatCallBack
                )) {
                    is Ok -> result.value
                    is Failed -> {
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
                                is Ok -> milpResult.value
                                is Failed -> return ResponseDTO(request, milpResult.error) to null
                                is Fatal -> return ResponseDTO(request, milpResult.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                            }
                        } else {
                            return ResponseDTO.noSolution("BendersFailed", notes) to null
                        }
                    }
                    is Fatal -> {
                        if (request.solvePolicy.bendersFallbackToMilp) {
                            notes.add("Benders fatal, falling back to MILP")
                            when (val milpResult = solveWithMILP(
                                id = request.id,
                                parameter = parameter,
                                startTime = startTime,
                                runningHeartBeatCallBack = runningHeartBeatCallBack
                            )) {
                                is Ok -> milpResult.value
                                is Failed -> return ResponseDTO(request, milpResult.error) to null
                                is Fatal -> return ResponseDTO(request, milpResult.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
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
                            is Ok -> result.value
                            is Failed -> return ResponseDTO(request, result.error) to null
                            is Fatal -> return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                        }
                    }
                    AircraftType.B767 -> TODO("not implemented yet")
                    AircraftType.B747 -> TODO("not implemented yet")
                    null -> TODO("not implemented yet")
                }
            }
        }

        val output = when (val result = stowageContext.analyze(solution, request)) {
            is Ok -> {
                result.value to if (withRender) {
                    solution.render()
                } else {
                    null
                }
            }

            is Failed -> {
                return ResponseDTO(request, result.error) to null
            }

            is Fatal -> {
                return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
            }
        }

        runningHeartBeatCallBack?.let {
            val runTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
            RunningHeartBeatDTO(
                id = request.id,
                runTime = runTime,
                estimatedTime = runTime,
                optimizedRate = Flt64(1.0)
            )
        }

        return output
    }

    private fun init(request: RequestDTO): Try {
        when (val result = aircraftContext.init(
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = stowageContext.init(
            aircraftContext = aircraftContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = macContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = airworthinessSecurityContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            macContext = macContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = softSecurityContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = macOptimizationContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            macContext = macContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = expressEffectivenessContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = loadingEffectivenessContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = redundancyContext.init(
            aircraftContext = aircraftContext,
            stowageContext = stowageContext,
            input = request
        )) {
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

    private suspend fun solveWithMILP(
        id: String,
        parameter: Parameter,
        startTime: kotlinx.datetime.Instant,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null
    ): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution> {
        val model = LinearMetaModel<Flt64>(converter = flt64Converter)
        when (val result = register(parameter, model)) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        val timeLimit = 1.minutes
        runningHeartBeatCallBack?.let {
            val runTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
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
        val modelSolution = when (val result = solver(
            model = model,
            registrationStatusCallBack = { status ->
                runningHeartBeatCallBack?.let {
                    val runTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
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
                    val runTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis()) - startTime
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
            is Ok -> {
                result.value
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                    TODO("not implemented yet")
                } else {
                    return Failed(result.error)
                }
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        val solution = when (val result = stowageContext.analyze(modelSolution.solution, model)) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return Ok<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution, ErrorCode, Error<ErrorCode>>(solution)
    }

    private fun register(
        parameter: Parameter,
        model: AbstractLinearMetaModel<Flt64>
    ): Try {
        when (val result = stowageContext.register(
            stowageMode = StowageMode.Predistribution,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = macContext.register(
            stowageMode = StowageMode.Predistribution,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = airworthinessSecurityContext.register(
            stowageMode = StowageMode.Predistribution,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = softSecurityContext.register(
            stowageMode = StowageMode.Predistribution,
            parameter = parameter,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = macOptimizationContext.register(
            stowageMode = StowageMode.Predistribution,
            parameter = parameter,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = expressEffectivenessContext.register(
            stowageMode = StowageMode.Predistribution,
            parameter = parameter,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = loadingEffectivenessContext.register(
            stowageMode = StowageMode.Predistribution,
            parameter = parameter,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        when (val result = redundancyContext.register(
            stowageMode = StowageMode.Predistribution,
            parameter = parameter,
            model = model
        )) {
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

    private fun solveWithBendersAlgorithm(
        parameter: Parameter,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null
    ): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution> {
        TODO("not implemented yet")
    }
}




















