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

## 生成物禁止提交

**严禁将任何生成物提交到 git**，包括但不限于：

- 构建产物（`build.log`、编译输出等）
- 测试运行结果与验证证据（测试报告、覆盖率报告、截图、视频、追踪记录等）
- 临时文件（`.exit.txt`、`.jsonl` 日志等）
- AI 辅助生成的中间产物（进度文件、计划草稿等）

执行 `git add` / `git commit` 前，必须确认暂存区中不包含上述生成物。若发现误添加，应立即通过 `git reset HEAD <file>` 撤出暂存区。

## 报告生成路径

编译、测试、脚本检查等命令生成的报告文件必须输出到临时目录（`%TEMP%` 或 `E:\temp\{project-name}-tmp`），避免在工作区中产生临时文件。

示例：
- Maven 构建日志：`mvn compile -T 0.75C > $env:TEMP\build.log 2>&1`
- 测试报告：`mvn test -T 0.75C > $env:TEMP\test.log 2>&1`
- 脚本检查报告：`pwsh -File scripts\check.ps1 > $env:TEMP\check.log 2>&1`

临时目录中的文件会随系统清理自动删除，无需手动管理，也不会污染工作区。

## 语言要求
始终使用简体中文与用户对话。

## 提交信息要求
进行 `git commit` 或 `git commit --amend` 时，提交信息内容要具体、完整，清晰说明改动目的与关键变更点，避免过于简短或笼统的描述。
提交信息必须包含符合 Conventional Commit 风格的 Header。

## Worktree 管理

当一个任务在独立的 git worktree 中完成后，必须将改动合并回主仓库，然后删除该 worktree，保持仓库结构整洁。

操作流程：
1. 在 worktree 中完成任务并提交改动
2. 切换到主仓库：`cd <主仓库路径>`
3. 合并 worktree 分支：`git merge <分支名>`
4. 删除 worktree：`git worktree remove <worktree路径>`
5. 清理已合并的分支（可选）：`git branch -d <分支名>`

合并前应确认主仓库工作区状态干净，避免合并冲突。

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

### 增量编译优先策略

本项目模块众多、体量较大，完整 `mvn clean` 构建耗时显著。在任务执行过程中应遵循以下原则：

- **任务进行中**：使用增量编译（`mvn compile`、`mvn test -pl <module>` 等），避免 `mvn clean`，以利用 Maven 的增量编译机制减少重复构建时间。
- **任务收尾验收**：在所有代码修改完成后，执行 `mvn clean compile test-compile -T 0.75C` 进行全量编译，再执行 `mvn test -T 0.75C` 进行全量测试，确保最终结果无遗漏问题。
- **增量编译异常时**：若增量编译出现疑似缓存导致的诡异错误（如已删除的类仍被引用、修改未生效等），可在单次构建中插入 `mvn clean` 排查，但应尽快回归增量模式继续后续工作。

### 常用命令
```bash
# 增量编译（任务进行中推荐）
mvn compile -T 0.75C
mvn compile test-compile -T 0.75C

# 增量测试指定模块
mvn test -pl aps-domain/aps-domain-production
mvn test -Dtest=ToolRepositoryTest
mvn test -Dtest=ToolRepositoryTest#testSaveTool_shouldAssignId

# 全量编译+测试（仅任务收尾验收时使用）
mvn clean compile test-compile -T 0.75C
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

**增量优先，全量收尾。** 任务进行中尽量使用增量编译以节省时间，仅在任务最终验收时执行 `mvn clean` 全量构建与全量测试，确保交付质量。
