package com.intellij.ml.llm.template.refactoringobjects

interface MyRefactoringFactory {
//    fun getParamsAndCreate(rawSuggestion: AtomicSuggestion, efLLMRequestProvider: LLMRequestProvider): AbstractRefactoring

    fun createObjectFromFuncCall(funcCall: String): AbstractRefactoring


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