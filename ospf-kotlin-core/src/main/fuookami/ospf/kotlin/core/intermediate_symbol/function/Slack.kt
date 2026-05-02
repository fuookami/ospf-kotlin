@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbolFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.UIntVar
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.core.variable.VariableType
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

class SlackFunction<V>(
    val x: LinearPolynomial<V>,
    val y: LinearPolynomial<V>,
    val type: VariableType<*> = UContinuous,
    val withNegative: Boolean = true,
    val withPositive: Boolean = true,
    val threshold: Boolean = false,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(withNegative || withPositive) { "At least one of withNegative or withPositive must be true" }
    }

    internal val negVar: AbstractVariableItem<*, *>? by lazy {
        if (withNegative) createVariable("${name}_neg") else null
    }
    internal val posVar: AbstractVariableItem<*, *>? by lazy {
        if (withPositive) createVariable("${name}_pos") else null
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(negVar, posVar)

    val neg: LinearPolynomial<V>? by lazy {
        negVar?.let { v ->
            LinearPolynomial(listOf(LinearMonomial(oneOf<V>(), v)), zeroOf<V>())
        }
    }

    val pos: LinearPolynomial<V>? by lazy {
        posVar?.let { v ->
            LinearPolynomial(listOf(LinearMonomial(oneOf<V>(), v)), zeroOf<V>())
        }
    }

    private fun createVariable(baseName: String): AbstractVariableItem<*, *> {
        return if (type.isIntegerType) UIntVar(baseName) else URealVar(baseName)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val yValue = y.evaluateWith(values) ?: return null
        val diff = (xValue.asFlt64() - yValue.asFlt64()).toDouble()
        return if (withNegative && withPositive) {
            @Suppress("UNCHECKED_CAST")
            Flt64(kotlin.math.abs(diff)) as V
        } else if (withNegative) {
            val v = kotlin.math.max(0.0, -diff)
            @Suppress("UNCHECKED_CAST")
            Flt64(v) as V
        } else if (withPositive) {
            val v = kotlin.math.max(0.0, diff)
            @Suppress("UNCHECKED_CAST")
            Flt64(v) as V
        } else {
            zeroOf<V>()
        }
    }

    override fun register(model: AbstractLinearMetaModel<V>): Try {
        when (val result = model.add(helperVariables)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        val xPoly = x.asFlt64Poly()
        val yPoly = y.asFlt64Poly()

        if (!threshold) {
            val eqConstraint = LinearInequality<Flt64>(xPoly, yPoly, Comparison.EQ, name)
            when (val result = model.addConstraint(relation = eqConstraint, name = eqConstraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        } else {
            if (withNegative && negVar != null) {
                val lhs = LinearPolynomial(xPoly.monomials + LinearMonomial(Flt64.one, negVar!!), xPoly.constant)
                val constraint = LinearInequality<Flt64>(lhs, yPoly, Comparison.GE, "${name}_neg")
                when (val result = model.addConstraint(relation = constraint, name = constraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            } else if (withPositive && posVar != null) {
                val lhs = LinearPolynomial(xPoly.monomials + LinearMonomial(-Flt64.one, posVar!!), xPoly.constant)
                val constraint = LinearInequality<Flt64>(lhs, yPoly, Comparison.LE, "${name}_pos")
                when (val result = model.addConstraint(relation = constraint, name = constraint.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    val polyX: LinearPolynomial<Flt64> by lazy {
        val xPoly = x.asFlt64Poly()
        var result = LinearPolynomial(xPoly.monomials.toMutableList(), xPoly.constant)
        if (withNegative && negVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(Flt64.one, negVar!!), result.constant)
        }
        if (withPositive && posVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(-Flt64.one, posVar!!), result.constant)
        }
        result
    }

    companion object {
        /**
         * Factory: accept AbstractVariableItem for x and Flt64 for y.
         * For framework code passing variable items directly.
         */
        @JvmStatic
        operator fun invoke(
            x: AbstractVariableItem<*, *>,
            y: Flt64,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackFunction(
                x = LinearPolynomial(listOf(LinearMonomial(Flt64.one, x)), Flt64.zero),
                y = LinearPolynomial(emptyList(), y),
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                name = name,
                displayName = displayName
            )
        )

        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            withPositive: Boolean = true,
            withNegative: Boolean? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val positive = withNegative?.let { !it } ?: withPositive
            return LinearFunctionSymbolAdapter(
                SlackFunction(
                    x = x,
                    y = LinearPolynomial(emptyList(), threshold),
                    type = type,
                    withNegative = !positive,
                    withPositive = positive,
                    threshold = true,
                    name = name,
                    displayName = displayName
                )
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 for y.
         * For framework code passing intermediate symbols directly.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            y: Flt64,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackFunction(
                x = x.toLinearPolynomial(),
                y = LinearPolynomial(emptyList(), y),
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                name = name,
                displayName = displayName
            )
        )

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 threshold.
         * For framework code passing intermediate symbols with threshold mode.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            withPositive: Boolean = true,
            withNegative: Boolean? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val positive = withNegative?.let { !it } ?: withPositive
            return LinearFunctionSymbolAdapter(
                SlackFunction(
                    x = x.toLinearPolynomial(),
                    y = LinearPolynomial(emptyList(), threshold),
                    type = type,
                    withNegative = !positive,
                    withPositive = positive,
                    threshold = true,
                    name = name,
                    displayName = displayName
                )
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 threshold with legacy constraint parameter.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            threshold: Flt64,
            type: VariableType<*> = UContinuous,
            withPositive: Boolean = true,
            withNegative: Boolean? = null,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val positive = withNegative?.let { !it } ?: withPositive
            return LinearFunctionSymbolAdapter(
                SlackFunction(
                    x = x.toLinearPolynomial(),
                    y = LinearPolynomial(emptyList(), threshold),
                    type = type,
                    withNegative = !positive,
                    withPositive = positive,
                    threshold = constraint,
                    name = name,
                    displayName = displayName
                )
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and UInt64 threshold.
         * For framework code using UInt64 threshold values.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            threshold: UInt64,
            type: VariableType<*> = UContinuous,
            withPositive: Boolean = true,
            withNegative: Boolean? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val positive = withNegative?.let { !it } ?: withPositive
            return LinearFunctionSymbolAdapter(
                SlackFunction(
                    x = x.toLinearPolynomial(),
                    y = LinearPolynomial(emptyList(), threshold.toFlt64()),
                    type = type,
                    withNegative = !positive,
                    withPositive = positive,
                    threshold = true,
                    name = name,
                    displayName = displayName
                )
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and UInt64 threshold with legacy constraint parameter.
         * For framework code using UInt64 threshold values and constraint flag.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            threshold: UInt64,
            type: VariableType<*> = UContinuous,
            withPositive: Boolean = true,
            withNegative: Boolean? = null,
            constraint: Boolean = true,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> {
            val positive = withNegative?.let { !it } ?: withPositive
            return LinearFunctionSymbolAdapter(
                SlackFunction(
                    x = x.toLinearPolynomial(),
                    y = LinearPolynomial(emptyList(), threshold.toFlt64()),
                    type = type,
                    withNegative = !positive,
                    withPositive = positive,
                    threshold = constraint,
                    name = name,
                    displayName = displayName
                )
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for both x and y.
         * For framework code computing slack between two intermediate symbols.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbolFlt64,
            y: LinearIntermediateSymbolFlt64,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<Flt64> = LinearFunctionSymbolAdapter(
            SlackFunction(
                x = x.toLinearPolynomial(),
                y = y.toLinearPolynomial(),
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                name = name,
                displayName = displayName
            )
        )
    }
}
