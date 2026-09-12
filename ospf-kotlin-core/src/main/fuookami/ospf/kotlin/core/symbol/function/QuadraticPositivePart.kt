@file:Suppress("unused")

/** 二次正部函数符号 / Quadratic positive-part function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 二次正部函数：$y = max(p(x), 0)$。 / Quadratic positive-part function: $y = max(p(x), 0)$.
 *
 * 该函数通过精确的 `-min(-p(x), 0)` 公式复用 [QuadraticMinFunction]，不再借用半连续变量的名称。
 * / The function reuses [QuadraticMinFunction] through the exact identity `-min(-p(x), 0)` and no longer
 * borrows the name of a semi-continuous variable.
 *
 * @property input 二次输入多项式 / quadratic input polynomial
 * @param bigM 精确选择模型的 Big-M；为空时从候选范围推导 / Big-M for the exact selector model; inferred from candidate bounds when null
 * @param converter 值类型转换器 / value converter
 * @property name 唯一名称 / unique name
 * @property displayName 可选显示名称 / optional display name
 */
class QuadraticPositivePartFunction<V>(
    val input: QuadraticPolynomial<V>,
    val bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V>
        where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val negativeInput: QuadraticPolynomial<V> = QuadraticPolynomial(
        input.monomials.map { monomial ->
            QuadraticMonomial(-monomial.coefficient, monomial.symbol1, monomial.symbol2)
        },
        -input.constant
    )
    private val inner = QuadraticMinFunction(
        polynomials = listOf(
            negativeInput,
            QuadraticPolynomial(emptyList(), converter.zero)
        ),
        exact = true,
        bigM = bigM,
        converter = converter,
        name = "${name}_positive_part"
    )

    val resultVar: AbstractVariableItem<*, *>
        get() = inner.resultVar
    val selectorVars: List<AbstractVariableItem<*, *>>
        get() = inner.binVars
    val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars

    override val identifier: UInt64 by lazy { IdentifierGenerator.gen() }
    override val index: Int = 0
    override val category: Category = Linear
    override val operationCategory: Category = fuookami.ospf.kotlin.math.symbol.Quadratic
    override val parent: IntermediateSymbol<out V>? = null
    override val dependencies: Set<IntermediateSymbol<out V>>
        get() = inner.dependencies
    override val cached: Boolean = false
    override val range: ExpressionRange<V>
        get() = SolverBoundaryCasts.fullExpressionRange()

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(
            listOf(QuadraticMonomial.linear(-converter.one, resultVar)),
            converter.zero
        )

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(
        polynomial.monomials,
        polynomial.constant
    )

    override fun flush(force: Boolean) {
        inner.flush(force)
    }

    override fun prepare(
        values: Map<Symbol, V>?,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>
    ): V? = inner.prepare(values, tokenTable, converter)?.let(::negateAndNormalizeZero)

    override fun evaluate(
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = inner.evaluate(tokenTable, converter, zeroIfNone)?.let(::negateAndNormalizeZero)

    override fun evaluate(
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = inner.evaluate(results, tokenTable, converter, zeroIfNone)?.let(::negateAndNormalizeZero)

    override fun evaluate(
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = inner.evaluate(values, tokenTable, converter, zeroIfNone)?.let(::negateAndNormalizeZero)

    private fun negateAndNormalizeZero(value: V): V = if (value == converter.zero) {
        converter.zero
    } else {
        -value
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try =
        inner.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try =
        inner.registerConstraints(model)
}
