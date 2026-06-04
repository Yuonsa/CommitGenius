package com.github.yuonsa.commitgenius.settings.field

import com.github.yuonsa.commitgenius.settings.binding.UIBinding
import com.github.yuonsa.commitgenius.settings.state.AppSettingState
import com.github.yuonsa.commitgenius.settings.state.ProjectSettingState
import com.intellij.ui.components.JBLabel
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

/**
 * 描述一个配置字段的完整元信息
 *
 * @param C Swing 控件类型
 * @param T 字段值类型
 */
class SettingFieldDef<C : JComponent, T> internal constructor(
    val label: StringOrLabel,
    val comment: String = "",
    val fillX: Boolean = true,
    val appGet: (AppSettingState) -> T,
    val appSet: (AppSettingState, T) -> Unit,
    val projGet: (ProjectSettingState) -> T?,
    val projSet: (ProjectSettingState, T?) -> Unit,
    val binding: UIBinding<C, T>,
) {
    val createComponent get() = binding.createComponent
    val uiGet get() = binding.uiGet
    val uiSet get() = binding.uiSet

    companion object {

        // 标准构造：App 非 nullable，Project nullable
        operator fun <C : JComponent, T> invoke(
            label: String,
            comment: String = "",
            fillX: Boolean = true,
            appProp: KMutableProperty1<AppSettingState, T>,
            projProp: KMutableProperty1<ProjectSettingState, T?>,
            binding: UIBinding<C, T>,
        ) = SettingFieldDef(
            label = StringOrLabel.StringValue(label),
            comment = comment,
            fillX = fillX,
            appGet = appProp::get,
            appSet = appProp::set,
            projGet = projProp::get,
            projSet = projProp::set,
            binding = binding,
        )

        // nullable 构造：App 侧本身就是 nullable（coreRules 等）
        // 用 appDefault 提供非 null 的默认值供 UI 展示
        operator fun <C : JComponent, T> invoke(
            label: String,
            comment: String = "",
            fillX: Boolean = true,
            appProp: KMutableProperty1<AppSettingState, T?>,
            projProp: KMutableProperty1<ProjectSettingState, T?>,
            appDefault: T,                          // App 为 null 时 UI 展示的默认值
            appSave: (AppSettingState, T) -> Unit,  // 存回 App 时的处理（可做 takeIf 等）
            binding: UIBinding<C, T>,
        ) = SettingFieldDef(
            label = StringOrLabel.StringValue(label),
            comment = comment,
            fillX = fillX,
            appGet = { appProp.get(it) ?: appDefault },
            appSet = appSave,
            projGet = projProp::get,
            projSet = projProp::set,
            binding = binding,
        )

        // 标准构造：App 非 nullable，Project nullable
        operator fun <C : JComponent, T> invoke(
            label: StringOrLabel,
            comment: String = "",
            fillX: Boolean = true,
            appProp: KMutableProperty1<AppSettingState, T>,
            projProp: KMutableProperty1<ProjectSettingState, T?>,
            binding: UIBinding<C, T>,
        ) = SettingFieldDef(
            label = label,
            comment = comment,
            fillX = fillX,
            appGet = appProp::get,
            appSet = appProp::set,
            projGet = projProp::get,
            projSet = projProp::set,
            binding = binding,
        )

        operator fun <C : JComponent, T> invoke(
            label: StringOrLabel,
            comment: String = "",
            fillX: Boolean = true,
            appProp: KMutableProperty1<AppSettingState, T?>,
            projProp: KMutableProperty1<ProjectSettingState, T?>,
            appDefault: T,                          // App 为 null 时 UI 展示的默认值
            appSave: (AppSettingState, T) -> Unit,  // 存回 App 时的处理（可做 takeIf 等）
            binding: UIBinding<C, T>,
        ) = SettingFieldDef(
            label = label,
            comment = comment,
            fillX = fillX,
            appGet = { appProp.get(it) ?: appDefault },
            appSet = appSave,
            projGet = projProp::get,
            projSet = projProp::set,
            binding = binding,
        )
    }

    sealed class StringOrLabel {
        data class StringValue(val value: String) : StringOrLabel()
        data class LabelValue(val value: JBLabel) : StringOrLabel()
    }
}
