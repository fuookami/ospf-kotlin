@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * OneOf function: exactly one of the input polynomials must be nonzero.
 *
 * Result:
 * - y = 1 if exactly one of the input polynomials is nonzero
 * - y = 0 otherwise
 *
 * Uses nonzero indicators with XOR-like linking constraints.
 *
 * @param polynomials the list of input linear polynomials
 * @param bigM Big-M bound (default 1e6)
 * @param tolerance zero tolerance (default 1e-6)
 * @param strictBoundary strict boundary value (default 0.5)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class OneOfFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    private val converter: IntoValue<V>,
    override var name: String = "oneof",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "OneOfFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_oneof")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_oneof_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_oneof_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (converter.fromValue(v).toDouble() != 0.0) count++
        }
        return if (count == 1) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mD = bigM
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators for each polynomial
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i], indicatorVars[i], sideVars[i], mD, tolerance, strictBoundary, converter, "${name}_oneof_nz_${i}")
        }

        // Exactly one indicator must be 1: sum(indicators) = 1
        val indMonos = indicatorVars.map { LinearMonomial(Flt64.one, it) }
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(indMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_oneof_exactly_one")

        // result = 1 (since exactly one indicator must be 1)
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(Flt64.one, resultVar)), Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one), Comparison.EQ, "${name}_oneof_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String = "oneof",
            displayName: String? = null
        ): OneOfFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            OneOfFunction(polynomials, bigM, converter = converter, name = name, displayName = displayName)

        operator fun invoke(
            polynomials: List<LinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String = "oneof",
            displayName: String? = null
        ): OneOfFunction<Flt64> = OneOfFunction(polynomials, bigM, converter = IntoValue.Flt64, name = name, displayName = displayName)

        @JvmStatic
        @JvmName("fromLinearPolynomials")
        fun fromLinearPolynomials(
            polynomials: List<fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            OneOfFunction<Flt64>(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                bigM = bigM,
                converter = IntoValue.Flt64,
       name = name,
                displayName = displayName
            ),
            converter = IntoValue.Flt64
        
        )
    }
}