@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelF64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.plus
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.multiarray.Shape1

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
class FirstFunction<T : Field<T>>(
    val polynomials: List<LinearPolynomial<T>>,
    val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    private val n: Int get() = polynomials.size

    // BinaryzationFunction for each polynomial
    private val binaryFunctions: List<BinaryzationFunction<Flt64>> by lazy {
        polynomials.mapIndexed { i, poly ->
            BinaryzationFunction(
                input = poly.asFlt64Poly(),
                threshold = epsilon,
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
    val result: LinearPolynomial<T> by lazy {
        // sum(i * y[i])
        val indexedMonos = _yVars.items.mapIndexed { i, yi ->
            LinearMonomial(Flt64(i.toDouble()) as T, yi)
        }
        // n * (1 - sum(y[i])) = n - n * sum(y[i])
        val nVal = Flt64(n.toDouble()) as T
        val nSumMonos = _yVars.items.map { LinearMonomial(-nVal, it) }
        val indexedPlus = indexedMonos + nSumMonos
        LinearPolynomial(indexedPlus, nVal)
    }

    override fun evaluate(values: Map<Symbol, T>): T? {
        for ((i, poly) in polynomials.withIndex()) {
            val value = poly.evaluate(values) ?: return null
            if (value.asFlt64().toDouble() > epsilon.toDouble()) {
                @Suppress("UNCHECKED_CAST")
                return Flt64(i.toDouble()) as T
            }
        }
        @Suppress("UNCHECKED_CAST")
        return Flt64(n.toDouble()) as T
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        // Register own auxiliary tokens (_yVars); binaryFunctions register their own via register()
        return tokens.add(_yVars.items)
    }

    override fun register(model: AbstractLinearMetaModelF64): Try {
        // Register all binary functions (they register their own auxiliary tokens + constraints)
        for (binFunc in binaryFunctions) {
            when (val r = binFunc.register(model)) {
                is Ok -> {}
                is Failed -> return Failed(r.error)
                is Fatal -> return Fatal(r.errors)
            }
        }

        // Register y variables via registerAuxiliaryTokens
        when (val r = registerAuxiliaryTokens(model)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        val allConstraints = mutableListOf<MathLinearInequality>()

        for (i in polynomials.indices) {
            val binResult = binaryFunctions[i].resultVar
            val yi = _yVars[i]

            // y[i] <= bin[i]
            allConstraints += MathLinearInequality(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, yi)), Flt64.zero),
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, binResult)), Flt64.zero),
                Comparison.LE, "${name}_ub1_$i"
            )

            if (i == 0) {
                // y[0] >= bin[0]
                allConstraints += MathLinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, yi)), Flt64.zero),
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, binResult)), Flt64.zero),
                    Comparison.GE, "${name}_lb_0"
                )
            } else {
                // y[i] >= bin[i] - sum(y[0]..y[i-1])
                // => y[i] + sum(y[0]..y[i-1]) >= bin[i]
                val prevYMonos = (0 until i).map { j -> LinearMonomial(Flt64.one, _yVars[j]) }
                val lhsMonos = listOf(LinearMonomial(Flt64.one, yi)) + prevYMonos
                allConstraints += MathLinearInequality(
                    LinearPolynomial(lhsMonos, Flt64.zero),
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, binResult)), Flt64.zero),
                    Comparison.GE, "${name}_lb_$i"
                )

                // y[i] <= y[i-1] (monotonicity)
                allConstraints += MathLinearInequality(
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, yi)), Flt64.zero),
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, _yVars[i - 1])), Flt64.zero),
                    Comparison.LE, "${name}_y_$i"
                )
            }
        }

        return addConstraints(model, allConstraints) ?: ok
    }

    companion object {
        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): FirstFunction<Flt64> = FirstFunction(polynomials, epsilon, name, displayName)
    }
}
