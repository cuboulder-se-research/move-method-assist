package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ExtractMethodFactory {
    companion object: MyRefactoringFactory{



        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {
            val params = getParamsFromFuncCall(funcCall)
            val newName = getStringFromParam(params[2])
            val lineStart = params[0].toInt()
            val lineEnd = params[1].toInt()
            val suggestion = EFSuggestion(newName, lineStart, lineEnd)

            val candidates = runReadAction {
                EFCandidateFactory().buildCandidates(
                    suggestion, editor, file
                )
            }

            return candidates.toList()
                .map { ExtractMethod.fromEFCandidate(it) }
                .toList()
        }


        override val logicalName: String
            get() = "Extract Method"

        override val apiFunctionName: String = "extract_method"

        override val APIDocumentation: String
            get() = """def extract_method(line_start, line_end, new_function_name):
    ""${'"'}
    Extracts a method from the specified range of lines in a source code file and creates a new function with the given name.

    This function is intended to refactor a block of code within a file, taking the lines from `line_start` to `line_end`, 
    inclusive, and moving them into a new function named `new_function_name`. The original block of code is replaced with a 
    call to the newly created function. 

    Parameters:
    - line_start (int): The starting line number from which the block of code will be extracted. Must be a positive integer.
    - line_end (int): The ending line number to which the block of code will be extracted. Must be a positive integer greater than or equal to `line_start`.
    - new_function_name (str): The name of the new function that will contain the extracted block of code. Must be a valid Python function name.
 
                    ""${'"'}
                    """.trimIndent()


    }


}