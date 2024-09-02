package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class MoveInstanceMethodProcessorAutoValidator(project: Project,
                                               method: PsiMethod,
                                               targetVariable: PsiVariable,
                                               newVisibility: String,
                                               oldClassParameterNames: Map<PsiClass, String> ):
    MoveInstanceMethodProcessor(project, method, targetVariable, newVisibility, oldClassParameterNames) {
    override fun showConflicts(conflicts: MultiMap<PsiElement, String>, usages: Array<out UsageInfo>?): Boolean {
        if (conflicts.isEmpty){
            return true
        }
        return false
    }

    override fun findUsages(): Array<UsageInfo> { // To make reflection work
        return super.findUsages()
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean { //to make reflection work.
        return super.preprocessUsages(refUsages)
    }
}