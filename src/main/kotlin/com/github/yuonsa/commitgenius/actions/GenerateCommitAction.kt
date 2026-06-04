package com.github.yuonsa.commitgenius.actions

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import com.github.yuonsa.commitgenius.AppBundle
import com.github.yuonsa.commitgenius.core.DiffAnalyzer
import com.github.yuonsa.commitgenius.core.PromptBuilder
import com.github.yuonsa.commitgenius.core.agent.LLMEngine
import com.github.yuonsa.commitgenius.core.notification.Notifier
import com.github.yuonsa.commitgenius.settings.state.EffectiveSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportProgressScope
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * {@link GenerateCommitAction}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/05/21
 */
class GenerateCommitAction : AnAction({ AppBundle["ui.action.GenerateCommitAction.title"] }) {

    private val isClickable = AtomicBoolean(true)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val commitWorkflow = e.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) ?: return
        val includedChanges = commitWorkflow.getIncludedChanges()
        val includedUnversionedFiles = commitWorkflow.getIncludedUnversionedFiles()

        val commitMessage = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL)
        // 禁用按钮
        isClickable.set(false)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val content = generateMessage(
                    project,
                    includedChanges,
                    includedUnversionedFiles,
                )
                if (content.isNullOrBlank()) {
                    Notifier.warn("commit.diff.content.empty", project)
                    return@launch
                }

                // 3. 拿到结果切回主线程回填
                withContext(Dispatchers.EDT) {
                    commitMessage?.setCommitMessage(content)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    thisLogger().debug("Generate commit message cancelled")
                    return@launch
                }
                thisLogger().error("Generate commit message failed", e)
            } finally {
                // 启用按钮
                isClickable.set(true)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.isEnabled = isClickable.get()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    suspend fun generateMessage(
        project: Project,
        includedChanges: List<Change>,
        includedUnversionedFiles: List<FilePath>,
    ): String? {
        val diffAnalyzer = DiffAnalyzer(project)

        return withBackgroundProgress(
            project,
            AppBundle["backgroundProcess.generateCommit.title"],
            TaskCancellation.cancellable()
        ) {
            reportProgressScope(100) { reporter ->
                // 1. 后台异步安全拿 Diff
                val diffContent = reporter.sizedStep(
                    20,
                    AppBundle["backgroundProcess.generateCommit.obtain-diff"]
                ) {
                    readAction {
                        diffAnalyzer.buildDiffPayload(includedChanges, includedUnversionedFiles)
                    }
                }
                if (diffContent.isBlank()) return@reportProgressScope null as String?

                // 2. 构建prompt
                val prompt = reporter.sizedStep(
                    10,
                    AppBundle["backgroundProcess.generateCommit.buildPrompt"]
                ) {
                    PromptBuilder.buildPrompt(
                        diffContent, PromptBuilder.OutputTemplate.Ultra
                    )
                }

                // 3. 初始化模型参数
                val llmModel = reporter.sizedStep(
                    10,
                    AppBundle["backgroundProcess.generateCommit.init-model-param"]
                ) {
                    val state = EffectiveSettings.resolve(project)
                    LLModel(
                        state.provider,
                        state.modelId,
                        listOf(
                            LLMCapability.OpenAIEndpoint.Completions,
                            LLMCapability.Tools,
                            LLMCapability.Completion,
                        )
                    )
                }

                // 3. 请求大模型
                reporter.sizedStep(
                    60,
                    AppBundle["backgroundProcess.generateCommit.call-model"].format(llmModel.id)
                ) {
                    val buffer = StringBuilder()

                    reporter.indeterminateStep(AppBundle["backgroundProcess.generateCommit.call-model.generating"]) {
                        LLMEngine.executeStreaming(
                            project,
                            prompt,
                            llmModel,
                            contentFlowAction = {
                                buffer.append(it)
                            }
                        )
                    }

                    buffer.toString()
                }
            }
        }
    }
}
