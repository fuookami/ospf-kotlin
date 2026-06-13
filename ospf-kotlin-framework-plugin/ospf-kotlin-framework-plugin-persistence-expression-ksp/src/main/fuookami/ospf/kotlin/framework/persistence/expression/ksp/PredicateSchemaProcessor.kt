/**
 * 谓词 schema KSP processor
 * Predicate schema KSP processor
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate

/**
 * 谓词 schema KSP 处理器
 * Predicate schema KSP processor
 *
 * 扫描带有 PredicateEntity 注解的类，生成对应的 PredicateSchema 代码。
 * Scans classes annotated with PredicateEntity and generates corresponding PredicateSchema code.
 *
 * @property codeGenerator KSP 代码生成器 / KSP code generator
 * @property logger KSP 日志器 / KSP logger
 */
class PredicateSchemaProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    /**
     * 处理带注解的符号
     * Process annotated symbols
     *
     * @param resolver KSP 解析器 / KSP resolver
     * @return 延迟处理的符号列表 / List of deferred symbols
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(PredicateEntityAnnotation)
        val deferred = mutableListOf<KSAnnotated>()
        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred.add(symbol)
                continue
            }
            symbol.accept(Visitor(), Unit)
        }
        return deferred
    }

    /**
     * 符号访问器，处理类声明
     * Symbol visitor for processing class declarations
     */
    private inner class Visitor : KSVisitorVoid() {
        /**
         * 访问类声明，生成 schema 代码文件
         * Visit class declaration and generate schema code file
         *
         * @param classDeclaration 类声明 / Class declaration
         * @param data 附加数据（未使用）/ Additional data (unused)
         */
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val model = classDeclaration.toModel() ?: return
            val sourceFile = classDeclaration.containingFile
            val dependencies = if (sourceFile != null) {
                Dependencies(aggregating = false, sourceFile)
            } else {
                Dependencies.ALL_FILES
            }
            val file = codeGenerator.createNewFile(
                dependencies = dependencies,
                packageName = model.packageName,
                fileName = model.schemaName
            )
            OutputStreamWriter(file, StandardCharsets.UTF_8).use {
                it.write(PredicateSchemaRenderer.render(model))
            }
        }

        /**
         * 将类声明转换为谓词 schema 模型
         * Convert class declaration to predicate schema model
         *
         * @return 谓词 schema 模型，验证失败时返回 null / Predicate schema model, or null if validation fails
         */
        private fun KSClassDeclaration.toModel(): PredicateSchemaModel? {
            val entityName = simpleName.asString()
            val packageName = packageName.asString()
            if (!validateEntity(entityName, this)) {
                return null
            }
            val annotation = annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PredicateEntityAnnotation
            } ?: return null
            val schemaName = annotation.stringArgument("schemaName")
                ?.takeIf { it.isNotBlank() }
                ?: "${entityName}Schema"
            if (!validateSchemaName(schemaName, this)) {
                return null
            }
            val generateResolver = annotation.booleanArgument("generateResolver") ?: true
            var hasPropertyError = false
            val properties = getAllProperties()
                .filter { it.isVisibleSchemaProperty() }
                .mapNotNull { property ->
                    val propertyName = property.simpleName.asString()
                    val kotlinName = propertyName.kotlinIdentifierOrNull()
                    if (kotlinName == null) {
                        logger.error(
                            "Predicate field '$propertyName' in $entityName is not a supported Kotlin identifier",
                            property
                        )
                        hasPropertyError = true
                        return@mapNotNull null
                    }
                    val backendName = property.predicateFieldName()
                    if (backendName != null && backendName.isBlank()) {
                        logger.error(
                            "Predicate field '$propertyName' in $entityName has a blank backend name",
                            property
                        )
                        hasPropertyError = true
                        return@mapNotNull null
                    }
                    PredicateProperty(
                        propertyName = propertyName,
                        backendName = backendName ?: propertyName,
                        kotlinName = kotlinName
                    )
                }
                .toList()

            if (hasPropertyError) {
                return null
            }
            if (generateResolver && properties.any { it.propertyName == "resolver" }) {
                logger.error(
                    "Predicate entity $entityName cannot generate resolver because it already has a property named 'resolver'",
                    this
                )
                return null
            }
            if (properties.isEmpty()) {
                logger.warn("Predicate entity $entityName has no readable properties", this)
            }
            return PredicateSchemaModel(
                packageName = packageName,
                entityName = entityName,
                kotlinEntityName = entityName.kotlinIdentifierOrNull() ?: entityName,
                schemaName = schemaName,
                generateResolver = generateResolver,
                properties = properties
            )
        }

        /**
         * 验证实体类是否符合要求
         * Validate entity class meets requirements
         *
         * @param entityName 实体类名 / Entity class name
         * @param declaration 类声明 / Class declaration
         * @return 是否通过验证 / Whether validation passes
         */
        private fun validateEntity(entityName: String, declaration: KSClassDeclaration): Boolean {
            if (declaration.parentDeclaration is KSDeclaration) {
                logger.error("Predicate entity $entityName cannot be a nested class", declaration)
                return false
            }
            if (declaration.typeParameters.isNotEmpty()) {
                logger.error("Predicate entity $entityName cannot declare type parameters", declaration)
                return false
            }
            if (!entityName.isRegularKotlinIdentifier()) {
                logger.error("Predicate entity name '$entityName' is not a supported Kotlin identifier", declaration)
                return false
            }
            return true
        }

        /**
         * 验证 schema 名称是否为合法标识符
         * Validate schema name is a valid identifier
         *
         * @param schemaName schema 名称 / Schema name
         * @param declaration 类声明 / Class declaration
         * @return 是否通过验证 / Whether validation passes
         */
        private fun validateSchemaName(schemaName: String, declaration: KSClassDeclaration): Boolean {
            if (!schemaName.isRegularKotlinIdentifier()) {
                logger.error(
                    "Predicate schema name '$schemaName' is not a valid Kotlin identifier",
                    declaration
                )
                return false
            }
            return true
        }

        /**
         * 判断属性是否为可见的 schema 属性
         * Check if property is a visible schema property
         *
         * @return 是否可见 / Whether visible
         */
        private fun KSPropertyDeclaration.isVisibleSchemaProperty(): Boolean {
            return simpleName.asString().isNotBlank()
        }

        /**
         * 获取属性的谓词字段名（通过 @PredicateField 注解指定）
         * Get predicate field name of property (specified via @PredicateField annotation)
         *
         * @return 后端字段名，未注解时返回 null / Backend field name, or null if not annotated
         */
        private fun KSPropertyDeclaration.predicateFieldName(): String? {
            return annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PredicateFieldAnnotation
            }?.stringArgument("name")
        }
    }
}

/**
 * 从注解中获取字符串参数值
 * Get string argument value from annotation
 *
 * @param name 参数名 / Argument name
 * @return 参数值，不存在时返回 null / Argument value, or null if not present
 */
private fun com.google.devtools.ksp.symbol.KSAnnotation.stringArgument(name: String): String? {
    return arguments.firstOrNull { it.name?.asString() == name }?.value as? String
}

/**
 * 从注解中获取布尔参数值
 * Get boolean argument value from annotation
 *
 * @param name 参数名 / Argument name
 * @return 参数值，不存在时返回 null / Argument value, or null if not present
 */
private fun com.google.devtools.ksp.symbol.KSAnnotation.booleanArgument(name: String): Boolean? {
    return arguments.firstOrNull { it.name?.asString() == name }?.value as? Boolean
}

private val kotlinKeywords = setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while"
)

private val regularIdentifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

/**
 * 判断字符串是否为合法的常规 Kotlin 标识符
 * Check if string is a valid regular Kotlin identifier
 *
 * @return 是否合法 / Whether valid
 */
private fun String.isRegularKotlinIdentifier(): Boolean {
    return matches(regularIdentifier) && this !in kotlinKeywords
}

/**
 * 将字符串转换为合法的 Kotlin 标识符，关键字用反引号包裹
 * Convert string to valid Kotlin identifier, wrapping keywords with backticks
 *
 * @return 合法标识符，不合法时返回 null / Valid identifier, or null if invalid
 */
private fun String.kotlinIdentifierOrNull(): String? {
    if (matches(regularIdentifier)) {
        return if (this in kotlinKeywords) {
            "`$this`"
        } else {
            this
        }
    }
    return null
}
