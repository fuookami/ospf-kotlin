package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class BalanceTernaryzationFunctionImplBuilderParams(
    val x: AbstractLinearPolynomial<*>,
    val self: BalanceTernaryzationFunction,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BalanceTernaryzationFunction,
            name: String,
            displayName: String? = null
        ): BalanceTernaryzationFunctionImplBuilderParams {
            return BalanceTernaryzationFunctionImplBuilderParams(
                x = x.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }
    }
}
typealias BalanceTernaryzationFunctionImplBuilder = (BalanceTernaryzationFunctionImplBuilderParams) -> AbstractBalanceTernaryzationFunctionImpl

abstract class AbstractBalanceTernaryzationFunctionImpl(
    protected val x: AbstractLinearPolynomial<*>,
    protected val self: BalanceTernaryzationFunction
) : LinearFunctionSymbol() {
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
            if (x.lowerBound!!.value.unwrap() ls Flt64.zero) {
                -Int8.one
            } else if (x.lowerBound!!.value.unwrap() eq Flt64.zero) {
                Int8.zero
            } else {
                Int8.one
            },
            if (x.upperBound!!.value.unwrap() ls Flt64.zero) {
                -Int8.one
            } else if (x.upperBound!!.value.unwrap() eq Flt64.zero) {
                Int8.zero
            } else {
                Int8.one
            }
        ).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "bter(${x.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
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
        val value = x.evaluate(results, tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
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
        val value = x.evaluate(values, tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
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
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
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
        val value = x.evaluate(results, tokenTable, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
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
        val value = x.evaluate(values, tokenTable, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class BalanceTernaryzationFunctionImpl(
    x: AbstractLinearPolynomial<*>,
    self: BalanceTernaryzationFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, self) {
    companion object : BalanceTernaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<*>
        > invoke(
            x: T,
            self: BalanceTernaryzationFunction,
            name: String,
            displayName: String? = null,
        ): BalanceTernaryzationFunctionImpl {
            return BalanceTernaryzationFunctionImpl(
                x = x.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BalanceTernaryzationFunctionImplBuilderParams): AbstractBalanceTernaryzationFunctionImpl {
            return BalanceTernaryzationFunctionImpl(
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
                x.evaluate(values, tokenTable)
            } ?: return null

            val pos = xValue gr Flt64.zero
            val neg = xValue ls Flt64.zero
            val yValue = if (pos) {
                Flt64.one
            } else if (neg) {
                -Flt64.one
            } else {
                Flt64.zero
            }

            yValue
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
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        return ok
    }
}

class BalanceTernaryzationFunctionPiecewiseImpl(
    x: AbstractLinearPolynomial<*>,
    self: BalanceTernaryzationFunction,
    private val epsilon: Flt64 = self.epsilon,
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, self) {
    companion object : BalanceTernaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<*>
        > invoke(
            x: T,
            self: BalanceTernaryzationFunction,
            epsilon: Flt64 = self.epsilon,
            name: String,
            displayName: String? = null,
        ): BalanceTernaryzationFunctionPiecewiseImpl {
            return BalanceTernaryzationFunctionPiecewiseImpl(
                x = x.toLinearPolynomial(),
                self = self,
                epsilon = epsilon,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BalanceTernaryzationFunctionImplBuilderParams): AbstractBalanceTernaryzationFunctionImpl {
            return BalanceTernaryzationFunctionPiecewiseImpl(params, params.self.epsilon)
        }
    }

    constructor(
        params: BalanceTernaryzationFunctionImplBuilderParams,
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
                Point2(x.lowerBound!!.value.unwrap(), -Flt64.one),
                Point2(-epsilon, -Flt64.one),
                Point2(-epsilon + Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                Point2(epsilon, Flt64.one),
                Point2(x.upperBound!!.value.unwrap(), Flt64.one)
            ),
            parent = parent ?: self,
            name = "${name}_piecewise"
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = piecewiseFunction.b.last() - piecewiseFunction.b.first()
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
        fixedValues: Map<Symbol, Flt64>,
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

class BalanceTernaryzationFunctionDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    self: BalanceTernaryzationFunction,
    private val extract: Boolean = self.extract,
    m: Flt64? = null,
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, self) {
    private val logger = logger()

    companion object : BalanceTernaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BalanceTernaryzationFunction,
            extract: Boolean = self.extract,
            name: String,
            displayName: String? = null,
        ): BalanceTernaryzationFunctionDiscreteImpl {
            return BalanceTernaryzationFunctionDiscreteImpl(
                x = x.toLinearPolynomial(),
                self = self,
                extract = extract,
                name = name,
                displayName = displayName
            )
        }

        override fun invoke(params: BalanceTernaryzationFunctionImplBuilderParams): AbstractBalanceTernaryzationFunctionImpl {
            return BalanceTernaryzationFunctionDiscreteImpl(params, params.self.extract, null)
        }
    }

    constructor(
        params: BalanceTernaryzationFunctionImplBuilderParams,
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

    private val possibleUpperBound get() = max(
        abs(x.lowerBound!!.value.unwrap()),
        abs(x.upperBound!!.value.unwrap())
    )
    private val mFixed = m != null
    private var m = m ?: possibleUpperBound

    private val y: BinVariable1 by lazy {
        val y = BinVariable1("${name}_y", Shape1(2))
        y[0].range.leq(x.lowerBound!!.value.unwrap() ls Flt64.zero)
        y[1].range.leq(x.upperBound!!.value.unwrap() gr Flt64.zero)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = y[1] - y[0]
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        x.flush(force)
        if (!mFixed) {
            m = possibleUpperBound
        }
        y[0].range.set(ValueRange(UInt8.zero, (x.lowerBound!!.value.unwrap() ls Flt64.zero).toUInt8()).value!!)
        y[1].range.set(ValueRange(UInt8.zero, (x.upperBound!!.value.unwrap() gr Flt64.zero).toUInt8()).value!!)
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
                x.evaluate(values, tokenTable)
            } ?: return null

            val pos = xValue gr Flt64.zero
            val neg = xValue ls Flt64.zero

            logger.trace { "Setting BalanceTernaryzationFunction ${name}.y initial solution: $pos, $neg" }
            tokenTable.find(y[0])?.let { token ->
                token._result = pos.toFlt64()
            }
            tokenTable.find(y[1])?.let { token ->
                token._result = neg.toFlt64()
            }

            if (pos) {
                Flt64.one
            } else if (neg) {
                -Flt64.one
            } else {
                Flt64.zero
            }
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
            m * y[1] geq x,
            name = "${name}_pub",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            -m * y[0] leq x,
            name = "${name}_nlb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                x geq (-m - Flt64.one) * (Flt64.one - y[1]) + Flt64.one,
                name = "${name}_plb",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                x leq (m + Flt64.one) * (Flt64.one - y[0]) - Flt64.one,
                name = "${name}_nub",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                sum(y) leq Flt64.one,
                name = "${name}_y",
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
        val ter = if (xValue gr Flt64.zero) {
            Flt64.one
        } else if (xValue ls Flt64.zero) {
            -Flt64.one
        } else {
            Flt64.zero
        }

        if (ter eq Flt64.one) {
            when (val result = tokenTable.add(y[1])) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (ter eq -Flt64.one) {
            when (val result = tokenTable.add(y[0])) {
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
        val ter = if (xValue gr Flt64.zero) {
            Flt64.one
        } else if (xValue ls Flt64.zero) {
            -Flt64.one
        } else {
            Flt64.zero
        }

        if (ter eq Flt64.one) {
            when (val result = model.addConstraint(
                m * y[1] geq x,
                name = "${name}_ub",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y[1] eq Flt64.one,
                name = "${name}_y",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y[1])?.let { token ->
                token._result = Flt64.one
            }
        } else if (ter eq -Flt64.one) {
            when (val result = model.addConstraint(
                -m * y[0] leq x,
                name = "${name}_lb",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y[0] eq Flt64.one,
                name = "${name}_y",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y[0])?.let { token ->
                token._result = Flt64.one
            }
        }

        return ok
    }
}

class BalanceTernaryzationFunctionExtractAndNotDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    self: BalanceTernaryzationFunction,
    private val epsilon: Flt64 = self.epsilon,
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, self) {
    private val logger = logger()

    companion object : BalanceTernaryzationFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            self: BalanceTernaryzationFunction,
            epsilon: Flt64 = self.epsilon,
            name: String,
            displayName: String? = null,
        ): BalanceTernaryzationFunctionExtractAndNotDiscreteImpl {
            return BalanceTernaryzationFunctionExtractAndNotDiscreteImpl(
                x = x.toLinearPolynomial(),
                self = self,
                epsilon = epsilon,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: BalanceTernaryzationFunctionImplBuilderParams): AbstractBalanceTernaryzationFunctionImpl {
            return BalanceTernaryzationFunctionExtractAndNotDiscreteImpl(params, params.self.epsilon)
        }
    }

    constructor(
        params: BalanceTernaryzationFunctionImplBuilderParams,
        epsilon: Flt64
    ): this(
        x = params.x,
        self = params.self,
        epsilon = epsilon,
        name = params.name,
        displayName = params.displayName
    )

    private val b: PctVariable1 by lazy {
        PctVariable1(name = "${name}_b", Shape1(2))
    }

    private val y: BinVariable1 by lazy {
        val y = BinVariable1("${name}_y", Shape1(2))
        y[0].range.leq(x.lowerBound!!.value.unwrap() ls Flt64.zero)
        y[1].range.leq(x.upperBound!!.value.unwrap() gr Flt64.zero)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = y[1] - y[0]
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        x.flush(force)
        y[0].range.set(ValueRange(UInt8.zero, (x.lowerBound!!.value.unwrap() ls Flt64.zero).toUInt8()).value!!)
        y[1].range.set(ValueRange(UInt8.zero, (x.upperBound!!.value.unwrap() gr Flt64.zero).toUInt8()).value!!)
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
            val xValue =  if (values.isNullOrEmpty()) {
                x.evaluate(tokenTable)
            } else {
                x.evaluate(values, tokenTable)
            } ?: return null

            val pos = xValue gr Flt64.zero
            val pocPct = if (pos) {
                xValue / x.upperBound!!.value.unwrap()
            } else {
                Flt64.zero
            }
            val neg = xValue ls Flt64.zero
            val negPct = if (neg) {
                xValue / x.lowerBound!!.value.unwrap()
            } else {
                Flt64.zero
            }
            val yValue = if (pos) {
                Flt64.one
            } else if (neg) {
                -Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting BalanceTernaryzationFunction ${name}.b initial solution: $pocPct, $negPct" }
            tokenTable.find(b[0])?.let { token ->
                token._result = pocPct
            }
            tokenTable.find(b[1])?.let { token ->
                token._result = negPct
            }
            logger.trace { "Setting BalanceTernaryzationFunction ${name}.y initial solution: $pos, $neg" }
            tokenTable.find(y[0])?.let { token ->
                token._result = pos.toFlt64()
            }
            tokenTable.find(y[1])?.let { token ->
                token._result = neg.toFlt64()
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = tokenTable.add(b)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

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
            x eq x.lowerBound!!.value.unwrap() * b[0] + x.upperBound!!.value.unwrap() * b[1],
            name = "${name}_xb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[1] geq b[1],
            name = "${name}_yb_pos_lb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[1] leq (Flt64.one / epsilon) * b[1],
            name = "${name}_yb_pos_ub",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[0] geq b[0],
            name = "${name}_yb_neg_lb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[0] leq (Flt64.one / epsilon) * b[0],
            name = "${name}_yb_neg_ub",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            b[0] + y[1] leq Flt64.one,
            name = "${name}_yb_pos",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            b[1] + y[0] leq Flt64.one,
            name = "${name}_yb_neg",
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
        val ter = if (xValue gr Flt64.zero) {
            Flt64.one
        } else if (xValue ls Flt64.zero) {
            -Flt64.one
        } else {
            Flt64.zero
        }

        if (ter eq Flt64.one) {
            when (val result = tokenTable.add(listOf(y[1], b[1]))) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (ter eq -Flt64.one) {
            when (val result = tokenTable.add(listOf(y[0], b[0]))) {
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
        val ter = if (xValue gr Flt64.zero) {
            Flt64.one
        } else if (xValue ls Flt64.zero) {
            -Flt64.one
        } else {
            Flt64.zero
        }

        if (ter eq Flt64.one) {
            when (val result = model.addConstraint(
                x eq x.upperBound!!.value.unwrap() * b[1],
                "${name}_xb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y[1] eq Flt64.one,
                "${name}_y"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y[1])?.let { token ->
                token._result = Flt64.one
            }

            val bValue = xValue / x.upperBound!!.value.unwrap()
            when (val result = model.addConstraint(
                b[1] eq bValue,
                "${name}_b"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(b[1])?.let { token ->
                token._result = bValue
            }
        } else if (ter eq -Flt64.one) {
            when (val result = model.addConstraint(
                x eq x.lowerBound!!.value.unwrap() * b[0],
                "${name}_xb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y[0] eq Flt64.one,
                "${name}_y"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y[0])?.let { token ->
                token._result = Flt64.one
            }

            val bValue = xValue / x.lowerBound!!.value.unwrap()
            when (val result = model.addConstraint(
                b[0] eq bValue,
                "${name}_b"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(b[0])?.let { token ->
                token._result = bValue
            }
        } else {
            when (val result = model.addConstraint(
                x eq Flt64.zero,
                "${name}_x"
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

class BalanceTernaryzationFunction(
    val x: AbstractLinearPolynomial<*>,
    internal val extract: Boolean = true,
    internal val epsilon: Flt64 = Flt64(1e-6),
    internal val piecewise: Boolean = false,
    m: Flt64? = null,
    override val parent: IntermediateSymbol? = null,
    impl: BalanceTernaryzationFunctionImplBuilder? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol() {
    companion object {
        val piecewiseThreshold: Flt64 = Flt64(1e-5)

        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x : T,
            extract: Boolean = true,
            epsilon: Flt64 = Flt64(1e-6),
            piecewise: Boolean = false,
            parent: IntermediateSymbol? = null,
            impl: BalanceTernaryzationFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null
        ): BalanceTernaryzationFunction {
            return BalanceTernaryzationFunction(
                x = x.toLinearPolynomial(),
                extract = extract,
                epsilon = epsilon,
                piecewise = piecewise,
                parent = parent,
                impl = impl,
                name = name,
                displayName = displayName
            )
        }
    }

    private val impl: AbstractBalanceTernaryzationFunctionImpl by lazy {
        impl?.invoke(BalanceTernaryzationFunctionImplBuilderParams(x, this, name, displayName))
            ?: if (x.discrete && ValueRange(-Flt64.one, Flt64.one).value!! contains x.range.range!!) {
                BalanceTernaryzationFunctionImpl(
                    x = x,
                    self = this,
                    name = name,
                    displayName = displayName
                )
            } else if (x.discrete) {
                BalanceTernaryzationFunctionDiscreteImpl(
                    x = x,
                    self = this,
                    extract = extract,
                    m = m,
                    name = name,
                    displayName = displayName
                )
            } else if (extract && !x.discrete && (piecewise || epsilon geq piecewiseThreshold)) {
                BalanceTernaryzationFunctionPiecewiseImpl(
                    x = x,
                    self = this,
                    epsilon = epsilon,
                    name = name,
                    displayName = displayName
                )
            } else {
                BalanceTernaryzationFunctionExtractAndNotDiscreteImpl(
                    x = x,
                    self = this,
                    epsilon = epsilon,
                    name = name,
                    displayName = displayName
                )
            }
    }

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
        return "bter(${x.toRawString(unfold)})"
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
        return impl.evaluate(results, tokenList, zeroIfNone)
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.evaluate(values, tokenList, zeroIfNone)
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
        return impl.calculateValue(results, tokenTable, zeroIfNone)
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        return impl.calculateValue(values, tokenTable, zeroIfNone)
    }
}
