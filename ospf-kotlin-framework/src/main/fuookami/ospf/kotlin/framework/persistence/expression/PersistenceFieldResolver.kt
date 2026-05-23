/**
 * 持久化字段解析器
 * Persistence Field Resolver
 *
 * 统一表达“表达式路径 -> 持久化字段”解析边界。
 * Unifies the "expression path -> persistence field" resolving boundary.
 *
 * 说明：
 * Notes:
 * 1. 解析输入是表达式路径（通常来自 PO/DTO 字段路径），不是领域聚合自动展开规则。
 * 1. The input is an expression path (usually from PO/DTO field path), not a domain object auto-expansion rule.
 * 2. 插件只消费解析结果，不推断 Quantity 等领域模型的落库策略。
 * 2. Plugins only consume resolved fields and do not infer persistence strategy for domain models such as Quantity.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

typealias PersistenceFieldResolver<C> = (String) -> C?
