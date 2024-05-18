package com.intellij.ml.llm.template.refactoringobjects.renamevariable

import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariable
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFObserver
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilBase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.jetbrains.kotlin.idea.core.moveCaret

class RenameVariableTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun testRenameLocalVariable() {
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)

        configureByFile("/testdata/HelloWorld.java")
        editor.moveCaret(122)
        val functionPsi: PsiElement? = PsiUtils.getParentFunctionOrNull(
            PsiUtilBase.getElementAtCaret(editor), file.language)
        val renameObj = RenameVariableFactory.fromOldNewName(
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

    fun testRenameField() {
        val codeTransformer = CodeTransformer()
        val efObserver = EFObserver()
        codeTransformer.addObserver(efObserver)

        configureByFile("/testdata/A1_CSC540.java")
        editor.moveCaret(7582)
        val functionPsi: PsiElement? = PsiUtils.getParentFunctionOrNull(
            PsiUtilBase.getElementAtCaret(editor), file.language)
        val renameObj = RenameVariableFactory.fromOldNewName(
            project, functionPsi,
//            3, 4,
            "x", "character"
        )
        assert(renameObj!=null)
        renameObj?.performRefactoring(project, editor, file)
        assert(PsiUtils.getVariableFromPsi(file, "x")==null)
        val characterVariable = PsiUtils.getVariableFromPsi(file, "character")
        assert(characterVariable!=null)
        assert(characterVariable?.text=="public char character;")
    }
}