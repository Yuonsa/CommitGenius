package com.github.yuonsa.commitgenius.settings.dsl

/*
 @SettingDsl 注解 + settingLayout 入口函数
 */

@DslMarker
annotation class SettingDsl

// 顶层入口函数
fun settingLayout(block: SettingLayoutBuilder.() -> Unit) =
    SettingLayoutBuilder().apply(block)
