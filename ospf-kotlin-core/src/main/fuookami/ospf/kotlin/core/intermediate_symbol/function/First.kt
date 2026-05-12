@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

/**
 * FirstFunction - Returns the index of the first polynomial in the list that evaluates to > 0.
 *
 * For each polynomial, a BinaryzationFunction creates binary bin[i] (1 if polynomial[i] > 0).
 * Output binary array y[0..n-1] where y[i]=1 means "polynomial i is the first nonzero".
 *
 * Constraints:
 * - y[i] <= bin[i] for each i (can only be first if it's nonzero)
 * - y[0] >= bin[0] (if first polynomial is nonzero, it's the first)
 * - y[i] >= bin[i] - sum(y[0]..y[i-1]) for i > 0
 * - y[i] <= y[i-1] for i > 0 (monotonicity)
 *
 * Output: result = sum(i * y[i]) + n * (1 - sum(y[i])) = index of first nonzero, or n if none
 */
class FirstFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    val epsilon: Flt64 = Flt64(1e-6),
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val n: Int get() = polynomials.size

    // BinaryzationFunction for each polynomial
    private val binaryFunctions: List<BinaryzationFunction<V>> by lazy {
        polynomials.mapIndexed { i, poly ->
            BinaryzationFunction(
                polynomial = poly,
                converter = converter,
                name = "${name}_bin_$i"
            )
        }
    }

    // Output binary array y[i] = 1 means polynomial i is the first nonzero
    private val _yVars: BinVariable1 by lazy {
        BinVariable1("${name}_first", Shape1(n))
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(*binaryFunctions.flatMap { it.helperVariables }.toTypedArray()) + _yVars.items

    /**
     * Result polynomial: sum(i * y[i]) + n * (1 - sum(y[i]))
     * Returns the index of the first nonzero polynomial, or n if none.
     */
    val result: LinearPolynomial<V> by lazy {
        // sum(i * y[i])
        val one = converter.one
        val indexedMonos = _yVars.items.mapIndexed { i, yi ->
            LinearMonomial(repeatAdd(one, i), yi)
        }
        // n * (1 - sum(y[i])) = n - n * sum(y[i])
        val nVal = repeatAdd(one, n)
        val nSumMonos = _yVars.items.map { LinearMonomial(-nVal, it) }
        val indexedPlus = indexedMonos + nSumMonos
        LinearPolynomial(indexedPlus, nVal)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val one = converter.one
        val epsilonValue = converter.intoValue(epsilon)
        for ((i, poly) in polynomials.withIndex()) {
            val value = poly.evaluateWith(values) ?: return null
            if (value gr epsilonValue) {
                return repeatAdd(one, i)
            }
        }
        return repeatAdd(one, n)
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        // Register all binary function constraints (tokens already registered in registerAuxiliaryTokens)
        for (binFunc in binaryFunctions) {
            when (val r = binFunc.registerConstraints(model)) {
                is Ok -> {}
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
        }

        val zero = converter.zero
        val one = converter.one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        for (i in polynomials.indices) {
            val binResult = binaryFunctions[i].resultVar
            val yi = _yVars[i]

            // y[i] <= bin[i]
            allConstraints += LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(one, yi)), zero),
                LinearPolynomial(listOf(LinearMonomial(one, binResult)), zero),
                Comparison.LE, "${name}_ub1_$i"
            )

            if (i == 0) {
                // y[0] >= bin[0]
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, yi)), zero),
                    LinearPolynomial(listOf(LinearMonomial(one, binResult)), zero),
                    Comparison.GE, "${name}_lb_0"
                )
            } else {
                // y[i] >= bin[i] - sum(y[0]..y[i-1])
                // => y[i] + sum(y[0]..y[i-1]) >= bin[i]
                val prevYMonos = (0 until i).map { j -> LinearMonomial(one, _yVars[j]) }
                val lhsMonos = listOf(LinearMonomial(one, yi)) + prevYMonos
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos, zero),
                    LinearPolynomial(listOf(LinearMonomial(one, binResult)), zero),
                    Comparison.GE, "${name}_lb_$i"
                )

                // y[i] <= y[i-1] (monotonicity)
                allConstraints += LinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(one, yi)), zero),
                    LinearPolynomial(listOf(LinearMonomial(one, _yVars[i - 1])), zero),
                    Comparison.LE, "${name}_y_$i"
                )
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): FirstFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            FirstFunction(polynomials, epsilon, converter, name, displayName)
    }
}
