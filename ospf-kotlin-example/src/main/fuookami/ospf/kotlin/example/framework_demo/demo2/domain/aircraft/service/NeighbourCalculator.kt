package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service

import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Check if two positions are on the same side of the aircraft.
 * 检查两个位置是否在飞机的同一侧。
 *
 * @param position1 The first position. / 第一个位置
 * @param position2 The second position. / 第二个位置
 * @return True if both positions are on the same side. / 如果两个位置在同一侧则返回 true
*/
private fun onSameSide(
    position1: Position,
    position2: Position
): Boolean {
    return position1.coordinate.transverse
            || position2.coordinate.transverse
            || (position1.coordinate.onLeft && position2.coordinate.onLeft)
            || (position1.coordinate.onRight && position2.coordinate.onRight)
}

/**
 * Calculates physical adjacency between cargo positions on the same deck.
 * 计算同一甲板上货物位置之间的物理邻接。
*/
data object PhysicalNeighbourCalculator {
    operator fun invoke(
        decks: List<Deck>
    ): Ret<List<Neighbour>> {
        val neighbours = ArrayList<Neighbour>()
        for (deck in decks) {
            if (deck.positions.size <= 1) {
                continue
            }

            for ((i, position1) in deck.positions.withIndex()) {
                for (j in (i + 1) until deck.positions.size) {
                    val position2 = deck.positions[j]

                    if (onSameLine(position1, position2) && !onSameSide(position1, position2)) {
                        neighbours.add(Neighbour(NeighbourType.Physics, position1 to position2))
                    } else if (onSameSide(position1, position2)) {
                        val frontArm = min(position1.coordinate.backArm, position2.coordinate.backArm)!!
                        val backArm = max(position1.coordinate.frontArm, position2.coordinate.frontArm)!!
                        if (deck.positions.none { otherPosition -> otherPosition.coordinate.between(frontArm, backArm) }) {
                            neighbours.add(Neighbour(NeighbourType.Physics, position1 to position2))
                        }
                    }
                }
            }
        }
        return Ok(neighbours)
    }

    /**
     * Check if two positions are on the same longitudinal line.
     * 检查两个位置是否在同一纵线上。
     *
     * @param position1 The first position. / 第一个位置
     * @param position2 The second position. / 第二个位置
     * @return True if both positions are on the same line. / 如果两个位置在同一条线上则返回 true
    */
    private fun onSameLine(
        position1: Position,
        position2: Position
    ): Boolean {
        return (position1.coordinate.on(position2.coordinate.longitudinalArm)
                || position2.coordinate.on(position1.coordinate.longitudinalArm)
        ) && (position1.coordinate.onLeft && position2.coordinate.onLeft)
    }
}

/**
 * Calculates indirect physical adjacency through shared physical neighbours.
 * 通过共享物理邻居计算间接物理邻接。
*/
data object IndirectPhysicsNeighbourCalculator {
    private val nearEnoughLongitudinalRatio = Flt64(0.5)

    operator fun invoke(
        decks: List<Deck>,
        physicalNeighbours: List<Neighbour>
    ): Ret<List<Neighbour>> {
        val neighbours = ArrayList<Neighbour>()
        neighbours.addAll(
            physicalNeighbours.map {
                Neighbour(
                    type = NeighbourType.IndirectPhysics,
                    pair = it.pair
                )
            }
        )

        val physicalNeighboursMap = tidy(physicalNeighbours)
        for (deck in decks) {
            for ((i, position1) in deck.positions.withIndex()) {
                val nhs = physicalNeighboursMap[position1] ?: emptyList()

                for (p in nhs.indices) {
                    val nhs1 = physicalNeighboursMap[nhs[p]] ?: emptyList()

                    for (q in (p + 1) until nhs.size) {
                        val nhs2 = physicalNeighboursMap[nhs[q]] ?: emptyList()

                        for (position2 in deck.positions.subList(i + 1, deck.positions.size).intersect(nhs1.intersect(nhs2.toSet()))) {
                            if (!onSameSide(position1, position2) && nearEnough(position1, position2) && position2 !in nhs) {
                                neighbours.add(
                                    Neighbour(
                                        type = NeighbourType.IndirectPhysics,
                                        pair = position1 to position2
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        return Ok(neighbours)
    }

    /**
     * Organize physical neighbour pairs into a bidirectional position map.
     * 将物理邻接对组织为双向位置映射。
     *
     * @param physicalNeighbours The list of physical neighbour relationships. / 物理邻接关系列表
     * @return A map from each position to its physically adjacent positions. / 从每个位置到其物理相邻位置的映射
    */
    private fun tidy(
        physicalNeighbours: List<Neighbour>
    ): Map<Position, List<Position>> {
        val map = HashMap<Position, MutableList<Position>>()
        for (neighbour in physicalNeighbours) {
            map.getOrPut(neighbour.pair.first) { ArrayList() }.add(neighbour.pair.second)
            map.getOrPut(neighbour.pair.second) { ArrayList() }.add(neighbour.pair.first)
        }
        return map
    }

    /**
     * Check if two positions are close enough longitudinally to be considered indirect neighbours.
     * 检查两个位置在纵向上是否足够接近以被视为间接邻居。
     *
     * @param position1 The first position. / 第一个位置
     * @param position2 The second position. / 第二个位置
     * @return True if the positions are near enough. / 如果位置足够接近则返回 true
    */
    private fun nearEnough(
        position1: Position,
        position2: Position
    ): Boolean {
        val distance = (nearEnoughLongitudinalRatio * min(position1.shape.length, position2.shape.length)!!)!!
        return if ((position2.coordinate.backArm leq position1.coordinate.frontArm)!!) {
            return (((position1.coordinate.frontArm - position2.coordinate.backArm)!!) leq distance)!!
        } else if ((position1.coordinate.backArm leq position2.coordinate.frontArm)!!) {
            return (((position2.coordinate.frontArm - position1.coordinate.backArm)!!) leq distance)!!
        } else {
            true
        }
    }
}

/**
 * Calculates linear loading order adjacency between consecutive positions on each deck.
 * 计算每个甲板上连续位置之间的线性装载顺序邻接。
*/
class LinearLoadingOrderNeighbourCalculator {
    operator fun invoke(
        decks: List<Deck>
    ): Ret<List<Neighbour>> {
        val neighbours = ArrayList<Neighbour>()
        for (deck in decks) {
            val positions = deck.positions.sortedBy { it.loadingOrder.order }
            if (positions.size > 1) {
                for (i in 0 until (positions.size - 1)) {
                    neighbours.add(
                        Neighbour(
                            type = NeighbourType.LinearLoadingOrder,
                            pair = positions[i] to positions[i + 1]
                        )
                    )
                }
            }
        }

        return Ok(neighbours)
    }
}

/**
 * Calculates topological loading order adjacency from direct successor relationships.
 * 从直接后继关系计算拓扑装载顺序邻接。
*/
class TopologicalLoadingOrderNeighbourCalculator {
    operator fun invoke(
        decks: List<Deck>
    ): Ret<List<Neighbour>> {
        val neighbours = ArrayList<Neighbour>()
        for (deck in decks) {
            for (position in deck.positions) {
                neighbours.addAll(
                    position.loadingOrder.directSucc.map {
                        Neighbour(
                            type = NeighbourType.TopologicalLoadingOrder,
                            pair = position to it
                        )
                    }
                )
            }
        }

        return Ok(neighbours)
    }
}
