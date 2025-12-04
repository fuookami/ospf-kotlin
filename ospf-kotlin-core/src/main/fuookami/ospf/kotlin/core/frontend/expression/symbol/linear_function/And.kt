package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

data class AndFunctionImplBuilderParams(
    val polynomials: List<AbstractLinearPolynomial<*>>,
    val self: AndFunction,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            self: AndFunction,
            name: String,
            displayName: String? = null
        ): AndFunctionImplBuilderParams {
            return AndFunctionImplBuilderParams(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                self = self,
                name = name,
                displayName = displayName
            )
        }
    }
}
typealias AndFunctionImplBuilder = (AndFunctionImplBuilderParams) -> AbstractAndFunctionImpl

abstract class AbstractAndFunctionImpl(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    protected val self: AndFunction
) : LinearLogicFunctionSymbol {
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

    protected val possibleRange
        get() = ValueRange(
            if (polynomials.any { it.lowerBound!!.value.unwrap() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            },
            if (polynomials.any { it.upperBound!!.value.unwrap() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            }
        ).value!!

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "and(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.zero) }})"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        return if (polynomials.all {
            val thisValue = it.evaluate(tokenList, zeroIfNone) ?: return null
            thisValue neq Flt64.zero
        }) {
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
        return if (polynomials.all {
            val thisValue = it.evaluate(results, tokenList, zeroIfNone) ?: return null
            thisValue neq Flt64.zero
        }) {
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
        return if (polynomials.all {
            val thisValue = it.evaluate(values, tokenList, zeroIfNone) ?: return null
            thisValue neq Flt64.zero
        }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        return if (polynomials.all {
            val thisValue = it.evaluate(tokenTable, zeroIfNone) ?: return null
            thisValue neq Flt64.zero
        }) {
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
        return if (polynomials.all {
            val thisValue = it.evaluate(results, tokenTable, zeroIfNone) ?: return null
            thisValue neq Flt64.zero
        }) {
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
        return if (polynomials.all {
            val thisValue = it.evaluate(values, tokenTable, zeroIfNone) ?: return null
            thisValue neq Flt64.zero
        }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class AndFunctionOnePolynomialImpl(
    val polynomial: AbstractLinearPolynomial<*>,
    self: AndFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractAndFunctionImpl(listOf(polynomial), self) {
    companion object : AndFunctionImplBuilder {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            polynomial: T,
            self: AndFunction,
            name: String,
            displayName: String? = null
        ): AndFunctionOnePolynomialImpl {
            return AndFunctionOnePolynomialImpl(
                polynomial = polynomial.toLinearPolynomial(),
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: AndFunctionImplBuilderParams): AndFunctionOnePolynomialImpl {
            return AndFunctionOnePolynomialImpl(
                polynomial = params.polynomials.first(),
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    private val bin: BinaryzationFunction by lazy {
        BinaryzationFunction(
            x = polynomial,
            parent = parent ?: self,
            name = "${name}_bin"
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(bin)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }
        bin.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        polynomial.cells
        bin.prepareAndCache(values, tokenTable)

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            if (values.isNullOrEmpty()) {
                bin.evaluate(tokenTable)
            } else {
                bin.evaluate(values, tokenTable)
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = bin.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = bin.register(model)) {
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
        when (val result = bin.register(tokenTable, fixedValues)) {
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
        when (val result = bin.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class AndFunctionMultiPolynomialImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    self: AndFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractAndFunctionImpl(polynomials, self) {
    companion object : AndFunctionImplBuilder {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            self: AndFunction,
            name: String,
            displayName: String? = null
        ): AndFunctionMultiPolynomialImpl {
            return AndFunctionMultiPolynomialImpl(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: AndFunctionImplBuilderParams): AbstractAndFunctionImpl {
            return AndFunctionMultiPolynomialImpl(
                polynomials = params.polynomials,
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    private val maxmin: MaxMinFunction by lazy {
        MaxMinFunction(
            polynomials = polynomials,
            parent = parent ?: self,
            name = "${name}_maxmin"
        )
    }

    private val bin: BinaryzationFunction by lazy {
        BinaryzationFunction(
            x = LinearPolynomial(maxmin),
            parent = parent ?: self,
            name = "${name}_bin"
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(bin)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }
        maxmin.flush(force)
        bin.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        for (polynomial in polynomials) {
            polynomial.cells
        }
        maxmin.prepareAndCache(values, tokenTable)
        bin.prepareAndCache(values, tokenTable)

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            if (values.isNullOrEmpty()) {
                bin.evaluate(tokenTable)
            } else {
                bin.evaluate(values, tokenTable)
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = maxmin.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = bin.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = maxmin.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = bin.register(model)) {
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
        when (val result = maxmin.register(tokenTable, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = bin.register(tokenTable, fixedValues)) {
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
        when (val result = maxmin.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = bin.register(model, fixedValues)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class AndFunctionMultiPolynomialBinaryImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    self: AndFunction,
    override var name: String,
    override var displayName: String? = null
) : AbstractAndFunctionImpl(polynomials, self) {
    companion object : AndFunctionImplBuilder {
        operator fun invoke(
            polynomials: List<AbstractLinearPolynomial<*>>,
            self: AndFunction,
            name: String,
            displayName: String? = null
        ): AndFunctionMultiPolynomialImpl {
            return AndFunctionMultiPolynomialImpl(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                self = self,
                name = name,
                displayName = displayName
            )
        }

        override operator fun invoke(params: AndFunctionImplBuilderParams): AbstractAndFunctionImpl {
            return AndFunctionMultiPolynomialBinaryImpl(
                polynomials = params.polynomials,
                self = params.self,
                name = params.name,
                displayName = params.displayName
            )
        }
    }

    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange)
        polyY
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

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(self)
        } else {
            tokenTable.cached(self, values)
        } == false) {
            val yValue = polynomials.all { polynomial ->
                val thisValue = if (values.isNullOrEmpty()) {
                    polynomial.evaluate(tokenTable)
                } else {
                    polynomial.evaluate(values, tokenTable)
                } ?: return null
                thisValue eq Flt64.zero
            }
            logger.trace { "Setting AndFunction ${name}.y to $yValue" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue.toFlt64()
            }

            if (yValue) {
                Flt64.one
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
        // if any polynomial is zero, y will be zero
        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                y leq polynomial,
                name = "${name}_ub_${polynomial.name.ifEmpty { "$i" }}",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        // if all polynomial are not zero, y will be not zero
        when (val result = model.addConstraint(
            y geq (sum(polynomials) - Flt64(polynomials.lastIndex)),
            name = "${name}_lb",
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
        return register(tokenTable)
    }

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val values = polynomials.map { it.evaluate(fixedValues, model.tokens) ?: return register(model) }
        val bin = values.all { it gr Flt64.zero }

        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                y leq polynomial,
                name = "${name}_ub_${polynomial.name.ifEmpty { "$i" }}",
                from = parent ?: self
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        when (val result = model.addConstraint(
            y geq (sum(polynomials) - Flt64(polynomials.lastIndex)),
            name = "${name}_lb",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y eq bin,
            name = "${name}_y",
            from = parent ?: self
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(y)?.let { token ->
            token._result = bin.toFlt64()
        }

        return ok
    }
}

class AndFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override val parent: IntermediateSymbol? = null,
    impl: AndFunctionImplBuilder? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            impl: AndFunctionImplBuilder? = null,
            name: String,
            displayName: String? = null
        ): AndFunction {
            return AndFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                parent = parent,
                impl = impl,
                name = name,
                displayName = displayName
            )
        }
    }

    private val impl: AbstractAndFunctionImpl by lazy {
        impl?.invoke(AndFunctionImplBuilderParams(polynomials, this, name, displayName))
            ?: if (polynomials.size == 1) {
                AndFunctionOnePolynomialImpl(
                    polynomial = polynomials.first(),
                    self = this,
                    name = name,
                    displayName = displayName
                )
            } else if (polynomials.all { it.discrete && it.upperBound!!.value.unwrap() leq Flt64.one }) {
                AndFunctionMultiPolynomialBinaryImpl(
                    polynomials = polynomials,
                    self = this,
                    name = name,
                    displayName = displayName
                )
            } else {
                AndFunctionMultiPolynomialImpl(
                    polynomials = polynomials,
                    self = this,
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
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound!!.value.unwrap() ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
            }
        }

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
            "and(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
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
