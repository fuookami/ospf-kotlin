/** Gurobi 列生成求解器实现 / Gurobi column generation solver implementation */
package fuookami.ospf.kotlin.core.solver.gurobi

import kotlinx.coroutines.*
import gurobi.GRB
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.model.basic.ModelFileFormat
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/**
 * Gurobi 列生成求解器
 *
 * 使用 Gurobi 求解器实现列生成策略，支持 MILP 求解、多解求解和 LP 松弛求解（含对偶解提取）。
 *
 * Gurobi column generation solver
 *
 * Implements column generation strategy using Gurobi solver, supporting MILP solving, multi-solution
 * solving, and LP relaxation solving (with dual solution extraction).
 *
 * @property config 求解器配置 / solver configuration
 * @property callBack 线性求解器回调 / linear solver callback
*/
class GurobiColumnGenerationSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack = GurobiLinearSolverCallBack()
) : ColumnGenerationSolver {
    override val name = "gurobi"

    /**
     * 求解 MILP 主问题 / Solve MILP master problem
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果 / solving result
    */
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<FeasibleSolverOutput<Flt64>> {
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
                    callBack = callBack.copy()
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
     * 求解 MILP 主问题并获取多个解 / Solve MILP master problem and obtain multiple solutions
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param amount 期望解的数量 / desired number of solutions
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return 求解结果及多个解 / solving result with multiple solutions
    */
    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        amount: UInt64,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<FeasibleSolverOutput<Flt64>, List<List<Flt64>>>> {
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

                val results = ArrayList<List<Flt64>>()
                val solver = GurobiLinearSolver(
                    config = config,
                    callBack = callBack.copy()
                        .configuration { _, gurobi, _, _ ->
                            if (amount gr UInt64.one) {
                                gurobi.set(GRB.DoubleParam.PoolGap, 1.0);
                                gurobi.set(GRB.IntParam.PoolSearchMode, 2);
                                gurobi.set(GRB.IntParam.PoolSolutions, amount.toInt())
                            }
                            ok
                        }
                        .analyzingSolution { _, gurobi, variables, _ ->
                            for (i in 0 until gurobi.get(GRB.IntAttr.SolCount)) {
                                gurobi.set(GRB.IntParam.SolutionNumber, i)
                                val thisResults = variables.map { variable -> Flt64(variable.get(GRB.DoubleAttr.Xn)) }
                                if (!results.any { it.toTypedArray() contentEquals thisResults.toTypedArray() }) {
                                    results.add(thisResults)
                                }
                            }
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(result.value.solution)
                        results.add(0, result.value.solution)
                        jobs.joinAll()
                        Ok(Pair(result.value, results))
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
     * 求解 LP 松弛问题（含对偶解提取）/ Solve LP relaxation problem (with dual solution extraction)
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @return LP 求解结果（含对偶解）/ LP solving result with dual solution
    */
    override suspend fun solveLP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<ColumnGenerationSolver.LPResult> {
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
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds ?: false,
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
                val solver = GurobiLinearSolver(
                    config = config,
                    callBack = callBack.copy()
                        .analyzingSolution { _, _, _, constraints ->
                            dualSolution = model.tidyDualSolution(constraints.map { constraint ->
                                Flt64(constraint.get(GRB.DoubleAttr.Pi))
                            })
                            ok
                        }
                )

                when (val result = solver(model, solvingStatusCallBack)) {
                    is Ok -> {
                        metaModel.tokens.setSolution(result.value.solution)
                        jobs.joinAll()
                        Ok(ColumnGenerationSolver.LPResult(result.value, dualSolution))
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
}


