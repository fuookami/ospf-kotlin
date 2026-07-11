/** Gurobi Benders 分解求解器 / Gurobi Benders decomposition solver */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlinx.coroutines.*
import gurobi.GRB
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

/** Gurobi 线性 Benders 分解求解器 / Gurobi linear Benders decomposition solver */
/**
 * Gurobi 线性 Benders 分解求解器
 *
 * 使用 Gurobi 求解器实现线性 Benders 分解策略，支持主问题求解和子问题求解（含对偶解和 Farkas 证明提取）。
 *
 * Gurobi linear Benders decomposition solver
 *
 * Implements linear Benders decomposition strategy using Gurobi solver, supporting master problem solving
 * and sub-problem solving (with dual solution and Farkas proof extraction).
 *
 * @property config 求解器配置 / solver configuration
 * @property linearCallBack 线性求解器回调 / linear solver callback
 */
class GurobiLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: GurobiLinearSolverCallBack = GurobiLinearSolverCallBack()
) : LinearBendersDecompositionSolver {
    override val name = "gurobi"

    /**
     * 求解线性主问题 / Solve linear master problem
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param toLogModel 是否记录模型日志 / whether to log model
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

                val solver = GurobiLinearSolver(
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

    /**
     * 求解线性子问题 / Solve linear sub-problem
     *
     * 线性松弛子问题，提取对偶解或 Farkas 证明用于生成 Benders 割平面。
     *
     * Solves linear-relaxed sub-problem, extracting dual solution or Farkas proof for Benders cut generation.
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param objectVariable 目标变量 / objective variable
     * @param fixedVariables 固定变量映射 / fixed variable mapping
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 线性子问题求解结果 / linear sub-problem result
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

                lateinit var dualSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
                lateinit var farkasSolution: kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>
                val solver = GurobiLinearSolver(
                    config = config,
                    callBack = linearCallBack.copy()
                        .configuration { _, model, _, _ ->
                            model.set(GRB.IntParam.InfUnbdInfo, 1)
                            ok
                        }
                        .analyzingSolution { _, _, _, constraints ->
                            dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(GRB.DoubleAttr.Pi))
                            })
                            ok
                        }
                        .afterFailure { status, _, _, constraints ->
                            if (status == SolverStatus.Infeasible) {
                                farkasSolution = model.tidyDualSolution(constraints.map { constraint ->
                                    Flt64(constraint.get(GRB.DoubleAttr.FarkasDual))
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

/** Gurobi 二次 Benders 分解求解器 / Gurobi quadratic Benders decomposition solver */
/**
 * Gurobi 二次 Benders 分解求解器
 *
 * 使用 Gurobi 求解器实现二次 Benders 分解策略，支持线性主问题委托和二次子问题求解。
 *
 * Gurobi quadratic Benders decomposition solver
 *
 * Implements quadratic Benders decomposition strategy using Gurobi solver, supporting linear master problem
 * delegation and quadratic sub-problem solving.
 *
 * @property config 求解器配置 / solver configuration
 * @property linearCallBack 线性求解器回调 / linear solver callback
 * @property quadraticCallBack 二次求解器回调 / quadratic solver callback
 */
class GurobiQuadraticBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: GurobiLinearSolverCallBack = GurobiLinearSolverCallBack(),
    private val quadraticCallBack: GurobiQuadraticSolverCallBack = GurobiQuadraticSolverCallBack()
) : QuadraticBendersDecompositionSolver {
    override val name = "gurobi"
    private val linear = GurobiLinearBendersDecompositionSolver(config, linearCallBack)

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

                val solver = GurobiQuadraticSolver(
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
     * 求解线性子问题 / Solve linear sub-problem
     *
     * 线性松弛子问题，提取对偶解或 Farkas 证明用于生成 Benders 割平面。
     *
     * Solves linear-relaxed sub-problem, extracting dual solution or Farkas proof for Benders cut generation.
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param objectVariable 目标变量 / objective variable
     * @param fixedVariables 固定变量映射 / fixed variable mapping
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 线性子问题求解结果 / linear sub-problem result
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
     * 求解线性子问题 / Solve linear sub-problem
     *
     * 线性松弛子问题，提取对偶解或 Farkas 证明用于生成 Benders 割平面。
     *
     * Solves linear-relaxed sub-problem, extracting dual solution or Farkas proof for Benders cut generation.
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param objectVariable 目标变量 / objective variable
     * @param fixedVariables 固定变量映射 / fixed variable mapping
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 线性子问题求解结果 / linear sub-problem result
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
                val solver = GurobiQuadraticSolver(
                    config = config,
                    callBack = quadraticCallBack.copy()
                        .configuration { _, model, _, _ ->
                            model.set(GRB.IntParam.InfUnbdInfo, 1)
                            ok
                        }
                        .analyzingSolution { _, _, _, constraints ->
                            dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(GRB.DoubleAttr.Pi))
                            })
                            ok
                        }
                        .afterFailure { status, _, _, constraints ->
                            if (status == SolverStatus.Infeasible) {
                                farkasSolution = model.tidyDualSolution(constraints.map { constraint ->
                                    Flt64(constraint.get(GRB.DoubleAttr.FarkasDual))
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
