package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

/*
This prompt passes the documentation of a refactoring API and
asks the llm to fill out the parameters and make a valid API call.
 */
class GetRefactoringObjParametersPrompt {

    companion object{
        fun getExtractMethodParameters():String{
            return
        }

        fun get(improvementDescription: String,
                refactoringType: String,
                refactoringApiDocumentation: String
                ): List<OpenAiChatMessage>{

            return mutableListOf(
                OpenAiChatMessage("user", """
                
                You suggested to improve the method by doing the following: \"$improvementDescription\". 
                Please provide a function call to a python function that performs this $refactoringType refactoring(see documentation below). 
                Respond with ONLY a call to the function below:
                Here is the signature of the function:
                $refactoringApiDocumentation
                   
                """.trimIndent())
            )
        }
    }


}