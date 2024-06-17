package com.intellij.ml.llm.template.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.models.GPTExtractFunctionRequestProvider
import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.models.LLMRequestProvider
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
import com.intellij.ml.llm.template.ui.ExtractFunctionPanel
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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.awt.Point
import java.awt.Rectangle
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


@Suppress("UnstableApiUsage")
abstract class ApplySuggestRefactoringInteractiveIntention(
    private val efLLMRequestProvider: LLMRequestProvider = GPTExtractFunctionRequestProvider
) : IntentionAction {
    private val MAX_ITERS: Int = 5
    private val logger = Logger.getInstance("#com.intellij.ml.llm")
    private val codeTransformer = CodeTransformer()
    private val telemetryDataManager = EFTelemetryDataManager()
    private var llmResponseTime = 0L
    private var functionSrc = ""
    private lateinit var functionPsiElement: PsiElement
    private var llmResponseCache = mutableMapOf<String, LLMBaseResponse>()
    private var apiResponseCache = mutableMapOf<String, MutableMap<String, LLMBaseResponse>>()

    var prompter: MethodPromptBase = ExtractMethodPrompt();

//    private val selectionPritioty = mapOf(
//
//    )

    init {
        codeTransformer.addObserver(EFLoggerObserver(logger))
        codeTransformer.addObserver(TelemetryDataObserver())
    }

    override fun getFamilyName(): String = LLMBundle.message("intentions.apply.transformation.family.name")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return editor != null && file != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val namedElement =
            PsiUtils.getParentFunctionOrNull(editor, file.language)
                ?: PsiUtils.getParentClassOrNull(editor, file.language)
        val selectionModel = editor.selectionModel

        if (namedElement != null) {
            functionPsiElement = namedElement

            telemetryDataManager.newSession()
            val codeSnippet = namedElement.text
            selectionModel.setSelection(namedElement.startOffset, namedElement.endOffset)
            val (startLineNumber, withLineNumbers) = setFunctionSrc(editor)


            val bodyLineStart = when (namedElement) {
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

            invokeLlm(withLineNumbers, project, editor, file)
        }

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

    private fun invokeLlm(text: String, project: Project, editor: Editor, file: PsiFile) {



        logger.debug("Method/Class:\n$functionSrc")

        val task = object : Task.Backgroundable(
            project, LLMBundle.message("intentions.request.extract.function.background.process.title")
        ) {
            override fun run(indicator: ProgressIndicator) {
                    for (iter in 1..MAX_ITERS){
                        val now = System.nanoTime()
                        logger.info(Companion.AGENT_HEADER)
                        logger.info("Asking for refactoring suggestions! ($iter/$MAX_ITERS)")

                        if (iter!=1)
                            setFunctionSrc(editor)

                        val messageList = prompter.getPrompt(functionSrc)

                        val cacheKey = functionSrc + iter
                        val response = llmResponseCache.get(cacheKey) ?: sendChatRequest(
                            project, messageList, efLLMRequestProvider.chatModel, efLLMRequestProvider, temperature = 0.5
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

            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    /*
    this function filters for valid extract methods candidates.
    Removes whole body, one-liners, invalid selections
     */
    private fun filterCandidates(
        candidates: List<EFCandidate>,
        candidatesApplicationTelemetryObserver: EFCandidatesApplicationTelemetryObserver,
        editor: Editor,
        file: PsiFile
    ): List<EFCandidate> {
        val filteredCandidates = candidates.filter {
            isCandidateExtractable(
                it, editor, file, listOf(EFLoggerObserver(logger), candidatesApplicationTelemetryObserver)
            )
        }.sortedByDescending { it.lineEnd - it.lineStart }

        return filteredCandidates
    }

    private suspend fun processLLMResponse(response: LLMBaseResponse, project: Project, editor: Editor, file: PsiFile) {

//        delay(3000)
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

    private fun showRefactoringOptionsPopup(
        project: Project,
        editor: Editor,
        file: PsiFile,
        candidates: List<AbstractRefactoring>,
        codeTransformer: CodeTransformer
    ) {
        val highlighter = AtomicReference(ScopeHighlighter(editor))
        val efPanel = ExtractFunctionPanel(
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
                .setTitle(LLMBundle.message("ef.candidates.popup.title"))
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

    private fun sendTelemetryData() {
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            TelemetryDataObserver().update(EFNotification(efTelemetryData))
        }
    }

    private fun buildProcessingTimeTelemetryData(llmResponseTime: Long, pluginProcessingTime: Long) {
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

    private fun buildCandidatesTelemetryData(
        numberOfSuggestions: Int, notificationPayloadList: List<EFCandidateApplicationPayload>
    ): EFCandidatesTelemetryData {
        val candidateTelemetryDataList = EFTelemetryDataUtils.buildCandidateTelemetryData(notificationPayloadList)
        return EFCandidatesTelemetryData(
            numberOfSuggestions = numberOfSuggestions, candidates = candidateTelemetryDataList
        )
    }

    private fun buildElapsedTimeTelemetryData(elapsedTimeTelemetryDataObserver: TelemetryElapsedTimeObserver) {
        val elapsedTimeTelemetryData = elapsedTimeTelemetryDataObserver.getTelemetryData()
        val efTelemetryData = telemetryDataManager.getData()
        if (efTelemetryData != null) {
            efTelemetryData.elapsedTime = elapsedTimeTelemetryData
        }
    }

    private suspend fun executeRefactorings(
        validRefactoringCandidates: List<AbstractRefactoring>,
        project: Project,
        editor: Editor,
        file: PsiFile
    ) {
        val myHighlighter = AtomicReference(ScopeHighlighter(editor))
        var count = 1
        for (refCandidate in validRefactoringCandidates.sortedBy { it is ExtractMethod }){
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
                delay(3_000)
                invokeLater {
                    refCandidate.performRefactoring(project, editor, file)
                    scopeHighlighter.dropHighlight()
                }
                delay(3_000)
                count+=1

        }
    }

    companion object {
        private const val AGENT_HEADER = "\n\n************************** Refactoring AGENT **************************"
        private const val LLM_HEADER = "\n\n******************************** LLM ********************************"
        fun selectionPriority(
            atomicSuggestion: AtomicSuggestion,
            validator: AbstractRefactoringValidator): Int{
            if (validator.isFor2While(atomicSuggestion) || validator.isEnhacedForRefactoring(atomicSuggestion)){
                return 10;
            }
            if (validator.isExtractMethod(atomicSuggestion))
                return 1;
            return 5;
        }
    }
}