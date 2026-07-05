package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.core.frontend.expression.identifiers.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.value.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.shared.model.*

/** 物品订单限制，用于定义特定字段值下订单物品的上下界约束 / Item order limit, used to define upper and lower bound constraints for order items under specific field values */
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
    /** 上界 / Upper bound */
    data class UpperBound(
        val value: Double,
        val sign: ConstraintSign,
        val limit: Double,
    )

    /** 下界 / Lower bound */
    data class LowerBound(
        val value: Double,
        val sign: ConstraintSign,
        val limit: Double,
    )
}

/**
 * 从托盘物品中获取对应的物品订单限制 / Get the corresponding item order limit from a pallet item
 * @param field 限制字段 / The constraint field
 * @param itemOrderLimits 物品订单限制列表 / The list of item order limits
 * @return 匹配的物品订单限制，如果未找到则返回 null / The matching item order limit, or null if not found
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
 * 获取物品订单限制关联的托盘物品 / Get the pallet item associated with the item order limit
 * @param palletItems 托盘物品列表 / The list of pallet items
 * @return 关联的托盘物品，如果未找到则返回 null / The associated pallet item, or null if not found
 */
fun ItemOrderLimit.palletItem(
    palletItems: List<PalletItem>,
): PalletItem? {
    return palletItems.firstOrNull { item ->
        item == this.palletItem
    }
}

/**
 * 获取物品订单限制的上界 / Get the upper bound of the item order limit
 * @return 上界对象，如果未定义则返回 null / The upper bound object, or null if not defined
 */
fun ItemOrderLimit.upperBound(): ItemOrderLimit.UpperBound? {
    return this.upperBoundLimit
}

/**
 * 获取物品订单限制的下界 / Get the lower bound of the item order limit
 * @return 下界对象，如果未定义则返回 null / The lower bound object, or null if not defined
 */
fun ItemOrderLimit.lowerBound(): ItemOrderLimit.LowerBound? {
    return this.lowerBoundLimit
}

/**
 * 生成物品订单限制的上界表达式 / Generate the upper bound expression for the item order limit
 * @param fieldValues 字段值映射 / The field value mapping
 * @return 上界表达式，如果未定义则返回 null / The upper bound expression, or null if not defined
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
 * 生成物品订单限制的下界表达式 / Generate the lower bound expression for the item order limit
 * @param fieldValues 字段值映射 / The field value mapping
 * @return 下界表达式，如果未定义则返回 null / The lower bound expression, or null if not defined
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
 * 获取物品订单限制上界的约束符号 / Get the constraint sign for the upper bound of the item order limit
 * @return 约束符号，如果未定义则返回 null / The constraint sign, or null if not defined
 */
fun ItemOrderLimit.upperBoundSign(): ConstraintSign? {
    return this.upperBoundLimit?.sign
}

/**
 * 获取物品订单限制下界的约束符号 / Get the constraint sign for the lower bound of the item order limit
 * @return 约束符号，如果未定义则返回 null / The constraint sign, or null if not defined
 */
fun ItemOrderLimit.lowerBoundSign(): ConstraintSign? {
    return this.lowerBoundLimit?.sign
}

/**
 * 生成物品订单限制的约束 / Generate the constraint for the item order limit
 * @param fieldValues 字段值映射 / The field value mapping
 * @param itemOrderLimits 物品订单限制列表 / The list of item order limits
 * @param palletItems 托盘物品列表 / The list of pallet items
 * @return 生成的约束列表 / The list of generated constraints
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
