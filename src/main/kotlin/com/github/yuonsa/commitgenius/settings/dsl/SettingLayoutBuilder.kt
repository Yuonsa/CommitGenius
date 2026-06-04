package com.github.yuonsa.commitgenius.settings.dsl

import com.github.yuonsa.commitgenius.AppBundle
import com.github.yuonsa.commitgenius.settings.field.SettingFieldDef
import org.jetbrains.annotations.PropertyKey
import javax.swing.JComponent

@SettingDsl
class SettingGroupBuilder(val title: String, val collapsible: Boolean) {

    val fields = mutableListOf<SettingFieldDef<*, *>>()

    fun <C : JComponent, T> field(block: SettingFieldDefBuilder<C, T>.() -> Unit) {
        fields.add(SettingFieldDefBuilder<C, T>().apply(block).build())
    }

    // 直接添加已定义好的字段
    operator fun SettingFieldDef<*, *>.unaryPlus() = fields.add(this)
}

@SettingDsl
class SettingLayoutBuilder {
    val groups = mutableListOf<SettingGroupBuilder>()

    fun group(title: String, block: SettingGroupBuilder.() -> Unit) {
        groups.add(SettingGroupBuilder(title, collapsible = false).apply(block))
    }

    fun collapsibleGroup(title: String, block: SettingGroupBuilder.() -> Unit) {
        groups.add(SettingGroupBuilder(title, collapsible = true).apply(block))
    }

    fun bundle(
        @PropertyKey(resourceBundle = AppBundle.BUNDLE) key: String,
        collapsible: Boolean = false,
        block: SettingGroupBuilder.() -> Unit
    ) {
        val title = AppBundle[key]
        groups.add(SettingGroupBuilder(title, collapsible).apply(block))
    }

    fun bundleGroup(
        @PropertyKey(resourceBundle = AppBundle.BUNDLE) key: String,
        block: SettingGroupBuilder.() -> Unit
    ) {
        bundle(key, collapsible = false, block)
    }

    fun bundleCollapsibleGroup(
        @PropertyKey(resourceBundle = AppBundle.BUNDLE) key: String,
        block: SettingGroupBuilder.() -> Unit
    ) {
        bundle(key, collapsible = true, block)
    }
}
