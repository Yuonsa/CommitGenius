package com.github.yuonsa.commitgenius.services

import ai.koog.prompt.llm.LLMProvider
import com.github.yuonsa.commitgenius.settings.state.AppSettingState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * 插件持久化配置
 * - API Key
 * - ACP 配置文件路径 (acp.json)
 * - 模型选择
 * - 其他用户偏好设置
 */
@Service
@State(
    name = AppPersistentStateComponent.SERVICE_NAME,
    storages = [Storage("CommitGeniusSettings.xml")]
)
class AppPersistentStateComponent : PersistentStateComponent<AppSettingState> {

    private var state: AppSettingState = AppSettingState()

    override fun getState(): AppSettingState = state

    override fun loadState(state: AppSettingState) {
        this.state = state
    }

    companion object {
        private const val SERVICE_NAME: String = "com.github.yuonsa.commitgenius.services.AppPersistentStateComponent"

        val instance: AppPersistentStateComponent
            get() = ApplicationManager.getApplication()
                .getService(AppPersistentStateComponent::class.java)

        fun supportedProviders() = listOf(LLMProvider.OpenAI, LLMProvider.Anthropic)
    }
}
