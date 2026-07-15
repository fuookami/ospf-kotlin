package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.core.frontend.expression.identifiers.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.value.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.shared.model.*

/**
 * Item order limit, used to define upper and lower bound constraints for order items under specific field values.
 * 物品订单限制，用于定义特定字段值下订单物品的上下界约束。
 *
 * @property palletItem The pallet item this limit applies to. / 此限制适用的托盘物品
 * @property field The field name used for matching. / 用于匹配的字段名
 * @property fieldValue The field value used for matching. / 用于匹配的字段值
 * @property isHard Whether this is a hard constraint. / 是否为硬约束
 * @property upperBoundValue The upper bound value, if defined. / 上界值，如果已定义
 * @property upperBoundLimit The upper bound constraint details, if defined. / 上界约束详情，如果已定义
 * @property lowerBoundValue The lower bound value, if defined. / 下界值，如果已定义
 * @property lowerBoundLimit The lower bound constraint details, if defined. / 下界约束详情，如果已定义
*/
data class ItemOrderLimit(
    val palletItem: PalletItem,
    val field: String,
    val fieldValue: String,
    val isHard: Boolean = false,
    val upperBoundValue: Double? = null,
    val upperBoundLimit: UpperBound? = null,
    val lowerBoundValue: Double? = null,
    val lowerBoundLimit: LowerBound? = null,
) {

    /**
     * Upper bound constraint for an item order limit.
     * 物品订单限制的上界约束。
     *
     * @property value The bound value. / 约束值
     * @property sign The constraint comparison sign. / 约束比较符号
     * @property limit The limit threshold. / 限制阈值
    */
    data class UpperBound(
        val value: Double,
        val sign: ConstraintSign,
        val limit: Double,
    )

    /**
     * Lower bound constraint for an item order limit.
     * 物品订单限制的下界约束。
     *
     * @property value The bound value. / 约束值
     * @property sign The constraint comparison sign. / 约束比较符号
     * @property limit The limit threshold. / 限制阈值
    */
    data class LowerBound(
        val value: Double,
        val sign: ConstraintSign,
        val limit: Double,
    )
}

/**
 * Gets the corresponding item order limit from a pallet item.
 * 从托盘物品中获取对应的物品订单限制。
 *
 * @param field The constraint field. / 约束字段
 * @param itemOrderLimits The list of item order limits. / 物品订单限制列表
 * @return The matching item order limit, or null if not found. / 匹配的物品订单限制，如果未找到则返回 null
*/
fun PalletItem.palletItem(
    field: String,
    itemOrderLimits: List<ItemOrderLimit>,
): ItemOrderLimit? {
    return itemOrderLimits.firstOrNull { limit ->
        limit.palletItem == this && limit.field == field
    }
}

/**
 * Gets the pallet item associated with the item order limit.
 * 获取物品订单限制关联的托盘物品。
 *
 * @param palletItems The list of pallet items. / 托盘物品列表
 * @return The associated pallet item, or null if not found. / 关联的托盘物品，如果未找到则返回 null
*/
fun ItemOrderLimit.palletItem(
    palletItems: List<PalletItem>,
): PalletItem? {
    return palletItems.firstOrNull { item ->
        item == this.palletItem
    }
}

/**
 * Gets the upper bound of the item order limit.
 * 获取物品订单限制的上界。
 *
 * @return The upper bound object, or null if not defined. / 上界对象，如果未定义则返回 null
*/
fun ItemOrderLimit.upperBound(): ItemOrderLimit.UpperBound? {
    return this.upperBoundLimit
}

/**
 * Gets the lower bound of the item order limit.
 * 获取物品订单限制的下界。
 *
 * @return The lower bound object, or null if not defined. / 下界对象，如果未定义则返回 null
*/
fun ItemOrderLimit.lowerBound(): ItemOrderLimit.LowerBound? {
    return this.lowerBoundLimit
}

/**
 * Generates the upper bound expression for the item order limit.
 * 生成物品订单限制的上界表达式。
 *
 * @param fieldValues The field value mapping. / 字段值映射
 * @return The upper bound expression, or null if not defined. / 上界表达式，如果未定义则返回 null
*/
fun ItemOrderLimit.upperBoundExpression(
    fieldValues: Map<String, List<FieldValue>>,
): Expression? {
    if (this.upperBoundValue == null) {
        return null
    }

    return this.palletItem.fieldExpression(field, fieldValue, fieldValues)
}

/**
 * Generates the lower bound expression for the item order limit.
 * 生成物品订单限制的下界表达式。
 *
 * @param fieldValues The field value mapping. / 字段值映射
 * @return The lower bound expression, or null if not defined. / 下界表达式，如果未定义则返回 null
*/
fun ItemOrderLimit.lowerBoundExpression(
    fieldValues: Map<String, List<FieldValue>>,
): Expression? {
    if (this.lowerBoundValue == null) {
        return null
    }

    return this.palletItem.fieldExpression(field, fieldValue, fieldValues)
}

/**
 * Gets the constraint sign for the upper bound of the item order limit.
 * 获取物品订单限制上界的约束符号。
 *
 * @return The constraint sign, or null if not defined. / 约束符号，如果未定义则返回 null
*/
fun ItemOrderLimit.upperBoundSign(): ConstraintSign? {
    return this.upperBoundLimit?.sign
}

/**
 * Gets the constraint sign for the lower bound of the item order limit.
 * 获取物品订单限制下界的约束符号。
 *
 * @return The constraint sign, or null if not defined. / 约束符号，如果未定义则返回 null
*/
fun ItemOrderLimit.lowerBoundSign(): ConstraintSign? {
    return this.lowerBoundLimit?.sign
}

/**
 * Generates the constraint for the item order limit.
 * 生成物品订单限制的约束。
 *
 * @param fieldValues The field value mapping. / 字段值映射
 * @param itemOrderLimits The list of item order limits. / 物品订单限制列表
 * @param palletItems The list of pallet items. / 托盘物品列表
 * @return The list of generated constraints. / 生成的约束列表
*/
fun ItemOrderLimit.limitConstraint(
    fieldValues: Map<String, List<FieldValue>>,
    itemOrderLimits: List<ItemOrderLimit>,
    palletItems: List<PalletItem>,
): List<Constraint> {
    val constraints = mutableListOf<Constraint>()

    val palletItem = this.palletItem(palletItems)
    if (palletItem == null) {
        return constraints
    }

    val itemOrderLimit = palletItem.palletItem(field, itemOrderLimits)
    if (itemOrderLimit == null) {
        return constraints
    }

    val fieldExpressions = palletItem.fieldExpression(field, fieldValue, fieldValues)
    if (fieldExpressions == null) {
        return constraints
    }

    val upperBoundExpression = itemOrderLimit.upperBoundExpression(fieldValues)
    val lowerBoundExpression = itemOrderLimit.lowerBoundExpression(fieldValues)

    if (upperBoundExpression != null) {
        val sign = itemOrderLimit.upperBoundSign() ?: ConstraintSign.Le
        constraints.add(
            if (this.isHard) {
                Constraint(
                    name = "limit_${field}_${fieldValue}_upper_${this.palletItem.name}",
                    items = upperBoundExpression,
                    sign = sign,
                    value = upperBoundExpression.eval(),
                )
            } else {
                Constraint(
                    name = "limit_${field}_${fieldValue}_upper_${this.palletItem.name}",
                    items = upperBoundExpression,
                    sign = sign,
                    value = upperBoundExpression.eval(),
                    penalty = Penalty(1.0),
                )
            }
        )
    }

    if (lowerBoundExpression != null) {
        val sign = itemOrderLimit.lowerBoundSign() ?: ConstraintSign.Ge
        constraints.add(
            if (this.isHard) {
                Constraint(
                    name = "limit_${field}_${fieldValue}_lower_${this.palletItem.name}",
                    items = lowerBoundExpression,
                    sign = sign,
                    value = lowerBoundExpression.eval(),
                )
            } else {
                Constraint(
                    name = "limit_${field}_${fieldValue}_lower_${this.palletItem.name}",
                    items = lowerBoundExpression,
                    sign = sign,
                    value = lowerBoundExpression.eval(),
                    penalty = Penalty(1.0),
                )
            }
        )
    }

    return constraints
}
