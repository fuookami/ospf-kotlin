/**
 * 拓扑算子 / Topology operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.topology

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 拓扑算子接口，定义个体之间的邻域关系。 / Topology operator interface defining the neighbourhood relation between individuals.
*/
fun interface Topology {

    /**
     * 获取邻居下标集合 / Get the set of neighbour indices
     *
     * @param index 中心个体下标 / Center individual index
     * @param size 种群规模 / Population size
     * @return 邻居下标集合 / Neighbour index set
    */
    fun neighbours(index: Int, size: Int): Set<Int>
}

/** 全局拓扑：任意个体都能看到所有个体 / Global topology: every individual sees everyone */
data object GlobalTopology : Topology {
    override fun neighbours(index: Int, size: Int): Set<Int> {
        return (0 until size).toSet()
    }
}

/** 环形拓扑：只与最近邻相连 / Ring topology: only adjacent individuals are connected */
data object RingTopology : Topology {
    override fun neighbours(index: Int, size: Int): Set<Int> {
        if (size <= 1) {
            return setOf(index)
        }
        return setOf((index - 1 + size) % size, index, (index + 1) % size)
    }
}

/**
 * k 邻环形拓扑 / k-neighbour ring topology
 *
 * @property radius 邻域半径 / Neighbourhood radius
*/
data class RingKTopology(
    val radius: Int
) : Topology {
    override fun neighbours(index: Int, size: Int): Set<Int> {
        if (size <= 1) {
            return setOf(index)
        }
        val result = HashSet<Int>()
        for (offset in -radius..radius) {
            result.add(((index + offset) % size + size) % size)
        }
        return result
    }
}

/**
 * 冯·诺依曼网格拓扑 / Von Neumann grid topology
 *
 * @property width 网格宽度 / Grid width
*/
data class VonNeumannTopology(
    val width: Int
) : Topology {
    override fun neighbours(index: Int, size: Int): Set<Int> {
        if (size <= 1 || width <= 0) {
            return setOf(index)
        }
        val row = index / width
        val column = index % width
        val result = HashSet<Int>()
        for ((rowOffset, columnOffset) in listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val neighbourRow = row + rowOffset
            val neighbourColumn = column + columnOffset
            if (neighbourRow < 0 || neighbourColumn < 0 || neighbourColumn >= width) {
                continue
            }
            val neighbour = neighbourRow * width + neighbourColumn
            if (neighbour in 0 until size) {
                result.add(neighbour)
            }
        }
        return result
    }
}

/**
 * 随机拓扑，每次查询返回随机的若干邻居 / Random topology returning random neighbours per query
 *
 * @property amount 邻居数量 / Neighbour amount
 * @property entropy 均匀分布熵源 / Uniform entropy source
*/
class RandomTopology(
    val amount: Int,
    private val entropy: Generator<Flt64>
) : Topology {
    override fun neighbours(index: Int, size: Int): Set<Int> {
        if (size <= 1) {
            return setOf(index)
        }
        val result = HashSet<Int>()
        result.add(index)
        val target = min(amount, size)
        var attempts = 0
        while (result.size < target && attempts < size * 4) {
            val candidate = floor((entropy()?.toDouble() ?: 0.0) * size).toInt().coerceIn(0, size - 1)
            result.add(candidate)
            ++attempts
        }
        return result
    }
}
