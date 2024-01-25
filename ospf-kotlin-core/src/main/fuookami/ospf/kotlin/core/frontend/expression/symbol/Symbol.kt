package fuookami.ospf.kotlin.core.frontend.expression.symbol

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

interface Symbol<Cell : MonomialCell<Cell, C>, C : Category> : Expression {
    val cells: List<Cell>
    val cached: Boolean

    fun flush(force: Boolean = false)

    fun toRawString(unfold: Boolean = false): String

    fun value(tokenTable: AbstractTokenTable<Cell, C>, zeroIfNone: Boolean = false): Flt64? {
        return value(tokenTable.tokenList, zeroIfNone)
    }

    fun value(results: List<Flt64>, tokenTable: AbstractTokenTable<Cell, C>, zeroIfNone: Boolean = false): Flt64? {
        return value(results, tokenTable.tokenList, zeroIfNone)
    }
}

class ExpressionSymbol<Poly : MutablePolynomial<Poly, M, Cell, C>, M : Monomial<M, Cell, C>, Cell : MonomialCell<Cell, C>, C : Category>(
    private val _polynomial: Poly,
    override var name: String = "",
    override var displayName: String? = null
) : Symbol<Cell, C> {
    companion object {
        operator fun invoke(
            item: AbstractVariableItem<*, *>,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return this(LinearPolynomial(item), name, displayName)
        }

        operator fun invoke(
            symbol: Symbol<LinearMonomialCell, Linear>,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return this(LinearPolynomial(symbol), name, displayName)
        }

        operator fun invoke(
            monomial: LinearMonomial,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return this(LinearPolynomial(monomial), name, displayName)
        }

        operator fun invoke(
            polynomial: AbstractLinearPolynomial<*>,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return LinearExpressionSymbol(
                _polynomial = (polynomial.asMutable() as MutableLinearPolynomial?) ?: polynomial.toMutable(),
                name = name.ifEmpty { polynomial.name },
                displayName = displayName ?: polynomial.displayName
            )
        }

        operator fun <T : RealNumber<T>> invoke(
            constant: T,
            name: String = "",
            displayName: String? = null
        ): LinearExpressionSymbol {
            return this(LinearPolynomial(constant), name, displayName)
        }
    }

    val polynomial: Polynomial<Poly, M, Cell, C> by ::_polynomial

    fun asMutable(): Poly {
        return _polynomial
    }

    override val discrete by polynomial::discrete

    override val range by polynomial::range
    override val lowerBound by polynomial::lowerBound
    override val upperBound by polynomial::upperBound

    override val cells by polynomial::cells
    override val cached by polynomial::cached

    override fun flush(force: Boolean) {
        polynomial.flush(force)
    }

    override fun toString(): String {
        return displayName
            ?: name.ifEmpty {
                polynomial.toString()
            }
    }

    override fun toRawString(unfold: Boolean): String {
        return "(${polynomial.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.value(tokenList, zeroIfNone)
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return polynomial.value(results, tokenList, zeroIfNone)
    }
}

interface FunctionSymbol<Cell : MonomialCell<Cell, C>, C : Category> : Symbol<Cell, C> {
    fun register(tokenTable: MutableTokenTable<Cell, C>): Try
    fun register(model: Model<Cell, C>): Try
}

interface LogicFunctionSymbol<Cell : MonomialCell<Cell, C>, C : Category> : FunctionSymbol<Cell, C> {
    fun isTrue(tokenList: AbstractTokenList, zeroIfNone: Boolean): Boolean? {
        return this.value(tokenList, zeroIfNone)?.let { it eq Flt64.one }
    }

    fun isTrue(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Boolean? {
        return this.value(results, tokenList, zeroIfNone)?.let { it eq Flt64.one }
    }
}

typealias LinearSymbol = Symbol<LinearMonomialCell, Linear>
typealias LinearExpressionSymbol = ExpressionSymbol<MutableLinearPolynomial, LinearMonomial, LinearMonomialCell, Linear>
typealias LinearFunctionSymbol = FunctionSymbol<LinearMonomialCell, Linear>
typealias LinearLogicFunctionSymbol = LogicFunctionSymbol<LinearMonomialCell, Linear>
