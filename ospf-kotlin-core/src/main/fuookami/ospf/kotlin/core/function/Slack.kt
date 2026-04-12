@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.UIntVar
import fuookami.ospf.kotlin.core.frontend.variable.UContinuous
import fuookami.ospf.kotlin.core.frontend.variable.URealVar
import fuookami.ospf.kotlin.core.frontend.model.mechanism.geq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.eq
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as UtilsLinearPolynomial
import fuookami.ospf.kotlin.math.operator.abs
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

class SlackFunction(
    val x: LinearPolynomial<Flt64>,
    val y: LinearPolynomial<Flt64>,
    val type: fuookami.ospf.kotlin.core.frontend.variable.VariableType<*> = UContinuous,
    val withNegative: Boolean = true,
    val withPositive: Boolean = true,
    val threshold: Boolean = false,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol {
    init {
        require(withNegative || withPositive) { "At least one of withNegative or withPositive must be true" }
    }

    private val negVar: AbstractVariableItem<*, *>? by lazy {
        if (withNegative) createVariable("${name}_neg") else null
    }
    private val posVar: AbstractVariableItem<*, *>? by lazy {
        if (withPositive) createVariable("${name}_pos") else null
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(negVar, posVar)

    val neg: LinearPolynomial<Flt64>? by lazy {
        negVar?.let { v -> LinearPolynomial(listOf(LinearMonomial(Flt64.one, v)), Flt64.zero) }
    }

    val pos: LinearPolynomial<Flt64>? by lazy {
        posVar?.let { v -> LinearPolynomial(listOf(LinearMonomial(Flt64.one, v)), Flt64.zero) }
    }

    private fun createVariable(baseName: String): AbstractVariableItem<*, *> {
        return if (type.isIntegerType) UIntVar(baseName) else URealVar(baseName)
    }

    override fun evaluate(values: Map<Symbol, Flt64>): Flt64? {
        val xValue = x.evaluate(values)
        val yValue = y.evaluate(values)
        return if (withNegative && withPositive) abs(xValue - yValue)
        else if (withNegative) max(Flt64.zero, yValue - xValue)
        else if (withPositive) max(Flt64.zero, xValue - yValue)
        else Flt64.zero
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        val vars = listOfNotNull(negVar, posVar)
        if (vars.isNotEmpty()) {
            when (val result = model.add(vars)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        if (!threshold) {
            when (val result = model.addConstraint(relation = polyX eq y, name = name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            if (withNegative && negVar != null) {
                val lhs = LinearPolynomial(x.monomials + LinearMonomial(Flt64.one, negVar!!), x.constant)
                when (val result = model.addConstraint(relation = lhs geq y, name = "${name}_neg")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else if (withPositive && posVar != null) {
                val lhs = LinearPolynomial(x.monomials + LinearMonomial(-Flt64.one, posVar!!), x.constant)
                when (val result = model.addConstraint(relation = lhs leq y, name = "${name}_pos")) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    val polyX: LinearPolynomial<Flt64> by lazy {
        var result = LinearPolynomial(x.monomials.toMutableList(), x.constant)
        if (withNegative && negVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(Flt64.one, negVar!!), result.constant)
        }
        if (withPositive && posVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(-Flt64.one, posVar!!), result.constant)
        }
        result
    }

    companion object {
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            y: Flt64,
            type: fuookami.ospf.kotlin.core.frontend.variable.VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String,
            displayName: String? = null
        ): SlackFunction = SlackFunction(
            x = x,
            y = LinearPolynomial(emptyList(), y),
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            threshold: Flt64,
            type: fuookami.ospf.kotlin.core.frontend.variable.VariableType<*> = UContinuous,
            withPositive: Boolean = true,
            withNegative: Boolean? = null,
            name: String,
            displayName: String? = null
        ): SlackFunction {
            val positive = withNegative?.let { !it } ?: withPositive
            return SlackFunction(
                x = x,
                y = LinearPolynomial(emptyList(), threshold),
                type = type,
                withNegative = !positive,
                withPositive = positive,
                threshold = true,
                name = name,
                displayName = displayName
            )
        }
    }
}
