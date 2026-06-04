package com.github.yuonsa.commitgenius.settings.state

import ai.koog.prompt.llm.LLMProvider
import com.github.yuonsa.commitgenius.core.PromptBuilder

// App 级别：字段全部必填
data class AppSettingState(
    var provider: LLMProvider = LLMProvider.OpenAI,
    var apiKey: String = "",
    var baseUrl: String = "",
    var modelId: String = "",
    var outputTemplate: PromptBuilder.OutputTemplate = PromptBuilder.OutputTemplate.Std,
    var coreRules: String? = null,
    var outputRequirements: String? = null,
)
