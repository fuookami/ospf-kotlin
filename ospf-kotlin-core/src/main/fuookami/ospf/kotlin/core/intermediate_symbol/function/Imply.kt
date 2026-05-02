@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModelFlt64
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
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
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Implication function: `if antecedent > 0 then consequent > 0`.
 *
 * Uses nonzero indicators for both antecedent and consequent,
 * with a linking constraint: indicator_antecedent <= indicator_consequent.
 *
 * @param antecedent the antecedent (condition) linear polynomial
 * @param consequent the consequent linear polynomial
 * @param bigM Big-M bound (default 1e6)
 * @param tolerance zero tolerance (default 1e-6)
 * @param strictBoundary strict boundary value (default 0.5)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class ImplyFunction<V>(
    val antecedent: LinearPolynomial<V>,
    val consequent: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "imply",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val antecedentIndicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_ant_nz")
    val antecedentSideVar: AbstractVariableItem<*, *> = BinVar("${name}_ant_side")
    val consequentIndicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_con_nz")
    val consequentSideVar: AbstractVariableItem<*, *> = BinVar("${name}_con_side")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(antecedentIndicatorVar, antecedentSideVar, consequentIndicatorVar, consequentSideVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val antValue = antecedent.evaluateWith(values) ?: return null
        val conValue = consequent.evaluateWith(values) ?: return null
        // Implication: if antecedent > 0, then consequent must be > 0
        // Returns 1 if the implication holds, 0 otherwise
        return if (antValue.asFlt64().toDouble() <= 0.0 || conValue.asFlt64().toDouble() > 0.0) {
            converter.one
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollectionFlt64): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModelFlt64): Try {
        val mVal = bigM
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators
        allConstraints += nonzeroIndicatorConstraints(antecedent, antecedentIndicatorVar, antecedentSideVar, mVal, tolerance, strictBoundary, "${name}_ant")
        allConstraints += nonzeroIndicatorConstraints(consequent, consequentIndicatorVar, consequentSideVar, mVal, tolerance, strictBoundary, "${name}_con")

        // Implication: antecedent_indicator <= consequent_indicator
        // If antecedent is nonzero, consequent must also be nonzero
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(
                listOf(
                    LinearMonomial(Flt64.one, antecedentIndicatorVar),
                    LinearMonomial(-Flt64.one, consequentIndicatorVar)
                ),
                Flt64.zero
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE, "${name}_imply_link"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    @Suppress("DEPRECATION")
    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val mVal = bigM
        val allConstraints = mutableListOf<Flt64LinearInequality>()

        // Nonzero indicators
        allConstraints += nonzeroIndicatorConstraints(antecedent, antecedentIndicatorVar, antecedentSideVar, mVal, tolerance, strictBoundary, "${name}_ant")
        allConstraints += nonzeroIndicatorConstraints(consequent, consequentIndicatorVar, consequentSideVar, mVal, tolerance, strictBoundary, "${name}_con")

        // Implication: antecedent_indicator <= consequent_indicator
        // If antecedent is nonzero, consequent must also be nonzero
        allConstraints += Flt64LinearInequality(
            LinearPolynomial(
                listOf(
                    LinearMonomial(Flt64.one, antecedentIndicatorVar),
                    LinearMonomial(-Flt64.one, consequentIndicatorVar)
                ),
                Flt64.zero
            ),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.LE, "${name}_imply_link"
        )

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            antecedent: LinearPolynomial<V>,
            consequent: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): ImplyFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            ImplyFunction(antecedent, consequent, converter, bigM, name = name, displayName = displayName)

        operator fun invoke(
            antecedent: LinearPolynomial<Flt64>,
            consequent: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): ImplyFunction<Flt64> = ImplyFunction(antecedent, consequent, IntoValue.Flt64, bigM, name = name, displayName = displayName)

        operator fun invoke(
            antecedent: LinearMonomial<Flt64>,
            consequent: LinearPolynomial<Flt64>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): ImplyFunction<Flt64> = ImplyFunction(
            antecedent = LinearPolynomial(listOf(antecedent), Flt64.zero),
            consequent = consequent,
            bigM = bigM,
            name = name,
            displayName = displayName
        )
    }
}
