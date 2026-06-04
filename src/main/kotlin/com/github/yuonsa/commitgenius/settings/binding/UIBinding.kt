package com.github.yuonsa.commitgenius.settings.binding

import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

/**
 * 描述控件的读写方式.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
class UIBinding<C : JComponent, T> private constructor(
    val createComponent: () -> C,
    val uiGet: (C) -> T,
    val uiSet: (C, T) -> Unit,
) {
    companion object {

        // 能用属性引用时走这个
        operator fun <C : JComponent, T> invoke(
            createComponent: () -> C,
            prop: KMutableProperty1<C, T>,
        ) = UIBinding(
            createComponent = createComponent,
            uiGet = prop::get,
            uiSet = prop::set,
        )

        // 无法用属性引用时走这个
        operator fun <C : JComponent, T> invoke(
            createComponent: () -> C,
            uiGet: (C) -> T,
            uiSet: (C, T) -> Unit,
        ) = UIBinding(createComponent, uiGet, uiSet)
    }
}
