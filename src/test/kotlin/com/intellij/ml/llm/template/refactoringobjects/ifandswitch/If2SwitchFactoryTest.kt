package com.intellij.ml.llm.template.refactoringobjects.ifandswitch

import com.intellij.codeInsight.daemon.impl.quickfix.ConvertSwitchToIfIntention
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ml.llm.template.refactoringobjects.conditionals.If2Switch
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.LightPlatformCodeInsightTestCase
import com.siyeh.ig.controlflow.ConditionalExpressionInspection
import com.siyeh.ig.migration.IfCanBeSwitchInspection
import com.siyeh.ig.style.SimplifiableIfStatementInspection
import io.ktor.util.reflect.*
import org.jetbrains.kotlin.idea.base.psi.getLineStartOffset
import org.jetbrains.uast.util.isInstanceOf
import org.junit.jupiter.api.Assertions.*
import java.util.function.Predicate

class If2SwitchFactoryTest: LightPlatformCodeInsightTestCase(){

    private var projectPath = "src/test"
    override fun getTestDataPath(): String {
        return projectPath
    }

    fun testSwitch2If(){
        configureByFile("/testdata/HelloWorld.java")
        val startLoc = 21
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc))
        val switchStatement = PsiTreeUtil.getParentOfType(element, PsiSwitchStatement::class.java)
        assert(switchStatement!=null)
        val intention = ConvertSwitchToIfIntention(switchStatement!!)
        WriteCommandAction.runWriteCommandAction(project,
            Runnable { intention.invoke(project, editor, file) })

        println(file.text)
        assert(file.text.contains("    public void prettyPrintInteger(Integer num){\n" +
                "        if (num == 1) {\n" +
                "            System.out.println(\"ONE!!\");\n" +
                "        } else if (num == 2) {\n" +
                "            System.out.println(\"TWO!!\");\n" +
                "        } else if (num == 5) {\n" +
                "            System.out.println(\"FIVE!!\");\n" +
                "        } else {\n" +
                "            System.out.println(num);\n" +
                "        }\n" +
                "    }"
        ))

    }


    fun testIf2Switch(){

        configureByFile("/testdata/HelloWorld.java")

        val refObjs = If2Switch.factory.createObjectsFromFuncCall(
            "convert_if2switch(29)", project, editor, file)
        assert(refObjs.isNotEmpty())
        refObjs[0].performRefactoring(project, editor, file)

    }

    fun testIf2Ternary(){
        configureByFile("/testdata/HelloWorld.java")

        val startLoc = 41
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc))
        val ifStatement = PsiTreeUtil.getParentOfType(element, PsiIfStatement::class.java)

        val simplifyIf = SimplifiableIfStatementInspection()
        simplifyIf.DONT_WARN_ON_TERNARY = false
        simplifyIf.DONT_WARN_ON_CHAINED_ID = false

        val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
        val visitor = simplifyIf.buildVisitor(problemsHolder, false)
        ifStatement?.accept(visitor)

        assert(problemsHolder.hasResults())
        val problem = problemsHolder.results[0]!!
        val fix = problem.fixes!![0]

        WriteCommandAction.runWriteCommandAction(project,
            Runnable { fix.applyFix(project, problem) })
        println(file.text)
        assert(file.text.contains(
          "    public Integer numMinus10(Integer num){\n" +
                "        return num >= 0 ? num - 10 : num + 10;\n" +
                "    }"))
    }

    fun testTernary2If(){

        configureByFile("/testdata/HelloWorld.java")

        val startLoc = 48
        val element =
            PsiUtilBase.getElementAtOffset(
                file, editor.document.getLineStartOffset(startLoc-1))

        val startOffset = editor.document.getLineStartOffset(startLoc-1)
        val endOffset = editor.document.getLineStartOffset(startLoc)
        val conditionalExpression = PsiTreeUtil.findChildrenOfType(element.parent, PsiConditionalExpression::class.java)
            .filter { it.startOffset in startOffset..endOffset }[0]


        val ternaryToIf = ConditionalExpressionInspection()

        val problemsHolder = ProblemsHolder(InspectionManager.getInstance(project), file, false)
        val visitor = ternaryToIf.buildVisitor(problemsHolder, false)
        conditionalExpression?.accept(visitor)

        assert(problemsHolder.hasResults())
        val problem = problemsHolder.results[0]!!
        val fix = problem.fixes!![0]

        WriteCommandAction.runWriteCommandAction(project,
            Runnable { fix.applyFix(project, problem) })
        println(file.text)
        assert(file.text.contains(
            "    public Integer numMinus10Ternary(Integer num){\n" +
                    "        if (num >= 0) return num - 10;\n" +
                    "        return num + 10;\n" +
                    "    }"))
    }


}