package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.LLMBaseResponse
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.refactoringobjects.conditionals.*
import com.intellij.ml.llm.template.refactoringobjects.looping.EnhancedForFactory
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.looping.For2Stream
import com.intellij.ml.llm.template.refactoringobjects.looping.For2While
import com.intellij.ml.llm.template.refactoringobjects.movemethod.MoveMethodFactory
import com.intellij.ml.llm.template.refactoringobjects.stringbuilder.StringBuilderRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.langchain4j.model.chat.ChatLanguageModel
import kotlinx.coroutines.*

class SimpleRefactoringValidator(
    private val llmChatModel: ChatLanguageModel,
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
    private val functionSrc: String,
    apiResponseCache: MutableMap<String, MutableMap<String, LLMBaseResponse>>
) : AbstractRefactoringValidator(llmChatModel, project, editor, file, functionSrc, apiResponseCache) {


    override suspend fun getRefactoringSuggestions(llmResponseText: String, limit: Int): List<AbstractRefactoring> {
        val refactoringSuggestion = getRawSuggestions(llmResponseText)?:return emptyList()
        val allRefactoringObjects = buildObjectsFromImprovementsList(refactoringSuggestion.improvements)
        return allRefactoringObjects;
    }

    override suspend fun buildObjectsFromImprovementsList(
        improvementsList: List<AtomicSuggestion>,
    ): List<AbstractRefactoring> {
        val allRefactoringObjects= mutableListOf<AbstractRefactoring>()
        coroutineScope {
            improvementsList.map { suggestion ->
                async(Dispatchers.Default) {
                    val refFactory = when {
                        isExtractMethod(suggestion) -> ExtractMethodFactory
                        isRenameVariable(suggestion) -> RenameVariableFactory
                        isEnhacedForRefactoring(suggestion) -> EnhancedForFactory.factory
                        isEnhancedSwitchRefactoring(suggestion) -> EnhancedSwitchFactory.factory
                        isFor2While(suggestion) -> For2While.factory
                        isFor2Streams(suggestion) -> For2Stream.factory
                        isIf2Switch(suggestion) -> If2Switch.factory
                        isSwitch2If(suggestion) -> Switch2IfFactory
                        isIf2Ternary(suggestion) -> If2Ternary.factory
                        isTernary2If(suggestion) -> Ternary2If.factory
                        isStringBuilder(suggestion) -> StringBuilderRefactoringFactory
                        isMoveMethod(suggestion) -> MoveMethodFactory
                        else -> ExtractMethodFactory // default
                    }

                    val createdRefactoringObjects =
                        getParamsAndCreateObject(suggestion, refFactory)

                    if (!createdRefactoringObjects.isNullOrEmpty()) {
                        println("Successfully created ${createdRefactoringObjects.size} refactoring object(s).")
                        createdRefactoringObjects
                    } else {
                        println("No refactoring objects were created.")
                        listOf(
                            UncreatableRefactoring(
                                suggestion.start, suggestion.end, refFactory::class.simpleName.toString()
                            )
                                .also {
                                    it.description = suggestion.shortDescription + "\n" + suggestion.longDescription
                                }
                        )
                    }
                }
            }.awaitAll().flatten().let {
                allRefactoringObjects.addAll(it)
            }
        }
        return allRefactoringObjects
    }

    override fun isEnhacedForRefactoring(atomicSuggestion: AtomicSuggestion): Boolean {
        return atomicSuggestion.shortDescription.lowercase().contains("enhanced")
                && atomicSuggestion.shortDescription.lowercase().contains("for")
    }

    override fun isEnhancedSwitchRefactoring(suggestion: AtomicSuggestion): Boolean {
        return suggestion.shortDescription.lowercase().contains("enhanced")
                && suggestion.shortDescription.lowercase().contains("switch")
    }

    override fun isFor2While(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("for")
                && lowercaseDescription.contains("while")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("for") < lowercaseDescription.indexOf("while")
    }

    override fun isFor2Streams(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("for")
                && lowercaseDescription.contains("stream")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("for") < lowercaseDescription.indexOf("stream")
    }

    override fun isIf2Switch(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("if")
                && lowercaseDescription.contains("switch")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("if") < lowercaseDescription.indexOf("switch")
    }

    override fun isSwitch2If(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("switch")
                && lowercaseDescription.contains("if")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("switch") < lowercaseDescription.indexOf("if")
    }

    override fun isIf2Ternary(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("if")
                && lowercaseDescription.contains("ternary")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("if") < lowercaseDescription.indexOf("ternary")
    }

    override fun isTernary2If(suggestion: AtomicSuggestion): Boolean {
        val lowercaseDescription = suggestion.shortDescription.lowercase()
        return lowercaseDescription.contains("ternary")
                && lowercaseDescription.contains("if")
                && lowercaseDescription.contains("convert")
                && lowercaseDescription.indexOf("ternary") < lowercaseDescription.indexOf("if")
    }

    override fun isStringBuilder(suggestion: AtomicSuggestion): Boolean {
        return suggestion.shortDescription.lowercase().contains("string") &&
                suggestion.shortDescription.lowercase().contains("builder")
    }

    override fun isMoveMethod(suggestion: AtomicSuggestion): Boolean {
        return suggestion.shortDescription.lowercase().contains("move") &&
                suggestion.shortDescription.lowercase().contains("method")
    }


    override fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("extract")
//                && atomicSuggestion.shortDescription.lowercase().contains("method")
    }


    override fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("rename")
//                && atomicSuggestion.shortDescription.lowercase().contains("variable")
    }

}