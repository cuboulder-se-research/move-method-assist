package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.RefactoringFactory
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.editor.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class RenameVariable(
                     override val startLoc: Int,
                     override val endLoc: Int,
                     val oldName: String,
                     val newName: String,
                     val oldVarPsi: PsiElement

): AbstractRefactoring() {

    companion object{
        fun fromOldNewName(project: Project,
                           functionPsiElement: PsiElement?,
                           oldName:String,
                           newName: String): AbstractRefactoring?{
            val varPsi = PsiUtils.getVariableFromPsi(functionPsiElement, oldName)
            if (varPsi!=null)
                return RenameVariable(varPsi.getLineNumber(),
                    varPsi.getLineNumber(), oldName, newName, varPsi)
            return null
        }
    }

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
//        val varPsi = PsiUtils.getVariableFromPsi(file, oldName)
        val refactoringFactory = RefactoringFactory.getInstance(project)
        val rename = refactoringFactory.createRename(oldVarPsi, newName)
        val usages = rename?.findUsages()
        rename?.doRefactoring(usages)

//        val renamer = VariableInplaceRenamer(psiElement,
//            editor,project,"myString","newMyString")
//        renamer.performInplaceRename()
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        // Valid if oldName exists and newName doesn't
        return PsiUtils.getVariableFromPsi(file, oldName)!=null
                && PsiUtils.getVariableFromPsi(file, newName)==null
    }

    override fun getRefactoringName(): String {
        return RenameVariableFactory.logicalName
    }

    override fun getStartOffset(): Int {
        return oldVarPsi.startOffset
    }

    override fun getEndOffset(): Int {
        return oldVarPsi.endOffset
    }
}