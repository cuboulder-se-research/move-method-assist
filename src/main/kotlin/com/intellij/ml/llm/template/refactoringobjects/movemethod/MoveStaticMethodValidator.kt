package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveMembers.MoveMembersDialog
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class MoveStaticMethodValidator(
    project: Project,
    sourceClass: PsiClass,
    targetClass: PsiClass,
    methodToMove: PsiMethod
): MoveMembersProcessor(
    project,
    MoveMembersDialog(project, sourceClass, targetClass, setOf(methodToMove), MoveCallback {  })
) {
    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        return super.preprocessUsages(refUsages)
    }

    override fun findUsages(): Array<UsageInfo> {
        return super.findUsages()
    }

    override fun showConflicts(conflicts: MultiMap<PsiElement, String>, usages: Array<out UsageInfo>?): Boolean {
        return conflicts.isEmpty
    }
}