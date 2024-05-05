package com.intellij.ml.llm.template.utils

import com.intellij.lang.java.JavaLanguage
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.customextractors.MyInplaceExtractionHelper
import com.intellij.ml.llm.template.refactoringobjects.extractfunction.EFCandidate
import com.intellij.ml.llm.template.models.FunctionNameProvider
import com.intellij.ml.llm.template.models.MyMethodExtractor
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ExtractKotlinFunctionHandler

class CodeTransformer : Observable() {
    private val logger = Logger.getInstance("#com.intellij.ml.llm")

    fun applyCandidate(refCandidate: AbstractRefactoring, project: Project, editor: Editor, file: PsiFile): Boolean {
        var applicationResult = EFApplicationResult.OK
        var reason = ""

//        if (!isCandidateValid(efCandidate)) {
         if (!refCandidate.isValid()){
            applicationResult = EFApplicationResult.FAIL
            reason = "invalid extract function candidate"
        } else {
            editor.selectionModel.setSelection(refCandidate.getStartOffset(), refCandidate.getEndOffset())
            try {
//                invokeExtractFunction(refCandidate.functionName, project, editor, file)
                refCandidate.performRefactoring(project, editor, file)
            } catch (e: Exception) {
                applicationResult = EFApplicationResult.FAIL
                reason = e.message ?: ""
            } catch (e: Error) {
                applicationResult = EFApplicationResult.FAIL
                reason = e.message ?: ""
            }
        }

        notifyObservers(
            EFNotification(
                EFCandidateApplicationPayload(
                    result = applicationResult,
                    reason = reason,
                    candidate = refCandidate
                )
            )
        )

        return applicationResult == EFApplicationResult.OK
    }





    private fun isCandidateValid(efCandidate: EFCandidate): Boolean {
        return efCandidate.offsetStart >= 0 && efCandidate.offsetEnd >= 0
    }
}