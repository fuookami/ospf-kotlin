@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.solver.value.IntoValue
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
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class SlackFunction<V>(
    val x: LinearPolynomial<V>,
    val y: LinearPolynomial<V>,
    val type: VariableType<*> = UContinuous,
    val withNegative: Boolean = true,
    val withPositive: Boolean = true,
    val threshold: Boolean = false,
    private val converter: IntoValue<V>,
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
            val unit = x.constant / x.constant
            LinearPolynomial(listOf(LinearMonomial(unit, v)), x.constant - x.constant)
        }
    }

    val pos: LinearPolynomial<V>? by lazy {
        posVar?.let { v ->
            val unit = x.constant / x.constant
            LinearPolynomial(listOf(LinearMonomial(unit, v)), x.constant - x.constant)
        }
    }

    private fun createVariable(baseName: String): AbstractVariableItem<*, *> {
        return if (type.isIntegerType) UIntVar(baseName) else URealVar(baseName)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val yValue = y.evaluateWith(values) ?: return null
        val diff = (converter.fromValue(xValue) - converter.fromValue(yValue)).toDouble()
        return if (withNegative && withPositive) {
            converter.intoValue(Flt64(kotlin.math.abs(diff)))
        } else if (withNegative) {
            val v = kotlin.math.max(0.0, -diff)
            converter.intoValue(Flt64(v))
        } else if (withPositive) {
            val v = kotlin.math.max(0.0, diff)
            converter.intoValue(Flt64(v))
        } else {
            converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val xPoly = x.asFlt64Poly(converter)
        val yPoly = y.asFlt64Poly(converter)

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
    val polyX: LinearPolynomial<V> by lazy {
        val unit = x.constant / x.constant
        var result = LinearPolynomial(x.monomials.toMutableList(), x.constant)
        if (withNegative && negVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(unit, negVar!!), result.constant)
        }
        if (withPositive && posVar != null) {
            result = LinearPolynomial(result.monomials + LinearMonomial(-unit, posVar!!), result.constant)
        }
        result
    }

    companion object {
        /** Generic V-typed invoke: primary entry point with x and y polynomials. */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            converter: IntoValue<V>,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return SlackFunction(
                x = x,
                y = y,
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                converter = converter,
                name = name ?: "",
                displayName = displayName
            )
        }

        /** Flt64-specific invoke: primary entry point with x and y polynomials. */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            y: LinearPolynomial<Flt64>,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<Flt64> = SlackFunction(
            x = x,
            y = y,
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            converter = flt64Converter,
            name = name ?: "",
            displayName = displayName
        )

        /** Generic V-typed invoke with LinearIntermediateSymbol<V>. */
        operator fun <V> invoke(
            x: LinearIntermediateSymbol<V>,
            y: LinearPolynomial<V>,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            converter: IntoValue<V>,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return invoke(
                x = x.polynomial,
                y = y,
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }

        /** Flt64-specific invoke with LinearIntermediateSymbol<Flt64>. */
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            y: LinearPolynomial<Flt64>,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<Flt64> = invoke(
            x = x.polynomial,
            y = y,
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )

        /** Generic V-typed invoke with ToLinearPolynomial<V>. */
        operator fun <V> invoke(
            x: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<V>,
            y: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<V>,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            converter: IntoValue<V>,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return invoke(
                x = x.toLinearPolynomial(),
                y = y.toLinearPolynomial(),
                type = type,
                withNegative = withNegative,
                withPositive = withPositive,
                threshold = threshold,
                converter = converter,
                name = name,
                displayName = displayName
            )
        }

        /** Flt64-specific invoke with ToLinearPolynomial<Flt64>. */
        operator fun invoke(
            x: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            y: fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial<Flt64>,
            type: VariableType<*> = UContinuous,
            withNegative: Boolean = true,
            withPositive: Boolean = true,
            threshold: Boolean = false,
            name: String? = null,
            displayName: String? = null
        ): SlackFunction<Flt64> = invoke(
            x = x.toLinearPolynomial(),
            y = y.toLinearPolynomial(),
            type = type,
            withNegative = withNegative,
            withPositive = withPositive,
            threshold = threshold,
            converter = flt64Converter,
            name = name,
            displayName = displayName
        )

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
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
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
                    converter = flt64Converter,
                    name = name,
                    displayName = displayName
                ),
            converter = flt64Converter
        
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 for y.
         * For framework code passing intermediate symbols directly.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
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
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 threshold.
         * For framework code passing intermediate symbols with threshold mode.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
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
                    converter = flt64Converter,
                    name = name,
                    displayName = displayName
                ),
            converter = flt64Converter
        
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and Flt64 threshold with legacy constraint parameter.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
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
                    converter = flt64Converter,
                    name = name,
                    displayName = displayName
                ),
            converter = flt64Converter
        
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and UInt64 threshold.
         * For framework code using UInt64 threshold values.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
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
                    converter = flt64Converter,
                    name = name,
                    displayName = displayName
                ),
            converter = flt64Converter
        
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for x and UInt64 threshold with legacy constraint parameter.
         * For framework code using UInt64 threshold values and constraint flag.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
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
                    converter = flt64Converter,
                    name = name,
                    displayName = displayName
                ),
            converter = flt64Converter
        
            )
        }

        /**
         * Factory: accept LinearIntermediateSymbol for both x and y.
         * For framework code computing slack between two intermediate symbols.
         */
        @JvmStatic
        operator fun invoke(
            x: LinearIntermediateSymbol<Flt64>,
            y: LinearIntermediateSymbol<Flt64>,
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
                converter = flt64Converter,
                name = name,
                displayName = displayName
            ),
            converter = flt64Converter
        
        )
    }
}
