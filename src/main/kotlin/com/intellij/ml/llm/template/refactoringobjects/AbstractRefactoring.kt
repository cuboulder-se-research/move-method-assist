package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

//abstract class AbstractRefactoring {
//    companion object{
//        fun something(){}
//    }
//
//    abstract fun performRefactoring()
//
//}


abstract class AbstractRefactoring{


//    companion object{
//        internal fun f(): Int{
//            return 1
//        }
//    }
    abstract fun performRefactoring(project: Project, editor: Editor, file: PsiFile)

    /*
    Return true if the refactoring object can be applied to the code.
     */
    abstract fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean

    /*
    Return the total lines of code covered by the refactoring object
     */
    fun sizeLoc(): Int{
        return endLoc-startLoc+1
    }

    /*
    Line numbers where refactoring is to be applied
     */
    abstract val startLoc: Int
    abstract val endLoc: Int

    /*
    Offsets where refactoring is to be applied(based on character index)
     */
//    val startOffset: Int
//    val endOffset: Int

    /*
    A logical name for the refactoring
     */
    abstract fun getRefactoringName(): String


    open fun getStartOffset(): Int {
        TODO("Implement logic.")
        return 1
    }

    open fun getEndOffset(): Int {
        TODO("Implement logic.")
        return 1
    }
}