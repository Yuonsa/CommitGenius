package com.github.yuonsa.commitgenius.settings.dsl

import com.github.yuonsa.commitgenius.AppBundle
import com.github.yuonsa.commitgenius.settings.binding.UIBinding
import com.github.yuonsa.commitgenius.settings.field.SettingFieldDef
import com.github.yuonsa.commitgenius.settings.state.AppSettingState
import com.github.yuonsa.commitgenius.settings.state.ProjectSettingState
import com.intellij.ui.components.JBLabel
import org.jetbrains.annotations.PropertyKey
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

/**
 * {@link SettingFieldDefBuilder}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
@SettingDsl
class SettingFieldDefBuilder<C : JComponent, T> {

    var label: SettingFieldDef.StringOrLabel? = null
    var comment: String = ""
    var fillX: Boolean = true
    var appProp: KMutableProperty1<AppSettingState, T>? = null
    var projProp: KMutableProperty1<ProjectSettingState, T?>? = null

    // 自定义 lambda，优先级高于 appProp
    var appGet: ((AppSettingState) -> T)? = null
    var appSet: ((AppSettingState, T) -> Unit)? = null
    var binding: UIBinding<C, T>? = null

    fun label(@PropertyKey(resourceBundle = AppBundle.BUNDLE) label: String) {
        this.label = SettingFieldDef.StringOrLabel.StringValue(AppBundle[label])
    }

    fun label(label: JBLabel) {
        this.label = SettingFieldDef.StringOrLabel.LabelValue(label)
    }

    fun comment(@PropertyKey(resourceBundle = AppBundle.BUNDLE) label: String) {
        this.comment = AppBundle[label]
    }

    fun build(): SettingFieldDef<C, T> {
        val resolvedAppGet = appGet ?: requireNotNull(appProp) { "appProp is required" }::get
        val resolvedAppSet = appSet ?: requireNotNull(appProp) { "appProp is required" }::set
        return SettingFieldDef(
            label = requireNotNull(label) { "label is required" },
            comment = comment,
            fillX = fillX,
            appGet = resolvedAppGet,
            appSet = resolvedAppSet,
            projGet = requireNotNull(projProp) { "projProp is required" }::get,
            projSet = requireNotNull(projProp) { "projProp is required" }::set,
            binding = requireNotNull(binding) { "binding is required" },
        )
    }
}
