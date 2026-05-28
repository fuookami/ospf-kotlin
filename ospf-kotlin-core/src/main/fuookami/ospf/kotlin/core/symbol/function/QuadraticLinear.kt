@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 二次线性函数符号 / Quadratic linear function symbol
 *
 * 提供 [QuadraticLinearFunction]，将二次多项式包装为二次中间符号。
 *
 * Provides [QuadraticLinearFunction] for wrapping a quadratic polynomial as a quadratic intermediate symbol.
 */

/**
 * Quadratic linear function: wraps a QuadraticPolynomial as a quadratic intermediate symbol.
 * If the polynomial is purely linear, no helper variable or constraint is needed.
 * If it contains quadratic terms, creates a helper variable y with constraint y = polynomial.
 */
class QuadraticLinearFunction<V>(
    private val _polynomial: QuadraticPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {

    private val isLinear: Boolean by lazy {
        _polynomial.monomials.none { it.isQuadratic }
    }

    private val y: AbstractVariableItem<*, *>? by lazy {
        if (!isLinear) URealVar("${name}_y") else null
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = if (isLinear) Linear else Quadratic
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = category

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (m in _polynomial.monomials) {
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) dep.flush(force)
    }

    private fun evaluateSymbol(
        symbol: Symbol,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable.find(symbol)?.result ?: if (zeroIfNone) converter.zero else null
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol).evaluate(tokenTable, converter, zeroIfNone)
            else -> if (zeroIfNone) converter.zero else null
        }
    }

    private fun evaluateSymbol(
        symbol: Symbol,
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) converter.zero else null
            }
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol).evaluate(results, tokenTable, converter, zeroIfNone)
            else -> if (zeroIfNone) converter.zero else null
        }
    }

    private fun evaluateSymbol(
        symbol: Symbol,
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean
    ): V? {
        return values[symbol] ?: when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable?.find(symbol)?.result
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol).evaluate(values, tokenTable, converter, zeroIfNone)
            else -> null
        } ?: if (zeroIfNone) converter.zero else null
    }

    private fun evaluateQuadratic(
        poly: QuadraticPolynomial<V>,
        resolve: (Symbol) -> V?
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbol1Value = resolve(monomial.symbol1) ?: return null
            var termValue = monomial.coefficient * symbol1Value
            if (monomial.symbol2 != null) {
                val symbol2Value = resolve(monomial.symbol2!!) ?: return null
                termValue *= symbol2Value
            }
            value += termValue
        }
        return value
    }

    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (typedValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(typedValues, tokenTable, converter, false)
        }
    }

    override val polynomial: QuadraticPolynomial<V>
        get() = _polynomial

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? = null

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(values, tokenTable, converter, false)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateQuadratic(_polynomial) { symbol ->
            evaluateSymbol(symbol, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateQuadratic(_polynomial) { symbol ->
            evaluateSymbol(symbol, results, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateQuadratic(_polynomial) { symbol ->
            evaluateSymbol(symbol, values, tokenTable, zeroIfNone)
        }
    }
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedValues = SolverBoundaryCasts.mapValues(values, converter)
        return evaluate(typedValues, tokenTable, converter, zeroIfNone)
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register helper variable y with the token collection (only if quadratic).
     */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        if (!isLinear && y != null) {
            return when (val result = tokens.add(listOf(y!!))) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * Register the quadratic equality constraint y = polynomial (only if quadratic).
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        if (!isLinear && y != null) {
            val yMon = QuadraticMonomial.linear(converter.one, y!!)
            val lhs = QuadraticPolynomial(
                listOf(yMon) + _polynomial.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                -_polynomial.constant
            )
            val rhs = QuadraticPolynomial<V>(emptyList(), converter.zero)
            val constraint = QuadraticInequalityOf(lhs, rhs, Comparison.EQ, name)
            return when (val result = model.addConstraint(relation = constraint, name = name)) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticLinearFunction(polynomial, converter, name, displayName)
    }
}
