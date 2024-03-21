package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

class GetExtractMethodParametersPrompt {

    companion object{
        fun getExtractMethodParameters():String{
            return """def extract_method(line_start, line_end, new_function_name):
    ""${'"'}
    Extracts a method from the specified range of lines in a source code file and creates a new function with the given name.

    This function is intended to refactor a block of code within a file, taking the lines from `line_start` to `line_end`, 
    inclusive, and moving them into a new function named `new_function_name`. The original block of code is replaced with a 
    call to the newly created function. 

    Parameters:
    - line_start (int): The starting line number from which the block of code will be extracted. Must be a positive integer.
    - line_end (int): The ending line number to which the block of code will be extracted. Must be a positive integer greater than or equal to `line_start`.
    - new_function_name (str): The name of the new function that will contain the extracted block of code. Must be a valid Python function name.
 
                    ""${'"'}
                    """.trimIndent()
        }
    }


    fun getPrompt(
        improvementDescription: String,
        refactoringType: String
                  ): MutableList<OpenAiChatMessage> {
        var parameter_prompt:String=""
        if (refactoringType.equals("extract_method")){
            parameter_prompt = getExtractMethodParameters()
        }


        return mutableListOf(
            OpenAiChatMessage("user", """
                
                You suggested to improve the method by doing the following: \"$improvementDescription\". 
                Please provide a function call to a python function that performs this $refactoringType refactoring(see documentation below). 
                Respond with ONLY a call to the function below:
                Here is the signature of the function:
                $parameter_prompt
                   
                """.trimIndent())
        )
    }
}