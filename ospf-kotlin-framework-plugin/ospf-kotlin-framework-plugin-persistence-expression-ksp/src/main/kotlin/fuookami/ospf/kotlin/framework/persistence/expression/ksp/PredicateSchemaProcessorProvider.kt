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

class PredicateSchemaProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PredicateSchemaProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}

internal const val PredicateEntityAnnotation =
    "fuookami.ospf.kotlin.framework.persistence.expression.PredicateEntity"

internal const val PredicateFieldAnnotation =
    "fuookami.ospf.kotlin.framework.persistence.expression.PredicateField"

internal const val PredicateDslPackage =
    "fuookami.ospf.kotlin.math.symbol.expression.dsl"

internal data class PredicateProperty(
    val propertyName: String,
    val backendName: String,
    val kotlinName: String
)

internal data class PredicateSchemaModel(
    val packageName: String,
    val entityName: String,
    val kotlinEntityName: String,
    val schemaName: String,
    val generateResolver: Boolean,
    val properties: List<PredicateProperty>
)
