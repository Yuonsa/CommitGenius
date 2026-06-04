package com.github.yuonsa.commitgenius

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

object AppBundle : DynamicBundle(AppBundle.BUNDLE) {

    @NonNls
    const val BUNDLE = "messages.AppBundle"

    operator fun get(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?) =
        getMessage(key, *params)

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?) =
        getMessage(key, *params)

    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?) =
        getLazyMessage(key, *params)
}
