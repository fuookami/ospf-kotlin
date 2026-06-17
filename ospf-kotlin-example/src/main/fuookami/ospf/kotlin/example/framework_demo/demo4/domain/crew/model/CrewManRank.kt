@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 枚举预定义的机组人员职级类别及其职级编号。Enumerates the predefined crew man rank classes with their rank numbers. */
enum class CrewManRankClass(val no: CrewManRankNo) {
    PrivateAttendant(CrewManRankNo("S002")),
    Attendant(CrewManRankNo("S001")),
    Maintainer(CrewManRankNo("M001")),
    CoMaintainer(CrewManRankNo("M002"))
}

/**
 * 具有可选类别、编号、名称和池化实例管理的机组人员职级。A crew man rank with optional class, number, name, and pooled instance management.
 *
 * @property cls 参数。
 * @property no 参数。
 * @property name 参数。
 * @property displayName 参数。
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
         * Retrieves a [CrewManRank] by class from the pool.
 *
         * @param cls 参数。
         * @return 返回结果。
         */
        operator fun invoke(cls: CrewManRankClass): CrewManRank? {
            return pool[cls.no]
        }

        /**
         * Retrieves a [CrewManRank] by rank number from the pool.
 *
         * @param no 参数。
         * @return 返回结果。
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
