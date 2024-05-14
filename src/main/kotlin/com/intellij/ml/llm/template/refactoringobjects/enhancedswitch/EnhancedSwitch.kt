package com.intellij.ml.llm.template.refactoringobjects.enhancedswitch

import com.intellij.codeInspection.EnhancedSwitchMigrationInspection
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase

class EnhancedSwitch(
    override val startLoc: Int,
    override val endLoc: Int,
    val theSwitchStatement: PsiSwitchStatement,
    val problemsHolder: ProblemsHolder
) : AbstractRefactoring() {


    companion object{
        fun fromStartLoc(startLoc: Int,
                         project: Project, editor: Editor, file: PsiFile): EnhancedSwitch?{

            val element =
                PsiUtilBase.getElementAtOffset(
                    file, editor.document.getLineStartOffset(startLoc))
            val switchStatement = PsiTreeUtil.getParentOfType(element, PsiSwitchStatement::class.java)
            if (switchStatement!=null){

                val switchMigrationInspection = EnhancedSwitchMigrationInspection()
                val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
                val visitor = switchMigrationInspection.buildVisitor(problemsHolder, false)
                switchStatement.accept(visitor)
                if (problemsHolder.hasResults())
                    return EnhancedSwitch(startLoc, startLoc, switchStatement, problemsHolder)
                else
                    return null // cannot be transformed to enhanced switch.
            }
            return null
        }
    }

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        val p0 = problemsHolder.results[0]!!
//        p0.fixes?.get(0)?.startInWriteAction()
//        p0.fixes?.get(0)?.applyFix(project, p0)
//        Runnable r = ()-> EditorModificationUtil.insertStringAtCaret(editor, string);
//        val r: Runnable =  p0.fixes?.get(0)?.applyFix(project, p0)
//        Runnable {  }
        WriteCommandAction.runWriteCommandAction(project,
            Runnable {  p0.fixes?.get(0)?.applyFix(project, p0)})
//        WriteCommandAction.runWriteCommandAction(project, r);
    }


    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRefactoringPreview(): String {
        TODO("Not yet implemented")
    }

    override fun getStartOffset(): Int {
        TODO("Not yet implemented")
    }

    override fun getEndOffset(): Int {
        TODO("Not yet implemented")
    }
}