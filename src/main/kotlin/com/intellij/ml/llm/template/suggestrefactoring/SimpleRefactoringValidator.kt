package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.refactoringobjects.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestion
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestionList
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class SimpleRefactoringValidator(
    private val efLLMRequestProvider: LLMRequestProvider,
    private val project: Project,
    private val functionSrc: String,
    private val functionPsiElement: PsiElement
) : AbstractRefactoringValidator(efLLMRequestProvider, project, functionSrc) {

    override fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("extract")
//                && atomicSuggestion.shortDescription.lowercase().contains("method")
    }

    override fun getExtractMethodSuggestions(llmText: String): EFSuggestionList {;

        val refactoringSuggestion = getRawSuggestions(llmText)

        val efSuggestions = mutableListOf<EFSuggestion>()
        for (suggestion in refactoringSuggestion.improvements) {


            if (isExtractMethod(suggestion)) {
                // TODO: get parameters
                val EFsug = getExtractMethodParameters(suggestion, refactoringSuggestion.finalCode)
                efSuggestions.add(
                        EFsug
                    )
                }
        }

        return EFSuggestionList(efSuggestions)
    }

    override fun isRenameVariable(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("rename")
//                && atomicSuggestion.shortDescription.lowercase().contains("variable")
    }
    override fun getRenamveVariableSuggestions(llmText: String): MutableList<RenameVariable>{
        val refactoringSuggestion = getRawSuggestions(llmText)

        val renameSuggestions = mutableListOf<RenameVariable>()
        for (suggestion in refactoringSuggestion.improvements) {
            if (isRenameVariable(suggestion)) {
                // TODO: get parameters
                val renameVariableSuggestion = getRenameVariableParameters(
                    suggestion,
                    refactoringSuggestion.finalCode,
                    functionPsiElement)
                if (renameVariableSuggestion!=null)
                    renameSuggestions.add(
                        renameVariableSuggestion
                    )
            }
        }

        return renameSuggestions
    }
}