/**
 * 持久化字段解析器 / Persistence Field Resolver
 *
 * 统一表达“表达式路径 -> 持久化字段”解析边界。 / Unifies the "expression path -> persistence field" resolving boundary.
 *
 * 说明： / Notes:
 * 1. 解析输入是表达式路径（通常来自 PO/DTO 字段路径），不是领域聚合自动展开规则。 / 1. The input is an expression path (usually from PO/DTO field path), not a domain object auto-expansion rule.
 * 2. 插件只消费解析结果，不推断 Quantity 等领域模型的落库策略。 / 2. Plugins only consume resolved fields and do not infer persistence strategy for domain models such as Quantity.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

/**
 * 持久化字段解析器函数类型 / Persistence field resolver
 *
 * @param C 字段容器类型 / Field container type
*/
fun interface PersistenceFieldResolver<C> {
    operator fun invoke(path: String): C?
}

/**
 * 持久化字段解析结果 / Persistence field resolution result
 *
 * 保留缺失和歧义的区别，供需要结构化错误的查询计划使用。
 * Preserves the distinction between missing and ambiguous fields for query plans that need structured errors.
 *
 * @param C 字段容器类型 / Field container type
 */
sealed interface PersistenceFieldResolution<out C> {

    /** 已解析字段 / Resolved field */
    data class Resolved<C>(val value: C) : PersistenceFieldResolution<C>

    /** 字段不存在 / Field is missing */
    data class Missing(val path: String) : PersistenceFieldResolution<Nothing>

    /** 字段存在多个候选 / Field has multiple candidates */
    data class Ambiguous(
        val path: String,
        val candidates: List<String>
    ) : PersistenceFieldResolution<Nothing>

    /** 解析器配置非法 / Resolver configuration is invalid */
    data class InvalidConfiguration(val reason: String) : PersistenceFieldResolution<Nothing>
}

/**
 * 带诊断信息的持久化字段解析器 / Persistence field resolver with diagnostics
 *
 * 旧的可空解析接口保持兼容；关系查询适配器可使用详细结果拒绝歧义字段。
 * The nullable resolver remains compatible while relational adapters can use detailed results to reject ambiguity.
 *
 * @param C 字段容器类型 / Field container type
 */
interface DiagnosticPersistenceFieldResolver<C> : PersistenceFieldResolver<C> {

    /**
     * 解析字段并保留失败原因 / Resolve a field while preserving the failure reason
     *
     * @param path 表达式路径 / Expression path
     * @return 结构化解析结果 / Structured resolution result
     */
    fun resolveDetailed(path: String): PersistenceFieldResolution<C>

    override fun invoke(path: String): C? {
        return (resolveDetailed(path) as? PersistenceFieldResolution.Resolved<C>)?.value
    }
}
