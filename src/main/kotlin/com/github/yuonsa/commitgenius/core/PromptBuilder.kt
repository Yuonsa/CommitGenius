package com.github.yuonsa.commitgenius.core

import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.markdown.MarkdownContentBuilder
import ai.koog.prompt.markdown.MarkdownContentBuilder.ListContext
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.params.LLMParams
import kotlinx.serialization.json.JsonPrimitive

/**
 * {@link PromptBuilder}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/05/28
 * @see ai.koog.prompt.Prompt
 */
object PromptBuilder {

    fun buildPrompt(
        diffContent: String,
        template: OutputTemplate = OutputTemplate.Std
    ) = buildPrompt(diffContent, template, null, null)

    fun buildPrompt(
        diffContent: String,
        customCoreRules: String,
        outputRequirements: String
    ) = buildPrompt(diffContent, OutputTemplate.Custom, customCoreRules, outputRequirements)

    fun buildPrompt(
        diffContent: String,
        template: OutputTemplate = OutputTemplate.Std,
        customCoreRules: String? = null,
        outputRequirements: String? = null
    ): Prompt = prompt(
        id = "generate-commit-message",
        params = LLMParams(
            temperature = 0.2,
            toolChoice = LLMParams.ToolChoice.None,
            maxTokens = 1024,
            additionalProperties = mapOf(
                "enable_thinking" to JsonPrimitive(false)
            )
        )
    ) {
        system {
            markdown {
                h1("你是一个专业的 Git 提交信息生成助手。请分析用户提供的 `git diff` 内容，生成符合 Conventional Commits 规范的中文提交信息。")
                br()

                h2("【核心规则】")
                this.systemCoreRules(customCoreRules)
                br()

                h2("【输出要求】")
                textWithNewLine("请直接输出内容，不要包含任何多余的解释、寒暄或 markdown 代码块标记（如 ```）")
                this.systemOutputRequirements(template, outputRequirements)
                br()

                h2("【错误处理】")
                textWithNewLine("如果 diff 内容为空或无法解析，请仅输出：“没有检测到有效的代码变更，请检查您的 diff 内容。”")
            }
        }

        user {
            markdown {
                textWithNewLine("请分析以下 git diff 内容并生成提交信息：")
                textWithNewLine("Diff：")
                code(diffContent)
            }
        }
    }

    private fun MarkdownContentBuilder.systemCoreRules(customCoreRules: String? = null) {
        if (!customCoreRules.isNullOrBlank()) {
            textWithNewLine(customCoreRules)
            return
        }
        numbered {
            item("格式：`type(scope): subject`")
            item {
                "" + code("type")
                text("：必须是")
                !listOf(
                    "feat",
                    "fix",
                    "docs",
                    "style",
                    "refactor",
                    "perf",
                    "test",
                    "chore",
                    "ci",
                    "build"
                ).joinToString("`,`", "`", "`")
                !"之一。"
            }
            item("scope：必须使用双语格式 `(英文模块名/中文描述)`，从文件路径推断（如 `src/auth/` -> `(auth/认证)`，`docs/` -> `(docs/文档)`，无法确定用 `(core/核心)`）。")
            item("subject：使用中文，简洁明了，祈使句，不加句号，不超过 50 字符。")
        }
    }

    private fun MarkdownContentBuilder.systemOutputRequirements(
        template: OutputTemplate,
        requirements: String? = null
    ) {
        if (template == OutputTemplate.Custom || !requirements.isNullOrBlank()) {
            requirements?.let { textWithNewLine(it) }
            return
        }
        bulleted(template.block)
    }

    enum class OutputTemplate(val block: ListContext.() -> Unit) {

        /**
         * 简洁版
         */
        Lite({
            item("选项1（简洁）:") {
                code("type(scope): subject")
            }
        }),

        /**
         * 标准版
         */
        Std({
            item("选项2（标准）:") {
                code("type(scope): subject")
                textWithNewLine(" ")
                textWithNewLine("说明为什么和怎么做（1-2句话）")
            }
        }),

        /**
         * 详细版
         */
        Ultra({
            item("选项3（详细）:") {
                code("type(scope): subject")
                textWithNewLine(" ")
                bulleted {
                    item("变更点 1")
                    item("变更点 2")
                    item("变更点 3")
                }
            }
        }),

        /**
         * 自定义
         */
        Custom({}),

        ;

        fun getDescription() = markdown {
            bulleted(block)
        }
    }
}
