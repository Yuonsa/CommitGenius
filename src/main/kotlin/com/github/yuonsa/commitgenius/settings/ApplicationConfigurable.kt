package com.github.yuonsa.commitgenius.settings

import com.github.yuonsa.commitgenius.AppBundle
import com.github.yuonsa.commitgenius.settings.bound.BoundField
import com.github.yuonsa.commitgenius.settings.field.SettingFieldDef
import com.github.yuonsa.commitgenius.services.AppPersistentStateComponent
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * {@link ApplicationConfigurable}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
open class ApplicationConfigurable : Configurable {

    // 不再用 open val，改为构造参数或抽象方法
    protected open fun isProjectLevel(): Boolean = false

    // lazy 延迟初始化，等到真正使用时才执行，此时子类已完成初始化
    protected val boundFields: List<BoundField<*, *>> by lazy {
        SettingLayout.groups
            .flatMap { it.fields }
            .map { def ->
                BoundField(
                    def = def,
                    checkBox = if (isProjectLevel()) JBCheckBox(AppBundle["ui.action.override"]) else null
                )
            }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun <C : JComponent, T> bound(def: SettingFieldDef<C, T>): BoundField<C, T> =
        boundFields.first { it.def === def } as BoundField<C, T>

    override fun getDisplayName() = AppBundle["configurable.name.app"]

    override fun createComponent(): JComponent = panel {
        SettingLayout.groups.forEach { group ->
            val block: Panel.() -> Unit = {
                group.fields.forEach { def -> fieldRow(def) }
            }
            if (group.collapsible) collapsibleGroup(title = group.title, init = block)
            else group(title = group.title, init = block)
        }
    }

    private fun Panel.fieldRow(def: SettingFieldDef<*, *>) {
        val bf = bound(def)
        val init: Row.() -> Unit = {
            cell(bf.component)
                .also { if (def.fillX) it.align(AlignX.FILL) }
                .also { if (def.comment.isNotEmpty()) it.comment(def.comment) }
                .resizableColumn()
            bf.checkBox?.let { cell(it).align(AlignX.RIGHT) }
        }
        when (val label = def.label) {
            is SettingFieldDef.StringOrLabel.StringValue -> row(label.value, init)
            is SettingFieldDef.StringOrLabel.LabelValue  -> row(label.value, init)
        }
    }

    override fun reset() {
        val app = AppPersistentStateComponent.instance.state
        boundFields.forEach { it.resetFromApp(app) }
    }

    override fun apply() {
        val app = AppPersistentStateComponent.instance.state
        boundFields.forEach { it.applyToApp(app) }
    }

    override fun isModified(): Boolean {
        val app = AppPersistentStateComponent.instance.state
        return boundFields.any { it.isModifiedApp(app) }
    }
}
