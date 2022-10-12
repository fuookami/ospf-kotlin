package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

interface Inequality<C : Category> {
    val lhs: Polynomial<C>
    val rhs: Polynomial<C>
    val sign: Sign
    var name: String
    var displayName: String?

    val cells: List<MonomialCell<C>>

    fun isTrue(tokenList: TokenList): Boolean? {
        val lhsValue = lhs.value(tokenList) ?: return null
        val rhsValue = rhs.value(tokenList) ?: return null
        return sign(lhsValue, rhsValue)
    }
    fun isTrue(result: List<Flt64>, tokenList: TokenList): Boolean {
        val lhsValue = lhs.value(result, tokenList)
        val rhsValue = rhs.value(result, tokenList)
        return sign(lhsValue, rhsValue)
    }
}

class LinearInequality(
    override val lhs: Polynomial<Linear>,
    override val rhs: Polynomial<Linear>,
    override val sign: Sign,
    override var name: String = "",
    override var displayName: String? = null
) : Inequality<Linear> {
    override val cells: List<LinearMonomialCell>
        get() {
            val cells = HashMap<ItemKey, LinearMonomialCell>()
            var constant = Flt64.zero
            for (cell in lhs.cells) {
                when (val temp = (cell as LinearMonomialCell).cell) {
                    is Either.Left -> {
                        if (cells.containsKey(temp.value.variable.key)) {
                            cells[temp.value.variable.key]!! += cell
                        } else {
                            cells[temp.value.variable.key] = cell
                        }
                    }

                    is Either.Right -> {
                        constant += temp.value
                    }
                }
            }
            for (cell in rhs.cells) {
                when (val temp = (cell as LinearMonomialCell).cell) {
                    is Either.Left -> {
                        if (cells.containsKey(temp.value.variable.key)) {
                            cells[temp.value.variable.key]!! -= cell
                        } else {
                            val copy = cell.copy()
                            copy *= -Flt64.one
                            cells[temp.value.variable.key] = copy
                        }
                    }

                    is Either.Right -> {
                        constant -= temp.value
                    }
                }
            }
            val ret = cells.map { it.value }.toMutableList()
            ret.add(LinearMonomialCell(constant))
            return ret
        }

    override fun toString() = "$lhs $sign $rhs"
}

// variable and constant

infix fun <T : RealNumber<T>> Item<*, *>.ls(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Less)

infix fun <T : RealNumber<T>> Item<*, *>.leq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.LessEqual)

infix fun <T : RealNumber<T>> Item<*, *>.eq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Equal)

infix fun <T : RealNumber<T>> Item<*, *>.neq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Unequal)

infix fun <T : RealNumber<T>> Item<*, *>.gr(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Greater)

infix fun <T : RealNumber<T>> Item<*, *>.geq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.GreaterEqual)

infix fun <T : RealNumber<T>> T.ls(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Less)

infix fun <T : RealNumber<T>> T.leq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.LessEqual)

infix fun <T : RealNumber<T>> T.eq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Equal)

infix fun <T : RealNumber<T>> T.neq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Unequal)

infix fun <T : RealNumber<T>> T.gr(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Greater)

infix fun <T : RealNumber<T>> T.geq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.GreaterEqual)

// symbol and constant

infix fun <T : RealNumber<T>> Symbol<Linear>.ls(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Less)

infix fun <T : RealNumber<T>> Symbol<Linear>.leq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.LessEqual)

infix fun <T : RealNumber<T>> Symbol<Linear>.eq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Equal)

infix fun <T : RealNumber<T>> Symbol<Linear>.neq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Unequal)

infix fun <T : RealNumber<T>> Symbol<Linear>.gr(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Greater)

infix fun <T : RealNumber<T>> Symbol<Linear>.geq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.GreaterEqual)

infix fun <T : RealNumber<T>> T.ls(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Less)

infix fun <T : RealNumber<T>> T.leq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.LessEqual)

infix fun <T : RealNumber<T>> T.eq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Equal)

infix fun <T : RealNumber<T>> T.neq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Unequal)

infix fun <T : RealNumber<T>> T.gr(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Greater)

infix fun <T : RealNumber<T>> T.geq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.GreaterEqual)

// monomial and constant

infix fun <T : RealNumber<T>> LinearMonomial.ls(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Less)

infix fun <T : RealNumber<T>> LinearMonomial.leq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.LessEqual)

infix fun <T : RealNumber<T>> LinearMonomial.eq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Equal)

infix fun <T : RealNumber<T>> LinearMonomial.neq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Unequal)

infix fun <T : RealNumber<T>> LinearMonomial.gr(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.Greater)

infix fun <T : RealNumber<T>> LinearMonomial.geq(rhs: T) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs.toFlt64()), Sign.GreaterEqual)

infix fun <T : RealNumber<T>> T.ls(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Less)

infix fun <T : RealNumber<T>> T.leq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.LessEqual)

infix fun <T : RealNumber<T>> T.eq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Equal)

infix fun <T : RealNumber<T>> T.neq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Unequal)

infix fun <T : RealNumber<T>> T.gr(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.Greater)

infix fun <T : RealNumber<T>> T.geq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this.toFlt64()), LinearPolynomial(rhs), Sign.GreaterEqual)

// polynomial and constant

infix fun <T : RealNumber<T>> Polynomial<Linear>.ls(rhs: T) =
    LinearInequality(this, LinearPolynomial(rhs.toFlt64()), Sign.Less)

infix fun <T : RealNumber<T>> Polynomial<Linear>.leq(rhs: T) =
    LinearInequality(this, LinearPolynomial(rhs.toFlt64()), Sign.LessEqual)

infix fun <T : RealNumber<T>> Polynomial<Linear>.eq(rhs: T) =
    LinearInequality(this, LinearPolynomial(rhs.toFlt64()), Sign.Equal)

infix fun <T : RealNumber<T>> Polynomial<Linear>.neq(rhs: T) =
    LinearInequality(this, LinearPolynomial(rhs.toFlt64()), Sign.Unequal)

infix fun <T : RealNumber<T>> Polynomial<Linear>.gr(rhs: T) =
    LinearInequality(this, LinearPolynomial(rhs.toFlt64()), Sign.Greater)

infix fun <T : RealNumber<T>> Polynomial<Linear>.geq(rhs: T) =
    LinearInequality(this, LinearPolynomial(rhs.toFlt64()), Sign.GreaterEqual)

infix fun <T : RealNumber<T>> T.ls(rhs: Polynomial<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), rhs, Sign.Less)

infix fun <T : RealNumber<T>> T.leq(rhs: Polynomial<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), rhs, Sign.LessEqual)

infix fun <T : RealNumber<T>> T.eq(rhs: Polynomial<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), rhs, Sign.Less)

infix fun <T : RealNumber<T>> T.neq(rhs: Polynomial<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), rhs, Sign.Unequal)

infix fun <T : RealNumber<T>> T.gr(rhs: Polynomial<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), rhs, Sign.Greater)

infix fun <T : RealNumber<T>> T.geq(rhs: Polynomial<Linear>) =
    LinearInequality(LinearPolynomial(this.toFlt64()), rhs, Sign.GreaterEqual)

// variable and variable

infix fun Item<*, *>.ls(rhs: Item<*, *>) = LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)
infix fun Item<*, *>.leq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun Item<*, *>.eq(rhs: Item<*, *>) = LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)
infix fun Item<*, *>.neq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun Item<*, *>.gr(rhs: Item<*, *>) = LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)
infix fun Item<*, *>.geq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

// symbol and variable

infix fun Symbol<Linear>.ls(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun Symbol<Linear>.leq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun Symbol<Linear>.eq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun Symbol<Linear>.neq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun Symbol<Linear>.gr(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun Symbol<Linear>.geq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

infix fun Item<*, *>.ls(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun Item<*, *>.leq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun Item<*, *>.eq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun Item<*, *>.neq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun Item<*, *>.gr(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun Item<*, *>.geq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

// monomial and variable

infix fun LinearMonomial.ls(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun LinearMonomial.leq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun LinearMonomial.eq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun LinearMonomial.neq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun LinearMonomial.gr(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun LinearMonomial.geq(rhs: Item<*, *>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

infix fun Item<*, *>.ls(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun Item<*, *>.leq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun Item<*, *>.eq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun Item<*, *>.neq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun Item<*, *>.gr(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun Item<*, *>.geq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

// polynomial and variable

infix fun Polynomial<Linear>.ls(rhs: Item<*, *>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Less)
infix fun Polynomial<Linear>.leq(rhs: Item<*, *>) = LinearInequality(this, LinearPolynomial(rhs), Sign.LessEqual)
infix fun Polynomial<Linear>.eq(rhs: Item<*, *>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Equal)
infix fun Polynomial<Linear>.neq(rhs: Item<*, *>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Unequal)
infix fun Polynomial<Linear>.gr(rhs: Item<*, *>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Greater)
infix fun Polynomial<Linear>.geq(rhs: Item<*, *>) = LinearInequality(this, LinearPolynomial(rhs), Sign.GreaterEqual)

infix fun Item<*, *>.ls(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Less)
infix fun Item<*, *>.leq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.LessEqual)
infix fun Item<*, *>.eq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Equal)
infix fun Item<*, *>.neq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Unequal)
infix fun Item<*, *>.gr(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Greater)
infix fun Item<*, *>.geq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.GreaterEqual)

// symbol and symbol

infix fun Symbol<Linear>.ls(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun Symbol<Linear>.leq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun Symbol<Linear>.eq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun Symbol<Linear>.neq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun Symbol<Linear>.gr(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun Symbol<Linear>.geq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

// monomial and symbol

infix fun LinearMonomial.ls(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun LinearMonomial.leq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun LinearMonomial.eq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun LinearMonomial.neq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun LinearMonomial.gr(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun LinearMonomial.geq(rhs: Symbol<Linear>) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

infix fun Symbol<Linear>.ls(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun Symbol<Linear>.leq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun Symbol<Linear>.eq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun Symbol<Linear>.neq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun Symbol<Linear>.gr(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun Symbol<Linear>.geq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

// polynomial and symbol

infix fun Polynomial<Linear>.ls(rhs: Symbol<Linear>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Less)
infix fun Polynomial<Linear>.leq(rhs: Symbol<Linear>) = LinearInequality(this, LinearPolynomial(rhs), Sign.LessEqual)
infix fun Polynomial<Linear>.eq(rhs: Symbol<Linear>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Equal)
infix fun Polynomial<Linear>.neq(rhs: Symbol<Linear>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Unequal)
infix fun Polynomial<Linear>.gr(rhs: Symbol<Linear>) = LinearInequality(this, LinearPolynomial(rhs), Sign.Greater)
infix fun Polynomial<Linear>.geq(rhs: Symbol<Linear>) = LinearInequality(this, LinearPolynomial(rhs), Sign.GreaterEqual)

infix fun Symbol<Linear>.ls(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Less)
infix fun Symbol<Linear>.leq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.LessEqual)
infix fun Symbol<Linear>.eq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Equal)
infix fun Symbol<Linear>.neq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Unequal)
infix fun Symbol<Linear>.gr(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Greater)
infix fun Symbol<Linear>.geq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.GreaterEqual)

// monomial and monomial

infix fun LinearMonomial.ls(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Less)

infix fun LinearMonomial.leq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.LessEqual)

infix fun LinearMonomial.eq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Equal)

infix fun LinearMonomial.neq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Unequal)

infix fun LinearMonomial.gr(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.Greater)

infix fun LinearMonomial.geq(rhs: LinearMonomial) =
    LinearInequality(LinearPolynomial(this), LinearPolynomial(rhs), Sign.GreaterEqual)

// polynomial and monomial

infix fun Polynomial<Linear>.ls(rhs: LinearMonomial) = LinearInequality(this, LinearPolynomial(rhs), Sign.Less)
infix fun Polynomial<Linear>.leq(rhs: LinearMonomial) = LinearInequality(this, LinearPolynomial(rhs), Sign.LessEqual)
infix fun Polynomial<Linear>.eq(rhs: LinearMonomial) = LinearInequality(this, LinearPolynomial(rhs), Sign.Equal)
infix fun Polynomial<Linear>.neq(rhs: LinearMonomial) = LinearInequality(this, LinearPolynomial(rhs), Sign.Unequal)
infix fun Polynomial<Linear>.gr(rhs: LinearMonomial) = LinearInequality(this, LinearPolynomial(rhs), Sign.Greater)
infix fun Polynomial<Linear>.geq(rhs: LinearMonomial) = LinearInequality(this, LinearPolynomial(rhs), Sign.GreaterEqual)

infix fun LinearMonomial.ls(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Less)
infix fun LinearMonomial.leq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.LessEqual)
infix fun LinearMonomial.eq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Equal)
infix fun LinearMonomial.neq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Unequal)
infix fun LinearMonomial.gr(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.Greater)
infix fun LinearMonomial.geq(rhs: Polynomial<Linear>) = LinearInequality(LinearPolynomial(this), rhs, Sign.GreaterEqual)

// polynomial and polynomial

infix fun Polynomial<Linear>.ls(rhs: Polynomial<Linear>) = LinearInequality(this, rhs, Sign.Less)
infix fun Polynomial<Linear>.leq(rhs: Polynomial<Linear>) = LinearInequality(this, rhs, Sign.LessEqual)
infix fun Polynomial<Linear>.eq(rhs: Polynomial<Linear>) = LinearInequality(this, rhs, Sign.Equal)
infix fun Polynomial<Linear>.neq(rhs: Polynomial<Linear>) = LinearInequality(this, rhs, Sign.Unequal)
infix fun Polynomial<Linear>.gr(rhs: Polynomial<Linear>) = LinearInequality(this, rhs, Sign.Greater)
infix fun Polynomial<Linear>.geq(rhs: Polynomial<Linear>) = LinearInequality(this, rhs, Sign.GreaterEqual)
