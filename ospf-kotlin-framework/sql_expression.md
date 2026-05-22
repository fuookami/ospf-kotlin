# SQL Expression 当前状态与后续事项

更新日期：2026-05-22

本文合并并替代以下旧文档：

- `ospf-kotlin-framework/sql_expression.md`
- `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression/daily.md`

## 1. 总结

SQL Expression 已从“设计与实施计划”进入“已有实现但需要补齐质量与一致性”的状态。

本轮（2026-05-22）执行结果：

1. 已完成 P2：Ktorm 补齐 `InExpression` 与 `Lt/Le/Gt/Ge`，并实现 `NullsOrder` 降级排序。
2. 已完成 P2：Ktorm/MyBatis/Mongo 统一 unsupported 表达式策略为“恒假条件/空结果策略”，避免误解释为无条件查询。
3. 已完成 P3：补齐三插件 translator 行为测试与 repository 集成测试。
4. 已完成 P3：framework + 三插件联测通过。

当前事实：

1. `ospf-kotlin-math` 的通用表达式 AST、DSL、parser、serde、normalize、evaluate 仍存在并有单元测试。
2. `ospf-kotlin-framework` 主模块保留表达式仓储公共接口与模型：`ExpressionRepository`、`SortBy`、`UpdateAssignments`。
3. ORM/存储适配已迁移到插件模块：Ktorm、MyBatis-Plus、MongoDB。
4. 旧计划中的 `EntityMeta` / `FieldBinding` 不再存在于当前代码中，实际实现改为 resolver 函数：
   - Ktorm：`KtormColumnResolver = (String) -> ColumnDeclaring<*>?`
   - MyBatis：`MybatisColumnNameResolver = (String) -> String?`
   - MongoDB：`MongoFieldNameResolver = (String) -> String?`
5. `daily.md` 中的 P8 泛型化、solver 边界、Rust 功能对齐等内容属于 core/framework 更大范围重构，不再放在 SQL Expression 文档中维护。

## 2. 当前代码结构

### 2.1 math 表达式层

位置：`ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/expression`

已存在：

- `PropertyPath.kt`
- `PathSymbol.kt`
- `ScalarExpression.kt`
- `BooleanExpression.kt`
- `ExpressionOperator.kt`
- `dsl/ExpressionDsl.kt`
- `parser/Lexer.kt`
- `parser/Parser.kt`
- `parser/Token.kt`
- `serde/ExpressionSerde.kt`
- `operation/Normalize.kt`
- `operation/EvaluateBoolean.kt`
- `operation/NumericOps.kt`

已存在测试：

- `PropertyPathPathSymbolTest.kt`
- `ExpressionASTTest.kt`
- `BooleanDslTest.kt`
- `BooleanParserTest.kt`
- `ExpressionSerdeTest.kt`
- `NormalizeTest.kt`
- `EvaluateBooleanTest.kt`

### 2.2 framework 公共模型层

位置：`ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/expression`

已存在：

- `RepositoryApi.kt`
  - `ExpressionRepository<E>`
  - `find(where)`
  - `find(where, sortBy, limit, offset)`
  - `count(where)`
  - `update(where, assignments)`
  - `delete(where)`
  - `exists(where)`
- `SortBy.kt`
  - `SortBy`
  - `SortItem`
  - `SortDirection`
  - `NullsOrder`
  - `NullsOrderSupport`
- `UpdateAssignment.kt`
  - `UpdateAssignments`
  - `SetValue`
  - `SetNull`
  - `SetFromExpression`

已存在测试：

- `SortByTest.kt`
- `UpdateAssignmentTest.kt`

### 2.3 Ktorm 插件

位置：`ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm`

已存在：

- `KtormRepository.kt`
- `translator/KtormBooleanTranslator.kt`
- `translator/KtormOrderByTranslator.kt`
- `translator/KtormUpdateTranslator.kt`
- `translator/PatternMatchPolicy.kt`

已存在测试：

- `KtormBooleanTranslatorTest.kt`
- `KtormOrderByTranslatorTest.kt`
- `KtormUpdateTranslatorTest.kt`

### 2.4 MyBatis-Plus 插件

位置：`ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis`

已存在：

- `MybatisRepository.kt`
- `translator/MybatisBooleanTranslator.kt`
- `translator/MybatisOrderByTranslator.kt`
- `translator/MybatisUpdateTranslator.kt`

已存在测试：

- `MybatisBooleanTranslatorTest.kt`

### 2.5 MongoDB 插件

位置：`ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb`

已存在：

- `MongoRepository.kt`
- `translator/MongoBooleanTranslator.kt`
- `translator/MongoOrderByTranslator.kt`
- `translator/MongoUpdateTranslator.kt`

已存在测试：

- `MongoBooleanTranslatorTest.kt`

## 3. 完成情况矩阵

| 能力 | 当前状态 | 说明 |
|------|----------|------|
| math AST | 已完成 | `ScalarExpression` / `BooleanExpression` 已存在 |
| math DSL/parser/serde/normalize/evaluate | 已完成 | 有对应实现与测试 |
| 公共排序模型 | 已完成 | `SortBy` 支持多字段、方向、nulls 配置 |
| 公共更新模型 | 已完成 | `UpdateAssignments` 支持 value/null/expression |
| 公共仓储接口 | 已完成 | `ExpressionRepository` 已存在 |
| Ktorm 插件骨架 | 已完成 | repository + 三类 translator 已存在 |
| MyBatis 插件骨架 | 已完成 | repository + 三类 translator 已存在 |
| MongoDB 插件骨架 | 已完成 | repository + 三类 translator 已存在 |
| `EntityMeta` / `FieldBinding` | 已取消或被替代 | 当前代码使用 resolver 函数，不再保留这两个模型 |
| 多 ORM 集成测试 | 已完成（本轮范围） | 已新增 Ktorm/MyBatis/Mongo repository 集成测试 |
| translator 行为完整性 | 已完成（本轮范围） | 三插件已统一 unsupported 语义并补行为断言 |

## 4. 主要未完成事项

### 4.1 文档与代码不一致

当前 `README.md` / `README_ch.md` 仍提到 `EntityMeta`、`FieldBinding` 和基于 `EntityMeta` 的示例，但代码中已不存在对应类型。

需要处理：

1. 更新 expression README，改为 resolver 函数示例。
2. 删除或改写 `EntityMeta` / `FieldBinding` 相关描述。
3. 修复 README 中指向本文件的相对链接。

### 4.2 Ktorm translator 支持不完整

`KtormBooleanTranslator` 当前存在明显 MVP 限制：

1. `InExpression` 直接返回 `null`。
2. `BooleanCustom` 返回 `null`。
3. `ComparisonOperator.Lt/Le/Gt/Ge` 在列-常量比较中返回 `null`。
4. 只处理列-常量和常量-列，不处理列-列、复杂标量表达式。
5. `BooleanConstant` 返回 `null`，调用方会把条件视为空结果或空查询，语义需要明确。

`KtormUpdateTranslator` 当前限制：

1. `SetFromExpression` 只支持 `ScalarConstant`。
2. 复杂表达式未翻译为 Ktorm SQL expression。
3. 未做列类型兼容校验。

`KtormOrderByTranslator` 当前限制：

1. 忽略 `NullsOrder`。
2. `NullsOrderSupport` 参数未真正参与排序降级。

### 4.3 MyBatis translator 与 repository 风险

`MybatisBooleanTranslator` 支持比较、IN、LIKE、NULL、AND/OR/NOT，但仍有边界：

1. `BooleanConstant` 被忽略。
2. `BooleanCustom` 被忽略。
3. `PatternMatchMode.Regex` 被忽略。
4. 只处理列-常量与常量-列，不处理列-列和复杂标量表达式。

`MybatisRepository.update` 当前存在高风险：

1. 先构造了 `queryWrapper` 并翻译 where。
2. 实际调用 `mapper.update(null, updateWrapper)` 时没有把 where 条件应用到 `updateWrapper`。
3. 这可能导致更新条件丢失。

`MybatisRepository.find` 分页也需要复核：

1. 连续调用 `wrapper.last("LIMIT ...")` 和 `wrapper.last("OFFSET ...")` 可能后者覆盖前者。
2. 应合并为单次 `last("LIMIT ... OFFSET ...")` 或使用 MyBatis-Plus 分页机制。

### 4.4 MongoDB translator 语义需复核

`MongoBooleanTranslator` 当前支持比较、IN、PatternMatch、NULL、AND/OR/NOT。

需要复核：

1. `NullCheck(IsNull)` 当前翻译为 `Filters.exists(field, false)`，这与 MongoDB 中字段存在但值为 `null` 的语义不同。
2. `PatternMatch` 没有处理 case-sensitive / case-insensitive 选项。
3. `BooleanCustom` 返回 `null`。

### 4.5 测试覆盖不足

当前不少 translator 测试只验证表达式对象可构造，并未验证实际翻译结果。

需要补齐：

1. Ktorm translator 结果测试或 SQL 生成测试。
2. MyBatis `QueryWrapper` / `UpdateWrapper` 条件生成测试。
3. MongoDB `Bson` 输出结构测试。
4. Repository 层集成测试，至少覆盖 where + sort + page + update + delete。
5. 失败路径测试：未知路径、空 assignments、BooleanConstant、unsupported expression。

## 5. 建议后续优先级

### P0：修复高风险行为

1. 修复 `MybatisRepository.update` 条件丢失问题。
2. 修复 MyBatis 分页 `last(...)` 覆盖问题。
3. 明确 `BooleanConstant(True/False/Unknown)` 在各 translator 中的语义，避免 `null` 被误解释。

### P1：让文档与当前架构一致

1. 更新 `README.md` / `README_ch.md`。
2. 移除 `EntityMeta` / `FieldBinding` 旧描述。
3. 补充 resolver 函数示例。

### P2：补齐 translator 能力

1. Ktorm 支持 `InExpression`。
2. Ktorm 支持 `Lt/Le/Gt/Ge`。
3. 明确或实现 `NullsOrder` 降级策略。
4. 各插件统一处理 unsupported expression：要么早失败，要么显式返回空条件策略。

### P3：补测试与门禁

1. 为每个插件补 translator 行为测试。
2. 增加 repository 集成测试。
3. 在 CI 或常用命令中覆盖 framework 与 persistence 插件模块。

## 6. 建议验证命令

```powershell
mvn -pl ospf-kotlin-math -Dtest=PropertyPathPathSymbolTest,ExpressionASTTest,BooleanDslTest,BooleanParserTest,ExpressionSerdeTest,NormalizeTest,EvaluateBooleanTest test
mvn -pl ospf-kotlin-framework -Dtest=SortByTest,UpdateAssignmentTest test
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm test
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis test
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb test
mvn -pl ospf-kotlin-framework,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-ktorm,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mybatis,ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mongodb test
```

如要做最终验收，应运行包含 framework plugin 的完整构建，而不仅是上述窄测试。

## 7. 旧计划处理说明

旧文档中以下内容已经归档，不再作为当前执行清单：

1. Phase 0-5 的早期实施计划。
2. Phase F0-F7 的逐日计划。
3. `EntityMeta` / `FieldBinding` 的设计草案。
4. P8 泛型化、solver/framework 入口命名、Rust 功能缺口等跨模块事项。

这些内容如需继续维护，应迁移到对应模块的专项文档，而不是放在 SQL Expression 文档中。
