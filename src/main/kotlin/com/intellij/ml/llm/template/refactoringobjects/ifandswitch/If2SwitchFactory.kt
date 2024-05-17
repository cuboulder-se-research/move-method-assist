package com.intellij.ml.llm.template.refactoringobjects.ifandswitch

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class If2SwitchFactory {
    companion object: MyRefactoringFactory{
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            TODO("Not yet implemented")
        }

        override val logicalName: String
            get() = TODO("Not yet implemented")
        override val apiFunctionName: String
            get() = TODO("Not yet implemented")
        override val APIDocumentation: String
            get() = TODO("Not yet implemented")

    }
}