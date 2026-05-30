/** 阈值松弛工具函数 / Threshold slack utility functions */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.VariableTypeKind
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial

/**
 * 创建阈值松弛函数 / Create a threshold slack function
 *
 * @param x 线性多项式 / Linear polynomial
 * @param threshold 阈值 / Threshold value
 * @param type 变量类型 / Variable type kind
 * @param name 名称 / Name
 * @return 阈值松弛函数适配器 / Threshold slack function adapter
 */
internal fun thresholdSlack(
    x: ToLinearPolynomial<Flt64>,
    threshold: Flt64,
    type: VariableTypeKind,
    name: String
): LinearFunctionSymbolAdapter<Flt64> {
    return LinearFunctionSymbolAdapter(
        delegate = SlackFunction(
            x = x.toLinearPolynomial(),
            y = LinearPolynomial(emptyList(), threshold),
            type = type,
            withNegative = false,
            withPositive = true,
            threshold = true,
            converter = IntoValue.Identity,
            name = name
        ),
        converter = IntoValue.Identity
    )
}

/**
 * 获取正松弛多项式 / Get the positive slack polynomial
 *
 * @return 正松弛线性多项式 / Positive slack linear polynomial
 */
internal fun LinearFunctionSymbolAdapter<Flt64>.positiveSlackPolynomial(): LinearPolynomial<Flt64> {
    return requireNotNull(pos) { "threshold slack requires a positive slack polynomial" }
}

/**
 * 获取阈值封顶多项式 / Get the threshold capped polynomial
 *
 * @return 阈值封顶线性多项式 / Threshold capped linear polynomial
 */
internal fun LinearFunctionSymbolAdapter<Flt64>.thresholdCappedPolynomial(): LinearPolynomial<Flt64> {
    return requireNotNull(polyX) { "threshold slack requires a capped polynomial" }
}
