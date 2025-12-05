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

class IfInFunction(
    private val x: AbstractLinearPolynomial<*>,
    lowerBound: AbstractLinearPolynomial<*>,
    upperBound: AbstractLinearPolynomial<*>,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol() {
    private val logger = logger()

    companion object {
        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            lowerBound: Int,
            upperBound: Int,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = LinearPolynomial(lowerBound),
                upperBound = LinearPolynomial(upperBound),
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
            T : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>
        > invoke(
            x: T,
            lowerBound: Double,
            upperBound: Double,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = LinearPolynomial(lowerBound),
                upperBound = LinearPolynomial(upperBound),
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
            T1 : ToLinearPolynomial<Poly>,
            Poly : AbstractLinearPolynomial<Poly>,
            T2 : RealNumber<T2>,
            T3 : RealNumber<T3>
        > invoke (
            x: T1,
            lowerBound: T2,
            upperBound: T3,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = LinearPolynomial(lowerBound),
                upperBound = LinearPolynomial(upperBound),
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
            T1 : ToLinearPolynomial<Poly1>,
            Poly1 : AbstractLinearPolynomial<Poly1>,
            T2 : ToLinearPolynomial<Poly2>,
            Poly2 : AbstractLinearPolynomial<Poly2>,
            T3 : ToLinearPolynomial<Poly3>,
            Poly3 : AbstractLinearPolynomial<Poly3>
        > invoke(
            x: T1,
            lowerBound: T2,
            upperBound: T3,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = lowerBound.toLinearPolynomial(),
                upperBound = upperBound.toLinearPolynomial(),
                epsilon = epsilon,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    private val lb = lowerBound
    private val ub = upperBound

    private val lowerBoundInequality = (x geq lb).normalize()
    private val upperBoundInequality = (x leq ub).normalize()

    init {
        lowerBoundInequality.name = "${name}_lb"
        upperBoundInequality.name = "${name}_ub"
    }

    private val lbk: PctVariable1 by lazy {
        PctVariable1("${name}_lbk", Shape1(3))
    }

    private val ubk: PctVariable1 by lazy {
        PctVariable1("${name}_ubk", Shape1(3))
    }

    private val lby: BinVar by lazy {
        BinVar("${name}_lby")
    }

    private val uby: BinVar by lazy {
        BinVar("${name}_uby")
    }

    private val y: AndFunction by lazy {
        AndFunction(
            polynomials = listOf(lby, uby),
            parent = parent ?: this,
            name = "${name}_y"
        )
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange)
        polyY
    }

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(lb.dependencies)
            dependencies.addAll(ub.dependencies)
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            // todo: impl by Inequality.judge()
            return ValueRange(Flt64.zero, Flt64.one).value!!
        }

    override fun flush(force: Boolean) {
        x.flush(force)
        lb.flush(force)
        ub.flush(force)
        lowerBoundInequality.flush(force)
        upperBoundInequality.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        lb.cells
        ub.cells
        lowerBoundInequality.cells
        upperBoundInequality.cells

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            val lbBin = if (values.isNullOrEmpty()) {
                lowerBoundInequality.isTrue(tokenTable)
            } else {
                lowerBoundInequality.isTrue(values, tokenTable)
            } ?: return null
            val lbyValue = if (lbBin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            val ubBin = if (values.isNullOrEmpty()) {
                upperBoundInequality.isTrue(tokenTable)
            } else {
                upperBoundInequality.isTrue(values, tokenTable)
            } ?: return null
            val ubyValue = if (ubBin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting IfInFunction ${name}.lby initial solution: $lbBin" }
            tokenTable.find(lby)?.let { token ->
                token._result = lbyValue
            }

            logger.trace { "Setting IfInFunction ${name}.uby initial solution: $ubBin" }
            tokenTable.find(uby)?.let { token ->
                token._result = ubyValue
            }

            y.prepare(values, tokenTable)
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        when (val result = lowerBoundInequality.register(
            parentName = name,
            k = lbk,
            flag = lby,
            tokenTable = tokenTable
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = upperBoundInequality.register(
            parentName = name,
            k = ubk,
            flag = uby,
            tokenTable = tokenTable
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = y.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = lowerBoundInequality.register(
            parent = parent ?: this,
            parentName = name,
            k = lbk,
            flag = lby,
            epsilon = epsilon,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = upperBoundInequality.register(
            parent = parent ?: this,
            parentName = name,
            k = ubk,
            flag = uby,
            epsilon = epsilon,
            model = model
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = y.register(model)) {
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
        when (val result = lowerBoundInequality.register(
            parent = parent ?: this,
            parentName = name,
            k = lbk,
            flag = lby,
            epsilon = epsilon,
            model = model,
            fixedValues = fixedValues
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = upperBoundInequality.register(
            parent = parent ?: this,
            parentName = name,
            k = ubk,
            flag = uby,
            epsilon = epsilon,
            model = model,
            fixedValues = fixedValues
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = y.register(model, fixedValues)) {
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
            "if_in(${x.toTidyRawString(unfold - UInt64.one)}, [${lb.toTidyRawString(unfold - UInt64.one)}, ${ub.toTidyRawString(unfold - UInt64.one)}])"
        }
    }

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenList, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(tokenList, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(tokenList, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
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
        val xValue = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(results, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(results, tokenList, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
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
        val xValue = x.evaluate(values, tokenList, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(values, tokenList, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(values, tokenList, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        val xValue = x.evaluate(tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(tokenTable, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
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
        val xValue = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
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
        val xValue = x.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val lbValue = lb.evaluate(values, tokenTable, zeroIfNone) ?: return null
        val ubValue = ub.evaluate(values, tokenTable, zeroIfNone) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}
