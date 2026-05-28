/**
 * 二次掩码区间函数符号 / Quadratic masking range function symbol
 *
 * 提供 [QuadraticMaskingRangeFunction]，实现当 z=1 时 y=poly，当 z=0 时 y 自由的二次约束建模。
 *
 * Provides [QuadraticMaskingRangeFunction] for quadratic constraint modeling where y=poly when z=1, and y is free when z=0.
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection

/**
 * Quadratic masking range: when z = 1, y is forced to equal polynomial;
 * when z = 0, y is free (within bounds).
 * Uses Big-M formulation: y <= polynomial + M*(1-z), y >= polynomial - M*(1-z).
 *
 * @param polynomial the quadratic polynomial to mask
 * @param z the binary control variable
 * @param bigM Big-M constant (default 1e6)
 */
class QuadraticMaskingRangeFunction<V>(
    val _polynomial: QuadraticPolynomial<V>,
    val z: AbstractVariableItem<*, *>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_y")

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = Linear

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (m in _polynomial.monomials) {
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
            }
            SolverBoundaryCasts.symbolAsIntermediateStar<V>(z)?.let { deps.add(it) }
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

    private fun evaluateMasking(
        resolve: (Symbol) -> V?
    ): V? {
        val zValue = resolve(z) ?: return converter.zero
        if (zValue eq converter.zero) {
            return converter.zero
        }
        return evaluateQuadratic(_polynomial, resolve)
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
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, resultVar)), converter.zero)

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
        return evaluateMasking { symbol ->
            evaluateSymbol(symbol, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateMasking { symbol ->
            evaluateSymbol(symbol, results, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateMasking { symbol ->
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
     * Register helper variable (resultVar) with the token collection.
     */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(listOf(resultVar))) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * Register Big-M masking constraints.
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val m = bigM
        val resultMon = QuadraticMonomial.linear(converter.one, resultVar)
        val bigMMon = QuadraticMonomial.linear(m, z)

        val negatedPolyMonos = _polynomial.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }

        val constraints = mutableListOf<QuadraticInequalityOf<V>>()

        // Constraint 1: y - polynomial + M*z <= M
        val lhs1 = QuadraticPolynomial(listOf(resultMon) + negatedPolyMonos + listOf(bigMMon), -_polynomial.constant)
        val rhs1 = QuadraticPolynomial<V>(emptyList(), m)
        constraints += QuadraticInequalityOf(lhs1, rhs1, Comparison.LE, "${name}_upper")

        // Constraint 2: y - polynomial - M*z >= -M
        val negBigMMon = QuadraticMonomial.linear(-m, z)
        val lhs2 = QuadraticPolynomial(listOf(resultMon) + negatedPolyMonos + listOf(negBigMMon), -_polynomial.constant)
        val rhs2 = QuadraticPolynomial<V>(emptyList(), -m)
        constraints += QuadraticInequalityOf(lhs2, rhs2, Comparison.GE, "${name}_lower")

        return addQuadraticConstraints(model, constraints) ?: ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            z: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticMaskingRangeFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticMaskingRangeFunction(polynomial, z, bigM, converter, name, displayName)
    }
}
