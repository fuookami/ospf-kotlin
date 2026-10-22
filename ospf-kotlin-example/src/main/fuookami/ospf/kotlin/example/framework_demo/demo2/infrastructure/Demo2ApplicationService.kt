package fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiLinearBendersDecompositionSolver
import fuookami.ospf.kotlin.core.solver.gurobi.GurobiColumnGenerationSolver
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.solver.LinearBendersConfig
import fuookami.ospf.kotlin.framework.bpp.application.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.StowageContext
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.render
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.LoadingContextFactory
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*
import fuookami.ospf.kotlin.example.solveLinearMetaModel

/** 三种装载模式的统一编排。航空规则留在领域管线，回退重新创建完整领域。
 * / Unified orchestration; aviation rules remain in domain pipelines and fallback rebuilds all domains.
 */
internal data object Demo2ApplicationService {
    suspend fun solve(
        request: RequestDTO,
        mode: StowageMode,
        running: ((RunningHeartBeatDTO) -> Try)?,
        finished: ((FinnishHeartBeatDTO) -> Unit)?,
        withRender: Boolean
    ): Pair<ResponseDTO, RenderDTO?> {
        val start = System.nanoTime()
        val notes = arrayListOf<String>()
        fun elapsed() = ((System.nanoTime() - start) / 1000000).milliseconds
        fun fail(error: Error<ErrorCode>): Pair<ResponseDTO, RenderDTO?> {
            finished?.invoke(FinnishHeartBeatDTO(id = request.id, runTime = elapsed(), error = error))
            val status = when (error.code) {
                ErrorCode.ORModelInfeasible -> "Infeasible"
                ErrorCode.OREngineSolvingException -> "NotConverged"
                else -> "Error"
            }
            val failureNotes = notes + error.message
            return ResponseDTO(request, error).copy(
                status = status,
                notes = failureNotes,
                diagnostics = Diagnostics.buildStructured(failureNotes)
            ) to null
        }
        if (!BendersStrategy.supportedAircraft(request.aircraftType)) return unsupportedAircraftResponse(
            request = request,
            path = when (mode) { StowageMode.FullLoad -> "full-load"; StowageMode.Predistribution -> "predistribution"; else -> "weight-recommendation" },
            pathName = mode.name
        )
        if (request.solvePolicy.partitionColumnGeneration && request.solvePolicy.preferBenders) {
            return fail(Err(ErrorCode.IllegalArgument, "分区列生成与 Benders 是独立策略，不能同时启用 / Partition CG and Benders are independent strategies and cannot be enabled together"))
        }
        val beat = running?.invoke(RunningHeartBeatDTO(id = request.id, runTime = elapsed(), estimatedTime = 1.minutes, optimizedRate = Flt64.zero))
        if (beat is Failed) return fail(beat.error)
        if (beat is Fatal) return fail(beat.firstError ?: Err(ErrorCode.ApplicationFailed))
        var active: StowageContext? = null
        var configured = false
        var strategy: SolveMode = SolveMode.Milp
        fun factory(): Ret<RegisteredDiscreteLoading<Flt64>> {
            val full = if (mode == StowageMode.FullLoad) FullLoadPipelineContext() else null
            val predicate = if (mode == StowageMode.Predistribution) PredistributionPipelineContext() else null
            val recommended = if (mode == StowageMode.WeightRecommendation) WeightRecommendationPipelineContext() else null
            val initialization = when {
                full != null -> full.init(request)
                predicate != null -> predicate.init(request)
                recommended != null -> recommended.init(request)
                else -> return Failed(ErrorCode.IllegalArgument, "未知装载模式 / Unknown loading mode")
            }
            if (initialization is Failed) return Failed(initialization.error)
            if (initialization is Fatal) return Fatal(initialization.errors)
            val stowage = full?.stowageContext ?: predicate?.stowageContext ?: recommended!!.stowageContext
            val model = LinearMetaModel<Flt64>(name = "${request.id}_${mode.name}", converter = IntoValue.Identity)
            val registration = when {
                full != null -> full.register(stowageMode = mode, parameter = request.parameter, model = model)
                predicate != null -> predicate.register(stowageMode = mode, parameter = request.parameter, model = model)
                else -> recommended!!.register(request.parameter, model)
            }
            if (registration is Failed) return Failed(registration.error)
            if (registration is Fatal) return Fatal(registration.errors)
            if (!configured) {
                strategy = BendersStrategy.resolveSolveMode(
                    request = request,
                    notes = notes,
                    binaryVariables = stowage.aggregation.stowage.loadingContext.aggregation.assignments.size
                )
                configured = true
            }
            active = stowage
            return Ok(RegisteredDiscreteLoading(
                context = stowage.aggregation.stowage.loadingContext,
                model = model,
                additionalWeights = { values ->
                    val solution = when (val result = stowage.analyze(values, model)) {
                        is Ok -> result.value
                        is Failed -> return@RegisteredDiscreteLoading Failed(result.error)
                        is Fatal -> return@RegisteredDiscreteLoading Fatal(result.errors)
                    }
                    Ok(stowage.aggregation.positions.associate { position ->
                        position.spaceName to ((solution.predicateLoadWeight[position]?.value ?: Flt64.zero) +
                            (solution.recommendedLoadWeight[position]?.value ?: Flt64.zero))
                    })
                }
            ))
        }
        // 首次装配用于选择稀疏模型策略；后续回退只能调用全新工厂。
        // First registration selects the sparse-model strategy; fallback always calls a fresh factory.
        var first = when (val result = factory()) {
            is Ok -> result.value
            is Failed -> return fail(result.error)
            is Fatal -> return fail(result.firstError ?: Err(ErrorCode.ApplicationFailed))
        }.let { it as RegisteredDiscreteLoading<Flt64>? }
        val effective = (strategy as? SolveMode.Benders)?.config
        val solverConfig = SolverConfig(time = 1.minutes)
        val application = DiscreteLoadingApplicationService<Flt64>(
            milpSolver = { model -> solveLinearMetaModel(LinearSolverBuilder(config = solverConfig), model) },
            bendersSolver = GurobiLinearBendersDecompositionSolver(config = solverConfig)
        )
        val solved = if (request.solvePolicy.partitionColumnGeneration) {
            val registration = first ?: return fail(Err(ErrorCode.ApplicationFailed, "缺少列生成注册 / Missing CG registration"))
            first = null
            val configuration = LoadingContextFactory.partitionConfiguration(request, active!!.aggregation)
            when (val result = PartitionColumnGenerationApplicationService<Flt64>(GurobiColumnGenerationSolver(
                config = solverConfig.copy(dumpIntermediateModelBounds = false, dumpIntermediateModelForceBounds = false)
            )).solve(
                registration = registration,
                partitions = configuration.first,
                extraDecisions = configuration.second
            )) {
                is Ok -> {
                    notes.add("partition_columns=${result.value.columnCount};iterations=${result.value.trace.size};root_lp_converged=${result.value.lpConverged}")
                    result.value.loading
                }
                is Failed -> return fail(result.error)
                is Fatal -> return fail(result.firstError ?: Err(ErrorCode.ApplicationFailed))
            }
        } else when (val result = application.solve(
            factory = { first?.let { first = null; Ok(it) } ?: factory() },
            config = DiscreteLoadingSolveConfig(
                preferBenders = strategy is SolveMode.Benders,
                fallbackToMilp = request.solvePolicy.bendersFallbackToMilp,
                benders = LinearBendersConfig(
                    iterationLimit = effective?.maxIterations ?: 64,
                    tolerance = effective?.tolerance ?: 1e-6,
                    timeLimitMillis = 60000,
                    stallLimit = effective?.maxStallIterations ?: 8
                ),
                qualityGuard = { result ->
                    val reason = effective?.let { adaptive -> BendersStrategy.resolveQualityReason(
                        adaptive = adaptive,
                        qualityGuard = BendersStrategy.resolveQualityGuardConfig(request.bendersQualityOverrides),
                        bendersIterations = result.iterations.size,
                        bendersGap = result.gap,
                        bendersTimeMs = elapsed().inWholeMilliseconds,
                        totalCuts = result.iterations.fold(0) { total, iteration -> total + iteration.cuts },
                        iterationSnapshots = result.iterations.map { it.lowerBound }
                    ) }
                    if (reason == null) ok else Failed(ErrorCode.OREngineSolvingException, "Benders 质量守卫 / Benders quality guard: $reason")
                }
            )
        )) {
            is Ok -> result.value
            is Failed -> return fail(result.error)
            is Fatal -> return fail(result.firstError ?: Err(ErrorCode.ApplicationFailed))
        }
        val stowage = active ?: return fail(Err(ErrorCode.ApplicationFailed, "缺少装载上下文 / Missing loading context"))
        val solution = when (val result = stowage.analyze(solved.values, solved.registration.model)) {
            is Ok -> result.value
            is Failed -> return fail(result.error)
            is Fatal -> return fail(result.firstError ?: Err(ErrorCode.ApplicationFailed))
        }
        notes.add("solver_path=${solved.path}")
        solved.termination?.let { notes.add("${if (solved.path == "partition_column_generation") "partition_cg" else "benders"}_termination=$it") }
        solved.fallbackReason?.let { notes.add("Benders failed, falling back to MILP"); notes.add(it) }
        notes.add("benders_iterations=${solved.trace.size};cuts=${solved.trace.fold(0) { total, iteration -> total + iteration.cuts }}")
        val framework = if (request.aircraftType == AircraftTypeInput.B737 || request.aircraftType == AircraftTypeInput.B757) "bpp1d" else "bpp2d"
        val response = ResponseDTO(
            succeed = true,
            status = solved.status,
            objective = solved.objective.toDouble(),
            assignments = solved.solution.assignments.entries.sortedBy { it.key }.map { "${it.key} -> ${it.value}" },
            notes = notes,
            diagnostics = Diagnostics.buildStructured(notes),
            frameworkPath = framework,
            selectedLayouts = solved.solution.selectedLayouts,
            occupiedPositions = solved.solution.occupiedPositions
        )
        running?.invoke(RunningHeartBeatDTO(id = request.id, runTime = elapsed(), estimatedTime = elapsed(), optimizedRate = Flt64.one))
        finished?.invoke(FinnishHeartBeatDTO(request.id, elapsed()))
        return response to if (withRender) solution.render() else null
    }
}
