# 项目规范

## 1. 编码风格

### 1.1 注释语言
编写注释时，要中英双语。

### 1.2 版权声明
不需要添加版权声明。

### 1.3 ReadMe 文件
英文 ReadMe：README.md，中文 ReadMe：README_ch.md，要添加超链接能互相跳转。

### 1.4 Shell 工具
PowerShell 用：pwsh.exe。

### 1.5 函数调用命名参数规范
超过 2 个参数时，使用多行和命名参数。

### 1.6 Kotlin 文件排版与 import 规范

本节约束 Kotlin 源文件的基础排版风格。重构时应优先保持本项目既有手写风格，不使用会大幅改写 import、空行和换行的自动格式化结果作为最终形态。

#### 1.6.1 文件整体结构

文件各部分的排列顺序和间距必须遵循以下规范：

1. **文件级注解**（如 `@file:Suppress`）必须放在文件最开头，与 `package` 声明之间不空行。
2. `package` 声明与首个 `import` 之间必须有且仅有一个空行。
3. 最后一个 `import` 语句与首个顶层声明之间必须有且仅有一个空行。
4. 文件末尾必须有且仅有一个换行符（即最后一个 `}` 后有一个空行）。

#### 1.6.2 import 排列

**整体顺序**：

1. 所有 `import` 连续排列，中间不按来源分组插空行。
2. import 来源层级按以下顺序排列（被依赖方排在前面）：
   - Kotlin / Java / kotlinx 标准或官方库
   - 第三方库（如 `org.apache.logging`、`org.http4k` 等）
   - `fuookami.ospf.kotlin.*`（按模块依赖深度升序）
3. `fuookami.ospf.kotlin.*` 内部按模块依赖层级排列，从底层到上层：
   - `fuookami.ospf.kotlin.utils.*`（基础工具，被所有模块依赖）
   - `fuookami.ospf.kotlin.multiarray.*`（多维数组）
   - `fuookami.ospf.kotlin.math.*`（数学库）
   - `fuookami.ospf.kotlin.quantities.*`（物理量）
   - `fuookami.ospf.kotlin.core.*`（优化核心）
   - `fuookami.ospf.kotlin.framework.*`（应用框架，含 solver、persistence 等基础设施）
   - `fuookami.ospf.kotlin.framework.<domain>.infrastructure.*`（领域基础设施）
   - `fuookami.ospf.kotlin.framework.<domain>.domain.*`（领域模型与服务，被依赖的子域排在前面）
   - `fuookami.ospf.kotlin.framework.<domain>.application.*`（应用服务）
4. 同一模块层级内，按 **首层差异路径段长度升序 -> 首层差异路径段字典序升序** 排列。比较的是共同前缀之后、第一个差异路径段（以 `.` 分隔）的长度和字典序，而非完整差异路径段或完整 import 字符串。
5. 同包或同模块下多类型引用允许使用 `*` 通配符；跨模块仅引用少数明确类型时，可使用显式 import。
6. 禁止 import 末尾使用分号。
7. 当 `kotlin.time.*` 与 `kotlinx.datetime.*` 同时出现时，允许两侧同时使用 `*` 通配符；若存在同名符号（如 `Instant`）歧义，必须显式 import 指定主来源（例如显式 `import kotlin.time.Instant`）。

**正确示例 1**（以 `ospf-kotlin-core` 模块中的文件为例）：

```kotlin
package fuookami.ospf.kotlin.core.model.mechanism

import kotlin.collections.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
```

说明：
- Kotlin / kotlinx 标准库 → 第三方库 → `utils`（最底层）→ `math`（被 core 依赖）→ `core`（当前模块）
- `utils` 内部：`error`(5) < `functional`(10)，按首层差异段长度升序

**正确示例 2**（以 `ospf-kotlin-framework-bpp3d` 领域模块中的文件为例）：

```kotlin
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.collections.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.geometry.g2d.*
import fuookami.ospf.kotlin.math.geometry.g3d.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.bla_context.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.primitives.*
```

说明：
- 标准库 → `utils` → `math`（`g2d`(3) < `g3d`(3)，字典序 g2d < g3d）→ `core` → `framework.bpp3d.infrastructure`（基础设施）→ `domain.item`（被 layer_assignment 依赖的子域）→ `domain.bla_context`（被依赖的子域）→ `domain.layer_assignment`（当前域）
- 子域间按依赖深度排列：`item` 和 `bla_context` 被 `layer_assignment` 依赖，排在其前面

**错误示例**：

```kotlin
import fuookami.ospf.kotlin.core.variable.*;   // 末尾分号
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model.*  // core 应排在 framework 之前

import fuookami.ospf.kotlin.utils.functional.*  // 空行分隔 imports
import fuookami.ospf.kotlin.utils.math.*  // 同前缀后 functional(10字符) 排在 math(4字符) 前，违反同层路径段长度升序
```

#### 1.6.3 换行、缩进与空行

**基础规则**：

- 使用 4 空格缩进，不使用 tab。
- 不使用尾逗号。
- 顶层声明之间保留一行空行。
- `companion object`、成员方法、内部方法之间保留一行空行。
- class / object / companion object 结束前不保留多余空行。
- 禁止保留重复 KDoc 或连续重复注释块。

**函数声明、构造器与函数调用**：

- 1-2 个参数且语义简单时可单行书写。
- 超过 2 个参数时，按 `1.5 函数调用命名参数规范` 使用多行和命名参数。
- 多行参数列表中，参数缩进一层；闭合括号与调用起始位置对齐。
- 嵌套函数调用按实际层级缩进，不得出现参数相对调用位置额外漂移。

```kotlin
val material = Material.from(
    id = id,
    code = code,
    name = name,
    status = MaterialStatus.Active
)
```

**返回与表达式体**：

- 非平凡函数使用块体和显式 `return`。
- 简单派生属性、简单单行函数可使用表达式体。

```kotlin
val isActive: Boolean get() = status == UserStatus.Active

fun create(input: MaterialCreateInput): Ret<Material> {
    return input.validate().map { validated ->
        Material(
            id = nextId(),
            spec = validated
        )
    }
}
```

#### 1.6.4 注释与分段

- 公共类、接口、重要公共方法使用中文 KDoc。
- 简短属性可使用单行 KDoc，如 `/** 是否已加载用户组引用 */`。
- 服务和仓储内部允许使用 `// ==================== 查询 ====================` 形式分段。
- 注释应说明业务意图、加载态语义或分段边界；避免重复描述代码本身。

#### 1.6.5 KDoc 标签与覆盖要求

**标签规范**：

- 公共类 / 接口必须使用 `@property` 标注构造参数或公开属性（语义明显者如 `id: Long` 可省略）。
- 公共函数（含扩展函数）必须使用 `@param` 标注每个参数，使用 `@return` 标注返回值（返回 `Unit` 或语义自明的 `this` 时可省略 `@return`）。
- 泛型类型参数语义非常规时使用 `@param T` 或 `@property T` 说明。
- 单行 KDoc（仅一句话、无标签需求）仍然允许，不必强加空标签。

**覆盖要求**：

- 每个重载（overload）都必须有独立的 KDoc，不得仅为一组重载的第一个添加 KDoc。
- 每个 `typealias` 都必须有独立的 KDoc（允许单行）。

### 1.7 泛型化命名规范

对外 API 使用业务自然名表达稳定抽象，不使用迁移期技术命名。

- 泛型化后的主接口、主模型、主服务占用自然名。
- 不使用 `V`、`Typed`、`Generic` 作为迁移痕迹型前后缀。
- 需要保留的 `Flt64` 专用接口、桥接接口或兼容入口，使用 `Flt64` 后缀显式标识。
- `TypedValueRange`、`ClosedTypedValueRange`、`TypedPathBuilder` 等本身表达类型级抽象的稳定概念可以保留 `Typed`。
- 内部变量、测试名、文档示例应尽量同步上述命名，避免保留迁移期表达。
