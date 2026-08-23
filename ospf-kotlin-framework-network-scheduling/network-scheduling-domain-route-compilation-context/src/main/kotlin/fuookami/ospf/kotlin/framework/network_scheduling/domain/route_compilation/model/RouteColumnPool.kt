package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*

/**
 * 路线列池。 / Route column pool.
 *
 * 按"车辆类型 ID + 有序节点序列"签名去重，禁止同一路线的重复变量破坏整数性判断。
 * 在无平行弧前提下，有序节点序列唯一确定了弧序列，因此节点序列去重
 * 与合同要求的"车辆类型 ID + 有序节点/弧序列"去重语义等价。 / Deduplicates by "vehicle type ID + ordered node sequence" signature, preventing
 * duplicate route variables from breaking integrality judgments.
 * Without parallel arcs, an ordered node sequence uniquely determines
 * the arc sequence, making node-sequence deduplication semantically equivalent
 * to the contract's "vehicle type ID + ordered node/arc sequence" deduplication.
 */
class RouteColumnPool<V : RealNumber<V>> {

    private val _routes: MutableList<Route<V>> = mutableListOf()
    private val _signatures: MutableSet<String> = mutableSetOf()

    /** 当前池中路线快照 / Snapshot of routes currently in the pool */
    val routes: List<Route<V>> get() = _routes.toList()

    /** 当前池中路线数量 / Number of routes currently in the pool */
    val size: Int get() = _routes.size

    /**
     * 添加路线列，去重。 / Add route columns, with deduplication.
     *
     * @param newRoutes 待添加路线 / Routes to add
     * @return 去重后实际添加的路线 / Actually added routes after deduplication
     */
    fun addColumns(newRoutes: List<Route<V>>): List<Route<V>> {
        val added = mutableListOf<Route<V>>()
        for (route in newRoutes) {
            if (_signatures.add(route.signature)) {
                _routes.add(route)
                added.add(route)
            }
        }
        return added.toList()
    }

    /**
     * 移除指定路线。 / Remove specified routes.
     *
     * @param routesToRemove 待移除路线 / Routes to remove
     */
    fun removeColumns(routesToRemove: Set<Route<V>>) {
        _routes.removeAll(routesToRemove.toSet())
        _signatures.removeAll(routesToRemove.map { it.signature }.toSet())
    }

    /**
     * 检查路线签名是否已存在。 / Check if a route signature already exists.
     */
    fun contains(signature: String): Boolean = _signatures.contains(signature)

    /**
     * 过滤与 BranchMask 兼容的路线。 / Filter routes compatible with a BranchMask.
     *
     * @param predicate 兼容性谓词 / Compatibility predicate
     * @return 兼容路线列表 / Compatible routes
     */
    fun filterCompatible(predicate: (Route<V>) -> Boolean): List<Route<V>> {
        return _routes.filter(predicate)
    }
}
