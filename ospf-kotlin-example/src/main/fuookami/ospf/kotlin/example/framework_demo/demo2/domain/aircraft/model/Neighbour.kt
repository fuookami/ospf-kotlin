package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.utils.functional.*

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

typealias PositionPair = Pair<Position, Position>

val PositionPair.symmetrical get() = PositionPair(second, first)

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
