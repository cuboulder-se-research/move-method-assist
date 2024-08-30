package com.intellij.ml.llm.template.intentions

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.prompts.MoveMethodRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.langchain4j.data.message.ChatMessage

class ApplyMoveMethodInteractiveIntention: ApplySuggestRefactoringIntention() {
    var MAX_ITERS = RefAgentSettingsManager.getInstance().getNumberOfIterations()
    override var prompter: MethodPromptBase = MoveMethodRefactoringPrompt()
    lateinit var currentEditor: Editor
    lateinit var currentFile: PsiFile
    lateinit var currentProject: Project

    data class MoveSuggestionList(
        val suggestionList: List<MoveMethodSuggestion>
    )

    data class MoveMethodSuggestion(
        @SerializedName("method_name")
        val methodName:String,
        @SerializedName("method_signature")
        val methodSignature: String,
        @SerializedName("target_class")
        val targetClass: String,
        @SerializedName("rationale")
        val rationale:String
    )

    override fun getFamilyName(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.move.method.family.name")
    }

    override fun getText(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.move.method.family.name")
    }


    override fun invokeLLM(project: Project, messageList: MutableList<ChatMessage>, editor: Editor, file: PsiFile) {

        currentFile = file
        currentEditor = editor
        currentProject = project

        MAX_ITERS = RefAgentSettingsManager.getInstance().getNumberOfIterations()
        llmChatModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!

        val vanillaLLMSuggestions = mutableListOf<MoveMethodSuggestion>()

        for (iter in 1..MAX_ITERS){
            val cacheKey = functionSrc + iter.toString()
            val response = llmResponseCache[cacheKey]?: sendChatRequest(project, messageList, llmChatModel)

            if (response!=null) {
                val llmText = response.getSuggestions()[0]
                val refactoringSuggestion =try {
                    Gson().fromJson(llmText.text, MoveSuggestionList::class.java)
                } catch (e: Exception) {
                    print("Failed to parse ${llmText.text}")
                    e.printStackTrace()
                    null
                }
                if (refactoringSuggestion!=null) {
                    llmResponseCache[cacheKey] ?: llmResponseCache.put(cacheKey, response) // cache response
                    vanillaLLMSuggestions.addAll(refactoringSuggestion.suggestionList)
                }
            }
        }
        val uniqueSuggestions = vanillaLLMSuggestions.distinctBy { it.methodName }
        if (uniqueSuggestions.isEmpty()){
            // show message to user.
        }
        else {
            val priority = getSuggestionPriority(uniqueSuggestions, project)
            if (priority!=null){
                createRefactoringObjectsAndShowSuggestions(priority)
            }
        }

    }

    private fun createRefactoringObjectsAndShowSuggestions(moveMethodSuggestions: List<MoveMethodSuggestion>): List<AbstractRefactoring> {
        return moveMethodSuggestions
            .map { MoveMethodFactory.createMoveMethodFromName(currentEditor, currentFile, it.methodName, currentProject, llmChatModel) }
            .reduce { acc, abstractRefactorings -> acc + abstractRefactorings }
    }

    private fun getSuggestionPriority(
        uniqueSuggestions: List<MoveMethodSuggestion>,
        project: Project
    ) : List<MoveMethodSuggestion>? {
        val messages =
            (prompter as MoveMethodRefactoringPrompt).askForMethodPriorityPrompt(functionSrc, uniqueSuggestions)
        val response = sendChatRequest(project, messages, llmChatModel)
        if (response != null) {
            var methodPriority = mutableListOf<String>()
            val methodPriority2 = try {
                Gson().fromJson(response.getSuggestions()[0].text, methodPriority::class.java)
            } catch (e: Exception) {
                null
            }
            if (methodPriority2!=null)
                return uniqueSuggestions.sortedBy { methodPriority2.indexOf(it.methodName) }
        }
        return null
    }

    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {
//        super.processLLMResponse(response, project, editor, file)
    }


}