package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次输入的精确最大值 / Exact maximum of quadratic inputs.
 *
 * @param polynomials 候选二次多项式，须具有有限范围 / candidate quadratic polynomials with finite bounds
 * @param bigM 可选显式 Big-M；默认按范围推导 / optional explicit Big-M, inferred from bounds by default
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticMaxFunction<V>(
    polynomials: List<QuadraticPolynomial<V>>,
    private val bigM: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_max",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = polynomials,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = MaxFunction(
        polynomials = inputs.ifEmpty { listOf(LinearPolynomial(emptyList(), converter.zero)) },
        bigM = bigM,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
