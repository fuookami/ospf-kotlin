package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.AbstractSymbolCombination
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.multiarray.Shape
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

private typealias ProductIntermediate<V> = IntermediateSymbol<out V>

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

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
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRangeV()

    /** Flt64 view of left polynomial for solver-boundary operations. */
    private val leftFlt64: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> by lazy { left.asFlt64Poly(converter) }
    /** Flt64 view of right polynomial for solver-boundary operations. */
    private val rightFlt64: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> by lazy { right.asFlt64Poly(converter) }

    override fun flush(force: Boolean) {
        for (dep in dependencies) {
            dep.flush(force)
        }
    }

    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64(tokenTable)
        val leftValue = if (values.isNullOrEmpty()) {
            evaluateLinear(leftFlt64, tokenList, false)
        } else {
            evaluateLinearFromValues(leftFlt64, values, tokenList, false)
        } ?: return null

        val rightValue = if (values.isNullOrEmpty()) {
            evaluateLinear(rightFlt64, tokenList, false)
        } else {
            evaluateLinearFromValues(rightFlt64, values, tokenList, false)
        } ?: return null

        return converter.intoValue(leftValue * rightValue)
    }

    /**
     * Expand left * right into a Flt64 quadratic polynomial (solver boundary).
     */
    private fun expandedQuadraticPolyFlt64(): QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        val leftC = leftFlt64
        val rightC = rightFlt64
        val leftConst = leftC.constant
        val rightConst = rightC.constant

        val monomials = mutableListOf<QuadraticMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

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

    internal fun toMathQuadraticInequality(): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        return QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(expandedQuadraticPolyFlt64(), QuadraticPolynomial(emptyList(), Flt64.one), Comparison.EQ)
    }

    internal val flattenedMonomials: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        get() {
            val poly = expandedQuadraticPolyFlt64()
            return QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(poly.monomials, poly.constant)
        }

    override val polynomial: QuadraticPolynomial<V>
        get() = expandedQuadraticPolyFlt64().asVQuadraticPoly(converter)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
        val leftVal = evaluateLinear(leftFlt64, tokenList, zeroIfNone) ?: return null
        val rightVal = evaluateLinear(rightFlt64, tokenList, zeroIfNone) ?: return null
        return leftVal * rightVal
    }

    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? {
        val leftVal = evaluateLinearFromResults(leftFlt64, results, tokenList, zeroIfNone) ?: return null
        val rightVal = evaluateLinearFromResults(rightFlt64, results, tokenList, zeroIfNone) ?: return null
        return leftVal * rightVal
    }

    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? {
        val leftVal = evaluateLinearFromValues(leftFlt64, values, tokenList, zeroIfNone) ?: return null
        val rightVal = evaluateLinearFromValues(rightFlt64, values, tokenList, zeroIfNone) ?: return null
        return leftVal * rightVal
    }
    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val flt64Values = values?.mapValues { converter.fromValue(it.value) }
        return prepareSolver(flt64Values, tokenTable, converter)
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64(tokenTable)
        val result = evaluate(tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64(tokenTable)
        val flt64Results = results.map { converter.fromValue(it) }
        val result = evaluate(flt64Results, tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64OrNull(tokenTable)
        val flt64Values = values.mapValues { converter.fromValue(it.value) }
        val result = evaluate(flt64Values, tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
    }
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64(tokenTable)
        val result = evaluate(results, tokenList, zeroIfNone) ?: return null
        return converter.intoValue(result)
    }
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val tokenList = SolverBoundaryCasts.tokenListAsFlt64OrNull(tokenTable)
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
        if (other == null || other::class != this::class) return false
        @Suppress("UNCHECKED_CAST")
        other as ProductFunction<V>
        return identifier == other.identifier && index == other.index && name == other.name
    }
    override fun toString(): String = displayName ?: name

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try = ok

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val poly = polynomial
        val rhs = QuadraticPolynomial<V>(constant = converter.zero)
        val inequality = QuadraticInequalityOf(poly, rhs, Comparison.EQ)
        return addQuadraticConstraints(model, listOf(inequality)) ?: ok
    }

    companion object {
        private fun evaluateLinear(
            poly: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            zeroIfNone: Boolean
        ): Flt64? {
            var value = poly.constant
            for (monomial in poly.monomials) {
                val symbol = monomial.symbol
                val symbolValue = when (symbol) {
                    is AbstractVariableItem<*, *> -> {
                        tokenList.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else return null
                    }
                    is LinearExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    is QuadraticExpressionSymbol<*> -> symbol.evaluate(tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    else -> return null
                }
                value += monomial.coefficient * symbolValue
            }
            return value
        }

        private fun evaluateLinearFromResults(
            poly: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
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
                    is LinearExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    is QuadraticExpressionSymbol<*> -> symbol.evaluate(results, tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    else -> return null
                }
                value += monomial.coefficient * symbolValue
            }
            return value
        }

        private fun evaluateLinearFromValues(
            poly: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            values: Map<Symbol, Flt64>,
            tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?,
            zeroIfNone: Boolean
        ): Flt64? {
            var value = poly.constant
            for (monomial in poly.monomials) {
                val symbol = monomial.symbol
                val symbolValue = when (symbol) {
                    is AbstractVariableItem<*, *> -> {
                        values[symbol] ?: tokenList?.find(symbol)?.result ?: if (zeroIfNone) Flt64.zero else return null
                    }
                    is LinearExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    is QuadraticExpressionSymbol<*> -> symbol.evaluate(values, tokenList, zeroIfNone) ?: if (zeroIfNone) Flt64.zero else return null
                    else -> values[symbol] ?: if (zeroIfNone) Flt64.zero else return null
                }
                value += monomial.coefficient * symbolValue
            }
            return value
        }

        // V-generic main factory
        operator fun <V> invoke(
            left: LinearPolynomial<V>,
            right: LinearPolynomial<V>,
            converter: IntoValue<V>,
            name: String = "product",
            displayName: String? = null
        ): ProductFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            ProductFunction(left, right, converter, name, displayName)

        // Flt64-specific convenience factory
        operator fun invoke(
            left: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            right: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            name: String = "product",
            displayName: String? = null
        ): ProductFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = ProductFunction(left, right, flt64Converter, name, displayName)
    }
}

