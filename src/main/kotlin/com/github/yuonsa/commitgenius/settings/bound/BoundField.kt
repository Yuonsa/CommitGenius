package com.github.yuonsa.commitgenius.settings.bound

import com.github.yuonsa.commitgenius.settings.field.SettingFieldDef
import com.github.yuonsa.commitgenius.services.AppPersistentStateComponent
import com.github.yuonsa.commitgenius.settings.state.AppSettingState
import com.github.yuonsa.commitgenius.settings.state.ProjectSettingState
import com.intellij.ui.components.JBCheckBox
import javax.swing.JComponent

/**
 * {@link BoundField}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
class BoundField<C : JComponent, T>(
    val def: SettingFieldDef<C, T>,
    val component: C = def.createComponent(),
    val checkBox: JBCheckBox? = null,
) {
    private val isOverriding get() = checkBox?.isSelected ?: true

    // 直接在内部取全局值，不需要外部传 lambda
    private fun globalValue(): T = def.appGet(AppPersistentStateComponent.instance.state)

    // ✅ 内部持有类型安全的 get/set，外部不暴露 T
    private fun getUiValue(): T = def.uiGet(component)
    private fun setUiValue(v: T) = def.uiSet(component, v)

    fun attachListener() {
        attachListener(::globalValue)
    }

    fun attachListener(globalValue: () -> T) {
        checkBox ?: return
        component.isEnabled = false
        checkBox.addActionListener {
            component.isEnabled = isOverriding
            if (!isOverriding) setUiValue(globalValue())
        }
    }

    // ---- App 页（无泛型暴露）----
    fun resetFromApp(state: AppSettingState) = setUiValue(def.appGet(state))
    fun applyToApp(state: AppSettingState) = def.appSet(state, getUiValue())
    fun isModifiedApp(state: AppSettingState): Boolean = getUiValue() != def.appGet(state)

    // ---- Project 页 ----
    fun resetFromProj(projState: ProjectSettingState, appState: AppSettingState) {
        val saved = def.projGet(projState)
        checkBox!!.isSelected = saved != null
        component.isEnabled = saved != null
        setUiValue(saved ?: def.appGet(appState))
    }

    fun applyToProj(projState: ProjectSettingState) {
        def.projSet(projState, if (isOverriding) getUiValue() else null)
    }

    fun isModifiedProj(projState: ProjectSettingState): Boolean {
        val saved = def.projGet(projState)
        if (isOverriding != (saved != null)) return true
        return isOverriding && getUiValue() != saved
    }
}
