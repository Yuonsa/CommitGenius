package com.github.yuonsa.commitgenius.settings

import com.github.yuonsa.commitgenius.AppBundle
import com.github.yuonsa.commitgenius.services.AppPersistentStateComponent
import com.github.yuonsa.commitgenius.services.ProjectPersistentStateComponent
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/**
 * {@link ProjectConfigurable}.
 *
 * @author zys
 * @version 1.0.0
 * @since 2026/06/04
 */
class ProjectConfigurable(private val project: Project) : ApplicationConfigurable() {

    override fun isProjectLevel() = true

    override fun getDisplayName() = AppBundle["configurable.name.project"]

    override fun createComponent(): JComponent {
        boundFields.forEach { it.attachListener() }
        return super.createComponent()
    }

    override fun reset() {
        val app = AppPersistentStateComponent.instance.state
        val proj = ProjectPersistentStateComponent.instance(project).state
        boundFields.forEach { it.resetFromProj(proj, app) }
    }

    override fun apply() {
        val proj = ProjectPersistentStateComponent.instance(project).state
        boundFields.forEach { it.applyToProj(proj) }
    }

    override fun isModified(): Boolean {
        val proj = ProjectPersistentStateComponent.instance(project).state
        return boundFields.any { it.isModifiedProj(proj) }
    }
}
