package fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function

import fuookami.ospf.kotlin.core.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.expression.polynomial.ToLinearPolynomial
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearLogicFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.prepareIfNotCached
import fuookami.ospf.kotlin.core.intermediate_symbol.toTidyRawString
import fuookami.ospf.kotlin.core.intermediate_model.flattenData
import fuookami.ospf.kotlin.core.intermediate_model.geq
import fuookami.ospf.kotlin.core.intermediate_model.leq
import fuookami.ospf.kotlin.core.intermediate_model.LinearConstraintInput
import fuookami.ospf.kotlin.core.intermediate_model.normalize

import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.intermediate_model.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractTokenList
import fuookami.ospf.kotlin.core.variable.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.PctVariable1
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import org.apache.logging.log4j.kotlin.logger

class IfInFunction(
    private val x: AbstractLinearPolynomial<*>,
    lowerBound: AbstractLinearPolynomial<*>,
    upperBound: AbstractLinearPolynomial<*>,
    private val epsilon: Flt64 = Flt64(1e-6),
    override val parent: IntermediateSymbol? = null,
    args: Any? = parent?.args,
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
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = LinearPolynomial(lowerBound),
                upperBound = LinearPolynomial(upperBound),
                epsilon = epsilon,
                parent = parent,
                args = args,
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
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = LinearPolynomial(lowerBound),
                upperBound = LinearPolynomial(upperBound),
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }

        operator fun <
                T1 : ToLinearPolynomial<Poly>,
                Poly : AbstractLinearPolynomial<Poly>,
                T2 : RealNumber<T2>,
                T3 : RealNumber<T3>
                > invoke(
            x: T1,
            lowerBound: T2,
            upperBound: T3,
            epsilon: Flt64 = Flt64(1e-6),
            parent: IntermediateSymbol? = null,
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = LinearPolynomial(lowerBound),
                upperBound = LinearPolynomial(upperBound),
                epsilon = epsilon,
                parent = parent,
                args = args,
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
            args: Any? = null,
            name: String,
            displayName: String? = null
        ): IfInFunction {
            return IfInFunction(
                x = x.toLinearPolynomial(),
                lowerBound = lowerBound.toLinearPolynomial(),
                upperBound = upperBound.toLinearPolynomial(),
                epsilon = epsilon,
                parent = parent,
                args = args,
                name = name,
                displayName = displayName
            )
        }
    }

    private val lb = lowerBound
    private val ub = upperBound

    private val lowerBoundInequality: LinearConstraintInput by lazy {
        val ineq = (x geq lb).normalize()
        LinearConstraintInput(
            flattenData = ineq.flattenData,
            sign = ineq.comparison,
            lhsRange = x.range.valueRange!!,
            name = "${name}_lb"
        )
    }

    private val upperBoundInequality: LinearConstraintInput by lazy {
        val ineq = (x leq ub).normalize()
        LinearConstraintInput(
            flattenData = ineq.flattenData,
            sign = ineq.comparison,
            lhsRange = x.range.valueRange!!,
            name = "${name}_ub"
        )
    }

    internal val _args = args
    override val args get() = _args ?: parent?.args

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
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        return prepareIfNotCached(values, tokenTable) {
            val lbBin = if (values.isNullOrEmpty()) {
                lowerBoundInequality.isTrue(tokenTable)
            } else {
                lowerBoundInequality.isTrue(values, tokenTable)
            } ?: return@prepareIfNotCached null
            val lbyValue = if (lbBin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            val ubBin = if (values.isNullOrEmpty()) {
                upperBoundInequality.isTrue(tokenTable)
            } else {
                upperBoundInequality.isTrue(values, tokenTable)
            } ?: return@prepareIfNotCached null
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
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
        lowerBoundInequality.register(parentName = name, k = lbk, flag = lby, tokenTable = tokenTable).takeUnless { it.ok }?.let { return it }

        upperBoundInequality.register(parentName = name, k = ubk, flag = uby, tokenTable = tokenTable).takeUnless { it.ok }?.let { return it }

        y.register(tokenTable).takeUnless { it.ok }?.let { return it }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        lowerBoundInequality.register(parent = parent ?: this, parentName = name, k = lbk, flag = lby, epsilon = epsilon, model = model).takeUnless { it.ok }?.let { return it }

        upperBoundInequality.register(parent = parent ?: this, parentName = name, k = ubk, flag = uby, epsilon = epsilon, model = model).takeUnless { it.ok }?.let { return it }

        y.register(model).takeUnless { it.ok }?.let { return it }

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
        lowerBoundInequality.register(parent = parent ?: this, parentName = name, k = lbk, flag = lby, epsilon = epsilon, model = model, fixedValues = fixedValues).takeUnless { it.ok }?.let { return it }

        upperBoundInequality.register(parent = parent ?: this, parentName = name, k = ubk, flag = uby, epsilon = epsilon, model = model, fixedValues = fixedValues).takeUnless { it.ok }?.let { return it }

        y.register(model, fixedValues).takeUnless { it.ok }?.let { return it }

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
        val xValue = x.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val lbValue = lb.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            results = results,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
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
        val xValue = x.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val lbValue = lb.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            values = values,
            tokenList = tokenList,
            zeroIfNone = zeroIfNone
        ) ?: return null
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
        val xValue = x.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val lbValue = lb.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            results = results,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
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
        val xValue = x.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val lbValue = lb.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        val ubValue = ub.evaluate(
            values = values,
            tokenTable = tokenTable,
            zeroIfNone = zeroIfNone
        ) ?: return null
        return if (lbValue leq xValue && xValue leq ubValue) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}





