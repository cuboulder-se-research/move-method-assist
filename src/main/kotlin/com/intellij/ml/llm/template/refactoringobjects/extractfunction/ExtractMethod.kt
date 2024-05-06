package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.models.FunctionNameProvider
import com.intellij.ml.llm.template.models.MyMethodExtractor
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.customextractors.MyInplaceExtractionHelper
import com.intellij.ml.llm.template.utils.EFLoggerObserver
import com.intellij.ml.llm.template.utils.isCandidateExtractable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler

class ExtractMethod(
    override val startLoc: Int,
    override val endLoc: Int,
    val newFuncName: String,
) : AbstractRefactoring {

    var efCandidate: EFCandidate? =null

    companion object{
        const val REFACTORING_NAME = "Extract Method"

        fun fromEFCandidate(candidate: EFCandidate): ExtractMethod{
            val em = ExtractMethod(candidate.lineStart, candidate.lineEnd, candidate.functionName)
            em.efCandidate = candidate
            return em
        }

    }

    override fun performRefactoring(project: Project, editor: Editor, file: PsiFile) {
        editor.selectionModel.setSelection(this.getStartOffset(), this.getEndOffset())
        invokeExtractFunction(newFuncName, project, editor, file)
    }

    override fun getStartOffset(): Int {
        return efCandidate!!.offsetStart
    }

    override fun getEndOffset(): Int {
        return efCandidate!!.offsetEnd
    }


    override fun isValid(project: Project, editor: Editor, file: PsiFile): Boolean {
        return this.efCandidate?.let {
            isCandidateExtractable(
                it, editor, file
            )
        }?: false

    }

    override fun getRefactoringName(): String {
        return REFACTORING_NAME
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