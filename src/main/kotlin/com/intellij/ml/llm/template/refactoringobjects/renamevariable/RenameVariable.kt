package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.RefactoringFactory
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType

class RenameVariable(
                     override val startLoc: Int,
                     override val endLoc: Int,
                     val oldName: String,
                     val newName: String
): AbstractRefactoring() {

    companion object{
        fun fromOldNewName(project: Project, functionPsiElement: PsiElement, oldName:String, newName: String): RenameVariable?{
            val varPsi = PsiUtils.getVariableFromPsi(functionPsiElement, oldName)
            if (varPsi!=null)
                return RenameVariable(1, 1, oldName, newName)
            return null
        }
    }

    fun doRename(){

//        val refactoringFactory=RefactoringFactory.getInstance(project)
//        val result = refactoringFactory.createRename(psiElement, newName)

//        val psiElement: PsiNamedElement? = null
//        val editor: Editor? = null
//        val project: Project? = null

//        VariableInplaceRenameHandler
//        val renamer = VariableInplaceRenamer(psiElement,
//            editor,project,"myString","newMyString")
//        renamer.performInplaceRename()
    }

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        val varPsi = PsiUtils.getVariableFromPsi(file, oldName)
        val variables = file.getChildrenOfType<PsiMethod>()

        for (v in variables){
            println("name: ${v.name}")
        }


        val refactoringFactory=RefactoringFactory.getInstance(project)
        val rename = varPsi?.let { refactoringFactory.createRename(it, newName) }
//        result?.run()
        val usages = rename?.findUsages()
//        RenameMethodStatistics.applyCount(selectedValue.second)
        rename?.doRefactoring(usages)

        print("Done??")


//        val renamer = VariableInplaceRenamer(varPsi,
//            editor,project,"myString","newMyString")
//        renamer.performInplaceRename()



//        TODO("Not yet implemented");
//        doRename()
    }

    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRefactoringName(): String {
        return RenameVariableFactory.logicalName
    }
}