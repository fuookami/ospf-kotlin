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
 * @param value 常量值 / Constant value
 * @return 线性多项式 / Linear polynomial
 */
internal fun flt64Constant(value: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), value)
}

/**
 * Creates a [LinearPolynomial] representing a single variable term with coefficient one.
 *
 * @param symbol 符号变量 / Symbol variable
 * @return 线性多项式 / Linear polynomial
 */
internal fun flt64Linear(symbol: Symbol): LinearPolynomial<Flt64> {
    return LinearPolynomial(listOf(LinearMonomial(Flt64.one, symbol)), Flt64.zero)
}

/**
 * Creates a [LinearPolynomial] from an [AbstractVariableItem] by casting it to [Symbol].
 *
 * @param variable 变量项 / Variable item
 * @return 线性多项式 / Linear polynomial
 */
internal fun flt64Linear(variable: AbstractVariableItem<*, *>): LinearPolynomial<Flt64> {
    return flt64Linear(variable as Symbol)
}

/**
 * Creates a threshold-based slack function adapter for penalizing constraint violations.
 *
 * @param x 输入表达式 / Input expression
 * @param threshold 阈值 / Threshold value
 * @param type 变量类型 / Variable type kind
 * @param withNegative 是否包含负偏差 / Whether to include negative deviation
 * @param withPositive 是否包含正偏差 / Whether to include positive deviation
 * @param constraint 是否作为约束 / Whether to use as constraint
 * @param name 函数名称 / Function name
 * @return 线性函数符号适配器 / Linear function symbol adapter
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
 * @param x 第一个表达式 / First expression
 * @param y 第二个表达式 / Second expression
 * @param type 变量类型 / Variable type kind
 * @param constraint 是否作为约束 / Whether to use as constraint
 * @param name 函数名称 / Function name
 * @return 线性函数符号适配器 / Linear function symbol adapter
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
 * @param solver 线性求解器 / Linear solver
 * @param metaModel 线性元模型 / Linear meta model
 * @param registrationStatusCallBack 注册状态回调 / Registration status callback
 * @param dumpingStatusCallBack 转储状态回调 / Dumping status callback
 * @param solvingStatusCallBack 求解状态回调 / Solving status callback
 * @return 求解结果 / Solver result
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
