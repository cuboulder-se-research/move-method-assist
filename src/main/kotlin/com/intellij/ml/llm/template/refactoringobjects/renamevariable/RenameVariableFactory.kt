package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.lang.Language
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.eval4j.checkNull

class RenameVariableFactory {
    companion object: MyRefactoringFactory {
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            val newName = funcCall.split(',')[1]
                .removeSuffix(")")
                .replace("\"", "")
                .replace(" ", "")
            print("new name:$newName")

            val oldName = funcCall.split(',')[0]
                .removeSuffix(")")
                .replace("\"", "")
                .replace(" ", "")
            println("old_name:$oldName")

            val functionPsi: PsiElement? = PsiUtils.Companion.getParentFunctionOrNull(editor, language = file.language)
            return listOf(
                RenameVariable.fromOldNewName(project, functionPsi, oldName, newName)!!
            )
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

    }
}