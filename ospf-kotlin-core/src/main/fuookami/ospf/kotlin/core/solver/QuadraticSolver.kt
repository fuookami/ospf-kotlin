package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModel
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.solver.config.SolverConfig
import fuookami.ospf.kotlin.core.solver.iis.IISConfig
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModelFlt64
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

interface AbstractQuadraticSolver {
    val name: String

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput>

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return solveWithOptionsAndIIS(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>>

    suspend operator fun invoke(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return solveWithOptionsAndIISForSolutionPool(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticTetradModelView,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModelFlt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput> {
        return solveWithOptions(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            )
        )
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModelFlt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return solveWithOptionsAndIIS(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModelFlt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return solveAsync(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            callBack = callBack
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModelFlt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModelFlt64,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return dump(model).use { intermediateModel ->
            this(
                model = intermediateModel,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend operator fun invoke(
        model: QuadraticMechanismModelFlt64,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return solveWithOptionsAndIISForSolutionPool(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModelFlt64,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMechanismModelFlt64,
        solutionAmount: UInt64,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModelFlt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<FeasibleSolverOutput> {
        return when (val result = dump(
            model = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
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
        }.use {
            solveWithOptions(
                model = it,
                options = SolveOptions(
                    solvingStatusCallBack = solvingStatusCallBack
                )
            )
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModelFlt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<SolverOutput> {
        return solveWithOptionsAndIIS(
            model = model,
            options = SolveOptions(
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModelFlt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<FeasibleSolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModelFlt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<SolverOutput>) -> Unit)? = null
    ): CompletableFuture<Ret<SolverOutput>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModelFlt64,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null
    ): Ret<Pair<FeasibleSolverOutput, List<Solution>>> {
        return when (val result = dump(
            model = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
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
        }.use {
            this(
                model = it,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            )
        }
    }

    suspend operator fun invoke(
        model: QuadraticMetaModelFlt64,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig
    ): Ret<Pair<SolverOutput, List<Solution>>> {
        return solveWithOptionsAndIISForSolutionPool(
            model = model,
            options = SolveOptions(
                solutionAmount = solutionAmount,
                solvingStatusCallBack = solvingStatusCallBack
            ),
            iisConfig = iisConfig,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModelFlt64,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        callBack: ((Ret<Pair<FeasibleSolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<FeasibleSolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack
            )
            callBack?.invoke(result)
            result
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun solveAsync(
        model: QuadraticMetaModelFlt64,
        solutionAmount: UInt64,
        registrationStatusCallBack: RegistrationStatusCallBack? = null,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
        solvingStatusCallBack: SolvingStatusCallBack? = null,
        iisConfig: IISConfig,
        callBack: ((Ret<Pair<SolverOutput, List<Solution>>>) -> Unit)? = null
    ): CompletableFuture<Ret<Pair<SolverOutput, List<Solution>>>> {
        return GlobalScope.future {
            val result = this@AbstractQuadraticSolver.invoke(
                model = model,
                solutionAmount = solutionAmount,
                registrationStatusCallBack = registrationStatusCallBack,
                dumpingStatusCallBack = dumpingStatusCallBack,
                solvingStatusCallBack = solvingStatusCallBack,
                iisConfig = iisConfig
            )
            callBack?.invoke(result)
            result
        }
    }

    suspend fun dump(model: QuadraticMechanismModelFlt64): QuadraticTetradModel {
        return QuadraticTetradModel(model)
    }

    suspend fun dump(
        model: QuadraticMetaModelFlt64,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModelFlt64> {
        return QuadraticMechanismModelFlt64(
            metaModel = model,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}

interface QuadraticSolver : AbstractQuadraticSolver {
    val config: SolverConfig

    override suspend fun dump(model: QuadraticMechanismModelFlt64): QuadraticTetradModel {
        return QuadraticTetradModel(
            model = model,
            fixedVariables = null,
            dumpConstraintsToBounds = config.dumpIntermediateModelBounds,
            forceDumpBounds = config.dumpIntermediateModelForceBounds,
            concurrent = config.dumpIntermediateModelConcurrent
        )
    }

    override suspend fun dump(
        model: QuadraticMetaModelFlt64,
        registrationStatusCallBack: RegistrationStatusCallBack?,
        dumpingStatusCallBack: MechanismModelDumpingStatusCallBack?
    ): Ret<QuadraticMechanismModelFlt64> {
        return QuadraticMechanismModelFlt64(
            metaModel = model,
            concurrent = config.dumpMechanismModelConcurrent,
            blocking = config.dumpMechanismModelBlocking,
            registrationStatusCallBack = registrationStatusCallBack,
            dumpingStatusCallBack = dumpingStatusCallBack
        )
    }
}



