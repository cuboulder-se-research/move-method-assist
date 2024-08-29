package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.MyRefactoringFactory
import com.intellij.ml.llm.template.refactoringobjects.UncreatableRefactoring
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.move.MoveInstanceMembersUtil
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.refactoring.openapi.impl.JavaRefactoringFactoryImpl
import com.intellij.refactoring.suggested.startOffset
import org.jetbrains.kotlin.idea.editor.fixers.endLine
import org.jetbrains.kotlin.idea.editor.fixers.startLine
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class MoveMethodFactory {


    data class MovePivot(val psiClass: PsiClass, val psiElement: PsiElement?)
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
                    PsiUtils.getParentClassOrNull(editor, language = file.language)?:
                    file.getChildOfType<PsiClass>()
                }
            val methodToMove = runReadAction {  PsiUtils.getMethodNameFromClass(outerClass, methodName) } ?: return listOf()
//            val psiTargetVariable =
//                runReadAction { PsiUtils.getVariableFromPsi(methodToMove, targetVariable) }
//                    ?: return runReadAction { tryMoveToClass(methodToMove, targetVariable, project, editor, file)}
            return createMoveMethodRefactorings(project, methodToMove, editor, file)
        }

        private fun createMoveMethodRefactorings(
            project: Project,
            methodToMove: PsiMethod,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring> {

            val targetPivots = getPotentialMovePivots(project, editor, file, methodToMove)
            val targetPivotsSorted = targetPivots
                .filter { methodToMove.containingClass?.qualifiedName!=it.psiClass.qualifiedName }
                .sortedByDescending { PsiUtils.computeCosineSimilarity(methodToMove, it.psiClass)  }

            if (PsiUtils.isMethodStatic(methodToMove)){
                return targetPivotsSorted.map {
                    it.psiClass.qualifiedName?.let { it1 ->
                        MyMoveStaticMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove, it1
                        )
                    }
                }.filterNotNull().subList(0, 3)
            }

            return targetPivotsSorted
                .map {
                    if (it.psiElement!=null) {
                        val processor = runReadAction {
                            MoveInstanceMethodProcessor(
                                project, methodToMove, it.psiElement as PsiVariable, "public",
                                runReadAction {
                                    getParamNamesIfNeeded(
                                        MoveInstanceMembersUtil.getThisClassesToMembers(methodToMove),
                                        it.psiElement as? PsiField
                                    )
                                }
                            )
                        }
                        MyMoveMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove,
                            processor
                        )
                    }else {
                        null
                    }
                }
                .filterNotNull()
                .subList(0, 3) // choose top-3 moves
        }

        private fun getPotentialMovePivots(project: Project, editor: Editor, file: PsiFile, methodToMove: PsiMethod): List<MovePivot> {
            if (methodToMove.containingClass==null) return emptyList()
            if (PsiUtils.isMethodStatic(methodToMove)){
                return (PsiUtils.fetchClassesInPackage(methodToMove.containingClass!!, project) + PsiUtils.fetchImportsInFile(file, project))
                    .map { MovePivot(it, null) }
            }else{
                val movePivots = mutableListOf<MovePivot>()
                for (field in methodToMove.containingClass!!.allFields){
                    val qualifier = field.type.canonicalText
                    if (PsiUtils.isInProject(qualifier, project)){
                        val clazz = PsiUtils.findClassFromQualifier(qualifier, project)
                        if (clazz!=null)
                            movePivots.add(
                                MovePivot(clazz, field)
                            )
                    }
                }

                for (parameter in methodToMove.parameterList.parameters){
                    val qualifier = parameter.type.canonicalText
                    if (PsiUtils.isInProject(qualifier, project)){
                        val clazz = PsiUtils.findClassFromQualifier(qualifier, project)
                        if (clazz!=null)
                            movePivots.add(
                                MovePivot(clazz, parameter)
                            )
                    }
                }

                return movePivots
            }
        }

        fun tryMoveToClass(
            methodToMove: PsiMethod,
            targetClassName: String,
            project: Project,
            editor: Editor,
            file: PsiFile
        ): List<AbstractRefactoring>{

            if (PsiUtils.isMethodStatic(methodToMove)) {
                val qualifiedClassName = PsiUtils.getQualifiedTypeInFile(
                    methodToMove.containingFile, targetClassName
                )
                if (qualifiedClassName!=null){
                    return listOf(
                        MyMoveStaticMethodRefactoring(
                            methodToMove.startLine(editor.document),
                            methodToMove.endLine(editor.document),
                            methodToMove, qualifiedClassName)
                    )
                }
            }else{
                val variableOfType = PsiUtils.getVariableOfType(methodToMove, targetClassName)
                if (variableOfType!=null){
//                    return createMoveMethodRefactorings(variableOfType, project, methodToMove, editor)
                }
            }

            return listOf()
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
                    "    to a target object identified by `target_variable_name`. It assumes that the necessary updates to the\n" +
                    "    source code are handled externally.\n" +
                    "\n" +
                    "    Parameters:\n" +
                    "    - method_name (str): The name of the method to be moved.\n" +
                    "    - target_variable_name (str): The name of the target object to which the method should be moved.\n" +
                    "    \"\"\""



        class MyMoveMethodRefactoring(
            override val startLoc: Int,
            override val endLoc: Int,
            val methodToMove: PsiMethod,
            val processor: MoveInstanceMethodProcessor
        ) : AbstractRefactoring(){
            override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
                super.performRefactoring(project, editor, file)
                processor.run()
                reverseRefactoring = getReverseRefactoringObject(project, editor, file)
            }

            override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
                isValid = methodToMove.isPhysical
                return isValid!!
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

            override fun getReverseRefactoringObject(
                project: Project,
                editor: Editor,
                file: PsiFile
            ): AbstractRefactoring? {
                TODO("Not yet implemented")
            }

            override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
                if (isValid==true)
                    return this
                return null // TODO: implement search
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

    class MyMoveStaticMethodRefactoring(
        override val startLoc: Int,
        override val endLoc: Int,
        val methodToMove: PsiMethod,
        val classToMoveTo: String
    ) : AbstractRefactoring(){
        val sourceClass: PsiClass = methodToMove.containingClass!!
        val methodName = methodToMove.name

        override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
            val refFactory = JavaRefactoringFactoryImpl(project)
            val moveRefactoring =
                refFactory.createMoveMembers(
                    arrayOf(methodToMove),
                    classToMoveTo,
                    "public")
            moveRefactoring.run()
            reverseRefactoring = getReverseRefactoringObject(project, editor, file)
        }

        override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
            isValid = methodToMove.isPhysical
            return methodToMove.isPhysical
        }

        override fun getRefactoringPreview(): String {
            return "Move Static method ${methodToMove.name} to class ${classToMoveTo}"
        }

        override fun getStartOffset(): Int {
            return methodToMove.startOffset
        }

        override fun getEndOffset(): Int {
            return methodToMove.endOffset
        }

        override fun getReverseRefactoringObject(
            project: Project,
            editor: Editor,
            file: PsiFile
        ): AbstractRefactoring? {

            val destClass = JavaPsiFacade.getInstance(project).findClass(
                classToMoveTo,
                GlobalSearchScope.projectScope(project))
            if (destClass!=null){
                val foundMethod = PsiUtils.getMethodNameFromClass(destClass, methodName)
                if (foundMethod!=null)
                    return MyMoveStaticMethodRefactoring(
                        foundMethod.startLine(editor.document),
                        foundMethod.endLine(editor.document),
                        foundMethod, sourceClass.qualifiedName!!)
            }
            return null
        }

        override fun recalibrateRefactoring(project: Project, editor: Editor, file: PsiFile): AbstractRefactoring? {
            if (isValid==true)
                return this
            return null // TODO: Impl search
        }

    }

}