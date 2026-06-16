package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Defines the loading order constraints between cargo positions, including precedence and succession relationships. */
data class LoadingOrder(
    val location: DeckLocation,
    val order: UInt8,
    val directPrec: Set<Position>,
    val directSucc: Set<Position>,
) {
    val precDepth: UInt8 by lazy {
        directPrec.minOfOrNull { it.loadingOrder.precDepth } ?: UInt8.zero
    }

    val succDepth: UInt8 by lazy {
        directSucc.minOfOrNull { it.loadingOrder.succDepth } ?: UInt8.zero
    }

    val prec: Set<Position> by lazy {
        directPrec + directPrec.flatMap { it.loadingOrder.prec }
    }

    val succ: Set<Position> by lazy {
        directSucc + directSucc.flatMap { it.loadingOrder.succ }
    }

    infix fun ls(other: Position): Boolean {
        return succ.contains(other)
    }

    infix fun leq(other: Position): Boolean {
        return !prec.contains(other)
    }

    infix fun gr(other: Position): Boolean {
        return prec.contains(other)
    }

    infix fun geq(other: Position): Boolean {
        return !succ.contains(other)
    }
}
