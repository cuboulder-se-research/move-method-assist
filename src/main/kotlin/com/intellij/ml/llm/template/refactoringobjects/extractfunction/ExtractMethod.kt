package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.models.FunctionNameProvider
import com.intellij.ml.llm.template.models.MyMethodExtractor
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.customextractors.MyInplaceExtractionHelper
import com.intellij.ml.llm.template.utils.isCandidateExtractable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ExtractMethod(
    override val startLoc: Int,
    override val endLoc: Int,
    val newFuncName: String,
    val leftPsi: PsiElement,
    val rightPsi: PsiElement,
    val candidateType: EfCandidateType
) : AbstractRefactoring() {

//    var efCandidate: EFCandidate? =null

    companion object{
        const val REFACTORING_NAME = "Extract Method"

    }

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        editor.selectionModel.setSelection(this.getStartOffset(), this.getEndOffset())
        invokeExtractFunction(newFuncName, project, editor, file)
    }

    override fun getStartOffset(): Int {
        return leftPsi.startOffset
    }

    override fun getEndOffset(): Int {
        return rightPsi.endOffset
    }


    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        val candidate = getEFCandidate()

        return isCandidateExtractable(
            candidate, editor, file
        )

    }

    fun getEFCandidate(): EFCandidate {
        val candidate = EFCandidate(
            functionName = this.newFuncName,
            offsetStart = this.getStartOffset(),
            offsetEnd = this.getEndOffset(),
            lineStart = this.startLoc,
            lineEnd = this.endLoc,
        ).also {
            it.efSuggestion = EFSuggestion(this.newFuncName, this.startLoc, this.endLoc)
            it.type = candidateType
        }
        return candidate
    }

    override fun getRefactoringPreview(): String {
        return "${REFACTORING_NAME}: $newFuncName"
    }


    private fun invokeExtractFunction(newFunctionName: String, project: Project, editor: Editor?, file: PsiFile?) {
        val functionNameProvider = FunctionNameProvider(newFunctionName)
        when (file?.language) {
            JavaLanguage.INSTANCE -> {
                MyMethodExtractor.invokeOnElements(
                    project, editor, file, findSelectedPsiElements(editor, file), FunctionNameProvider(newFunctionName)
                )
            }

            KotlinLanguage.INSTANCE -> {
                val dataContext = (editor as EditorEx).dataContext
                val allContainersEnabled = false
                val inplaceExtractionHelper = MyInplaceExtractionHelper(allContainersEnabled, functionNameProvider)
                ExtractKotlinFunctionHandler(allContainersEnabled, inplaceExtractionHelper).invoke(
                    project, editor, file, dataContext
                )
            }
        }
    }

    private fun findSelectedPsiElements(editor: Editor?, file: PsiFile?): Array<PsiElement> {
        if (editor == null) {
            return emptyArray()
        }
        val selectionModel = editor.selectionModel
        val startOffset = selectionModel.selectionStart
        val endOffset = selectionModel.selectionEnd

        val startElement = file?.findElementAt(startOffset)
        val endElement = file?.findElementAt(if (endOffset > 0) endOffset - 1 else endOffset)

        if (startElement == null || endElement == null) {
            return emptyArray()
        }

        val commonParent = PsiTreeUtil.findCommonParent(startElement, endElement) ?: return emptyArray()

        val selectedElements = PsiTreeUtil.findChildrenOfType(commonParent, PsiElement::class.java)
        val result = selectedElements.filter {
            it.textRange.startOffset >= startOffset && it.textRange.endOffset <= endOffset
        }.toTypedArray()
        return result
    }


}