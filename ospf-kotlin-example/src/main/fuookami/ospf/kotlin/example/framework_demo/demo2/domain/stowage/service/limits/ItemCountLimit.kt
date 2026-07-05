package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import java.util.*

/** 物品数量限制，用于约束货舱中装载的物品总数 / Item count limit, used to constrain the total number of items loaded in a compartment */
data class ItemCountLimit(
    /** 需要计数的物品种类列表 / List of item types to count */
    val itemTypes: List<String>,
    /** 物品数量上限 / Upper limit of item count */
    val limit: UInt64,
    /** 计数执行器，用于计算实际装载数量并转换限制值 / Counting enforcer, used to compute the actual loaded count and convert the limit value */
    private val enforcer: Enforcer = Enforcer.Default,
) {
    val name: String
        get() = "ItemCount"

    /** 计数执行器接口，定义如何计算物品数量以及如何转换限制值 / Enforcer interface defining how to count items and convert the limit value */
    fun interface Enforcer {
        /**
         * 根据分配方案计算实际物品数量 / Compute the actual item count based on the assignment plan
         * @param assignments 物品种类到数量的映射 / Mapping from item types to quantities
         * @return 实际物品数量 / Actual item count
         */
        fun invoke(assignments: Map<String, UInt64>): UInt64

        /**
         * 转换限制值，用于适配不同单位的限制 / Convert the limit value to adapt limits of different units
         * @param limit 原始限制值 / Original limit value
         * @return 转换后的限制值 / Converted limit value
         */
        fun convert(limit: UInt64): UInt64

        companion object {
            /** 默认执行器：对所有分配数量求和，且不转换限制值 / Default enforcer: sums all assigned quantities and does not convert the limit value */
            val Default: Enforcer = Enforcer { assignments ->
                var total = UInt64.Zero
                for (assignment in assignments) {
                    total += assignment.value
                }
                total
            }.also { enforcer ->
                object : Enforcer {
                    override fun invoke(assignments: Map<String, UInt64>): UInt64 {
                        return enforcer.invoke(assignments)
                    }

                    override fun convert(limit: UInt64): UInt64 {
                        return limit
                    }
                }
            }
        }
    }
}
