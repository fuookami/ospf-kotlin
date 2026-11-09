/**
 * ONNX 输出聚合实现 / ONNX output aggregation implementation
*/
package fuookami.ospf.kotlin.core.plugin.onnx.output

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.symbol.function.MinFunction
import fuookami.ospf.kotlin.core.plugin.onnx.error.OnnxError
import fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxExpr
import fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxExprAlgebra
import fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxExprResult
import fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxTensorExpr

/**
 * 聚合结果 / Aggregation result
 *
 * @property expr 聚合后的标量表达式 / aggregated scalar expression
 * @property symbols 聚合引入的辅助符号 / auxiliary symbols introduced by the aggregation
*/
internal data class OnnxAggregationResult(
    val expr: OnnxExpr,
    val symbols: List<IntermediateSymbol<Flt64>>
)

/**
 * 把张量表达式按选择聚合为标量表达式 / Fold a tensor expression into a scalar expression according to the selection
 *
 * `Sum`/`Mean` 直接在表达式代数内完成；`Max`/`Min` 使用核心的精确极值函数符号，
 * 因此要求输入为仿射表达式。 / `Sum`/`Mean` stay inside the expression algebra; `Max`/`Min` use the core exact extremum
 * function symbols and therefore require affine inputs.
 *
 * @param tensor 张量表达式 / tensor expression
 * @param selection 输出选择 / output selection
 * @param mode 次数模式 / degree mode
 * @param nodeName 用于错误上下文的名称 / name used for error context
 * @return 聚合结果 / aggregation result
 */
internal fun foldTensor(
    tensor: OnnxTensorExpr,
    selection: OnnxOutputSelection,
    mode: fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxDegreeMode,
    nodeName: String
): Ret<OnnxAggregationResult> {
    if (tensor.elements.isEmpty()) {
        return failed(
            OnnxError.OutputSelection(
                detail = "输出 '$nodeName' 没有元素可供聚合 / output '$nodeName' has no element to aggregate",
                tensorName = nodeName
            ).toError()
        )
    }
    return when (selection.aggregation) {
        OnnxAggregation.Element -> {
            if (selection.elementIndex !in tensor.elements.indices) {
                return failed(
                    OnnxError.OutputSelection(
                        detail = "输出 '$nodeName' 元素数 ${tensor.elements.size} 不含索引 ${selection.elementIndex} / " +
                            "output '$nodeName' has ${tensor.elements.size} elements, index ${selection.elementIndex} is out of range",
                        tensorName = nodeName
                    ).toError()
                )
            }
            ok(OnnxAggregationResult(tensor[selection.elementIndex], emptyList()))
        }

        OnnxAggregation.First -> ok(OnnxAggregationResult(tensor.elements.first(), emptyList()))

        OnnxAggregation.Last -> ok(OnnxAggregationResult(tensor.elements.last(), emptyList()))

        OnnxAggregation.Sum -> foldArithmetic(tensor, nodeName, divisor = null)

        OnnxAggregation.Mean -> foldArithmetic(
            tensor = tensor,
            nodeName = nodeName,
            divisor = Flt64(tensor.elements.size.toDouble())
        )

        OnnxAggregation.Max -> foldExtremum(
            tensor = tensor,
            nodeName = nodeName,
            mode = mode,
            maximum = true
        )

        OnnxAggregation.Min -> foldExtremum(
            tensor = tensor,
            nodeName = nodeName,
            mode = mode,
            maximum = false
        )
    }
}

/**
 * 求和 / 求平均聚合 / Sum or mean aggregation
 *
 * @param tensor 张量表达式 / tensor expression
 * @param nodeName 名称 / name
 * @param divisor 除数，为 null 表示求和 / divisor; null means sum
 * @return 聚合结果 / aggregation result
 */
private fun foldArithmetic(
    tensor: OnnxTensorExpr,
    nodeName: String,
    divisor: Flt64?
): Ret<OnnxAggregationResult> {
    var accumulator: OnnxExpr = OnnxExpr.Constant(Flt64.zero)
    for (element in tensor.elements) {
        accumulator = when (
            val sum = OnnxExprAlgebra.add(
                accumulator,
                element,
                fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxDegreeMode.Quadratic,
                nodeName
            )
        ) {
            is OnnxExprResult.Success -> sum.expr
            is OnnxExprResult.Failure -> return failed(sum.error.toError())
        }
    }
    if (divisor != null) {
        accumulator = when (
            val scaled = OnnxExprAlgebra.mul(
                accumulator,
                OnnxExpr.Constant(Flt64.one / divisor),
                fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxDegreeMode.Quadratic,
                nodeName
            )
        ) {
            is OnnxExprResult.Success -> scaled.expr
            is OnnxExprResult.Failure -> return failed(scaled.error.toError())
        }
    }
    return ok(OnnxAggregationResult(accumulator, emptyList()))
}

/**
 * 极值聚合 / Extremum aggregation
 *
 * @param tensor 张量表达式 / tensor expression
 * @param nodeName 名称 / name
 * @param mode 次数模式 / degree mode
 * @param maximum 是否取最大值 / whether to take the maximum
 * @return 聚合结果 / aggregation result
 */
private fun foldExtremum(
    tensor: OnnxTensorExpr,
    nodeName: String,
    mode: fuookami.ospf.kotlin.core.plugin.onnx.lowering.OnnxDegreeMode,
    maximum: Boolean
): Ret<OnnxAggregationResult> {
    // 全常量张量直接数值折叠，避免为常量建极值模型。 / A fully constant tensor folds numerically, avoiding an extremum model for constants.
    if (tensor.isConstant) {
        val values = tensor.elements.map { (it as OnnxExpr.Constant).value.toDouble() }
        val folded = if (maximum) values.max() else values.min()
        return ok(OnnxAggregationResult(OnnxExpr.Constant(Flt64(folded)), emptyList()))
    }
    val affine = ArrayList<fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial<Flt64>>(tensor.elements.size)
    for ((position, element) in tensor.elements.withIndex()) {
        val linear = OnnxExprAlgebra.toLinear(element)
            ?: return failed(
                OnnxError.UnsupportedOperator(
                    opType = if (maximum) "Max" else "Min",
                    detail = "极值聚合要求仿射输入，元素 $position 为二次 / " +
                        "extremum aggregation requires affine inputs, element $position is quadratic",
                    nodeName = nodeName
                ).toError()
            )
        affine.add(linear)
    }
    val converter = IntoValue.Identity
    val function = if (maximum) {
        MaxFunction(polynomials = affine, bigM = null, converter = converter, name = nodeName)
    } else {
        MinFunction(polynomials = affine, bigM = null, converter = converter, name = nodeName)
    }
    val adapter = LinearFunctionSymbolAdapter(function, converter)
    return ok(OnnxAggregationResult(OnnxExpr.Linear(adapter.polynomial), listOf(adapter)))
}

/**
 * 在数值张量上执行聚合 / Aggregate a numeric tensor
 *
 * @param data 张量数据 / tensor data
 * @param selection 输出选择 / output selection
 * @param nodeName 名称 / name
 * @return 聚合后的标量 / aggregated scalar
 */
internal fun foldTensorData(
    data: fuookami.ospf.kotlin.core.plugin.onnx.inference.OnnxTensorData,
    selection: OnnxOutputSelection,
    nodeName: String
): Ret<Double> {
    if (data.data.isEmpty()) {
        return failed(
            OnnxError.OutputSelection(
                detail = "输出 '$nodeName' 没有元素可供聚合 / output '$nodeName' has no element to aggregate",
                tensorName = nodeName
            ).toError()
        )
    }
    return when (selection.aggregation) {
        OnnxAggregation.Element -> {
            if (selection.elementIndex !in data.data.indices) {
                return failed(
                    OnnxError.OutputSelection(
                        detail = "输出 '$nodeName' 元素数 ${data.data.size} 不含索引 ${selection.elementIndex} / " +
                            "output '$nodeName' has ${data.data.size} elements, index ${selection.elementIndex} is out of range",
                        tensorName = nodeName
                    ).toError()
                )
            }
            ok(data.data[selection.elementIndex])
        }

        OnnxAggregation.First -> ok(data.data.first())
        OnnxAggregation.Last -> ok(data.data.last())
        OnnxAggregation.Sum -> ok(data.data.sum())
        OnnxAggregation.Mean -> ok(data.data.average())
        OnnxAggregation.Max -> ok(data.data.max())
        OnnxAggregation.Min -> ok(data.data.min())
    }
}
