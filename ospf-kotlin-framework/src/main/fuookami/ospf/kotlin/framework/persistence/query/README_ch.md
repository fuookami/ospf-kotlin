# 关系查询计划

:us: [English](README.md) | :cn: 简体中文

`RelationalQueryPlan` 是框架层、与数据库无关的受控关系查询契约，包含数据源、Join、谓词、投影、分组、排序、分页和可选根键。不包含业务数据集类型、物理表名、SQL 字符串、权限或预算。

## 核心语义

- `QuerySource` 表示由适配器注册的数据源，可带别名。
- 计划边界中的 `ColumnRef` 始终按数据源限定。
- `Inner` 和 `Left` 编译为关系 Join；`Exists` 编译为相关半连接，一对多条件不会放大根记录粒度。Join 条件必须通过列引用关联当前数据源与已绑定数据源，常量条件会被拒绝；`Exists` 只允许一对多或多对多基数。
- `distinct` 作用于选定投影；`rootKey` 明确声明根粒度计数，适配器必须拒绝不支持的复合根键计数策略。
- `PageSpec` 在谓词、分组和排序之后应用；NULL 排序通过 `NullsOrder` 显式表达。

计划容器会在构造时防御性复制外层集合、表达式容器和框架已知的可变 payload 容器；`List`、`Map`、`Set` 与数组会递归快照。框架无法安全复制的不透明自定义 payload 或循环容器只保留用于诊断，并会使 `validate()` 返回结构化失败，绝不会被替换为伪快照。`canonical()`/`canonicalHash()` 编码规范化后的表达式形状以及类型/容器结构，明确不包含标量和自定义 payload 的字面量原值。计划在后端编译前完成结构校验。后端必须维护数据源和字段白名单；隐式投影必须使用适配器显式注册的字段白名单，不能直接选择物理表全部列。未知数据源、未知或歧义字段、非法 Join 条件、不支持的方言能力和参数绑定错误都必须返回结构化失败。根粒度计数的根键必须属于根数据源。

SQL 适配器和审计摘要契约请参阅 Ktorm 插件文档。

`SqlRelationalQueryCompiler` 提供无驱动的参数化 SQL 编译实现，返回 `CompiledRelationalQuery`。它按照方言安全引用标识符，
并将标量表达式值全部绑定为 `?` 参数。MySQL 分页使用 `LIMIT offset, limit`，PostgreSQL 使用 `LIMIT limit OFFSET offset`，
Oracle 使用 `OFFSET offset ROWS FETCH NEXT limit ROWS ONLY`。`RelationalQuerySourceRegistry` 可将逻辑来源和字段映射到适配器维护的表与字段白名单；
未提供注册表时，显式投影中的逻辑名称作为简单适配器的物理名称使用。

`RelationalQueryExecutionStatsRecorder` 是线程安全的内存审计记录器。`recordSuccess` 和 `recordFailure` 保存计划哈希、规范化表示、
耗时、行数和失败状态；`snapshot()` 返回当前记录且不暴露内部队列。

旧的 `ExpressionRepository` 接口保持不变，因为其同步的列表、计数和更新方法不是 `Ret` 返回模式。适配器可以在基础设施边界
将既有 `BooleanExpression` DSL 构造成 `RelationalQueryPlan`，再转换编译器返回的 `Ret` 结果。
