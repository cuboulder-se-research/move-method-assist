package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.refactoring.move.MoveInstanceMembersUtil
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.idea.editor.fixers.endLine
import org.jetbrains.kotlin.idea.editor.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class MoveMethodFactory {
    companion object: MyRefactoringFactory{
        override fun createObjectsFromFuncCall(
            funcCall: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {

            val params = getParamsFromFuncCall(funcCall)
            val methodName = getStringFromParam(params[0])
            val targetVariable = getStringFromParam(params[1])

            val outerClass: PsiElement? =
                runReadAction {
                    PsiUtils.getParentClassOrNull(editor, language = file.language)
                }
            val methodToMove = runReadAction {  PsiUtils.getMethodNameFromClass(outerClass, methodName) } ?: return listOf()
            val psiTargetVariable = runReadAction { PsiUtils.getVariableFromPsi(methodToMove, targetVariable) } ?: return listOf()
            var targetAsField: PsiField? = null
            if (psiTargetVariable is PsiField){
                targetAsField = psiTargetVariable as PsiField
            }

            val processor = runReadAction {  MoveInstanceMethodProcessor(
                project, methodToMove, psiTargetVariable as PsiVariable, "public",
                runReadAction{
                    getParamNamesIfNeeded(MoveInstanceMembersUtil.getThisClassesToMembers(methodToMove), targetAsField)
                }
            ) }
            return listOf(
                MyMoveMethodRefactoring(
                    methodToMove.startLine(editor.document),
                    methodToMove.endLine(editor.document),
                    methodToMove,
                    processor)
            )
        }

        override val logicalName: String
            get() = "Move Method"
        override val apiFunctionName: String
            get() = "move_method"
        override val APIDocumentation: String
            get() = "def move_method(method_name, target_variable_name):\n" +
                    "    \"\"\"\n" +
                    "    Moves a method from its current class or context to a target class or object.\n" +
                    "\n" +
                    "    This function refactors code by moving a method identified by `method_name` from its original class or context\n" +
                    "    to a target class or object identified by `target_variable_name`. It assumes that the necessary updates to the\n" +
                    "    source code are handled externally.\n" +
                    "\n" +
                    "    Parameters:\n" +
                    "    - method_name (str): The name of the method to be moved.\n" +
                    "    - target_variable_name (str): The name of the target class or object to which the method should be moved.\n" +
                    "    \"\"\""



        class MyMoveMethodRefactoring(
            override val startLoc: Int,
            override val endLoc: Int,
            val methodToMove: PsiMethod,
            val processor: MoveInstanceMethodProcessor
        ) : AbstractRefactoring(){
            override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
                processor.run()
            }

            override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
                return true
            }

            override fun getRefactoringPreview(): String {
                return "Move method ${methodToMove.name}"
            }

            override fun getStartOffset(): Int {
                return methodToMove.startOffset
            }

            override fun getEndOffset(): Int {
                return methodToMove.endOffset
            }

        }


        private fun getParamNamesIfNeeded(
            myThisClassesMap: Map<PsiClass, Set<PsiMember>>,
            targetVariable: PsiField?
        ): MutableMap<PsiClass, String> {
            var parameterNames: MutableMap<PsiClass, String> = mutableMapOf()
            for (aClass in myThisClassesMap.keys) {

                val members = myThisClassesMap[aClass]
                if (targetVariable==null || members == null || members.size != 1 || !members.contains(targetVariable)) {
                    parameterNames.put(aClass, aClass.name!!.lowercase())
                }
                //Just the field is referenced
                // Skip adding parameter
            }
            return parameterNames
        }

    }
}