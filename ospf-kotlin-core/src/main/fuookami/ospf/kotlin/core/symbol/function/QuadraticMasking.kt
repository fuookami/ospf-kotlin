package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

/**
 * 二次输入的二值掩码，避免三次展开 / Binary masking of a quadratic input without cubic expansion.
 *
 * @param input 有界二次输入 / bounded quadratic input
 * @param mask 二值掩码变量 / binary mask variable
 * @param bigM 可选显式 Big-M；默认按范围推导 / optional explicit Big-M, inferred from bounds by default
 * @param converter 数值转换器 / value converter
 * @param name 符号名称 / symbol name
 * @param displayName 显示名称 / display name
 */
class QuadraticMaskingFunction<V>(
    input: QuadraticPolynomial<V>,
    private val mask: BinVar,
    private val bigM: V? = null,
    converter: IntoValue<V>,
    name: String = "quadratic_masking",
    displayName: String? = null
) : QuadraticFunctionSymbol<V>(
    inputs = listOf(input, QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, mask)), converter.zero)),
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    override fun createFunction(inputs: List<LinearPolynomial<V>>): MathFunctionSymbol<V> = MaskingFunction(
        input = inputs[0],
        mask = mask,
        bigM = bigM,
        converter = converter,
        name = name,
        displayName = displayName
    )
}
