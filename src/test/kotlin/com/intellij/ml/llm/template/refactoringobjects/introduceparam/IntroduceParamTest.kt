package com.intellij.ml.llm.template.refactoringobjects.introduceparam

import com.intellij.ml.llm.template.utils.PsiUtils
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.refactoring.introduceParameter.IntroduceParameterProcessor
import com.intellij.refactoring.introduceVariable.IntroduceVariableBase
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import it.unimi.dsi.fastutil.ints.IntList

class IntroduceParamTest: LightPlatformCodeInsightTestCase() {
    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    fun testIntroduceParam(){
        configureByFile("/testdata/HelloWorld.java")
        val lineNumber = 14
        val psiMethods: List<PsiMethod> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, lineNumber, PsiMethod::class.java
        )
        assert(psiMethods.isNotEmpty())
        val methodToReplace = psiMethods[0]
        val psiLiterals: List<PsiLiteralExpression> = PsiUtils.getElementsOfTypeOnLine(
            file, editor, 17, PsiLiteralExpression::class.java
        )
        assert(psiLiterals.isNotEmpty())


        IntroduceParameterProcessor(
            project,
            methodToReplace,
            methodToReplace,
            psiLiterals[0],
            psiLiterals[0],
            null,
            false,
            "printString",
            IntroduceVariableBase.JavaReplaceChoice.ALL,
            1,
            false,
            false,
            false,
            psiLiterals[0].type,
            IntList.of()
        ).run()

        println(file.text)
    }
}