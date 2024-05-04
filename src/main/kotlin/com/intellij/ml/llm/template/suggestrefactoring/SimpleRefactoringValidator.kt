package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.refactoringobjects.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.RenameVariableFactory
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestion
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFSuggestionList
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class SimpleRefactoringValidator(
    private val efLLMRequestProvider: LLMRequestProvider,
    private val project: Project,
    private val functionSrc: String,
    private val functionPsiElement: PsiElement
) : AbstractRefactoringValidator(efLLMRequestProvider, project, functionSrc) {



    override fun getRefactoringSuggestions(llmResponseText: String): List<AbstractRefactoring> {
        val refactoringSuggestion = getRawSuggestions(llmResponseText)

        val refactoringObjects= mutableListOf<AbstractRefactoring>()


        var refFactory: MyRefactoringFactory
        for (suggestion in refactoringSuggestion.improvements) {
            refFactory = if (isExtractMethod(suggestion)) {
                ExtractMethodFactory
            } else if (isRenameVariable(suggestion)){
                RenameVariableFactory
            } else{
                ExtractMethodFactory //default
            }
            getParamsAndCreateObject(suggestion, refactoringSuggestion.finalCode, refFactory)?.let {
                refactoringObjects.add(
                    it
                )
            }
        }

        return refactoringObjects;

    }


    override fun isExtractMethod(atomicSuggestion: AtomicSuggestion): Boolean{
        return atomicSuggestion.shortDescription.lowercase().contains("extract")
//                && atomicSuggestion.shortDescription.lowercase().contains("method")
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