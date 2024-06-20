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
                     val oldVarPsi: PsiElement,
                     val outerPsiElement: PsiElement

): AbstractRefactoring() {


    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        super.performRefactoring(project, editor, file)
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
//        return PsiUtils.getVariableFromPsi(file, oldName)!=null
//                && PsiUtils.getVariableFromPsi(file, newName)==null
        return true
    }

    override fun getRefactoringPreview(): String {
        return "${RenameVariableFactory.logicalName} $oldName -> $newName"
    }

    override fun getStartOffset(): Int {
        return oldVarPsi.startOffset
    }

    override fun getEndOffset(): Int {
        return oldVarPsi.endOffset
    }

    override fun getReverseRefactoringObject(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
        return RenameVariableFactory.fromOldNewName(
            project, outerPsiElement,
            newName, oldName
        )
    }
}