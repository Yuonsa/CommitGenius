package com.github.yuonsa.commitgenius.services

import com.github.yuonsa.commitgenius.settings.state.ProjectSettingState
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * 插件的项目配置
 */
@Service(Service.Level.PROJECT)
@State(
    name = ProjectPersistentStateComponent.SERVICE_NAME,
    storages = [Storage("CommitGeniusProjectSettings.xml")]
)
class ProjectPersistentStateComponent : PersistentStateComponent<ProjectSettingState> {

    private var state: ProjectSettingState = ProjectSettingState()

    override fun getState(): ProjectSettingState = state

    override fun loadState(state: ProjectSettingState) {
        this.state = state
    }

    companion object {
        private const val SERVICE_NAME: String = "com.github.yuonsa.commitgenius.services.ProjectPersistentStateComponent"

        fun instance(project: Project): ProjectPersistentStateComponent =
            project.getService(ProjectPersistentStateComponent::class.java)
    }
}
