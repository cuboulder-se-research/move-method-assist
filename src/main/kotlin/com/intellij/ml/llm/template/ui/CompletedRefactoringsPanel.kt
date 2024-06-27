package com.intellij.ml.llm.template.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.showEFNotification
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.telemetry.TelemetryDataAction
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.table.JBTable
import java.util.concurrent.atomic.AtomicReference

class CompletedRefactoringsPanel(
    project: Project,
    editor: Editor,
    file: PsiFile,
    candidates: List<AbstractRefactoring>,
    codeTransformer: CodeTransformer,
    highlighter: AtomicReference<ScopeHighlighter>,
    efTelemetryDataManager: EFTelemetryDataManager,
    val reverseRefactorings: List<AbstractRefactoring?> = getReverseObjects(candidates, project, editor, file)
) : RefactoringSuggestionsPanel(
    project, editor,
    file,
    candidates, codeTransformer, highlighter, efTelemetryDataManager, LLMBundle.message("ef.candidates.completed.popup.extract.function.button.title")
) {

    companion object{
        fun getReverseObjects(
            refactoringObjects: List<AbstractRefactoring>,
            project: Project, editor: Editor, file: PsiFile
        ): List<AbstractRefactoring?>{
            return refactoringObjects.map { it.reverseRefactoring }
        }
    }

    override fun performAction(index: Int) {
        if (index !in completedIndices){
            notifyObservers(
                EFNotification(
                    EFTelemetryDataElapsedTimeNotificationPayload(
                        TelemetryDataAction.STOP,
                        prevSelectedCandidateIndex
                    )
                )
            )
            addSelectionToTelemetryData(index)

            // Undo refactorings after index.
            // Undo Refactoring.
            // Redo refactoring.

            val refCandidate = myCandidates[index]
//            for (i in (index until myCandidates.size).reversed()){
//                val refCandidate = myCandidates[i]
//                refCandidate.getReverseRefactoringObject(myProject, myEditor, myFile)?.performRefactoring(myProject, myEditor, myFile)
//            }
//            for (i in index+1 until myCandidates.size){
//                val refCandidate = myCandidates[i]
//                refCandidate.performRefactoring(myProject, myEditor, myFile)
//            }
//

//            val reverseRefactoring = refCandidate.getReverseRefactoringObject(myProject, myEditor, myFile)
            val reverseRefactoring = reverseRefactorings[index]
            if (reverseRefactoring!=null) {
                val runnable = Runnable {
                    reverseRefactoring.performRefactoring(myProject, myEditor, myFile)
                }
                runnable.run()
                //        myPopup!!.cancel()
                refCandidate.undone = true
                refreshCandidates(index, "UNDID")
            }
            else {
                showEFNotification(
                    myProject,
                    LLMBundle.message("notification.extract.function.not.reversible"),
                    NotificationType.ERROR
                )
            }
        }
    }

//    override fun getSelectedRefactoringObject(extractFuncationCandidateJBTable: JBTable): AbstractRefactoring? {
//        val index = extractFuncationCandidateJBTable.selectedRow
//        if (index < reverseRefactorings.size)
//            return reverseRefactorings[index]
//        return null
//    }

    override fun getEndOffset(index: Int): Int {
        if(reverseRefactorings[index]!=null)
            return reverseRefactorings[index]!!.getEndOffset()
        return 0
    }

    override fun getStartOffset(index: Int): Int {
        if(reverseRefactorings[index]!=null)
            return reverseRefactorings[index]!!.getStartOffset()
        return 0
    }

    override fun getStartLoc(index: Int): Int {
        if(reverseRefactorings[index]!=null)
            return reverseRefactorings[index]!!.startLoc
        return 0
    }


}