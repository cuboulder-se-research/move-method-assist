package com.intellij.ml.llm.template.prompts

import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage

class SuggestRefactoringPrompt: MethodPromptBase() {
    override fun getPrompt(methodCode: String): MutableList<OpenAiChatMessage> {
        return mutableListOf(
            OpenAiChatMessage("system", "You are an expert programmer looking for move method refactoring operations."),
            OpenAiChatMessage("user", """
                    Please provide suggestions to move methods inside the following Java method/class to other classes. Move method refactoring involves relocating methods to the class where they logically belong. This is an essential technique in object-oriented design.
                     
                    Your response should be formatted as a JSON object comprising two main fields. The first field, named 'improvements', should be a list of JSON objects, each with the following attributes: 'shortDescription' providing a brief summary of the improvement, 'longDescription' offering a detailed explanation of the improvement, 'start', indicating the starting line number where the improvement should be applied, 'end', indicating the ending line number where the improvement should be applied.
                    
                     1. public class A{
                     2.   B objB;
                     3.   int counter = 0;
                     4.   public void m1(){
                     5.       objB.foo();
                     6.       objB.bar();
                     7.   }
                     8.
                     9.   public void m2(C paramObjC){
                     10.       paramObjC.baz();
                     11.       paramObjC.foo2();
                     12.   }
                     13. }
                    

                     """.trimIndent()),
            OpenAiChatMessage("assistant", """
{
    "improvements": [
        {
            "shortDescription": "Move Method m1",
            "longDescription": "The method m1 in class A is primarily using the functionality of classe B. This indicates that the method might be more appropriately placed within class B.",
            "start": 4,
            "end": 7
        }
    ]
}
"""
            ),
            OpenAiChatMessage("user", methodCode)
        )
    }
}