package com.github.yuonsa.commitgenius.core.agent

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.streaming.StreamFrame
import com.github.yuonsa.commitgenius.settings.state.EffectiveSettings
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.collectIndexed
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * {@link LLMEngine}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/05/29
 */
object LLMEngine {

    suspend fun executeStreaming(
        project: Project? = null,
        prompt: Prompt,
        llmModel: LLModel,
        action: (String) -> Unit
    ) {
        val streaming = build(project).executeStreaming(prompt, llmModel)
        streaming.collectIndexed { index, frame ->
            when (frame) {
                is StreamFrame.ReasoningDelta -> frame.text?.let { action(it) }
                is StreamFrame.TextDelta      -> action(frame.text)
                else                          -> {}
            }
        }
    }

    /**
     * Execute streaming.
     *
     * @param prompt  prompt
     * @param llmModel llm model
     * @param reasoningFlowAction reasoning flow action
     * @param contentFlowAction content flow action
     * @return reasoning, content
     */
    suspend fun executeStreaming(
        project: Project? = null,
        prompt: Prompt, llmModel: LLModel,
        reasoningFlowAction: (suspend (String) -> Unit)? = null,
        contentFlowAction: (suspend (String) -> Unit)? = null,
    ): Pair<String, String> {
        val streaming = build(project).executeStreaming(prompt, llmModel)
        val reasoning = StringBuilder()
        val content = StringBuilder()
        val millis = System.currentTimeMillis()
        var firstTimestamp: Long = millis
        var finishedTimestamp: Long = millis
        streaming.collect { frame ->
            if (firstTimestamp == millis) {
                firstTimestamp = System.currentTimeMillis()
            }
            when (frame) {
                is StreamFrame.ReasoningDelta -> {
                    frame.text?.let {
                        reasoning.append(it)
                        reasoningFlowAction?.invoke(it)
                    }
                }

                is StreamFrame.TextDelta      -> {
                    content.append(frame.text)
                    contentFlowAction?.invoke(frame.text)
                }

                is StreamFrame.End            -> {
                    finishedTimestamp = System.currentTimeMillis()
                }

                else                          -> {}
            }
        }
        val timeFirst = (firstTimestamp - millis).toDuration(DurationUnit.MILLISECONDS)
        val timeFinished = (finishedTimestamp - millis).toDuration(DurationUnit.MILLISECONDS)
        thisLogger().info(
            "Model: ${llmModel.id}, Time(first/end): $timeFirst, $timeFinished"
        )

        return reasoning.toString() to content.toString()
    }

    suspend fun models(provider: LLMProvider, apiKey: String, baseUrl: String? = null) =
        build(provider, apiKey, baseUrl).models()

    suspend fun models(project: Project? = null) = build(project).models()

    fun build(provider: LLMProvider, apiKey: String, baseUrl: String? = null) =
        MultiLLMPromptExecutor(provider to buildClientInternal(provider, apiKey, baseUrl))

    fun build(project: Project? = null): MultiLLMPromptExecutor {
        val state = EffectiveSettings.resolve(project)
        return buildInternal(state.provider, state.apiKey, state.baseUrl)
    }

    private fun buildInternal(provider: LLMProvider, apiKey: String, baseUrl: String? = null) =
        MultiLLMPromptExecutor(provider to buildClientInternal(provider, apiKey, baseUrl))

    @Suppress("UnstableApiUsage")
    private fun buildClientInternal(
        provider: LLMProvider,
        apiKey: String,
        baseUrl: String? = null
    ): LLMClient {
        val factory = KtorKoogHttpClient.Factory()
        val effectiveBaseUrl = baseUrl ?: ""
        return when (provider) {
            LLMProvider.OpenAI -> OpenAILLMClient(
                apiKey = apiKey,
                settings = OpenAIClientSettings(
                    baseUrl = effectiveBaseUrl.ifBlank { OpenAIClientSettings().baseUrl }
                ),
                httpClientFactory = factory
            )

            LLMProvider.Anthropic -> AnthropicLLMClient(
                apiKey = apiKey,
                settings = AnthropicClientSettings(
                    baseUrl = effectiveBaseUrl.ifBlank { AnthropicClientSettings().baseUrl },
                    modelVersionsMap = object : HashMap<LLModel, String>() {
                        override fun get(key: LLModel): String = key.id
                    }
                ),
                httpClientFactory = factory
            )

            else -> throw UnsupportedOperationException("Unsupported provider: $provider")
        }
    }
}
