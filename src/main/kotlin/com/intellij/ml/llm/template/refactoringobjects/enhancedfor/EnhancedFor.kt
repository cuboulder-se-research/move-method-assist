package com.intellij.ml.llm.template.refactoringobjects.enhancedfor

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiForStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.siyeh.ig.migration.ForCanBeForeachInspection
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class EnhancedFor(
    override val startLoc: Int,
    override val endLoc: Int,
    val forStatement: PsiForStatement,
    val problemsHolder: ProblemsHolder
) : AbstractRefactoring() {

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        val p0 = problemsHolder.results[0]!!
        WriteCommandAction.runWriteCommandAction(project,
            Runnable {  p0.fixes?.get(0)?.applyFix(project, p0)})
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        return true
    }

    override fun getRefactoringPreview(): String {
        return "Use enhanced for loop on line $startLoc"
    }

    override fun getStartOffset(): Int {
        return forStatement.startOffset
    }

    override fun getEndOffset(): Int {
        return forStatement.endOffset
    }

    companion object {
        fun fromStartLoc(startLine: Int, project: Project, editor: Editor, file: PsiFile): EnhancedFor? {

            val element =
                PsiUtilBase.getElementAtOffset(
                    file, editor.document.getLineStartOffset(startLine))
            val forStatement = PsiTreeUtil.getParentOfType(element, PsiForStatement::class.java)

            if (forStatement!=null){
                val foreachInspection = ForCanBeForeachInspection()
                val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
                val visitor = foreachInspection.buildVisitor(problemsHolder, false)
                forStatement.accept(visitor)

                if (problemsHolder.hasResults())
                    return EnhancedFor(startLine, startLine, forStatement, problemsHolder)
                return null
            }
            return null


        }
    }
}