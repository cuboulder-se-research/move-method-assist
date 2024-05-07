package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariable
import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariableFactory
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFObserver
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.idea.core.moveCaret
import org.junit.jupiter.api.Assertions.*

class RenameVariableTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun testRename() {
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)

        configureByFile("/testdata/HelloWorld.java")
        editor.moveCaret(72)
        val functionPsi: PsiElement? = PsiUtils.getParentFunctionOrNull(
            PsiUtilBase.getElementAtCaret(editor), file.language)
        val renameObj = RenameVariable.fromOldNewName(
            project, functionPsi,
//            3, 4,
            "x", "count"
        )
        assert(renameObj!=null)
        renameObj?.performRefactoring(project, editor, file)
        assert(PsiUtils.getVariableFromPsi(file, "x")==null)
        val countVariable = PsiUtils.getVariableFromPsi(file, "count")
        assert(countVariable!=null)
        assert(countVariable?.text=="int count =1;")
    }
}