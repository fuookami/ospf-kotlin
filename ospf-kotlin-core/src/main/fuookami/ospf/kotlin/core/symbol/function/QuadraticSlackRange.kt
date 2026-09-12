@file:Suppress("unused")

/** 二次区间距离函数符号 / Quadratic range-distance function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 二次表达式到闭区间的精确距离：$s = max(lower-p(x), p(x)-upper, 0)$。
 * Exact distance from a quadratic expression to a closed interval:
 * $s = max(lower-p(x), p(x)-upper, 0)$.
 */
class QuadraticSlackRangeFunction<V>(
    val input: QuadraticPolynomial<V>,
    val lower: V,
    val upper: V,
    val bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V>
        where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    init {
        require(lower.isFinite() && upper.isFinite()) {
            "QuadraticSlackRangeFunction bounds must be finite"
        }
        require(!(lower gr upper)) { "QuadraticSlackRangeFunction requires lower <= upper" }
    }

    val lowerViolation = QuadraticPositivePartFunction(
        input = QuadraticPolynomial(
            input.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
            lower - input.constant
        ),
        bigM = bigM,
        converter = converter,
        name = "${name}_lower"
    )
    val upperViolation = QuadraticPositivePartFunction(
        input = QuadraticPolynomial(input.monomials, input.constant - upper),
        bigM = bigM,
        converter = converter,
        name = "${name}_upper"
    )

    override val identifier: UInt64 by lazy { IdentifierGenerator.gen() }
    override val index: Int = 0
    override val category: Category = Linear
    override val operationCategory: Category = Quadratic
    override val parent: IntermediateSymbol<out V>? = null
    override val dependencies: Set<IntermediateSymbol<out V>>
        get() = lowerViolation.dependencies + upperViolation.dependencies
    override val cached: Boolean = false
    override val range: ExpressionRange<V>
        get() = SolverBoundaryCasts.fullExpressionRange()

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(
            lowerViolation.polynomial.monomials + upperViolation.polynomial.monomials,
            lowerViolation.polynomial.constant + upperViolation.polynomial.constant
        )

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(
        polynomial.monomials,
        polynomial.constant
    )

    override fun flush(force: Boolean) {
        lowerViolation.flush(force)
        upperViolation.flush(force)
    }

    override fun prepare(
        values: Map<Symbol, V>?,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>
    ): V? = combine(
        lowerViolation.prepare(values, tokenTable, converter),
        upperViolation.prepare(values, tokenTable, converter)
    )

    override fun evaluate(
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = combine(
        lowerViolation.evaluate(tokenTable, converter, zeroIfNone),
        upperViolation.evaluate(tokenTable, converter, zeroIfNone)
    )

    override fun evaluate(
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = combine(
        lowerViolation.evaluate(results, tokenTable, converter, zeroIfNone),
        upperViolation.evaluate(results, tokenTable, converter, zeroIfNone)
    )

    override fun evaluate(
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        converter: IntoValue<V>,
        zeroIfNone: Boolean
    ): V? = combine(
        lowerViolation.evaluate(values, tokenTable, converter, zeroIfNone),
        upperViolation.evaluate(values, tokenTable, converter, zeroIfNone)
    )

    private fun combine(lowerValue: V?, upperValue: V?): V? =
        if (lowerValue == null || upperValue == null) null else lowerValue + upperValue

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val first = lowerViolation.registerAuxiliaryTokens(tokens)
        return if (first is Ok) upperViolation.registerAuxiliaryTokens(tokens) else first
    }

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val first = lowerViolation.registerConstraints(model)
        return if (first is Ok) upperViolation.registerConstraints(model) else first
    }
}
