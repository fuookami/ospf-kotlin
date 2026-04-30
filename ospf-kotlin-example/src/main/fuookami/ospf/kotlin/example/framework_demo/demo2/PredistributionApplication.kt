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

        when (val result = init(request)) {
            is Ok -> {}

            is Failed -> {
                return ResponseDTO(request, result.error) to null
            }

            is Fatal -> {
                return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
            }
        }

        val solution = when (aircraftContext.aggregation.aircraftModel.type) {
            AircraftType.B737, AircraftType.B757 -> {
                when (val result = solveWithMILP(
                    id = request.id,
                    parameter = parameter,
                    startTime = startTime,
                    runningHeartBeatCallBack = runningHeartBeatCallBack
                )) {
                    is Ok -> {
                        result.value
                    }

                    is Failed -> {
                        return ResponseDTO(request, result.error) to null
                    }

                    is Fatal -> {
                        return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                    }
                }
            }

            AircraftType.B767 -> {
                TODO("not implemented yet")
            }

            AircraftType.B747 -> {
                TODO("not implemented yet")
            }

            null -> {
                TODO("not implemented yet")
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
        val model = LinearMetaModelFlt64()
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
        model: AbstractLinearMetaModelFlt64
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




















