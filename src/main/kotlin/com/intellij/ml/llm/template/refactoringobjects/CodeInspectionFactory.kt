package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class CodeInspectionFactory<T: PsiElement>(
    override val logicalName: String,
    override val apiFunctionName: String,
    override val APIDocumentation: String, // first param to API call is the start line of element
    val psiClass: Class<out T>,
    val inspection: AbstractBaseJavaLocalInspectionTool, // inspection object, with the right options triggered.
    val refactoringPreview: (PsiElement) -> String,
    val isOnTheFly: Boolean = false
) : MyRefactoringFactory {
    override fun createObjectsFromFuncCall(
        funcCall: String,
        project: Project,
        editor: Editor,
        file: PsiFile
    ): List<AbstractRefactoring> {
        val params = getParamsFromFuncCall(funcCall)
        val startLine = params[0].toInt()

        val refObj = this.fromStartLoc(
            startLine, project, editor, file
        )
        if (refObj!=null)
            return listOf(refObj)
        return listOf()

    }

    fun fromStartLoc(startLine: Int, project: Project, editor: Editor, file: PsiFile)
    : AbstractRefactoring? {
        return runReadAction {
            val elementAtStartLine =
                PsiUtilBase.getElementAtOffset(
                    file, editor.document.getLineStartOffset(startLine - 1)
                )

            val startOffset = editor.document.getLineStartOffset(startLine - 1)
            val endOffset = editor.document.getLineStartOffset(startLine)
            val foundElements = PsiTreeUtil
                .findChildrenOfType(elementAtStartLine.parent, psiClass)
                .filter { it.startOffset in (startOffset..endOffset) }
            if (foundElements.isEmpty())
                return@runReadAction null
            if (foundElements[0] != null) {
                val problemsHolder = ProblemsHolder(
                    InspectionManager.getInstance(project), file, isOnTheFly
                )
                val visitor = inspection.buildVisitor(problemsHolder, isOnTheFly)
                foundElements[0].accept(visitor)
                if (problemsHolder.hasResults())
                    return@runReadAction InspectionBasedRefactoring(
                        startLine, startLine, foundElements[0], problemsHolder, refactoringPreview
                    )
                else
                    return@runReadAction null // cannot be transformed to refactoring object
            }
            return@runReadAction null
        }
    }


    private class InspectionBasedRefactoring(
        override val startLoc: Int,
        override val endLoc: Int,
        val psiElement: PsiElement,
        val problemsHolder: ProblemsHolder,
        val refactoringPreview: (PsiElement) -> String
    ) : AbstractRefactoring() {
        override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
            super.performRefactoring(project, editor, file)
            val p0 = problemsHolder.results[0]!!
            WriteCommandAction.runWriteCommandAction(project,
                Runnable {  p0.fixes?.get(0)?.applyFix(project, p0)})
        }

        override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
            isValid = true
            return isValid!!
        }

        override fun getRefactoringPreview(): String {
            return refactoringPreview(psiElement)
        }

        override fun getStartOffset(): Int {
            return psiElement.startOffset
        }

        override fun getEndOffset(): Int {
            return psiElement.endOffset
        }

    }
}