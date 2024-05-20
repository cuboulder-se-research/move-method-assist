package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

class SuggestRefactoringPrompt: MethodPromptBase() {
    override fun getPrompt(methodCode: String): MutableList<OpenAiChatMessage> {
        return mutableListOf(
            OpenAiChatMessage("system", "You are an expert programmer."),
            OpenAiChatMessage("user", """
                    Please provide suggestions to improve the following Java method. 
                    Only provide suggestions that are: 
                    1. Extract Method. 
                    2. Rename Variable 
                    3. Use Enhanced For Loop
                    4. Convert For Loop to While Loop
                    5. Convert For loop to use Java Streams 
                    6. Use Enhanced Switch Statement
                    7. Convert If Statement to Switch Statement (and vice versa)
                    8. Convert If Statement to Ternary Operator (and vice versa)
                    
                    Ensure that your recommendations are specific to this method, Your response should be formatted as a JSON object comprising two main fields. 
                    The first field, named 'improvements', should be a list of JSON objects, each with the following attributes: 'shortDescription' providing a brief summary of the improvement, 'longDescription' offering a detailed explanation of the improvement, 'start', indicating the starting line number where the improvement should be applied, 'end', indicating the ending line number where the improvement should be applied.
                    
                     1.    public static int calculateSum(int[] arr) {
                     2.        int sum = 0;
                     3.        for (int i = 0; i < arr.length; i++) {
                     4.            sum += arr[i]; 
                     5.        }
                     6.        return sum;
                     7.    }
                     """.trimIndent()),
            OpenAiChatMessage("assistant", """
{
    "improvements": [
        {
            "shortDescription": "Use Enhanced For Loop",
            "longDescription": "Instead of using a traditional for loop to iterate over `arr`, use an enhanced for loop.",
            "start": 3,
            "end": 3
        },
        {
            "shortDescription": "Rename Variable",
            "longDescription": "Rename `arr` to `numbers` to indicate that the array contains numeric values.",
            "start": 1,
            "end": 4
        }
    ]
}
"""
            ),
            OpenAiChatMessage("user", methodCode)
        )
    }
}