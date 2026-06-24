@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 枚举预定义的飞行员职级类别及其职级编号。Enumerates the predefined pilot rank classes with their rank numbers. */
enum class PilotRankClass(val no: PilotRankNo) {
    Captain(PilotRankNo("A001")),
    SecondInCommand(PilotRankNo("A002")),
    CruiseCaptain(PilotRankNo("B001")),
    FirstOfficer(PilotRankNo("C001")),
    StudentPilotInCommand(PilotRankNo("J001")),
    PilotMonitor(PilotRankNo("F001")),
    PilotObserver(PilotRankNo("K001")),
}

/**
 * 具有可选类别、编号、名称和池化实例管理的飞行员职级。A pilot rank with optional class, number, name, and pooled instance management.
 *
 * @property cls 参数。
 * @property no 参数。
 * @property name 参数。
 * @property displayName 参数。
 */
data class PilotRank(
    val cls: PilotRankClass?,
    val no: PilotRankNo,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        private val pool = HashMap<PilotRankNo, PilotRank>()
        val values by pool::values

        /**
         * 通过类别从池中获取飞行员职级。Retrieves a [PilotRank] by class from the pool.
 *
         * @param cls 参数。
         * @return 返回结果。
         */
        operator fun invoke(cls: PilotRankClass): PilotRank? {
            return pool[cls.no]
        }

        /**
         * 通过职级编号从池中获取飞行员职级。Retrieves a [PilotRank] by rank number from the pool.
 *
         * @param no 参数。
         * @return 返回结果。
         */
        operator fun invoke(no: PilotRankNo): PilotRank? {
            return pool[no]
        }
    }

    init {
        pool[no] = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PilotRank) return false

        if (cls != other.cls) return false
        if (no != other.no) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cls?.hashCode() ?: 0
        result = 31 * result + no.hashCode()
        return result
    }

    override fun toString(): String {
        return displayName ?: name
    }
}
