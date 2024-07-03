package com.intellij.ml.llm.template.utils

import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring

fun refactoringOrderComparator() = Comparator<AbstractRefactoring>{ a, b ->
    when {
        (a.getStartOffset() > b.getStartOffset() && a.getEndOffset() < b.getEndOffset()) -> -1
        (b.getStartOffset() > a.getStartOffset() && b.getEndOffset() < a.getEndOffset()) -> 1
        else -> 0
    }
}
fun getExecutionOrder(validRefactoringCandidates: List<AbstractRefactoring>): List<AbstractRefactoring> {
    return validRefactoringCandidates.sortedWith(refactoringOrderComparator())
}