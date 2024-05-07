package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

interface MyRefactoringFactory {
//    fun getParamsAndCreate(rawSuggestion: AtomicSuggestion, efLLMRequestProvider: LLMRequestProvider): AbstractRefactoring

    // Return refactoring objects from a function call.
    // May return more than 1 object after enhancing data ensure a well-formed object
    // Ex: extract method enhancements to ensure line numbers are a valid block.
    // extract_method(10, 20, "newMethodName")
    // rename_variable("x", "count")
    fun createObjectsFromFuncCall(
        funcCall: String,
        project: Project,
        editor: Editor,
        file: PsiFile): List<AbstractRefactoring>


    // Logical name for the refactoring. Different from apiFunctionName
    // because this variable can contain spaces
    val logicalName: String

    /*
    Function name passed to the LLM, while asking it to create params.
    Different from logicalName because it must be a valid identifier.
     */
    val apiFunctionName: String

    // API Documentation for passed to the LLM, while asking it to create params.
    val APIDocumentation: String


}