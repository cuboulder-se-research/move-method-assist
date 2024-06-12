package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class RenameVariableFactory {
    companion object: MyRefactoringFactory {
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {

            val params = getParamsFromFuncCall(funcCall)
            val newName = getStringFromParam(params[1])
            val oldName = getStringFromParam(params[0])
            val functionPsi: PsiElement? =
                runReadAction {
                    PsiUtils.getParentFunctionOrNull(editor, language = file.language)?:
                    PsiUtils.getParentClassOrNull(editor, language = file.language)?:
                    file.getChildOfType<PsiClass>()
                }

            val renameObj = RenameVariableFactory.fromOldNewName(project, functionPsi, oldName, newName)
            if (renameObj!=null)
                return listOf(renameObj)
            return listOf()
        }

        override val logicalName: String
            get() = "Rename Variable"
        override val apiFunctionName: String
            get() = "rename_variable"
        // rename_variable("x", "count")
        override val APIDocumentation: String
            get() = """def rename_variable(old_variable_name, new_variable_name):
    ""${'"'}
    Renames occurrences of a variable within the scope of a function or method.

    This function is intended to refactor code by replacing all occurrences of the variable named `old_variable_name`
    with the new variable name `new_variable_name` within the scope of the function or method where it is called.

    Parameters:
    - old_variable_name (str): The name of the variable to be renamed.
    - new_variable_name (str): The new name for the variable.
    ""${'"'}
                    """.trimIndent()

        fun fromOldNewName(project: Project,
                           outerPsiElement: PsiElement?,
                           oldName:String,
                           newName: String): AbstractRefactoring?{
            val varPsi = runReadAction { PsiUtils.getVariableFromPsi(outerPsiElement, oldName) }
            if (varPsi!=null)
                return RenameVariable(
                    runReadAction{ varPsi.getLineNumber() },
                    runReadAction{ varPsi.getLineNumber() },
                    oldName, newName, varPsi)
            return null
        }

    }


}