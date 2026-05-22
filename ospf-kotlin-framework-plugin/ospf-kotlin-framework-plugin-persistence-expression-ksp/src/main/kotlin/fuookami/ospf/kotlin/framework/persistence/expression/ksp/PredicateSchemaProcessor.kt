/**
 * 谓词 schema KSP processor
 * Predicate schema KSP processor
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

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
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class PredicateSchemaProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
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

    private inner class Visitor : KSVisitorVoid() {
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

        private fun KSPropertyDeclaration.isVisibleSchemaProperty(): Boolean {
            return simpleName.asString().isNotBlank()
        }

        private fun KSPropertyDeclaration.predicateFieldName(): String? {
            return annotations.firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PredicateFieldAnnotation
            }?.stringArgument("name")
        }
    }
}

private fun com.google.devtools.ksp.symbol.KSAnnotation.stringArgument(name: String): String? {
    return arguments.firstOrNull { it.name?.asString() == name }?.value as? String
}

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

private fun String.isRegularKotlinIdentifier(): Boolean {
    return matches(regularIdentifier) && this !in kotlinKeywords
}

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
