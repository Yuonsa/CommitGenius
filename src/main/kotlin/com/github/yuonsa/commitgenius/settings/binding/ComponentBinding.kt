package com.github.yuonsa.commitgenius.settings.binding

import com.github.yuonsa.commitgenius.components.MarkdownTextField
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import javax.swing.ListCellRenderer

/**
 * {@link ComponentBinding}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
object ComponentBinding {

    /**
     * @see com.intellij.ui.components.JBTextField
     */
    fun textField() = UIBinding(
        createComponent = { JBTextField() },
        uiGet = { it.text },
        uiSet = { c, v -> c.setText(v) },
    )

    fun markdownField(trim: Boolean = true, block: (MarkdownTextField.() -> Unit)? = null) =
        UIBinding(
            createComponent = { MarkdownTextField().apply { block?.invoke(this) } },
            uiGet = { if (trim) it.text.trim() else it.text },
            uiSet = { c, v -> c.setText(v) },
        )

    /**
     * @see com.intellij.ui.components.JBPasswordField
     */
    fun passwordField() = UIBinding(
        createComponent = { JBPasswordField() },
        uiGet = { String(it.password) },
        uiSet = { c, v -> c.setText(v) },
    )

    /**
     * @see com.intellij.openapi.ui.ComboBox
     */
    fun <T> comboBox(
        items: Array<T>,
        toDisplay: (T) -> String = { it.toString() },
    ) = comboBox(
        items = items,
        renderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = toDisplay(value)
        }
    )

    /**
     * @see com.intellij.openapi.ui.ComboBox
     */
    fun <T> comboBox(
        items: Array<T>,
        renderer: ListCellRenderer<T>,
    ) = UIBinding(
        createComponent = {
            ComboBox(items).apply {
                this.renderer = renderer
            }
        },
        uiGet = {
            @Suppress("UNCHECKED_CAST")
            it.selectedItem as T
        },
        uiSet = { c, v -> c.selectedItem = v },
    )
}
