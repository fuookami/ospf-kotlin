@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 枚举预定义的机组人员职级类别及其职级编号。Enumerates the predefined crew man rank classes with their rank numbers.
 *
 * @property no 职级编号 / the rank number
*/
enum class CrewManRankClass(val no: CrewManRankNo) {
    /** Private attendant rank / 普通乘务员职级 */
    PrivateAttendant(CrewManRankNo("S002")),
    /** Attendant rank / 乘务员职级 */
    Attendant(CrewManRankNo("S001")),
    /** Maintainer rank / 维护人员职级 */
    Maintainer(CrewManRankNo("M001")),
    /** Co-maintainer rank / 副维护人员职级 */
    CoMaintainer(CrewManRankNo("M002"))
}

/**
 * 具有可选类别、编号、名称和池化实例管理的机组人员职级。A crew man rank with optional class, number, name, and pooled instance management.
 *
 * @property cls 职级类别 / Rank class category
 * @property no 职级编号 / Rank number
 * @property name 职级名称 / Rank name
 * @property displayName 职级显示名称 / Display name for the rank
*/
data class CrewManRank(
    val cls: CrewManRankClass?,
    val no: CrewManRankNo,
    val name: String,
    val displayName: String? = null
) {
    companion object {
        private val pool = HashMap<CrewManRankNo, CrewManRank>()
        val values by pool::values

        /**
         * 通过类别从池中获取机组人员职级。Retrieves a [CrewManRank] by class from the pool.
         *
         * @param cls 要查找的职级类别 / Rank class to look up
         * @return 匹配的机组人员职级，未找到则返回 null / The matching CrewManRank, or null if not found
        */
        operator fun invoke(cls: CrewManRankClass): CrewManRank? {
            return pool[cls.no]
        }

        /**
         * 通过职级编号从池中获取机组人员职级。Retrieves a [CrewManRank] by rank number from the pool.
         *
         * @param no 要查找的职级编号 / Rank number to look up
         * @return 匹配的机组人员职级，未找到则返回 null / The matching CrewManRank, or null if not found
        */
        operator fun invoke(no: CrewManRankNo): CrewManRank? {
            return pool[no]
        }
    }

    init {
        pool[no] = this
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CrewManRank) return false

        if (cls != other.cls) return false
        if (no != other.no) return false

        return true
    }

    override fun hashCode(): Int {
        return cls?.hashCode() ?: no.hashCode()
    }
}
