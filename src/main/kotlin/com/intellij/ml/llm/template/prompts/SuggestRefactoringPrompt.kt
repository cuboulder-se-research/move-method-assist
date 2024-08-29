package com.intellij.ml.llm.template.prompts

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage

class SuggestRefactoringPrompt: MethodPromptBase() {

    companion object{
        const val systemMessageText = "You are an expert programmer performing refactoring operations."
        const val suggestionAskText = """Please provide suggestions to improve the following Java method/class. 
                    Only provide suggestions that are: Move Method"""
    }


    fun getPromptWithoutExample(methodCode: String): MutableList<ChatMessage>{
        return mutableListOf(
            SystemMessage.from(systemMessageText),
            UserMessage.from("""
                $suggestionAskText
                
                $methodCode
                """.trimIndent())
        )
    }


    override fun getPrompt(methodCode: String): MutableList<ChatMessage> {

        return mutableListOf(
            SystemMessage.from(systemMessageText),
            UserMessage.from("""
                    $suggestionAskText
                    
                    Ensure that your recommendations are specific to this method/class and are actionable immediately. 
                    Your response should be formatted as a JSON object comprising two main fields. The first field, named 'improvements', should be a list of JSON objects, each with the following attributes: 'shortDescription' providing a brief summary of the improvement, 'longDescription' offering a detailed explanation of the improvement, 'start', indicating the starting line number where the improvement should be applied, 'end', indicating the ending line number where the improvement should be applied.
                    
                     1.    public static int calculateSum(int[] arr) {
                     2.        int sum = 0;
                     3.        for (int i = 0; i < arr.length; i++) {
                     4.            sum += arr[i]; 
                     5.        }
                     6.        return sum;
                     7.    }
                     """.trimIndent()),
            AiMessage.from("""
{
    "improvements": [
        {
            "shortDescription": "Convert For Loop to Use Enhanced For Loop",
            "longDescription": "Instead of using a traditional for loop to iterate over `arr`, use an enhanced for loop.",
            "start": 3,
            "end": 3
        },
        {
            "shortDescription": "Rename Variable `arr` to `numbers`",
            "longDescription": "Rename `arr` to `numbers` to indicate that the array contains numeric values.",
            "start": 1,
            "end": 4
        }
    ]
}
"""
            ),
            UserMessage.from(methodCode)
        )
    }
}