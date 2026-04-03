package fuookami.ospf.kotlin.math.symbol.parser

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import kotlinx.serialization.Serializable

@Serializable
sealed interface Expr {
    @Serializable
    data class NumberLiteral(val text: String) : Expr

    @Serializable
    data class Identifier(val name: String) : Expr

    @Serializable
    data class UnaryMinus(val operand: Expr) : Expr

    @Serializable
    data class Binary(val left: Expr, val operator: BinaryOperator, val right: Expr) : Expr

    @Serializable
    data class FunctionCall(val name: String, val arguments: List<Expr>) : Expr

    @Serializable
    data class Comparison(val left: Expr, val operator: ComparisonOperator, val right: Expr) : Expr
}

@Serializable
enum class BinaryOperator {
    Add,
    Subtract,
    Multiply,
    Power
}

@Serializable
enum class ComparisonOperator {
    Less,
    LessEqual,
    Equal,
    NotEqual,
    GreaterEqual,
    Greater
}

