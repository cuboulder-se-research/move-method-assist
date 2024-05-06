package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.GPTExtractFunctionRequestProvider
import com.intellij.testFramework.LightPlatformCodeInsightTestCase

class SimpleRefactoringValidatorTest :LightPlatformCodeInsightTestCase(){
//    private var projectPath = "src/test"
//    override fun getTestDataPath(): String {
//        return projectPath
//    }

    fun testReadResourceFile(){
        val content = SimpleRefactoringValidatorTest::class.java.getResource("/func_src.txt")?.readText()
        println(content)
    }
    fun testGetRefactoringSuggestions() {
        val srcFile = "/testdata/A1_CSC540.java"

        val funcSrc = SimpleRefactoringValidatorTest::class.java.getResource("/func_src.txt")?.readText()
        val llmResponse = SimpleRefactoringValidatorTest::class.java.getResource("/llm_output.txt")?.readText()


        val suggestions = SimpleRefactoringValidator(
            GPTExtractFunctionRequestProvider,
            project,
            editor,
            file,
            funcSrc!!
        ).getRefactoringSuggestions(llmResponse!!)

        println(suggestions)

        assert(suggestions.size==2)

    }

}