package com.intellij.ml.llm.template.refactoringobjects.stringbuilder

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiPolyadicExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ig.controlflow.ConditionalExpressionInspection
import com.siyeh.ig.migration.StringBufferReplaceableByStringBuilderInspection
import com.siyeh.ig.performance.StringConcatenationInLoopsInspection
import com.siyeh.ipp.concatenation.ReplaceConcatenationWithStringBufferIntention
import org.junit.jupiter.api.Assertions.*

class StringBuilderRefactoringTest: LightPlatformCodeInsightTestCase(){

    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }
    fun testStringBuilder(){
//        val stringBuilderRefactoring = StringBufferReplaceableByStringBuilderInspection()
        configureByFile("/testdata/HelloWorld.java")

        val startLoc = 63
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc-1))

        val startOffset = editor.document.getLineStartOffset(startLoc-1)
        val endOffset = editor.document.getLineStartOffset(startLoc)
        val polyadicExpression = PsiTreeUtil
            .findChildrenOfType(element.parent, PsiPolyadicExpression::class.java)
            .filter { it.startOffset in startOffset..endOffset }[0]


        val sbConcat = ReplaceConcatenationWithStringBufferIntention()

        WriteCommandAction.runWriteCommandAction(project,
            Runnable {
                sbConcat.processIntention(polyadicExpression)
            })
        println(file.text)
        assert(file.text.contains(
            "    public static void constructString(Integer a, Boolean b){\n" +
                    "        String s = new StringBuilder().append(\"String: \").append(\"s\").append(a).append(b).toString();\n" +
                    "        System.out.println(s);\n" +
                    "    }"))
    }

    fun testStringBuilderLoop(){
//        val stringBuilderRefactoring = StringBufferReplaceableByStringBuilderInspection()
        configureByFile("/testdata/HelloWorld.java")

        val startLoc = 55
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc-1))

        val startOffset = editor.document.getLineStartOffset(startLoc-1)
        val endOffset = editor.document.getLineStartOffset(startLoc)
        val polyadicExpression = PsiTreeUtil
            .findChildrenOfType(element.parent, PsiAssignmentExpression::class.java)
            .filter { it.startOffset in startOffset..endOffset }[0]


        val sbConcat = StringConcatenationInLoopsInspection()
        val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
        val visitor = sbConcat.buildVisitor(problemsHolder, false)
        polyadicExpression?.accept(visitor)
        assert(problemsHolder.hasResults())

        val problem = problemsHolder.results[0]!!
        val fix = problem.fixes!![0]

        WriteCommandAction.runWriteCommandAction(project,
            Runnable { fix.applyFix(project, problem) })
        println(file.text)
        assert(file.text.contains(
            "    public static void constructString(Integer a, Boolean b){\n" +
                    "        String s = new StringBuilder().append(\"String: \").append(\"s\").append(a).append(b).toString();\n" +
                    "        System.out.println(s);\n" +
                    "    }"))
    }
}