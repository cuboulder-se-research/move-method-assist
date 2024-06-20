package com.intellij.ml.llm.template.refactoringobjects.extractfunction

import com.intellij.ml.llm.template.utils.CodeTransformer
import com.intellij.ml.llm.template.utils.EFObserver
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import org.junit.jupiter.api.Assertions.*

class InlineMethodFactoryTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }


    fun testInlineMethod() {
        configureByFile("/testdata/HelloWorld.java")

        val refObjs = InlineMethodFactory.createObjectsFromFuncCall(
            "inline_method(\"constructString\")", project, editor, file
        )

        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)
        print(file.text)
    }
}