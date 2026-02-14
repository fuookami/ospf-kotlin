package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class BinaryzationFunctionImplBuilderParams(
    val x: AbstractLinearPolynomial<*>,
    val self: BinaryzationFunction,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BinaryzationFunction,
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionImplBuilderParams {
            return BinaryzationFunctionImplBuilderParams(
                x = x.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }
    }
}
typealias BinaryzationFunctionImplBuilder = (BinaryzationFunctionImplBuilderParams) -> AbstractBinaryzationFunctionImpl

abstract class AbstractBinaryzationFunctionImpl(
    protected val x: AbstractLinearPolynomial<*>,
    protected val self: BinaryzationFunction,
) : LinearLogicFunctionSymbol() {
    protected abstract val polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val parent get() = self.parent
    override val args get() = self.args
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
        val value = x.evaluate(tokenList, zeroIfNone)
            ?: return null
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
        )
            ?: return null
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
        )
            ?: return null
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
        val value = x.evaluate(tokenTable, zeroIfNone)
            ?: return null
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
        )
            ?: return null
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
        )
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class BinaryzationFunctionImpl(
    x: AbstractLinearPolynomial<*>,
    self: BinaryzationFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, self) {
    companion object : BinaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BinaryzationFunction,
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionImpl {
            return BinaryzationFunctionImpl(
                x = x.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
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

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        x.copy()
    }

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(
                    values = values,
                    tokenTable = tokenTable
                )
            } ?: return null

            if (xValue gr Flt64.zero) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            null
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

class BinaryzationFunctionPiecewiseImpl(
    x: AbstractLinearPolynomial<*>,
    self: BinaryzationFunction,
    private val epsilon: Flt64 = self.epsilon,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, self) {
    companion object : BinaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BinaryzationFunction,
            epsilon: Flt64 = self.epsilon,
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionPiecewiseImpl {
            return BinaryzationFunctionPiecewiseImpl(
                x = x.toLinearPolynomial(),
                self = self,
                epsilon = epsilon,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BinaryzationFunctionImplBuilderParams): AbstractBinaryzationFunctionImpl {
            return BinaryzationFunctionPiecewiseImpl(params, params.self.epsilon)
        }
    }

    constructor(
        params: BinaryzationFunctionImplBuilderParams,
        epsilon: Flt64
    ): this(
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
                Point2(Flt64.zero, Flt64.zero),
                Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.zero),
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
        x.flush(force)
        piecewiseFunction.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        piecewiseFunction.prepareAndCache(values, tokenTable)

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            if (values.isNullOrEmpty()) {
                piecewiseFunction.evaluate(tokenTable)
            } else {
                piecewiseFunction.evaluate(values, tokenTable)
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = piecewiseFunction.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = piecewiseFunction.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = piecewiseFunction.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = piecewiseFunction.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

class BinaryzationFunctionDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    self: BinaryzationFunction,
    private val extract: Boolean = self.extract,
    m: Flt64? = null,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, self) {
    private val logger = logger()

    companion object : BinaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
           x: T,
           self: BinaryzationFunction,
           extract: Boolean = self.extract,
           m: Flt64? = null,
           name: String,
           displayName: String? = null
        ): BinaryzationFunctionDiscreteImpl {
            return BinaryzationFunctionDiscreteImpl(
                x = x.toLinearPolynomial(),
                self = self,
                extract = extract,
                m = m,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BinaryzationFunctionImplBuilderParams): AbstractBinaryzationFunctionImpl {
            return BinaryzationFunctionDiscreteImpl(
                params = params,
                extract = params.self.extract,
                m = null
            )
        }
    }

    constructor(
        params: BinaryzationFunctionImplBuilderParams,
        extract: Boolean,
        m: Flt64? = null
    ): this(
        x = params.x,
        self = params.self,
        extract = extract,
        m = m,
        name = params.name,
        displayName = params.displayName
    )

    private val mFixed = m != null
    private var m = m ?: x.upperBound!!.value.unwrap()

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

    override fun flush(force: Boolean) {
        x.flush(force)
        val newPossibleRange = possibleRange
        y.range.set(newPossibleRange)
        polyY.flush(force)
        polyY.range.set(newPossibleRange.toFlt64())
        if (!mFixed) {
            m = x.upperBound!!.value.unwrap()
        }
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val bin = xValue gr Flt64.zero
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
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            constraint = m * y geq x,
            name = "${name}_ub",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                constraint = y leq x,
                name = "${name}_lb",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = when (tokenTable) {
            is AbstractTokenTable -> {
                x.evaluate(fixedValues, tokenTable) ?: return register(tokenTable)
            }

            is FunctionSymbolRegistrationScope -> {
                x.evaluate(fixedValues, tokenTable.origin) ?: return register(tokenTable)
            }

            else -> {
                return register(tokenTable)
            }
        }
        val bin = if (xValue gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }

        if (bin eq Flt64.one) {
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val bin = if (xValue gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }

        if (bin eq Flt64.one) {
            when (val result = model.addConstraint(
                constraint = m * y geq x,
                name = "${name}_ub",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                constraint = y leq x,
                name = "${name}_lb",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                constraint = y eq bin,
                name = "${name}_y",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y)?.let { token ->
                token._result = bin
            }
        } else {
            when (val result = model.addConstraint(
                constraint = Flt64.zero geq x,
                name = "${name}_lb",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                constraint = Flt64.zero leq x,
                name = "${name}_ub",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}

class BinaryzationFunctionExtractAndNotDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    self: BinaryzationFunction,
    private val epsilon: Flt64 = self.epsilon,
    m: Flt64? = null,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, self) {
    private val logger = logger()

    companion object : BinaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BinaryzationFunction,
            epsilon: Flt64 = self.epsilon,
            m: Flt64? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunctionExtractAndNotDiscreteImpl {
            return BinaryzationFunctionExtractAndNotDiscreteImpl(
                x = x.toLinearPolynomial(),
                self = self,
                epsilon = epsilon,
                m = m,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BinaryzationFunctionImplBuilderParams): AbstractBinaryzationFunctionImpl {
            return BinaryzationFunctionExtractAndNotDiscreteImpl(
                params = params,
                epsilon = params.self.epsilon,
                m = null
            )
        }
    }

    constructor(
        params: BinaryzationFunctionImplBuilderParams,
        epsilon: Flt64,
        m: Flt64? = null
    ): this(
        x = params.x,
        self = params.self,
        epsilon = epsilon,
        m = m,
        name = params.name,
        displayName = params.displayName
    )

    private val mFixed = m != null
    private var m = m ?: x.upperBound!!.value.unwrap()

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

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
        if (!mFixed) {
            m = x.upperBound!!.value.unwrap()
        }
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            val xValue = if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val pct = xValue / x.upperBound!!.value.unwrap()
            val bin = xValue gr Flt64.zero
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
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(listOf(b, y))) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = model.addConstraint(
            constraint = x eq m * b,
            name = "${name}_xb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            constraint = y geq b,
            name = "${name}_lb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            constraint = y leq (Flt64.one / epsilon) * b,
            name = "${name}_ub",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = when (tokenTable) {
            is AbstractTokenTable -> {
                x.evaluate(fixedValues, tokenTable) ?: return register(tokenTable)
            }

            is FunctionSymbolRegistrationScope -> {
                x.evaluate(fixedValues, tokenTable.origin) ?: return register(tokenTable)
            }

            else -> {
                return register(tokenTable)
            }
        }
        val bin = if (xValue gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }

        if (bin eq Flt64.one) {
            when (val result = tokenTable.add(listOf(b, y))) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val xValue = x.evaluate(fixedValues, model.tokens) ?: return register(model)
        val pct = xValue / x.upperBound!!.value.unwrap()
        val bin = if (xValue gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }

        if (bin eq Flt64.zero) {
            when (val result = model.addConstraint(
                constraint = x eq m * b,
                name = "${name}_xb",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                constraint = y eq bin,
                name = "${name}_y",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y)?.let { token ->
                token._result = bin
            }

            when (val result = model.addConstraint(
                constraint = b eq pct,
                name = "${name}_b",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(b)?.let { token ->
                token._result = pct
            }
        } else {
            when (val result = model.addConstraint(
                constraint = x eq Flt64.zero,
                name = "${name}_x",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}

class BinaryzationFunction(
    private val x: AbstractLinearPolynomial<*>,
    internal val extract: Boolean = true,
    internal val epsilon: Flt64 = Flt64(1e-6),
    internal val piecewise: Boolean = false,
    override val parent: IntermediateSymbol? = null,
    args: Any? = null,
    impl: BinaryzationFunctionImplBuilder? = null,
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
            impl: BinaryzationFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null
        ): BinaryzationFunction {
            return BinaryzationFunction(
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

    private val impl: AbstractBinaryzationFunctionImpl by lazy {
        impl?.invoke(BinaryzationFunctionImplBuilderParams(x, this, name, displayName))
            ?: if (x.discrete && ValueRange(Flt64.zero, Flt64.one).value!! contains x.range.range!!) {
                BinaryzationFunctionImpl(
                    x = x,
                    self = this,
                    name = name,
                    displayName = displayName
                )
            } else if (x.discrete || !extract) {
                BinaryzationFunctionDiscreteImpl(
                    x = x,
                    self = this,
                    extract = extract,
                    name = name,
                    displayName = displayName
                )
            } else if (!x.discrete && (piecewise || epsilon geq piecewiseThreshold)) {
                BinaryzationFunctionPiecewiseImpl(
                    x = x,
                    self = this,
                    epsilon = epsilon,
                    name = name,
                    displayName = displayName
                )
            } else {
                BinaryzationFunctionExtractAndNotDiscreteImpl(
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
        when (val result = impl.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = impl.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = impl.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        when (val result = impl.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: UInt64): String {
        return "bin(${x.toRawString(unfold)})"
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