package com.intellij.ml.llm.template.refactoringobjects

import com.intellij.ml.llm.template.refactoringobjects.renamevariable.RenameVariable
import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFObserver
import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
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
        val renameObj = RenameVariable(
            3, 4, "x", "count"
        )
        renameObj.performRefactoring(project, editor, file)
        assert(PsiUtils.getVariableFromPsi(file, "x")==null)
        val countVariable = PsiUtils.getVariableFromPsi(file, "count")
        assert(countVariable!=null)
        assert(countVariable?.text=="int count =1;")
    }
}