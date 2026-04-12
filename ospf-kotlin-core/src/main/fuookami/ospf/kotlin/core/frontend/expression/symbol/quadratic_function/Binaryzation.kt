@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.QuadraticLogicFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.model.mechanism.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.geq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.toFlt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import org.apache.logging.log4j.kotlin.logger

data class BinaryzationFunctionImplBuilderParams(
    val x: AbstractQuadraticPolynomial<*>,
    val self: BinaryzationFunction,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            self: BinaryzationFunction,
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionImplBuilderParams {
            return BinaryzationFunctionImplBuilderParams(
                x = x.toQuadraticPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }
    }
}
typealias BinaryzationFunctionImplBuilder = (BinaryzationFunctionImplBuilderParams) -> AbstractBinaryzationFunctionImpl

abstract class AbstractBinaryzationFunctionImpl(
    protected val x: AbstractQuadraticPolynomial<*>,
    protected val self: BinaryzationFunction
) : QuadraticLogicFunctionSymbol() {
    protected abstract val polyY: AbstractQuadraticPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val parent get() = self.parent
    override val dependencies get() = x.dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    protected val possibleRange
        get() = ValueRange(
            if (x.lowerBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.zero
            } else {
                UInt8.one
            },
            if (x.upperBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.zero
            } else {
                UInt8.one
            }
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun toRawString(unfold: UInt64): String {
        return "bin(${x.toRawString(unfold)})"
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone) ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(tokenTable, zeroIfNone) ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class BinaryzationFunctionImpl(
    x: AbstractQuadraticPolynomial<*>,
    self: BinaryzationFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, self) {
    companion object : BinaryzationFunctionImplBuilder {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            self: BinaryzationFunction,
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionImpl {
            return BinaryzationFunctionImpl(
                x = x.toQuadraticPolynomial(),
                self = self,
                name,
                displayName
            )
        }

        override operator fun invoke(params: BinaryzationFunctionImplBuilderParams): AbstractBinaryzationFunctionImpl {
            return BinaryzationFunctionImpl(
                x = params.x,
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    override val polyY: AbstractQuadraticPolynomial<*> by lazy {
        x.copy()
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(self, values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val yValue = if (xValue gr Flt64.zero) {
                Flt64.one
            } else {
                Flt64.zero
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return ok
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return ok
    }
}

class BinaryzationFunctionLinearImpl(
    x: AbstractQuadraticPolynomial<*>,
    self: BinaryzationFunction,
    private val epsilon: Flt64 = self.epsilon,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, self) {
    private val logger = logger()

    companion object : BinaryzationFunctionImplBuilder {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            self: BinaryzationFunction,
            epsilon: Flt64 = Flt64(1e-6),
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionLinearImpl {
            return BinaryzationFunctionLinearImpl(
                x = x.toQuadraticPolynomial(),
                self = self,
                epsilon = epsilon,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BinaryzationFunctionImplBuilderParams): AbstractBinaryzationFunctionImpl {
            return BinaryzationFunctionLinearImpl(params, params.self.epsilon)
        }
    }

    constructor(
        params: BinaryzationFunctionImplBuilderParams,
        epsilon: Flt64
    ) : this(
        x = params.x,
        self = params.self,
        epsilon = epsilon,
        name = params.name,
        displayName = params.displayName
    )

    private val linearX: LinearFunction by lazy {
        LinearFunction(
            polynomial = x,
            parent = parent ?: self,
            name = "${name}_linear"
        )
    }

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val polyY: AbstractQuadraticPolynomial<*> by lazy {
        val polyY = QuadraticPolynomial(y)
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        linearX.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        linearX.prepareAndCache(values, tokenTable)

        return prepareIfNotCached(self, values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                linearX.evaluate(tokenTable)
            } else {
                linearX.evaluate(values, tokenTable)
            } ?: return null

            val yValue = if (xValue gr Flt64.zero) {
                Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: $yValue" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        tokenTable.add(y).takeUnless { it.ok }?.let { return it }

        linearX.register(tokenTable).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        linearX.register(model).takeUnless { it.ok }?.let { return it }

        model.addConstraint( (Flt64.one - y) * linearX leq x.upperBound!!.value.unwrap().toFlt64() * y, name = "${name}_ub", from = parent ?: self ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( x geq epsilon * y, name = "${name}_lb", from = parent ?: self ).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        tokenTable.add(y).takeUnless { it.ok }?.let { return it }

        linearX.register(tokenTable, fixedValues).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val value = x.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val bin = value gr Flt64.zero

        linearX.register(model, fixedValues).takeUnless { it.ok }?.let { return it }

        model.addConstraint( (Flt64.one - y) * linearX leq x.upperBound!!.value.unwrap().toFlt64() * y, name = "${name}_ub", from = parent ?: self ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( x geq epsilon * y, name = "${name}_lb", from = parent ?: self ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( y eq bin, name = "${name}_y", from = parent ?: self ).takeUnless { it.ok }?.let { return it }

        model.tokens.find(y)?.let { token ->
            token._result = bin.toFlt64()
        }

        return ok
    }
}

class BinaryzationFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    internal val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    impl: BinaryzationFunctionImplBuilder? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticLogicFunctionSymbol() {
    companion object {
        operator fun <
                T : ToQuadraticPolynomial<Poly>,
                Poly : AbstractQuadraticPolynomial<Poly>
                > invoke(
            x: T,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            impl: BinaryzationFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunction {
            return BinaryzationFunction(
                x = x.toQuadraticPolynomial(),
                epsilon = epsilon,
                parent = parent,
                args = args,
                impl = impl,
                name = name,
                displayName = displayName
            )
        }
    }

    private val impl: AbstractBinaryzationFunctionImpl by lazy {
        impl?.invoke(BinaryzationFunctionImplBuilderParams(x, this, name, displayName))
            ?: if (x.discrete && ValueRange(Flt64.zero, Flt64.one).value!! contains x.range.range!!) {
                BinaryzationFunctionImpl(
                    x = x,
                    self = this,
                    name = name,
                    displayName = displayName
                )
            } else {
                BinaryzationFunctionLinearImpl(
                    x = x,
                    self = this,
                    epsilon = epsilon,
                    name = name,
                    displayName = displayName
                )
            }
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

    override val discrete = true

    override val range get() = impl.range
    override val lowerBound get() = impl.lowerBound
    override val upperBound get() = impl.upperBound

    override val category get() = Linear

    override val dependencies: Set<IntermediateSymbol> get() = impl.dependencies
    override val cells get() = impl.cells
    override val cached get() = impl.cached

    override fun flush(force: Boolean) {
        impl.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return impl.prepare(values, tokenTable)
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        impl.register(tokenTable).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        impl.register(model).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        impl.register(tokenTable, fixedValues).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        model: AbstractQuadraticMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        impl.register(model, fixedValues).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "bin(${x.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.evaluate(tokenList, zeroIfNone)
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        )
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.calculateValue(tokenTable, zeroIfNone)
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.calculateValue(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.calculateValue(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        )
    }
}





