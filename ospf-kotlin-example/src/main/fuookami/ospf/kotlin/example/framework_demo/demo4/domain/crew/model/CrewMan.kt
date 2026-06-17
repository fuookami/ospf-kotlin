@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 具有工号和身份信息的机组人员实体的密封接口。Sealed interface for crew man entities with worker number and identity information. */
sealed interface AbstractCrewMan {
    val workerNo: WorkerNo?
    val name: String
    val displayName: String?
    val nationality: String
}

/**
 * 通过工号标识的非飞行员机组成员（具有池化实例管理）。A non-pilot crew member identified by worker number, with pooled instance management.
 *
 */
data class CrewMan(
    override val workerNo: WorkerNo,
    override val name: String,
    override val displayName: String? = null,
    override val nationality: String,
) : AbstractCrewMan {
    companion object {
        private val pool = HashMap<WorkerNo, CrewMan>()
        val values by pool::values

        /**
         * Retrieves a [CrewMan] by worker number from the pool.
 *
         * @param workerNo 参数。
         * @return 返回结果。
         */
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
