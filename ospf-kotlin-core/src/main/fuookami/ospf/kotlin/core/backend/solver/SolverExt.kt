package fuookami.ospf.kotlin.core.backend.solver

import fuookami.ospf.kotlin.core.backend.intermediate_model.LinearTriadModelView
import fuookami.ospf.kotlin.core.backend.intermediate_model.QuadraticTetradModelView
import fuookami.ospf.kotlin.core.backend.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.backend.solver.value.validateLinearModelValueConversion
import fuookami.ospf.kotlin.core.backend.solver.value.validateQuadraticModelValueConversion
import fuookami.ospf.kotlin.core.backend.solver.value.withSolveValueConversionPolicy
import fuookami.ospf.kotlin.core.frontend.model.Solution
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.frontend.model.mechanism.toModelBuildingStatus
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

suspend fun AbstractLinearSolver.solve(model: LinearTriadModelView): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearTriadModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    when (val validation = validateLinearModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        options.solutionAmount?.let { solutionAmount ->
            unwrapSolution(this@solveWithOptions(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = options.solvingStatusCallBack
            ))
        } ?: this@solveWithOptions(
            model = model,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }
}

suspend fun AbstractLinearSolver.solve(model: LinearMetaModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearMetaModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    val registrationStatusCallBack: RegistrationStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = false)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = registrationStatusCallBack,
        dumpingStatusCallBack = dumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptions(it, options)
    }
}

suspend fun AbstractLinearSolver.solve(model: LinearMechanismModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractLinearSolver.solveWithOptions(
    model: LinearMechanismModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    return dump(model).use {
        solveWithOptions(it, options)
    }
}

suspend fun AbstractQuadraticSolver.solve(model: QuadraticMetaModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solve(model: QuadraticTetradModelView): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticTetradModelView,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    when (val validation = validateQuadraticModelValueConversion(model, options.effectiveValueConversionPolicy)) {
        is Failed -> return Failed(validation.error)
        is Fatal -> return Fatal(validation.errors)
        else -> {}
    }

    return withSolveValueConversionPolicy(options.effectiveValueConversionPolicy) {
        options.solutionAmount?.let { solutionAmount ->
            unwrapSolution(this@solveWithOptions(
                model = model,
                solutionAmount = solutionAmount,
                solvingStatusCallBack = options.solvingStatusCallBack
            ))
        } ?: this@solveWithOptions(
            model = model,
            solvingStatusCallBack = options.solvingStatusCallBack
        )
    }
}

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticMetaModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    val registrationStatusCallBack: RegistrationStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name)) }
    }
    val dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = options.modelBuildingStatusCallBack?.let { callback ->
        { status -> callback(status.toModelBuildingStatus(model.name, quadratic = true)) }
    }

    val mechanismModel = when (val result = dump(
        model = model,
        registrationStatusCallBack = registrationStatusCallBack,
        dumpingStatusCallBack = dumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return mechanismModel.use {
        solveWithOptions(it, options)
    }
}

suspend fun AbstractQuadraticSolver.solve(model: QuadraticMechanismModel): Ret<FeasibleSolverOutput> {
    return solveWithOptions(model, SolveOptions())
}

suspend fun AbstractQuadraticSolver.solveWithOptions(
    model: QuadraticMechanismModel,
    options: SolveOptions
): Ret<FeasibleSolverOutput> {
    return dump(model).use {
        solveWithOptions(it, options)
    }
}

private fun unwrapSolution(result: Ret<Pair<FeasibleSolverOutput, List<Solution>>>): Ret<FeasibleSolverOutput> {
    return when (result) {
        is Ok -> {
            Ok(result.value.first)
        }

        is Failed -> {
            Failed(result.error)
        }

        is Fatal -> {
            Fatal(result.errors)
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearMetaModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearMechanismModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractLinearSolver.solveAsync(
    model: LinearTriadModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticMetaModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticMechanismModel,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun AbstractQuadraticSolver.solveAsync(
    model: QuadraticTetradModelView,
    options: SolveOptions,
    callBack: ((Ret<FeasibleSolverOutput>) -> Unit)? = null
): CompletableFuture<Ret<FeasibleSolverOutput>> {
    return GlobalScope.future {
        val result = solveWithOptions(model, options)
        callBack?.invoke(result)
        result
    }
}
