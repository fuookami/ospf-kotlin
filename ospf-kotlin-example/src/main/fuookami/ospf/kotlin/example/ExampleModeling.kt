package fuookami.ospf.kotlin.example

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.RegistrationStatusCallBack
import fuookami.ospf.kotlin.core.model.intermediate.MechanismModelDumpingStatusCallBack
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.AbstractLinearSolver
import fuookami.ospf.kotlin.core.solver.output.FeasibleSolverOutput
import fuookami.ospf.kotlin.core.solver.output.SolvingStatusCallBack
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.variable.*

/**
 * Creates a [LinearPolynomial] representing a constant Flt64 value.
 *
 * @param value 参数。
 * @return 返回结果。
 */
internal fun flt64Constant(value: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), value)
}

/**
 * Creates a [LinearPolynomial] representing a single variable term with coefficient one.
 *
 * @param symbol 参数。
 * @return 返回结果。
 */
internal fun flt64Linear(symbol: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, symbol)), Flt64.zero)
}

/**
 * Creates a [LinearPolynomial] from an [AbstractVariableItem] by casting it to [Symbol].
 *
 * @param variable 参数。
 * @return 返回结果。
 */
internal fun flt64Linear(variable: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return flt64Linear(variable as Symbol)
}

/**
 * Creates a threshold-based slack function adapter for penalizing constraint violations.
 *
 * @param x 参数。
 * @param threshold 参数。
 * @param type 参数。
 * @param withNegative 参数。
 * @param withPositive 参数。
 * @param constraint 参数。
 * @param name 参数。
 * @return 返回结果。
 */
internal fun exampleThresholdSlack(
    x: ToLinearPolynomial<Flt64>,
    threshold: Flt64,
    type: VariableTypeKind = UContinuous,
    withNegative: Boolean = false,
    withPositive: Boolean = true,
    constraint: Boolean = true,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = flt64Constant(threshold),
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = true,
            constraint = constraint,
            converter = IntoValue.Identity,
            name = name
        ),
        converter = IntoValue.Identity
    )
}

/**
 * Creates an absolute-difference slack function adapter for penalizing deviations between two expressions.
 *
 * @param x 参数。
 * @param y 参数。
 * @param type 参数。
 * @param constraint 参数。
 * @param name 参数。
 * @return 返回结果。
 */
internal fun exampleAbsoluteSlack(
    x: ToLinearPolynomial<Flt64>,
    y: ToLinearPolynomial<Flt64>,
    type: VariableTypeKind = UContinuous,
    constraint: Boolean = true,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = y.toLinearPolynomial(),
            type = type,
            withNegative = true,
            withPositive = true,
            threshold = false,
            constraint = constraint,
            converter = IntoValue.Identity,
            name = name
        ),
        converter = IntoValue.Identity
    )
}

/**
 * Dumps a [LinearMetaModel] into a mechanism model, then solves it with the given solver.
 *
 * @param solver 参数。
 * @param metaModel 参数。
 * @param registrationStatusCallBack 参数。
 * @param dumpingStatusCallBack 参数。
 * @param solvingStatusCallBack 参数。
 * @return 返回结果。
 */
internal suspend fun solveLinearMetaModel(
    solver: AbstractLinearSolver,
    metaModel: LinearMetaModel<Flt64>,
    registrationStatusCallBack: RegistrationStatusCallBack? = null,
    dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null,
    solvingStatusCallBack: SolvingStatusCallBack? = null
): Ret<FeasibleSolverOutput<Flt64>> {
    val mechanism = when (val result = solver.dump(
        model = metaModel,
        registrationStatusCallBack = registrationStatusCallBack,
        dumpingStatusCallBack = dumpingStatusCallBack
    )) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val triad = solver.dump(mechanism)
    return solver(
        model = triad,
        solvingStatusCallBack = solvingStatusCallBack
    )
}
