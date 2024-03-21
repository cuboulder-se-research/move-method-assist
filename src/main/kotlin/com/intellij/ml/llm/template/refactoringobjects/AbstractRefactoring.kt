package com.intellij.ml.llm.template.refactoringobjects

//abstract class AbstractRefactoring {
//    companion object{
//        fun something(){}
//    }
//
//    abstract fun performRefactoring()
//
//}
interface AbstractRefactoring{
    companion object {
        fun something(){}
    }

    fun performRefactoring()
}