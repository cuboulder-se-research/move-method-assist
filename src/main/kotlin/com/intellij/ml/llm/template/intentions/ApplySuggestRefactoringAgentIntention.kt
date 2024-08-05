package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.ollama.localOllamaMistral
import com.intellij.ml.llm.template.models.sendChatRequest
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.settings.RefAgentSettingsManager
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
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.idea.editor.fixers.endLine
import org.jetbrains.kotlin.idea.editor.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnstableApiUsage")
open class ApplySuggestRefactoringAgentIntention(
    private var llmChatModel: ChatLanguageModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!,
    private val useDelays: Boolean = true
) : ApplySuggestRefactoringIntention(llmChatModel) {
    val refactoringLimit: Int = 10
    private var MAX_ITERS: Int = RefAgentSettingsManager.getInstance().getNumberOfIterations()
    private val performedRefactorings = mutableListOf<AbstractRefactoring>()
    private val refactoringsPerIteration = mutableMapOf<Int, List<AbstractRefactoring>>()
    private val logger = Logger.getInstance(ApplySuggestRefactoringAgentIntention::class.java)
    private val telemetryIds = mutableListOf<String>()

    override fun getText(): String {
        return LLMBundle.message("intentions.apply.suggest.refactoring.agent.family.name")
    }

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.suggest.refactoring.agent.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    private fun setFunctionSrc(
        editor: Editor,
    ): Pair<Int, String> {
        return runReadAction {
            val startLineNumber = editor.document.getLineNumber(functionPsiElement.startOffset) + 1
            val withLineNumbers = addLineNumbersToCodeSnippet(functionPsiElement.text, startLineNumber)
            functionSrc = withLineNumbers
            Pair(startLineNumber, withLineNumbers)
        }
    }

    override fun invokeLLM(
        project: Project,
        messageList: MutableList<ChatMessage>,
        editor: Editor,
        file: PsiFile
    ) {
        llmChatModel = RefAgentSettingsManager.getInstance().createAndGetAiModel()!!
        MAX_ITERS = RefAgentSettingsManager.getInstance().getNumberOfIterations()
        performedRefactorings.removeAll({it->true})
        for (iter in 1..MAX_ITERS) {
            setTelemetryData(editor, file)
            val now = System.nanoTime()
            logger.info(AGENT_HEADER)
            logger.info("Asking for refactoring suggestions! ($iter/$MAX_ITERS)")

            if (iter != 1)
                setFunctionSrc(editor)

            val newMessageList = prompter.getPrompt(functionSrc)

            val cacheKey = functionSrc + iter
            val response = llmResponseCache.get(cacheKey) ?: sendChatRequest(
                project, newMessageList, llmChatModel
            )
            if (response != null) {
                llmResponseCache.get(cacheKey) ?: llmResponseCache.put(cacheKey, response)
                llmResponseTime = System.nanoTime() - now
                if (response.getSuggestions().isEmpty()) {
    //                                showEFNotification(
    //                                    project,
    //                                    LLMBundle.message("notification.extract.function.with.llm.no.suggestions.message"),
    //                                    NotificationType.INFORMATION
    //                                )
                } else {
                    runBlocking { processLLMResponse(response, project, editor, file) }
                }
            }
        }

        showExecutedRefactorings(project, editor, file)
    }

    private fun setTelemetryData(editor: Editor, file: PsiFile) {
        telemetryDataManager.newSession()
        telemetryDataManager.addHostFunctionTelemetryData(
            EFTelemetryDataUtils.buildHostFunctionTelemetryData(
                codeSnippet = functionSrc,
                lineStart = functionPsiElement.startLine(editor.document),
                bodyLineStart = functionPsiElement.endLine(editor.document),
                language = file.language.id.toLowerCaseAsciiOnly()
            )
        )

        telemetryIds.add(telemetryDataManager.currentSession())

    }

    private fun showExecutedRefactorings(project: Project, editor: Editor, file: PsiFile) {
        invokeLater {
            showCompletedRefactoringOptionsPopup(
                project, editor, file, performedRefactorings, codeTransformer,
            )
        }
    }


    override fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {

//        delay(3000)
        val now = System.nanoTime()

        val llmResponse = response.getSuggestions()[0]
        val validatorModel = if(RefAgentSettingsManager.getInstance().getUseLocalLLM()) localOllamaMistral else llmChatModel
        val validator = SimpleRefactoringValidator(validatorModel,
            project,
            editor,
            file,
            functionSrc,
            apiResponseCache
        )
        if (useDelays)
            Thread.sleep(2000)
        val filteredSuggestions = filterSuggestions(response, refactoringLimit, validator) ?: return
        //        val efSuggestionList = validator.getExtractMethodSuggestions(llmResponse.text)
//        val renameSuggestions = validator.getRenamveVariableSuggestions(llmResponse.text)
        logger.info(AGENT_HEADER)
        logger.info("Processing LLM Recommendations...")
        logger.info("\n")
        val refactoringCandidates: List<AbstractRefactoring> =
            runBlocking {
                validator.buildObjectsFromImprovementsList(filteredSuggestions)
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
            telemetryDataManager.setRefactoringObjects(emptyList())
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


    private fun filterSuggestions(response: LLMBaseResponse,
                                  limit: Int,
                                  validator: AbstractRefactoringValidator): List<AtomicSuggestion>? {
        logger.info(LLM_HEADER)
        val suggestions = response.getSuggestions()


        if (suggestions.isNotEmpty()){
            val suggestion = suggestions[0]
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

            logLLMResponse(improvementsSorted, useDelays)
            return improvementsSorted
        }
        return null
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
        efPanel.initTable()
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


    override fun startInWriteAction(): Boolean = false


    private suspend fun executeRefactorings(
        validRefactoringCandidates: List<AbstractRefactoring>,
        project: Project,
        editor: Editor,
        file: PsiFile
    ) {
        val myHighlighter = AtomicReference(ScopeHighlighter(editor))
        var count = 1
        for (refCandidate in getExecutionOrder(validRefactoringCandidates)){
                val scopeHighlighter: ScopeHighlighter = myHighlighter.get()
                invokeLater {
                    editor.scrollingModel.scrollTo(
                        LogicalPosition(editor.document.getLineNumber(refCandidate.getStartOffset()), 0),
                        ScrollType.CENTER
                    )

                    scopeHighlighter.dropHighlight()
                    val range = TextRange(refCandidate.getStartOffset(), refCandidate.getEndOffset())
                    scopeHighlighter.highlight(com.intellij.openapi.util.Pair(range, listOf(range)))


                    editor.selectionModel.setSelection(refCandidate.getStartOffset(), refCandidate.getStartOffset())
                }
                logger.info("$count. Executing refactoring: ${refCandidate.getRefactoringPreview()}".prependIndent("     "))
                if (useDelays)
                    delay(3_000)
                invokeLater {
                    refCandidate.performRefactoring(project, editor, file)
                    scopeHighlighter.dropHighlight()
                }
                performedRefactorings.add(refCandidate)
                if (useDelays)
                    delay(3_000)
                count+=1

        }
    }




    override fun sendTelemetryData() {
        val agenticTelemetry = AgenticTelemetry.createFromSessionIds(
            telemetryIds, telemetryDataManager
        )
        TelemetryDataObserver().update(EFNotification(agenticTelemetry))
    }

    companion object {
        private const val AGENT_HEADER = "\n\n************************** Refactoring AGENT **************************"
        private const val LLM_HEADER = "\n\n******************************** LLM ********************************"
    }
}

fun selectionPriority(
    atomicSuggestion: AtomicSuggestion,
    validator: AbstractRefactoringValidator
): Int{
    if (validator.isFor2While(atomicSuggestion) || validator.isEnhacedForRefactoring(atomicSuggestion)){
        return 10;
    }
    if (validator.isExtractMethod(atomicSuggestion))
        return 1;
    return 5;
}