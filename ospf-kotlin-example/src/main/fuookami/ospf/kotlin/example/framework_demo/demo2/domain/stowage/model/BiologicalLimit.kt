package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

data class BiologicalLimit(
    val adjacentLimit: List<Pair<CargoType, CargoType>>,
    val bulkConflictLimit: List<Pair<CargoType, CargoType>>,
    val bulkLimit: List<CargoType>
)
