package com.intellij.ml.llm.template.suggestrefactoring

import com.intellij.ml.llm.template.models.GPTExtractFunctionRequestProvider
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import kotlinx.coroutines.runBlocking

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

        runBlocking {
            val suggestions = SimpleRefactoringValidator(
                GPTExtractFunctionRequestProvider,
                project,
                editor,
                file,
                funcSrc!!,
                mutableMapOf()
            ).getRefactoringSuggestions(llmResponse!!, 1000)

            println(suggestions)

            assert(suggestions.size == 2)
        }

    }

}