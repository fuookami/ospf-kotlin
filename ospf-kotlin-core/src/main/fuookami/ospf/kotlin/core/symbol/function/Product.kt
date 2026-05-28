/**
 * 乘积函数符号 / Product function symbol
 *
 * 提供 [ProductFunction]，实现两个线性多项式乘积 y = left * right 的二次建模。
 *
 * Provides [ProductFunction] for quadratic modeling of the product of two linear polynomials y = left * right.
 */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*

private typealias ProductIntermediate<V> = IntermediateSymbol<out V>

/**
 * Product of two linear polynomials: y = left * right.
 *
 * @param V value type for the polynomial coefficients.
 */
class ProductFunction<V>(
    val left: LinearPolynomial<V>,
    val right: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String = "product",
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    internal var _group: AbstractSymbolCombination<out Shape>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = Quadratic
    override val parent: ProductIntermediate<V>? = null
    override val operationCategory: Category = Quadratic

    override val dependencies: Set<ProductIntermediate<V>>
        get() {
            val deps = mutableSetOf<ProductIntermediate<V>>()
            for (m in left.monomials) {
                val s = m.symbol
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(s)?.let { deps.add(it) }
            }
            for (m in right.monomials) {
                val s = m.symbol
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(s)?.let { deps.add(it) }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) {
            dep.flush(force)
        }
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

    private fun evaluateLinear(
        poly: LinearPolynomial<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, zeroIfNone) ?: return null
            value += monomial.coefficient * symbolValue
        }
        return value
    }

    private fun evaluateLinearFromResults(
        poly: LinearPolynomial<V>,
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, results, tokenTable, zeroIfNone) ?: return null
            value += monomial.coefficient * symbolValue
        }
        return value
    }

    private fun evaluateLinearFromValues(
        poly: LinearPolynomial<V>,
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, zeroIfNone) ?: return null
            value += monomial.coefficient * symbolValue
        }
        return value
    }

    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        val leftValue = if (typedValues.isNullOrEmpty()) {
            evaluateLinear(left, tokenTable, false)
        } else {
            evaluateLinearFromValues(left, typedValues, tokenTable, false)
        } ?: return null

        val rightValue = if (typedValues.isNullOrEmpty()) {
            evaluateLinear(right, tokenTable, false)
        } else {
            evaluateLinearFromValues(right, typedValues, tokenTable, false)
        } ?: return null

        return leftValue * rightValue
    }

    /**
     * Expand left * right into a V-typed quadratic polynomial.
     */
    private fun expandedQuadraticPoly(): QuadraticPolynomial<V> {
        val leftC = left
        val rightC = right
        val leftConst = leftC.constant
        val rightConst = rightC.constant

        val monomials = mutableListOf<QuadraticMonomial<V>>()

        for (lm in leftC.monomials) {
            for (rm in rightC.monomials) {
                monomials.add(QuadraticMonomial(
                    lm.coefficient * rm.coefficient,
                    lm.symbol,
                    rm.symbol
                ))
            }
        }

        for (lm in leftC.monomials) {
            monomials.add(QuadraticMonomial.linear(
                lm.coefficient * rightConst,
                lm.symbol
            ))
        }
        for (rm in rightC.monomials) {
            monomials.add(QuadraticMonomial.linear(
                rm.coefficient * leftConst,
                rm.symbol
            ))
        }

        return QuadraticPolynomial(monomials, leftConst * rightConst)
    }

    override val polynomial: QuadraticPolynomial<V>
        get() = expandedQuadraticPoly()

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val leftValue = if (values.isNullOrEmpty()) {
            evaluateLinear(left, tokenTable, false)
        } else {
            evaluateLinearFromValues(left, values, tokenTable, false)
        } ?: return null

        val rightValue = if (values.isNullOrEmpty()) {
            evaluateLinear(right, tokenTable, false)
        } else {
            evaluateLinearFromValues(right, values, tokenTable, false)
        } ?: return null

        return leftValue * rightValue
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val leftValue = evaluateLinear(left, tokenTable, zeroIfNone) ?: return null
        val rightValue = evaluateLinear(right, tokenTable, zeroIfNone) ?: return null
        return leftValue * rightValue
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val leftValue = evaluateLinearFromResults(left, results, tokenTable, zeroIfNone) ?: return null
        val rightValue = evaluateLinearFromResults(right, results, tokenTable, zeroIfNone) ?: return null
        return leftValue * rightValue
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val leftValue = evaluateLinearFromValues(left, values, tokenTable, zeroIfNone) ?: return null
        val rightValue = evaluateLinearFromValues(right, values, tokenTable, zeroIfNone) ?: return null
        return leftValue * rightValue
    }
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedValues = SolverBoundaryCasts.mapValues(values, converter)
        return evaluate(typedValues, tokenTable, converter, zeroIfNone)
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            "product($left, $right)"
        } else {
            displayName ?: name
        }
    }

    override fun hashCode(): Int = identifier.toInt() * 31 + index
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        @Suppress("UNCHECKED_CAST")
        other as ProductFunction<V>
        return identifier == other.identifier && index == other.index && name == other.name
    }
    override fun toString(): String = displayName ?: name

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val poly = polynomial
        val rhs = QuadraticPolynomial<V>(constant = converter.zero)
        val inequality = QuadraticInequalityOf(poly, rhs, Comparison.EQ, "${name}_eq")
        return addQuadraticConstraints(model, listOf(inequality)) ?: ok
    }

    companion object {
        operator fun <V> invoke(
            left: LinearPolynomial<V>,
            right: LinearPolynomial<V>,
            converter: IntoValue<V>,
            name: String = "product",
            displayName: String? = null
        ): ProductFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            ProductFunction(left, right, converter, name, displayName)
    }
}
