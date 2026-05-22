/**
 * 谓词 schema 代码渲染器
 * Predicate schema code renderer
 */
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

internal object PredicateSchemaRenderer {
    fun render(model: PredicateSchemaModel): String {
        return buildString {
            if (model.packageName.isNotBlank()) {
                appendLine("package ${model.packageName}")
                appendLine()
            }
            appendLine("import $PredicateDslPackage.PredicateSchema")
            appendLine()
            appendLine("object ${model.schemaName} : PredicateSchema<${model.kotlinEntityName}>() {")
            for (property in model.properties) {
                appendLine("    val ${property.kotlinName} = field(${model.kotlinEntityName}::${property.kotlinName})")
            }
            if (model.generateResolver) {
                appendLine()
                appendLine("    val resolver: (String) -> String? = { path ->")
                appendLine("        when (path) {")
                for (property in model.properties) {
                    appendLine("            \"${escape(property.propertyName)}\" -> \"${escape(property.backendName)}\"")
                }
                appendLine("            else -> null")
                appendLine("        }")
                appendLine("    }")
            }
            appendLine("}")
        }
    }

    private fun escape(value: String): String {
        return buildString {
            for (char in value) {
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}
