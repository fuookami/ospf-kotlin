# AGENTS

## 作用范围
本文件适用于当前目录及其所有子目录。

## 规则来源
请遵循 `.rules/` 下实际存在的规则文件：

- `.rules/chore.md`
- `.rules/framework-architecture.md`
- `.rules/error-handling.md`

## 明确优先级顺序（从高到低）
1. `.rules/chore.md`
2. `.rules/framework-architecture.md`
3. `.rules/error-handling.md`

## 冲突处理
当规则冲突时，严格按上述优先级顺序执行。

## 语言要求
始终使用简体中文与用户对话。

## 提交信息要求
进行 `git commit` 或 `git commit --amend` 时，提交信息内容要具体、完整，清晰说明改动目的与关键变更点，避免过于简短或笼统的描述。
提交信息必须包含符合 Conventional Commit 风格的 Header。

## 命令行环境优先级

执行命令行操作时，优先使用 **PowerShell 7**（命令名 `pwsh`）或 **git bash**，而非 cmd.exe 或旧版 Windows PowerShell（5.x）。

- 优先级：`pwsh` > git bash > cmd.exe / Windows PowerShell 5.x
- `pwsh` 即 PowerShell 7+，跨平台二进制名统一为 `pwsh`（不同于 Windows PowerShell 5.x 的 `powershell`）
- `pwsh` 支持 UTF-8 默认编码、更好的管道对象模型、`||`/`&&` 链式操作符
- git bash 提供 Unix-like 工具链（grep、sed、awk 等），适合脚本操作
- 避免使用旧版 `powershell.exe`（Windows PowerShell 5.x），其编码和兼容性问题较多
- 当 AGENTS.md 中的示例使用 bash 语法时，在 git bash 中直接执行；若需在 pwsh 中执行，注意语法差异（如变量引用 `$env:VAR` vs `$VAR`、数组 `@()` vs `()` 等）

## Maven 构建优化

使用 `-T 0.75C` 参数可以加速多模块构建，该参数表示每个 CPU 核心使用 0.75 个线程进行并行构建。

### 常用命令
```bash
# 基础构建
mvn clean install -DskipTests
mvn compile

# 使用并行构建加速
mvn clean install -DskipTests -T 0.75C
mvn compile -T 0.75C

# 测试相关
mvn test -pl aps-domain/aps-domain-production
mvn test -Dtest=ToolRepositoryTest
mvn test -Dtest=ToolRepositoryTest#testSaveTool_shouldAssignId
mvn test -T 0.75C

# 排除特定测试（用于解决既有编译问题）
mvn test -pl aps-infrastructure -Dtest='!ChildValueVersionSupportTest'
```

## 编译与测试输出的获取规则

本项目整体编译与测试耗时较长，**严禁通过反复执行 `mvn compile` / `mvn test` 配合 `grep | tail -10` 或 `grep | head -10` 的方式分批获取错误信息**。这种做法会因每次只截取部分输出而漏看关键错误，迫使我们再次启动一轮完整构建，造成多次重复编译，严重浪费时间和资源。

### 强制做法（二选一）

1. **一次性获取全部错误信息**

   执行构建/测试命令时，必须保留完整的错误输出，不要用 `grep`、`tail`、`head` 截断后再判断。推荐将输出重定向到文件，再读取该文件全量分析：

   ```bash
   # 编译并保存完整输出到文件，便于一次性分析全部错误
   mvn clean compile test-compile -T 0.75C > build.log 2>&1
   # 失败时直接查看完整日志中的错误段落
   grep -nE "ERROR|BUILD|FAILURE|error:" build.log
   ```

   若需要查看具体错误的完整上下文，应直接阅读 `build.log` 对应行附近的内容，而不是再跑一次构建。

2. **交由用户执行并全量复制**

   若当前会话不便执行长耗时构建，**不要自行启动构建**。应明确告知用户需要执行的确切命令，请用户在本地执行后将完整输出（或完整的错误段落）粘贴回来，基于完整信息再继续处理。

### 禁止的反模式

```bash
# 错误：只取尾部 10 行，遗漏大部分错误，导致需要重复编译
mvn clean compile test-compile -T 0.75C 2>&1 | grep -E "ERROR|BUILD|SUCCESS|FAILURE" | tail -10

# 错误：只取头部 10 行，同样会漏看错误，触发二次编译
mvn clean compile test-compile -T 0.75C 2>&1 | grep "error:\|ERROR" | head -10
```

### 核心原则

**一次构建，一次分析。** 任何一次 `mvn` 构建或测试的输出都必须被完整利用，不得因输出截断而引发重复构建。
