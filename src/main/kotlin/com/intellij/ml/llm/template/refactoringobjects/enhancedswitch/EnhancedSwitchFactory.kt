package com.intellij.ml.llm.template.refactoringobjects.enhancedswitch

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class EnhancedSwitchFactory {
    companion object: MyRefactoringFactory{
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val startLine = params[0].toInt()

            val refObj = EnhancedSwitch.fromStartLoc(
                startLine, project, editor, file
            )
            if (refObj!=null)
                return listOf(refObj)
            return listOf()

        }

        override val logicalName: String
            get() = "Use Enhaced Switch Statement"
        override val apiFunctionName: String
            get() = "use_enhanced_switch"
        override val APIDocumentation: String
            get() = TODO("Not yet implemented")

    }
}