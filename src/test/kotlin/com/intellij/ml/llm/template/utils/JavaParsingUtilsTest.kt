package com.intellij.ml.llm.template.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.io.path.Path

class JavaParsingUtilsTest{

    @Test
    fun testIsStatic(){
        val filePath = "/Users/abhiram/Documents/TBE/RefactoringMiner/src/main/java/org/refactoringminer/util/AstUtils.java"
        val outPath = "/Users/abhiram/Documents/TBE/RefactoringAgentProject/llm-guide-refactorings/data/refminer_data/isStaticOut.txt"
        val signature = "public getKeyFromMethodBinding(binding IMethodBinding) : String"
        assertTrue(
            JavaParsingUtils.isMethodStatic(Path(filePath), signature)
        )
    }

    @Test
    fun testFindFields(){
        val filePath = "/Users/abhiram/Documents/TBE/RefactoringMiner/src/main/java/org/refactoringminer/util/AstUtils.java"
        assertTrue(
            JavaParsingUtils.findFieldTypes(Path(filePath), "org.refactoringminer.util.AstUtils").isEmpty()
        )
    }

    @Test
    fun testFindFields2(){
        val filePath = "/Users/abhiram/Documents/TBE/jmove/src/src/br/ufmg/dcc/labsoft/java/jmove/approach/CalculateMediaApproach.java"

        val fields = JavaParsingUtils.findFieldTypes(Path(filePath), "br.ufmg.dcc.labsoft.java.jmove.approach.CalculateMediaApproach")
        print(fields)
        assertTrue(
            fields.isNotEmpty()
        )
    }
}