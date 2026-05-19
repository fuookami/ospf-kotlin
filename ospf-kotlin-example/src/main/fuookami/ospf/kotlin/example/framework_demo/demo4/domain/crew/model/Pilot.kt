@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

data class Pilot(
    val code: PilotCode,
    override val workerNo: WorkerNo,
    override val name: String,
    override val displayName: String? = null,
    override val nationality: String,
) : AbstractCrewMan {
    companion object {
        private val pool = HashMap<PilotCode, Pilot>()
        val values by pool::values

        operator fun invoke(code: PilotCode): Pilot? {
            return pool[code]
        }

        operator fun invoke(workerNo: WorkerNo): Pilot? {
            return pool.values.find { it.workerNo == workerNo }
        }
    }

    init {
        pool[code] = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pilot) return false

        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun toString(): String {
        return displayName ?: name
    }
}