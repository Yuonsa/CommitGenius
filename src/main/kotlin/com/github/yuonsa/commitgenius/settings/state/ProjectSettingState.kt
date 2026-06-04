package com.github.yuonsa.commitgenius.settings.state

import ai.koog.prompt.llm.LLMProvider
import com.github.yuonsa.commitgenius.core.PromptBuilder

// Project 级别：全部 nullable，null = 继承全局
data class ProjectSettingState(
    var provider: LLMProvider? = null,
    var apiKey: String? = null,
    var baseUrl: String? = null,
    var modelId: String? = null,
    var outputTemplate: PromptBuilder.OutputTemplate? = null,
    var coreRules: String? = null,
    var outputRequirements: String? = null,
)
