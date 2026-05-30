/** 资源松弛工具函数 / Resource slack utility functions */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.VariableTypeKind
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * 创建常量线性多项式 / Create a constant linear polynomial
 *
 * @param value 常量值 / Constant value
 * @return 常量线性多项式 / Constant linear polynomial
 */
internal fun constantPolynomial(value: Flt64): LinearPolynomial<Flt64> {
    return LinearPolynomial(emptyList(), value)
}

/**
 * 创建资源松弛函数 / Create a resource slack function
 *
 * @param x 线性中间符号 / Linear intermediate symbol
 * @param threshold 阈值 / Threshold value
 * @param type 变量类型 / Variable type kind
 * @param withNegative 是否包含负松弛 / Whether to include negative slack
 * @param withPositive 是否包含正松弛 / Whether to include positive slack
 * @param constraint 是否作为约束 / Whether to add as constraint
 * @param name 名称 / Name
 * @return 资源松弛函数适配器 / Resource slack function adapter
 */
internal fun resourceSlack(
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
            y = constantPolynomial(threshold),
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
