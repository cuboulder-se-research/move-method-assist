package com.intellij.ml.llm.template.utils

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset

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

                override fun visitClass(aClass: PsiClass) {
                    super.visitClass(aClass)
                    print("Found ${aClass.name}")
                }

                override fun visitVariable(variable: PsiVariable) {
                    super.visitVariable(variable)
                    print("Found ${variable.name}")
                }
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    print("found :"+variable.name)
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
//                    print("found :"+identifier.text)
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

                override fun visitClass(aClass: PsiClass) {
                    super.visitClass(aClass)
                    print("Found ${aClass.name}")
                }

                override fun visitVariable(variable: PsiVariable) {
                    super.visitVariable(variable)
                    print("Found ${variable.name}")
                }
                override fun visitLocalVariable(variable: PsiLocalVariable) {
                    super.visitLocalVariable(variable)
                    print("found :"+variable.name)
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

    }


}