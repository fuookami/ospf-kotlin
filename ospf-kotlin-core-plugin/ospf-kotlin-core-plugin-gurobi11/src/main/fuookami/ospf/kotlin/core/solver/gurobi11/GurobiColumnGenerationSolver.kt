/** Gurobi 11 列生成求解器实现 / Gurobi 11 column generation solver implementation */
package fuookami.ospf.kotlin.core.solver.gurobi11

import fuookami.ospf.kotlin.core.solver.report.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.core.solver.report.*
import com.gurobi.gurobi.GRB
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.ModelFileFormat
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModel
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.core.solver.report.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver

/** Gurobi 11 列生成求解器 / Gurobi 11 column generation solver */
class GurobiColumnGenerationSolver(
    private val config: SolverConfig = SolverConfig(),
    private val callBack: GurobiLinearSolverCallBack = GurobiLinearSolverCallBack()
) : ColumnGenerationSolver {
    override val name = "gurobi"

    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<SolveReport<Flt64>> {
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
            val model = LinearTriadModel(
                model = mechanismModel,
                fixedVariables = null,
                dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
                forceDumpBounds = config.dumpIntermediateModelForceBounds,
                concurrent = config.dumpIntermediateModelConcurrent
            )
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
                    metaModel.tokens.setSolution(result.value.values)
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

    override suspend fun solveMILP(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        amount: UInt64,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?
    ): Ret<Pair<SolveReport<Flt64>, List<List<Flt64>>>> {
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
                        metaModel.tokens.setSolution(result.value.values)
                        results.add(0, result.value.values)
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
                        metaModel.tokens.setSolution(result.value.values)
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

    /**
     * 求解 LP 松弛并保留不可行终态 / Solve the LP relaxation while preserving infeasibility.
     *
     * @param name 模型名称 / model name
     * @param metaModel 线性元模型 / linear meta model
     * @param toLogModel 是否记录模型日志 / whether to log model
     * @param registrationStatusCallBack 注册状态回调 / registration status callback
     * @param solvingStatusCallBack 求解状态回调 / solving status callback
     * @param iisConfig 不可行子系统配置 / infeasible subsystem configuration
     * @return 结构化 LP 终态 / structured LP terminal result
    */
    override suspend fun solveLPWithStatus(
        name: String,
        metaModel: LinearMetaModel<Flt64>,
        toLogModel: Boolean,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        solvingStatusCallBack: SolvingStatusCallBack?,
        iisConfig: IISConfig
    ): Ret<ColumnGenerationSolver.LPResultWithStatus> {
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
            is Ok -> result.value
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

                when (val result = solver(model, solvingStatusCallBack, iisConfig)) {
                    is Ok -> {
                        when (val output = result.value) {
                            is LinearInfeasibleSolverOutput -> {
                                jobs.joinAll()
                                Ok(ColumnGenerationSolver.LPResultWithStatus.Infeasible(output))
                            }
                            is SolveReport<*> -> {
                                @Suppress("UNCHECKED_CAST")
                                val feasible = output as SolveReport<Flt64>
                                metaModel.tokens.setSolution(feasible.values)
                                jobs.joinAll()
                                Ok(
                                    ColumnGenerationSolver.LPResultWithStatus.Feasible(
                                        ColumnGenerationSolver.LPResult(
                                            result = feasible,
                                            dualSolution = dualSolution
                                        )
                                    )
                                )
                            }
                            else -> {
                                jobs.joinAll()
                                Failed(Err(
                                    ErrorCode.IllegalArgument,
                                    "Gurobi 11 线性求解器返回了不支持的 LP 输出类型 / " +
                                        "Gurobi 11 linear solver returned an unsupported LP output type: ${output::class.qualifiedName}"
                                ))
                            }
                        }
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


