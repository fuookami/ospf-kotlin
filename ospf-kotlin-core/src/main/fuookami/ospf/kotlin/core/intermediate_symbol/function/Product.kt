package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModelFlt64
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.AbstractSymbolCombination
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.token.AbstractTokenListFlt64
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial as MathQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial as MathMutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as MathQuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Product of two linear polynomials: y = left * right.
 * Aligns with Rust ProductFunction<V>.
 *
 * The symbol itself is quadratic and can be used in expression/evaluation
 * pipelines even when no linearized mechanism constraints are required.
 *
 * @param V value type for the polynomial coefficients. Internal kernel uses Flt64
 *          for solver compatibility; V-typed access is via IntoValue<V> conversion.
 */
class ProductFunction<V>(
    val left: MathLinearPolynomial<Flt64>,
    val right: MathLinearPolynomial<Flt64>,
    override var name: String = "${left}*${right}",
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.Ring<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    internal var _group: AbstractSymbolCombination<*>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = Quadratic
    override val parent: IntermediateSymbol<*>? = null
    override val operationCategory: Category = Quadratic

    override val dependencies: Set<IntermediateSymbol<*>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<*>>()
            for (m in left.monomials) {
                val s = m.symbol
                if (s is IntermediateSymbol<*>) deps.add(s)
            }
            for (m in right.monomials) {
                val s = m.symbol
                if (s is IntermediateSymbol<*>) deps.add(s)
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) {
            dep.flush(force)
        }
    }

    override fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        @Suppress("UNCHECKED_CAST")
        val tt = tokenTable as AbstractTokenTableFlt64
        val tokenList = tt.tokenList
        val leftValue = if (values.isNullOrEmpty()) {
            evaluateLinear(left, tokenList, false)
        } else {
            evaluateLinearFromValues(left, values, tokenList, false)
        } ?: return null

        val rightValue = if (values.isNullOrEmpty()) {
            evaluateLinear(right, tokenList, false)
        } else {
            evaluateLinearFromValues(right, values, tokenList, false)
        } ?: return null

        return converter.intoValue(leftValue * rightValue)
    }

    /**
     * Expand left * right into a quadratic polynomial.
     * Aligns with Rust ProductFunction::to_quadratic_polynomial().
     */
    override fun toMathQuadraticInequality(): MathQuadraticInequality {
        val leftConst = left.constant
        val rightConst = right.constant

        val monomials = mutableListOf<MathQuadraticMonomial<Flt64>>()

        // quadratic terms: (a_i x_i) * (b_j x_j)
        for (lm in left.monomials) {
            for (rm in right.monomials) {
                monomials.add(MathQuadraticMonomial(
                    lm.coefficient * rm.coefficient,
                    lm.symbol,
                    rm.symbol
                ))
            }
        }

        // linear terms from constant cross terms
        for (lm in left.monomials) {
            monomials.add(MathQuadraticMonomial.linear(
                lm.coefficient * rightConst,
                lm.symbol
            ))
        }
        for (rm in right.monomials) {
            monomials.add(MathQuadraticMonomial.linear(
                rm.coefficient * leftConst,
                rm.symbol
            ))
        }

        return MathQuadraticInequality(MathQuadraticPolynomial(monomials, leftConst * rightConst), MathQuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    override val flattenedMonomials: QuadraticFlattenDataFlt64
        get() {
            val poly = toMathQuadraticPolynomial()
            return QuadraticFlattenDataFlt64(poly.monomials, poly.constant)
        }

    @Suppress("UNCHECKED_CAST")
    override val polynomial: MathQuadraticPolynomial<V>
        get() = toMathQuadraticPolynomial() as MathQuadraticPolynomial<V>

    @Suppress("UNCHECKED_CAST")
    override fun asMutable(): MathMutableQuadraticPolynomial<V> = MathMutableQuadraticPolynomial(emptyList(), Flt64.zero) as MathMutableQuadraticPolynomial<V>

    override fun evaluate(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? {
        val leftVal = evaluateLinear(left, tokenList, zeroIfNone) ?: return null
        val rightVal = evaluateLinear(right, tokenList, zeroIfNone) ?: return null
        return leftVal * rightVal
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? {
        val leftVal = evaluateLinearFromResults(left, results, tokenList, zeroIfNone) ?: return null
        val rightVal = evaluateLinearFromResults(right, results, tokenList, zeroIfNone) ?: return null
        return leftVal * rightVal
    }

    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListFlt64?, zeroIfNone: Boolean): Flt64? {
        val leftVal = evaluateLinearFromValues(left, values, tokenList, zeroIfNone) ?: return null
        val rightVal = evaluateLinearFromValues(right, values, tokenList, zeroIfNone) ?: return null
        return leftVal * rightVal
    }

    @Suppress("UNCHECKED_CAST")
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tt = tokenTable as AbstractTokenTableFlt64
        val tokenList = tt.tokenList as AbstractTokenListFlt64
        val result = evaluate(tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
    }

    @Suppress("UNCHECKED_CAST")
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tt = tokenTable as AbstractTokenTableFlt64
        val tokenList = tt.tokenList as AbstractTokenListFlt64
        val result = evaluate(results, tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
    }

    @Suppress("UNCHECKED_CAST")
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tokenList = tokenTable?.let { (it as AbstractTokenTableFlt64).tokenList as AbstractTokenListFlt64 }
        val result = evaluate(values, tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
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
        if (other !is ProductFunction<*>) return false
        return identifier == other.identifier && index == other.index && name == other.name
    }
    override fun toString(): String = displayName ?: name

    /**
     * Register this product as a quadratic equality constraint: this = left * right.
     * Adds the constraint to the model via addConstraint.
     */
    fun register(model: AbstractQuadraticMechanismModelFlt64): Try {
        val poly = toMathQuadraticPolynomial()
        val rhs = MathQuadraticPolynomial<Flt64>(constant = Flt64.zero)
        val inequality = MathQuadraticInequality(poly, rhs, Comparison.EQ)
        return model.addConstraint(inequality, name = name, from = this to true)
    }

    companion object {
        private fun evaluateLinear(
            poly: MathLinearPolynomial<Flt64>,
            tokenList: AbstractTokenListFlt64,
            zeroIfNone: Boolean
        ): Flt64? {
            var value = poly.constant
            for (monomial in poly.monomials) {
                val symbol = monomial.symbol
                val symbolValue = when (symbol) {
                    is AbstractVariableItem<*, *> -> {
                        tokenList.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else return null
                    }
                    is IntermediateSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    else -> return null
                }
                value += monomial.coefficient * symbolValue
            }
            return value
        }

        private fun evaluateLinearFromResults(
            poly: MathLinearPolynomial<Flt64>,
            results: List<Flt64>,
            tokenList: AbstractTokenListFlt64,
            zeroIfNone: Boolean
        ): Flt64? {
            var value = poly.constant
            for (monomial in poly.monomials) {
                val symbol = monomial.symbol
                val symbolValue = when (symbol) {
                    is AbstractVariableItem<*, *> -> {
                        val index = tokenList.indexOf(symbol)
                        if (index != null && index >= 0 && index < results.size) results[index]
                        else if (zeroIfNone) Flt64.zero else return null
                    }
                    is IntermediateSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    else -> return null
                }
                value += monomial.coefficient * symbolValue
            }
            return value
        }

        private fun evaluateLinearFromValues(
            poly: MathLinearPolynomial<Flt64>,
            values: Map<Symbol, Flt64>,
            tokenList: AbstractTokenListFlt64?,
            zeroIfNone: Boolean
        ): Flt64? {
            var value = poly.constant
            for (monomial in poly.monomials) {
                val symbol = monomial.symbol
                val symbolValue = when (symbol) {
                    is AbstractVariableItem<*, *> -> {
                        values[symbol] ?: tokenList?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else return null
                    }
                    is IntermediateSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else return null
                }
                value += monomial.coefficient * symbolValue
            }
            return value
        }
    }
}

typealias ProductFunctionFlt64 = ProductFunction<Flt64>
