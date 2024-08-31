package com.intellij.ml.llm.template.intentions

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.prompts.MoveMethodRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import com.intellij.ml.llm.template.telemetry.TelemetryDataAction
import com.intellij.ml.llm.template.telemetry.TelemetryElapsedTimeObserver
import com.intellij.ml.llm.template.ui.RefactoringSuggestionsPanel
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import dev.langchain4j.data.message.ChatMessage
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference


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
                val refactoringSuggestions =try {
                    (JsonParser.parseString(llmText.text) as JsonArray)
                        .map {
                            try{
                                Gson().fromJson(it, MoveMethodSuggestion::class.java )
                            } catch (e: Exception){
                                print("failed to decode json ->$it")
                                null
                            }
                        }.filterNotNull()
                } catch (e: Exception) {
                    print("Failed to parse ${llmText.text}")
                    e.printStackTrace()
                    null
                }
                if (refactoringSuggestions!=null) {
                    llmResponseCache[cacheKey] ?: llmResponseCache.put(cacheKey, response) // cache response
                    vanillaLLMSuggestions.addAll(refactoringSuggestions)
                }
            }
        }
        val uniqueSuggestions = vanillaLLMSuggestions.distinctBy { it.methodName }
        if (uniqueSuggestions.isEmpty()){
            // show message to user.
            invokeLater { showEFNotification(
                project,
                LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                NotificationType.INFORMATION
            ) }
        }
        else {
            val priority = getSuggestionPriority(uniqueSuggestions, project)
            if (priority!=null){
                createRefactoringObjectsAndShowSuggestions(priority)
            }
        }

    }

    private fun createRefactoringObjectsAndShowSuggestions(moveMethodSuggestions: List<MoveMethodSuggestion>) {
        invokeLater {
            val refObjs = moveMethodSuggestions
                .map {
                    MoveMethodFactory.createMoveMethodFromName(
                        currentEditor,
                        currentFile,
                        it.methodName,
                        currentProject,
                        llmChatModel
                    )
                }
                .reduce { acc, abstractRefactorings -> acc + abstractRefactorings }
            showRefactoringOptionsPopup(currentProject, currentEditor, currentFile, refObjs, codeTransformer)
        }

    }
    fun showRefactoringOptionsPopup(
        project: Project,
        editor: Editor,
        file: PsiFile,
        candidates: List<AbstractRefactoring>,
        codeTransformer: CodeTransformer
    ) {
        val highlighter = AtomicReference(ScopeHighlighter(editor))
        val efPanel = RefactoringSuggestionsPanel(
            project = project,
            editor = editor,
            file = file,
            candidates = candidates,
            codeTransformer = codeTransformer,
            highlighter = highlighter,
            efTelemetryDataManager = telemetryDataManager,
            button_name = LLMBundle.message("ef.candidates.popup.extract.function.button.title")
        )
        efPanel.initTable()
        val elapsedTimeTelemetryDataObserver = TelemetryElapsedTimeObserver()
        efPanel.addObserver(elapsedTimeTelemetryDataObserver)
        val panel = efPanel.createPanel()

        // Create the popup
        val efPopup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, efPanel.myExtractFunctionsCandidateTable)
                .setRequestFocus(true)
                .setTitle(LLMBundle.message("ef.candidates.popup.title"))
                .setResizable(true)
                .setMovable(true)
                .setCancelOnClickOutside(false)
                .createPopup()

        // Add onClosed listener
        efPopup.addListener(object : JBPopupListener {
            override fun onClosed(event: LightweightWindowEvent) {
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.STOP, 0)
                    )
                )
                buildElapsedTimeTelemetryData(elapsedTimeTelemetryDataObserver)
                highlighter.getAndSet(null).dropHighlight()
                sendTelemetryData()
            }

            override fun beforeShown(event: LightweightWindowEvent) {
                super.beforeShown(event)
                elapsedTimeTelemetryDataObserver.update(
                    EFNotification(
                        EFTelemetryDataElapsedTimeNotificationPayload(TelemetryDataAction.START, 0)
                    )
                )
            }
        })

        // set the popup as delegate to the Extract Function panel
        efPanel.setDelegatePopup(efPopup)

        // Show the popup at the top right corner of the current editor
        val contentComponent = editor.contentComponent
        val visibleRect: Rectangle = contentComponent.visibleRect
        val point = Point(visibleRect.x + visibleRect.width - 500, visibleRect.y)
        efPopup.show(RelativePoint(contentComponent, point))
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
                return uniqueSuggestions.sortedBy {
                    val index = methodPriority2.indexOf(it.methodName)
                    if (index == -1){
                        uniqueSuggestions.size+1
                    }
                    else{
                        index
                    }
                }
        }
        return null
    }

    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {
        throw Exception("Shouldn't be here.")
    }


}