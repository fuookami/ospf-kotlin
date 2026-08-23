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
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearBendersDecompositionSolver
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.airworthiness_security.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.mac_optimization.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.redundancy.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.soft_security.*
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
 * Entry point for the predistribution application, responsible for coordinating the execution of the predistribution algorithm.
 * 预分配应用入口类，负责协调预分配算法的执行。
*/
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

    /**
     * Benders decomposition model bundle.
     * Benders 分解的模型封装。
     *
     * @property masterModel 主问题模型 / Master problem model.
     * @property subModel 子问题模型 / Subproblem model.
     * @property objectVariable 目标变量 / Objective variable.
     * @property fixedVariables 固定变量映射 / Map of fixed variables.
    */
    private data class BendersModels(
        /** Master problem model / 主问题模型 */
        val masterModel: LinearMetaModel<Flt64>,

        /** Subproblem model / 子问题模型 */
        val subModel: LinearMetaModel<Flt64>,

        /** Objective variable / 目标变量 */
        val objectVariable: AbstractVariableItem<*, *>,

        /** Map of fixed variables / 固定变量映射 */
        val fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>
    )
}

/**
 * Concrete implementation of the predistribution algorithm.
 * 预分配算法的具体实现。
*/
private class PredistributionAlgorithmImpl {
    private val aircraftContext = AircraftContext()
    private val stowageContext = StowageContext()
    private val macContext = MacContext()
    private val airworthinessSecurityContext = AirworthinessSecurityContext()
    private val softSecurityContext = SoftSecurityContext()
    private val macOptimizationContext = MacOptimizationContext()
    private val expressEffectivenessContext = ExpressEffectivenessContext()
    private val loadingEffectivenessContext = LoadingEffectivenessContext()
    private val redundancyContext = RedundancyContext()

    suspend operator fun invoke(
        request: RequestDTO,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null,
        finnishHeartBeatCallBack: ((FinnishHeartBeatDTO) -> Unit)? = null,
        withRender: Boolean = false
    ): Pair<ResponseDTO, RenderDTO?> {
        val startTime = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val parameter = request.parameter
        val notes = mutableListOf<String>()

        if (!BendersStrategy.supportedAircraft(request.aircraftType)) {
            return unsupportedAircraftResponse(
                request = request,
                path = "predistribution",
                pathName = "预分配"
            )
        }

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

        val solveMode = BendersStrategy.resolveSolveMode(request, notes)

        val solution = when (solveMode) {
            is SolveMode.Benders -> {
                notes.add("solver_path=benders")
                when (val result = solveWithBendersAlgorithm(
                    request = request,
                    notes = notes
                )) {
                    is Ok -> result.value!!
                    is Failed -> {
                        if (request.solvePolicy.bendersFallbackToMilp) {
                            notes.add("Benders failed, falling back to MILP")
                            notes.add("solver_path=milp_fallback_after_benders")
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
                                is Ok -> milpResult.value!!
                                is Failed -> return solverFailureResponse(
                                    request = request,
                                    notes = notes,
                                    error = milpResult.error
                                )
                                is Fatal -> return ResponseDTO(request, milpResult.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                            }
                        } else {
                            return ResponseDTO.noSolution("BendersFailed", notes) to null
                        }
                    }
                    is Fatal -> {
                        if (request.solvePolicy.bendersFallbackToMilp) {
                            notes.add("Benders fatal, falling back to MILP")
                            notes.add("solver_path=milp_fallback_after_benders")
                            when (val milpResult = solveWithMILP(
                                id = request.id,
                                parameter = parameter,
                                startTime = startTime,
                                runningHeartBeatCallBack = runningHeartBeatCallBack
                            )) {
                                is Ok -> milpResult.value!!
                                is Failed -> return solverFailureResponse(
                                    request = request,
                                    notes = notes,
                                    error = milpResult.error
                                )
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
                            is Ok -> result.value!!
                            is Failed -> return solverFailureResponse(
                                request = request,
                                notes = notes,
                                error = result.error
                            )
                            is Fatal -> return ResponseDTO(request, result.firstError ?: Err(ErrorCode.ApplicationFailed)) to null
                        }
                    }
                    AircraftType.B767, AircraftType.B747, null -> return unsupportedAircraftResponse(
                        request = request,
                        path = "predistribution",
                        pathName = "预分配"
                    )
                }
            }
        }

        val output = when (val result = stowageContext.analyze(solution, request)) {
            is Ok -> {
                result.value!!.withSolverNotes(notes) to if (withRender) {
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
     * Initialize all domain contexts.
     * 初始化所有领域上下文。
     *
     * @param request 请求 DTO / Request DTO.
     * @return 初始化结果 / Initialization result.
    */
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
            input = request,
            stowageMode = StowageMode.Predistribution
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
            input = request,
            stowageMode = StowageMode.Predistribution
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
            input = request,
            stowageMode = StowageMode.Predistribution
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

    /**
     * Solve the predistribution problem using MILP solver.
     * 使用 MILP 求解器求解预分配问题。
     *
     * @param id 请求 ID / Request ID.
     * @param parameter 求解参数 / Solving parameters.
     * @param startTime 开始时间 / Start time.
     * @param runningHeartBeatCallBack 运行心跳回调 / Running heartbeat callback.
     * @return 求解结果，包含解决方案 / Result containing the solution.
    */
    private suspend fun solveWithMILP(
        id: String,
        parameter: Parameter,
        startTime: kotlin.time.Instant,
        runningHeartBeatCallBack: ((RunningHeartBeatDTO) -> Try)? = null
    ): Ret<Solution> {
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
            is Ok -> {
                result.value!!
            }

            is Failed -> {
                if (result.error.code == ErrorCode.ORModelInfeasible || result.error.code == ErrorCode.ORModelInfeasibleOrUnbounded) {
                    return Failed(Err(
                        result.error.code,
                        "预分配 MILP 无可行解：${result.error.message} / Predistribution MILP has no feasible solution: ${result.error.message}"
                    ))
                } else {
                    return Failed(result.error)
                }
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        val solution = when (val result = stowageContext.analyze(modelSolution.values, model)) {
            is Ok -> {
                result.value!!
            }

            is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
        }

        return Ok<Solution, ErrorCode, Error<ErrorCode>>(solution)
    }

    /**
     * Register all domain contexts into the optimization model.
     * 将所有领域上下文注册到优化模型中。
     *
     * @param parameter 求解参数 / Solving parameters.
     * @param model 线性元模型 / Linear meta model.
     * @return 注册结果 / Registration result.
    */
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

    /**
     * Solve the predistribution problem using Benders decomposition algorithm.
     * 使用 Benders 分解算法求解预分配问题。
     *
     * @param request 请求 DTO / Request DTO.
     * @param notes 诊断笔记列表 / List of diagnostic notes.
     * @return 求解结果，包含解决方案 / Result containing the solution.
    */
    private suspend fun solveWithBendersAlgorithm(
        request: RequestDTO,
        notes: MutableList<String>
    ): Ret<Solution> {
        val bendersModels = when (val result = buildBendersModels(request.parameter)) {
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
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
            is Ok -> result.value!!
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
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
                return Failed(Err(
                    ErrorCode.ApplicationError,
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
            is Ok -> Ok(result.value!!)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * Build master and subproblem models for Benders decomposition.
     * 构建 Benders 分解的主问题和子问题模型。
     *
     * @return Benders models bundle. / Benders 模型封装
    */
    private fun buildBendersModels(parameter: Parameter): Ret<BendersModels> {
        val masterModel = LinearMetaModel<Flt64>(
            name = "demo2_predistribution_master",
            converter = flt64Converter
        )
        val subModel = LinearMetaModel<Flt64>(
            name = "demo2_predistribution_sub",
            converter = flt64Converter
        )

        // Master: stowage + mac + soft_security + mac_optimization + express + loading + redundancy
        stowageContext.registerForBendersMP(StowageMode.Predistribution, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        macContext.registerForBendersMP(StowageMode.Predistribution, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        softSecurityContext.registerForBendersMP(StowageMode.Predistribution, parameter, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        macOptimizationContext.registerForBendersMP(StowageMode.Predistribution, parameter, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        expressEffectivenessContext.registerForBendersMP(StowageMode.Predistribution, parameter, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        loadingEffectivenessContext.registerForBendersMP(StowageMode.Predistribution, parameter, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        redundancyContext.registerForBendersMP(StowageMode.Predistribution, parameter, masterModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )

        // Sub: stowage (shared variables) + airworthiness
        stowageContext.registerForBendersSP(StowageMode.Predistribution, subModel, emptyList()).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        airworthinessSecurityContext.registerForBendersSP(StowageMode.Predistribution, subModel).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )

        // Create deviation variable z for predistribution objective
        val zVar = URealVar("max_deviation")
        masterModel.add(zVar).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )
        masterModel.minimize(
            variable = zVar,
            name = "benders_recourse_objective"
        ).orReturn(
            failedHandler = { return Failed(it) },
            fatalHandler = { return Fatal(it) }
        )

        val stowageAgg = stowageContext.aggregation
        val fixedVariables = mutableMapOf<AbstractVariableItem<*, *>, Flt64>()
        for (i in stowageAgg.items.indices) {
            for (j in stowageAgg.positions.indices) {
                fixedVariables[stowageAgg.stowage.x[i, j]] = Flt64.zero
            }
        }

        return Ok(BendersModels(
            masterModel = masterModel,
            subModel = subModel,
            objectVariable = zVar,
            fixedVariables = fixedVariables
        ))
    }

    /**
     * Benders decomposition model bundle containing master problem, subproblem, objective variable and fixed variables.
     * Benders 分解的模型封装，包含主问题、子问题、目标变量和固定变量。
     *
     * @property masterModel 主问题模型 / Master problem model.
     * @property subModel 子问题模型 / Subproblem model.
     * @property objectVariable 目标变量 / Objective variable.
     * @property fixedVariables 固定变量映射 / Map of fixed variables.
    */
    private data class BendersModels(
        /** Master problem model / 主问题模型 */
        val masterModel: LinearMetaModel<Flt64>,

        /** Subproblem model / 子问题模型 */
        val subModel: LinearMetaModel<Flt64>,

        /** Objective variable / 目标变量 */
        val objectVariable: AbstractVariableItem<*, *>,

        /** Map of fixed variables / 固定变量映射 */
        val fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>
    )
}


