package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

class LinearInequality(
    override val lhs: AbstractLinearPolynomial<*>,
    override val rhs: AbstractLinearPolynomial<*>,
    sign: Sign,
    name: String = "",
    displayName: String? = null
) : Inequality<LinearInequality, LinearMonomialCell>(lhs, rhs, sign, name, displayName) {
    override val cells: List<LinearMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                val cells = HashMap<VariableItemKey, LinearMonomialCell>()
                var constant = Flt64.zero
                for (cell in lhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            cells[symbol.value.variable.key] = cells[symbol.value.variable.key]?.let { it + cell } ?: cell
                        }

                        is Either.Right -> {
                            constant += symbol.value
                        }
                    }
                }
                for (cell in rhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            cells[symbol.value.variable.key] = cells[symbol.value.variable.key]?.let { it - cell } ?: -cell
                        }

                        is Either.Right -> {
                            constant -= symbol.value
                        }
                    }
                }
                val ret = cells.map { it.value }.toMutableList()
                ret.add(LinearMonomialCell(constant))
                _cells = ret
            }
            return _cells
        }

    override fun reverse(name: String?, displayName: String?): LinearInequality {
        return LinearInequality(
            lhs = LinearPolynomial(lhs.monomials.map { it.copy() }),
            rhs = LinearPolynomial(rhs.monomials.map { it.copy() }),
            sign = sign.reverse,
            name = name ?: this.name.let {
                if (it.isNotEmpty()) {
                    "not_$it"
                } else {
                    null
                }
            } ?: "",
            displayName = displayName ?: this.displayName?.let {
                if (it.isNotEmpty()) {
                    "not_$it"
                } else {
                    null
                }
            }
        )
    }

    override fun normalize(): LinearInequality {
        return LinearInequality(
            lhs = LinearPolynomial(lhs.monomials.map { it.copy() } + rhs.monomials.map { -it }),
            rhs = LinearPolynomial(-lhs.constant + rhs.constant),
            sign = sign,
            name = name,
            displayName = displayName
        )
    }
}

// variable and constant

infix fun AbstractVariableItem<*, *>.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.ls(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.leq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.eq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.neq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}


infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.gr(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun AbstractVariableItem<*, *>.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractVariableItem<*, *>.geq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun Int.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// symbol and constant

infix fun LinearSymbol.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun LinearSymbol.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearSymbol.ls(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun LinearSymbol.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun LinearSymbol.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearSymbol.leq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun LinearSymbol.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun LinearSymbol.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearSymbol.eq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun LinearSymbol.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun LinearSymbol.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearSymbol.neq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun LinearSymbol.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun LinearSymbol.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearSymbol.gr(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun LinearSymbol.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun LinearSymbol.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearSymbol.geq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun Int.leq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: LinearSymbol): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// monomial and constant

infix fun LinearMonomial.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun LinearMonomial.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.ls(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun LinearMonomial.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.leq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun LinearMonomial.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.eq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun LinearMonomial.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.neq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun LinearMonomial.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.gr(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun LinearMonomial.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> LinearMonomial.geq(rhs: T): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun Int.leq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: LinearMonomial): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.toFlt64()),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and constant

infix fun AbstractLinearPolynomial<*>.ls(rhs: Int): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.ls(rhs: Double): LinearInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.ls(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: Int): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: Double): LinearInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.leq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: Int): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: Double): LinearInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.eq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: Int): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: Double): LinearInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.neq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: Int): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: Double): LinearInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.gr(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: Int): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: Double): LinearInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractLinearPolynomial<*>.geq(rhs: T): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun Int.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// variable and variable

infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// symbol and variable

infix fun LinearSymbol.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearSymbol.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearSymbol.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearSymbol.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearSymbol.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearSymbol.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// monomial and variable

infix fun LinearMonomial.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and variable

infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractVariableItem<*, *>): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// symbol and symbol

infix fun LinearSymbol.ls(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearSymbol.leq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearSymbol.eq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearSymbol.neq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearSymbol.gr(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearSymbol.geq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// monomial and symbol

infix fun LinearMonomial.ls(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearSymbol.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearSymbol.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearSymbol.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearSymbol.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearSymbol.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearSymbol.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and symbol

infix fun AbstractLinearPolynomial<*>.ls(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: LinearSymbol): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearSymbol.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearSymbol.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearSymbol.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearSymbol.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearSymbol.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearSymbol.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// monomial and monomial

infix fun LinearMonomial.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and monomial

infix fun AbstractLinearPolynomial<*>.ls(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: LinearMonomial): LinearInequality {
    return LinearInequality(
        this.copy(),
        LinearPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun LinearMonomial.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        LinearPolynomial(this.copy()),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// polynomial and polynomial

infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractLinearPolynomial<*>): LinearInequality {
    return LinearInequality(
        this.copy(),
        rhs.copy(),
        Sign.GreaterEqual
    )
}
