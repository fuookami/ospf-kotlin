package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

/**
 * Biological cargo compatibility rules, defining which cargo types cannot be
 * placed adjacent to each other, in the same bulk position, or which types are
 * restricted from bulk stowage entirely.
 * 生物货物兼容性规则，定义哪些货物类型不能相邻放置、不能放在同一散货位置，
 * 或哪些类型完全禁止散货装载。
 *
 * @property adjacentLimit pairs of cargo types that must not be placed in adjacent positions / 不能放在相邻位置的货物类型对
 * @property bulkConflictLimit pairs of cargo types that must not share the same bulk position / 不能共享同一散货位置的货物类型对
 * @property bulkLimit cargo types that are restricted from bulk stowage / 被限制进行散货装载的货物类型
*/
data class BiologicalLimit(
    val adjacentLimit: List<Pair<CargoType, CargoType>>,
    val bulkConflictLimit: List<Pair<CargoType, CargoType>>,
    val bulkLimit: List<CargoType>
)
