/** Gurobi 11 Benders 分解求解器 / Gurobi 11 Benders decomposition solver */
package fuookami.ospf.kotlin.core.solver.gurobi11

import kotlinx.coroutines.*
import com.gurobi.gurobi.GRB
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

/** Gurobi 11 线性 Benders 分解求解器 / Gurobi 11 linear Benders decomposition solver */
class GurobiLinearBendersDecompositionSolver(
    private val config: SolverConfig = SolverConfig(),
    private val linearCallBack: GurobiLinearSolverCallBack = GurobiLinearSolverCallBack()
) : LinearBendersDecompositionSolver {
    override val name = "gurobi"

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

/**
 * Gurobi 11 二次 Benders 分解求解器
 *
 * 使用 Gurobi 11 求解器实现二次 Benders 分解策略，支持线性主问题求解（委托给线性 Benders 求解器）和二次主问题求解，
 * 以及二次子问题求解（含对偶解和 Farkas 证明提取）。
 *
 * Gurobi 11 quadratic Benders decomposition solver
 *
 * Implements quadratic Benders decomposition strategy using Gurobi 11 solver, supporting linear master problem solving
 * (delegates to linear Benders solver) and quadratic master problem solving, as well as quadratic sub-problem solving
 * (with dual solution and Farkas proof extraction).
 *
 * @property config 求解器配置 / solver configuration
 * @property linearCallBack 线性求解器回调 / linear solver callback
 * @property quadraticCallBack 二次求解器回调 / quadratic solver callback
*/
class GurobiBendersDecompositionSolver(
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
