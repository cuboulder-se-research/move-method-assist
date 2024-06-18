package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.GPTExtractFunctionRequestProvider
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.models.openai.OpenAiChatMessage
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.prompts.SuggestRefactoringPrompt
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.suggestrefactoring.SimpleRefactoringValidator
import com.intellij.ml.llm.template.telemetry.*
import com.intellij.ml.llm.template.ui.RefactoringSuggestionsPanel
import com.intellij.ml.llm.template.utils.*
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnstableApiUsage")
abstract class ApplySuggestRefactoringIntention(
    private val efLLMRequestProvider: LLMRequestProvider = GPTExtractFunctionRequestProvider
) : IntentionAction {
    private val logger = Logger.getInstance("#com.intellij.ml.llm")
    val codeTransformer = CodeTransformer()
    val telemetryDataManager = EFTelemetryDataManager()
    var llmResponseTime = 0L
    var functionSrc = ""
    lateinit var functionPsiElement: PsiElement
    var llmResponseCache = mutableMapOf<String, LLMBaseResponse>()
    var apiResponseCache = mutableMapOf<String, MutableMap<String, LLMBaseResponse>>()
    val MAX_REFACTORINGS = 10

    var prompter: MethodPromptBase = SuggestRefactoringPrompt();

    init {
        codeTransformer.addObserver(EFLoggerObserver(logger))
        codeTransformer.addObserver(TelemetryDataObserver())
    }

//    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val selectionModel = editor.selectionModel
        val namedElement =
            PsiUtils.getParentFunctionOrNull(editor, file.language)
                ?: PsiUtils.getParentClassOrNull(editor, file.language)
        if (namedElement != null) {

            telemetryDataManager.newSession()
            val codeSnippet = namedElement.text

            val textRange = namedElement.textRange
            selectionModel.setSelection(textRange.startOffset, textRange.endOffset)
            val startLineNumber = editor.document.getLineNumber(selectionModel.selectionStart) + 1
            val withLineNumbers = addLineNumbersToCodeSnippet(codeSnippet, startLineNumber)
            functionSrc = withLineNumbers
            functionPsiElement = namedElement

            val bodyLineStart = when(namedElement){
                is PsiClass -> PsiUtils.getClassBodyStartLine(namedElement)
                else -> PsiUtils.getFunctionBodyStartLine(namedElement)
            }
            telemetryDataManager.addHostFunctionTelemetryData(
                EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                    codeSnippet = codeSnippet,
                    lineStart = startLineNumber,
                    bodyLineStart = bodyLineStart,
                    language = file.language.id.toLowerCaseAsciiOnly()
                )
            )

            getPromptAndRunBackgroundable(withLineNumbers, project, editor, file)
        }
    }

//    abstract fun invokeLlm(text: String, project: Project, editor: Editor, file: PsiFile)
    private fun getPromptAndRunBackgroundable(text: String, project: Project, editor: Editor, file: PsiFile) {
        logger.info("Invoking LLM with text: $text")
        val messageList = prompter.getPrompt(text)

        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.request.extract.function.background.process.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
                invokeLLM(project, messageList, editor, file)
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    open fun invokeLLM(
        project: Project,
        messageList: MutableList<OpenAiChatMessage>,
        editor: Editor,
        file: PsiFile
    ) {
        val now = System.nanoTime()
        val response = llmResponseCache.get(functionSrc) ?: sendChatRequest(
            project, messageList, efLLMRequestProvider.chatModel, efLLMRequestProvider
        )
        if (response != null) {
            llmResponseCache.get(functionSrc) ?: llmResponseCache.put(functionSrc, response)
            invokeLater {
                llmResponseTime = System.nanoTime() - now
                if (response.getSuggestions().isEmpty()) {
                    showEFNotification(
                        project,
                        LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
                        NotificationType.INFORMATION
                    )
                } else {
                    processLLMResponse(response, project, editor, file)
                }
            }
        }
    }


    abstract fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile)

    override fun startInWriteAction(): Boolean = false


    fun sendTelemetryData() {
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            TelemetryDataObserver().update(EFNotification(efTelemetryData))
        }
    }

    fun buildProcessingTimeTelemetryData(llmResponseTime: Long, pluginProcessingTime: Long) {
        val llmResponseTimeMillis = TimeUnit.NANOSECONDS.toMillis(llmResponseTime)
        val pluginProcessingTimeMillis = TimeUnit.NANOSECONDS.toMillis(pluginProcessingTime)
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            efTelemetryData.processingTime = EFTelemetryDataProcessingTime(

                llmResponseTime = llmResponseTimeMillis,
                pluginProcessingTime = pluginProcessingTimeMillis,
                totalTime = llmResponseTimeMillis + pluginProcessingTimeMillis
            )
        }
    }

    fun buildCandidatesTelemetryData(
        numberOfSuggestions: Int, notificationPayloadList: List<EFCandidateApplicationPayload>
    ): EFCandidatesTelemetryData {
        val candidateTelemetryDataList = EFTelemetryDataUtils.buildCandidateTelemetryData(notificationPayloadList)
        return EFCandidatesTelemetryData(
            numberOfSuggestions = numberOfSuggestions, candidates = candidateTelemetryDataList
        )
    }

    fun buildElapsedTimeTelemetryData(elapsedTimeTelemetryDataObserver: TelemetryElapsedTimeObserver) {
        val elapsedTimeTelemetryData = elapsedTimeTelemetryDataObserver.getTelemetryData()
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            efTelemetryData.elapsedTime = elapsedTimeTelemetryData
        }
    }
}