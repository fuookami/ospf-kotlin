package fuookami.ospf.kotlin.core.frontend.inequality

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*

class QuadraticInequality(
    override val lhs: AbstractQuadraticPolynomial<*>,
    override val rhs: AbstractQuadraticPolynomial<*>,
    sign: Sign,
    name: String = "",
    displayName: String? = null
) : Inequality<QuadraticInequality, QuadraticMonomialCell>(lhs, rhs, sign, name, displayName) {
    companion object {
        operator fun invoke(inequality: LinearInequality): QuadraticInequality {
            return QuadraticInequality(
                QuadraticPolynomial(inequality.lhs),
                QuadraticPolynomial(inequality.rhs),
                inequality.sign,
                inequality.name,
                inequality.displayName
            )
        }
    }

    override val cells: List<QuadraticMonomialCell>
        get() {
            if (_cells.isEmpty()) {
                val cells = HashMap<Pair<VariableItemKey, VariableItemKey?>, QuadraticMonomialCell>()
                var constant = Flt64.zero
                for (cell in lhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            val key = Pair(symbol.value.variable1.key, symbol.value.variable2?.key)
                            cells[key] = cells[key]?.let { it + cell } ?: cell
                        }

                        is Either.Right -> {
                            constant += symbol.value
                        }
                    }
                }
                for (cell in rhs.cells) {
                    when (val symbol = cell.cell) {
                        is Either.Left -> {
                            val key = Pair(symbol.value.variable1.key, symbol.value.variable2?.key)
                            cells[key] = cells[key]?.let { it - cell } ?: -cell
                        }

                        is Either.Right -> {
                            constant -= symbol.value
                        }
                    }
                }
                val ret = cells.map { it.value }.toMutableList()
                ret.add(QuadraticMonomialCell(constant))
                _cells = ret
            }
            return _cells
        }

    override fun reverse(name: String?, displayName: String?): QuadraticInequality {
        return QuadraticInequality(
            lhs = QuadraticPolynomial(lhs.monomials.map { it.copy() }),
            rhs = QuadraticPolynomial(rhs.monomials.map { it.copy() }),
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

    override fun normalize(): QuadraticInequality {
        return QuadraticInequality(
            lhs = QuadraticPolynomial(lhs.monomials.map { it.copy() } + rhs.monomials.map { -it }),
            rhs = QuadraticPolynomial(-lhs.constant + rhs.constant),
            sign = sign,
            name = name,
            displayName = displayName
        )
    }
}

// symbol and constant

infix fun QuadraticIntermediateSymbol.ls(rhs: Int): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.ls(rhs: Double): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.ls(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: Int): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.leq(rhs: Double): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.leq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: Int): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.eq(rhs: Double): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.eq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: Int): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.neq(rhs: Double): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.neq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: Int): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.gr(rhs: Double): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.gr(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: Int): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun QuadraticIntermediateSymbol.geq(rhs: Double): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticIntermediateSymbol.geq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun Int.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// monomial and constant

infix fun QuadraticMonomial.ls(rhs: Int): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun QuadraticMonomial.ls(rhs: Double): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.ls(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: Int): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun QuadraticMonomial.leq(rhs: Double): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.leq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: Int): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun QuadraticMonomial.eq(rhs: Double): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.eq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: Int): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun QuadraticMonomial.neq(rhs: Double): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.neq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: Int): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun QuadraticMonomial.gr(rhs: Double): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.gr(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: Int): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun QuadraticMonomial.geq(rhs: Double): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> QuadraticMonomial.geq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.toFlt64()),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun Int.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.toFlt64()),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and constant

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Int): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: Double): QuadraticInequality {
    return this.ls(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.ls(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Int): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: Double): QuadraticInequality {
    return this.leq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.leq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Int): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: Double): QuadraticInequality {
    return this.eq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.eq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Int): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: Double): QuadraticInequality {
    return this.neq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.neq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Int): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: Double): QuadraticInequality {
    return this.gr(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.gr(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Int): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: Double): QuadraticInequality {
    return this.geq(Flt64(rhs))
}

infix fun <T : RealNumber<T>> AbstractQuadraticPolynomial<*>.geq(rhs: T): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun Int.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun Double.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).ls(rhs)
}

infix fun <T : RealNumber<T>> T.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun Int.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun Double.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).leq(rhs)
}

infix fun <T : RealNumber<T>> T.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun Int.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun Double.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).eq(rhs)
}

infix fun <T : RealNumber<T>> T.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun Int.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun Double.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).neq(rhs)
}

infix fun <T : RealNumber<T>> T.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun Int.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun Double.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).gr(rhs)
}

infix fun <T : RealNumber<T>> T.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun Int.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun Double.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return Flt64(this).geq(rhs)
}

infix fun <T : RealNumber<T>> T.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// symbol and variable

infix fun QuadraticIntermediateSymbol.ls(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// monomial and variable

infix fun QuadraticMonomial.ls(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and variable

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractVariableItem<*, *>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractVariableItem<*, *>.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractVariableItem<*, *>.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractVariableItem<*, *>.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractVariableItem<*, *>.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractVariableItem<*, *>.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractVariableItem<*, *>.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// symbol and symbol

infix fun LinearIntermediateSymbol.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

// monomial and symbol

infix fun LinearMonomial.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearIntermediateSymbol.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and symbol

infix fun AbstractLinearPolynomial<*>.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: LinearIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearIntermediateSymbol.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearIntermediateSymbol.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearIntermediateSymbol.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearIntermediateSymbol.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearIntermediateSymbol.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearIntermediateSymbol.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: QuadraticIntermediateSymbol): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticIntermediateSymbol.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun QuadraticIntermediateSymbol.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun QuadraticIntermediateSymbol.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun QuadraticIntermediateSymbol.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun QuadraticIntermediateSymbol.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun QuadraticIntermediateSymbol.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// monomial and monomial

infix fun LinearMonomial.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}


infix fun QuadraticMonomial.ls(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

// polynomial and monomial

infix fun AbstractLinearPolynomial<*>.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: LinearMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun LinearMonomial.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun LinearMonomial.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun LinearMonomial.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun LinearMonomial.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun LinearMonomial.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun LinearMonomial.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: QuadraticMonomial): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs.copy()),
        Sign.GreaterEqual
    )
}

infix fun QuadraticMonomial.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Less
    )
}

infix fun QuadraticMonomial.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun QuadraticMonomial.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun QuadraticMonomial.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun QuadraticMonomial.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun QuadraticMonomial.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this.copy()),
        rhs.copy(),
        Sign.GreaterEqual
    )
}

// polynomial and polynomial

infix fun AbstractLinearPolynomial<*>.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractLinearPolynomial<*>.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractLinearPolynomial<*>.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractLinearPolynomial<*>.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractLinearPolynomial<*>.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractLinearPolynomial<*>.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        QuadraticPolynomial(this),
        rhs.copy(),
        Sign.GreaterEqual
    )
}


infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractLinearPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        QuadraticPolynomial(rhs),
        Sign.GreaterEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.ls(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Less
    )
}

infix fun AbstractQuadraticPolynomial<*>.leq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.LessEqual
    )
}

infix fun AbstractQuadraticPolynomial<*>.eq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Equal
    )
}

infix fun AbstractQuadraticPolynomial<*>.neq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Unequal
    )
}

infix fun AbstractQuadraticPolynomial<*>.gr(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.Greater
    )
}

infix fun AbstractQuadraticPolynomial<*>.geq(rhs: AbstractQuadraticPolynomial<*>): QuadraticInequality {
    return QuadraticInequality(
        this.copy(),
        rhs.copy(),
        Sign.GreaterEqual
    )
}
