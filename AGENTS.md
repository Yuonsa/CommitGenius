# AGENTS.md - CommitGenius

> IntelliJ IDEA 插件 — Git Commit Message 智能生成器 (基于 Koog AI Framework)

## 项目概览

| 属性 | 值 |
|---|---|
| **名称** | CommitGenius |
| **Group ID** | `com.github.yuonsa.commitgenius` |
| **版本** | `0.0.1` |
| **语言** | Kotlin 2.3.10 |
| **目标平台** | IntelliJ IDEA 2026.1.2 (since build 261) |
| **构建工具** | Gradle (Configuration Cache + Build Cache) |
| **核心依赖** | Koog AI Framework `1.0.0` (`koog-agents`) |

## 开发者命令

```bash
# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew check

# 验证插件兼容性
./gradlew verifyPlugin

# 运行插件 (在 IDE 中调试)
./gradlew runIde

# 清理构建
./gradlew clean

# 运行指定单测
./gradlew test --tests "com.github.yuonsa.commitgenius.XxxTest"

# 完整流程: 清理 → 构建 → 测试 → 验证
./gradlew clean buildPlugin check verifyPlugin
```

**注意**: Gradle 启用了 Configuration Cache (`org.gradle.configuration-cache=true`) 和 Build Cache。

## CI/CD

- CI 基于 GitHub Actions (`.github/workflows/build.yml`)
- Java 21 (Zulu), Ubuntu-latest
- 流程: build → test → verify → releaseDraft
- PR 触发全量检查, main push 触发 release draft

## 架构

### 目录结构

```
src/main/kotlin/com/github/yuonsa/commitgenius/
├── AppBundle.kt                          # 主国际化 Bundle
├── NotificationBundle.kt                 # 通知国际化 Bundle
├── actions/
│   └── GenerateCommitAction.kt           # ★ 核心入口: AI 提交按钮
├── core/
│   ├── DiffAnalyzer.kt                   # ★ Diff 分析引擎 (LCS 算法)
│   ├── PromptBuilder.kt                  # ★ Prompt 构建器 (Koog DSL)
│   ├── notification/
│   │   └── Notifier.kt                   # IDE 通知工具
│   └── agent/
│       └── LLMEngine.kt                  # ★ LLM 执行引擎 (Koog)
├── components/
│   └── MarkdownTextField.kt              # Markdown 语法高亮文本框
├── services/
│   ├── AppPersistentStateComponent.kt    # 应用级持久化服务
│   └── ProjectPersistentStateComponent.kt # 项目级持久化服务
├── settings/
│   ├── ApplicationConfigurable.kt        # 应用级设置面板
│   ├── ProjectConfigurable.kt            # 项目级设置面板
│   ├── SettingLayout.kt                  # 设置布局定义 (DSL 实例 + SettingFields)
│   ├── dsl/
│   │   ├── SettingDsl.kt                 # DSL 注解 + 入口
│   │   ├── SettingLayoutBuilder.kt       # Layout 构建器
│   │   └── SettingFieldDefBuilder.kt     # 字段定义构建器
│   ├── field/
│   │   └── SettingFieldDef.kt            # 字段元信息定义
│   ├── binding/
│   │   ├── UIBinding.kt                  # UI 控件绑定抽象
│   │   └── ComponentBinding.kt           # 具体控件绑定工厂
│   ├── bound/
│   │   └── BoundField.kt                 # 绑定字段运行时
│   └── state/
│       ├── AppSettingState.kt            # 应用级配置数据模型
│       ├── ProjectSettingState.kt        # 项目级配置数据模型
│       └── EffectiveSettings.kt          # ★ 配置合并解析器
└── startup/
    └── WelComeActivity.kt                # 启动活动
```

### 核心包职责

| 包 | 职责 |
|---|---|
| `actions` | 用户交互入口: Commit 窗口按钮 |
| `core` | AI 引擎 (Koog 框架封装)、Diff 分析、Prompt 构建、协议适配 |
| `core/agent` | LLM 执行引擎，封装 Koog MultiLLMPromptExecutor，支持 OpenAI 和 Anthropic 流式调用 |
| `core/notification` | IDE 通知封装 |
| `components` | 自定义 UI 组件 (Markdown 高亮文本框) |
| `services` | 应用/项目级持久化服务 (State Storage) |
| `settings` | DSL 驱动的双层配置系统 (全局 + 项目级覆盖) |
| `startup` | 项目启动活动 |

### 类间依赖关系

```
GenerateCommitAction
  ├── DiffAnalyzer ──────────────────────────┐
  ├── PromptBuilder ─────────────────────────┤
  ├── LLMEngine ─────────────────────────────┤
  │     └── EffectiveSettings ───────────────┤
  │           ├── AppPersistentStateComponent┤
  │           └── ProjectPersistentState─────┤
  ├── Notifier                               │
  └── OutputTemplate.Ultra ← PromptBuilder  ←┘

ApplicationConfigurable
  ├── SettingLayout (DSL) ───────────────────────────┐
  │     ├── SettingFields (provider, apiKey, ...)    │
  │     └── SettingGroupBuilder + FieldDefBuilder    │
  ├── BoundField ────────────────────────────────────┤
  │     ├── SettingFieldDef ─────────────────────────┤
  │     ├── UIBinding ───────────────────────────────┤
  │     │     └── ComponentBinding (textField, ...) ─┤
  │     └── JBCheckBox (override)                    │
  └── AppPersistentStateComponent ←──────────────────┘

ProjectConfigurable → ApplicationConfigurable (继承)
  └── ProjectPersistentStateComponent
```

## 核心模块详解

### Actions 层

| 类 | 职责 | 关键方法 |
|---|---|---|
| `GenerateCommitAction` | IDE Commit 窗口中的 "AI 生成" 按钮。获取选中变更、调用 Diff 分析、构建 Prompt、流式调用 LLM、回填 Commit 消息 | `actionPerformed()`: 入口，禁用按钮后在 IO 协程异步执行<br>`generateMessage()`: 核心流程，带后台进度条 (diff → prompt → LLM stream)<br>`update()`: 控制按钮可用性 (BGT 线程) |

**执行流程**:
1. 从 `VcsDataKeys.COMMIT_WORKFLOW_UI` 获取已勾选变更和未版本文件
2. 在后台进度容器中逐步执行: 获取 Diff (20%) → 构建 Prompt (10%) → 初始化模型参数 (10%) → 调用模型流式生成 (60%)
3. 结果通过 EDT 线程回填到 `COMMIT_MESSAGE_CONTROL`

### Core 层

| 类 | 职责 | 关键特性 |
|---|---|---|
| `DiffAnalyzer` | 智能 Diff 分析引擎 | 支持行级勾选 (partial commit)、LCS 行级 diff 算法、单文件 20KB/总计 100KB 限制 |
| `PromptBuilder` | 使用 Koog Prompt DSL 构建 Prompt | 使用 `markdown {}` DSL 定义系统提示，支持 Lite/Std/Ultra/Custom 四种输出模板 |
| `LLMEngine` | LLM 执行引擎 | 封装 Koog MultiLLMPromptExecutor，支持 OpenAI 和 Anthropic；提供两个 `executeStreaming` 重载 (简单/带 Reasoning Delta 分离)；内置首次响应/完成耗时日志 |
| `ProtocolAdapter` | 协议适配器基类 | sealed class 骨架，预留 API 模式 vs ACP 模式的统一接口 (待实现) |
| `Notifier` | IDE 通知封装 | 基于 `NotificationGroupManager` 提供 `info`/`warn`/`error`/`notify` 方法，支持 i18n key 查找 |

### Settings 层 (DSL 驱动的双层配置系统)

这是一个 **DSL 驱动的双层配置系统**，支持 App 级别 (全局) 和 Project 级别 (项目级覆盖)。

#### 数据模型

| 类 | 说明 |
|---|---|
| `AppSettingState` | 应用级配置 data class。7 个字段，全部必填 (非 nullable) |
| `ProjectSettingState` | 项目级配置 data class。7 个字段，全部 nullable (null = 继承全局) |
| `EffectiveSettings` | **配置解析器**: 合并 App + Project 配置，Project 非 null 优先，返回 `AppSettingState` |

#### 配置字段清单

| 字段 | 类型 | 说明 |
|---|---|---|
| `provider` | `LLMProvider` | AI 供应商 (OpenAI / Anthropic) |
| `apiKey` | `String` | API 密钥 |
| `baseUrl` | `String` | 自定义 API 端点地址 (空 = 使用供应商默认) |
| `modelId` | `String` | 模型 ID |
| `outputTemplate` | `OutputTemplate` | 输出模板 (Lite/Std/Ultra/Custom) |
| `coreRules` | `String?` | 自定义核心规则 (覆盖默认规则) |
| `outputRequirements` | `String?` | 自定义输出要求 (Custom 模板时必填) |

#### DSL 框架

| 类 | 职责 |
|---|---|
| `@SettingDsl` | DSL 标记注解 |
| `settingLayout()` | DSL 入口函数 |
| `SettingLayoutBuilder` | 布局构建器，管理多个 `SettingGroupBuilder` |
| `SettingGroupBuilder` | 配置组构建器，管理多个 `SettingFieldDef` |
| `SettingFieldDefBuilder` | 字段定义构建器 (label, comment, binding, appProp, projProp) |

#### 绑定系统

| 类 | 职责 |
|---|---|
| `SettingFieldDef<C, T>` | 字段元信息: label, comment, appGet/appSet, projGet/projSet, binding |
| `UIBinding<C, T>` | UI 控件绑定抽象: createComponent, uiGet, uiSet |
| `ComponentBinding` | 控件工厂: textField(), passwordField(), comboBox(), markdownField() |
| `BoundField<C, T>` | 运行时绑定字段: 持有组件 + CheckBox(项目级覆盖开关) + 状态同步方法 |

#### SettingLayout 定义的分组

1. **模型配置** (`bundleGroup`): provider, apiKey, baseUrl, modelId
2. **规则配置** (`bundleCollapsibleGroup`): outputTemplate, coreRules, outputRequirements (可折叠)

#### SettingFields 定义

在 `SettingLayout.kt` 中定义了 4 个共享的 `SettingFieldDef`:
- `provider`: ComboBox 绑定，选项来自 `AppPersistentStateComponent.supportedProviders()`
- `apiKey`: PasswordField 绑定
- `baseUrl`: TextField 绑定
- `modelId`: TextField 绑定

所有字段通过 `createI18nField()` 辅助函数创建，自动从 AppBundle 加载 label 和 comment。

### Services 层

| 类 | 职责 | 存储文件 |
|---|---|---|
| `AppPersistentStateComponent` | 应用级持久化配置服务。全局唯一的 API Key、模型等配置。提供 `supportedProviders()` 静态方法返回支持的供应商列表 | `CommitGeniusSettings.xml` |
| `ProjectPersistentStateComponent` | 项目级持久化配置服务。每个项目独立的配置 (可覆盖全局) | `CommitGeniusProjectSettings.xml` |

### Components 层

| 类 | 职责 |
|---|---|
| `MarkdownTextField` | Markdown 语法高亮多行文本输入框。基于 `LanguageTextField(MarkdownLanguage)`，支持行号、代码折叠、软换行 |

## AI 引擎架构

### Koog 框架集成

项目使用 **Koog AI Framework** (`ai.koog`) 作为核心 AI 引擎，统一对接 OpenAI 和 Anthropic API。

- **LLMEngine**: 封装 `MultiLLMPromptExecutor`，支持流式响应和 Reasoning Delta 分离
- **支持供应商**: OpenAI, Anthropic
- **Prompt 模板**: Lite / Std / Ultra / Custom 四种模式
- **HTTP 客户端**: 使用 `KtorKoogHttpClient` 作为统一 HTTP 工厂

### Prompt 系统规则

插件通过 Koog 的 `prompt {}` DSL 构建结构化 Prompt:

```kotlin
prompt(id = "generate-commit-message", params = LLMParams(...)) {
    system {
        markdown {
            h1("助手角色定义")
            h2("【核心规则】")
            h2("【输出要求】")
            h2("【错误处理】")
        }
    }
    user {
        markdown {
            code(diffContent)
        }
    }
}
```

**LLM 参数**: `temperature=0.2`, `toolChoice=None`, `maxTokens=1024`, `enable_thinking=false`

**核心规则** (默认):
1. 格式：`type(scope): subject`
2. `type` 必须是 `feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`chore`、`ci`、`build` 之一
3. `scope`：双语格式 `(英文模块名/中文描述)`，从文件路径推断
4. `subject`：中文，祈使句，不加句号，不超过 50 字符

**输出模板**:
- **Lite**: 仅 `type(scope): subject`
- **Std**: 标题 + 1-2句说明 (为什么和怎么做)
- **Ultra**: 标题 + 变更点列表
- **Custom**: 完全由 `outputRequirements` 定义

## Diff 分析引擎

### 核心特性

1. **行级 Partial Commit 支持**: `DiffAnalyzer.tryGetPartialContent()` 通过 IntelliJ 的 `PartialLocalLineStatusTracker` 获取用户行级勾选的实际提交内容，而非整个文件。

2. **LCS 行级 Diff 算法**: 不使用 `git diff` 命令，而是用 LCS 算法逐行对比，生成标准 unified diff 格式，支持 context 行合并 (默认 3 行)。

3. **Token 优化**: Diff 分析器内置单文件 20KB / 总计 100KB 限制，超大文件自动跳过。

4. **未版本文件处理**: 未追踪文件 (unversioned files) 全文视为新增内容。

## 插件配置 (plugin.xml)

### 注册清单

| 扩展点 | 实现类/ID | 说明 |
|---|---|---|
| `postStartupActivity` | `WelComeActivity` | 项目启动活动 |
| `notificationGroup` | `com.github.yuonsa.commitgenius.notificationGroup` | 通知组 (BALLOON) |
| `applicationService` | `AppPersistentStateComponent` | 应用级持久化服务 |
| `applicationConfigurable` | `ApplicationConfigurable` | 全局设置面板 (parentId: tools) |
| `projectService` | `ProjectPersistentStateComponent` | 项目级持久化服务 |
| `projectConfigurable` | `ProjectConfigurable` | 项目设置面板 (parentId: tools) |
| `action` | `GenerateCommitAction` | AI 提交按钮 (添加到 `Vcs.MessageActionGroup`)，图标 `AllIcons.Actions.Lightning` |

### 依赖模块

`platform`, `lang`, `idea`, `vcs`, `Git4Idea`, `markdown`

### 其他配置

- `require-restart="false"`: 插件更新无需重启 IDE

## 国际化

### AppBundle.properties (主配置)

| Key | 值 | 类别 |
|---|---|---|
| `configurable.name.app` | CommitGenius | 配置 |
| `configurable.name.project` | CommitGenius (Project) | 配置 |
| `configurable.group.model.name` | 模型配置 | 配置 |
| `configurable.group.model.provider.label` | 供应商 | 配置 |
| `configurable.group.model.provider.desc` | 选择模型供应商 | 配置 |
| `configurable.group.model.apiKey.label` | ApiKey | 配置 |
| `configurable.group.model.apiKey.desc` | (空) | 配置 |
| `configurable.group.model.baseUrl.label` | 服务地址 | 配置 |
| `configurable.group.model.baseUrl.desc` | (空) | 配置 |
| `configurable.group.model.modelId.label` | 模型 | 配置 |
| `configurable.group.model.modelId.desc` | (空) | 配置 |
| `configurable.group.rule.name` | 规则配置 | 配置 |
| `configurable.group.rule.outputTemplate.label` | 输出模板 | 配置 |
| `configurable.group.rule.outputTemplate.desc` | (空) | 配置 |
| `configurable.group.rule.coreRules.label` | 核心规则 | 配置 |
| `configurable.group.rule.coreRules.desc` | (空) | 配置 |
| `configurable.group.rule.outputRequirements.label` | 输出要求 | 配置 |
| `configurable.group.rule.outputRequirements.desc` | (空) | 配置 |
| `ui.action.override` | 覆盖 | UI |
| `ui.action.GenerateCommitAction.title` | 生成提交信息 | UI |
| `backgroundProcess.generateCommit.title` | CommitGenius 正在解析代码意图... | 后台进程 |
| `backgroundProcess.generateCommit.obtain-diff` | 获取变动信息 | 后台进程 |
| `backgroundProcess.generateCommit.buildPrompt` | 构建提示词 | 后台进程 |
| `backgroundProcess.generateCommit.init-model-param` | 初始化模型参数 | 后台进程 |
| `backgroundProcess.generateCommit.call-model` | 调用模型: {0} | 后台进程 |
| `backgroundProcess.generateCommit.call-model.generating` | 生成中... | 后台进程 |

### NotificationBundle.properties (通知)

| Key | 值 |
|---|---|
| `notification.group.name` | CommitGeniusNotifications |
| `commit.diff.content.empty` | 你根本没改任何有意义的代码，让我怎么编？🤷 |

## 测试

- 框架: JUnit 4 + IntelliJ BasePlatformTestCase
- 测试文件位于 `src/test/kotlin/`
- 运行单测: `./gradlew test --tests "com.github.yuonsa.commitgenius.XxxTest"`

## 设计亮点

1. **DSL 驱动的设置系统**: 使用 Kotlin DSL 声明式定义所有配置字段，通过 `UIBinding` 抽象实现类型安全的 Swing 控件绑定，代码简洁且易扩展。

2. **双层配置继承**: App 级 (全局必填) + Project 级 (nullable，null = 继承)，通过 `EffectiveSettings` 统一解析，配合 ProjectConfigurable 的 CheckBox 实现"覆盖/继承"开关。

3. **行级 Partial Commit 支持**: `DiffAnalyzer.tryGetPartialContent()` 通过 IntelliJ 的 `PartialLocalLineStatusTracker` 获取用户行级勾选的实际提交内容，而非整个文件。

4. **LCS 行级 Diff 算法**: 不使用 `git diff` 命令，而是用 LCS 算法逐行对比，生成标准 unified diff 格式，支持 context 行合并。

5. **Koog 多模型框架**: 通过 `ai.koog` 框架统一对接 OpenAI 和 Anthropic API，支持流式响应和 Reasoning Delta 分离。

6. **Token 优化**: Diff 分析器内置单文件 20KB / 总计 100KB 限制，超大文件自动跳过。

7. **Koog Prompt DSL**: 使用结构化 `prompt {}` DSL + `markdown {}` 构建系统提示，参数可控 (temperature、toolChoice、maxTokens)，比纯文本注入更安全灵活。

## 注意事项

- 示例代码 (WelComeActivity) 需在开发完成后清理
- 每次修改 plugin.xml 后需重新构建才能生效
- `gradle.properties` 中 `kotlin.stdlib.default.dependency = false` 避免重复打包 stdlib
