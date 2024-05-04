package com.intellij.ml.llm.template.refactoringobjects
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.RefactoringFactory
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.refactoring.rename.inplace.VariableInplaceRenamer;
class RenameVariable(
                     override val startLoc: Int,
                     override val endLoc: Int,
                     val oldName: String,
                     val newName: String
): AbstractRefactoring {

    companion object{
        fun fromOldNewName(project: Project, functionPsiElement: PsiElement, oldName:String, newName: String): RenameVariable?{
            val varPsi = PsiUtils.getVariableFromPsi(functionPsiElement, oldName)
            if (varPsi!=null)
                return RenameVariable(project, varPsi, newName)
            return null
        }
    }

    fun doRename(){

        val refactoringFactory=RefactoringFactory.getInstance(project)
        val result = refactoringFactory.createRename(psiElement, newName)

//        val psiElement: PsiNamedElement? = null
//        val editor: Editor? = null
//        val project: Project? = null

//        VariableInplaceRenameHandler
//        val renamer = VariableInplaceRenamer(psiElement,
//            editor,project,"myString","newMyString")
//        renamer.performInplaceRename()
    }

    override fun performRefactoring() {
        TODO("Not yet implemented")
        doRename()
    }

    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRefactoringName(): String {
        return RenameVariableFactory.logicalName
    }
}