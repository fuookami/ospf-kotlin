@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 通过代码和工号标识的飞行员（具有池化实例管理）。A pilot identified by code and worker number, with pooled instance management.
 *
 * @property code 参数。
 * @property override val workerNo 参数。
 * @property override val name 参数。
 * @property override val displayName 参数。
 * @property override val nationality 参数。
 */
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

        /**
         * Retrieves a [Pilot] by pilot code from the pool.
 *
         * @param code 参数。
         * @return 返回结果。
         */
        operator fun invoke(code: PilotCode): Pilot? {
            return pool[code]
        }

        /**
         * Retrieves a [Pilot] by worker number from the pool.
 *
         * @param workerNo 参数。
         * @return 返回结果。
         */
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
