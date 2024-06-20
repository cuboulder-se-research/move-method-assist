package com.intellij.ml.llm.template.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataElapsedTimeNotificationPayload
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.telemetry.TelemetryDataAction
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFNotification
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.table.JBTable
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicReference
import javax.swing.KeyStroke
import javax.swing.table.DefaultTableModel

class CompletedRefactoringsPanel(
    project: Project,
    editor: Editor,
    file: PsiFile,
    candidates: List<AbstractRefactoring>,
    codeTransformer: CodeTransformer,
    highlighter: AtomicReference<ScopeHighlighter>,
    efTelemetryDataManager: EFTelemetryDataManager
) : RefactoringSuggestionsPanel(
    project, editor,
    file,
    candidates, codeTransformer, highlighter, efTelemetryDataManager, LLMBundle.message("ef.candidates.completed.popup.extract.function.button.title")
) {
//    override fun buildRefactoringCandidatesTable(
//        tableModel: DefaultTableModel,
//        candidateSignatureMap: Map<AbstractRefactoring, String>
//    ): JBTable {
//        val extractFunctionCandidateTable = object : JBTable(tableModel) {
//            override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
//                if (e.keyCode == KeyEvent.VK_ENTER) {
//                    if (e.id == KeyEvent.KEY_PRESSED) {
//                        if (!isEditing && e.modifiersEx == 0) {
//                            undoRefactoring(selectedRow)
//                        }
//                    }
//                    e.consume()
//                    return true
//                }
//                if (e.keyCode == KeyEvent.VK_ESCAPE) {
//                    if (e.id == KeyEvent.KEY_PRESSED) {
//                        myPopup?.cancel()
//                    }
//                }
//                return super.processKeyBinding(ks, e, condition, pressed)
//            }
//
//            override fun processMouseEvent(e: MouseEvent?) {
//                if (e != null && e.clickCount == 2) {
//                    undoRefactoring(selectedRow)
//                }
//                super.processMouseEvent(e)
//            }
//        }
//
//        extractFunctionCandidateTable.minimumSize = Dimension(-1, 100)
//        extractFunctionCandidateTable.tableHeader = null
//
//
//        extractFunctionCandidateTable.columnModel.getColumn(0).maxWidth = 50
//        extractFunctionCandidateTable.columnModel.getColumn(1).cellRenderer = FunctionNameTableCellRenderer()
//        extractFunctionCandidateTable.setShowGrid(false)
//
//        return extractFunctionCandidateTable
//    }

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
            val efCandidate = myCandidates[index]

            val runnable = Runnable {
                efCandidate.getReverseRefactoringObject(myProject, myEditor, myFile)
                    ?.performRefactoring(myProject, myEditor,myFile)
            }
            runnable.run()
            //        myPopup!!.cancel()
            refreshCandidates(index, "UNDID")
        }
    }


}