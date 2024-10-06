package com.intellij.ml.llm.template.refactoringobjects.movemethod

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiVariable
import com.intellij.refactoring.move.moveInstanceMethod.MoveInstanceMethodDialog

class MyMoveInstanceMethodDialog(method: PsiMethod,
                                 variables: Array<out PsiVariable>
) : MoveInstanceMethodDialog(method, variables) {
    var triggeredRefactoring = false

    override fun doAction() {
        triggeredRefactoring = true
        super.doAction()
    }
}