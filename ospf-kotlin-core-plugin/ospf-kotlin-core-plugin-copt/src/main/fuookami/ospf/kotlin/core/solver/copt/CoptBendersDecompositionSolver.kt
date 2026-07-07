/** COPT Benders 分解求解器 / COPT Benders decomposition solver */
package fuookami.ospf.kotlin.core.solver.copt

import kotlinx.coroutines.*
import copt.COPT
import copt.get
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.framework.solver.LinearBendersDecompositionSolver
import fuookami.ospf.kotlin.framework.solver.QuadraticBendersDecompositionSolver

/**
 * COPT 线性 Benders 分解求解器 / COPT linear Benders decomposition solver
 *
 * @property config 求解器配置 / solver configuration
 * @property linearCallBack 线性求解器回调 / linear solver callback
 */
class CplexLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: CoptLinearSolverCallBack = CoptLinearSolverCallBack()
) : LinearBendersDecompositionSolver {
    override val name = "copt"

    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        return when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                val solver = CoptLinearSolver(
                    config = config,
                    callBack = linearCallBack.copy()
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(result.value.solution)
                        jobs.joinAll()
                        Ok(result.value)
                    }

                    is Failed -> {
                        jobs.joinAll()
                        Failed(result.error)
                    }

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
            }
        }
    }

    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        return when (val result = LinearMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            fixedVariables = fixedVariables,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                model.linearRelax()
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                lateinit var dualSolution: Map<Constraint<Flt64, Linear>, Flt64>
                lateinit var farkasSolution: Map<Constraint<Flt64, Linear>, Flt64>
                val solver = CoptLinearSolver(
                    config = config,
                    callBack = linearCallBack.copy()
                        .analyzingSolution { _, _, _, constraints ->
                            dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(COPT.DoubleInfo.Dual))
                            })
                            ok
                        }
                        .afterFailure { status, _, _, constraints ->
                            if (status == SolverStatus.Infeasible) {
                                farkasSolution = model.tidyDualSolution(constraints.map { constraint ->
                                    Flt64(constraint.get(COPT.DoubleInfo.DualFarkas))
                                })
                            }
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(model.tokensInSolver.mapIndexed { index, token ->
                            token.variable to result.value.solution[index]
                        }.toMap() + fixedVariables)
                        jobs.joinAll()
                        Ok(
                            LinearBendersDecompositionSolver.LinearFeasibleResult(
                                result = result.value,
                                dualSolution = dualSolution,
                                cuts = mechanismModel.generateFlt64OptimalCut(
                                    objectVariable = objectVariable,
                                    fixedVariables = fixedVariables,
                                    dualSolution = dualSolution
                                )
                            )
                        )
                    }

                    is Failed -> {
                        jobs.joinAll()
                        if (result.error.code == ErrorCode.ORModelInfeasible) {
                            Ok(
                                LinearBendersDecompositionSolver.LinearInfeasibleResult(
                                    farkasDualSolution = farkasSolution,
                                    cuts = mechanismModel.generateFlt64FeasibleCut(
                                        fixedVariables = fixedVariables,
                                        farkasDualSolution = farkasSolution
                                    )
                                )
                            )
                        } else {
                            Failed(result.error)
                        }
                    }

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
            }
        }
    }
}

/**
 * COPT 二次 Benders 分解求解器 / COPT quadratic Benders decomposition solver
 *
 * @property config 求解器配置 / solver configuration
 * @property linearCallBack 线性求解器回调 / linear solver callback
 * @property quadraticCallBack 二次求解器回调 / quadratic solver callback
 */
class CoptLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: CoptLinearSolverCallBack = CoptLinearSolverCallBack(),
    private val quadraticCallBack: CoptQuadraticSolverCallBack = CoptQuadraticSolverCallBack()
) : QuadraticBendersDecompositionSolver {
    override val name = "copt"
    private val linear = CplexLinearBendersDecompositionSolver(config, linearCallBack)

    /**
     * 求解线性主问题（委托给线性 Benders 求解器）/ Solve linear master problem (delegates to linear Benders solver)
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param toLogModel 是否导出模型文件 / whether to export model file
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果 / solving result
     */
    override suspend fun solveMaster(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        return linear.solveMaster(
            name = name,
            metaModel = metaModel,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 求解二次主问题 / Solve quadratic master problem
     *
     * @param name 模型名称 / model name
     * @param metaModel 二次元模型 / quadratic meta model
     * @param toLogModel 是否导出模型文件 / whether to export model file
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果 / solving result
     */
    override suspend fun solveMaster(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolverOutput> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        return when (val result = QuadraticMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            QuadraticTetradModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                val solver = CoptQuadraticSolver(
                    config = config,
                    callBack = quadraticCallBack.copy()
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(result.value.solution)
                        jobs.joinAll()
                        Ok(result.value)
                    }

                    is Failed -> {
                        jobs.joinAll()
                        Failed(result.error)
                    }

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
            }
        }
    }

    /**
     * 求解线性子问题（委托给线性 Benders 求解器）/ Solve linear sub problem (delegates to linear Benders solver)
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param objectVariable 目标变量 / objective variable
     * @param fixedVariables 固定变量映射 / fixed variables mapping
     * @param toLogModel 是否导出模型文件 / whether to export model file
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 子问题求解结果 / sub problem solving result
     */
    override suspend fun solveSub(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<LinearBendersDecompositionSolver.LinearSubResult> {
        return linear.solveSub(
            name = name,
            metaModel = metaModel,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            toLogModel = toLogModel,
            registrationStatusCallBack = registrationStatusCallBack,
            solvingStatusCallBack = solvingStatusCallBack
        )
    }

    /**
     * 求解二次子问题 / Solve quadratic sub problem
     *
     * @param name 模型名称 / model name
     * @param metaModel 二次元模型 / quadratic meta model
     * @param objectVariable 目标变量 / objective variable
     * @param fixedVariables 固定变量映射 / fixed variables mapping
     * @param toLogModel 是否导出模型文件 / whether to export model file
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 二次子问题求解结果 / quadratic sub problem solving result
     */
    override suspend fun solveSub(
        name: String,
        metaModel: QuadraticMetaModel<Flt64>,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<QuadraticBendersDecompositionSolver.QuadraticSubResult> {
        val jobs = ArrayList<Job>()
        if (toLogModel) {
            jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                metaModel.export("$name.opm")
            })
        }
        return when (val result = QuadraticMechanismModel(
            metaModel = metaModel,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            fixedVariables = fixedVariables,
            registrationStatusCallBack = registrationStatusCallBack
        )) {
            is Ok -> {
                result.value
            }

            is Failed -> {
                jobs.joinAll()
                return Failed(result.error)
            }

            is Fatal -> {
                jobs.joinAll()
                return Fatal(result.errors)
            }
        }.use { mechanismModel ->
            QuadraticTetradModel(
                model = mechanismModel,
                fixedVariables = fixedVariables,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: true,
                forceDumpBounds = config.dumpIntermediateModelForceBounds ?: false,
                concurrent = config.dumpIntermediateModelConcurrent
            ).use { model ->
                model.linearRelax()
                if (toLogModel) {
                    jobs.add(pluginSolverAsyncScope.launch(Dispatchers.IO) {
                        model.export("$name.lp", ModelFileFormat.LP)
                    })
                }

                lateinit var dualSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>
                lateinit var farkasSolution: kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>
                val solver = CoptQuadraticSolver(
                    config = config,
                    callBack = quadraticCallBack.copy()
                        .analyzingSolution { _, _, _, constraints ->
                            dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(COPT.DoubleInfo.Dual))
                            })
                            ok
                        }
                        .afterFailure { status, _, _, constraints ->
                            if (status == SolverStatus.Infeasible) {
                                farkasSolution = model.tidyDualSolution(constraints.map { constraint ->
                                    Flt64(constraint.get(COPT.DoubleInfo.DualFarkas))
                                })
                            }
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(model.tokensInSolver.mapIndexed { index, token ->
                            token.variable to result.value.solution[index]
                        }.toMap() + fixedVariables)
                        jobs.joinAll()
                        val cuts = when (val result = mechanismModel.generateFlt64OptimalCut(
                            objectVariable = objectVariable,
                            fixedVariables = fixedVariables,
                            dualSolution = dualSolution
                        )) {
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
                        Ok(
                            QuadraticBendersDecompositionSolver.QuadraticFeasibleResult(
                                result = result.value,
                                dualSolution = dualSolution,
                                linearCuts = cuts.filterIsInstance<LinearInequality<Flt64>>(),
                                quadraticCuts = cuts.filterIsInstance<QuadraticInequalityOf<Flt64>>()
                            )
                        )
                    }

                    is Failed -> {
                        jobs.joinAll()
                        if (result.error.code == ErrorCode.ORModelInfeasible) {
                            val cuts = when (val result = mechanismModel.generateFlt64FeasibleCut(
                                fixedVariables = fixedVariables,
                                farkasDualSolution = farkasSolution
                            )) {
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
                            Ok(
                                QuadraticBendersDecompositionSolver.QuadraticInfeasibleResult(
                                    farkasDualSolution = farkasSolution,
                                    linearCuts = cuts.filterIsInstance<LinearInequality<Flt64>>(),
                                    quadraticCuts = cuts.filterIsInstance<QuadraticInequalityOf<Flt64>>()
                                )
                            )
                        } else {
                            Failed(result.error)
                        }
                    }

                    is Fatal -> {
                        jobs.joinAll()
                        Fatal(result.errors)
                    }
                }
            }
        }
    }
}
