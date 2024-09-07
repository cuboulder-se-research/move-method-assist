package com.intellij.ml.llm.template.utils

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ModalTaskOwner.project
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.patterns.PsiJavaPatterns.psiClass
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.j2k.accessModifier
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.math.sqrt

class PsiUtils {
    companion object {
        fun getParentClassOrNull(editor: Editor, language: Language?): PsiElement? {
            val psiElement = PsiUtilBase.getElementAtCaret(editor)
            when (language) {
                JavaLanguage.INSTANCE -> return PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
            }
            return null
        }

        fun getClassBodyStartLine(psiClass: PsiClass): Int{
            return psiClass.getLineNumber(true)+1
        }

        fun getParentFunctionOrNull(psiElement: PsiElement?, language: Language?): PsiElement? {
            when (language) {
                JavaLanguage.INSTANCE -> return PsiTreeUtil.getParentOfType(psiElement, PsiMethod::class.java)
                KotlinLanguage.INSTANCE -> return PsiTreeUtil.getParentOfType(psiElement, KtNamedFunction::class.java)
            }
            return null
        }

        fun getParentFunctionOrNull(editor: Editor, language: Language?): PsiElement? {
            return getParentFunctionOrNull(PsiUtilBase.getElementAtCaret(editor), language)
        }

        fun getParentFunctionBlockOrNull(psiElement: PsiElement?, language: Language?): PsiElement? {
            if (psiElement == null) return null
            return getFunctionBlockOrNull(getParentFunctionOrNull(psiElement, language))
        }

        fun getFunctionBlockOrNull(psiElement: PsiElement?): PsiElement? {
            when (psiElement) {
                is PsiMethod -> return PsiTreeUtil.getChildOfType(psiElement, PsiCodeBlock::class.java)
                is KtNamedFunction -> return PsiTreeUtil.getChildOfType(psiElement, KtBlockExpression::class.java)
            }
            return null
        }

        fun getFunctionBodyStartLine(psiElement: PsiElement?): Int {
            val block = getFunctionBlockOrNull(psiElement)
            var startLine = -1
//            when(psiElement){
//                is PsiClass -> 1
//            }
            if (block != null) {
                startLine = block.firstChild.getLineNumber(false) + 1
            }
            return startLine
        }

        fun getParentFunctionCallOrNull(psiElement: PsiElement, language: Language): PsiElement? {
            when (language) {
                KotlinLanguage.INSTANCE ->
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, KtDotQualifiedExpression::class.java)
                        ?: PsiTreeUtil.getTopmostParentOfType(psiElement, KtCallExpression::class.java)
            }
            return null
        }

        fun elementsBelongToSameFunction(start: PsiElement, end: PsiElement, language: Language): Boolean {
            val parentFunctionStart = getParentFunctionOrNull(start, language) ?: return false
            val parentFunctionEnd = getParentFunctionOrNull(end, language) ?: return false
            return parentFunctionStart == parentFunctionEnd
        }

        fun isElementArgumentOrArgumentList(psiElement: PsiElement, language: Language): Boolean {
            when (language) {
                KotlinLanguage.INSTANCE -> {
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, KtValueArgumentList::class.java) != null
                }

                JavaLanguage.INSTANCE -> {
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, PsiExpressionList::class.java) != null
                }
            }
            return false
        }

        fun isElementParameterOrParameterList(psiElement: PsiElement?, language: Language): Boolean {
            when (language) {
                KotlinLanguage.INSTANCE -> {
                    val x = PsiTreeUtil.getTopmostParentOfType(psiElement, KtParameterList::class.java)
                    return x != null
                }

                JavaLanguage.INSTANCE -> {
                    return PsiTreeUtil.getTopmostParentOfType(psiElement, PsiParameterList::class.java) != null
                }
            }
            return false
        }

        fun getVariableFromPsi(psiElement: PsiElement?, variableName: String): PsiElement?{

            var foundVariable: PsiElement? = null
            class VariableFinder: JavaRecursiveElementVisitor() {

                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.name == variableName)
                        foundVariable = variable
                }

                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.referenceName==variableName)
                        foundVariable = expression.reference?.resolve()
                }
//                override fun visitIdentifier(identifier: PsiIdentifier) {
//                    super.visitIdentifier(identifier)
//                    if (identifier.text == variableName)
//                        foundVariable = identifier
//                }

            }
            if (psiElement != null) {
                psiElement.accept(VariableFinder())
            }
            if (foundVariable!=null && psiElement!=null) {
                return foundVariable
            }
            return null
        }

        fun getVariableAndReferencesFromPsi(psiElement: PsiElement?, variableName: String): List<PsiElement>{

            var matches: MutableList<PsiElement> = mutableListOf()
            class VariableFinder: JavaRecursiveElementVisitor() {

                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.name == variableName)
                        matches.add(variable)
                }

                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.referenceName==variableName)
                        matches.add(expression)
                }

            }
            if (psiElement != null) {
                psiElement.accept(VariableFinder())
            }
            return matches
        }

        fun getLeftmostPsiElement(lineNumber: Int, editor: Editor, file: PsiFile): PsiElement? {
            // get the PsiElement on the given lineNumber
            var psiElement: PsiElement = file.findElementAt(editor.document.getLineStartOffset(lineNumber)) ?: return null

            // if there are multiple sibling PsiElements on the same line, look for the first one
            while (psiElement.getLineNumber(false) == psiElement.prevSibling?.getLineNumber(false)) {
                psiElement = psiElement.prevSibling
            }

            // if we are still on a PsiWhiteSpace, then go right
            while (psiElement.getLineNumber(false) == psiElement.nextSibling?.getLineNumber(false) && psiElement is PsiWhiteSpace) {
                psiElement = psiElement.nextSibling
            }

            // if there are multiple parent PsiElements on the same line, look for the top one
            val psiElementLineNumber = psiElement.getLineNumber(false)
            while (true) {
                if (psiElement.parent == null) break
                if (psiElement.parent is PsiCodeBlock || psiElement.parent is KtBlockExpression) break
                if (psiElementLineNumber != psiElement.parent.getLineNumber(false)) break
                psiElement = psiElement.parent
            }

            // move to next non-white space sibling
            while (psiElement is PsiWhiteSpace) {
                psiElement = psiElement.nextSibling
            }

            return psiElement
        }

        fun <T: PsiElement> getElementsOfTypeOnLine(file: PsiFile, editor: Editor, lineNumber: Int, clazz: Class<out T>): List<T> {
            val elementAtStartLine =
                PsiUtilBase.getElementAtOffset(
                    file, editor.document.getLineStartOffset(lineNumber - 1)
                )

            val startOffset = editor.document.getLineStartOffset(lineNumber - 1)
            val endOffset = editor.document.getLineStartOffset(lineNumber)
            val foundElements = PsiTreeUtil
                .findChildrenOfType(elementAtStartLine.parent, clazz)
                .filter { it.startOffset in (startOffset..endOffset) }
            return foundElements

        }

        fun getMethodNameFromClass(outerClass: PsiElement?, methodName: String): PsiMethod? {
            var match: PsiMethod? = null
            class MethodFinder: JavaRecursiveElementVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (method.name == methodName)
                        match = method
                }

            }
            if (outerClass != null) {
                outerClass.accept(MethodFinder())
            }
            return match
        }

        fun getQualifiedTypeInFile(psiFile: PsiFile, typeName: String): String?{
            var match: String? = null
            class TypeFinder: JavaRecursiveElementVisitor() {
                override fun visitTypeElement(type: PsiTypeElement) {
                    super.visitTypeElement(type)
                    if (type.text == typeName
                        && (type.type as PsiClassReferenceType).resolve()!=null)
                        match = (type.type as PsiClassReferenceType).resolve()?.qualifiedName
                }
                override fun visitImportStatement(statement: PsiImportStatement) {
                    super.visitImportStatement(statement)

                    val childrenOfTypeReference = statement.childrenOfType<PsiJavaCodeReferenceElement>()
                    if (childrenOfTypeReference.isNotEmpty()
                        && childrenOfTypeReference[0].referenceName==typeName){
                        match = childrenOfTypeReference[0].qualifiedName
                    }

                }

            }
            psiFile.accept(TypeFinder())
            return match
        }

        fun isMethodStatic(psiMethod: PsiMethod): Boolean{
            return psiMethod.modifierList.text.contains("static")
        }

        fun getVariableOfType(psiElement: PsiElement, typeName: String): PsiElement?{
            var match: PsiElement? = null
            class TypeFinder: JavaRecursiveElementVisitor() {
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    if (variable.type?.canonicalText?.split(".")?.last()==typeName){
                        match = variable
                    }
                }
                override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                    super.visitReferenceExpression(expression)
                    if (expression.type?.canonicalText?.split(".")?.last()==typeName)
                        match = expression.resolve()
                }
            }
            psiElement.accept(TypeFinder())
            return match
        }

        fun searchForPsiElement(outerPsiElement: PsiElement, elementToSearchFor: PsiElement): PsiElement?{
            var match: PsiElement? = null
            val psiManager = PsiManager.getInstance(
                outerPsiElement.project
            )
            val elementHash = elementToSearchFor.text.filter { !it.isWhitespace() }
            class ElementFinder: JavaRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    super.visitElement(element)
                    if (element.text.filter { !it.isWhitespace() } == elementHash) {
                        match = element
                    }
                }
            }

            outerPsiElement.accept(ElementFinder())
            return match
        }

        fun fetchClassesInPackage(containingClass: PsiClass, project: Project): List<PsiClass> {
            val javaFile = containingClass.containingFile as PsiJavaFile
            val psiPackage = JavaPsiFacade.getInstance(project)
                .findPackage(javaFile.packageName)
            if (psiPackage != null) {
                return runReadAction{ psiPackage.classes.toList() }
            }
            return emptyList()
        }

        fun fetchImportsInFile(file: PsiFile, project: Project): List<PsiClass> {
            return file.childrenOfType<PsiImportStatement>()
                .map {
                    if (it.qualifiedName==null) return@map null
                    if (isInProject(it.qualifiedName!!, project)){
                        return@map findClassFromQualifier(it.qualifiedName!!, project)
                    }
                    null
                }.filterNotNull()
        }

        fun findClassFromQualifier(canonicalType: @NlsSafe String, project: Project): PsiClass? {
            return JavaPsiFacade.getInstance(project)
                .findClass(canonicalType, project.projectScope())
        }

        fun isInProject(qualifier: @NlsSafe String, project: Project): Boolean {
            return JavaPsiFacade.getInstance(project)
                .findClass(qualifier, project.projectScope())!=null
        }

        fun computeCosineSimilarity(psiMethod: PsiMethod, psiClass: PsiClass): Double {
            val methodBody = psiMethod.text
            val classBody = psiClass.text

            return computeCosineSimilarity(methodBody, classBody)
        }

        private fun tokenize(text: String): List<String> {
            return text.split("\\s+".toRegex()).map { it.toLowerCase() }
        }

        private fun termFrequency(tokens: List<String>): Map<String, Int> {
            return tokens.groupingBy { it }.eachCount()
        }

        private fun vectorize(termFreq: Map<String, Int>, vocabulary: Set<String>): List<Double> {
            return vocabulary.map { termFreq[it]?.toDouble() ?: 0.0 }
        }

        private fun cosineSimilarity(vectorA: List<Double>, vectorB: List<Double>): Double {
            val dotProduct = vectorA.zip(vectorB).sumOf { it.first * it.second }
            val magnitudeA = sqrt(vectorA.sumOf { it * it })
            val magnitudeB = sqrt(vectorB.sumOf { it * it })

            return if (magnitudeA != 0.0 && magnitudeB != 0.0) {
                dotProduct / (magnitudeA * magnitudeB)
            } else {
                0.0
            }
        }

        fun computeCosineSimilarity(textA: String, textB: String): Double {
            val tokensA = tokenize(textA)
            val tokensB = tokenize(textB)

            val vocabulary = (tokensA + tokensB).toSet()

            val vectorA = vectorize(termFrequency(tokensA), vocabulary)
            val vectorB = vectorize(termFrequency(tokensB), vocabulary)

            return cosineSimilarity(vectorA, vectorB)
        }
        fun getMethodWithSignatureFromClass(outerClass: PsiElement?, signature: MethodSignature): PsiMethod? {
            var match: PsiMethod? = null
            class MethodFinder: JavaRecursiveElementVisitor() {
                override fun visitMethod(method: PsiMethod) {
                    super.visitMethod(method)
                    if (method.name == signature.methodName &&
                        method.accessModifier()==signature.modifier &&
                        method.returnType.toString().split(":")[1]==signature.returnType &&
                        matchMethodParams(method, signature.paramsList)
                    )
                        match = method
                }

            }
            if (outerClass != null) {
                outerClass.accept(MethodFinder())
            }
            return match
        }

        fun matchMethodParams(psiMethod: PsiMethod, paramsList: List<Parameter>): Boolean{
            if (psiMethod.parameterList.parameters.size!=paramsList.size)
                return false
            for (param in paramsList.withIndex()){
                if (param.index >= psiMethod.parameterList.parameters.size)
                    return false
                if (psiMethod.parameterList.parameters[param.index].name != param.value.name)
                    return false
                if (psiMethod.parameterList.parameters[param.index].type.toString()
                    .split(":")[1].replace(" ", "") != param.value.type) {
                    if (!psiMethod.parameterList.parameters[param.index].type.canonicalText.endsWith(param.value.type))
                        return false
                }
            }
            return true
        }

        fun getMethodParameter(methodPsi: PsiMethod, parameterToFind: Parameter): PsiParameter? {
            var match: PsiParameter? = null
            class MethodFinder: JavaRecursiveElementVisitor() {
                override fun visitParameter(parameter: PsiParameter) {
                    if (parameter.name == parameterToFind.name &&
                        parameter.type.toString().split(":")[1] == parameterToFind.type)
                        match = parameter
                }

            }
            methodPsi.accept(MethodFinder())
            return match
        }

    }


}