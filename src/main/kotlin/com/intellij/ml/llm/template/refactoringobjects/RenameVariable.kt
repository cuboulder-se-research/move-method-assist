package com.intellij.ml.llm.template.refactoringobjects
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer;
class RenameVariable {
    fun doRename(){
        val psiElement: PsiNamedElement? = null
        val editor: Editor? = null
        val project: Project? = null
//        val renamer = VariableInplaceRenamer(psiElement,
//            editor,project,"myString","newMyString")
//        renamer.performInplaceRename()
    }
}