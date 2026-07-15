# Framework 架构规范

## 1. 适用范围

本规范适用于 `ospf-kotlin-framework-*` 下以优化建模为核心的 framework 模块，包括但不限于 CSP1D、BPP3D、Gantt Scheduling 等领域框架。

普通工具模块、DTO/序列化、starter 示例、测试夹具不强制套用本规范；但一旦代码承担优化模型装配、领域建模、列生成或求解编排职责，应优先遵循本规范。

## 2. MetaModel 轴心

优化建模应以 `MetaModel` / `AbstractLinearMetaModel` 作为模型装配轴心。

1. domain context、aggregation、model component 负责定义并注册变量、中间值、表达式、约束、目标和结果提取逻辑。
2. service/limits/pipeline 负责小粒度约束、目标、惩罚项、影子价格提取和可选规则注册。
3. application solver 负责流程编排、模型创建、上下文注册、求解调用、异常边界、状态映射和 solution 组装，不应直接硬编码各领域子模型细节。
4. 同一业务能力在普通 MILP、列生成 LP、列生成 final MILP 中应尽量共享同一套注册路径，避免多处重复拼模型。

## 3. Context 与 Aggregation

framework 领域能力应优先拆为 context / aggregation / model component / pipeline 结构。

1. context 是对 application 暴露的建模入口，负责初始化领域聚合并注册到模型。
2. aggregation 组合领域内的多个 model component，并协调它们的注册顺序。
3. model component 持有领域变量、中间值、派生表达式和结果解析所需引用。
4. pipeline 或 limit 负责单一约束族、目标族、惩罚项或 shadow price 逻辑。
5. extra context / extra pipeline 应作为标准扩展方式，使下游业务可以追加变量、约束、目标和结果提取逻辑。

## 4. 列生成生命周期

列生成或类似迭代建模框架应显式覆盖以下生命周期。

1. `register`：注册初始变量、中间值、约束和目标。
2. `addColumns`：新增列时同步注册变量并刷新相关中间值、约束和目标。
3. `removeColumns`：移除列时同步更新变量范围、模型 token 和领域聚合状态。
4. `refreshShadowPrice` / `extractShadowPrice`：从模型约束和中间值中提取或刷新影子价格。
5. `finalize` / `extractSolution`：从 solver solution 回填领域结果、KPI、trace 和 render 数据。
6. 普通求解与迭代求解应尽量复用同一组 context、aggregation、pipeline 抽象。

## 5. 泛型化与物理量化

framework public API 应保持泛型化和物理量化。

1. 对外领域模型、配置和结果优先使用 `V : RealNumber<V>` 等泛型数值抽象。
2. 宽度、长度、重量、面积、需求、产出、余料、容量等领域量应优先使用 `Quantity<V>`、`PhysicalUnit` 或明确的物理量类型表达。
3. `Flt64`、`Double`、无单位裸值只能作为 solver adapter、registration、extraction 或低层数学接口的实现细节。
4. 数值类型转换和单位换算应集中在 adapter、context registration 或 extraction 边界，避免散落在 application solver 中。
5. 不同单位的量不得通过裸值直接混算；需要比较、聚合或转化时必须有明确单位口径。

## 6. 可扩展计算与判断

所有计算、判断、派生值、过滤、可行性检查和目标系数生成，都应尽可能预留扩展点。

1. 默认实现可以提供通用逻辑，但不应把可变业务规则硬编码在 solver/application 编排层。
2. 可变规则优先通过 strategy、context、pipeline、adapter、函数参数、接口或领域 policy 注入。
3. 扩展点应能覆盖额外变量、中间值、约束、目标、shadow price extractor、solution extractor 和 KPI/render enrich。
4. 下游业务约束应能通过 extra context / pipeline 接入，例如同单位长度、同宽、宽差、兼容性、业务成本、软硬约束切换等规则。
5. 扩展接口应暴露足够的通用领域上下文，如 product、material、machine、cutting plan、quantity、model registration context 和 solver value adapter。

## 7. Application 层边界

application 层应保持编排职责清晰。

1. application 可以组合 context、选择 solver、处理 warm start/recovery、映射状态、组织 trace/KPI/render。
2. application 不应成为领域变量、约束、目标和结果提取的集中堆积点。
3. 当 application 中出现大量 `model.add(...)`、`model.addConstraint(...)`、目标项拼接或 token 解析逻辑时，应优先下沉到 domain context、aggregation、model component 或 pipeline。
4. application 的公共入口应保持稳定，内部建模结构演进不应迫使调用方直接依赖 solver 私有细节。

## 8. 文档与测试

引入或调整 framework 建模结构时，应同步更新文档和测试。

1. README / README_ch 应说明 public API、建模扩展点、泛型数值边界和物理量边界。
2. 关键 context、pipeline、extension point 应有针对性测试，验证其能注册到 `MetaModel` 并参与求解或结果提取。
3. 列生成类模块应覆盖初始注册、加列、影子价格、最终求解和部分失败路径。
4. 若新增扩展接口，应至少提供一个 fake 或最小示例验证下游能追加约束或目标，而无需修改 application solver 主流程。
