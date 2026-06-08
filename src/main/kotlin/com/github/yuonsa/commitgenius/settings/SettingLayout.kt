package com.github.yuonsa.commitgenius.settings

import com.github.yuonsa.commitgenius.AppBundle
import com.github.yuonsa.commitgenius.core.PromptBuilder
import com.github.yuonsa.commitgenius.core.agent.LLMEngine
import com.github.yuonsa.commitgenius.settings.binding.ComponentBinding
import com.github.yuonsa.commitgenius.settings.binding.UIBinding
import com.github.yuonsa.commitgenius.settings.dsl.settingLayout
import com.github.yuonsa.commitgenius.settings.field.SettingFieldDef
import com.github.yuonsa.commitgenius.settings.state.AppSettingState
import com.github.yuonsa.commitgenius.settings.state.ProjectSettingState
import org.jetbrains.annotations.PropertyKey
import javax.swing.JComponent
import kotlin.reflect.KMutableProperty1

object SettingFields {

    private fun <C : JComponent, T> createI18nField(
        @PropertyKey(resourceBundle = AppBundle.BUNDLE) label: String,
        @PropertyKey(resourceBundle = AppBundle.BUNDLE) comment: String? = null,
        appProp: KMutableProperty1<AppSettingState, T>,
        projProp: KMutableProperty1<ProjectSettingState, T?>,
        binding: UIBinding<C, T>
    ) = SettingFieldDef(
        label = AppBundle[label],
        comment = if (!comment.isNullOrBlank()) AppBundle[comment] else "",
        appProp = appProp,
        projProp = projProp,
        binding = binding
    )

    val provider = createI18nField(
        label = "configurable.group.model.provider.label",
        comment = "configurable.group.model.provider.desc",
        appProp = AppSettingState::provider,
        projProp = ProjectSettingState::provider,
        binding = ComponentBinding.comboBox(
            LLMEngine.supportedProviders().toTypedArray()
        ) { it.display }
    )

    val apiKey = createI18nField(
        label = "configurable.group.model.apiKey.label",
        comment = "configurable.group.model.apiKey.desc",
        appProp = AppSettingState::apiKey,
        projProp = ProjectSettingState::apiKey,
        binding = ComponentBinding.passwordField()
    )

    val baseUrl = createI18nField(
        label = "configurable.group.model.baseUrl.label",
        comment = "configurable.group.model.baseUrl.desc",
        appProp = AppSettingState::baseUrl,
        projProp = ProjectSettingState::baseUrl,
        binding = ComponentBinding.textField()
    )

    val modelId = createI18nField(
        label = "configurable.group.model.modelId.label",
        comment = "configurable.group.model.modelId.desc",
        appProp = AppSettingState::modelId,
        projProp = ProjectSettingState::modelId,
        binding = ComponentBinding.textField()
    )
}

val SettingLayout = settingLayout {

    bundleGroup("configurable.group.model.name") {
        +SettingFields.provider

        +SettingFields.apiKey

        +SettingFields.baseUrl

        +SettingFields.modelId
    }

    bundleCollapsibleGroup("configurable.group.rule.name") {

        field {
            label("configurable.group.rule.outputTemplate.label")
            comment("configurable.group.rule.outputTemplate.desc")
            appProp = AppSettingState::outputTemplate
            projProp = ProjectSettingState::outputTemplate
            binding = ComponentBinding.comboBox(
                PromptBuilder.OutputTemplate.entries.toTypedArray(),
                { it.name }
            )
        }

        field {
            label("configurable.group.rule.coreRules.label")
            comment("configurable.group.rule.coreRules.desc")
            appGet = { it.coreRules ?: "" }
            appSet = { s, v -> s.coreRules = v.takeIf { it.isNotEmpty() } }
            projProp = ProjectSettingState::coreRules
            binding = ComponentBinding.markdownField()
        }

        field {
            label("configurable.group.rule.outputRequirements.label")
            comment("configurable.group.rule.outputRequirements.desc")
            appGet = { it.outputRequirements ?: "" }
            appSet = { s, v -> s.outputRequirements = v.takeIf { it.isNotEmpty() } }
            projProp = ProjectSettingState::outputRequirements
            binding = ComponentBinding.markdownField()
        }
    }
}
