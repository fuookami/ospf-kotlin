/**
 * 谓词 schema 代码渲染器
 * Predicate schema code renderer
*/
package fuookami.ospf.kotlin.framework.persistence.expression.ksp

/**
 * 谓词 schema 代码渲染器
 * Predicate schema code renderer
 *
 * 将 PredicateSchemaModel 渲染为 Kotlin 源代码字符串。
 * Renders PredicateSchemaModel to Kotlin source code string.
*/
internal object PredicateSchemaRenderer {

    /**
     * 渲染谓词 schema 模型为 Kotlin 源代码
     * Render predicate schema model to Kotlin source code
     *
     * @param model 谓词 schema 模型 / Predicate schema model
     * @return 生成的 Kotlin 源代码字符串 / Generated Kotlin source code string
    */
    fun render(model: PredicateSchemaModel): String {
        return buildString {
            if (model.packageName.isNotBlank()) {
                appendLine("package ${model.packageName}")
                appendLine()
            }
            appendLine("import $PredicateDslPackage.PredicateSchema")
            if (model.generateColumnMapping) {
                appendLine("import $ColumnBinderPackage.HasColumnMapping")
                appendLine("import $ColumnBinderPackage.ColumnBinder")
            }
            appendLine()

            val interfaces = buildList {
                add("PredicateSchema<${model.kotlinEntityName}>()")
                if (model.generateColumnMapping) {
                    add("HasColumnMapping")
                }
            }
            appendLine("object ${model.schemaName} : ${interfaces.joinToString(", ")} {")
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
            if (model.generateColumnMapping) {
                appendLine()
                appendLine("    override val columnMapping: Map<String, String> = mapOf(")
                model.properties.forEachIndexed { index, property ->
                    val comma = if (index < model.properties.size - 1) "," else ""
                    appendLine("        \"${escape(property.propertyName)}\" to \"${escape(property.backendName)}\"$comma")
                }
                appendLine("    )")
                appendLine()
                appendLine("    fun <C> createBinder(resolver: (String) -> C?): ColumnBinder<C> {")
                appendLine("        return object : ColumnBinder<C> {")
                appendLine("            override fun resolve(path: String): C? {")
                appendLine("                val backendName = columnMapping[path] ?: path")
                appendLine("                return resolver(backendName)")
                appendLine("            }")
                appendLine("        }")
                appendLine("    }")
            }
            appendLine("}")
        }
    }

    /**
     * 转义字符串中的特殊字符
     * Escape special characters in string
     *
     * @param value 原始字符串 / Original string
     * @return 转义后的字符串 / Escaped string
    */
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
