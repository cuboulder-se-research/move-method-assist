package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

class SuggestRefactoringPrompt: MethodPromptBase() {
    override fun getPrompt(methodCode: String): MutableList<OpenAiChatMessage> {
        return mutableListOf(
            OpenAiChatMessage("system", "You are an expert programmer."),
            OpenAiChatMessage("user", """
                    Please provide suggestions to improve the following Java method. 
                    Only provide suggestions that are: 1. Extract Method. 2. Rename Variable 
                    
                    Ensure that your recommendations are specific to this method, Your response should be formatted as a JSON object comprising two main fields. 
                    The first field, named 'improvements', should be a list of JSON objects, each with the following attributes: 'shortDescription' providing a brief summary of the improvement, 'longDescription' offering a detailed explanation of the improvement, 'start', indicating the starting line number where the improvement should be applied, 'end', indicating the ending line number where the improvement should be applied, 'changeDiff', differences in the git diff style representing the intended changes for this improvement.
                    The second field, named 'finalCode', should contain the code with all the suggested improvements applied. Please include only the JSON structure specified in your response.
                    
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
            "shortDescription": "Use enhanced for loop instead of traditional for loop",
            "changeDiff": "- for (int i = 0; i < arr.length; i++) {\n+ for (int num : arr) {",
            "longDescription": "Instead of using a traditional for loop to iterate over `arr`, use an enhanced for loop.",
            "start": 3,
            "end": 3
        }
    ],
    "finalCode": "    public static int calculateSum(int[] arr) {\n        int sum = 0;\n        for (int num : arr) {\n            sum += num; // Add each element to the sum\n        }\n\n        return sum;\n    }"
}
"""
            ),
            OpenAiChatMessage("user", methodCode)
        )
    }
}