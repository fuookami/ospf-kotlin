/**
 * 谓词 schema KSP processor provider
 * Predicate schema KSP processor provider
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * 谓词 schema KSP 处理器提供者
 * Predicate schema KSP processor provider
 *
 * 注册 PredicateSchemaProcessor 供 KSP 框架使用。
 * Registers PredicateSchemaProcessor for use by the KSP framework.
 */
class PredicateSchemaProcessorProvider : SymbolProcessorProvider {
    /**
     * 创建谓词 schema 处理器
     * Create predicate schema processor
     *
     * @param environment KSP 处理器环境 / KSP processor environment
     * @return 谓词 schema 处理器实例 / Predicate schema processor instance
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PredicateSchemaProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}

/** 谓词实体注解全限定名 / Fully qualified name of PredicateEntity annotation */
internal const val PredicateEntityAnnotation =
    "fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity"

/** 谓词字段注解全限定名 / Fully qualified name of PredicateField annotation */
internal const val PredicateFieldAnnotation =
    "fuookami.ospf.kotlin.framework.persistence.expression.PredicateField"

/** 谓词 DSL 包名 / Predicate DSL package name */
internal const val PredicateDslPackage =
    "fuookami.ospf.kotlin.math.symbol.expression.dsl"

/**
 * 谓词属性信息
 * Predicate property information
 *
 * @property propertyName 原始属性名 / Original property name
 * @property backendName 后端字段名 / Backend field name
 * @property kotlinName Kotlin 标识符名（可能带反引号转义）/ Kotlin identifier name (may be backtick-escaped)
 */
internal data class PredicateProperty(
    val propertyName: String,
    val backendName: String,
    val kotlinName: String
)

/**
 * 谓词 schema 模型
 * Predicate schema model
 *
 * @property packageName 包名 / Package name
 * @property entityName 实体类名 / Entity class name
 * @property kotlinEntityName Kotlin 标识符实体名 / Kotlin identifier entity name
 * @property schemaName 生成的 schema 类名 / Generated schema class name
 * @property generateResolver 是否生成 resolver 字段 / Whether to generate resolver field
 * @property properties 属性列表 / Property list
 */
internal data class PredicateSchemaModel(
    val packageName: String,
    val entityName: String,
    val kotlinEntityName: String,
    val schemaName: String,
    val generateResolver: Boolean,
    val properties: List<PredicateProperty>
)
