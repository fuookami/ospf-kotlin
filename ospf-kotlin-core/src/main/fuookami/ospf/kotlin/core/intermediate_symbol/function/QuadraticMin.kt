@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

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
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

private val flt64Converter = object : IntoValue<Flt64> {
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
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_min")
    val binVars: List<AbstractVariableItem<*, *>> by lazy {
        if (exact) polynomials.indices.map { i -> BinVar("${name}_u_$i") } else emptyList()
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<*>? = null
    override val operationCategory: Category get() = Linear

    override val dependencies: Set<IntermediateSymbol<*>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<*>>()
            for (poly in polynomials) {
                for (m in poly.monomials) {
                    if (m.symbol1 is IntermediateSymbol<*>) deps.add(m.symbol1 as IntermediateSymbol<*>)
                    if (m.symbol2 != null && m.symbol2 is IntermediateSymbol<*>) deps.add(m.symbol2 as IntermediateSymbol<*>)
                }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) dep.flush(force)
    }

    override fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null

    override fun toMathQuadraticInequality(): QuadraticInequality {
        return QuadraticInequality(
            QuadraticPolynomial(emptyList(), Flt64.zero),
            QuadraticPolynomial(emptyList(), Flt64.one),
            Comparison.EQ
        )
    }

    override val flattenedMonomials: QuadraticFlattenData<Flt64>
        get() = QuadraticFlattenData<Flt64>(emptyList(), Flt64.zero)

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, resultVar)), converter.zero)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    override fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? = null

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register helper variables (resultVar, binVars) with the token collection.
     */
    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<Flt64>): Try {
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
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<Flt64>): Try {
        val mF = converter.fromValue(bigM)
        val resultMon = QuadraticMonomial.linear(Flt64.one, resultVar)

        // y <= pi for each polynomial
        for ((i, poly) in polynomials.withIndex()) {
            val flt64Poly = poly.asFlt64QuadraticPoly(converter)
            val negatedMonos = flt64Poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
            val lhs = QuadraticPolynomial(negatedMonos + listOf(resultMon), -flt64Poly.constant)
            val rhs = QuadraticPolynomial(emptyList(), Flt64.zero)
            val constraint = QuadraticInequality(lhs, rhs, Comparison.LE, "${name}_lb_$i")
            when (val r = model.addConstraint(relation = constraint, name = constraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
        }

        if (exact) {
            // y >= pi - M*(1 - ui) for each polynomial
            for ((i, poly) in polynomials.withIndex()) {
                val uVar = binVars[i]
                val flt64Poly = poly.asFlt64QuadraticPoly(converter)
                val negatedPolyMonos = flt64Poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
                val bigMTerm = QuadraticMonomial.linear(mF, uVar)
                val lhs = QuadraticPolynomial(negatedPolyMonos + listOf(resultMon) + listOf(bigMTerm), -flt64Poly.constant)
                val rhs = QuadraticPolynomial(emptyList(), mF)
                val constraint = QuadraticInequality(lhs, rhs, Comparison.GE, "${name}_ub_$i")
                when (val r = model.addConstraint(relation = constraint, name = constraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(r.error)
                    is Fatal -> return Fatal(r.errors)
                }
            }

            // sum(ui) = 1
            val sumMonos = binVars.map { QuadraticMonomial.linear(Flt64.one, it) }
            val sumLhs = QuadraticPolynomial(sumMonos, Flt64.zero)
            val sumRhs = QuadraticPolynomial(emptyList(), Flt64.one)
            val sumConstraint = QuadraticInequality(sumLhs, sumRhs, Comparison.EQ, "${name}_u")
            when (val r = model.addConstraint(relation = sumConstraint, name = sumConstraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
        }

        return ok
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
            polynomials: List<QuadraticPolynomial<Flt64>>,
            exact: Boolean = true,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): QuadraticMinFunction<Flt64> = QuadraticMinFunction(polynomials, exact, bigM, flt64Converter, name, displayName)
    }
}