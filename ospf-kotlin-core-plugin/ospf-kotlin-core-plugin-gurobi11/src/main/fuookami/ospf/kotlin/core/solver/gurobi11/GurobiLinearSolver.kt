/** Gurobi 11 线性求解器 / Gurobi 11 Linear Solver */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.*
import com.gurobi.gurobi.*
import fuookami.ospf.kotlin.core.model.basic.nonNullConstraintPriorityAmount
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.*
import fuookami.ospf.kotlin.core.solver.config.GurobiSolverConfig
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.value.toSolverDouble
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.utils.concept.copyIfNotNullOr
import fuookami.ospf.kotlin.utils.functional.*

/** Gurobi 11 线性求解器 / Gurobi 11 linear solver */
class GurobiLinearSolver(
    override val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack? = null
) : LinearSolver {
    internal var nativePiecewiseWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<UnivariateLinearPiecewiseStructure<*>>
    ) -> Try = ::addGurobiNativePiecewise
    internal var nativeAbsWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<AbsStructure<*>>
    ) -> Try = ::addGurobiNativeAbs
    internal var nativeMaxWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<MaxStructure<*>>
    ) -> Try = ::addGurobiNativeMax
    internal var nativeSemiWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<SemiStructure<*>>
    ) -> Try = ::addGurobiNativeSemi
    internal var nativeIndicatorWriter: (GRBModel, Map<VariableItemKey, GRBVar>, List<IndicatorStructure<*>>) -> Try =
        ::addGurobiNativeIndicator
    internal var nativeMaskingWriter: (GRBModel, Map<VariableItemKey, GRBVar>, List<MaskingStructure<*>>) -> Try =
        ::addGurobiNativeMasking
    internal var nativeBinaryLogicWriter: (GRBModel, Map<VariableItemKey, GRBVar>, List<BinaryLogicStructure<*>>) -> Try =
        ::addGurobiNativeBinaryLogic

    /**
     * 编号前选择原生 PWL，写入失败时释放原生模型并重建 fallback。 /
     * Select native PWL before indexing, then dispose and rebuild the fallback model on writer failure.
     *
     * @param model 机制模型 / Mechanism model
     * @param converter 结果转换器 / Result converter
     * @param solvingStatusCallBack 求解状态回调 / Solving status callback
     * @return 求解报告或错误 / Solve report or an error
     */
    override suspend fun <V> solve(
        model: MechanismModel<V>,
        converter: IntoValue<V>,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<V>> where V : RealNumber<V>, V : NumberField<V> {
        if (config.functionExpansionPolicy == FunctionExpansionPolicy.EAGER ||
            model !is LinearMechanismModel<*> || model.functionExpansionPolicy == FunctionExpansionPolicy.EAGER
        ) {
            return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
        }
        val converted = when (val result = convertMechanismModelToFlt64(model)) {
            is Ok -> result.value as? LinearMechanismModel<Flt64>
                ?: return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        try {
            val candidatesPwl = selectGurobiNativePiecewise(converted)
            val candidatesAbs = selectGurobiNativeAbs(converted)
            val candidatesMax = selectGurobiNativeMax(converted)
            val candidatesIndicator = selectGurobiNativeIndicator(converted)
            val candidatesMasking = selectGurobiNativeMasking(converted)
            val candidatesBinaryLogic = converted.deferredFunctionStructures.filterIsInstance<BinaryLogicStructure<*>>().filter {
                it.operation != BinaryLogicOperation.Xor && it.usage.location != FunctionUsageLocation.Unused &&
                    it.usage.location != FunctionUsageLocation.External && gurobiFunctionSolverCapabilities().supports(FunctionNativeCapability.BinaryLogic)
            }
            val candidatesSemi = converted.deferredFunctionStructures.filterIsInstance<SemiStructure<*>>()
            if (candidatesPwl.isEmpty() && candidatesAbs.isEmpty() && candidatesMax.isEmpty() && candidatesSemi.isEmpty() && candidatesIndicator.isEmpty() && candidatesMasking.isEmpty() && candidatesBinaryLogic.isEmpty()) {
                return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
            }
            val nativeModel = when (val result = LinearTriadModel.invokeResult(
                model = converted,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent,
                nativeFunctionKeys = (candidatesPwl.map { it.resultVariable.key } +
                    candidatesAbs.map { it.resultVariable.key } +
                    candidatesMax.map { it.resultVariable.key } +
                    candidatesSemi.map { it.resultVariable.key } + candidatesIndicator.map { it.resultVariable.key } +
                    candidatesMasking.map { it.resultVariable.key } + candidatesBinaryLogic.map { it.resultVariable.key }).toSet()
            )) {
                is Ok -> result.value
                is Failed, is Fatal -> return super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
            }
            val attempt = nativeModel.use {
                val nativeKeys = planGurobiFunctionLowering(it)
                    .filter { plan -> plan.decision.useNative }
                    .mapNotNull { plan ->
                        when (val structure = plan.structure) {
                            is UnivariateLinearPiecewiseStructure<*> -> structure.resultVariable.key
                            is AbsStructure<*> -> structure.resultVariable.key
                            is MaxStructure<*> -> structure.resultVariable.key
                            is SemiStructure<*> -> structure.resultVariable.key
                            is IndicatorStructure<*> -> structure.resultVariable.key
                            is MaskingStructure<*> -> structure.resultVariable.key
                            is BinaryLogicStructure<*> -> structure.resultVariable.key
                            else -> null
                        }
                    }
                    .toSet()
                val absKeys = it.deferredFunctionStructures.filterIsInstance<AbsStructure<*>>()
                    .map { structure -> structure.resultVariable.key }
                    .toSet()
                val maxKeys = it.deferredFunctionStructures.filterIsInstance<MaxStructure<*>>()
                    .map { structure -> structure.resultVariable.key }
                    .toSet()
                val semiKeys = it.deferredFunctionStructures.filterIsInstance<SemiStructure<*>>()
                    .map { structure -> structure.resultVariable.key }
                    .toSet()
                if (candidatesPwl.any { structure -> structure.resultVariable.key !in nativeKeys } ||
                    candidatesAbs.any { structure -> structure.resultVariable.key !in absKeys } ||
                    candidatesMax.any { structure -> structure.resultVariable.key !in maxKeys } ||
                    candidatesSemi.any { structure -> structure.resultVariable.key !in semiKeys } ||
                    candidatesIndicator.any { structure -> structure.resultVariable.key !in nativeKeys } ||
                    candidatesMasking.any { structure -> structure.resultVariable.key !in nativeKeys }
                    || candidatesBinaryLogic.any { structure -> structure.resultVariable.key !in nativeKeys }
                ) {
                    return@use null
                }
                GurobiLinearSolverImpl(
                    config = config,
                    callBack = callBack,
                    statusCallBack = solvingStatusCallBack,
                    nativeStructures = candidatesPwl,
                    nativePiecewiseWriter = nativePiecewiseWriter,
                    nativeAbsStructures = candidatesAbs,
                    nativeAbsWriter = nativeAbsWriter,
                    nativeMaxStructures = candidatesMax,
                    nativeMaxWriter = nativeMaxWriter,
                    nativeSemiStructures = candidatesSemi,
                    nativeSemiWriter = nativeSemiWriter,
                    nativeIndicatorStructures = candidatesIndicator,
                    nativeIndicatorWriter = nativeIndicatorWriter,
                    nativeMaskingStructures = candidatesMasking,
                    nativeMaskingWriter = nativeMaskingWriter,
                    nativeBinaryLogicStructures = candidatesBinaryLogic,
                    nativeBinaryLogicWriter = nativeBinaryLogicWriter
                ).use { implementation ->
                    val result = implementation(it)
                    if (implementation.nativeFunctionFailed) {
                        null
                    } else {
                        when (result) {
                            is Ok -> restoreGurobiPiecewiseSolution(
                                report = result.value.withLinearBackendMetadata(it, config, descriptor),
                                nativeModel = it,
                                originalTokens = converted.tokens.tokensInSolver,
                                structures = candidatesPwl,
                                absStructures = candidatesAbs,
                                maxStructures = candidatesMax,
                                semiStructures = candidatesSemi,
                                indicatorStructures = candidatesIndicator,
                                maskingStructures = candidatesMasking,
                                binaryLogicStructures = candidatesBinaryLogic
                            )
                            is Failed -> Failed(result.error)
                            is Fatal -> Fatal(result.errors)
                        }
                    }
                }
            }
            return when (attempt) {
                null -> super<LinearSolver>.solve(model, converter, solvingStatusCallBack)
                is Ok -> Ok(attempt.value.convertTo(converter))
                is Failed -> Failed(attempt.error)
                is Fatal -> Fatal(attempt.errors)
            }
        } finally {
            converted.close()
        }
    }

    override val name = "gurobi"
    override val descriptor = SolverDescriptor(
        solverId = "gurobi11",
        backendName = "Gurobi 11",
        backendVersion = gurobi11NativeVersion(),
        pluginVersion = GurobiLinearSolver::class.java.`package`.implementationVersion,
        capabilities = SolverCapabilities(
            modelTypes = setOf(SolverModelType.LP, SolverModelType.MIP),
            warmStart = true,
            solutionPool = true,
            callback = true,
            interrupt = true
        )
    )

    override suspend operator fun invoke(
        model: LinearTriadModelView,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return GurobiLinearSolverImpl(
            config = config,
            callBack = callBack,
            statusCallBack = solvingStatusCallBack
        ).use { impl ->
            val result = impl(model)
            cleanupAfterSolverRun()
            result
        }
    }

    override suspend fun invoke(
        model: LinearTriadModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
        return if (solutionAmount leq UInt64.one) {
            this(model).map { it to emptyList() }
        } else {
            val results = ArrayList<List<Flt64>>()
            GurobiLinearSolverImpl(
                config = config,
                callBack = callBack
                    .copyIfNotNullOr { GurobiLinearSolverCallBack() }
                    .configuration { _, gurobi, _, _ ->
                        if (solutionAmount gr UInt64.one) {
                            gurobi.set(GRB.DoubleParam.PoolGap, 1.0)
                            gurobi.set(GRB.IntParam.PoolSearchMode, 2)
                            gurobi.set(GRB.IntParam.PoolSolutions, solutionAmount.toInt())
                        }
                        ok
                    }
                    .analyzingSolution { _, gurobi, variables, _ ->
                        for (i in 0 until min(solutionAmount.toInt(), gurobi.get(GRB.IntAttr.SolCount))) {
                            gurobi.set(GRB.IntParam.SolutionNumber, i)
                            val thisResults = variables.map { Flt64(it.get(GRB.DoubleAttr.Xn)) }
                            if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                results.add(thisResults)
                            }
                        }
                        ok
                    },
                statusCallBack = solvingStatusCallBack
            ).use { impl ->
                val result = impl(model).map { it to results }
                cleanupAfterSolverRun()
                result
            }
        }
    }
}

/**
 * GurobiLinearSolverImpl class.
 * GurobiLinearSolverImpl类。
*/
private class GurobiLinearSolverImpl(
    private val config: SolverConfig,
    private val callBack: GurobiLinearSolverCallBack? = null,
    private val statusCallBack: SolvingStatusCallBack? = null,
    private val nativeStructures: List<UnivariateLinearPiecewiseStructure<*>> = emptyList(),
    private val nativePiecewiseWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<UnivariateLinearPiecewiseStructure<*>>
    ) -> Try = ::addGurobiNativePiecewise,
    private val nativeAbsStructures: List<AbsStructure<*>> = emptyList(),
    private val nativeAbsWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<AbsStructure<*>>
    ) -> Try = ::addGurobiNativeAbs,
    private val nativeMaxStructures: List<MaxStructure<*>> = emptyList(),
    private val nativeMaxWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<MaxStructure<*>>
     ) -> Try = ::addGurobiNativeMax,
    private val nativeSemiStructures: List<SemiStructure<*>> = emptyList(),
    private val nativeSemiWriter: (
        GRBModel,
        Map<VariableItemKey, GRBVar>,
        List<SemiStructure<*>>
    ) -> Try = ::addGurobiNativeSemi,
    private val nativeIndicatorStructures: List<IndicatorStructure<*>> = emptyList(),
    private val nativeIndicatorWriter: (GRBModel, Map<VariableItemKey, GRBVar>, List<IndicatorStructure<*>>) -> Try =
        ::addGurobiNativeIndicator,
    private val nativeMaskingStructures: List<MaskingStructure<*>> = emptyList(),
    private val nativeMaskingWriter: (GRBModel, Map<VariableItemKey, GRBVar>, List<MaskingStructure<*>>) -> Try =
        ::addGurobiNativeMasking,
    private val nativeBinaryLogicStructures: List<BinaryLogicStructure<*>> = emptyList(),
    private val nativeBinaryLogicWriter: (GRBModel, Map<VariableItemKey, GRBVar>, List<BinaryLogicStructure<*>>) -> Try =
        ::addGurobiNativeBinaryLogic
) : GurobiSolver() {
    var nativePiecewiseFailed: Boolean = false
        private set
    var nativeAbsFailed: Boolean = false
        private set
    var nativeMaxFailed: Boolean = false
        private set
    var nativeSemiFailed: Boolean = false
        private set
    val nativeFunctionFailed: Boolean
        get() = nativeWriterFailed || nativePiecewiseFailed || nativeAbsFailed || nativeMaxFailed || nativeSemiFailed || nativeIndicatorFailed || nativeMaskingFailed || nativeBinaryLogicFailed
    private var nativeWriterFailed = false
    private var nativeMaskingFailed = false
    private var nativeIndicatorFailed = false
    private var nativeBinaryLogicFailed = false

    private lateinit var grbVars: List<GRBVar>
    private lateinit var grbConstraints: List<GRBConstr>
    private lateinit var output: SolveReport<Flt64>

    private var initialBestObj: Flt64? = null
    private var bestObj: Flt64? = null
    private var bestBound: Flt64? = null
    private var bestSolution: List<Flt64>? = null
    private var bestTime: Duration = Duration.ZERO

    suspend operator fun invoke(model: LinearTriadModelView): Ret<SolveReport<Flt64>> {
        val gurobiConfig = config.backendConfiguration as? GurobiSolverConfig
        val server = gurobiConfig?.server
        val password = gurobiConfig?.password
        val connectionTime = gurobiConfig?.connectionTime

        val processes = arrayOf(
            {
                if (server != null && password != null && connectionTime != null) {
                    it.init(
                        server = server,
                        password = password,
                        connectionTime = connectionTime,
                        name = model.name,
                        callBack = callBack?.creatingEnvironmentFunction
                    )
                } else {
                    it.init(
                        name = model.name,
                        callBack = callBack?.creatingEnvironmentFunction
                    )
                }
            },
            { it.dump(model) },
            { it.configure(model) },
            GurobiLinearSolverImpl::solve,
            GurobiLinearSolverImpl::analyzeStatus,
            GurobiLinearSolverImpl::analyzeSolution
        )
        for (process in processes) {
            when (val result = process(this)) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
        }
        return Ok(output)
    }

/**
 * Dump the linear model into Gurobi variables, constraints, and objective.
 * 将线性模型转储为 Gurobi 变量、约束和目标函数。
 *
 * @param model 待转储的线性模型视图 / the linear model view to dump
 * @return 转储成功返回成功，建模错误返回失败 / success if model was dumped, or failure on modeling error
*/
    private suspend fun dump(model: LinearTriadModelView): Try {
        return try {
            warnIgnoredConstraintPriority("gurobi11", model.nonNullConstraintPriorityAmount())

            val variableDumpingData = prepareVariableDumpingData(
                variables = model.variables,
                scopeName = "linear"
            )
            val variableAmount = model.variables.size
            val variableTypes = CharArray(variableAmount)
            for (col in model.variables.indices) {
                variableTypes[col] = GurobiVariable(model.variables[col].type).toGurobiVar()
            }
            grbVars = grbModel.addVars(
                variableDumpingData.lowerBounds,
                variableDumpingData.upperBounds,
                null,
                variableTypes,
                variableDumpingData.names,
                0,
                variableAmount
            ).toList()

            if (nativeStructures.isNotEmpty() || nativeAbsStructures.isNotEmpty() || nativeMaxStructures.isNotEmpty() || nativeSemiStructures.isNotEmpty() || nativeIndicatorStructures.isNotEmpty() || nativeMaskingStructures.isNotEmpty() || nativeBinaryLogicStructures.isNotEmpty()) {
                val variablesByKey = model.variables.mapIndexedNotNull { index, variable ->
                    variable.origin?.key?.let { it to grbVars[index] }
                }.toMap()
                val registry = NativeFunctionWriterRegistry(
                    listOf(
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativePiecewiseWriter(model, variables, batch.filterIsInstance<UnivariateLinearPiecewiseStructure<*>>())
                        },
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativeAbsWriter(model, variables, batch.filterIsInstance<AbsStructure<*>>())
                        },
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativeMaxWriter(model, variables, batch.filterIsInstance<MaxStructure<*>>())
                        },
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativeSemiWriter(model, variables, batch.filterIsInstance<SemiStructure<*>>())
                        },
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativeIndicatorWriter(model, variables, batch.filterIsInstance<IndicatorStructure<*>>())
                        },
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativeMaskingWriter(model, variables, batch.filterIsInstance<MaskingStructure<*>>())
                        },
                        NativeFunctionWriter<GRBModel, GRBVar> { model, variables, batch ->
                            nativeBinaryLogicWriter(model, variables, batch.filterIsInstance<BinaryLogicStructure<*>>())
                        }
                    )
                )
                when (val result = registry.write(
                    model = grbModel,
                    variables = variablesByKey,
                    batches = listOf(
                        nativeStructures.map { it as Any },
                        nativeAbsStructures.map { it as Any },
                        nativeMaxStructures.map { it as Any },
                        nativeSemiStructures.map { it as Any },
                        nativeIndicatorStructures.map { it as Any },
                        nativeMaskingStructures.map { it as Any },
                        nativeBinaryLogicStructures.map { it as Any }
                    )
                )) {
                    is Ok -> Unit
                    is Failed -> {
                        nativeWriterFailed = true
                        return Failed(result.error)
                    }
                    is Fatal -> {
                        nativeWriterFailed = true
                        return Fatal(result.errors)
                    }
                }
            }

            for ((col, initialResult) in variableDumpingData.initialResults) {
                grbVars[col].set(GRB.DoubleAttr.Start, initialResult)
            }

            val constraints = coroutineScope {
                if (Runtime.getRuntime().availableProcessors() > 2 && model.constraints.size > Runtime.getRuntime().availableProcessors()) {
                    val segment = computeConstraintSegmentSize(model.constraints.size)
                    val chunkAmount = (model.constraints.size + segment - 1) / segment
                    val promises = (0 until chunkAmount).map { i ->
                        async(Dispatchers.Default) {
                            val from = i * segment
                            val to = minOf(model.constraints.size, from + segment)
                            val constraints = (from until to).map { ii ->
                                val lhs = GRBLinExpr()
                                model.constraints.sparseLhs.forEachEntry(ii) { colIndex, coefficient ->
                                    lhs.addTerm(coefficient.toSolverDouble("linear.constraints.lhs[$ii][$colIndex].coefficient"), grbVars[colIndex])
                                }
                                ii to lhs
                            }
                            cleanupOnSolverMemoryPressure()
                            constraints
                        }
                    }
                    promises.flatMap { promise ->
                        val result = promise.await().map {
                            grbModel.addConstr(
                                it.second,
                                GurobiConstraintSign(model.constraints.signs[it.first]).toGurobiConstraintSign(),
                                model.constraints.rhs[it.first].toSolverDouble("linear.constraints.rhs[${it.first}]"),
                                model.constraints.names[it.first]
                            )
                        }
                        cleanupOnSolverMemoryPressure()
                        result
                    }
                } else {
                    model.constraints.indices.map { i ->
                        val lhs = GRBLinExpr()
                        model.constraints.sparseLhs.forEachEntry(i) { colIndex, coefficient ->
                            lhs.addTerm(coefficient.toSolverDouble("linear.constraints.lhs[$i][$colIndex].coefficient"), grbVars[colIndex])
                        }
                        grbModel.addConstr(
                            lhs,
                            GurobiConstraintSign(model.constraints.signs[i]).toGurobiConstraintSign(),
                            model.constraints.rhs[i].toSolverDouble("linear.constraints.rhs[$i]"),
                            model.constraints.names[i]
                        )
                    }
                }
            }
            cleanupAfterSolverRun()
            grbConstraints = constraints

            val obj = GRBLinExpr()
            for (cell in model.objective.objective) {
                obj.addTerm(cell.coefficient.toSolverDouble("linear.objective.cells[${cell.colIndex}].coefficient"), grbVars[cell.colIndex])
            }
            obj.addConstant(model.objective.constant.toSolverDouble("linear.objective.constant"))
            grbModel.setObjective(
                obj, when (model.objective.category) {
                    ObjectCategory.Minimum -> {
                        GRB.MINIMIZE
                    }

                    ObjectCategory.Maximum -> {
                        GRB.MAXIMIZE
                    }
                }
            )

            when (val result = callBack?.execIfContain(
                point = Point.AfterModeling,
                status = null,
                gurobi = grbModel,
                variables = grbVars,
                constraints = grbConstraints
            )) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

/**
 * Configure Gurobi solver parameters for the linear model.
 * 为线性模型配置 Gurobi 求解器参数。
 *
 * @param model 待配置的线性模型视图 / the linear model view to configure
 * @return 配置成功返回成功，出错返回失败 / success if configuration was applied, or failure on error
*/
    private suspend fun configure(model: LinearTriadModelView): Try {
        return try {
            grbModel.set(GRB.DoubleParam.TimeLimit, config.time.toDouble(DurationUnit.SECONDS))
            grbModel.set(GRB.DoubleParam.MIPGap, config.gap.toSolverDouble("linear.config.gap"))
            grbModel.set(GRB.IntParam.Threads, config.threadNum.toInt())

            if (config.notImprovementTime != null || callBack?.nativeCallback != null || statusCallBack != null) {
                grbModel.setCallback(object : GRBCallback() {
                    override fun callback() {
                        callBack?.nativeCallback?.invoke(this)

                        if (where == GRB.CB_MIPSOL) {
                            bestSolution = getSolution(grbVars.toTypedArray()).map { Flt64(it) }
                        }
                        if (where == GRB.CB_MIP) {
                            val currentObj = Flt64(getDoubleInfo(GRB.Callback.MIP_OBJBST))
                            val currentBound = Flt64(getDoubleInfo(GRB.Callback.MIP_OBJBND))
                            val currentTime = getDoubleInfo(GRB.Callback.RUNTIME).seconds

                            if (initialBestObj == null) {
                                initialBestObj = currentObj
                            }

                            config.notImprovementTime?.let { notImprovementTime ->
                                val previousBestObj = bestObj
                                val previousBestBound = bestBound
                                if (previousBestObj == null
                                    || previousBestBound == null
                                    || (currentObj - previousBestObj).abs() geq config.improveThreshold
                                    || (currentBound - previousBestBound).abs() geq config.improveThreshold
                                ) {
                                    bestObj = currentObj
                                    bestBound = currentBound
                                    bestTime = currentTime
                                } else if (currentTime - bestTime >= notImprovementTime
                                    && config.interruptibleTime?.let { currentTime >= it } ?: true
                                    && config.interruptibleGap?.let { (currentObj - currentBound).abs() ls it } ?: true
                                ) {
                                    abort()
                                }
                            }

                            statusCallBack?.let {
                                val callbackResult = it(
                                    SolvingStatus(
                                        solver = "gurobi",
                                        solverConfig = config,
                                        intermediateModel = model,
                                        solverModel = grbModel,
                                        solverCallBack = this,
                                        objectCategory = when (grbModel.get(GRB.IntAttr.ModelSense)) {
                                            GRB.MINIMIZE -> {
                                                ObjectCategory.Minimum
                                            }

                                            GRB.MAXIMIZE -> {
                                                ObjectCategory.Maximum
                                            }

                                            else -> {
                                                null
                                            }
                                        },
                                        time = currentTime,
                                        obj = currentObj,
                                        possibleBestObj = currentBound,
                                        bestBound = currentBound,
                                        initialBestObj = initialBestObj ?: currentObj,
                                        gap = (currentObj - currentBound + Flt64.decimalPrecision) / (currentObj + Flt64.decimalPrecision),
                                        currentBestSolution = bestSolution
                                    )
                                )
                                if (shouldAbortOnCallbackFailure(callbackResult) { abort() }) {
                                    return
                                }
                            }
                        }
                    }
                })
            }

            when (val result = callBack?.execIfContain(
                point = Point.Configuration,
                status = null,
                gurobi = grbModel,
                variables = grbVars,
                constraints = grbConstraints
            )) {
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }

                else -> {}
            }
            ok
        } catch (e: GRBException) {
            solverModelingException(e.message)
        } catch (e: Exception) {
            solverModelingException()
        }
    }

/**
 * Analyze the Gurobi solving result and extract the solution output.
 * 分析 Gurobi 求解结果并提取解输出。
 *
 * @return 成功时返回提取结果，求解失败时返回失败 / success if solution was extracted, or failure if solving failed
*/
    private suspend fun analyzeSolution(): Try {
        return try {
            if (status.succeeded) {
                val results = ArrayList<Flt64>()
                for (grbVar in grbVars) {
                    results.add(Flt64(grbVar.get(GRB.DoubleAttr.X)))
                }
                val isMip = grbModel.get(GRB.IntAttr.IsMIP) != 0
                val isMinimize = grbModel.get(GRB.IntAttr.ModelSense) == GRB.MINIMIZE
                val possibleBestObj = when {
                    isMip -> Flt64(grbModel.get(GRB.DoubleAttr.ObjBound))
                    status == SolverStatus.Optimal -> Flt64(grbModel.get(GRB.DoubleAttr.ObjVal))
                    isMinimize -> Flt64.negativeInfinity
                    else -> Flt64.infinity
                }
                val gap = when {
                    status != SolverStatus.Optimal -> Flt64.infinity
                    isMip -> Flt64(grbModel.get(GRB.DoubleAttr.MIPGap))
                    else -> Flt64.zero
                }
                output = status.toSolveReport(
                    objective = Flt64(grbModel.get(GRB.DoubleAttr.ObjVal)),
                    values = results,
                    solveTime = grbModel.get(GRB.DoubleAttr.Runtime).seconds,
                    bestBound = possibleBestObj,
                    gap = gap
                )
                when (val result = callBack?.execIfContain(
                    point = Point.AnalyzingSolution,
                    status = status,
                    gurobi = grbModel,
                    variables = grbVars,
                    constraints = grbConstraints
                )) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    else -> {}
                }
                ok
            } else {
                when (val result = callBack?.execIfContain(
                    point = Point.AfterFailure,
                    status = status,
                    gurobi = grbModel,
                    variables = grbVars,
                    constraints = grbConstraints
                )) {
                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    else -> {}
                }
                failByStatus(status)
            }
        } catch (e: GRBException) {
            solverSolvingException(e.message)
        } catch (e: Exception) {
            solverSolvingException()
        }
    }
}
