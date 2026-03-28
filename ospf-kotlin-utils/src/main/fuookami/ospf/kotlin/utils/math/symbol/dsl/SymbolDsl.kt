package fuookami.ospf.kotlin.utils.math.symbol.dsl

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import fuookami.ospf.kotlin.utils.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.math.symbol.parser.BinaryOperator
import fuookami.ospf.kotlin.utils.math.symbol.parser.ComparisonOperator
import fuookami.ospf.kotlin.utils.math.symbol.parser.Expr

class SymbolDslScope {
    fun num(value: Number): Expr.NumberLiteral {
        return Expr.NumberLiteral(value.toString())
    }

    fun symbol(name: String): Expr.Identifier {
        return Expr.Identifier(name)
    }

    fun call(
        name: String,
        vararg arguments: Expr
    ): Expr.FunctionCall {
        return Expr.FunctionCall(name = name, arguments = arguments.toList())
    }
}

fun symbolExpr(block: SymbolDslScope.() -> Expr): Expr {
    return SymbolDslScope().block()
}

operator fun Expr.plus(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Add, rhs)
}

operator fun Expr.minus(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Subtract, rhs)
}

operator fun Expr.times(rhs: Expr): Expr {
    return Expr.Binary(this, BinaryOperator.Multiply, rhs)
}

operator fun Expr.unaryMinus(): Expr {
    return Expr.UnaryMinus(this)
}

infix fun Expr.pow(exponent: Int): Expr {
    return Expr.Binary(
        left = this,
        operator = BinaryOperator.Power,
        right = Expr.NumberLiteral(exponent.toString())
    )
}

infix fun Expr.lt(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Less, rhs)
}

infix fun Expr.le(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.LessEqual, rhs)
}

infix fun Expr.eq(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Equal, rhs)
}

infix fun Expr.ne(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.NotEqual, rhs)
}

infix fun Expr.ge(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.GreaterEqual, rhs)
}

infix fun Expr.gt(rhs: Expr): Expr.Comparison {
    return Expr.Comparison(this, ComparisonOperator.Greater, rhs)
}
