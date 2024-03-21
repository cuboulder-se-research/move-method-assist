package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

class GetRenameVariableParametersPrompt {
    companion object{
        fun getRenameVariableParameters():String{
            return """def rename_variable(old_variable_name, new_variable_name):
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


    fun getPrompt(
        improvementDescription: String,
        refactoringType: String
    ): MutableList<OpenAiChatMessage> {
        var parameter_prompt: String = ""
//        if (refactoringType.equals("rename_variable")){
        parameter_prompt = getRenameVariableParameters()
//        }


        return mutableListOf(
            OpenAiChatMessage(
                "user", """
                
                You suggested to improve the method by doing the following: \"$improvementDescription\". 
                Please provide a function call to a python function that performs this $refactoringType refactoring(see documentation below). 
                Respond with ONLY a call to the function below:
                Here is the signature of the function:
                $parameter_prompt
                   
                """.trimIndent()
            )
        )
    }
}