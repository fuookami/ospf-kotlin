@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 松弛区间函数符号 / Slack range function symbol
 *
 * 提供 [SlackRangeFunction]，为区间约束引入正/负松弛变量。
 *
 * Provides [SlackRangeFunction] for introducing positive/negative slack variables for range constraints.
 */

/**
 * Slack range function: bounds x within [lb, ub] using slack variables.
 *
 * Original semantics: polyX = x + neg - pos, with constraints polyX leq ub / geq lb.
 * neg = lower slack (x below lb), pos = upper slack (x above ub).
 *
 * @param x the expression to bound
 * @param lb lower bound polynomial
 * @param ub upper bound polynomial
 * @param type variable type kind (UInteger or UContinuous)
 * @param constraint whether to add polyX leq ub / geq lb constraints
 * @param converter value type converter
 */
class SlackRangeFunction<V>(
    val x: LinearPolynomial<V>,
    val lb: LinearPolynomial<V>,
    val ub: LinearPolynomial<V>,
    val type: VariableTypeKind = UContinuous,
    val constraint: Boolean = true,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    val negVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_neg") else URealVar("${name}_neg")
    }
    val posVar: AbstractVariableItem<*, *> by lazy {
        if (type.isIntegerType) UIntVar("${name}_pos") else URealVar("${name}_pos")
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(negVar, posVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, posVar)), converter.zero)

    val neg: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, negVar)), converter.zero)
    }
    val pos: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, posVar)), converter.zero)
    }

    val polyX: LinearPolynomial<V> by lazy {
        val unit = converter.one
        LinearPolynomial(
            x.monomials + LinearMonomial(unit, negVar) + LinearMonomial(-unit, posVar),
            x.constant
        )
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val lbValue = lb.evaluateWith(values) ?: return null
        val ubValue = ub.evaluateWith(values) ?: return null
        return if (xValue ls lbValue) {
            lbValue - xValue
        } else if (xValue gr ubValue) {
            xValue - ubValue
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        if (constraint) {
            val constraints = mutableListOf<LinearInequality<V>>()
            constraints += LinearInequality(polyX, ub, Comparison.LE, "${name}_ub")
            constraints += LinearInequality(polyX, lb, Comparison.GE, "${name}_lb")
            return addConstraints(model, constraints) ?: ok
        }
        return ok
    }

    companion object {
        /** V-generic factory with lb/ub polynomials. */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SlackRangeFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SlackRangeFunction(x, lb, ub, type, constraint, converter, name, displayName)

        /** V-generic factory with LinearIntermediateSymbol and lb/ub polynomials. */
        @JvmStatic
        fun <V> fromLinearIntermediateSymbol(
            x: LinearIntermediateSymbol<V>,
            lb: LinearPolynomial<V>,
            ub: LinearPolynomial<V>,
            type: VariableTypeKind = UContinuous,
            constraint: Boolean = true,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            SlackRangeFunction(x.toLinearPolynomial(), lb, ub, type, constraint, converter, name, displayName),
            converter = converter
        )

    }
}
