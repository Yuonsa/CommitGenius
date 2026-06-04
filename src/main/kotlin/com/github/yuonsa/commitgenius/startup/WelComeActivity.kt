package com.github.yuonsa.commitgenius.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class WelComeActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().warn(
            "别忘了删除所有不需要的示例代码文件及其对应注册条目在“plugin.xml”中。"
        )
    }
}
