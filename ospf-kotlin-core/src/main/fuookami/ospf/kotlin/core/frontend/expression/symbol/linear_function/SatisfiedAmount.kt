package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class SatisfiedAmountPolynomialFunctionImplBuilderParams(
    val polynomials: List<AbstractLinearPolynomial<*>>,
    val self: AbstractSatisfiedAmountPolynomialFunction,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            self: AbstractSatisfiedAmountPolynomialFunction,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountPolynomialFunctionImplBuilderParams {
            return SatisfiedAmountPolynomialFunctionImplBuilderParams(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                self = self,
                name = name,
                displayName = displayName
            )
        }
    }
}
typealias SatisfiedAmountPolynomialFunctionImplBuilder = (SatisfiedAmountPolynomialFunctionImplBuilderParams) -> AbstractSatisfiedAmountPolynomialFunctionImpl

abstract class AbstractSatisfiedAmountPolynomialFunctionImpl(
    protected val amount: UInt64? = null,
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    protected val self: AbstractSatisfiedAmountPolynomialFunction
) : LinearFunctionSymbol {
    protected abstract val polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category get() = Linear

    override val parent get() = self.parent
    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    protected val possibleRange: ValueRange<Flt64>
        get() {
            val minAmount = UInt64(polynomials.count { it.lowerBound!!.value.unwrap() neq Flt64.zero })
            val maxAmount = UInt64(polynomials.size - polynomials.count { it.upperBound!!.value.unwrap() eq Flt64.zero })
            return if (amount != null) {
                if (minAmount geq amount) {
                    ValueRange(Flt64.one, Flt64.one).value!!
                } else if (maxAmount ls amount) {
                    ValueRange(Flt64.zero, Flt64.zero).value!!
                } else {
                    ValueRange(Flt64.zero, Flt64.one).value!!
                }
            } else {
                ValueRange(minAmount.toFlt64(), maxAmount.toFlt64()).value!!
            }
        }

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }

        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        for (polynomial in polynomials) {
            polynomial.cells
        }
        return null
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            if (amount != null) {
                "satisfied_amount_${amount}(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            } else {
                "satisfied_amount(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            }
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(results, tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(values, tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(results, tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(values, tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }
}

private class SatisfiedAmountPolynomialFunctionAnyImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    self: AbstractSatisfiedAmountPolynomialFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunctionImpl(UInt64.one, polynomials, self) {
    companion object : SatisfiedAmountPolynomialFunctionImplBuilder {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            self: AbstractSatisfiedAmountPolynomialFunction,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountPolynomialFunctionAnyImpl {
            return SatisfiedAmountPolynomialFunctionAnyImpl(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: SatisfiedAmountPolynomialFunctionImplBuilderParams): AbstractSatisfiedAmountPolynomialFunctionImpl {
            return SatisfiedAmountPolynomialFunctionAnyImpl(
                polynomials = params.polynomials,
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    private val or: OrFunction by lazy {
        OrFunction(
            polynomials = polynomials,
            parent = parent,
            name = name,
            displayName = displayName
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(or)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        or.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        if (values.isNullOrEmpty()) {
            super.prepareAndCache(null, tokenTable)
            or.prepareAndCache(null, tokenTable)
        } else {
            super.prepareAndCache(values, tokenTable)
            or.prepareAndCache(values, tokenTable)
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            val bin = if (values.isNullOrEmpty()) {
                or.evaluate(tokenTable)
            } else {
                or.evaluate(values, tokenTable)
            } ?: return null

            if (bin eq Flt64.one) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = or.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = or.register(model)) {
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
        when (val result = or.register(tokenTable, fixedValues)) {
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
        when (val result = or.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class SatisfiedAmountPolynomialFunctionAllImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    self: AbstractSatisfiedAmountPolynomialFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunctionImpl(UInt64(polynomials.size), polynomials, self) {
    companion object : SatisfiedAmountPolynomialFunctionImplBuilder {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            self: AbstractSatisfiedAmountPolynomialFunction,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountPolynomialFunctionAllImpl {
            return SatisfiedAmountPolynomialFunctionAllImpl(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: SatisfiedAmountPolynomialFunctionImplBuilderParams): AbstractSatisfiedAmountPolynomialFunctionImpl {
            return SatisfiedAmountPolynomialFunctionAllImpl(
                polynomials = params.polynomials,
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    private val and: AndFunction by lazy {
        AndFunction(
            polynomials = polynomials,
            parent = parent ?: this,
            name = name,
            displayName = displayName
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(and)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        and.flush(force)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        if (values.isNullOrEmpty()) {
            super.prepareAndCache(null, tokenTable)
            and.prepareAndCache(null, tokenTable)
        } else {
            super.prepareAndCache(values, tokenTable)
            and.prepareAndCache(values, tokenTable)
        }

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            val bin = if (values.isNullOrEmpty()) {
                and.evaluate(tokenTable)
            } else {
                and.evaluate(values, tokenTable)
            } ?: return null

            if (bin eq Flt64.one) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = and.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = and.register(model)) {
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
        when (val result = and.register(tokenTable, fixedValues)) {
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
        when (val result = and.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class SatisfiedAmountPolynomialFunctionSomeImpl(
    amount: UInt64?,
    polynomials: List<AbstractLinearPolynomial<*>>,
    private val extract: Boolean = true,
    self: AbstractSatisfiedAmountPolynomialFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunctionImpl(amount, polynomials, self) {
    private val logger = logger()

    companion object : SatisfiedAmountPolynomialFunctionImplBuilder {
        operator fun invoke(
            amount: UInt64?,
            polynomials: List<ToLinearPolynomial<*>>,
            extract: Boolean,
            self: AbstractSatisfiedAmountPolynomialFunction,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountPolynomialFunctionSomeImpl {
            return SatisfiedAmountPolynomialFunctionSomeImpl(
                amount = amount,
                polynomials = polynomials.map { it.toLinearPolynomial() },
                extract = extract,
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: SatisfiedAmountPolynomialFunctionImplBuilderParams): AbstractSatisfiedAmountPolynomialFunctionImpl {
            return SatisfiedAmountPolynomialFunctionSomeImpl(params, params.self.amount, params.self.extract)
        }
    }

    constructor(
        params: SatisfiedAmountPolynomialFunctionImplBuilderParams,
        amount: UInt64?,
        extract: Boolean
    ): this(
        amount = amount,
        polynomials = params.polynomials,
        extract = extract,
        self = params.self,
        name = params.name,
        displayName = params.displayName
    )

    private val bins: SymbolCombination<BinaryzationFunction, Shape1> by lazy {
        SymbolCombination("${name}_bin", Shape1(polynomials.size)) { i, _ ->
            BinaryzationFunction(
                x = polynomials[i],
                parent = parent ?: this,
                name = "${name}_bin_$i"
            )
        }
    }

    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        if (amount != null) {
            val polyY = LinearPolynomial(y)
            polyY.range.set(possibleRange)
            polyY
        } else {
            val polyY = sum(bins)
            polyY.range.set(possibleRange)
            polyY
        }
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        for (bin in bins) {
            bin.flush(force)
        }
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        if (values.isNullOrEmpty()) {
            super.prepareAndCache(null, tokenTable)
        } else {
            super.prepareAndCache(values, tokenTable)
        }
        tokenTable.cache(
            bins.mapNotNull {
                val value = if (values.isNullOrEmpty()) {
                    it.prepare(null, tokenTable)
                } else {
                    it.prepare(values, tokenTable)
                }
                if (value != null) {
                    (it as IntermediateSymbol) to value
                } else {
                    null
                }
            }.toMap()
        )

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && tokenTable.cached(self) == false) {
            val count = bins.count {
                val value = it.evaluate(tokenTable) ?: return null
                value eq Flt64.one
            }

            val yValue = if (amount != null) {
                val bin = UInt64(count) >= amount
                val yValue = bin.toFlt64()

                logger.trace { "Setting SatisfiedAmountPolynomialFunction ${name}.y to $bin" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }
                yValue
            } else {
                Flt64(count)
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        for (bin in bins) {
            when (val result = bin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (amount != null) {
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for (bin in bins) {
            when (val result = bin.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (amount != null) {
            when (val result = model.addConstraint(
                y geq (sum(bins) - amount + UInt64.one) / UInt64(polynomials.size),
                name = "${name}_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (extract) {
                when (val result = model.addConstraint(
                    y leq sum(bins) / amount,
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
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
        val values = polynomials.map {
            it.evaluate(fixedValues, model.tokens) ?: return register(model)
        }
        val amountValue = UInt64(values.count { it gr Flt64.zero })

        for (bin in bins) {
            when (val result = bin.register(model, fixedValues)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (amount != null) {
            val bin = amountValue == amount

            when (val result = model.addConstraint(
                y geq (sum(bins) - amount + UInt64.one) / UInt64(polynomials.size),
                name = "${name}_ub",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (extract) {
                when (val result = model.addConstraint(
                    y leq sum(bins) / amount,
                    name = "${name}_lb",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            when (val result = model.addConstraint(
                y eq bin.toFlt64(),
                name = "${name}_y",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y)?.let { token ->
                token._result = bin.toFlt64()
            }
        }

        return ok
    }
}

sealed class AbstractSatisfiedAmountPolynomialFunction(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    internal val extract: Boolean = true,
    override val parent: IntermediateSymbol? = null,
    impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    open val amount: UInt64? = null

    private val impl: AbstractSatisfiedAmountPolynomialFunctionImpl by lazy {
        impl?.invoke(SatisfiedAmountPolynomialFunctionImplBuilderParams(polynomials, this, name, displayName))
            ?: when (amount) {
                UInt64.one -> {
                    SatisfiedAmountPolynomialFunctionAnyImpl(
                        polynomials = polynomials,
                        self = this,
                        name = name,
                        displayName = displayName
                    )
                }

                UInt64(polynomials.size) -> {
                    SatisfiedAmountPolynomialFunctionAllImpl(
                        polynomials = polynomials,
                        self = this,
                        name = name,
                        displayName = displayName
                    )
                }

                else -> {
                    SatisfiedAmountPolynomialFunctionSomeImpl(
                        amount = amount,
                        polynomials = polynomials,
                        extract = extract,
                        self = this,
                        name = name,
                        displayName = displayName
                    )
                }
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
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            if (amount != null) {
                "satisfied_amount_${amount}(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            } else {
                "satisfied_amount(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            }
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

class SatisfiedAmountPolynomialFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: IntermediateSymbol? = null,
    impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunction(
    polynomials = polynomials,
    parent = parent,
    impl = impl,
    name = name,
    displayName = displayName
) {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountPolynomialFunction {
            return SatisfiedAmountPolynomialFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                parent = parent,
                impl = impl,
                name = name,
                displayName = displayName
            )
        }
    }
}

class AtLeastPolynomialFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    override val amount: UInt64,
    extract: Boolean = true,
    parent: IntermediateSymbol? = null,
    impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunction(
    polynomials = polynomials,
    extract = extract,
    parent = parent,
    impl = impl,
    name = name,
    displayName = displayName
), LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            amount: UInt64,
            extract: Boolean = true,
            parent: IntermediateSymbol? = null,
            impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null
        ): AtLeastPolynomialFunction {
            return AtLeastPolynomialFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                amount = amount,
                extract = extract,
                parent = parent,
                impl = impl,
                name = name,
                displayName = displayName
            )
        }
    }

    init {
        assert(amount != UInt64.zero)
        assert(UInt64(polynomials.size) geq amount)
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "at_least_${amount}(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }
}

data object SatisfiedAmountFunction {
    operator fun invoke(
        polynomials: List<AbstractLinearPolynomial<*>>,
        parent: IntermediateSymbol? = null,
        impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
        name: String,
        displayName: String? = null
    ): SatisfiedAmountPolynomialFunction {
        return SatisfiedAmountPolynomialFunction(
            polynomials = polynomials,
            parent = parent,
            impl = impl,
            name = name,
            displayName = displayName
        )
    }

    @JvmName("constructWithInequalities")
    operator fun invoke(
        inequalities: List<LinearInequality>,
        parent: IntermediateSymbol? = null,
        name: String,
        displayName: String? = null
    ): SatisfiedAmountInequalityFunction {
        return SatisfiedAmountInequalityFunction(
            inequalities = inequalities,
            parent = parent,
            name = name,
            displayName = displayName
        )
    }

    @JvmName("constructWithToInequalities")
    operator fun invoke(
        inequalities: List<ToLinearInequality>,
        parent: IntermediateSymbol? = null,
        name: String,
        displayName: String? = null
    ): SatisfiedAmountInequalityFunction {
        return SatisfiedAmountInequalityFunction(
            inequalities = inequalities.map { it.toLinearInequality() },
            parent = parent,
            name = name,
            displayName = displayName
        )
    }
}

data object AtLeastFunction {
    operator fun invoke(
        polynomials: List<AbstractLinearPolynomial<*>>,
        amount: UInt64,
        extract: Boolean = true,
        parent: IntermediateSymbol? = null,
        impl: SatisfiedAmountPolynomialFunctionImplBuilder? = null,
        name: String,
        displayName: String? = null
    ): AtLeastPolynomialFunction {
        return AtLeastPolynomialFunction(
            polynomials = polynomials,
            amount = amount,
            extract = extract,
            parent = parent,
            impl = impl,
            name = name,
            displayName = displayName
        )
    }

    @JvmName("constructWithInequalities")
    operator fun invoke(
        inequalities: List<LinearInequality>,
        constraint: Boolean = true,
        amount: UInt64,
        parent: IntermediateSymbol? = null,
        name: String,
        displayName: String? = null
    ): AtLeastInequalityFunction {
        return AtLeastInequalityFunction(
            inequalities = inequalities,
            constraint = constraint,
            amount = amount,
            parent = parent,
            name = name,
            displayName = displayName
        )
    }

    @JvmName("constructWithToInequalities")
    operator fun invoke(
        inequalities: List<ToLinearInequality>,
        constraint: Boolean = true,
        amount: UInt64,
        parent: IntermediateSymbol? = null,
        name: String,
        displayName: String? = null
    ): AtLeastInequalityFunction {
        return AtLeastInequalityFunction(
            inequalities = inequalities.map { it.toLinearInequality() },
            constraint = constraint,
            amount = amount,
            parent = parent,
            name = name,
            displayName = displayName
        )
    }
}
