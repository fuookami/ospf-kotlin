@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 枚举预定义的飞行员职级类别及其职级编号。Enumerates the predefined pilot rank classes with their rank numbers.
 *
 * @property no 职级编号 / the rank number
*/
enum class PilotRankClass(val no: PilotRankNo) {
    /** Captain / 机长 */
    Captain(PilotRankNo("A001")),
    /** Second in command / 副驾驶 */
    SecondInCommand(PilotRankNo("A002")),
    /** Cruise captain / 巡航机长 */
    CruiseCaptain(PilotRankNo("B001")),
    /** First officer / 高级副驾驶 */
    FirstOfficer(PilotRankNo("C001")),
    /** Student pilot in command / 学员机长 */
    StudentPilotInCommand(PilotRankNo("J001")),
    /** Pilot monitor / 监控飞行员 */
    PilotMonitor(PilotRankNo("F001")),
    /** Pilot observer / 观察飞行员 */
    PilotObserver(PilotRankNo("K001")),
}

/**
 * 具有可选类别、编号、名称和池化实例管理的飞行员职级。A pilot rank with optional class, number, name, and pooled instance management.
 *
 * @property cls 职级类别 / Rank class category
 * @property no 职级编号 / Rank number
 * @property name 职级名称 / Rank name
 * @property displayName 职级显示名称 / Display name for the rank
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
         * @param cls 要查找的职级类别 / Rank class to look up
         * @return 匹配的飞行员职级，未找到则返回 null / The matching PilotRank, or null if not found
        */
        operator fun invoke(cls: PilotRankClass): PilotRank? {
            return pool[cls.no]
        }

        /**
         * 通过职级编号从池中获取飞行员职级。Retrieves a [PilotRank] by rank number from the pool.
         *
         * @param no 要查找的职级编号 / Rank number to look up
         * @return 匹配的飞行员职级，未找到则返回 null / The matching PilotRank, or null if not found
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
