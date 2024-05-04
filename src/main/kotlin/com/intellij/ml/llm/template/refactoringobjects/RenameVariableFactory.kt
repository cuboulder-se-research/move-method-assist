package com.intellij.ml.llm.template.refactoringobjects

class RenameVariableFactory {
    companion object: MyRefactoringFactory{
        override fun createObjectFromFuncCall(funcCall: String): AbstractRefactoring {
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

            return RenameVariable(1, 1, oldName, newName)
        }

        override val logicalName: String
            get() = "Rename Variable"
        override val apiFunctionName: String
            get() = "rename_variable"
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