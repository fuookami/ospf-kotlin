package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.utils.functional.*

/**
 * Types of adjacency relationships between cargo positions.
 * 货物位置之间的邻接关系类型。
*/
enum class NeighbourType {
    Physics,
    IndirectPhysics,
    LinearLoadingOrder {
        override val ordered = true
    },
    TopologicalLoadingOrder {
        override val ordered = true
    };

    open val ordered = false
}

/**
 * A pair of positions representing an ordered or unordered relationship.
 * 表示有序或无序关系的位置对。
*/
typealias PositionPair = Pair<Position, Position>

val PositionPair.symmetrical get() = PositionPair(second, first)

/**
 * An adjacency relationship between two cargo positions of a given type.
 * 给定类型的两个货物位置之间的邻接关系。
 *
 * @property type The type of neighbour relationship. / 邻接关系类型
 * @property pair The pair of positions in this relationship. / 该关系中的位置对
*/
data class Neighbour(
    val type: NeighbourType,
    val pair: PositionPair,
) : PartialEq<Neighbour>, Eq<Neighbour> {
    val ordered by type::ordered
    val symmetrical get() = Neighbour(type, pair.symmetrical)

    override fun partialEq(rhs: Neighbour): Boolean {
        return type == rhs.type && if (type.ordered) {
            pair.first == rhs.pair.first
                    && pair.second == rhs.pair.second
        } else {
            pair == rhs.pair || pair.symmetrical == rhs.pair
        }
    }
}
