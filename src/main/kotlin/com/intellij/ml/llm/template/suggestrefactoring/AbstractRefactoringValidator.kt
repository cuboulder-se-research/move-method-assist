package com.intellij.ml.llm.template.suggestrefactoring

import com.google.gson.Gson
import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.GetExtractMethodParametersPrompt
import com.intellij.ml.llm.template.prompts.GetRenameVariableParametersPrompt
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestion
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestionList
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

abstract class AbstractRefactoringValidator(
    private val efLLMRequestProvider: LLMRequestProvider,
    private val project: Project,
    private val functionSrc: String

) {

    abstract fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean;
    abstract fun getExtractMethodSuggestions(llmText: String): EFSuggestionList;

    fun getExtractMethodParameters(atomicSuggestion: AtomicSuggestion,
                                   finalCode: String): EFSuggestion{

        // TODO: generate chat messages.
        // TODO: Give LLM documentation for extract method API.
        // TODO: send to LLM.
        // TODO: package response in EFSuggestion.
        var messageList:MutableList<OpenAiChatMessage> = mutableListOf()
        val basePrompt = SuggestRefactoringPrompt().getPrompt(functionSrc)
        messageList.addAll(basePrompt)
        messageList.add(
            OpenAiChatMessage("assistant",
            Gson().toJson(RefactoringSuggestion(mutableListOf(atomicSuggestion), finalCode)).toString()
            )
        )

        messageList.addAll(
            GetExtractMethodParametersPrompt().getPrompt(atomicSuggestion.shortDescription, "extract_method")
        )

        val response = sendChatRequest(
            project, messageList, efLLMRequestProvider.chatModel, efLLMRequestProvider
        )
        var new_name = "newMethod"
        if (response != null) {
            val funcCall:String = response.getSuggestions()[0].text
            println(funcCall)
            if(funcCall.startsWith("extract_method"))
                print("Looks like a extract_method call!")
            new_name = funcCall.split(',')[2].removeSuffix(")")
                .replace("\"", "").replace(" ", "")
            if (new_name.contains("=")){
                new_name = new_name.split("=")[1].replace(" " ,"")
            }
            print("new name:$new_name")
        }
        return EFSuggestion(
                    functionName = new_name,
                    lineStart = atomicSuggestion.start,
                    lineEnd = atomicSuggestion.end,
                )
    }


    fun getRenameVariableParameters(atomicSuggestion: AtomicSuggestion,
                                   finalCode: String, functionPsiElement: PsiElement):
            RenameVariable?{

        var messageList:MutableList<OpenAiChatMessage> = mutableListOf()
        val basePrompt = SuggestRefactoringPrompt().getPrompt(functionSrc)
        messageList.addAll(basePrompt)
        messageList.add(
            OpenAiChatMessage("assistant",
                Gson().toJson(RefactoringSuggestion(mutableListOf(atomicSuggestion), finalCode)).toString()
            )
        )

        messageList.addAll(
            GetRenameVariableParametersPrompt().getPrompt(atomicSuggestion.shortDescription, "rename_variable")
        )

        val response = sendChatRequest(
            project, messageList, efLLMRequestProvider.chatModel, efLLMRequestProvider
        )
        var new_name = "newVariableName"
        var old_name = "oldVariableName"
        if (response != null) {
            val funcCall:String = response.getSuggestions()[0].text
            println(funcCall)
            if(funcCall.startsWith("rename_variable"))
                print("Looks like a rename_variable call!")
            new_name = funcCall.split(',')[1]
                .removeSuffix(")")
                .replace("\"", "")
                .replace(" ", "")
            print("new name:$new_name")

            old_name = funcCall.split(',')[0]
                .removeSuffix(")")
                .replace("\"", "")
                .replace(" ", "")
            println("old_name:$old_name")
        }
        return RenameVariable.fromOldNewName(project, functionPsiElement,
            "old_name", "new_name")
    }



    companion object{
        fun getRawSuggestions(llmText: String): RefactoringSuggestion{
            var refactoringSuggestion: RefactoringSuggestion;
            try {
                refactoringSuggestion= Gson().fromJson(llmText, RefactoringSuggestion::class.java)
            } catch (e: Exception){
                refactoringSuggestion = Gson().fromJson("{$llmText}", RefactoringSuggestion::class.java)
            }
            return refactoringSuggestion
        }
    }

    abstract fun getRenamveVariableSuggestions(llmText: String): MutableList<RenameVariable>
    abstract fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean
}