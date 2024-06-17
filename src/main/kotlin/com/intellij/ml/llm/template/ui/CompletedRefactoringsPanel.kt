package com.intellij.ml.llm.template.ui

import com.intellij.codeInsight.unwrap.ScopeHighlighter
import com.intellij.ml.llm.template.LLMBundle
import com.intellij.ml.llm.template.refactoringobjects.AbstractRefactoring
import com.intellij.ml.llm.template.telemetry.EFTelemetryDataManager
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.ui.table.JBTable
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicReference
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
    override fun buildRefactoringCandidatesTable(
        tableModel: DefaultTableModel,
        candidateSignatureMap: Map<AbstractRefactoring, String>
    ): JBTable {
        val extractFunctionCandidateTable = object : JBTable(tableModel) {
        }
        extractFunctionCandidateTable.minimumSize = Dimension(-1, 100)
        extractFunctionCandidateTable.tableHeader = null


        extractFunctionCandidateTable.columnModel.getColumn(0).maxWidth = 50
        extractFunctionCandidateTable.columnModel.getColumn(1).cellRenderer = FunctionNameTableCellRenderer()
        extractFunctionCandidateTable.setShowGrid(false)

        return extractFunctionCandidateTable
    }
}