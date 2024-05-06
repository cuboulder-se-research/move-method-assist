package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.LLMRequestProvider
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.refactoringobjects.RenameVariableFactory
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.ExtractMethodFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class SimpleRefactoringValidator(
    private val efLLMRequestProvider: LLMRequestProvider,
    private val project: Project,
    private val editor: Editor,
    private val file: PsiFile,
    private val functionSrc: String
) : AbstractRefactoringValidator(efLLMRequestProvider, project, editor, file, functionSrc) {



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
                refactoringObjects.addAll(
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

}