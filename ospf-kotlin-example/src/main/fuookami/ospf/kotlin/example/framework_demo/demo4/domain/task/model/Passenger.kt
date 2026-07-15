@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import fuookami.ospf.kotlin.utils.concept.*

/**
 * Enumerates the passenger classes with their index and short string representation.
 * 枚举舱位等级，具有索引和短字符串表示。
*/
enum class PassengerClass: Indexed {
    First {
        override fun toShortString() = "F"
    },

    Business {
        override fun toShortString() = "B"
    },

    Economy {
        override fun toShortString() = "E"
    };

    override val index: Int get() = this.ordinal

    companion object {
        /**
         * Converts an infrastructure [fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.PassengerClass] to this enum.
         * 将基础设施层的 PassengerClass 转换为此枚举。
         *
         * @param cls The infrastructure passenger class to convert / 要转换的基础设施舱位等级
         * @return The corresponding domain passenger class enum value / 对应的领域舱位等级枚举值
        */
        operator fun invoke(cls: fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.PassengerClass): PassengerClass {
            return valueOf(cls.cls)
        }
    }

    /**
     * Returns the short string representation of this passenger class (e.g., "F", "B", "E").
     * 返回此舱位等级的短字符串表示（如 "F"、"B"、"E"）。
     * @return The short string code for this passenger class / 此舱位等级的短字符串代码
    */
    abstract fun toShortString(): String
}
