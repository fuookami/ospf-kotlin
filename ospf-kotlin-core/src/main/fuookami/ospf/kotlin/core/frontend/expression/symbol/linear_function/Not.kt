@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.LinearLogicFunctionSymbol
import fuookami.ospf.kotlin.core.frontend.expression.symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.frontend.expression.symbol.toTidyRawString
import fuookami.ospf.kotlin.core.frontend.model.mechanism.eq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.geq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractTokenTable
import fuookami.ospf.kotlin.core.frontend.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.frontend.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.core.frontend.variable.PctVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.Flt32
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import org.apache.logging.log4j.kotlin.logger

data class NotFunctionImplBuilderParams(
    val x: AbstractLinearPolynomial<*>,
    val self: NotFunction,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            self: NotFunction,
            name: String,
            displayName: String? = null
        ): NotFunctionImplBuilderParams {
            return NotFunctionImplBuilderParams(
                x = x.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }
    }
}
typealias NotFunctionImplBuilder = (NotFunctionImplBuilderParams) -> AbstractNotFunctionImpl

abstract class AbstractNotFunctionImpl(
    protected val x: AbstractLinearPolynomial<*>,
    protected val self: NotFunction
) : LinearLogicFunctionSymbol() {
    protected abstract val polyY: AbstractLinearPolynomial<*>

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
            if (x.upperBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.one
            } else {
                UInt8.zero
            },
            if (x.lowerBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.one
            } else {
                UInt8.zero
            }
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "not(${x.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone) ?: return null
        return if (value eq Flt64.zero) {
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
        return if (value eq Flt64.zero) {
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
        return if (value eq Flt64.zero) {
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
        return if (value eq Flt64.zero) {
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
        return if (value eq Flt64.zero) {
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
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class NotFunctionImpl(
    x: AbstractLinearPolynomial<*>,
    self: NotFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, self) {
    companion object : NotFunctionImplBuilder {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            self: NotFunction,
            name: String,
            displayName: String? = null,
        ): NotFunctionImpl {
            return NotFunctionImpl(
                x = x.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: NotFunctionImplBuilderParams): NotFunctionImpl {
            return NotFunctionImpl(
                x = params.x,
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        Flt64.one - x
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(self, values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            if (xValue eq Flt64.zero) {
                Flt64.one
            } else {
                Flt64.zero
            }
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return ok
    }
}

class NotFunctionPiecewiseImpl(
    x: AbstractLinearPolynomial<*>,
    self: NotFunction,
    private val epsilon: Flt64 = self.epsilon,
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, self) {
    companion object : NotFunctionImplBuilder {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            self: NotFunction,
            epsilon: Flt64,
            name: String,
            displayName: String? = null,
        ): NotFunctionPiecewiseImpl {
            return NotFunctionPiecewiseImpl(
                x = x.toLinearPolynomial(),
                self = self,
                epsilon = epsilon,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: NotFunctionImplBuilderParams): NotFunctionPiecewiseImpl {
            return NotFunctionPiecewiseImpl(params, params.self.epsilon)
        }
    }

    constructor(
        params: NotFunctionImplBuilderParams,
        epsilon: Flt64
    ) : this(
        x = params.x,
        self = params.self,
        epsilon = epsilon,
        name = params.name,
        displayName = params.displayName
    )

    private val piecewiseFunction: UnivariateLinearPiecewiseFunction by lazy {
        UnivariateLinearPiecewiseFunction(
            x = x,
            points = listOf(
                Point2(Flt64.zero, Flt64.one),
                Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.one),
                Point2(epsilon, Flt64.one),
                Point2(Flt64.one, Flt64.one)
            ),
            parent = parent ?: self,
            name = "${name}_piecewise"
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(piecewiseFunction.b.last())
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        piecewiseFunction.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        piecewiseFunction.prepareAndCache(values, tokenTable)

        return prepareIfNotCached(self, values, tokenTable) {
            if (values.isNullOrEmpty()) {
                piecewiseFunction.evaluate(tokenTable)
            } else {
                piecewiseFunction.evaluate(values, tokenTable)
            }
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        piecewiseFunction.register(tokenTable).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        piecewiseFunction.register(model).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        piecewiseFunction.register(tokenTable, fixedValues).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        piecewiseFunction.register(model, fixedValues).takeUnless { it.ok }?.let { return it }

        return ok
    }
}

class NotFunctionDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    self: NotFunction,
    private val extract: Boolean = self.extract,
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, self) {
    private val logger = logger()

    companion object : NotFunctionImplBuilder {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            self: NotFunction,
            extract: Boolean,
            name: String,
            displayName: String? = null,
        ): NotFunctionDiscreteImpl {
            return NotFunctionDiscreteImpl(
                x = x.toLinearPolynomial(),
                self = self,
                extract = extract,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: NotFunctionImplBuilderParams): NotFunctionDiscreteImpl {
            return NotFunctionDiscreteImpl(params, params.self.extract)
        }
    }

    constructor(
        params: NotFunctionImplBuilderParams,
        extract: Boolean
    ) : this(
        x = params.x,
        self = params.self,
        extract = extract,
        name = params.name,
        displayName = params.displayName
    )

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(self, values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val bin = xValue eq Flt64.zero
            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: $bin" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        tokenTable.add(y).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        model.addConstraint( relation = x.upperBound!!.value.unwrap() * (Flt64.one - y) geq x, name = "${name}_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        if (extract) {
            model.addConstraint( relation = (Flt64.one - y) leq x, name = "${name}_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val bin = xValue eq Flt64.zero

        model.addConstraint( relation = x.upperBound!!.value.unwrap() * (Flt64.one - y) geq x, name = "${name}_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        if (extract) {
            model.addConstraint( relation = (Flt64.one - y) leq x, name = "${name}_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }
        }

        model.addConstraint( relation = y eq if (bin) Flt64.one else Flt64.zero, name = "${name}_y", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        return ok
    }
}

class NotFunctionExtractAndNotDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    self: NotFunction,
    private val epsilon: Flt64 = self.epsilon,
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, self) {
    private val logger = logger()

    companion object : NotFunctionImplBuilder {
        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            self: NotFunction,
            epsilon: Flt64,
            name: String,
            displayName: String? = null,
        ): NotFunctionExtractAndNotDiscreteImpl {
            return NotFunctionExtractAndNotDiscreteImpl(
                x = x.toLinearPolynomial(),
                self = self,
                epsilon = epsilon,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: NotFunctionImplBuilderParams): NotFunctionExtractAndNotDiscreteImpl {
            return NotFunctionExtractAndNotDiscreteImpl(params, params.self.epsilon)
        }
    }

    constructor(
        params: NotFunctionImplBuilderParams,
        epsilon: Flt64
    ) : this(
        x = params.x,
        self = params.self,
        epsilon = epsilon,
        name = params.name,
        displayName = params.displayName
    )

    private val b: PctVar by lazy {
        PctVar("${name}_b")
    }

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(self, values, tokenTable) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            val pct = xValue / x.upperBound!!.value.unwrap()
            val bin = xValue eq Flt64.zero
            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting BinaryzationFunction ${name}.b initial solution: $pct" }
            tokenTable.find(b)?.let { token ->
                token._result = pct
            }
            logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: $bin" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            yValue
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        tokenTable.add(b).takeUnless { it.ok }?.let { return it }

        tokenTable.add(y).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        model.addConstraint( x eq x.upperBound!!.value.unwrap() * b, name = "${name}_xb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( (Flt64.one - y) geq b, name = "${name}_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( (Flt64.one - y) leq (Flt64.one / epsilon) * b, name = "${name}_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        tokenTable.add(b).takeUnless { it.ok }?.let { return it }

        tokenTable.add(y).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(
            values = fixedValues,
            tokenTable = model.tokens
        ) ?: return register(model)
        val pct = xValue / x.upperBound!!.value.unwrap()
        val bin = xValue eq Flt64.zero

        model.addConstraint( x eq x.upperBound!!.value.unwrap() * b, name = "${name}_xb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( (Flt64.one - y) geq b, name = "${name}_lb", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( (Flt64.one - y) leq (Flt64.one / epsilon) * b, name = "${name}_ub", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.addConstraint( b eq pct, name = "${name}_b", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.tokens.find(b)?.let { token ->
            token._result = pct
        }

        model.addConstraint( y eq if (bin) Flt64.one else Flt64.zero, name = "${name}_y", from = parent ?: this ).takeUnless { it.ok }?.let { return it }

        model.tokens.find(y)?.let { token ->
            token._result = if (bin) Flt64.one else Flt64.zero
        }

        return ok
    }
}

class NotFunction(
    private val x: AbstractLinearPolynomial<*>,
    internal val extract: Boolean = true,
    internal val epsilon: Flt64 = Flt64(1e-6),
    internal val piecewise: Boolean = false,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    impl: NotFunctionImplBuilder? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol() {
    companion object {
        val piecewiseThreshold: Flt64 = Flt64(1e-5)

        operator fun <
                T : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>
                > invoke(
            x: T,
            extract: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            piecewise: Boolean = false,
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            impl: NotFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null,
        ): NotFunction {
            return NotFunction(
                x = x.toLinearPolynomial(),
                extract = extract,
                epsilon = epsilon,
                piecewise = piecewise,
                parent = parent,
                args = args,
                impl = impl,
                name = name,
                displayName = displayName
            )
        }
    }

    private val impl: AbstractNotFunctionImpl by lazy {
        impl?.invoke(NotFunctionImplBuilderParams(x, this, name, displayName))
            ?: if (x.discrete && ValueRange(Flt64.zero, Flt64.one).value!! contains x.range.range!!) {
                NotFunctionImpl(
                    x = x,
                    self = this,
                    name = name,
                    displayName = displayName
                )
            } else if (x.discrete) {
                NotFunctionDiscreteImpl(
                    x = x,
                    self = this,
                    extract = extract,
                    name = name,
                    displayName = displayName
                )
            } else if (extract && !x.discrete && (piecewise || epsilon geq piecewiseThreshold)) {
                NotFunctionPiecewiseImpl(
                    x = x,
                    self = this,
                    epsilon = epsilon,
                    name = name,
                    displayName = displayName
                )
            } else {
                NotFunctionExtractAndNotDiscreteImpl(
                    x = x,
                    self = this,
                    epsilon = epsilon,
                    name = name,
                    displayName = displayName
                )
            }
    }

    private val _args = args
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

    override fun register(model: AbstractLinearMechanismModel): Try {
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
        model: AbstractLinearMechanismModel,
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
            "not(${x.toTidyRawString(unfold - UInt64.one)})"
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



