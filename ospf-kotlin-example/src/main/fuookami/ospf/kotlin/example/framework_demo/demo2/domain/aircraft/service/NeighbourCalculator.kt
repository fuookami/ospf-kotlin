package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.service


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*

/**
 * 是否在同一侧（宽体机）
 *
 * @param position1             舱位 1
 * @param position2             舱位 2
 * @return                      是否在同一侧
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
 * 物理相邻计算器
 */
data object PhysicalNeighbourCalculator {
    /**
     * 计算物理相邻
     *
     * @param decks                 舱
     * @return                      物理相邻
     */
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
                        // 如果在同一条线上且不在同一侧，则说明横向相邻
                        neighbours.add(Neighbour(NeighbourType.Physics, position1 to position2))
                    } else if (onSameSide(position1, position2)) {
                        // 如果是在同一侧，则需要判断是否有阻隔舱位，如果没有则说明纵向相邻
                        // 整个舱位都在中间才能判定为阻隔，不是整个舱位都在中间应当要判定为冲突舱位
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
     * 是否在（横向）同一条线上：两个舱位的纵向力臂范围有交集
     *
     * @param position1             舱位 1
     * @param position2             舱位 2
     * @return                      是否在同一条线上
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
 * 间接物理相邻计算器
 */
data object IndirectPhysicsNeighbourCalculator {
    private val nearEnoughLongitudinalRatio = Flt64(0.5)

    /**
     * 计算间隔物理相邻
     *
     * @param decks                 舱
     * @param physicalNeighbours    物理相邻
     * @return                      间接物理相邻
     */
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
     * 整理物理相邻
     *
     * @param physicalNeighbours        物料相邻
     * @return                          舱位-物理相邻舱位映射
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
     * 是否足够近：两个舱位的纵向距离足够近
     *
     * @param position1                 舱位 1
     * @param position2                 舱位 2
     * @return                          是否足够近
     */
    private fun nearEnough(
        position1: Position,
        position2: Position
    ): Boolean {
        val distance = nearEnoughLongitudinalRatio * min(position1.shape.length, position2.shape.length)!!
        return if ((position2.coordinate.backArm leq position1.coordinate.frontArm)!!) {
            return ((position1.coordinate.frontArm - position2.coordinate.backArm) leq distance)!!
        } else if ((position1.coordinate.backArm leq position2.coordinate.frontArm)!!) {
            return ((position2.coordinate.frontArm - position1.coordinate.backArm) leq distance)!!
        } else {
            true
        }
    }
}

/**
 * 线性装载顺序相邻计算器
 */
class LinearLoadingOrderNeighbourCalculator {
    /**
     * 计算线性装载顺序相邻
     *
     * @param decks                 舱
     * @return                      线性装载相邻
     */
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
 * 拓扑装载顺序相邻计算器
 */
class TopologicalLoadingOrderNeighbourCalculator {
    /**
     * 计算拓扑装载顺序相邻
     *
     * @param decks                 舱
     * @return                      拓扑装载相邻
     */
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

