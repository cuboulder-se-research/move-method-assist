package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.models.grazie.GrazieGPT4RequestProvider
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.prompts.ExtractMethodPrompt
import com.intellij.ml.llm.template.prompts.MethodPromptBase
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethod
import com.intellij.ml.llm.template.suggestrefactoring.AbstractRefactoringValidator
import com.intellij.ml.llm.template.suggestrefactoring.AtomicSuggestion
import com.intellij.ml.llm.template.suggestrefactoring.SimpleRefactoringValidator
import com.intellij.ml.llm.template.telemetry.*
import com.intellij.ml.llm.template.ui.CompletedRefactoringsPanel
import com.intellij.ml.llm.template.utils.*
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent

import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnstableApiUsage")
class ApplySuggestRefactoringInteractiveIntention(
    private val efLLMRequestProvider: LLMRequestProvider = GrazieGPT4RequestProvider
) : ApplySuggestRefactoringIntention(efLLMRequestProvider) {
    val logger = Logger.getInstance(ApplySuggestRefactoringInteractiveIntention::class.java)

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.transformation.family.name")

    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {
        val now = System.nanoTime()

        val llmResponse = response.getSuggestions()[0]
        val validator = SimpleRefactoringValidator(efLLMRequestProvider,
            project,
            editor,
            file,
            functionSrc,
            apiResponseCache
        )

        delay(2000)
        val limit = 3
        logLLMResponse(response, limit, validator)
//        val efSuggestionList = validator.getExtractMethodSuggestions(llmResponse.text)
//        val renameSuggestions = validator.getRenamveVariableSuggestions(llmResponse.text)
        logger.info(AGENT_HEADER)
        logger.info("Processing LLM Recommendations...")
        logger.info("\n")
        val refactoringCandidates: List<AbstractRefactoring> =
            runBlocking {
                validator.getRefactoringSuggestions(llmResponse.text, limit)
            }

//        val refactoringCandidates: List<AbstractRefactoring> = runBlocking {
//                validator.getRefactoringSuggestions(llmResponse.text)
//        }

//        val candidates = EFCandidateFactory().buildCandidates(efSuggestionList.suggestionList, editor, file).toList()
        if (refactoringCandidates.isEmpty()) {
//            showEFNotification(
//                project,
//                LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
//                NotificationType.INFORMATION
//            )
            telemetryDataManager.addCandidatesTelemetryData(buildCandidatesTelemetryData(0, emptyList()))
            buildProcessingTimeTelemetryData(llmResponseTime, System.nanoTime() - now)
            sendTelemetryData()
        } else {
            telemetryDataManager.setRefactoringObjects(refactoringCandidates)
            val candidatesApplicationTelemetryObserver = EFCandidatesApplicationTelemetryObserver()
//            val filteredCandidates = filterCandidates(candidates, candidatesApplicationTelemetryObserver, editor, file)
            val validRefactoringCandidates = refactoringCandidates.filter {
                runReadAction{ it.isValid(project, editor, file) }
            }

            telemetryDataManager.addCandidatesTelemetryData(
                buildCandidatesTelemetryData(
                    refactoringCandidates.size,
                    candidatesApplicationTelemetryObserver.getData()
                )
            )
            buildProcessingTimeTelemetryData(llmResponseTime, System.nanoTime() - now)

            logger.info("\n")
            if (validRefactoringCandidates.isEmpty()) {
//                showEFNotification(
//                    project,
//                    LLMBundle.message("notification.extract.function.with.llm.no.extractable.candidates.message"),
//                    NotificationType.INFORMATION
//                )
                logger.info("No valid refactoring objects created.")
                sendTelemetryData()
            } else {
//                refactoringObjectsCache.get(functionSrc)?:refactoringObjectsCache.put(functionSrc, validRefactoringCandidates)
//                showRefactoringOptionsPopup(
//                    project, editor, file, validRefactoringCandidates, codeTransformer,
//                )
                logger.info("Performing Refactoring Actions:")
                runBlocking { executeRefactorings(validRefactoringCandidates, project, editor, file) }

            }
        }
    }


    private fun logLLMResponse(response: LLMBaseResponse,
                               limit: Int,
                               validator: AbstractRefactoringValidator) {
        logger.info(LLM_HEADER)
        for (suggestion in response.getSuggestions()){
//            logger.info(suggestion.)
            val improvements = AbstractRefactoringValidator.getRawSuggestions(suggestion.text).improvements
            val realLimit = if(improvements.size > limit){
                limit
            } else{
                improvements.size
            }
            val improvementsSorted =
                improvements
                    .sortedBy { selectionPriority(it, validator) }
                    .subList(0, realLimit)
                    .sortedBy { validator.isExtractMethod(it) }
                    .withIndex()
            for (atomicSuggestion in improvementsSorted){
                logger.info("${atomicSuggestion.index+1}: ${atomicSuggestion.value.shortDescription}")
                logger.info("Suggestion: ${atomicSuggestion.value.longDescription}".prependIndent("    "))
                logger.info("\n")
                Thread.sleep(3000)
            }
        }
    }

    private fun showCompletedRefactoringOptionsPopup(
        project: Project,
        editor: Editor,
        file: PsiFile,
        candidates: List<AbstractRefactoring>,
        codeTransformer: CodeTransformer
    ) {
        val highlighter = AtomicReference(ScopeHighlighter(editor))
        val efPanel = CompletedRefactoringsPanel(
            project = project,
            editor = editor,
            file = file,
            candidates = candidates,
            codeTransformer = codeTransformer,
            highlighter = highlighter,
            efTelemetryDataManager = telemetryDataManager
        )
        val elapsedTimeTelemetryDataObserver = TelemetryElapsedTimeObserver()
        efPanel.addObserver(elapsedTimeTelemetryDataObserver)
        val panel = efPanel.createPanel()

        // Create the popup
        val efPopup =
            JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, efPanel.myExtractFunctionsCandidateTable)
                .setRequestFocus(true)
                .setTitle(LLMBundle.message("ef.candidates.completed.popup.title"))
                .setResizable(true)
                .setMovable(true).createPopup()

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

    abstract fun getInstruction(project: Project, editor: Editor): String?

    override fun startInWriteAction(): Boolean = false

}