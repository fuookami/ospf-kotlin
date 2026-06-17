package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

enum class CargoCode {
    BAL,
    ELD,
    FKI,
    CCD,
    ICE,
    AVI,
    AOG,
    ELI,
    ELM,
    MAG,
    HUB,
    PER,
    YYI,
    MAT,
    RRM,
    BIG,
    OHG,
    RFG,
    RFL,
    RFS,
    ROX,
    YYE,
    HWJ,
    RRY,
    RRW,
    CVV,
    Crush,
    Stiff,
    Empty,
    Virtual
}

data class CargoType(
    val code: CargoCode?,
    val type: String
) {
    companion object {
        private val cache: MutableMap<String, CargoType> = HashMap()

        operator fun invoke(code: CargoCode): CargoType {
            return cache.getOrPut(code.name) {
                CargoType(
                    code = code,
                    type = code.name
                )
            }
        }

        operator fun invoke(name: String): CargoType {
            return cache.getOrPut(name) {
                CargoType(
                    code = CargoCode.entries.firstOrNull { it.name == name },
                    type = name,
                )
            }
        }
    }
}

enum class CargoPriorityCategory {
    High,
    Normal,
    Low
}

data class CargoPriority(
    val name: String,
    val priority: UInt64,
    val category: CargoPriorityCategory,
    val transfer: Boolean = false
)

infix fun CargoPriority.ord(rhs: CargoPriority): Order {
    return orderOf(this.priority compareTo rhs.priority)
}
