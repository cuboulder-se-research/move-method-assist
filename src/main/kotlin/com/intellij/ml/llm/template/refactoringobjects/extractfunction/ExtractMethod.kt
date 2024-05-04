package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring

class ExtractMethod(
    override val startLoc: Int,
    override val endLoc: Int,
    val newFuncName: String
) : AbstractRefactoring {


    companion object{
        const val REFACTORING_NAME = "Extract Method"

    }

    override fun performRefactoring() {
        TODO("Not yet implemented")
    }

    override fun isValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRefactoringName(): String {
        return REFACTORING_NAME
    }


}