package com.github.yuonsa.commitgenius.settings.state

import com.github.yuonsa.commitgenius.services.AppPersistentStateComponent
import com.github.yuonsa.commitgenius.services.ProjectPersistentStateComponent
import com.intellij.openapi.project.Project

object EffectiveSettings {

    fun resolve(project: Project? = null): AppSettingState {
        val app = AppPersistentStateComponent.instance.state
        val proj = project?.let { ProjectPersistentStateComponent.instance(it).state }
                   ?: return app

        return AppSettingState(
            provider = proj.provider ?: app.provider,
            apiKey = proj.apiKey ?: app.apiKey,
            baseUrl = proj.baseUrl ?: app.baseUrl,
            modelId = proj.modelId ?: app.modelId,
            outputTemplate = proj.outputTemplate ?: app.outputTemplate,
            coreRules = proj.coreRules ?: app.coreRules,
            outputRequirements = proj.outputRequirements ?: app.outputRequirements
        )
    }
}
