@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

sealed interface AbstractCrewMan {
    val workerNo: WorkerNo?
    val name: String
    val displayName: String?
    val nationality: String
}

data class CrewMan(
    override val workerNo: WorkerNo,
    override val name: String,
    override val displayName: String? = null,
    override val nationality: String,
) : AbstractCrewMan {
    companion object {
        private val pool = HashMap<WorkerNo, CrewMan>()
        val values by pool::values

        operator fun invoke(workerNo: WorkerNo): CrewMan? {
            return pool[workerNo]
        }
    }

    init {
        pool[workerNo] = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CrewMan) return false

        if (workerNo != other.workerNo) return false

        return true
    }

    override fun hashCode(): Int {
        return workerNo.hashCode()
    }

    override fun toString(): String {
        return displayName ?: name
    }
}