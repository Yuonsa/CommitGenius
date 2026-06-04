package com.github.yuonsa.commitgenius.components

import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.LanguageTextField
import org.intellij.plugins.markdown.lang.MarkdownLanguage
import java.awt.Dimension

/**
 * 基于 Markdown 语法高亮的多行文本输入框。
 *
 * 在 [LanguageTextField] 的基础上封装了以下能力：
 * - 默认启用 Markdown 语法高亮
 * - 支持限制组件高度
 * - 支持控制滚动条可见性
 * - 支持通过 [editorSettingApply] 回调进一步定制 Editor 行为
 *
 * 使用示例：
 * ```kotlin
 * MarkdownTextField(
 *     project = project,
 *     preferredHeight = 120,
 * ) { settings ->
 *     settings.isLineNumbersShown = false
 * }
 * ```
 *
 * @param project 当前项目，用于语言服务注入；传 null 时语法高亮仍生效，但部分补全功能不可用
 * @param value 初始文本内容，默认为空字符串
 * @param oneLineMode 是否单行模式；单行模式下回车键不换行，默认 false（多行）
 * @param preferredHeight 组件首选高度（px）；为 null 时由布局管理器决定高度
 * @param verticalScrollbarVisible 是否显示垂直滚动条，默认 true
 * @param horizontalScrollbarVisible 是否显示水平滚动条，默认 true
 * @param editorSettingApply 在默认 Editor 设置应用完毕后执行的回调，可用于覆盖默认值
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/03
 */
@Suppress("JComponentDataProvider")
class MarkdownTextField(
    project: Project? = null,
    value: String = "",
    oneLineMode: Boolean = false,
    preferredSize: Dimension? = null,
    val verticalScrollbarVisible: Boolean = true,
    val horizontalScrollbarVisible: Boolean = true,
    val editorSettingApply: (EditorSettings) -> Unit = {},
) : LanguageTextField(MarkdownLanguage.INSTANCE, project, value, oneLineMode) {

    init {
        preferredSize?.let {
            super.preferredSize = it
        }
    }

    /**
     * 创建底层 [EditorEx] 并应用默认配置。
     *
     * 父类 [LanguageTextField.createEditor] 负责创建 Editor 实例，
     * 本方法在其基础上追加滚动条和 [EditorSettings] 配置，
     * 最后通过 [editorSettingApply] 回调将控制权交还给调用方。
     */
    override fun createEditor(): EditorEx {
        val editor = super.createEditor()

        editor.setVerticalScrollbarVisible(verticalScrollbarVisible)
        editor.setHorizontalScrollbarVisible(horizontalScrollbarVisible)

        editor.settings.apply {
            // 左侧显示行号
            isLineNumbersShown = true
            // 启用自动代码折叠（如 Markdown 标题折叠）
            isAutoCodeFoldingEnabled = true
            // 显示折叠区域指示图标
            isFoldingOutlineShown = true
            // 允许单逻辑行内容折叠
            isAllowSingleLogicalLineFolding = true
            // 底部保留 1 行空白，避免内容紧贴边缘
            additionalLinesCount = 1
            // 启用软换行，长行自动折回，适合 Markdown 纯文本编辑
            isUseSoftWraps = true
            // 隐藏右侧列宽参考线，文本编辑场景通常不需要
            isRightMarginShown = false

            // 调用方自定义配置，可覆盖上方任意默认值
            editorSettingApply(this)
        }

        return editor
    }
}
