@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import fuookami.ospf.kotlin.utils.concept.*

/**
 * Enumerates the passenger classes with their index and short string representation.
 * 中文枚举舱位等级，具有索引和短字符串表示。
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
         * 将基础设施层的 PassengerClass 转换为此枚举。/ Converts an infrastructure [fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.PassengerClass] to this enum.
 *
         * @param cls 参数。
         * @return 返回结果。
         */
        operator fun invoke(cls: fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.PassengerClass): PassengerClass {
            return valueOf(cls.cls)
        }
    }

    abstract fun toShortString(): String
}
