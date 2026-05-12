@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Quadratic min function: y = min(p1, p2, ..., pn).
 * Uses Big-M formulation with binary selection variables for exact min,
 * or simple lower-bound constraints for relaxed min.
 *
 * @param polynomials list of quadratic polynomials to take the min of
 * @param exact if true, uses binary variables for exact min; if false, only enforces y <= pi
 * @param bigM Big-M constant for exact formulation (default 1e6)
 */
class QuadraticMinFunction<V>(
    val polynomials: List<QuadraticPolynomial<V>>,
    val exact: Boolean = true,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_min")
    val binVars: List<AbstractVariableItem<*, *>> by lazy {
        if (exact) polynomials.indices.map { i -> BinVar("${name}_u_$i") } else emptyList()
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = Linear

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (poly in polynomials) {
                for (m in poly.monomials) {
                    SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                    SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
                }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRangeV()

    override fun flush(force: Boolean) {
        for (dep in dependencies) dep.flush(force)
    }

    private fun evaluateSymbolV(
        symbol: Symbol,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable.find(symbol)?.result ?: if (zeroIfNone) converter.zero else null
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediateV<V>(symbol).evaluate(tokenTable, converter, zeroIfNone)
            else -> if (zeroIfNone) converter.zero else null
        }
    }

    private fun evaluateSymbolV(
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
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediateV<V>(symbol).evaluate(results, tokenTable, converter, zeroIfNone)
            else -> if (zeroIfNone) converter.zero else null
        }
    }

    private fun evaluateSymbolV(
        symbol: Symbol,
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean
    ): V? {
        return values[symbol] ?: when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable?.find(symbol)?.result
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediateV<V>(symbol).evaluate(values, tokenTable, converter, zeroIfNone)
            else -> null
        } ?: if (zeroIfNone) converter.zero else null
    }

    private fun evaluateQuadraticV(
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

    private fun chooseMin(
        eval: (QuadraticPolynomial<V>) -> V?
    ): V? {
        var minValue: V? = null
        for (poly in polynomials) {
            val value = eval(poly) ?: return null
            if (minValue == null || value ls minValue) {
                minValue = value
            }
        }
        return minValue
    }

    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValuesToV(it, converter) }
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
        return chooseMin { poly ->
            evaluateQuadraticV(poly) { symbol ->
                evaluateSymbolV(symbol, tokenTable, zeroIfNone)
            }
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return chooseMin { poly ->
            evaluateQuadraticV(poly) { symbol ->
                evaluateSymbolV(symbol, results, tokenTable, zeroIfNone)
            }
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return chooseMin { poly ->
            evaluateQuadraticV(poly) { symbol ->
                evaluateSymbolV(symbol, values, tokenTable, zeroIfNone)
            }
        }
    }
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedValues = SolverBoundaryCasts.mapValuesToV(values, converter)
        return evaluate(typedValues, tokenTable, converter, zeroIfNone)
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register helper variables (resultVar, binVars) with the token collection.
     */
    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        val allVars = mutableListOf<AbstractVariableItem<*, *>>(resultVar)
        allVars.addAll(binVars)
        return when (val result = tokens.add(allVars)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * Register min constraints (y <= pi, and if exact: y >= pi - M*(1-ui), sum(ui)=1).
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val m = bigM
        val one = converter.one
        val zero = converter.zero
        val resultMon = QuadraticMonomial.linear(one, resultVar)
        val constraints = mutableListOf<QuadraticInequalityOf<V>>()

        // y <= pi for each polynomial
        for ((i, poly) in polynomials.withIndex()) {
            val negatedMonos = poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
            val lhs = QuadraticPolynomial(negatedMonos + listOf(resultMon), -poly.constant)
            val rhs = QuadraticPolynomial<V>(emptyList(), zero)
            constraints += QuadraticInequalityOf(lhs, rhs, Comparison.LE, "${name}_lb_$i")
        }

        if (exact) {
            // y >= pi - M*(1 - ui) for each polynomial
            for ((i, poly) in polynomials.withIndex()) {
                val uVar = binVars[i]
                val negatedPolyMonos = poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
                val bigMTerm = QuadraticMonomial.linear(m, uVar)
                val lhs = QuadraticPolynomial(negatedPolyMonos + listOf(resultMon, bigMTerm), -poly.constant)
                val rhs = QuadraticPolynomial<V>(emptyList(), m)
                constraints += QuadraticInequalityOf(lhs, rhs, Comparison.GE, "${name}_ub_$i")
            }

            // sum(ui) = 1
            val sumMonos = binVars.map { QuadraticMonomial.linear(one, it) }
            val sumLhs = QuadraticPolynomial(sumMonos, zero)
            val sumRhs = QuadraticPolynomial<V>(emptyList(), one)
            constraints += QuadraticInequalityOf(sumLhs, sumRhs, Comparison.EQ, "${name}_u")
        }

        return addQuadraticConstraints(model, constraints) ?: ok
    }

    companion object {
        operator fun <V> invoke(
            polynomials: List<QuadraticPolynomial<V>>,
            exact: Boolean = true,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticMinFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticMinFunction(polynomials, exact, bigM, converter, name, displayName)

        operator fun invoke(
            polynomials: List<QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>>,
            exact: Boolean = true,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): QuadraticMinFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticMinFunction(polynomials, exact, bigM, flt64Converter, name, displayName)
    }
}
