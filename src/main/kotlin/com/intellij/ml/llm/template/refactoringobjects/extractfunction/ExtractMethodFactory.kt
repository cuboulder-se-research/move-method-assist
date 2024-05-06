package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ExtractMethodFactory {
    companion object: MyRefactoringFactory{

        fun get_param_value_from_string(param: String): String{
            var value = param.removeSuffix(")")
                .replace("\"", "").replace(" ", "")
            if (value.contains("(")) {
                value = value.split("(")[1].replace(" ", "")
            }

            if (value.contains("=")) {
                value = value.split("=")[1].replace(" ", "")
            }

            return value
        }

        override fun createObjectFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): AbstractRefactoring {
            val func_parts = funcCall.split(',')
            var new_name = func_parts[2].removeSuffix(")")
                .replace("\"", "").replace(" ", "")
            if (new_name.contains("=")) {
                new_name = new_name.split("=")[1].replace(" ", "")
            }

            val lineStart = get_param_value_from_string(func_parts[0]).toInt()
            val lineEnd = get_param_value_from_string(func_parts[1]).toInt()
            val suggestion = EFSuggestion(new_name, lineStart, lineEnd)

            val candidate = EFCandidateFactory().buildCandidates(
                    suggestion, editor, file
                )

            return ExtractMethod.fromEFCandidate(
                candidate.toList()[0] // TODO: Change to include all elements.
            )
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