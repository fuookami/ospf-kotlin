package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * Defines the loading order constraints between cargo positions, including precedence and succession relationships.
 * 定义货物位置之间的装载顺序约束（包括前驱和后继关系）。
 *
 * @property location The deck location this loading order applies to. / 该装载顺序适用的甲板位置
 * @property order The linear loading order index. / 线性装载顺序索引
 * @property directPrec The set of positions that must be loaded directly before this one. / 必须在此位置之前直接装载的位置集合
 * @property directSucc The set of positions that must be loaded directly after this one. / 必须在此位置之后直接装载的位置集合
*/
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

/**
 * Checks whether this position is loaded strictly before the other position (less-than).
 * 检查此位置是否严格在另一位置之前装载（小于）。
 * @param other The position to compare against / 要比较的位置
 * @return true if the other position is in this position's successor set / 如果另一位置在此位置的后继集合中则为 true
*/

    infix fun ls(other: Position): Boolean {
        return succ.contains(other)
    }

/**
 * Checks whether this position is loaded before or at the same time as the other position (less-than-or-equal).
 * 检查此位置是否在另一位置之前或同时装载（小于等于）。
 * @param other The position to compare against / 要比较的位置
 * @return true if the other position is not in this position's predecessor set / 如果另一位置不在此位置的前驱集合中则为 true
*/

    infix fun leq(other: Position): Boolean {
        return !prec.contains(other)
    }

/**
 * Checks whether this position is loaded strictly after the other position (greater-than).
 * 检查此位置是否严格在另一位置之后装载（大于）。
 * @param other The position to compare against / 要比较的位置
 * @return true if the other position is in this position's predecessor set / 如果另一位置在此位置的前驱集合中则为 true
*/

    infix fun gr(other: Position): Boolean {
        return prec.contains(other)
    }

/**
 * Checks whether this position is loaded after or at the same time as the other position (greater-than-or-equal).
 * 检查此位置是否在另一位置之后或同时装载（大于等于）。
 * @param other The position to compare against / 要比较的位置
 * @return true if the other position is not in this position's successor set / 如果另一位置不在此位置的后继集合中则为 true
*/

    infix fun geq(other: Position): Boolean {
        return !succ.contains(other)
    }
}
