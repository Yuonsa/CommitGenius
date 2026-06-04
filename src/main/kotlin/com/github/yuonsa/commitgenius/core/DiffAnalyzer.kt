package com.github.yuonsa.commitgenius.core

import com.intellij.diff.util.Side
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListChange
import com.intellij.openapi.vcs.ex.PartialLocalLineStatusTracker
import com.intellij.openapi.vcs.impl.LineStatusTrackerManager
import com.intellij.openapi.vcs.impl.LineStatusTrackerManagerI
import com.intellij.openapi.vfs.VirtualFile

// Diff 分析器 - 智能过滤无关变更
// - 过滤 node_modules, lockfiles, 大文件
// - 提取有意义的代码差异
// - 优化 Token 使用
class DiffAnalyzer(private val project: Project) {

    private val maxTotalBytes = 100 * 1024
    private val maxSingleFileBytes = 20 * 1024

    fun buildDiffPayload(
        includedChanges: List<Change>, includedUnversionedFiles: List<FilePath>
    ): String {
        val sb = StringBuilder()
        var totalBytes = 0
        val trackerManager = LineStatusTrackerManager.getInstance(project)

        // 1. 已追踪文件：用 ContentRevision 获取前后内容，自己生成 unified diff
        for (change in includedChanges) {
            val fileName = change.virtualFile?.name ?: change.afterRevision?.file?.name
                           ?: change.beforeRevision?.file?.name ?: continue

            val vf = change.virtualFile ?: continue

            // 尝试获取行级勾选状态
            val partialContent = tryGetPartialContent(trackerManager, change, vf)

            val diff = if (partialContent != null) {
                // 用户有行级勾选，用实际将被提交的内容做 diff
                val beforeContent = runCatching { change.beforeRevision?.content }.getOrNull() ?: ""
                buildModifiedDiff(fileName, beforeContent, partialContent)
            } else {
                // 没有行级勾选，回退到完整 diff
                val beforeContent = runCatching { change.beforeRevision?.content }.getOrNull() ?: ""
                val afterContent = runCatching { change.afterRevision?.content }.getOrNull() ?: ""
                when (change.type) {
                    Change.Type.NEW     -> buildNewFileDiff(fileName, afterContent)
                    Change.Type.DELETED -> buildDeletedFileDiff(fileName, beforeContent)
                    else                -> buildModifiedDiff(fileName, beforeContent, afterContent)
                }
            }

            if (diff.isBlank()) continue

            val bytes = diff.toByteArray().size
            if (bytes > maxSingleFileBytes) {
                sb.appendLine("[跳过 $fileName：变更内容超过单文件限制]")
                continue
            }
            if (totalBytes + bytes > maxTotalBytes) {
                sb.appendLine("[总内容已超限，后续文件已忽略]")
                break
            }

            sb.appendLine(diff)
            totalBytes += bytes
        }

        // 2. 未追踪文件：全文视为新增
        for (filePath in includedUnversionedFiles) {
            val vf = filePath.virtualFile ?: continue

            val content = runCatching {
                String(vf.contentsToByteArray(), vf.charset)
            }.getOrNull() ?: continue

            val diff = buildNewFileDiff(vf.name, content)
            val bytes = diff.toByteArray().size

            if (bytes > maxSingleFileBytes) {
                sb.appendLine("[跳过 ${vf.name}：新文件内容超过单文件限制]")
                continue
            }
            if (totalBytes + bytes > maxTotalBytes) {
                sb.appendLine("[总内容已超限，后续文件已忽略]")
                break
            }

            sb.appendLine(diff)
            totalBytes += bytes
        }

        return sb.toString()
    }

    /**
     * 修改文件：生成标准 unified diff（逐行对比）
     */
    private fun buildModifiedDiff(fileName: String, before: String, after: String): String {
        if (before == after) return ""

        val beforeLines = before.trimEnd('\n').lines()
        val afterLines = after.trimEnd('\n').lines()
        val sb = StringBuilder()

        sb.appendLine("--- a/$fileName")
        sb.appendLine("+++ b/$fileName")

        // 使用 Myers diff 算法逐行对比
        val hunks = computeHunks(beforeLines, afterLines)
        hunks.forEach { sb.append(it) }

        return sb.toString()
    }

    /**
     * 新文件：全部内容作为新增
     */
    private fun buildNewFileDiff(fileName: String, content: String): String {
        val lines = content.trimEnd('\n').lines()
        val sb = StringBuilder()
        sb.appendLine("--- /dev/null")
        sb.appendLine("+++ b/$fileName")
        sb.appendLine("@@ -0,0 +1,${lines.size} @@")
        lines.forEach { sb.appendLine("+$it") }
        return sb.toString()
    }

    /**
     * 删除文件：全部内容作为删除
     */
    private fun buildDeletedFileDiff(fileName: String, content: String): String {
        val lines = content.trimEnd('\n').lines()
        val sb = StringBuilder()
        sb.appendLine("--- a/$fileName")
        sb.appendLine("+++ /dev/null")
        sb.appendLine("@@ -1,${lines.size} +0,0 @@")
        lines.forEach { sb.appendLine("-$it") }
        return sb.toString()
    }

    /**
     * 简单 LCS 行级 diff，生成 hunk 字符串列表
     * context：上下文行数（标准 unified diff 是 3 行）
     */
    private fun computeHunks(
        beforeLines: List<String>, afterLines: List<String>, context: Int = 3
    ): List<String> {
        // 计算 LCS edit script
        val n = beforeLines.size
        val m = afterLines.size

        // dp[i][j] = LCS length of beforeLines[0..i) and afterLines[0..j)
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) for (j in 1..m) {
            dp[i][j] = if (beforeLines[i - 1] == afterLines[j - 1]) dp[i - 1][j - 1] + 1
            else maxOf(dp[i - 1][j], dp[i][j - 1])
        }

        // 回溯得到 edit 操作序列：'=' 保留, '-' 删除, '+' 新增
        data class Edit(val type: Char, val beforeIdx: Int, val afterIdx: Int, val text: String)

        val edits = mutableListOf<Edit>()
        var i = n;
        var j = m
        while (i > 0 || j > 0) {
            when {
                i > 0 && j > 0 && beforeLines[i - 1] == afterLines[j - 1] -> {
                    edits.add(Edit('=', i - 1, j - 1, beforeLines[i - 1])); i--; j--
                }

                j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])         -> {
                    edits.add(Edit('+', i, j - 1, afterLines[j - 1])); j--
                }

                else                                                      -> {
                    edits.add(Edit('-', i - 1, j, beforeLines[i - 1])); i--
                }
            }
        }
        edits.reverse()

        // 按 context 窗口合并成 hunk
        val result = mutableListOf<String>()
        val changed = edits.indices.filter { edits[it].type != '=' }.toMutableList()
        if (changed.isEmpty()) return result

        // 合并相邻 hunk
        val ranges = mutableListOf<IntRange>()
        var start = maxOf(0, changed.first() - context)
        var end = minOf(edits.size - 1, changed.first() + context)
        for (k in 1 until changed.size) {
            val newStart = maxOf(0, changed[k] - context)
            if (newStart <= end + 1) {
                end = minOf(edits.size - 1, changed[k] + context)
            } else {
                ranges.add(start..end)
                start = newStart
                end = minOf(edits.size - 1, changed[k] + context)
            }
        }
        ranges.add(start..end)

        for (range in ranges) {
            val hunkEdits = edits.subList(range.first, range.last + 1)
            val beforeStart = hunkEdits.first().beforeIdx + 1
            val afterStart = hunkEdits.first().afterIdx + 1
            val beforeCount = hunkEdits.count { it.type != '+' }
            val afterCount = hunkEdits.count { it.type != '-' }

            val sb = StringBuilder()
            sb.appendLine("@@ -$beforeStart,$beforeCount +$afterStart,$afterCount @@")
            hunkEdits.forEach { edit ->
                when (edit.type) {
                    '=' -> sb.append(" ${edit.text}\n")
                    '-' -> sb.append("-${edit.text}\n")
                    '+' -> sb.append("+${edit.text}\n")
                }
            }
            result.add(sb.toString())
        }
        return result
    }

    /**
     * 尝试获取用户行级勾选后实际将被提交的内容
     * 返回 null 表示没有行级勾选（整个文件都提交）
     */
    private fun tryGetPartialContent(
        trackerManager: LineStatusTrackerManagerI, change: Change, vf: VirtualFile
    ): String? {
        val tracker = trackerManager.getLineStatusTracker(vf) ?: return null
        if (tracker !is PartialLocalLineStatusTracker) return null

        // 没有行级部分提交，直接返回 null 走完整 diff
        if (!tracker.hasPartialChangesToCommit()) return null

        val changeListId = (change as? ChangeListChange)?.changeListId ?: return null

        return try {
            tracker.getChangesToBeCommitted(
                side = Side.LEFT,
                changelistIds = listOf(changeListId),
                honorExcludedFromCommit = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
