package com.intellij.ml.llm.template.refactoringobjects.enhancedfor

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.siyeh.ig.migration.ForCanBeForeachInspection

class EnhancedForFactory {
    companion object: MyRefactoringFactory{
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val startLine = params[0].toInt()

            val refObj: EnhancedFor? = EnhancedFor.fromStartLoc(startLine, project, editor, file)
            if (refObj!=null)
                return listOf(refObj)
            return listOf()
        }

        override val logicalName: String
            get() = "Use Enhanced For Loop"
        override val apiFunctionName: String
            get() = "use_enhanced_forloop"
        override val APIDocumentation: String
            get() = """def use_enhanced_forloop(line_start):
    ""${'"'}
    Converts a conventional for-loop to an enhanced for-loop where applicable.

    This function is intended to refactor code by replacing conventional for-loops with enhanced for-loops (also known as 
    "foreach" loops) where applicable, starting from the specified line number `line_start`. It assumes that the necessary 
    updates to the source code are handled externally.

    Parameters:
    - line_start (int): The line number from which to start searching for conventional for-loops to convert. Must be a positive integer.
    ""${'"'}""".trimIndent()

    }
}