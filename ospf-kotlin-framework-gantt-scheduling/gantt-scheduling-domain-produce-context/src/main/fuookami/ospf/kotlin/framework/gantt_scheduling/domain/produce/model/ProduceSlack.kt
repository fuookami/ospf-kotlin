/** 生产松弛工具 / Produce slack utilities */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.variable.VariableTypeKind

/**
 * 生产常数多项式 / Produce constant polynomial
 *
 * @param value 值 / Value
 * @return 线性多项式 / Linear polynomial
*/
internal fun produceConstantPolynomial(value: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), value)
}

/**
 * 生产松弛构建 / Produce slack construction
 *
 * @param x 线性中间符号 / Linear intermediate symbol
 * @param threshold 阈值 / Threshold
 * @param type 变量类型 / Variable type
 * @param withNegative 是否包含负松弛 / Whether to include negative slack
 * @param withPositive 是否包含正松弛 / Whether to include positive slack
 * @param constraint 是否为约束 / Whether it is a constraint
 * @param name 名称 / Name
 * @return 线性函数符号适配器 / Linear function symbol adapter
*/
internal fun produceSlack(
    x: LinearIntermediateSymbol<Flt64>,
    threshold: Flt64,
    type: VariableTypeKind,
    withNegative: Boolean,
    withPositive: Boolean,
    constraint: Boolean = true,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = produceConstantPolynomial(threshold),
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
